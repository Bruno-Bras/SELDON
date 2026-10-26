package com.seldon.fleet.data.repository

import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.core.Resource
import com.seldon.fleet.data.local.FleetDataSource
import com.seldon.fleet.data.parser.CorruptedFileParser
import com.seldon.fleet.data.prefs.FleetPreferencesDataSource
import com.seldon.fleet.domain.model.FleetFailure
import com.seldon.fleet.domain.model.FleetSnapshot
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class FleetRepositoryImplTest {

    @Test
    fun `observeFleet emits Loading then combines fleet and persisted favorites`() = runTest {
        val preferences = FakePreferences(initial = setOf("DEV-1"))
        val repository = repository(
            raw = oneDeviceJson("DEV-1"),
            preferences = preferences,
            scope = backgroundScope,
        )

        val emissions = mutableListOf<Resource<FleetSnapshot>>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeFleet().take(2).toList(emissions)
        }
        collector.join()

        assertThat(emissions[0]).isEqualTo(Resource.Loading)
        val success = emissions[1] as Resource.Success
        assertThat(success.data.fleet.devices).hasSize(1)
        assertThat(success.data.favorites).containsExactly("DEV-1")
    }

    @Test
    fun `favorite changes flow through the combined repository stream`() = runTest {
        val preferences = FakePreferences()
        val repository = repository(
            raw = oneDeviceJson("DEV-1"),
            preferences = preferences,
            scope = backgroundScope,
        )

        val favoriteStates = mutableListOf<Set<String>>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeFleet()
                .map { (it as? Resource.Success)?.data?.favorites ?: emptySet() }
                .take(2)
                .toList(favoriteStates)
        }

        assertThat(favoriteStates.first()).isEmpty()
        assertThat(repository.toggleFavorite("DEV-1").getOrThrow()).isTrue()
        collector.join()

        assertThat(favoriteStates.last()).containsExactly("DEV-1")
    }

    @Test
    fun `corrupted source becomes an explicit failure`() = runTest {
        val repository = repository(
            raw = "{ definitely not json",
            preferences = FakePreferences(),
            scope = backgroundScope,
        )

        val failure = repository.observeFleet()
            .filterIsInstance<Resource.Failure>()
            .first()

        assertThat(failure.failure).isInstanceOf(FleetFailure.MalformedSource::class.java)
    }

    @Test
    fun `source read errors become ASSET_UNAVAILABLE`() = runTest {
        val repository = repository(
            raw = null,
            preferences = FakePreferences(),
            scope = backgroundScope,
            readFailure = IOException("asset missing"),
        )

        val failure = repository.observeFleet()
            .filterIsInstance<Resource.Failure>()
            .first()

        assertThat(failure.failure.code).isEqualTo("ASSET_UNAVAILABLE")
    }

    @Test
    fun `toggleFavorite failure is returned as Result failure`() = runTest {
        val preferences = FakePreferences(failWrites = true)
        val repository = repository(
            raw = oneDeviceJson("DEV-1"),
            preferences = preferences,
            scope = backgroundScope,
        )

        val result = repository.toggleFavorite("DEV-1")

        assertThat(result.isFailure).isTrue()
    }

    private fun TestScope.repository(
        raw: String?,
        preferences: FleetPreferencesDataSource,
        scope: CoroutineScope,
        readFailure: Throwable? = null,
    ) = FleetRepositoryImpl(
        dataSource = object : FleetDataSource {
            override suspend fun readFleet(): String {
                if (readFailure != null) throw readFailure
                return requireNotNull(raw)
            }
        },
        parser = CorruptedFileParser(
            json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                allowSpecialFloatingPointValues = true
            },
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        ),
        preferences = preferences,
        applicationScope = scope,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun oneDeviceJson(id: String): String =
        """
        {
          "fleetId": "TEST-FLEET",
          "devices": [
            {
              "id": "$id",
              "model": "Test Device",
              "soc": "Test SoC",
              "tier": "mid",
              "batteryCapacityMah": 5000,
              "screenOnMinutes": 120,
              "energyConsumed": 500,
              "energyUnit": "mAh",
              "jankFramesPct": 1,
              "crashCount7d": 0
            }
          ]
        }
        """.trimIndent()

    private class FakePreferences(
        initial: Set<String> = emptySet(),
        private val failWrites: Boolean = false,
    ) : FleetPreferencesDataSource {
        private val favoriteState = MutableStateFlow(initial)

        override val favorites: Flow<Set<String>> = favoriteState

        override suspend fun toggleFavorite(deviceId: String): Result<Boolean> {
            if (failWrites) return Result.failure(IOException("disk full"))
            val current = favoriteState.value.toMutableSet()
            val isFavorite = if (current.contains(deviceId)) {
                current.remove(deviceId)
                false
            } else {
                current.add(deviceId)
                true
            }
            favoriteState.value = current
            return Result.success(isFavorite)
        }
    }
}

package com.seldon.fleet.data.repository

import com.seldon.fleet.core.Resource
import com.seldon.fleet.data.local.FleetDataSource
import com.seldon.fleet.data.parser.CorruptedFileParser
import com.seldon.fleet.data.parser.ParseOutcome
import com.seldon.fleet.data.prefs.FleetPreferencesDataSource
import com.seldon.fleet.di.ApplicationScope
import com.seldon.fleet.di.IoDispatcher
import com.seldon.fleet.domain.model.Fleet
import com.seldon.fleet.domain.model.FleetFailure
import com.seldon.fleet.domain.model.FleetSnapshot
import com.seldon.fleet.domain.repository.FleetRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class FleetRepositoryImpl @Inject constructor(
    private val dataSource: FleetDataSource,
    private val parser: CorruptedFileParser,
    private val preferences: FleetPreferencesDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FleetRepository {

    private val refreshTrigger = MutableStateFlow(0)

    /**
     * Single Source of Truth do arquivo: uma leitura/parse é compartilhada por
     * lista e detalhe, e o refresh reexecuta o pipeline inteiro.
     */
    private val fleetResource: StateFlow<Resource<Fleet>> = refreshTrigger
        .flatMapLatest { loadFleet() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.Loading,
        )

    private val favorites: Flow<Set<String>> = preferences.favorites
        .catch { emit(emptySet()) }

    override fun observeFleet(): Flow<Resource<FleetSnapshot>> =
        combine(fleetResource, favorites) { fleetResult, favoriteIds ->
            when (fleetResult) {
                is Resource.Loading -> Resource.Loading
                is Resource.Failure -> Resource.Failure(fleetResult.failure)
                is Resource.Success -> Resource.Success(
                    FleetSnapshot(fleet = fleetResult.data, favorites = favoriteIds),
                )
            }
        }.distinctUntilChanged()

    override suspend fun toggleFavorite(deviceId: String): Result<Boolean> =
        preferences.toggleFavorite(deviceId)

    override suspend fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    private fun loadFleet(): Flow<Resource<Fleet>> = flow {
        emit(Resource.Loading)

        val raw = try {
            withContext(ioDispatcher) { dataSource.readFleet() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            emit(
                Resource.Failure(
                    FleetFailure.AssetUnavailable(
                        "Não foi possível ler assets/frota.json: " +
                            (error.message ?: error::class.java.simpleName),
                    ),
                ),
            )
            return@flow
        }

        when (val outcome = parser.parse(raw)) {
            is ParseOutcome.Success -> emit(Resource.Success(outcome.fleet))
            is ParseOutcome.Failure -> emit(Resource.Failure(outcome.failure))
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(
            Resource.Failure(
                FleetFailure.Unexpected(error.message ?: error::class.java.simpleName),
            ),
        )
    }
}

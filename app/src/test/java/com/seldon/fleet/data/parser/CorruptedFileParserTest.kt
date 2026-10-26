package com.seldon.fleet.data.parser

import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.domain.engine.FleetAnomalyEngine
import com.seldon.fleet.domain.model.DataIssueCode
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.FleetFailure
import java.io.File
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class CorruptedFileParserTest {

    private val engine = FleetAnomalyEngine()

    @Test
    fun `official dataset has the expected status distribution`() = runTest {
        val outcome = parser().parse(officialJson())

        assertThat(outcome).isInstanceOf(ParseOutcome.Success::class.java)
        val fleet = (outcome as ParseOutcome.Success).fleet
        val analyses = fleet.devices.map(engine::analyze)
        val counts = analyses.groupingBy { it.status }.eachCount()

        assertThat(fleet.devices).hasSize(30)
        assertThat(fleet.warnings).isEmpty()
        assertThat(counts[DeviceStatus.OK]).isEqualTo(16)
        assertThat(counts[DeviceStatus.ATENCAO]).isEqualTo(8)
        assertThat(counts[DeviceStatus.CRITICO]).isEqualTo(4)
        assertThat(counts[DeviceStatus.INDETERMINADO]).isEqualTo(2)
        assertThat(fleet.devices.count { it.dataIssues.isNotEmpty() }).isEqualTo(2)
    }

    @Test
    fun `official mWh records are normalized and DEV-0204 stays OK`() = runTest {
        val fleet = successFleet()
        val analysis = fleet.devices
            .first { it.id == "DEV-0204" }
            .let(engine::analyze)

        assertThat(analysis.device.energyConsumedMah).isWithin(0.0001).of(980.0)
        assertThat(analysis.relativeDrainPctPerHour).isWithin(0.001).of(6.3226)
        assertThat(analysis.status).isEqualTo(DeviceStatus.OK)
        assertThat(analysis.device.dataIssues).isEmpty()
    }

    @Test
    fun `official critical drain record is classified CRITICO`() = runTest {
        val fleet = successFleet()
        val analysis = fleet.devices
            .first { it.id == "DEV-0522" }
            .let(engine::analyze)

        assertThat(analysis.device.energyConsumedMah).isWithin(0.001).of(1585.6589)
        assertThat(analysis.relativeDrainPctPerHour).isWithin(0.001).of(13.4)
        assertThat(analysis.status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `official indeterminate records keep the reason from the source`() = runTest {
        val fleet = successFleet()
        val zeroCapacity = fleet.devices
            .first { it.id == "DEV-0311" }
            .let(engine::analyze)
        val zeroScreen = fleet.devices
            .first { it.id == "DEV-0745" }
            .let(engine::analyze)

        assertThat(zeroCapacity.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(zeroCapacity.device.dataIssues.map { it.code })
            .contains(DataIssueCode.INVALID_CAPACITY)
        assertThat(zeroScreen.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(zeroScreen.device.dataIssues.map { it.code })
            .contains(DataIssueCode.INVALID_SCREEN_ON)
    }

    @Test
    fun `mWh is converted to mAh using nominal voltage`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-MWH",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 1000,
                  "energyUnit": "mWh",
                  "nominalVoltage": 4.0,
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        assertThat(device.energyConsumedMah).isWithin(0.0001).of(250.0)
    }

    @Test
    fun `same physical consumption in mAh and mWh yields the same drain and status`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-MAH",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 1000,
                  "energyUnit": "mAh",
                  "nominalVoltage": 4.0,
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
                """
                {
                  "id": "DEV-MWH",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 4000,
                  "energyUnit": "mWh",
                  "nominalVoltage": 4.0,
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val analyses = (outcome as ParseOutcome.Success).fleet.devices.map(engine::analyze)
        val mah = analyses.first { it.device.id == "DEV-MAH" }
        val mwh = analyses.first { it.device.id == "DEV-MWH" }

        assertThat(mah.device.energyConsumedMah).isWithin(0.0001).of(1000.0)
        assertThat(mwh.device.energyConsumedMah).isWithin(0.0001).of(1000.0)
        assertThat(mah.relativeDrainPctPerHour)
            .isWithin(0.0001)
            .of(mwh.relativeDrainPctPerHour ?: Double.NaN)
        assertThat(mah.status).isEqualTo(mwh.status)
        assertThat(mah.status).isEqualTo(DeviceStatus.ATENCAO)
    }

    @Test
    fun `absent capacity is indeterminate without NaN or Infinity`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-NO-CAP",
                  "screenOnMinutes": 120,
                  "energyConsumed": 1000,
                  "energyUnit": "mAh",
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        val analysis = engine.analyze(device)

        assertThat(device.batteryCapacityMah).isNull()
        assertThat(device.dataIssues.map { it.code }).contains(DataIssueCode.INVALID_CAPACITY)
        assertThat(analysis.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(analysis.relativeDrainPctPerHour).isNull()
        assertThat(analysis.diagnosis.riskScore.isFinite()).isTrue()
    }

    @Test
    fun `mWh without valid voltage is indeterminate instead of a guessed value`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-NO-VOLT",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 1000,
                  "energyUnit": "mWh",
                  "nominalVoltage": 0,
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        assertThat(device.energyConsumedMah).isNull()
        assertThat(device.dataIssues.map { it.code })
            .contains(DataIssueCode.MISSING_VOLTAGE_FOR_MWH)
        assertThat(engine.analyze(device).status).isEqualTo(DeviceStatus.INDETERMINADO)
    }

    @Test
    fun `unknown energy unit is never treated as mAh`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-UNIT",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 1200,
                  "energyUnit": "Wh",
                  "nominalVoltage": 4.0,
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        assertThat(device.energyConsumedMah).isNull()
        assertThat(device.dataIssues.map { it.code })
            .contains(DataIssueCode.UNKNOWN_ENERGY_UNIT)
    }

    @Test
    fun `duplicate IDs are renamed and flagged`() = runTest {
        val record = """
            {
              "id": "DEV-DUP",
              "batteryCapacityMah": 5000,
              "screenOnMinutes": 120,
              "energyConsumed": 500,
              "energyUnit": "mAh",
              "jankFramesPct": 1,
              "crashCount7d": 0
            }
        """.trimIndent()
        val outcome = parser().parse(jsonArrayDevice(record, record))

        val devices = (outcome as ParseOutcome.Success).fleet.devices
        assertThat(devices.map { it.id }).containsExactly("DEV-DUP", "DEV-DUP-DUP2").inOrder()
        assertThat(devices[1].dataIssues.map { it.code })
            .contains(DataIssueCode.DUPLICATE_ID)
    }

    @Test
    fun `malformed records are skipped without aborting the fleet`() = runTest {
        val outcome = parser().parse(
            """
            {
              "devices": [
                null,
                "not-an-object",
                {
                  "id": "DEV-OK",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 500,
                  "energyUnit": "mAh",
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
              ]
            }
            """.trimIndent(),
        )

        val fleet = (outcome as ParseOutcome.Success).fleet
        assertThat(fleet.devices.map { it.id }).containsExactly("DEV-OK")
        assertThat(fleet.warnings.map { it.code })
            .containsExactly(DataIssueCode.INVALID_RECORD, DataIssueCode.INVALID_RECORD)
    }

    @Test
    fun `defensive field parsing absorbs wrong types and invalid ranges`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": null,
                  "model": " ",
                  "batteryCapacityMah": "5000",
                  "screenOnMinutes": "120,5",
                  "energyConsumed": "NaN",
                  "energyUnit": "MAH",
                  "nominalVoltage": "3.85",
                  "jankFramesPct": "101%",
                  "crashCount7d": -3,
                  "batteryLevelPct": 150,
                  "ramMb": -1,
                  "androidApi": 0,
                  "tier": "banana",
                  "lastSeen": "not-a-date"
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        val codes = device.dataIssues.map { it.code }
        assertThat(device.id).isEqualTo("DEV-UNKNOWN-1")
        assertThat(device.model).isEqualTo("Modelo não identificado")
        assertThat(device.batteryCapacityMah).isEqualTo(5000.0)
        assertThat(device.screenOnMinutes).isEqualTo(120.5)
        assertThat(device.energyConsumedMah).isNull()
        assertThat(device.jankFramesPct).isNull()
        assertThat(device.crashCount7d).isNull()
        assertThat(device.batteryLevelPct).isNull()
        assertThat(device.ramMb).isNull()
        assertThat(device.androidApi).isNull()
        assertThat(device.lastSeenEpochMs).isNull()
        assertThat(codes).containsAtLeast(
            DataIssueCode.MISSING_ID,
            DataIssueCode.MISSING_MODEL,
            DataIssueCode.MISSING_ENERGY,
            DataIssueCode.INVALID_JANK,
            DataIssueCode.INVALID_CRASH_COUNT,
            DataIssueCode.INVALID_BATTERY_LEVEL,
            DataIssueCode.INVALID_RAM,
            DataIssueCode.INVALID_API,
            DataIssueCode.INVALID_TIER,
            DataIssueCode.INVALID_LAST_SEEN,
        )
    }

    @Test
    fun `numeric strings with comma and percent survive sanitization`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-NUMBERS",
                  "batteryCapacityMah": "5000",
                  "screenOnMinutes": " 180 ",
                  "energyConsumed": "1200,5",
                  "energyUnit": "mAh",
                  "jankFramesPct": "7,5%",
                  "crashCount7d": "2",
                  "batteryLevelPct": "42"
                }
                """.trimIndent(),
            ),
        )

        val device = (outcome as ParseOutcome.Success).fleet.devices.single()
        assertThat(device.batteryCapacityMah).isEqualTo(5000.0)
        assertThat(device.screenOnMinutes).isEqualTo(180.0)
        assertThat(device.energyConsumedMah).isEqualTo(1200.5)
        assertThat(device.jankFramesPct).isEqualTo(7.5)
        assertThat(device.crashCount7d).isEqualTo(2)
        assertThat(device.batteryLevelPct).isEqualTo(42)
    }

    @Test
    fun `invalid payloads return a controlled failure`() = runTest {
        assertThat(parser().parse(""))
            .isInstanceOf(ParseOutcome.Failure::class.java)
        assertThat(parser().parse("{ nope"))
            .isInstanceOf(ParseOutcome.Failure::class.java)
        assertThat(parser().parse("""{"devices":[]}"""))
            .isInstanceOf(ParseOutcome.Failure::class.java)
        assertThat(parser().parse("""{"fleetId":"X"}"""))
            .isInstanceOf(ParseOutcome.Failure::class.java)
    }

    @Test
    fun `blank and empty fleet failures are explicit`() = runTest {
        val empty = parser().parse("""{"devices":[]}""") as ParseOutcome.Failure
        val malformed = parser().parse("null") as ParseOutcome.Failure
        val missing = parser().parse("""{"fleetId":"X"}""") as ParseOutcome.Failure

        assertThat(empty.failure).isInstanceOf(FleetFailure.EmptyFleet::class.java)
        assertThat(malformed.failure).isInstanceOf(FleetFailure.MalformedSource::class.java)
        assertThat(missing.failure).isInstanceOf(FleetFailure.MalformedSource::class.java)
    }

    @Test
    fun `root array is accepted and metadata stays null`() = runTest {
        val outcome = parser().parse(
            jsonArrayDevice(
                """
                {
                  "id": "DEV-ARRAY",
                  "batteryCapacityMah": 5000,
                  "screenOnMinutes": 120,
                  "energyConsumed": 500,
                  "energyUnit": "mAh",
                  "jankFramesPct": 1,
                  "crashCount7d": 0
                }
                """.trimIndent(),
            ),
        )

        val fleet = (outcome as ParseOutcome.Success).fleet
        assertThat(fleet.devices).hasSize(1)
        assertThat(fleet.fleetId).isNull()
        assertThat(fleet.exportedAtEpochMs).isNull()
    }

    private fun parser() = CorruptedFileParser(
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowSpecialFloatingPointValues = true
        },
        dispatcher = UnconfinedTestDispatcher(),
    )

    private suspend fun successFleet() =
        (parser().parse(officialJson()) as ParseOutcome.Success).fleet

    private fun jsonArrayDevice(vararg records: String): String =
        records.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun officialJson(): String {
        val candidates = listOf(
            File("src/main/assets/frota.json"),
            File("app/src/main/assets/frota.json"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("frota.json oficial não encontrado a partir de ${File(".").absolutePath}")
    }
}

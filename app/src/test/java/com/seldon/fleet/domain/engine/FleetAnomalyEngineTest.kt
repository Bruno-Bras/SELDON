package com.seldon.fleet.domain.engine

import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.domain.model.DataIssue
import com.seldon.fleet.domain.model.DataIssueCode
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.DeviceTier
import org.junit.Test

class FleetAnomalyEngineTest {

    private val engine = FleetAnomalyEngine()

    @Test
    fun `OK just below every attention threshold`() {
        val analysis = engine.analyze(
            device(energyMah = 69.9, jank = 7.9, crashes = 0),
        )

        assertThat(analysis.status).isEqualTo(DeviceStatus.OK)
        assertThat(analysis.diagnosis.dataQuality).isEqualTo(DataQuality.HIGH)
        assertThat(analysis.relativeDrainPctPerHour).isWithin(0.0001).of(6.99)
    }

    @Test
    fun `ATENCAO exactly at drain threshold`() {
        val analysis = engine.analyze(device(energyMah = 70.0))

        assertThat(analysis.status).isEqualTo(DeviceStatus.ATENCAO)
        assertThat(analysis.relativeDrainPctPerHour).isWithin(0.0001).of(7.0)
    }

    @Test
    fun `ATENCAO just below critical drain threshold`() {
        val analysis = engine.analyze(device(energyMah = 119.9))

        assertThat(analysis.status).isEqualTo(DeviceStatus.ATENCAO)
    }

    @Test
    fun `CRITICO exactly at drain threshold`() {
        val analysis = engine.analyze(device(energyMah = 120.0))

        assertThat(analysis.status).isEqualTo(DeviceStatus.CRITICO)
        assertThat(analysis.relativeDrainPctPerHour).isWithin(0.0001).of(12.0)
    }

    @Test
    fun `values immediately above thresholds promote the status`() {
        assertThat(engine.analyze(device(energyMah = 70.1)).status)
            .isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(energyMah = 120.1)).status)
            .isEqualTo(DeviceStatus.CRITICO)
        assertThat(engine.analyze(device(jank = 8.01)).status)
            .isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(jank = 15.01)).status)
            .isEqualTo(DeviceStatus.CRITICO)
        assertThat(engine.analyze(device(crashes = 2)).status)
            .isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(crashes = 4)).status)
            .isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `crashes use both sides of attention and critical thresholds`() {
        assertThat(engine.analyze(device(crashes = 0)).status).isEqualTo(DeviceStatus.OK)
        assertThat(engine.analyze(device(crashes = 1)).status).isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(crashes = 2)).status).isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(crashes = 3)).status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `jank uses both sides of attention and critical thresholds`() {
        assertThat(engine.analyze(device(jank = 7.99)).status).isEqualTo(DeviceStatus.OK)
        assertThat(engine.analyze(device(jank = 8.0)).status).isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(jank = 14.99)).status).isEqualTo(DeviceStatus.ATENCAO)
        assertThat(engine.analyze(device(jank = 15.0)).status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `any critical trigger wins over attention triggers`() {
        val analysis = engine.analyze(
            device(energyMah = 70.0, jank = 15.0, crashes = 0),
        )

        assertThat(analysis.status).isEqualTo(DeviceStatus.CRITICO)
        assertThat(analysis.diagnosis.contributions.map { it.factor })
            .contains(com.seldon.fleet.domain.model.RiskFactor.JANK)
    }

    @Test
    fun `indeterminate wins over critical signals when capacity is zero`() {
        val analysis = engine.analyze(
            device(
                capacityMah = 0.0,
                energyMah = 500.0,
                jank = 50.0,
                crashes = 9,
                issues = listOf(
                    DataIssue(
                        code = DataIssueCode.INVALID_CAPACITY,
                        field = "batteryCapacityMah",
                        rawValue = "0",
                        message = "Capacidade de bateria ausente ou inválida.",
                    ),
                ),
            ),
        )

        assertThat(analysis.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(analysis.diagnosis.dataQuality).isEqualTo(DataQuality.VOID)
        assertThat(analysis.diagnosis.riskScore).isEqualTo(0.0)
        assertThat(analysis.diagnosis.headline).contains("Capacidade")
    }

    @Test
    fun `indeterminate when screen time is zero or negative`() {
        assertThat(engine.analyze(device(screenMinutes = 0.0)).status)
            .isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(engine.analyze(device(screenMinutes = -1.0)).status)
            .isEqualTo(DeviceStatus.INDETERMINADO)
    }

    @Test
    fun `indeterminate when energy was not normalized`() {
        val analysis = engine.analyze(
            device(
                energyMah = null,
                issues = listOf(
                    DataIssue(
                        code = DataIssueCode.MISSING_VOLTAGE_FOR_MWH,
                        field = "nominalVoltage",
                        rawValue = "null",
                        message = "Energia em mWh sem tensão nominal válida.",
                    ),
                ),
            ),
        )

        assertThat(analysis.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(analysis.diagnosis.headline).contains("tensão nominal")
    }

    @Test
    fun `indeterminate when jank or crash count is missing`() {
        assertThat(engine.analyze(device(jank = null)).status)
            .isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(engine.analyze(device(crashes = null)).status)
            .isEqualTo(DeviceStatus.INDETERMINADO)
    }

    @Test
    fun `risk score is bounded and floored by official status`() {
        val ok = engine.analyze(device(energyMah = 0.0, jank = 0.0, crashes = 0))
        val attention = engine.analyze(device(energyMah = 70.0))
        val critical = engine.analyze(device(energyMah = 120.0))

        assertThat(ok.diagnosis.riskScore).isEqualTo(0.0)
        assertThat(attention.diagnosis.riskScore).isAtLeast(40.0)
        assertThat(critical.diagnosis.riskScore).isAtLeast(75.0)
        listOf(ok, attention, critical).forEach {
            assertThat(it.diagnosis.riskScore.isFinite()).isTrue()
            assertThat(it.diagnosis.riskScore).isAtLeast(0.0)
            assertThat(it.diagnosis.riskScore).isAtMost(100.0)
        }
    }

    @Test
    fun `non finite values never crash the engine`() {
        val analysis = engine.analyze(
            device(
                capacityMah = Double.NaN,
                screenMinutes = Double.POSITIVE_INFINITY,
                energyMah = Double.NEGATIVE_INFINITY,
                jank = Double.NaN,
            ),
        )

        assertThat(analysis.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(analysis.diagnosis.riskScore.isFinite()).isTrue()
    }

    private fun device(
        id: String = "DEV-0001",
        capacityMah: Double? = 1000.0,
        screenMinutes: Double? = 60.0,
        energyMah: Double? = 10.0,
        jank: Double? = 0.0,
        crashes: Int? = 0,
        issues: List<DataIssue> = emptyList(),
    ) = Device(
        id = id,
        model = "Test Device",
        soc = "Test SoC",
        tier = DeviceTier.MID,
        ramMb = 4096,
        androidApi = 34,
        batteryCapacityMah = capacityMah,
        batteryLevelPct = 50,
        screenOnMinutes = screenMinutes,
        energyConsumedMah = energyMah,
        nominalVoltage = 3.85,
        jankFramesPct = jank,
        crashCount7d = crashes,
        lastSeenEpochMs = 1_760_000_000_000,
        dataIssues = issues,
    )
}

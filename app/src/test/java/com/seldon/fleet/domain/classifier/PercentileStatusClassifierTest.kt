package com.seldon.fleet.domain.classifier

import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.DeviceTier
import org.junit.Test

class PercentileStatusClassifierTest {

    private val classifier = PercentileStatusClassifier()

    @Test
    fun `invalid device is indeterminate without a score`() {
        val device = device(id = "DEV-INVALID", capacityMah = 0.0)
        val fleet = listOf(device, device(id = "DEV-1", energyMah = 50.0))

        val assessment = classifier.evaluate(device, fleet)

        assertThat(assessment.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(assessment.score).isNull()
        assertThat(assessment.peerCount).isEqualTo(0)
    }

    @Test
    fun `a single valid device has no peer distribution`() {
        val device = device(id = "DEV-ONLY")

        val assessment = classifier.evaluate(device, listOf(device))

        assertThat(assessment.status).isEqualTo(DeviceStatus.INDETERMINADO)
        assertThat(assessment.score).isNull()
        assertThat(assessment.peerCount).isEqualTo(0)
    }

    @Test
    fun `equal peers produce a midrank of one half and no alert`() {
        val fleet = (1..10).map { index -> device(id = "DEV-$index") }

        val assessment = classifier.evaluate(fleet.first(), fleet)

        assertThat(assessment.status).isEqualTo(DeviceStatus.OK)
        assertThat(assessment.drainPercentile).isWithin(0.0001).of(0.5)
        assertThat(assessment.jankPercentile).isWithin(0.0001).of(0.5)
        assertThat(assessment.crashPercentile).isWithin(0.0001).of(0.5)
        assertThat(assessment.score).isWithin(0.0001).of(0.5)
        assertThat(assessment.peerCount).isEqualTo(9)
    }

    @Test
    fun `highest drain is critical even with normal jank and crashes`() {
        val outlier = device(id = "DEV-OUTLIER", energyMah = 200.0)
        val fleet = listOf(outlier) + (1..9).map { index -> device(id = "DEV-$index") }

        val assessment = classifier.evaluate(outlier, fleet)

        assertThat(assessment.drainPercentile).isWithin(0.0001).of(1.0)
        assertThat(assessment.score).isWithin(0.0001).of(1.0)
        assertThat(assessment.status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `second highest among ten peers lands exactly on P90`() {
        val boundary = device(id = "DEV-BOUNDARY", energyMah = 100.0)
        val fleet = listOf(
            boundary,
            device(id = "DEV-TOP", energyMah = 200.0),
        ) + (1..9).map { index -> device(id = "DEV-LOW-$index", energyMah = 50.0) }

        val assessment = classifier.evaluate(boundary, fleet)

        assertThat(assessment.drainPercentile).isWithin(0.0001).of(0.90)
        assertThat(assessment.status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `sixth rank among ten peers lands exactly on P60`() {
        val boundary = device(id = "DEV-BOUNDARY", energyMah = 50.0)
        val fleet = listOf(boundary) +
            (1..6).map { index -> device(id = "DEV-LOW-$index", energyMah = 10.0) } +
            (1..4).map { index -> device(id = "DEV-HIGH-$index", energyMah = 90.0) }

        val assessment = classifier.evaluate(boundary, fleet)

        assertThat(assessment.drainPercentile).isWithin(0.0001).of(0.60)
        assertThat(assessment.status).isEqualTo(DeviceStatus.ATENCAO)
    }

    @Test
    fun `tied values use midrank instead of promoting both`() {
        val target = device(id = "DEV-TARGET", energyMah = 100.0)
        val fleet = listOf(
            target,
            device(id = "DEV-TIE", energyMah = 100.0),
            device(id = "DEV-LOW", energyMah = 10.0),
        )

        val assessment = classifier.evaluate(target, fleet)

        assertThat(assessment.drainPercentile).isWithin(0.0001).of(0.75)
        assertThat(assessment.status).isEqualTo(DeviceStatus.ATENCAO)
    }

    @Test
    fun `invalid peers are excluded from the percentile distribution`() {
        val target = device(id = "DEV-TARGET", energyMah = 100.0)
        val fleet = listOf(
            target,
            device(id = "DEV-VALID-1"),
            device(id = "DEV-VALID-2", energyMah = 10.0),
            device(id = "DEV-INVALID", capacityMah = 0.0),
        )

        val assessment = classifier.evaluate(target, fleet)

        assertThat(assessment.peerCount).isEqualTo(2)
        assertThat(assessment.drainPercentile).isWithin(0.0001).of(1.0)
    }

    @Test
    fun `the worst relative signal drives the score`() {
        val target = device(id = "DEV-JANK", energyMah = 50.0, jank = 10.0)
        val fleet = listOf(target) + (1..9).map { index ->
            device(id = "DEV-$index", energyMah = 50.0, jank = 1.0)
        }

        val assessment = classifier.evaluate(target, fleet)

        assertThat(assessment.drainPercentile).isWithin(0.0001).of(0.5)
        assertThat(assessment.jankPercentile).isWithin(0.0001).of(1.0)
        assertThat(assessment.score).isWithin(0.0001).of(1.0)
        assertThat(assessment.status).isEqualTo(DeviceStatus.CRITICO)
    }

    @Test
    fun `classify delegates to the same assessment`() {
        val target = device(id = "DEV-TARGET", energyMah = 200.0)
        val fleet = listOf(target) + (1..9).map { index -> device(id = "DEV-$index") }

        assertThat(classifier.classify(target, fleet))
            .isEqualTo(classifier.evaluate(target, fleet).status)
    }

    private fun device(
        id: String,
        capacityMah: Double = 1000.0,
        energyMah: Double = 50.0,
        jank: Double = 1.0,
        crashes: Int = 0,
    ) = Device(
        id = id,
        model = "Test Device",
        soc = "Test SoC",
        tier = DeviceTier.MID,
        ramMb = 4096,
        androidApi = 34,
        batteryCapacityMah = capacityMah,
        batteryLevelPct = 50,
        screenOnMinutes = 60.0,
        energyConsumedMah = energyMah,
        nominalVoltage = 3.85,
        jankFramesPct = jank,
        crashCount7d = crashes,
        lastSeenEpochMs = 1_760_000_000_000,
    )
}

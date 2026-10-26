package com.seldon.fleet.domain.comparison

import com.seldon.fleet.domain.analysis.DeviceMetrics
import com.seldon.fleet.domain.classifier.DeviceStatusClassifier
import com.seldon.fleet.domain.classifier.PercentileStatusClassifier
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus

data class ClassificationComparisonRow(
    val deviceId: String,
    val referenceStatus: DeviceStatus,
    val candidateStatus: DeviceStatus,
    val relativeDrainPctPerHour: Double?,
    val jankFramesPct: Double?,
    val crashCount7d: Int?,
    val candidateScore: Double?,
    val candidateDrainPercentile: Double?,
    val candidateJankPercentile: Double?,
    val candidateCrashPercentile: Double?,
    val candidatePeerCount: Int,
) {
    val agrees: Boolean get() = referenceStatus == candidateStatus
}

data class StatusConfusion(
    val reference: DeviceStatus,
    val candidate: DeviceStatus,
    val count: Int,
)

data class ClassificationComparison(
    val rows: List<ClassificationComparisonRow>,
    val referenceCounts: Map<DeviceStatus, Int>,
    val candidateCounts: Map<DeviceStatus, Int>,
    val agreements: Int,
    val confusion: List<StatusConfusion>,
    val disagreements: List<ClassificationComparisonRow>,
) {
    val total: Int get() = rows.size
}

/**
 * Executa as duas implementações sobre a mesma frota. O resultado é apenas
 * analítico: nenhuma dessas linhas entra no `FleetTelemetry` ou na UI.
 */
class ClassificationComparisonService(
    private val referenceClassifier: DeviceStatusClassifier,
    private val candidateClassifier: PercentileStatusClassifier,
) {
    fun compare(devices: List<Device>): ClassificationComparison {
        val rows = devices.map { device ->
            val metrics = DeviceMetrics.from(device)
            val referenceStatus = referenceClassifier.classify(device, devices)
            val assessment = candidateClassifier.evaluate(device, devices)

            ClassificationComparisonRow(
                deviceId = device.id,
                referenceStatus = referenceStatus,
                candidateStatus = assessment.status,
                relativeDrainPctPerHour = metrics?.relativeDrainPctPerHour,
                jankFramesPct = metrics?.jankFramesPct,
                crashCount7d = metrics?.crashCount7d,
                candidateScore = assessment.score,
                candidateDrainPercentile = assessment.drainPercentile,
                candidateJankPercentile = assessment.jankPercentile,
                candidateCrashPercentile = assessment.crashPercentile,
                candidatePeerCount = assessment.peerCount,
            )
        }

        val disagreements = rows.filterNot { it.agrees }
        return ClassificationComparison(
            rows = rows,
            referenceCounts = counts(rows, ClassificationComparisonRow::referenceStatus),
            candidateCounts = counts(rows, ClassificationComparisonRow::candidateStatus),
            agreements = rows.size - disagreements.size,
            confusion = DeviceStatus.entries.flatMap { reference ->
                DeviceStatus.entries.map { candidate ->
                    StatusConfusion(
                        reference = reference,
                        candidate = candidate,
                        count = rows.count {
                            it.referenceStatus == reference && it.candidateStatus == candidate
                        },
                    )
                }
            },
            disagreements = disagreements,
        )
    }

    private fun counts(
        rows: List<ClassificationComparisonRow>,
        status: (ClassificationComparisonRow) -> DeviceStatus,
    ): Map<DeviceStatus, Int> = DeviceStatus.entries.associateWith { expected ->
        rows.count { status(it) == expected }
    }
}

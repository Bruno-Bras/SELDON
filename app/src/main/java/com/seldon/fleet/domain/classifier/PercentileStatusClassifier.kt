package com.seldon.fleet.domain.classifier

import com.seldon.fleet.domain.analysis.DeviceMetrics
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus
import javax.inject.Inject

/**
 * Segunda implementação da classificação, usada apenas na avaliação da Trilha A.
 *
 * O score é o máximo dos ranks percentuais dos três sinais (dreno relativo,
 * jank e falhas), calculados entre os dispositivos válidos da frota, excluindo
 * o próprio dispositivo. O rank usa *midrank* para que empates não sejam
 * arbitrariamente promovidos.
 *
 * Os cortes P90/P60 são de percentil, não dos valores físicos oficiais. Esta
 * implementação é deliberadamente independente dos limiares absolutos da
 * referência e não é injetada no fluxo de produção.
 */
class PercentileStatusClassifier @Inject constructor() : DeviceStatusClassifier {

    override fun classify(device: Device, fleet: List<Device>): DeviceStatus =
        evaluate(device, fleet).status

    fun evaluate(device: Device, fleet: List<Device>): PercentileAssessment {
        val metrics = DeviceMetrics.from(device)
            ?: return indeterminate(peerCount = 0)

        val peers = fleet.asSequence()
            .filter { it.id != device.id }
            .mapNotNull { DeviceMetrics.from(it) }
            .toList()

        if (peers.isEmpty()) return indeterminate(peerCount = 0)

        val drainPercentile = percentileRank(
            value = metrics.relativeDrainPctPerHour,
            peers = peers.map { it.relativeDrainPctPerHour },
        )
        val jankPercentile = percentileRank(
            value = metrics.jankFramesPct,
            peers = peers.map { it.jankFramesPct },
        )
        val crashPercentile = percentileRank(
            value = metrics.crashCount7d.toDouble(),
            peers = peers.map { it.crashCount7d.toDouble() },
        )

        val score = maxOf(drainPercentile, jankPercentile, crashPercentile)
        val status = when {
            score >= CRITICAL_PERCENTILE -> DeviceStatus.CRITICO
            score >= ATTENTION_PERCENTILE -> DeviceStatus.ATENCAO
            else -> DeviceStatus.OK
        }

        return PercentileAssessment(
            status = status,
            score = score,
            drainPercentile = drainPercentile,
            jankPercentile = jankPercentile,
            crashPercentile = crashPercentile,
            peerCount = peers.size,
        )
    }

    private fun indeterminate(peerCount: Int) = PercentileAssessment(
        status = DeviceStatus.INDETERMINADO,
        score = null,
        drainPercentile = null,
        jankPercentile = null,
        crashPercentile = null,
        peerCount = peerCount,
    )

    /** Rank empírico em 0..1; maior significa mais anômalo dentro da frota. */
    private fun percentileRank(value: Double, peers: List<Double>): Double {
        val below = peers.count { it < value }
        val equal = peers.count { it == value }
        return (below + equal * 0.5) / peers.size
    }

    companion object {
        const val CRITICAL_PERCENTILE = 0.90
        const val ATTENTION_PERCENTILE = 0.60
    }
}

data class PercentileAssessment(
    val status: DeviceStatus,
    val score: Double?,
    val drainPercentile: Double?,
    val jankPercentile: Double?,
    val crashPercentile: Double?,
    val peerCount: Int,
)

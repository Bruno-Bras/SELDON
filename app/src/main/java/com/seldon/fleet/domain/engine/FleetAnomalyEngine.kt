package com.seldon.fleet.domain.engine

import com.seldon.fleet.domain.analysis.DeviceMetrics
import com.seldon.fleet.domain.classifier.DeviceStatusClassifier
import com.seldon.fleet.domain.model.DataIssueCode
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceAnalysis
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.DiagnosisResult
import com.seldon.fleet.domain.model.RiskContribution
import com.seldon.fleet.domain.model.RiskFactor
import java.util.Locale
import javax.inject.Inject

/**
 * Regra de negócio central. Recebe apenas o modelo de domínio sanitizado e
 * devolve status + diagnóstico. Nenhuma dependência de Android, JSON ou Context.
 *
 * A classificação segue a precedência oficial:
 * INDETERMINADO > CRITICO > ATENCAO > OK.
 *
 * O índice de risco 0..100 é complementar: ele enriquece a UI e o detalhe, mas
 * nunca substitui a decisão binária dos limiares oficiais.
 */
class FleetAnomalyEngine @Inject constructor() : DeviceStatusClassifier {

    override fun classify(device: Device, fleet: List<Device>): DeviceStatus =
        analyze(device).status

    fun analyze(device: Device): DeviceAnalysis {
        val indeterminateReason = indeterminateReason(device)
        if (indeterminateReason != null) {
            return indeterminate(device, indeterminateReason)
        }

        val metrics = DeviceMetrics.from(device)
            ?: return indeterminate(device, "dados insuficientes para calcular as derivadas")
        val capacity = requireNotNull(device.batteryCapacityMah)
        val screenMinutes = requireNotNull(device.screenOnMinutes)
        val jank = metrics.jankFramesPct
        val crashes = metrics.crashCount7d

        val absoluteDrain = metrics.absoluteDrainMahPerHour
        val relativeDrain = metrics.relativeDrainPctPerHour

        val criticalTriggers = buildList {
            if (relativeDrain >= CRITICAL_DRAIN) add(Trigger(Signal.DRAIN, relativeDrain, CRITICAL_DRAIN))
            if (crashes >= CRITICAL_CRASHES) add(Trigger(Signal.CRASHES, crashes.toDouble(), CRITICAL_CRASHES.toDouble()))
            if (jank >= CRITICAL_JANK) add(Trigger(Signal.JANK, jank, CRITICAL_JANK))
        }
        val attentionTriggers = buildList {
            if (relativeDrain >= ATTENTION_DRAIN) add(Trigger(Signal.DRAIN, relativeDrain, ATTENTION_DRAIN))
            if (crashes >= ATTENTION_CRASHES) add(Trigger(Signal.CRASHES, crashes.toDouble(), ATTENTION_CRASHES.toDouble()))
            if (jank >= ATTENTION_JANK) add(Trigger(Signal.JANK, jank, ATTENTION_JANK))
        }

        val status = when {
            criticalTriggers.isNotEmpty() -> DeviceStatus.CRITICO
            attentionTriggers.isNotEmpty() -> DeviceStatus.ATENCAO
            else -> DeviceStatus.OK
        }

        val triggers = if (status == DeviceStatus.CRITICO) criticalTriggers else attentionTriggers

        val drainRatio = (relativeDrain / CRITICAL_DRAIN).coerceIn(0.0, 1.0)
        val crashRatio = (crashes.toDouble() / CRITICAL_CRASHES).coerceIn(0.0, 1.0)
        val jankRatio = (jank / CRITICAL_JANK).coerceIn(0.0, 1.0)

        val contributions = listOf(
            RiskContribution(RiskFactor.DRAIN, drainRatio, drainRatio * 55.0),
            RiskContribution(RiskFactor.CRASHES, crashRatio, crashRatio * 30.0),
            RiskContribution(RiskFactor.JANK, jankRatio, jankRatio * 15.0),
        ).filter { it.points > 0.0 }
            .sortedByDescending { it.points }

        val weightedScore = contributions.sumOf { it.points }.coerceIn(0.0, 100.0)
        val score = when (status) {
            DeviceStatus.CRITICO -> weightedScore.coerceAtLeast(75.0)
            DeviceStatus.ATENCAO -> weightedScore.coerceAtLeast(40.0)
            else -> weightedScore
        }.coerceIn(0.0, 100.0)

        val primary = triggers.maxByOrNull { it.relativeExceedance }?.signal
        val primaryFactor = when (primary) {
            Signal.DRAIN -> RiskFactor.DRAIN
            Signal.CRASHES -> RiskFactor.CRASHES
            Signal.JANK -> RiskFactor.JANK
            null -> RiskFactor.NONE
        }
        val primaryTrigger = triggers.maxByOrNull { it.relativeExceedance }
        val dataQuality = if (device.dataIssues.isEmpty()) DataQuality.HIGH else DataQuality.DEGRADED

        val headline = when (status) {
            DeviceStatus.CRITICO -> "Classificação crítica: ${triggers.joinToString("; ") { it.description() }}."
            DeviceStatus.ATENCAO -> "Classificação em atenção: ${triggers.joinToString("; ") { it.description() }}."
            else -> "Nenhum limiar de atenção foi atingido: dreno relativo de ${decimal(relativeDrain)} %/h, " +
                "$crashes falha(s) em 7 dias e ${decimal(jank)}% de quadros perdidos."
        }

        val detail = buildString {
            append("Consumo normalizado de ${decimal(absoluteDrain)} mAh/h ")
            append("(${decimal(relativeDrain)} %/h) sobre capacidade de ${decimal(capacity, 0)} mAh. ")
            append("Tela ligada por ${decimal(screenMinutes, 0)} min; ")
            append("${decimal(jank)}% de quadros perdidos; ")
            append("$crashes falha(s) em 7 dias.")
        }

        return DeviceAnalysis(
            device = device,
            diagnosis = DiagnosisResult(
                status = status,
                riskScore = score,
                primaryFactor = primaryFactor,
                contributions = contributions,
                dataQuality = dataQuality,
                headline = headline,
                detail = detail,
                observedValue = primaryTrigger?.observedLabel(),
                thresholdValue = primaryTrigger?.thresholdLabel(),
            ),
            absoluteDrainMahPerHour = absoluteDrain,
            relativeDrainPctPerHour = relativeDrain,
        )
    }

    private fun indeterminateReason(device: Device): String? {
        return when {
            !device.batteryCapacityMah.isFinitePositive() ->
                issueMessageOr(device, DataIssueCode.INVALID_CAPACITY) {
                    "capacidade de bateria ausente, zero ou negativa"
                }
            !device.screenOnMinutes.isFinitePositive() ->
                issueMessageOr(device, DataIssueCode.INVALID_SCREEN_ON) {
                    "tempo de tela ausente, zero ou negativo"
                }
            !device.energyConsumedMah.isFiniteNonNegative() ->
                issueMessageOr(device, DataIssueCode.MISSING_VOLTAGE_FOR_MWH) {
                    issueMessageOr(device, DataIssueCode.UNKNOWN_ENERGY_UNIT) {
                        issueMessageOr(device, DataIssueCode.MISSING_ENERGY) {
                            "energia consumida não pôde ser normalizada para mAh"
                        }
                    }
                }
            !device.jankFramesPct.isFiniteNumber() ->
                issueMessageOr(device, DataIssueCode.INVALID_JANK) {
                    "percentual de quadros perdidos ausente ou inválido"
                }
            device.crashCount7d == null ->
                issueMessageOr(device, DataIssueCode.INVALID_CRASH_COUNT) {
                    "contagem de falhas em 7 dias ausente ou inválida"
                }
            else -> null
        }
    }

    private fun Double?.isFinitePositive(): Boolean = this != null && isFinite() && this > 0.0

    private fun Double?.isFiniteNonNegative(): Boolean = this != null && isFinite() && this >= 0.0

    private fun Double?.isFiniteNumber(): Boolean = this != null && isFinite()

    private fun indeterminate(device: Device, reason: String): DeviceAnalysis {
        val cleanReason = reason.trimEnd('.')
        return DeviceAnalysis(
            device = device,
            diagnosis = DiagnosisResult(
                status = DeviceStatus.INDETERMINADO,
                riskScore = 0.0,
                primaryFactor = RiskFactor.DATA_INTEGRITY,
                contributions = emptyList(),
                dataQuality = DataQuality.VOID,
                headline = "Classificação indeterminada: $cleanReason.",
                detail = "O dreno relativo não pôde ser calculado com segurança. " +
                    "O registro foi mantido na frota como INDETERMINADO para não mascarar a falha de dados. " +
                    "Dados disponíveis: capacidade=${device.batteryCapacityMah?.let { decimal(it, 0) } ?: "N/D"} mAh, " +
                    "tela=${device.screenOnMinutes?.let { decimal(it, 0) } ?: "N/D"} min, " +
                    "energia=${device.energyConsumedMah?.let { decimal(it) } ?: "N/D"} mAh, " +
                    "quadros=${device.jankFramesPct?.let { decimal(it) } ?: "N/D"}%, " +
                    "falhas=${device.crashCount7d?.toString() ?: "N/D"}.",
                observedValue = null,
                thresholdValue = null,
            ),
            absoluteDrainMahPerHour = null,
            relativeDrainPctPerHour = null,
        )
    }

    private fun issueMessageOr(device: Device, code: DataIssueCode, fallback: () -> String): String {
        return device.dataIssues.firstOrNull { it.code == code }?.message?.trim()?.trimEnd('.')
            ?: fallback()
    }

    private fun decimal(value: Double, digits: Int = 1): String =
        String.format(Locale.forLanguageTag("pt-BR"), "%.${digits}f", value)

    private enum class Signal {
        DRAIN,
        CRASHES,
        JANK,
    }

    private data class Trigger(
        val signal: Signal,
        val observed: Double,
        val threshold: Double,
    ) {
        val relativeExceedance: Double get() = observed / threshold

        fun description(): String = when (signal) {
            Signal.DRAIN -> "dreno relativo de ${format(observed)} %/h (limiar ${format(threshold, 0)} %/h)"
            Signal.CRASHES -> "${observed.toInt()} falha(s) em 7 dias (limiar ${threshold.toInt()})"
            Signal.JANK -> "${format(observed)}% de quadros perdidos (limiar ${format(threshold, 0)}%)"
        }

        fun observedLabel(): String = when (signal) {
            Signal.DRAIN -> "${format(observed)} %/h"
            Signal.CRASHES -> "${observed.toInt()} falha(s)"
            Signal.JANK -> "${format(observed)}%"
        }

        fun thresholdLabel(): String = when (signal) {
            Signal.DRAIN -> "${format(threshold, 0)} %/h"
            Signal.CRASHES -> "${threshold.toInt()} falha(s)"
            Signal.JANK -> "${format(threshold, 0)}%"
        }

        private fun format(value: Double, digits: Int = 1): String =
            String.format(Locale.forLanguageTag("pt-BR"), "%.${digits}f", value)
    }

    companion object {
        const val CRITICAL_DRAIN = 12.0
        const val CRITICAL_CRASHES = 3
        const val CRITICAL_JANK = 15.0
        const val ATTENTION_DRAIN = 7.0
        const val ATTENTION_CRASHES = 1
        const val ATTENTION_JANK = 8.0
    }
}

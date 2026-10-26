package com.seldon.fleet.domain.model

enum class RiskFactor {
    DRAIN,
    CRASHES,
    JANK,
    DATA_INTEGRITY,
    NONE,
}

enum class DataQuality {
    HIGH,
    DEGRADED,
    VOID,
}

data class RiskContribution(
    val factor: RiskFactor,
    /** Intensidade normalizada (0..1) em relação ao limiar crítico. */
    val normalized: Double,
    /** Pontos que o fator adiciona ao índice composto (0..100). */
    val points: Double,
)

/**
 * Resultado rico da engine: o status oficial do desafio, o índice de risco
 * complementar, o fator primário tipado e a justificativa legível.
 */
data class DiagnosisResult(
    val status: DeviceStatus,
    val riskScore: Double,
    val primaryFactor: RiskFactor,
    val contributions: List<RiskContribution>,
    val dataQuality: DataQuality,
    val headline: String,
    val detail: String,
    val observedValue: String?,
    val thresholdValue: String?,
)

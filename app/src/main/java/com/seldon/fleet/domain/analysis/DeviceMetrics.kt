package com.seldon.fleet.domain.analysis

import com.seldon.fleet.domain.model.Device

/**
 * Derivadas físicas comuns às duas implementações de classificação.
 *
 * A validação aqui é intencionalmente a mesma usada pela regra de referência:
 * um registro só produz métricas quando os divisores e os sinais necessários
 * são finitos e têm faixa utilizável. A mensagem de `INDETERMINADO` continua
 * sendo responsabilidade da engine, que conhece os códigos de problema do
 * parser.
 */
data class DeviceMetrics(
    val absoluteDrainMahPerHour: Double,
    val relativeDrainPctPerHour: Double,
    val jankFramesPct: Double,
    val crashCount7d: Int,
) {
    companion object {
        fun from(device: Device): DeviceMetrics? {
            val capacity = device.batteryCapacityMah
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?: return null
            val screenMinutes = device.screenOnMinutes
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?: return null
            val energyMah = device.energyConsumedMah
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: return null
            val jank = device.jankFramesPct
                ?.takeIf { it.isFinite() }
                ?: return null
            val crashes = device.crashCount7d ?: return null

            val absoluteDrain = energyMah / (screenMinutes / 60.0)
            val relativeDrain = (absoluteDrain / capacity) * 100.0
            if (!absoluteDrain.isFinite() || !relativeDrain.isFinite()) return null

            return DeviceMetrics(
                absoluteDrainMahPerHour = absoluteDrain,
                relativeDrainPctPerHour = relativeDrain,
                jankFramesPct = jank,
                crashCount7d = crashes,
            )
        }
    }
}

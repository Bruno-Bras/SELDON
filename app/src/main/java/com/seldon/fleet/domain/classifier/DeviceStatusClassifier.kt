package com.seldon.fleet.domain.classifier

import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus

/**
 * Contrato mínimo para classificar um dispositivo.
 *
 * A lista [fleet] permite implementações orientadas por dados, que precisam
 * comparar o dispositivo com seus pares. Implementações baseadas em limiares
 * absolutos podem ignorá-la. A própria lista pode conter o dispositivo; cada
 * implementação decide como tratá-lo.
 */
fun interface DeviceStatusClassifier {
    fun classify(device: Device, fleet: List<Device>): DeviceStatus
}

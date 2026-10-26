package com.seldon.fleet.domain.model

data class DeviceAnalysis(
    val device: Device,
    val diagnosis: DiagnosisResult,
    val absoluteDrainMahPerHour: Double?,
    val relativeDrainPctPerHour: Double?,
) {
    val status: DeviceStatus get() = diagnosis.status
}

package com.seldon.fleet.domain.model

sealed class FleetFailure(
    val code: String,
    open val technicalDetail: String,
) {
    data class AssetUnavailable(override val technicalDetail: String) :
        FleetFailure("ASSET_UNAVAILABLE", technicalDetail)

    data class MalformedSource(override val technicalDetail: String) :
        FleetFailure("MALFORMED_SOURCE", technicalDetail)

    data class EmptyFleet(override val technicalDetail: String) :
        FleetFailure("EMPTY_FLEET", technicalDetail)

    data class PreferencesUnavailable(override val technicalDetail: String) :
        FleetFailure("PREFERENCES_UNAVAILABLE", technicalDetail)

    data class DeviceNotFound(val deviceId: String) :
        FleetFailure("DEVICE_NOT_FOUND", "Dispositivo $deviceId não encontrado na frota.")

    data class Unexpected(override val technicalDetail: String) :
        FleetFailure("UNEXPECTED", technicalDetail)
}

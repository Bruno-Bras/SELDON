package com.seldon.fleet.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object FleetRoute

@Serializable
data class DeviceDetailRoute(val deviceId: String)

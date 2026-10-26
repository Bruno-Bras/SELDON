package com.seldon.fleet.domain.model

data class FleetSnapshot(
    val fleet: Fleet,
    val favorites: Set<String>,
)

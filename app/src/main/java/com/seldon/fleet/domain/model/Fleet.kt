package com.seldon.fleet.domain.model

data class Fleet(
    val fleetId: String?,
    val exportedAtEpochMs: Long?,
    val devices: List<Device>,
    val warnings: List<DataIssue> = emptyList(),
)

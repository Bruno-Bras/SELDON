package com.seldon.fleet.domain.model

enum class DataIssueCode {
    MISSING_ID,
    DUPLICATE_ID,
    MISSING_MODEL,
    INVALID_CAPACITY,
    INVALID_SCREEN_ON,
    MISSING_ENERGY,
    UNKNOWN_ENERGY_UNIT,
    MISSING_VOLTAGE_FOR_MWH,
    INVALID_VOLTAGE,
    INVALID_JANK,
    INVALID_CRASH_COUNT,
    INVALID_LAST_SEEN,
    INVALID_BATTERY_LEVEL,
    INVALID_RAM,
    INVALID_API,
    INVALID_TIER,
    INVALID_RECORD,
}

data class DataIssue(
    val code: DataIssueCode,
    val field: String,
    val rawValue: String?,
    val message: String,
)

package com.seldon.fleet.data.parser

import com.seldon.fleet.di.DefaultDispatcher
import com.seldon.fleet.domain.model.DataIssue
import com.seldon.fleet.domain.model.DataIssueCode
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceTier
import com.seldon.fleet.domain.model.Fleet
import com.seldon.fleet.domain.model.FleetFailure
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface ParseOutcome {
    data class Success(val fleet: Fleet) : ParseOutcome
    data class Failure(val failure: FleetFailure) : ParseOutcome
}

/**
 * Parser defensivo: cada campo é lido de forma isolada, com fallback explícito.
 * Nenhuma exceção de parse chega à UI; no máximo, um FleetFailure tratado.
 */
@Singleton
class CorruptedFileParser @Inject constructor(
    private val json: Json,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend fun parse(raw: String): ParseOutcome = withContext(dispatcher) {
        try {
            parseInternal(raw)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            ParseOutcome.Failure(
                FleetFailure.MalformedSource(
                    "Não foi possível interpretar o arquivo: ${error.message ?: error::class.java.simpleName}",
                ),
            )
        }
    }

    private fun parseInternal(raw: String): ParseOutcome {
        if (raw.isBlank()) {
            return ParseOutcome.Failure(FleetFailure.MalformedSource("O arquivo de frota está vazio."))
        }

        val root = try {
            json.parseToJsonElement(raw)
        } catch (error: Exception) {
            return ParseOutcome.Failure(
                FleetFailure.MalformedSource(
                    "JSON inválido: ${error.message ?: "estrutura não reconhecida"}",
                ),
            )
        }

        val rootObject = root as? JsonObject
        val devicesArray: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> root["devices"] as? JsonArray
                ?: return ParseOutcome.Failure(
                    FleetFailure.MalformedSource("O objeto raiz não contém o array 'devices'."),
                )
            else -> return ParseOutcome.Failure(
                FleetFailure.MalformedSource("A raiz do JSON deve ser um objeto ou um array."),
            )
        }

        if (devicesArray.isEmpty()) {
            return ParseOutcome.Failure(FleetFailure.EmptyFleet("O arquivo não contém dispositivos."))
        }

        val fleetWarnings = mutableListOf<DataIssue>()
        val seenIds = mutableSetOf<String>()
        val devices = mutableListOf<Device>()

        devicesArray.forEachIndexed { index, element ->
            val record = element as? JsonObject
            if (record == null) {
                fleetWarnings += DataIssue(
                    code = DataIssueCode.INVALID_RECORD,
                    field = "devices[$index]",
                    rawValue = element.toString().take(48),
                    message = "Registro ${index + 1} não é um objeto JSON e foi ignorado.",
                )
                return@forEachIndexed
            }

            val parsed = parseDevice(record, index)
            val uniqueDevice: Device = if (seenIds.add(parsed.id)) {
                parsed
            } else {
                var candidate = "${parsed.id}-DUP${index + 1}"
                while (!seenIds.add(candidate)) {
                    candidate = "$candidate-X"
                }
                val duplicateIssue = DataIssue(
                    code = DataIssueCode.DUPLICATE_ID,
                    field = "id",
                    rawValue = parsed.id,
                    message = "Identificador duplicado (${parsed.id}); o registro foi renomeado para $candidate.",
                )
                parsed.copy(id = candidate, dataIssues = parsed.dataIssues + duplicateIssue)
            }
            devices += uniqueDevice
        }

        if (devices.isEmpty()) {
            return ParseOutcome.Failure(
                FleetFailure.EmptyFleet("Nenhum registro utilizável foi encontrado no arquivo."),
            )
        }

        return ParseOutcome.Success(
            Fleet(
                fleetId = rootObject?.stringOrNull("fleetId"),
                exportedAtEpochMs = parseInstant(rootObject?.stringOrNull("exportedAt")),
                devices = devices,
                warnings = fleetWarnings,
            ),
        )
    }

    private fun parseDevice(record: JsonObject, index: Int): Device {
        val issues = mutableListOf<DataIssue>()

        val rawId = record.stringOrNull("id")
        val id = rawId ?: run {
            issues += issue(
                code = DataIssueCode.MISSING_ID,
                field = "id",
                rawValue = record.rawValue("id"),
                message = "Identificador ausente; foi atribuído DEV-UNKNOWN-${index + 1}.",
            )
            "DEV-UNKNOWN-${index + 1}"
        }

        val model = record.stringOrNull("model") ?: run {
            issues += issue(
                code = DataIssueCode.MISSING_MODEL,
                field = "model",
                rawValue = record.rawValue("model"),
                message = "Modelo ausente; o registro será exibido como não identificado.",
            )
            "Modelo não identificado"
        }

        val capacity = record.numberOrNull("batteryCapacityMah")?.takeIf { it > 0.0 }
        if (capacity == null) {
            issues += issue(
                code = DataIssueCode.INVALID_CAPACITY,
                field = "batteryCapacityMah",
                rawValue = record.rawValue("batteryCapacityMah"),
                message = "Capacidade de bateria ausente ou inválida " +
                    "(${record.rawValue("batteryCapacityMah") ?: "ausente"}); era esperado um valor > 0 mAh.",
            )
        }

        val screenOnMinutes = record.numberOrNull("screenOnMinutes")?.takeIf { it > 0.0 }
        if (screenOnMinutes == null) {
            issues += issue(
                code = DataIssueCode.INVALID_SCREEN_ON,
                field = "screenOnMinutes",
                rawValue = record.rawValue("screenOnMinutes"),
                message = "Tempo de tela ausente ou inválido " +
                    "(${record.rawValue("screenOnMinutes") ?: "ausente"}); era esperado um valor > 0 min.",
            )
        }

        val rawEnergy = record.numberOrNull("energyConsumed")?.takeIf { it >= 0.0 }
        if (rawEnergy == null) {
            issues += issue(
                code = DataIssueCode.MISSING_ENERGY,
                field = "energyConsumed",
                rawValue = record.rawValue("energyConsumed"),
                message = "Energia consumida ausente ou inválida.",
            )
        }

        val unit = record.stringOrNull("energyUnit")?.uppercase(Locale.ROOT)
        val voltage = record.numberOrNull("nominalVoltage")?.takeIf { it > 0.0 }
        if (record.numberOrNull("nominalVoltage") != null && voltage == null) {
            issues += issue(
                code = DataIssueCode.INVALID_VOLTAGE,
                field = "nominalVoltage",
                rawValue = record.rawValue("nominalVoltage"),
                message = "Tensão nominal inválida; a conversão de mWh para mAh não é possível.",
            )
        }

        val energyMah = when (unit) {
            "MAH" -> rawEnergy
            "MWH" -> {
                if (rawEnergy != null && voltage == null) {
                    issues += issue(
                        code = DataIssueCode.MISSING_VOLTAGE_FOR_MWH,
                        field = "nominalVoltage",
                        rawValue = record.rawValue("nominalVoltage"),
                        message = "Energia em mWh sem tensão nominal válida; impossível normalizar para mAh.",
                    )
                }
                rawEnergy?.let { energy -> voltage?.let { v -> energy / v } }
            }
            null -> {
                if (rawEnergy != null) {
                    issues += issue(
                        code = DataIssueCode.UNKNOWN_ENERGY_UNIT,
                        field = "energyUnit",
                        rawValue = record.rawValue("energyUnit"),
                        message = "Unidade de energia ausente; apenas mAh e mWh são aceitos.",
                    )
                }
                null
            }
            else -> {
                if (rawEnergy != null) {
                    issues += issue(
                        code = DataIssueCode.UNKNOWN_ENERGY_UNIT,
                        field = "energyUnit",
                        rawValue = record.rawValue("energyUnit"),
                        message = "Unidade de energia desconhecida (${record.rawValue("energyUnit")}); " +
                            "apenas mAh e mWh são aceitos.",
                    )
                }
                null
            }
        }

        val jankFramesPct = record.numberOrNull("jankFramesPct")?.takeIf { it in 0.0..100.0 }
        if (jankFramesPct == null) {
            issues += issue(
                code = DataIssueCode.INVALID_JANK,
                field = "jankFramesPct",
                rawValue = record.rawValue("jankFramesPct"),
                message = "Percentual de quadros perdidos ausente ou fora de 0–100.",
            )
        }

        val crashCount7d = record.intOrNull("crashCount7d")?.takeIf { it >= 0 }
        if (crashCount7d == null) {
            issues += issue(
                code = DataIssueCode.INVALID_CRASH_COUNT,
                field = "crashCount7d",
                rawValue = record.rawValue("crashCount7d"),
                message = "Contagem de falhas em 7 dias ausente ou inválida.",
            )
        }

        val batteryLevelPct = record.intOrNull("batteryLevelPct")?.takeIf { it in 0..100 }
        if (record.intOrNull("batteryLevelPct") != null && batteryLevelPct == null) {
            issues += issue(
                code = DataIssueCode.INVALID_BATTERY_LEVEL,
                field = "batteryLevelPct",
                rawValue = record.rawValue("batteryLevelPct"),
                message = "Nível de bateria fora de 0–100.",
            )
        }

        val lastSeen = parseInstant(record.stringOrNull("lastSeen"))
        if (lastSeen == null && record.stringOrNull("lastSeen") != null) {
            issues += issue(
                code = DataIssueCode.INVALID_LAST_SEEN,
                field = "lastSeen",
                rawValue = record.rawValue("lastSeen"),
                message = "Data de último contato inválida.",
            )
        }

        val ramMb = record.intOrNull("ramMb")?.takeIf { it > 0 }
        if (record.intOrNull("ramMb") != null && ramMb == null) {
            issues += issue(
                code = DataIssueCode.INVALID_RAM,
                field = "ramMb",
                rawValue = record.rawValue("ramMb"),
                message = "RAM inválida.",
            )
        }

        val androidApi = record.intOrNull("androidApi")?.takeIf { it > 0 }
        if (record.intOrNull("androidApi") != null && androidApi == null) {
            issues += issue(
                code = DataIssueCode.INVALID_API,
                field = "androidApi",
                rawValue = record.rawValue("androidApi"),
                message = "Nível de API inválido.",
            )
        }

        val tier = parseTier(record.stringOrNull("tier"))
        if (record.stringOrNull("tier") != null && tier == DeviceTier.UNKNOWN) {
            issues += issue(
                code = DataIssueCode.INVALID_TIER,
                field = "tier",
                rawValue = record.rawValue("tier"),
                message = "Tier desconhecido; será exibido como não classificado.",
            )
        }

        return Device(
            id = id,
            model = model,
            soc = record.stringOrNull("soc") ?: "SoC não informado",
            tier = tier,
            ramMb = ramMb,
            androidApi = androidApi,
            batteryCapacityMah = capacity,
            batteryLevelPct = batteryLevelPct,
            screenOnMinutes = screenOnMinutes,
            energyConsumedMah = energyMah,
            nominalVoltage = voltage,
            jankFramesPct = jankFramesPct,
            crashCount7d = crashCount7d,
            lastSeenEpochMs = lastSeen,
            dataIssues = issues,
        )
    }

    private fun parseTier(raw: String?): DeviceTier = when (raw?.lowercase(Locale.ROOT)) {
        "low" -> DeviceTier.LOW
        "mid" -> DeviceTier.MID
        "high" -> DeviceTier.HIGH
        else -> DeviceTier.UNKNOWN
    }

    private fun parseInstant(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        raw.toLongOrNull()?.let { numeric ->
            return if (numeric < 100_000_000_000L) numeric * 1_000 else numeric
        }
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }

    private fun issue(
        code: DataIssueCode,
        field: String,
        rawValue: String?,
        message: String,
    ) = DataIssue(code = code, field = field, rawValue = rawValue, message = message)

    private fun JsonObject.rawValue(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.toString()?.take(48)

    private fun JsonObject.stringOrNull(key: String): String? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        val value = primitive.content.trim()
        if (value.isEmpty()) return null
        if (value.lowercase(Locale.ROOT) in INVALID_STRINGS) return null
        return value
    }

    private fun JsonObject.numberOrNull(key: String): Double? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        val raw = primitive.content
            .trim()
            .removeSuffix("%")
            .replace(',', '.')
            .trim()
        if (raw.isEmpty()) return null
        return raw.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val value = numberOrNull(key) ?: return null
        val longValue = value.toLong()
        if (longValue.toDouble() != value) return null
        if (longValue < Int.MIN_VALUE.toLong() || longValue > Int.MAX_VALUE.toLong()) return null
        return longValue.toInt()
    }

    private companion object {
        val INVALID_STRINGS = setOf("null", "undefined", "n/a", "na", "nan", "infinity", "-")
    }
}

package com.seldon.fleet.domain.comparison

import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.data.parser.CorruptedFileParser
import com.seldon.fleet.data.parser.ParseOutcome
import com.seldon.fleet.domain.classifier.PercentileStatusClassifier
import com.seldon.fleet.domain.engine.FleetAnomalyEngine
import com.seldon.fleet.domain.model.DeviceStatus
import java.io.File
import java.util.Locale
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class FleetComparisonTest {

    private val candidate = PercentileStatusClassifier()
    private val comparison = ClassificationComparisonService(
        referenceClassifier = FleetAnomalyEngine(),
        candidateClassifier = candidate,
    )

    @Test
    fun `official fleet comparison reproduces the pre-registered experiment`() = runTest {
        val fleet = officialFleet()
        val result = comparison.compare(fleet.devices)

        println(result.toMarkdown())

        assertThat(fleet.devices).hasSize(30)
        assertThat(result.total).isEqualTo(30)
        assertThat(result.referenceCounts).containsExactly(
            DeviceStatus.OK, 16,
            DeviceStatus.ATENCAO, 8,
            DeviceStatus.CRITICO, 4,
            DeviceStatus.INDETERMINADO, 2,
        )
        assertThat(result.candidateCounts).containsExactly(
            DeviceStatus.OK, 11,
            DeviceStatus.ATENCAO, 11,
            DeviceStatus.CRITICO, 6,
            DeviceStatus.INDETERMINADO, 2,
        )
        assertThat(result.agreements).isEqualTo(21)
        assertThat(result.disagreements.map { it.deviceId }).containsExactly(
            "DEV-0137",
            "DEV-1546",
            "DEV-1582",
            "DEV-1924",
            "DEV-1366",
            "DEV-1057",
            "DEV-0854",
            "DEV-1231",
            "DEV-0800",
        )
        assertThat(result.confusion.sumOf { it.count }).isEqualTo(30)

        val byId = result.rows.associateBy { it.deviceId }
        assertThat(byId.getValue("DEV-1924").candidateStatus)
            .isEqualTo(DeviceStatus.ATENCAO)
        assertThat(byId.getValue("DEV-1924").candidateScore!!)
            .isWithin(0.001).of(0.889)
        assertThat(byId.getValue("DEV-0137").candidateStatus)
            .isEqualTo(DeviceStatus.CRITICO)
        assertThat(byId.getValue("DEV-0137").candidateScore!!)
            .isWithin(0.001).of(1.0)
        assertThat(byId.getValue("DEV-1924").candidatePeerCount).isEqualTo(27)

        val indeterminate = result.rows.filter {
            it.referenceStatus == DeviceStatus.INDETERMINADO
        }
        assertThat(indeterminate).hasSize(2)
        assertThat(indeterminate.map { it.candidateStatus })
            .containsExactly(DeviceStatus.INDETERMINADO, DeviceStatus.INDETERMINADO)
        assertThat(indeterminate.all { it.candidateScore == null }).isTrue()
    }

    private fun ClassificationComparison.toMarkdown(): String = buildString {
        appendLine("## Resultado da comparação")
        appendLine()
        appendLine("| status | referência | candidato |")
        appendLine("|---|---:|---:|")
        DeviceStatus.entries.forEach { status ->
            appendLine("| $status | ${referenceCounts[status]} | ${candidateCounts[status]} |")
        }
        appendLine()
        appendLine("| ID | dreno %/h | jank % | crashes | referência | candidato | score | dreno p | jank p | crash p |")
        appendLine("|---|---:|---:|---:|---|---|---:|---:|---:|---:|")
        disagreements.forEach { row ->
            appendLine(
                "| ${row.deviceId} | ${format(row.relativeDrainPctPerHour)} | " +
                    "${format(row.jankFramesPct)} | ${row.crashCount7d ?: "N/D"} | " +
                    "${row.referenceStatus} | ${row.candidateStatus} | " +
                    "${format(row.candidateScore)} | ${format(row.candidateDrainPercentile)} | " +
                    "${format(row.candidateJankPercentile)} | ${format(row.candidateCrashPercentile)} |",
            )
        }
    }

    private fun format(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "N/D"

    private suspend fun officialFleet() = (parser().parse(officialJson()) as ParseOutcome.Success).fleet

    private fun parser() = CorruptedFileParser(
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowSpecialFloatingPointValues = true
        },
        dispatcher = UnconfinedTestDispatcher(),
    )

    private fun officialJson(): String {
        val candidates = listOf(
            File("src/main/assets/frota.json"),
            File("app/src/main/assets/frota.json"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("frota.json oficial não encontrado a partir de ${File(".").absolutePath}")
    }
}

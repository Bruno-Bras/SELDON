package com.seldon.fleet.presentation.common

import androidx.compose.runtime.Immutable
import com.seldon.fleet.domain.model.FleetFailure

@Immutable
data class UiFailure(
    val code: String,
    val title: String,
    val message: String,
    val hint: String,
    val technicalDetail: String,
    val retryable: Boolean,
)

fun FleetFailure.toUiFailure(): UiFailure = when (this) {
    is FleetFailure.AssetUnavailable -> UiFailure(
        code = code,
        title = "FONTE INDISPONÍVEL",
        message = "O arquivo assets/frota.json não pôde ser lido.",
        hint = "Verifique se o arquivo está empacotado no APK e tente novamente.",
        technicalDetail = technicalDetail,
        retryable = true,
    )
    is FleetFailure.MalformedSource -> UiFailure(
        code = code,
        title = "TELEMETRIA CORROMPIDA",
        message = "O conteúdo do arquivo não pôde ser interpretado.",
        hint = "O app não encerrou. Corrija o JSON de origem ou restaure uma exportação válida.",
        technicalDetail = technicalDetail,
        retryable = true,
    )
    is FleetFailure.EmptyFleet -> UiFailure(
        code = code,
        title = "FROTA SEM REGISTROS",
        message = "O arquivo é válido, mas não contém nenhum dispositivo utilizável.",
        hint = "Exporte a frota novamente com pelo menos um registro.",
        technicalDetail = technicalDetail,
        retryable = true,
    )
    is FleetFailure.PreferencesUnavailable -> UiFailure(
        code = code,
        title = "PREFERÊNCIAS INDISPONÍVEIS",
        message = "Não foi possível acessar o armazenamento local de favoritos.",
        hint = "A leitura da frota continua disponível. Tente novamente mais tarde.",
        technicalDetail = technicalDetail,
        retryable = true,
    )
    is FleetFailure.DeviceNotFound -> UiFailure(
        code = code,
        title = "DISPOSITIVO AUSENTE",
        message = "O dispositivo $deviceId não está presente na frota atual.",
        hint = "Volte para a lista e atualize a fonte de dados.",
        technicalDetail = technicalDetail,
        retryable = false,
    )
    is FleetFailure.Unexpected -> UiFailure(
        code = code,
        title = "FALHA NÃO ESPERADA",
        message = "A operação foi interrompida por um erro inesperado.",
        hint = "Tente novamente. Se persistir, consulte o detalhe técnico abaixo.",
        technicalDetail = technicalDetail,
        retryable = true,
    )
}

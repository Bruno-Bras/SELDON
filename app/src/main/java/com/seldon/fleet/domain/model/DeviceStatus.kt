package com.seldon.fleet.domain.model

/**
 * Estados de saúde do dispositivo. Os nomes são deliberadamente técnicos e
 * não contêm rótulos de interface ("Sem dados" é responsabilidade da apresentação).
 */
enum class DeviceStatus {
    OK,
    ATENCAO,
    CRITICO,
    INDETERMINADO,
}

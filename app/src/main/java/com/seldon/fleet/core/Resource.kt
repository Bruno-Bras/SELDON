package com.seldon.fleet.core

import com.seldon.fleet.domain.model.FleetFailure

/**
 * Resultado explícito de uma operação assíncrona observável.
 * A UI nunca recebe exceções: apenas Loading, Success ou Failure.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Failure(val failure: FleetFailure) : Resource<Nothing>
}

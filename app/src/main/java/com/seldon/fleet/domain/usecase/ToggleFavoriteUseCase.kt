package com.seldon.fleet.domain.usecase

import com.seldon.fleet.domain.repository.FleetRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FleetRepository,
) {
    suspend operator fun invoke(deviceId: String): Result<Boolean> = repository.toggleFavorite(deviceId)
}

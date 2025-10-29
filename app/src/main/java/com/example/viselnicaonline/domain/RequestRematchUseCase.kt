package com.example.viselnicaonline.domain

import javax.inject.Inject

class RequestRematchUseCase  @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(
        gameId: String,
        requester: String
    ) = repository.requestRematch(
        gameId,
        requester
    )
}
package com.example.viselnicaonline.domain

import javax.inject.Inject

class ObserveGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(gameId: String) = repository.observeGame(gameId)
}
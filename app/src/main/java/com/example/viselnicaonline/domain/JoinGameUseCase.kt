package com.example.viselnicaonline.domain

import javax.inject.Inject

class JoinGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(
        gameId: String,
        playerName: String,
    ) = repository.joinGame(
        gameId,
        playerName
    )
}
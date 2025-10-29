package com.example.viselnicaonline.domain

import javax.inject.Inject

class LeaveGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(
        gameId: String,
        playerName: String,
    ) = repository.leaveGame(
        gameId,
        playerName
    )
}
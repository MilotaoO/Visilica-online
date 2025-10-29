package com.example.viselnicaonline.domain

import javax.inject.Inject

class DeleteGameUseCase @Inject constructor(
    private val repository: GameRepository
){
    suspend operator fun invoke(gameId: String) = repository.deleteGame(gameId)
}
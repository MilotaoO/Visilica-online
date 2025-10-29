package com.example.viselnicaonline.domain

import javax.inject.Inject

class CreateGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(hostName: String) = repository.createGame(hostName)
}
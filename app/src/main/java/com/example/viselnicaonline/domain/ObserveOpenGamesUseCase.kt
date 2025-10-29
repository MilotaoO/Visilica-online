package com.example.viselnicaonline.domain

import javax.inject.Inject

class ObserveOpenGamesUseCase  @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke() = repository.observeOpenGames()
}
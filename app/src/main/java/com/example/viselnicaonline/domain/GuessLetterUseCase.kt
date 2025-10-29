package com.example.viselnicaonline.domain

import javax.inject.Inject

class GuessLetterUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(
        gameId: String,
        playerName: String,
        letter: Char
    ) = repository.guessLetter(
        gameId,
        playerName,
        letter
    )
}
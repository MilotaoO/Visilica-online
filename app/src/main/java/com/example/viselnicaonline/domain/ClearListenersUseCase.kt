package com.example.viselnicaonline.domain

import javax.inject.Inject

class ClearListenersUseCase @Inject constructor(
    private val repository: GameRepository
) {

    operator fun invoke() = repository.clearListeners()
}
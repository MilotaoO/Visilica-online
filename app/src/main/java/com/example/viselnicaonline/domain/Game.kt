package com.example.viselnicaonline.domain

data class Game(
    val id: String,
    val player1: String?,
    val player2: String?,
    val word: String,
    val guessedWord: List<Char>,
    val userLetters: List<Char>,
    val attemptsLeft: Int,
    val turn: String,
    val status: String,
    val winner: String
)

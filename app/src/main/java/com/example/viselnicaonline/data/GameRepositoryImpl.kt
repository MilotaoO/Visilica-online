package com.example.viselnicaonline.data

import com.example.viselnicaonline.domain.GameInfo
import com.example.viselnicaonline.domain.GameRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GameRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val wordsProvider: WordsProvider,
) : GameRepository {

    private val gamesRef = database.getReference("games")
    private val listeners = mutableMapOf<String, ValueEventListener>()

    override suspend fun createGame(hostName: String): Result<String> {
        return try {
            val gameId =
                gamesRef.push().key ?: return Result.failure(Exception("Cannot generate gameId"))
            val word = wordsProvider.getRandomWord()
            val guessedWord = List(word.length) { '_' }
            val newGame = GameInfo(
                id = gameId,
                player1 = hostName,
                player2 = null,
                word = word,
                guessedWord = guessedWord,
                userLetters = emptyList(),
                attemptsLeft = 6,
                turn = hostName,
                status = "waiting",
                winner = ""
            )
            gamesRef.child(gameId).setValue(newGame).await()
            Result.success(gameId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGame(gameId: String, playerName: String): Result<Unit> {
        return try {
            suspendCancellableCoroutine<Unit> { cont ->
                val gameRef = gamesRef.child(gameId)
                gameRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val game = currentData.getValue(GameInfo::class.java)
                            ?: return Transaction.success(currentData)
                        if (game.player2 != null || game.status != "waiting") return Transaction.abort()
                        currentData.value = game.copy(player2 = playerName, status = "playing")
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) cont.resumeWithException(error.toException())
                        else if (!committed) cont.resumeWithException(Exception("Transaction aborted"))
                        else cont.resume(Unit)
                    }
                })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeGame(gameId: String): Flow<GameInfo?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(GameInfo::class.java)
                trySend(game).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.child(gameId).addValueEventListener(listener)
        listeners[gameId] = listener
        awaitClose { gamesRef.child(gameId).removeEventListener(listener) }
    }

    override suspend fun guessLetter(
        gameId: String,
        playerName: String,
        letter: Char
    ): Result<Unit> {
        return try {
            suspendCancellableCoroutine<Unit> { cont ->
                val gameRef = gamesRef.child(gameId)
                gameRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val game = currentData.getValue(GameInfo::class.java)
                            ?: return Transaction.success(currentData)
                        if (game.status != "playing" || game.turn != playerName || game.userLetters.contains(
                                letter
                            )
                        )
                            return Transaction.abort()

                        val newGuessed = game.guessedWord.toMutableList()
                        val newUserLetters = game.userLetters + letter
                        var attemptsLeft = game.attemptsLeft
                        var turn = if (playerName == game.player1) game.player2
                            ?: game.player1!! else game.player1 ?: game.player2!!
                        var status = game.status
                        var winner = game.winner

                        if (game.word.contains(letter, ignoreCase = true)) {
                            game.word.forEachIndexed { index, c ->
                                if (c.equals(letter, ignoreCase = true)) newGuessed[index] = c
                            }
                        } else {
                            attemptsLeft -= 1
                        }

                        if (!newGuessed.contains('_')) {
                            status = "finished"
                            winner = playerName
                        } else if (attemptsLeft <= 0) {
                            status = "finished"
                            winner =
                                if (playerName == game.player1) game.player2 ?: "" else game.player1
                                    ?: ""
                        } else {
                            turn = if (playerName == game.player1) game.player2
                                ?: game.player1!! else game.player1 ?: game.player2!!
                        }

                        val updatedGame = game.copy(
                            guessedWord = newGuessed,
                            userLetters = newUserLetters,
                            attemptsLeft = attemptsLeft,
                            turn = turn,
                            status = status,
                            winner = winner
                        )
                        currentData.value = updatedGame
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) cont.resumeWithException(error.toException())
                        else if (!committed) cont.resumeWithException(Exception("Transaction aborted"))
                        else cont.resume(Unit)
                    }
                })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGameOnce(gameId: String): Result<GameInfo?> {
        return try {
            val snapshot = gamesRef.child(gameId).get().await()
            Result.success(snapshot.getValue(GameInfo::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGame(gameId: String, playerName: String): Result<Unit> {
        return try {
            suspendCancellableCoroutine<Unit> { cont ->
                val gameRef = gamesRef.child(gameId)
                gameRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val game = currentData.getValue(GameInfo::class.java)
                            ?: return Transaction.success(currentData)
                        if (game.status == "waiting") {
                            currentData.value = null
                        } else if (game.status == "playing") {
                            val status = "finished"
                            val winner =
                                if (playerName == game.player1) game.player2 ?: "" else game.player1
                                    ?: ""
                            currentData.value = game.copy(status = status, winner = winner)
                        }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) cont.resumeWithException(error.toException())
                        else if (!committed) cont.resumeWithException(Exception("Transaction aborted"))
                        else cont.resume(Unit)
                    }
                })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestRematch(gameId: String, requester: String): Result<String> {
        return try {
            val snapshot = gamesRef.child(gameId).get().await()
            val oldGame = snapshot.getValue(GameInfo::class.java)
                ?: return Result.failure(Exception("Game not found"))
            val newGameId =
                gamesRef.push().key ?: return Result.failure(Exception("Cannot create new game id"))
            val newGuessedWord = List(oldGame.word.length) { '_' }
            val newGame = oldGame.copy(
                id = newGameId,
                player2 = null,
                guessedWord = newGuessedWord,
                userLetters = emptyList(),
                attemptsLeft = 6,
                turn = oldGame.player1 ?: "",
                status = "waiting",
                winner = ""
            )
            gamesRef.child(newGameId).setValue(newGame).await()
            Result.success(newGameId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGame(gameId: String): Result<Unit> {
        return try {
            gamesRef.child(gameId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeOpenGames(): Flow<List<GameInfo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(GameInfo::class.java) }
                    .filter { it.status == "waiting" }
                trySend(list).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.addValueEventListener(listener)
        awaitClose { gamesRef.removeEventListener(listener) }
    }

    override fun clearListeners() {
        listeners.forEach { (gameId, listener) ->
            gamesRef.child(gameId).removeEventListener(listener)
        }
        listeners.clear()
    }
}
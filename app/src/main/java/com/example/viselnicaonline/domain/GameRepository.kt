package com.example.viselnicaonline.domain

interface GameRepository {

    // 1. Создать новую игру и вернуть её id
    suspend fun createGame(hostName: String): Result<String>

    // 2. Присоединиться к игре по id
    suspend fun joinGame(gameId: String, playerName: String): Result<Unit>

    // 3. Наблюдать за состоянием игры (реактивно)
    fun observeGame(gameId: String): kotlinx.coroutines.flow.Flow<GameInfo?>

    // 4. Сделать ход — угадать букву (атомарно, с транзакцией)
    suspend fun guessLetter(gameId: String, playerName: String, letter: Char): Result<Unit>

    // 5. Получить одноразово состояние игры
    suspend fun fetchGameOnce(gameId: String): Result<GameInfo?>

    // 6. Покинуть игру (освободить слот, возможно удалить комнату если оба вышли)
    suspend fun leaveGame(gameId: String, playerName: String): Result<Unit>

    // 7. Запросить рематч (инициировать новую игру с тем же словом/параметрами)
    suspend fun requestRematch(gameId: String, requester: String): Result<String> // возвращает новый gameId

    // 8. Удалить игру (только хост или по завершению)
    suspend fun deleteGame(gameId: String): Result<Unit>

    // 9. Слушать список доступных игр (опционально — лобби)
    fun observeOpenGames(): kotlinx.coroutines.flow.Flow<List<GameInfo>>

    // 10. Отменить все слушатели (cleanup) — вызывается при onCleared()
    fun clearListeners()
}
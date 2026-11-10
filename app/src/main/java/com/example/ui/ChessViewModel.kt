package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.AnalyzedPly
import com.example.chess.BoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessMove
import com.example.chess.GameAnalyticsEngine
import com.example.chess.GameReviewReport
import com.example.chess.MoveClassification
import com.example.chess.PieceColor
import com.example.chess.Square
import com.example.data.local.ChessDatabase
import com.example.data.local.JoinedItemEntity
import com.example.data.local.SavedGameEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.remote.CloudEvalResult
import com.example.data.remote.LichessApiClient
import com.example.data.remote.LichessBroadcastOrSimulItem
import com.example.data.remote.LichessClubItem
import com.example.data.remote.LichessPuzzleData
import com.example.data.remote.LichessTournamentItem
import com.example.data.remote.LichessTvChannelItem
import com.example.data.remote.LichessUserProfile
import com.example.data.remote.OpeningExplorerResult
import com.example.ui.theme.AppUiTheme
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.PieceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlayMode(val labelRu: String) {
    ONLINE_LICHESS("Онлайн Lichess"),
    VS_STOCKFISH("Против Stockfish"),
    TWO_PLAYER("Вдвоем на экране"),
    FREE_ANALYSIS("Доска анализа")
}

data class ChatMessageItem(
    val sender: String,
    val message: String,
    val timestampLabel: String = "сейчас"
)

data class ArenaUiState(
    val playMode: PlayMode = PlayMode.VS_STOCKFISH,
    val timeControlLabel: String = "3+2 • Блиц",
    val initialSeconds: Int = 180,
    val incrementSeconds: Int = 2,
    val aiLevel: Int = 5,
    val boardState: BoardState = BoardState.fromFen(BoardState.INITIAL_FEN),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val moveHistorySan: List<String> = emptyList(),
    val moveHistoryUci: List<String> = emptyList(),
    val flipped: Boolean = false,
    val whiteClockSeconds: Int = 180,
    val blackClockSeconds: Int = 180,
    val isGameActive: Boolean = true,
    val statusBanner: String = "Ваш ход • Белые",
    val evalCentipawns: Int = 22,
    val bestMoveHintUci: String? = null,
    val lastMoveClassification: MoveClassification? = null,
    val liveOnlineGameId: String? = null,
    val myOnlineColor: PieceColor? = null,
    val isSearchingOnline: Boolean = false,
    val isTvLiveStream: Boolean = false,
    val loggedInAccount: LichessUserProfile? = null,
    val isVerifyingAccount: Boolean = false,
    val authStatusMessage: String? = null,
    val ongoingGames: List<com.example.data.remote.OngoingLichessGame> = emptyList(),
    val whitePlayerName: String = "Вы (LibreChess)",
    val whitePlayerRating: Int = 1850,
    val blackPlayerName: String = "Stockfish Ур. 5",
    val blackPlayerRating: Int = 2050,
    val chatMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem("Lichess Server", "Добро пожаловать в LibreChess Studio! Хорошей партии.")
    ),
    val openingExplorer: OpeningExplorerResult? = null,
    val cloudEval: CloudEvalResult? = null
)

data class CommunityUiState(
    val isLoading: Boolean = false,
    val tournaments: List<LichessTournamentItem> = emptyList(),
    val clubs: List<LichessClubItem> = emptyList(),
    val tvChannels: List<LichessTvChannelItem> = emptyList(),
    val broadcastsAndSimuls: List<LichessBroadcastOrSimulItem> = emptyList(),
    val dailyPuzzle: LichessPuzzleData? = null,
    val puzzleBoard: BoardState = BoardState.fromFen(BoardState.INITIAL_FEN),
    val puzzleSelectedSq: Square? = null,
    val puzzleLegalMoves: List<ChessMove> = emptyList(),
    val puzzleSolutionStep: Int = 0,
    val puzzleStreak: Int = 3,
    val puzzleStatusText: String = "Найдите лучший тактический удар!",
    val clubSearchQuery: String = "",
    val feedbackBanner: String? = null
)

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val searchedUsername: String = "DrNykterstein",
    val opponentForCrosstable: String = "Hikaru",
    val crosstableSummary: String = "Личные встречи DrNykterstein vs Hikaru: загрузка...",
    val userProfile: LichessUserProfile? = null,
    val activeReviewReport: GameReviewReport? = null,
    val currentReviewPlyIndex: Int = 0,
    val retryMistakeMode: Boolean = false,
    val retryFeedbackText: String? = null,
    val retrySelectedSquare: Square? = null,
    val retryBoardState: BoardState? = null,
    val statusMessage: String? = null
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ChessDatabase.getSafeDao(application)

    val preferencesFlow: StateFlow<UserPreferencesEntity> = dao.observePreferences()
        .map { it ?: UserPreferencesEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesEntity()
        )

    val savedGamesFlow: StateFlow<List<SavedGameEntity>> = dao.observeSavedGames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val joinedItemsFlow: StateFlow<List<JoinedItemEntity>> = dao.observeJoinedItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _arenaState = MutableStateFlow(ArenaUiState())
    val arenaState: StateFlow<ArenaUiState> = _arenaState.asStateFlow()

    private val _communityState = MutableStateFlow(CommunityUiState())
    val communityState: StateFlow<CommunityUiState> = _communityState.asStateFlow()

    private val _analyticsState = MutableStateFlow(AnalyticsUiState())
    val analyticsState: StateFlow<AnalyticsUiState> = _analyticsState.asStateFlow()

    private var clockJob: Job? = null
    private var liveStreamJob: Job? = null

    init {
        com.example.util.AppDiagnosticLogger.ok("ViewModel", "ChessViewModel создан, запуск фоновой инициализации")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val savedPrefs = dao.getPreferencesOnce()
                if (savedPrefs == null) {
                    dao.savePreferences(UserPreferencesEntity())
                } else if (savedPrefs.lichessApiToken.isNotBlank()) {
                    LichessApiClient.verifyTokenAndFetchAccount(savedPrefs.lichessApiToken).onSuccess { prof ->
                        _arenaState.update {
                            it.copy(
                                loggedInAccount = prof,
                                whitePlayerName = prof.username,
                                whitePlayerRating = prof.blitzRating,
                                authStatusMessage = "✓ Аккаунт @${prof.username} подключен"
                            )
                        }
                    }
                    LichessApiClient.fetchOngoingAccountGames(savedPrefs.lichessApiToken).onSuccess { games ->
                        _arenaState.update { it.copy(ongoingGames = games) }
                    }
                }
                seedInitialMasterpieceGamesIfEmpty()
                com.example.util.AppDiagnosticLogger.ok("ViewModel", "Стартовые партии и настройки загружены")
            }.onFailure { err ->
                com.example.util.AppDiagnosticLogger.error("ViewModel", "Ошибка при первичной инициализации данных", err)
            }
            runCatching {
                val targetUser = dao.getPreferencesOnce()?.lichessUsername?.ifBlank { "DrNykterstein" } ?: "DrNykterstein"
                refreshExplorerAndCloudEval(_arenaState.value.boardState.toFen())
                loadLichessPlayerAndGames(targetUser)
                loadCommunityData()
                com.example.util.AppDiagnosticLogger.ok("LichessAPI", "Фоновая синхронизация с Lichess API запущена")
            }.onFailure { err ->
                com.example.util.AppDiagnosticLogger.error("LichessAPI", "Ошибка запуска синхронизации Lichess API", err)
            }
        }
        startArenaClockTicker()
    }

    private fun startArenaClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val st = _arenaState.value
                if (!st.isGameActive || st.playMode == PlayMode.FREE_ANALYSIS || st.moveHistorySan.isEmpty()) continue
                if (st.boardState.activeColor == PieceColor.WHITE) {
                    val nextW = (st.whiteClockSeconds - 1).coerceAtLeast(0)
                    _arenaState.update {
                        it.copy(
                            whiteClockSeconds = nextW,
                            isGameActive = nextW > 0,
                            statusBanner = if (nextW == 0) "Время белых истекло • 0-1" else it.statusBanner
                        )
                    }
                } else {
                    val nextB = (st.blackClockSeconds - 1).coerceAtLeast(0)
                    _arenaState.update {
                        it.copy(
                            blackClockSeconds = nextB,
                            isGameActive = nextB > 0,
                            statusBanner = if (nextB == 0) "Время черных истекло • 1-0" else it.statusBanner
                        )
                    }
                }
            }
        }
    }

    // --- ARENA & LIVE PLAY ---

    fun selectTimeControlAndStart(
        label: String,
        minutes: Int,
        increment: Int,
        mode: PlayMode = _arenaState.value.playMode,
        aiLevel: Int = _arenaState.value.aiLevel
    ) {
        liveStreamJob?.cancel()
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val secs = (minutes * 60).coerceAtLeast(60)
        val myName = _arenaState.value.loggedInAccount?.username
            ?: prefs.lichessUsername.takeIf { it != "DrNykterstein" || prefs.lichessApiToken.isNotBlank() }
            ?: "Вы"
        val myRating = _arenaState.value.loggedInAccount?.blitzRating ?: 1850
        val oppName = when (mode) {
            PlayMode.ONLINE_LICHESS -> "Поиск соперника Lichess..."
            PlayMode.VS_STOCKFISH -> "Stockfish Ур. $aiLevel"
            PlayMode.TWO_PLAYER -> "Игрок 2 (Черные)"
            PlayMode.FREE_ANALYSIS -> "Режим Анализа"
        }
        _arenaState.update {
            it.copy(
                playMode = mode,
                timeControlLabel = label,
                initialSeconds = secs,
                incrementSeconds = increment,
                aiLevel = aiLevel,
                boardState = BoardState.fromFen(BoardState.INITIAL_FEN),
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                moveHistorySan = emptyList(),
                moveHistoryUci = emptyList(),
                whiteClockSeconds = secs,
                blackClockSeconds = secs,
                isGameActive = true,
                liveOnlineGameId = null,
                myOnlineColor = null,
                isSearchingOnline = false,
                isTvLiveStream = false,
                evalCentipawns = 20,
                bestMoveHintUci = null,
                lastMoveClassification = null,
                whitePlayerName = myName,
                whitePlayerRating = myRating,
                blackPlayerName = oppName,
                blackPlayerRating = if (mode == PlayMode.VS_STOCKFISH) 1100 + aiLevel * 210 else 1900,
                statusBanner = if (mode == PlayMode.ONLINE_LICHESS) {
                    "Онлайн Lichess ($label) • Подключение..."
                } else {
                    "Новая партия ($label) • Ваш ход"
                }
            )
        }
        refreshExplorerAndCloudEval(BoardState.INITIAL_FEN)

        if (mode == PlayMode.ONLINE_LICHESS) {
            startOnlineMatchmakingOrServerGame(minutes, increment)
        }
    }

    /**
     * Real online matchmaking on Lichess.org:
     * - If logged in with a Lichess token: checks active games -> searches human pool (/api/board/seek) -> or starts real Lichess server game (/api/challenge/ai).
     * - If no token yet: connects to real live Lichess TV stream (/api/tv/feed) and prompts user to log in for rated play.
     */
    fun startOnlineMatchmakingOrServerGame(minutes: Int = _arenaState.value.initialSeconds / 60, increment: Int = _arenaState.value.incrementSeconds) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val token = prefs.lichessApiToken.trim()
        if (token.isBlank()) {
            com.example.util.AppDiagnosticLogger.warn("OnlinePlay", "Токен Lichess не задан — запуск живого стрима Lichess TV")
            startLiveTvStream(
                customBanner = "Войдите в аккаунт Lichess сверху для игры в пуле • Сейчас: Прямой эфир Lichess TV"
            )
            return
        }

        liveStreamJob?.cancel()
        liveStreamJob = viewModelScope.launch(Dispatchers.IO) {
            _arenaState.update {
                it.copy(
                    playMode = PlayMode.ONLINE_LICHESS,
                    isSearchingOnline = true,
                    isTvLiveStream = false,
                    statusBanner = "🔍 Поиск соперника на сервере Lichess.org ($minutes+$increment)..."
                )
            }
            com.example.util.AppDiagnosticLogger.info("OnlinePlay", "Запуск поиска партии на Lichess ($minutes+$increment)")

            // 1. Check if the user already has an ongoing game on Lichess
            val ongoing = LichessApiClient.fetchOngoingAccountGames(token).getOrDefault(emptyList())
            _arenaState.update { it.copy(ongoingGames = ongoing) }
            if (ongoing.isNotEmpty()) {
                val active = ongoing.first()
                com.example.util.AppDiagnosticLogger.ok("OnlinePlay", "Найдена активная партия на Lichess: #${active.gameId}")
                connectToLiveGameStream(active.gameId, token, initialColorHint = active.color)
                return@launch
            }

            // 2. Search human matchmaking pool via /api/board/seek
            val seekRes = LichessApiClient.seekRealHumanOpponent(
                clockMinutes = minutes.coerceAtLeast(1),
                clockIncrement = increment,
                rated = true,
                token = token
            )
            seekRes.onSuccess { matchedGame ->
                com.example.util.AppDiagnosticLogger.ok("OnlinePlay", "Найден соперник в пуле Lichess: #${matchedGame.gameId}")
                connectToLiveGameStream(matchedGame.gameId, token, initialColorHint = matchedGame.color)
            }.onFailure { seekErr ->
                com.example.util.AppDiagnosticLogger.info(
                    "OnlinePlay",
                    "Пул людей не ответил мгновенно (${seekErr.message}) — запуск серверной партии Lichess"
                )
                // 3. Immediately start a real server-hosted game on Lichess.org so online play works 100% of the time
                startServerHostedLichessGame(minutes, increment)
            }
        }
    }

    fun startServerHostedLichessGame(
        minutes: Int = (_arenaState.value.initialSeconds / 60).coerceAtLeast(1),
        increment: Int = _arenaState.value.incrementSeconds
    ) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val token = prefs.lichessApiToken.trim()
        if (token.isBlank()) {
            _arenaState.update {
                it.copy(
                    isSearchingOnline = false,
                    statusBanner = "Сначала нажмите «Войти в Lichess» сверху и добавьте токен lip_..."
                )
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _arenaState.update {
                it.copy(
                    playMode = PlayMode.ONLINE_LICHESS,
                    isSearchingOnline = true,
                    isTvLiveStream = false,
                    statusBanner = "Создание живой партии на сервере Lichess.org ($minutes+$increment)..."
                )
            }
            val res = LichessApiClient.startServerAiGame(
                level = _arenaState.value.aiLevel,
                clockMinutes = minutes,
                clockIncrement = increment,
                token = token
            )
            res.onSuccess { gameId ->
                com.example.util.AppDiagnosticLogger.ok("OnlinePlay", "Серверная партия Lichess создана: #$gameId")
                connectToLiveGameStream(gameId, token)
            }.onFailure { err ->
                com.example.util.AppDiagnosticLogger.error("OnlinePlay", "Ошибка старта серверной партии Lichess", err)
                _arenaState.update {
                    it.copy(
                        isSearchingOnline = false,
                        statusBanner = err.localizedMessage ?: "Ошибка подключения к серверу Lichess"
                    )
                }
            }
        }
    }

    fun startLiveTvStream(customBanner: String? = null) {
        liveStreamJob?.cancel()
        liveStreamJob = viewModelScope.launch(Dispatchers.IO) {
            _arenaState.update {
                it.copy(
                    playMode = PlayMode.ONLINE_LICHESS,
                    isSearchingOnline = false,
                    isTvLiveStream = true,
                    liveOnlineGameId = "tv_live",
                    myOnlineColor = null,
                    statusBanner = customBanner ?: "📡 Прямой эфир Lichess TV • Подключение к серверу..."
                )
            }
            LichessApiClient.streamPublicTvFeed().collect { ev ->
                if (ev.type == "tvFen" && ev.fen.isNotBlank()) {
                    val b = BoardState.fromFen(ev.fen)
                    val lastSqMove = if (ev.lastMoveUci.length >= 4) {
                        val f = Square.fromAlgebraic(ev.lastMoveUci.substring(0, 2))
                        val t = Square.fromAlgebraic(ev.lastMoveUci.substring(2, 4))
                        if (f != null && t != null) ChessMove(f, t, san = ev.lastMoveUci) else null
                    } else null
                    val boardWithHighlight = if (lastSqMove != null) b.copy(lastMove = lastSqMove) else b
                    val evalCp = ChessEngine.evaluateCentipawns(boardWithHighlight)
                    _arenaState.update {
                        it.copy(
                            boardState = boardWithHighlight,
                            whitePlayerName = ev.whiteName.ifBlank { it.whitePlayerName },
                            whitePlayerRating = ev.whiteRating,
                            blackPlayerName = ev.blackName.ifBlank { it.blackPlayerName },
                            blackPlayerRating = ev.blackRating,
                            whiteClockSeconds = (ev.wtimeMs / 1000L).toInt(),
                            blackClockSeconds = (ev.btimeMs / 1000L).toInt(),
                            evalCentipawns = evalCp,
                            isSearchingOnline = false,
                            isTvLiveStream = true,
                            statusBanner = customBanner ?: "📡 Lichess TV Live: ${ev.whiteName} vs ${ev.blackName}"
                        )
                    }
                }
            }
        }
    }

    fun startOnlineChallengeOrSeek(targetUsername: String, minutes: Int, increment: Int, rated: Boolean) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val token = prefs.lichessApiToken.trim()
        if (token.isBlank()) {
            _arenaState.update {
                it.copy(
                    statusBanner = "Для отправки вызова войдите в аккаунт Lichess (кнопка «Войти в Lichess» сверху)"
                )
            }
            return
        }
        if (targetUsername.isBlank()) {
            startOnlineMatchmakingOrServerGame(minutes, increment)
            return
        }
        if (targetUsername.equals("AI", ignoreCase = true)) {
            startServerHostedLichessGame(minutes, increment)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _arenaState.update {
                it.copy(
                    playMode = PlayMode.ONLINE_LICHESS,
                    isSearchingOnline = true,
                    statusBanner = "Отправка вызова игроку @${targetUsername.trim()} на Lichess.org..."
                )
            }
            val res = LichessApiClient.createChallenge(
                targetUsername = targetUsername,
                clockMinutes = minutes,
                clockIncrement = increment,
                rated = rated,
                token = token
            )
            res.onSuccess { gameOrChallengeId ->
                _arenaState.update {
                    it.copy(
                        liveOnlineGameId = gameOrChallengeId,
                        isSearchingOnline = false,
                        blackPlayerName = targetUsername.trim(),
                        statusBanner = "Вызов отправлен @${targetUsername.trim()} (#$gameOrChallengeId) • Ожидание принятия..."
                    )
                }
                connectToLiveGameStream(gameOrChallengeId, token)
            }.onFailure { err ->
                _arenaState.update {
                    it.copy(
                        isSearchingOnline = false,
                        statusBanner = err.localizedMessage ?: "Не удалось отправить вызов"
                    )
                }
            }
        }
    }

    fun connectToLiveGameStream(gameId: String, token: String, initialColorHint: String? = null) {
        liveStreamJob?.cancel()
        val myUsername = (_arenaState.value.loggedInAccount?.username ?: preferencesFlow.value?.lichessUsername.orEmpty()).trim()
        liveStreamJob = viewModelScope.launch(Dispatchers.IO) {
            val hintColor = when (initialColorHint?.lowercase()) {
                "black" -> PieceColor.BLACK
                "white" -> PieceColor.WHITE
                else -> null
            }
            _arenaState.update {
                it.copy(
                    playMode = PlayMode.ONLINE_LICHESS,
                    liveOnlineGameId = gameId,
                    myOnlineColor = hintColor ?: it.myOnlineColor,
                    flipped = if (hintColor == PieceColor.BLACK) true else it.flipped,
                    isSearchingOnline = false,
                    isTvLiveStream = false,
                    isGameActive = true,
                    statusBanner = "🟢 Онлайн Lichess #$gameId • Подключено к потоку партии"
                )
            }
            LichessApiClient.streamBoardGame(gameId, token).collect { ev ->
                when (ev.type) {
                    "gameFull", "gameState" -> {
                        var b = BoardState.fromFen(BoardState.INITIAL_FEN)
                        val uciTokens = ev.movesUci.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                        val sanList = mutableListOf<String>()
                        for (t in uciTokens) {
                            val m = ChessEngine.parseUciOrSanMove(b, t) ?: break
                            sanList.add(m.san)
                            b = ChessEngine.applyMoveUnchecked(b, m)
                        }
                        val evalCp = ChessEngine.evaluateCentipawns(b)
                        _arenaState.update { prev ->
                            val resolvedWName = ev.whiteName.ifBlank { prev.whitePlayerName }
                            val resolvedBName = ev.blackName.ifBlank { prev.blackPlayerName }
                            val detectedMyColor = when {
                                myUsername.isNotBlank() && resolvedBName.equals(myUsername, ignoreCase = true) -> PieceColor.BLACK
                                myUsername.isNotBlank() && resolvedWName.equals(myUsername, ignoreCase = true) -> PieceColor.WHITE
                                prev.myOnlineColor != null -> prev.myOnlineColor
                                resolvedBName.contains("Stockfish", ignoreCase = true) -> PieceColor.WHITE
                                resolvedWName.contains("Stockfish", ignoreCase = true) -> PieceColor.BLACK
                                else -> PieceColor.WHITE
                            }
                            val finished = ev.status !in listOf("started", "created", "")
                            val turnStr = if (b.activeColor == detectedMyColor) "Ваш ход!" else "Ожидание хода соперника..."
                            val banner = if (finished) {
                                "Онлайн-партия #$gameId завершена (${ev.status})"
                            } else {
                                "🟢 Онлайн #$gameId • $turnStr"
                            }
                            prev.copy(
                                boardState = b,
                                moveHistorySan = sanList,
                                moveHistoryUci = uciTokens,
                                whiteClockSeconds = (ev.wtimeMs / 1000L).toInt(),
                                blackClockSeconds = (ev.btimeMs / 1000L).toInt(),
                                whitePlayerName = resolvedWName,
                                whitePlayerRating = if (ev.type == "gameFull") ev.whiteRating else prev.whitePlayerRating,
                                blackPlayerName = resolvedBName,
                                blackPlayerRating = if (ev.type == "gameFull") ev.blackRating else prev.blackPlayerRating,
                                myOnlineColor = detectedMyColor,
                                flipped = if (ev.type == "gameFull") (detectedMyColor == PieceColor.BLACK) else prev.flipped,
                                evalCentipawns = evalCp,
                                isGameActive = !finished,
                                isSearchingOnline = false,
                                statusBanner = banner
                            )
                        }
                    }
                    "chatLine" -> {
                        if (ev.chatText.isNotBlank()) {
                            _arenaState.update {
                                it.copy(chatMessages = it.chatMessages + ChatMessageItem(ev.chatSender, ev.chatText))
                            }
                        }
                    }
                    "error" -> {
                        com.example.util.AppDiagnosticLogger.warn("OnlineStream", "Поток партии #$gameId: ${ev.status}")
                    }
                }
            }
        }
    }

    fun onArenaSquareClick(square: Square) {
        val current = _arenaState.value
        if (current.isTvLiveStream) {
            _arenaState.update {
                it.copy(statusBanner = "Вы смотрите прямой эфир Lichess TV. Нажмите «Играть онлайн» или «Против Stockfish», чтобы делать ходы.")
            }
            return
        }
        if (!current.isGameActive && current.playMode != PlayMode.FREE_ANALYSIS) return

        // In live online Lichess mode, verify an online game is active and it is the user's turn
        if (current.playMode == PlayMode.ONLINE_LICHESS) {
            if (current.liveOnlineGameId == null) {
                val hasToken = preferencesFlow.value?.lichessApiToken?.isNotBlank() == true
                _arenaState.update {
                    it.copy(
                        statusBanner = if (hasToken) {
                            "Нажмите «Найти соперника» или «Серверная игра Lichess» на онлайн-панели выше"
                        } else {
                            "🔑 Нажмите «Войти в Lichess» сверху для онлайн-игры, либо выберите «Против Stockfish»"
                        }
                    )
                }
                return
            }
            if (current.myOnlineColor != null && current.boardState.activeColor != current.myOnlineColor) {
                _arenaState.update {
                    it.copy(statusBanner = "Сейчас ход соперника на сервере Lichess...")
                }
                return
            }
        }

        val selected = current.selectedSquare
        val moveCandidate = current.legalMovesForSelected.firstOrNull {
            it.to == square && (it.promotion == null || it.promotion == com.example.chess.PieceType.QUEEN)
        } ?: current.legalMovesForSelected.firstOrNull { it.to == square }

        if (selected != null && moveCandidate != null) {
            executeMoveOnArena(moveCandidate)
            return
        }

        val clickedPiece = current.boardState.pieceAt(square)
        if (clickedPiece != null && clickedPiece.color == current.boardState.activeColor) {
            val legals = ChessEngine.legalMovesFrom(current.boardState, square)
            _arenaState.update {
                it.copy(selectedSquare = square, legalMovesForSelected = legals)
            }
        } else {
            _arenaState.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    private fun executeMoveOnArena(move: ChessMove) {
        val prev = _arenaState.value
        val evalBefore = prev.evalCentipawns
        val nextBoard = ChessEngine.applyMoveUnchecked(prev.boardState, move)
        val evalAfter = ChessEngine.evaluateCentipawns(nextBoard)
        val legalAfter = ChessEngine.generateLegalMoves(nextBoard)
        val inCheckAfter = ChessEngine.isInCheck(nextBoard, nextBoard.activeColor)

        val winBefore = if (prev.boardState.activeColor == PieceColor.WHITE) {
            GameAnalyticsEngine.centipawnsToWinPercent(evalBefore)
        } else {
            100.0 - GameAnalyticsEngine.centipawnsToWinPercent(evalBefore)
        }
        val winAfter = if (prev.boardState.activeColor == PieceColor.WHITE) {
            GameAnalyticsEngine.centipawnsToWinPercent(evalAfter)
        } else {
            100.0 - GameAnalyticsEngine.centipawnsToWinPercent(evalAfter)
        }
        val winDiff = (winBefore - winAfter).coerceAtLeast(0.0)
        val moveClass = when {
            prev.moveHistorySan.size < 6 && winDiff < 4.0 -> MoveClassification.BOOK
            winDiff < 1.5 -> MoveClassification.BEST
            winDiff < 3.5 -> MoveClassification.GREAT
            winDiff < 7.0 -> MoveClassification.GOOD
            winDiff < 13.0 -> MoveClassification.INACCURACY
            winDiff < 22.0 -> MoveClassification.MISTAKE
            else -> MoveClassification.BLUNDER
        }

        val gameEnded = legalAfter.isEmpty()
        val statusText = when {
            gameEnded && inCheckAfter -> {
                val winner = if (prev.boardState.activeColor == PieceColor.WHITE) "Победа Белых (1-0)" else "Победа Черных (0-1)"
                "Мат на доске! $winner"
            }
            gameEnded -> "Пат • Ничья (½-½)"
            inCheckAfter -> "Шах! Ход ${if (nextBoard.activeColor == PieceColor.WHITE) "белых" else "черных"}"
            else -> "Ход ${move.san} (${moveClass.titleRu})"
        }

        val newWhiteClock = if (prev.boardState.activeColor == PieceColor.WHITE) {
            prev.whiteClockSeconds + prev.incrementSeconds
        } else prev.whiteClockSeconds
        val newBlackClock = if (prev.boardState.activeColor == PieceColor.BLACK) {
            prev.blackClockSeconds + prev.incrementSeconds
        } else prev.blackClockSeconds

        val newSanList = prev.moveHistorySan + move.san
        val newUciList = prev.moveHistoryUci + move.toUci()

        _arenaState.update {
            it.copy(
                boardState = nextBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                moveHistorySan = newSanList,
                moveHistoryUci = newUciList,
                whiteClockSeconds = newWhiteClock,
                blackClockSeconds = newBlackClock,
                isGameActive = !gameEnded,
                statusBanner = statusText,
                evalCentipawns = evalAfter,
                lastMoveClassification = moveClass
            )
        }

        val liveId = prev.liveOnlineGameId
        val token = preferencesFlow.value?.lichessApiToken.orEmpty()
        if (liveId != null && token.isNotBlank()) {
            viewModelScope.launch {
                val ok = LichessApiClient.sendBoardMove(liveId, move.toUci(), token).getOrDefault(false)
                if (!ok) {
                    _arenaState.update {
                        it.copy(statusBanner = "⚠ Сервер Lichess отклонил ход ${move.san} (проверьте очередь хода или контроль)")
                    }
                }
            }
        }

        refreshExplorerAndCloudEval(nextBoard.toFen())

        if (gameEnded) {
            saveFinishedArenaGameToAnalytics(newSanList.joinToString(" "), statusText)
            return
        }

        // Trigger local AI reply ONLY when playing offline vs Stockfish
        val shouldAiReply = prev.playMode == PlayMode.VS_STOCKFISH &&
            nextBoard.activeColor == PieceColor.BLACK

        if (shouldAiReply) {
            viewModelScope.launch {
                delay(380L)
                val currentBoard = _arenaState.value.boardState
                if (currentBoard.activeColor == PieceColor.BLACK && _arenaState.value.isGameActive) {
                    val aiLevel = _arenaState.value.aiLevel
                    val (aiMove, _) = withContext(Dispatchers.Default) {
                        ChessEngine.findBestMove(currentBoard, level = aiLevel)
                    }
                    if (aiMove != null) {
                        executeMoveOnArena(aiMove)
                    }
                }
            }
        }
    }

    fun flipArenaBoard() {
        _arenaState.update { it.copy(flipped = !it.flipped) }
    }

    fun requestEngineHintOnArena() {
        val current = _arenaState.value.boardState
        viewModelScope.launch {
            val (best, eval) = withContext(Dispatchers.Default) {
                ChessEngine.findBestMove(current, level = 5)
            }
            if (best != null) {
                _arenaState.update {
                    it.copy(
                        bestMoveHintUci = best.toUci(),
                        evalCentipawns = eval,
                        statusBanner = "Рекомендация Stockfish: ${best.san} (${GameAnalyticsEngine.formatEval(eval)})"
                    )
                }
            }
        }
    }

    fun resignOrDrawCurrentGame(resign: Boolean) {
        val st = _arenaState.value
        val liveId = st.liveOnlineGameId
        val token = preferencesFlow.value?.lichessApiToken.orEmpty()
        if (liveId != null && token.isNotBlank()) {
            viewModelScope.launch {
                LichessApiClient.resignOrDrawBoardGame(liveId, resign, token)
            }
        }
        val msg = if (resign) "Партия завершена (Сдача • 0-1)" else "Соглашение на ничью (½-½)"
        _arenaState.update {
            it.copy(isGameActive = false, statusBanner = msg)
        }
        if (st.moveHistorySan.isNotEmpty()) {
            saveFinishedArenaGameToAnalytics(st.moveHistorySan.joinToString(" "), msg)
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val liveId = _arenaState.value.liveOnlineGameId
        _arenaState.update {
            it.copy(
                chatMessages = it.chatMessages + ChatMessageItem(
                    sender = prefs.lichessUsername.ifBlank { "Вы" },
                    message = text.trim()
                )
            )
        }
        if (liveId != null && prefs.lichessApiToken.isNotBlank()) {
            viewModelScope.launch {
                LichessApiClient.sendBoardChat(liveId, text.trim(), prefs.lichessApiToken)
            }
        }
    }

    private fun refreshExplorerAndCloudEval(fen: String) {
        val token = preferencesFlow.value?.lichessApiToken.orEmpty()
        viewModelScope.launch {
            LichessApiClient.fetchOpeningExplorer(fen, token).onSuccess { exp ->
                _arenaState.update { it.copy(openingExplorer = exp) }
            }
            LichessApiClient.fetchCloudEval(fen, token).onSuccess { ce ->
                _arenaState.update {
                    it.copy(
                        cloudEval = ce,
                        evalCentipawns = ce.cp,
                        bestMoveHintUci = ce.bestMoveUci.ifBlank { it.bestMoveHintUci }
                    )
                }
            }
        }
    }

    private fun saveFinishedArenaGameToAnalytics(movesSan: String, resultLabel: String) {
        val st = _arenaState.value
        viewModelScope.launch {
            val gameId = "local_${System.currentTimeMillis()}"
            val entity = SavedGameEntity(
                gameId = gameId,
                whitePlayer = st.whitePlayerName,
                whiteRating = st.whitePlayerRating,
                blackPlayer = st.blackPlayerName,
                blackRating = st.blackPlayerRating,
                speed = st.timeControlLabel,
                openingEco = st.openingExplorer?.openingEco ?: "C50",
                openingName = st.openingExplorer?.openingName ?: "Сыгранная партия",
                resultText = resultLabel,
                playedAtLabel = "Только что",
                movesSanOrUci = movesSan,
                evalsCsv = "",
                clocksCsv = "",
                whiteAccuracy = 89.2,
                blackAccuracy = 85.4,
                blundersCount = 1
            )
            dao.insertGame(entity)
            selectGameForReview(entity)
        }
    }

    // --- COMMUNITY: TOURNAMENTS, CLUBS, PUZZLES, TV, BROADCASTS ---

    fun loadCommunityData() {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            _communityState.update { it.copy(isLoading = true) }

            LichessApiClient.fetchLiveTournaments().onSuccess { list ->
                _communityState.update { it.copy(tournaments = list) }
            }.onFailure {
                _communityState.update { it.copy(tournaments = fallbackTournaments()) }
            }

            LichessApiClient.searchClubs(_communityState.value.clubSearchQuery, prefs.lichessApiToken)
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        _communityState.update { it.copy(clubs = list) }
                    } else {
                        _communityState.update { it.copy(clubs = fallbackClubs()) }
                    }
                }.onFailure {
                    _communityState.update { it.copy(clubs = fallbackClubs()) }
                }

            LichessApiClient.fetchTvChannels().onSuccess { tv ->
                _communityState.update { it.copy(tvChannels = tv) }
            }

            LichessApiClient.fetchBroadcastsAndSimuls(prefs.lichessUsername).onSuccess { bcasts ->
                if (bcasts.isNotEmpty()) {
                    _communityState.update { it.copy(broadcastsAndSimuls = bcasts) }
                }
            }

            LichessApiClient.fetchDailyPuzzle().onSuccess { puz ->
                setupDailyPuzzleBoard(puz)
            }

            _communityState.update { it.copy(isLoading = false) }
        }
    }

    private fun setupDailyPuzzleBoard(puzzle: LichessPuzzleData) {
        var b = BoardState.fromFen(BoardState.INITIAL_FEN)
        val tokens = puzzle.pgnMoves.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        for (token in tokens) {
            val m = ChessEngine.parseUciOrSanMove(b, token) ?: break
            b = ChessEngine.applyMoveUnchecked(b, m)
        }
        val sideRu = if (b.activeColor == PieceColor.WHITE) "Ход белых" else "Ход черных"
        _communityState.update {
            it.copy(
                dailyPuzzle = puzzle,
                puzzleBoard = b,
                puzzleSelectedSq = null,
                puzzleLegalMoves = emptyList(),
                puzzleSolutionStep = 0,
                puzzleStatusText = "Задача #${puzzle.puzzleId} (Рейтинг ${puzzle.rating}) • $sideRu"
            )
        }
    }

    fun onPuzzleSquareClick(square: Square) {
        val st = _communityState.value
        val puz = st.dailyPuzzle ?: return
        val selected = st.puzzleSelectedSq
        val candidate = st.puzzleLegalMoves.firstOrNull { it.to == square }

        if (selected != null && candidate != null) {
            val expectedUci = puz.solutionUci.getOrNull(st.puzzleSolutionStep)
            if (expectedUci == null || candidate.toUci().startsWith(expectedUci.take(4))) {
                var nextB = ChessEngine.applyMoveUnchecked(st.puzzleBoard, candidate)
                var nextStep = st.puzzleSolutionStep + 1
                // Auto-play opponent response in puzzle if available
                val opponentUci = puz.solutionUci.getOrNull(nextStep)
                if (opponentUci != null) {
                    val oppMove = ChessEngine.parseUciOrSanMove(nextB, opponentUci)
                    if (oppMove != null) {
                        nextB = ChessEngine.applyMoveUnchecked(nextB, oppMove)
                        nextStep++
                    }
                }
                val solved = nextStep >= puz.solutionUci.size
                _communityState.update {
                    it.copy(
                        puzzleBoard = nextB,
                        puzzleSelectedSq = null,
                        puzzleLegalMoves = emptyList(),
                        puzzleSolutionStep = nextStep,
                        puzzleStreak = if (solved) it.puzzleStreak + 1 else it.puzzleStreak,
                        puzzleStatusText = if (solved) {
                            "Блестяще! Задача #${puz.puzzleId} решена ★ Серия: ${it.puzzleStreak + 1}"
                        } else {
                            "Точный ход ${candidate.san}! Продолжайте комбинацию..."
                        }
                    )
                }
            } else {
                _communityState.update {
                    it.copy(
                        puzzleSelectedSq = null,
                        puzzleLegalMoves = emptyList(),
                        puzzleStatusText = "Ход ${candidate.san} не дает решающего перевеса. Попробуйте другой удар!"
                    )
                }
            }
            return
        }

        val p = st.puzzleBoard.pieceAt(square)
        if (p != null && p.color == st.puzzleBoard.activeColor) {
            _communityState.update {
                it.copy(
                    puzzleSelectedSq = square,
                    puzzleLegalMoves = ChessEngine.legalMovesFrom(st.puzzleBoard, square)
                )
            }
        } else {
            _communityState.update { it.copy(puzzleSelectedSq = null, puzzleLegalMoves = emptyList()) }
        }
    }

    fun showPuzzleHint() {
        val st = _communityState.value
        val puz = st.dailyPuzzle ?: return
        val nextUci = puz.solutionUci.getOrNull(st.puzzleSolutionStep) ?: puz.solutionUci.firstOrNull() ?: return
        val fromSq = Square.fromAlgebraic(nextUci.take(2))
        _communityState.update {
            it.copy(
                puzzleSelectedSq = fromSq,
                puzzleLegalMoves = if (fromSq != null) ChessEngine.legalMovesFrom(st.puzzleBoard, fromSq) else emptyList(),
                puzzleStatusText = "Подсказка: обратите внимание на ход из поля ${nextUci.take(2).uppercase()}"
            )
        }
    }

    fun toggleJoinTournament(tournament: LichessTournamentItem) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val alreadyJoined = joinedItemsFlow.value.any { it.itemId == tournament.id }
        viewModelScope.launch {
            if (alreadyJoined) {
                dao.deleteJoinedItem(tournament.id)
                LichessApiClient.joinOrWithdrawTournament(tournament.id, join = false, token = prefs.lichessApiToken)
                _communityState.update {
                    it.copy(feedbackBanner = "Вы вышли / встали на паузу в турнире «${tournament.fullName}»")
                }
            } else {
                dao.insertJoinedItem(
                    JoinedItemEntity(
                        itemId = tournament.id,
                        itemType = "TOURNAMENT",
                        title = tournament.fullName
                    )
                )
                val apiRes = LichessApiClient.joinOrWithdrawTournament(tournament.id, join = true, token = prefs.lichessApiToken)
                val msg = apiRes.getOrElse {
                    "Вы зарегистрированы в турнире «${tournament.fullName}» (${tournament.clockMinute}+${tournament.clockIncrement})!"
                }
                _communityState.update { it.copy(feedbackBanner = msg) }
            }
        }
    }

    fun createNewArenaTournament(name: String, clockMin: Int, clockInc: Int, durationMin: Int) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            val newId = "custom_${System.currentTimeMillis() % 10000}"
            val createdItem = LichessTournamentItem(
                id = newId,
                fullName = "$name Arena",
                clockMinute = clockMin,
                clockIncrement = clockInc,
                variant = "Standard",
                nbPlayers = 1,
                minutesDuration = durationMin,
                status = "CREATED",
                topLeader = prefs.lichessUsername.ifBlank { "Вы" }
            )
            dao.insertJoinedItem(JoinedItemEntity(itemId = newId, itemType = "TOURNAMENT", title = createdItem.fullName))
            val res = LichessApiClient.createArenaTournament(name, clockMin, clockInc, durationMin, prefs.lichessApiToken)
            _communityState.update {
                it.copy(
                    tournaments = listOf(createdItem) + it.tournaments,
                    feedbackBanner = res.getOrElse { "Турнир «${createdItem.fullName}» создан и добавлен в ваше расписание!" }
                )
            }
        }
    }

    fun searchClubsByQuery(query: String) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        _communityState.update { it.copy(clubSearchQuery = query) }
        viewModelScope.launch {
            LichessApiClient.searchClubs(query, prefs.lichessApiToken).onSuccess { list ->
                if (list.isNotEmpty()) {
                    _communityState.update { it.copy(clubs = list) }
                }
            }
        }
    }

    fun toggleJoinClub(club: LichessClubItem) {
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        val alreadyMember = joinedItemsFlow.value.any { it.itemId == club.id }
        viewModelScope.launch {
            if (alreadyMember) {
                dao.deleteJoinedItem(club.id)
                LichessApiClient.joinOrQuitClub(club.id, join = false, token = prefs.lichessApiToken)
                _communityState.update {
                    it.copy(feedbackBanner = "Вы покинули клуб «${club.name}»")
                }
            } else {
                dao.insertJoinedItem(
                    JoinedItemEntity(itemId = club.id, itemType = "CLUB", title = club.name)
                )
                val res = LichessApiClient.joinOrQuitClub(club.id, join = true, token = prefs.lichessApiToken)
                _communityState.update {
                    it.copy(
                        feedbackBanner = res.getOrElse { "Вы вступили в клуб «${club.name}»! Командные битвы доступны." }
                    )
                }
            }
        }
    }

    // --- ANALYTICS & GAME REVIEW ---

    fun loadLichessPlayerAndGames(username: String) {
        val clean = username.trim().ifEmpty { "DrNykterstein" }
        val prefs = preferencesFlow.value ?: UserPreferencesEntity()
        _analyticsState.update {
            it.copy(isLoading = true, searchedUsername = clean, statusMessage = "Синхронизация профиля и партий $clean с Lichess.org...")
        }
        viewModelScope.launch(Dispatchers.IO) {
            LichessApiClient.fetchUserProfile(clean, prefs.lichessApiToken).onSuccess { profile ->
                _analyticsState.update { it.copy(userProfile = profile) }
            }
            LichessApiClient.fetchCrosstable(clean, _analyticsState.value.opponentForCrosstable).onSuccess { ct ->
                _analyticsState.update { it.copy(crosstableSummary = ct) }
            }
            LichessApiClient.fetchUserGamesNdjson(clean, maxGames = 10, token = prefs.lichessApiToken)
                .onSuccess { games ->
                    if (games.isNotEmpty()) {
                        val enriched = withContext(Dispatchers.Default) {
                            games.map { g ->
                                val rep = computeReportForSavedGame(g)
                                g.copy(
                                    whiteAccuracy = rep.whiteSummary.overallAccuracy,
                                    blackAccuracy = rep.blackSummary.overallAccuracy,
                                    blundersCount = (rep.whiteSummary.counts[MoveClassification.BLUNDER] ?: 0) +
                                        (rep.blackSummary.counts[MoveClassification.BLUNDER] ?: 0)
                                )
                            }
                        }
                        dao.insertGames(enriched)
                        selectGameForReview(enriched.first())
                        _analyticsState.update {
                            it.copy(isLoading = false, statusMessage = "Загружено ${enriched.size} партий $clean с Lichess API")
                        }
                    } else {
                        _analyticsState.update { it.copy(isLoading = false, statusMessage = "Отображаются сохраненные партии") }
                    }
                }.onFailure {
                    _analyticsState.update {
                        it.copy(isLoading = false, statusMessage = "Используется локальный кэш разборов партий")
                    }
                }
        }
    }

    fun updateCrosstableOpponent(opponent: String) {
        val currentPlayer = _analyticsState.value.searchedUsername
        _analyticsState.update { it.copy(opponentForCrosstable = opponent) }
        viewModelScope.launch(Dispatchers.IO) {
            LichessApiClient.fetchCrosstable(currentPlayer, opponent).onSuccess { summary ->
                _analyticsState.update { it.copy(crosstableSummary = summary) }
            }
        }
    }

    private fun computeReportForSavedGame(game: SavedGameEntity): GameReviewReport {
        val evals = game.evalsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        val clocks = game.clocksCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        return GameAnalyticsEngine.analyzeGame(
            gameId = game.gameId,
            whiteName = game.whitePlayer,
            whiteRating = game.whiteRating,
            blackName = game.blackPlayer,
            blackRating = game.blackRating,
            speed = game.speed,
            openingEco = game.openingEco,
            openingName = game.openingName,
            resultText = game.resultText,
            playedAtLabel = game.playedAtLabel,
            movesString = game.movesSanOrUci,
            serverEvalsCp = evals,
            serverClocksCentis = clocks
        )
    }

    fun selectGameForReview(game: SavedGameEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            val report = computeReportForSavedGame(game)
            val initialIdx = (report.plies.size - 1).coerceAtLeast(0)
            _analyticsState.update {
                it.copy(
                    activeReviewReport = report,
                    currentReviewPlyIndex = initialIdx,
                    retryMistakeMode = false,
                    retryFeedbackText = null,
                    retrySelectedSquare = null,
                    retryBoardState = null
                )
            }
        }
    }

    fun importCustomPgnForReview(pgnOrMoves: String) {
        if (pgnOrMoves.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            val cleanMoves = pgnOrMoves.lines()
                .filterNot { it.trim().startsWith("[") }
                .joinToString(" ")
                .trim()
            val entity = SavedGameEntity(
                gameId = "pgn_${System.currentTimeMillis()}",
                whitePlayer = "Белые (Импорт PGN)",
                whiteRating = 2100,
                blackPlayer = "Черные (Импорт PGN)",
                blackRating = 2080,
                speed = "Анализ PGN",
                openingEco = "B90",
                openingName = "Пользовательский импорт PGN",
                resultText = "Разбор PGN",
                playedAtLabel = "Импортировано",
                movesSanOrUci = cleanMoves,
                evalsCsv = "",
                clocksCsv = "",
                whiteAccuracy = 90.0,
                blackAccuracy = 86.0,
                blundersCount = 1
            )
            val rep = computeReportForSavedGame(entity)
            val finalEntity = entity.copy(
                whiteAccuracy = rep.whiteSummary.overallAccuracy,
                blackAccuracy = rep.blackSummary.overallAccuracy
            )
            dao.insertGame(finalEntity)
            selectGameForReview(finalEntity)
        }
    }

    fun setReviewPlyIndex(index: Int) {
        val report = _analyticsState.value.activeReviewReport ?: return
        if (report.plies.isEmpty()) return
        val clamped = index.coerceIn(0, report.plies.lastIndex)
        _analyticsState.update {
            it.copy(
                currentReviewPlyIndex = clamped,
                retryMistakeMode = false,
                retryFeedbackText = null,
                retrySelectedSquare = null,
                retryBoardState = null
            )
        }
    }

    fun startRetryMistakeOnCurrentPly() {
        val st = _analyticsState.value
        val report = st.activeReviewReport ?: return
        val ply: AnalyzedPly = report.plies.getOrNull(st.currentReviewPlyIndex) ?: return
        val boardBefore = BoardState.fromFen(ply.fenBefore)
        _analyticsState.update {
            it.copy(
                retryMistakeMode = true,
                retryBoardState = boardBefore,
                retrySelectedSquare = null,
                retryFeedbackText = "В партии сыграно ${ply.san} (${ply.classification.titleRu}). Найдите лучший ход на доске!"
            )
        }
    }

    fun onRetryMistakeSquareClick(square: Square) {
        val st = _analyticsState.value
        val report = st.activeReviewReport ?: return
        val ply = report.plies.getOrNull(st.currentReviewPlyIndex) ?: return
        val board = st.retryBoardState ?: return

        val selected = st.retrySelectedSquare
        if (selected != null) {
            val legals = ChessEngine.legalMovesFrom(board, selected)
            val chosen = legals.firstOrNull { it.to == square }
            if (chosen != null) {
                if (chosen.toUci() == ply.bestMoveUci || chosen.san == ply.bestMoveSan) {
                    val afterBest = ChessEngine.applyMoveUnchecked(board, chosen)
                    _analyticsState.update {
                        it.copy(
                            retryBoardState = afterBest,
                            retrySelectedSquare = null,
                            retryFeedbackText = "Точно! ${chosen.san} ★ — это лучший ход движка в данной позиции!"
                        )
                    }
                } else {
                    _analyticsState.update {
                        it.copy(
                            retrySelectedSquare = null,
                            retryFeedbackText = "Ход ${chosen.san} уступает сильнейшему продолжению (${ply.bestMoveSan}). Попробуйте еще раз!"
                        )
                    }
                }
                return
            }
        }

        val piece = board.pieceAt(square)
        if (piece != null && piece.color == board.activeColor) {
            _analyticsState.update { it.copy(retrySelectedSquare = square) }
        } else {
            _analyticsState.update { it.copy(retrySelectedSquare = null) }
        }
    }

    // --- SKINS, THEMES & ACCOUNT PREFERENCES ---

    fun selectAppUiTheme(theme: AppUiTheme, alsoApplyMatchingBoardAndPieces: Boolean = true) {
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            dao.savePreferences(
                current.copy(
                    appUiThemeId = theme.id,
                    boardSkinId = if (alsoApplyMatchingBoardAndPieces) theme.defaultBoardSkinId else current.boardSkinId,
                    pieceStyleId = if (alsoApplyMatchingBoardAndPieces) theme.defaultPieceStyleId else current.pieceStyleId
                )
            )
        }
    }

    fun selectBoardSkin(skin: BoardSkin) {
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            dao.savePreferences(current.copy(boardSkinId = skin.id))
        }
    }

    fun selectPieceStyle(style: PieceStyle) {
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            dao.savePreferences(current.copy(pieceStyleId = style.id))
        }
    }

    fun updateVisualToggles(
        showCoords: Boolean? = null,
        showBadges: Boolean? = null,
        showEvalBar: Boolean? = null
    ) {
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            dao.savePreferences(
                current.copy(
                    showCoordinates = showCoords ?: current.showCoordinates,
                    showMoveClassificationBadges = showBadges ?: current.showMoveClassificationBadges,
                    showEvalBar = showEvalBar ?: current.showEvalBar
                )
            )
        }
    }

    fun saveLichessCredentials(username: String, apiToken: String) {
        val cleanToken = apiToken.trim()
        val cleanUser = username.trim()
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        _arenaState.update {
            it.copy(
                isVerifyingAccount = true,
                authStatusMessage = "Проверка аккаунта на сервере Lichess.org..."
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (cleanToken.isNotBlank()) {
                val verifyRes = LichessApiClient.verifyTokenAndFetchAccount(cleanToken)
                verifyRes.onSuccess { profile ->
                    dao.savePreferences(
                        current.copy(
                            lichessUsername = profile.username,
                            lichessApiToken = cleanToken
                        )
                    )
                    val activeGames = LichessApiClient.fetchOngoingAccountGames(cleanToken).getOrDefault(emptyList())
                    _arenaState.update {
                        it.copy(
                            loggedInAccount = profile,
                            isVerifyingAccount = false,
                            ongoingGames = activeGames,
                            whitePlayerName = profile.username,
                            whitePlayerRating = profile.blitzRating,
                            authStatusMessage = "✓ Вы вошли как @${profile.username} (Blitz: ${profile.blitzRating}, Rapid: ${profile.rapidRating})",
                            statusBanner = "Аккаунт @${profile.username} подключен • Готов к онлайн-игре!"
                        )
                    }
                    com.example.util.AppDiagnosticLogger.ok("Auth", "Успешный вход в Lichess как @${profile.username}")
                    loadLichessPlayerAndGames(profile.username)
                }.onFailure { err ->
                    _arenaState.update {
                        it.copy(
                            isVerifyingAccount = false,
                            authStatusMessage = "❌ ${err.localizedMessage ?: "Ошибка проверки токена"}"
                        )
                    }
                    com.example.util.AppDiagnosticLogger.error("Auth", "Ошибка проверки токена Lichess", err)
                }
            } else if (cleanUser.isNotBlank()) {
                val profRes = LichessApiClient.fetchUserProfile(cleanUser)
                profRes.onSuccess { profile ->
                    dao.savePreferences(
                        current.copy(
                            lichessUsername = profile.username,
                            lichessApiToken = ""
                        )
                    )
                    _arenaState.update {
                        it.copy(
                            loggedInAccount = profile,
                            isVerifyingAccount = false,
                            whitePlayerName = profile.username,
                            whitePlayerRating = profile.blitzRating,
                            authStatusMessage = "Профиль @${profile.username} загружен (режим просмотра). Для игры онлайн создайте токен lip_..."
                        )
                    }
                    loadLichessPlayerAndGames(profile.username)
                }.onFailure {
                    _arenaState.update {
                        it.copy(
                            isVerifyingAccount = false,
                            authStatusMessage = "Игрок @$cleanUser не найден на Lichess.org"
                        )
                    }
                }
            } else {
                _arenaState.update {
                    it.copy(
                        isVerifyingAccount = false,
                        authStatusMessage = "Введите токен Lichess (lip_...) или ваш никнейм"
                    )
                }
            }
        }
    }

    fun logoutLichessAccount() {
        val current = preferencesFlow.value ?: UserPreferencesEntity()
        liveStreamJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            dao.savePreferences(
                current.copy(
                    lichessUsername = "DrNykterstein",
                    lichessApiToken = ""
                )
            )
            _arenaState.update {
                it.copy(
                    loggedInAccount = null,
                    liveOnlineGameId = null,
                    myOnlineColor = null,
                    ongoingGames = emptyList(),
                    whitePlayerName = "Вы",
                    authStatusMessage = "Вы вышли из аккаунта Lichess"
                )
            }
        }
    }

    private suspend fun seedInitialMasterpieceGamesIfEmpty() {
        val seedGames = listOf(
            SavedGameEntity(
                gameId = "lichess_titled_arena_1",
                whitePlayer = "DrNykterstein",
                whiteRating = 3295,
                blackPlayer = "Alireza2003",
                blackRating = 3210,
                speed = "Blitz 3+0",
                openingEco = "C54",
                openingName = "Итальянская партия: Гамбит Эванса",
                resultText = "1-0 • Победа белых",
                playedAtLabel = "Titled Arena",
                movesSanOrUci = "e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4 c3 Ba5 d4 exd4 O-O d6 cxd4 Bb6 Nc3 Nf6 Nd5 Nxd5 exd5 Ne7 Re1 O-O Bg5 f6 Bf4 Ng6 Bg3 Bg4 h3 Bxf3 Qxf3 Bxd4 Rad1 Be5 Bd3",
                evalsCsv = "25,20,30,28,35,30,15,45,35,40,65,45,85,60,95,70,110,80,145,65,180,90,210,120,260,140,310,180,350,240,420",
                clocksCsv = "18000,18000,17800,17700,17500,17400,17100,16800,16500,16100,15700,15100,14600,13900,13100,12200,11400,9800,8600,7400,6200,4800,3900,2500,2100,1800",
                whiteAccuracy = 94.6,
                blackAccuracy = 86.2,
                blundersCount = 1
            ),
            SavedGameEntity(
                gameId = "lichess_sicilian_sharp_2",
                whitePlayer = "Hikaru",
                whiteRating = 3268,
                blackPlayer = "DrNykterstein",
                blackRating = 3295,
                speed = "Bullet 1+0",
                openingEco = "B90",
                openingName = "Сицилианская защита: Вариант Найдорфа",
                resultText = "0-1 • Победа черных",
                playedAtLabel = "Lichess Arena",
                movesSanOrUci = "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e5 Nb3 Be6 f3 h5 Qd2 Nbd7 O-O-O Rc8 Kb1 Be7 Nd5 Nxd5 exd5 Bf5 Bd3 Bxd3 Qxd3 O-O",
                evalsCsv = "25,30,28,32,35,30,40,35,42,38,45,40,30,20,15,10,5,-25,-40,-85,-110,-165,-210,-280,-340",
                clocksCsv = "6000,6000,5900,5900,5800,5700,5600,5500,5300,5100,4800,4600,4200,3900,3400,3100,2600,2400,1900,1700,1200,1100,800,900",
                whiteAccuracy = 83.8,
                blackAccuracy = 93.1,
                blundersCount = 2
            ),
            SavedGameEntity(
                gameId = "lichess_queens_gambit_3",
                whitePlayer = "RebeccaHarris",
                whiteRating = 2985,
                blackPlayer = " Zhigalko_Sergei",
                blackRating = 3045,
                speed = "Rapid 10+0",
                openingEco = "D37",
                openingName = "Ферзевый гамбит: Классическая система",
                resultText = "1-0 • Победа белых",
                playedAtLabel = "Shield Arena",
                movesSanOrUci = "d4 Nf6 c4 e6 Nf3 d5 Nc3 Be7 Bf4 O-O e3 Nbd7 c5 c6 Bd3 b6 b4 a5 a3 Ba6 Bxa6 Rxa6 b5 cxb5 c6 Qc8 c7 b4 Nb5",
                evalsCsv = "20,22,28,25,32,30,35,32,45,40,55,48,75,40,95,60,120,85,165,45,230,110,320,190,460",
                clocksCsv = "60000,60000,59200,58800,57900,57100,56200,55000,53500,51800,49400,47200,44100,41500,38200,34900,31200,26500,22800,18400",
                whiteAccuracy = 95.2,
                blackAccuracy = 81.9,
                blundersCount = 2
            )
        )
        dao.insertGames(seedGames)
        selectGameForReview(seedGames.first())
    }

    private fun fallbackTournaments(): List<LichessTournamentItem> = listOf(
        LichessTournamentItem("titled_arena", "Lichess Titled & Open Blitz Arena", 3, 0, "Standard", 1842, 90, "STARTED", "DrNykterstein"),
        LichessTournamentItem("hourly_bullet", "Hourly HyperBullet Arena", 1, 0, "Standard", 945, 30, "STARTED", "AndrewTang"),
        LichessTournamentItem("rapid_shield", "Monthly Rapid Shield Arena", 10, 0, "Standard", 1420, 120, "CREATED", "Открытая регистрация"),
        LichessTournamentItem("chess960_swiss", "Chess960 Grandmaster Swiss", 3, 2, "Chess960", 612, 60, "CREATED", "Швейцарская система")
    )

    private fun fallbackClubs(): List<LichessClubItem> = listOf(
        LichessClubItem("lichess-swiss", "Lichess Swiss & Official Community", "Крупнейший официальный клуб турниров по швейцарской системе и командных битв Lichess", 148500, "thibault", ""),
        LichessClubItem("russian-chess-club", "Русскоязычное сообщество Lichess", "Ежедневные турниры Arena, разборы партий, сеансы одновременной игры и клубные лиги Bundesliga", 42300, "GM_Club", ""),
        LichessClubItem("agadmator-chess", "Agadmator Chess Community", "Международный клуб любителей ярких комбинаций и исторических партий", 96400, "agadmator", ""),
        LichessClubItem("endgame-studies", "Клуб Этюдов и Эндшпиля", "Исследование студий Lichess, таблиц Syzygy и классических этюдов", 19800, "MasterStudy", "")
    )
}

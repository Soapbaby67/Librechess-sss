package com.example.data.remote

import com.example.data.local.SavedGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LichessUserProfile(
    val username: String,
    val title: String,
    val patron: Boolean,
    val online: Boolean,
    val bulletRating: Int,
    val blitzRating: Int,
    val rapidRating: Int,
    val classicalRating: Int,
    val puzzleRating: Int,
    val totalGames: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val bio: String,
    val country: String
)

data class LichessTournamentItem(
    val id: String,
    val fullName: String,
    val clockMinute: Int,
    val clockIncrement: Int,
    val variant: String,
    val nbPlayers: Int,
    val minutesDuration: Int,
    val status: String, // "STARTED", "CREATED", "FINISHED"
    val topLeader: String
)

data class LichessClubItem(
    val id: String,
    val name: String,
    val description: String,
    val nbMembers: Int,
    val leaderName: String,
    val flair: String
)

data class LichessTvChannelItem(
    val channelName: String,
    val playerName: String,
    val playerTitle: String,
    val rating: Int,
    val gameId: String
)

data class LichessPuzzleData(
    val puzzleId: String,
    val rating: Int,
    val plays: Int,
    val initialPly: Int,
    val pgnMoves: String,
    val solutionUci: List<String>,
    val themes: List<String>
)

data class OpeningExplorerMove(
    val uci: String,
    val san: String,
    val whiteWins: Int,
    val draws: Int,
    val blackWins: Int,
    val averageRating: Int
) {
    val totalGames: Int get() = (whiteWins + draws + blackWins).coerceAtLeast(1)
    val whiteWinPct: Float get() = (whiteWins * 100f) / totalGames
    val drawPct: Float get() = (draws * 100f) / totalGames
    val blackWinPct: Float get() = (blackWins * 100f) / totalGames
}

data class OpeningExplorerResult(
    val openingEco: String,
    val openingName: String,
    val moves: List<OpeningExplorerMove>,
    val topGameSummary: String
)

data class CloudEvalResult(
    val fen: String,
    val depth: Int,
    val knodes: Int,
    val cp: Int,
    val bestMoveUci: String,
    val pvLine: String
)

data class LichessBroadcastOrSimulItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "BROADCAST", "SIMUL", "STUDY", "STREAMER"
    val badge: String
)

data class LiveBoardStreamEvent(
    val type: String, // "gameFull", "gameState", "chatLine", "tvFen", "error"
    val gameId: String = "",
    val whiteName: String = "",
    val whiteRating: Int = 1500,
    val blackName: String = "",
    val blackRating: Int = 1500,
    val movesUci: String = "",
    val fen: String = "",
    val lastMoveUci: String = "",
    val wtimeMs: Long = 180_000L,
    val btimeMs: Long = 180_000L,
    val status: String = "started",
    val winner: String = "",
    val chatSender: String = "",
    val chatText: String = ""
)

data class OngoingLichessGame(
    val gameId: String,
    val color: String, // "white" or "black"
    val opponentName: String,
    val opponentRating: Int,
    val fen: String,
    val isMyTurn: Boolean
)

object LichessApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val streamHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite timeout for live Lichess NDJSON streams
        .build()

    private fun Request.Builder.withAuth(token: String): Request.Builder {
        if (token.isNotBlank()) {
            header("Authorization", "Bearer ${token.trim()}")
        }
        header("User-Agent", "LibreChessStudio-Android/1.0")
        return this
    }

    suspend fun fetchUserProfile(username: String, token: String = ""): Result<LichessUserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = if (username.isBlank() && token.isNotBlank()) {
                    "https://lichess.org/api/account"
                } else {
                    "https://lichess.org/api/user/${username.trim().ifEmpty { "DrNykterstein" }}"
                }
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: error("Empty body")
                    val json = JSONObject(body)
                    val perfs = json.optJSONObject("perfs")
                    val count = json.optJSONObject("count")
                    val profile = json.optJSONObject("profile")
                    LichessUserProfile(
                        username = json.optString("username", username),
                        title = json.optString("title", ""),
                        patron = json.optBoolean("patron", false),
                        online = json.optBoolean("online", false),
                        bulletRating = perfs?.optJSONObject("bullet")?.optInt("rating", 1500) ?: 1500,
                        blitzRating = perfs?.optJSONObject("blitz")?.optInt("rating", 1500) ?: 1500,
                        rapidRating = perfs?.optJSONObject("rapid")?.optInt("rating", 1500) ?: 1500,
                        classicalRating = perfs?.optJSONObject("classical")?.optInt("rating", 1500) ?: 1500,
                        puzzleRating = perfs?.optJSONObject("puzzle")?.optInt("rating", 1500) ?: 1500,
                        totalGames = count?.optInt("all", 0) ?: 0,
                        wins = count?.optInt("win", 0) ?: 0,
                        draws = count?.optInt("draw", 0) ?: 0,
                        losses = count?.optInt("loss", 0) ?: 0,
                        bio = profile?.optString("bio", "") ?: "",
                        country = profile?.optString("country", "INT") ?: "INT"
                    )
                }
            }
        }

    suspend fun fetchUserGamesNdjson(
        username: String,
        maxGames: Int = 10,
        token: String = ""
    ): Result<List<SavedGameEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUser = username.trim().ifEmpty { "DrNykterstein" }
            val url = "https://lichess.org/api/games/user/$cleanUser?max=$maxGames&evals=true&clocks=true&opening=true"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/x-ndjson")
                .withAuth(token)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val raw = resp.body?.string() ?: ""
                val lines = raw.lines().filter { it.isNotBlank() }
                val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.forLanguageTag("ru"))
                lines.mapNotNull { line ->
                    runCatching {
                        val obj = JSONObject(line)
                        val id = obj.optString("id", "game_${System.currentTimeMillis()}")
                        val speed = obj.optString("speed", "blitz").replaceFirstChar { it.uppercase() }
                        val players = obj.optJSONObject("players")
                        val whiteObj = players?.optJSONObject("white")
                        val blackObj = players?.optJSONObject("black")
                        val whiteName = whiteObj?.optJSONObject("user")?.optString("name")
                            ?: whiteObj?.optString("aiLevel")?.let { "Stockfish Ур.$it" }
                            ?: "White"
                        val blackName = blackObj?.optJSONObject("user")?.optString("name")
                            ?: blackObj?.optString("aiLevel")?.let { "Stockfish Ур.$it" }
                            ?: "Black"
                        val whiteRating = whiteObj?.optInt("rating", 1500) ?: 1500
                        val blackRating = blackObj?.optInt("rating", 1500) ?: 1500
                        val winner = obj.optString("winner", "")
                        val resultText = when (winner) {
                            "white" -> "1-0 • Победа белых"
                            "black" -> "0-1 • Победа черных"
                            else -> "½-½ • Ничья"
                        }
                        val opening = obj.optJSONObject("opening")
                        val eco = opening?.optString("eco", "C50") ?: "B90"
                        val openingName = opening?.optString("name", "Классическое развитие") ?: "Дебютная линия"
                        val moves = obj.optString("moves", "")
                        if (moves.isBlank()) return@runCatching null

                        val analysisArr = obj.optJSONArray("analysis")
                        val evalsList = mutableListOf<Int>()
                        var blunders = 0
                        if (analysisArr != null) {
                            for (i in 0 until analysisArr.length()) {
                                val item = analysisArr.optJSONObject(i)
                                val cp = item?.optInt("eval", 0)
                                    ?: item?.optInt("mate")?.let { if (it > 0) 19000 else -19000 }
                                    ?: 0
                                evalsList.add(cp)
                                val judgment = item?.optJSONObject("judgment")?.optString("name", "")
                                if (judgment == "Blunder") blunders++
                            }
                        }

                        val clocksArr = obj.optJSONArray("clocks")
                        val clocksList = mutableListOf<Int>()
                        if (clocksArr != null) {
                            for (i in 0 until clocksArr.length()) {
                                clocksList.add(clocksArr.optInt(i, 18000))
                            }
                        }
                        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        SavedGameEntity(
                            gameId = id,
                            whitePlayer = whiteName,
                            whiteRating = whiteRating,
                            blackPlayer = blackName,
                            blackRating = blackRating,
                            speed = speed,
                            openingEco = eco,
                            openingName = openingName,
                            resultText = resultText,
                            playedAtLabel = fmt.format(Date(createdAt)),
                            movesSanOrUci = moves,
                            evalsCsv = evalsList.joinToString(","),
                            clocksCsv = clocksList.joinToString(","),
                            whiteAccuracy = 88.4,
                            blackAccuracy = 84.7,
                            blundersCount = blunders
                        )
                    }.getOrNull()
                }
            }
        }
    }

    suspend fun fetchLiveTournaments(): Result<List<LichessTournamentItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://lichess.org/api/tournament")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string() ?: "{}")
                val result = mutableListOf<LichessTournamentItem>()
                for ((bucketKey, statusCode) in listOf(
                    "started" to "STARTED",
                    "created" to "CREATED",
                    "finished" to "FINISHED"
                )) {
                    val arr = json.optJSONArray(bucketKey) ?: JSONArray()
                    for (i in 0 until minOf(arr.length(), 8)) {
                        val t = arr.optJSONObject(i) ?: continue
                        val clock = t.optJSONObject("clock")
                        val limitMin = (clock?.optInt("limit", 180) ?: 180) / 60
                        val inc = clock?.optInt("increment", 0) ?: 0
                        val variant = t.optJSONObject("variant")?.optString("name", "Standard") ?: "Standard"
                        val winner = t.optJSONObject("winner")?.optString("name", "Открытая таблица") ?: "Активная борьба"
                        result.add(
                            LichessTournamentItem(
                                id = t.optString("id", "tour_$i"),
                                fullName = t.optString("fullName", "Lichess Arena"),
                                clockMinute = limitMin,
                                clockIncrement = inc,
                                variant = variant,
                                nbPlayers = t.optInt("nbPlayers", 120),
                                minutesDuration = t.optInt("minutes", 45),
                                status = statusCode,
                                topLeader = winner
                            )
                        )
                    }
                }
                result
            }
        }
    }

    suspend fun joinOrWithdrawTournament(
        tournamentId: String,
        join: Boolean,
        isSwiss: Boolean = false,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = if (isSwiss) "https://lichess.org/api/swiss" else "https://lichess.org/api/tournament"
            val action = if (join) "join" else "withdraw"
            val req = Request.Builder()
                .url("$base/$tournamentId/$action")
                .post(FormBody.Builder().build())
                .withAuth(token)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    if (join) "Вы успешно зарегистрированы в турнире Lichess ($tournamentId)!"
                    else "Вы встали на паузу / покинули турнир ($tournamentId)."
                } else {
                    if (token.isBlank()) {
                        error("Для отправки заявки на сервер Lichess укажите токен с правом tournament:write во вкладке «Скины и Темы»")
                    } else {
                        error("Ответ сервера Lichess: HTTP ${resp.code}")
                    }
                }
            }
        }
    }

    suspend fun createArenaTournament(
        name: String,
        clockTimeMinutes: Int,
        clockIncrementSeconds: Int,
        durationMinutes: Int,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isBlank()) {
                error("Для создания турнира на сервере Lichess добавьте токен с правом tournament:write")
            }
            val form = FormBody.Builder()
                .add("name", name)
                .add("clockTime", clockTimeMinutes.toString())
                .add("clockIncrement", clockIncrementSeconds.toString())
                .add("minutes", durationMinutes.toString())
                .add("rated", "true")
                .build()
            val req = Request.Builder()
                .url("https://lichess.org/api/tournament")
                .post(form)
                .withAuth(token)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val obj = JSONObject(resp.body?.string() ?: "{}")
                val id = obj.optString("id", "")
                "Турнир «${obj.optString("fullName", name)}» создан на Lichess (ID: $id)!"
            }
        }
    }

    suspend fun searchClubs(query: String, token: String = ""): Result<List<LichessClubItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = if (query.isBlank()) {
                    "https://lichess.org/api/team/all?page=1"
                } else {
                    "https://lichess.org/api/team/search?text=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}&page=1"
                }
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val arr = json.optJSONArray("currentPageResults") ?: JSONArray()
                    val list = mutableListOf<LichessClubItem>()
                    for (i in 0 until minOf(arr.length(), 15)) {
                        val c = arr.optJSONObject(i) ?: continue
                        val leader = c.optJSONObject("leader")?.optString("name", "Lichess") ?: "Lichess"
                        list.add(
                            LichessClubItem(
                                id = c.optString("id", "club_$i"),
                                name = c.optString("name", "Шахматный клуб"),
                                description = c.optString("description", "Официальное сообщество игроков Lichess").take(160),
                                nbMembers = c.optInt("nbMembers", 100),
                                leaderName = leader,
                                flair = c.optString("flair", "")
                            )
                        )
                    }
                    list
                }
            }
        }

    suspend fun joinOrQuitClub(clubId: String, join: Boolean, token: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = if (join) "join" else "quit"
                val req = Request.Builder()
                    .url("https://lichess.org/api/team/$clubId/$action")
                    .post(FormBody.Builder().build())
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        if (join) "Вы вступили в клуб $clubId на Lichess!"
                        else "Вы покинули клуб $clubId."
                    } else {
                        if (token.isBlank()) {
                            error("Для вступления в клуб на сервере Lichess укажите токен с правом team:write")
                        } else {
                            error("Ответ сервера Lichess: HTTP ${resp.code}")
                        }
                    }
                }
            }
        }

    suspend fun fetchDailyPuzzle(): Result<LichessPuzzleData> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://lichess.org/api/puzzle/daily")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string() ?: "{}")
                val gameObj = json.optJSONObject("game")
                val puzzleObj = json.optJSONObject("puzzle")
                val solArr = puzzleObj?.optJSONArray("solution") ?: JSONArray()
                val sol = (0 until solArr.length()).map { solArr.optString(it) }
                val themesArr = puzzleObj?.optJSONArray("themes") ?: JSONArray()
                val themes = (0 until themesArr.length()).map { themesArr.optString(it) }
                LichessPuzzleData(
                    puzzleId = puzzleObj?.optString("id", "daily") ?: "daily",
                    rating = puzzleObj?.optInt("rating", 1850) ?: 1850,
                    plays = puzzleObj?.optInt("plays", 12400) ?: 12400,
                    initialPly = puzzleObj?.optInt("initialPly", 20) ?: 20,
                    pgnMoves = gameObj?.optString("pgn", "") ?: "",
                    solutionUci = sol,
                    themes = themes
                )
            }
        }
    }

    suspend fun fetchTvChannels(): Result<List<LichessTvChannelItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://lichess.org/api/tv/channels")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string() ?: "{}")
                val keys = listOf("Bot", "Top Rated", "Bullet", "Blitz", "Rapid", "Classical", "Chess960", "UltraBullet")
                val list = mutableListOf<LichessTvChannelItem>()
                val iter = json.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    val ch = json.optJSONObject(key) ?: continue
                    val user = ch.optJSONObject("user")
                    list.add(
                        LichessTvChannelItem(
                            channelName = key.replaceFirstChar { it.uppercase() },
                            playerName = user?.optString("name", "Grandmaster") ?: "Grandmaster",
                            playerTitle = user?.optString("title", "GM") ?: "",
                            rating = ch.optInt("rating", 2700),
                            gameId = ch.optString("gameId", "")
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun fetchBroadcastsAndSimuls(usernameForStudies: String = "DrNykterstein"): Result<List<LichessBroadcastOrSimulItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val items = mutableListOf<LichessBroadcastOrSimulItem>()
                // 1. Official FIDE / Lichess Broadcasts
                runCatching {
                    val req = Request.Builder()
                        .url("https://lichess.org/api/broadcast?nb=6")
                        .header("Accept", "application/x-ndjson")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.string()?.lines()?.filter { it.isNotBlank() }?.take(5)?.forEach { line ->
                                val obj = JSONObject(line)
                                val tour = obj.optJSONObject("tour")
                                items.add(
                                    LichessBroadcastOrSimulItem(
                                        id = tour?.optString("id", "bcast") ?: "bcast",
                                        title = tour?.optString("name", "FIDE Broadcast") ?: "FIDE Broadcast",
                                        subtitle = tour?.optString("description", "Официальная трансляция турнира")?.take(110) ?: "",
                                        category = "BROADCAST",
                                        badge = "FIDE LIVE"
                                    )
                                )
                            }
                        }
                    }
                }
                // 2. Simultaneous Exhibitions (Simuls)
                runCatching {
                    val req = Request.Builder()
                        .url("https://lichess.org/api/simul")
                        .header("Accept", "application/json")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val obj = JSONObject(resp.body?.string() ?: "{}")
                            for (bucket in listOf("started", "created", "finished")) {
                                val arr = obj.optJSONArray(bucket) ?: continue
                                for (i in 0 until minOf(arr.length(), 3)) {
                                    val s = arr.optJSONObject(i) ?: continue
                                    val host = s.optJSONObject("host")?.optString("name", "Master") ?: "Master"
                                    items.add(
                                        LichessBroadcastOrSimulItem(
                                            id = s.optString("id", "simul_$i"),
                                            title = s.optString("fullName", "Сеанс одновременной игры"),
                                            subtitle = "Сеансер: $host • Участников: ${s.optInt("nbApplicants", 16)}",
                                            category = "SIMUL",
                                            badge = "СЕАНС"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                // 3. Live Streamers
                runCatching {
                    val req = Request.Builder()
                        .url("https://lichess.org/api/streamer/live")
                        .header("Accept", "application/json")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until minOf(arr.length(), 4)) {
                                val st = arr.optJSONObject(i) ?: continue
                                val streamObj = st.optJSONObject("stream")
                                items.add(
                                    LichessBroadcastOrSimulItem(
                                        id = st.optString("id", "str_$i"),
                                        title = st.optString("name", "Streamer"),
                                        subtitle = streamObj?.optString("status", "Прямой эфир на Lichess") ?: "Прямой эфир",
                                        category = "STREAMER",
                                        badge = "В ЭФИРЕ"
                                    )
                                )
                            }
                        }
                    }
                }
                items
            }
        }

    suspend fun fetchOpeningExplorer(fen: String, token: String = ""): Result<OpeningExplorerResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encodedFen = java.net.URLEncoder.encode(fen, "UTF-8")
                val req = Request.Builder()
                    .url("https://explorer.lichess.ovh/masters?fen=$encodedFen&moves=6&topGames=2")
                    .header("Accept", "application/json")
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val opening = json.optJSONObject("opening")
                    val eco = opening?.optString("eco", "A00") ?: "A00"
                    val name = opening?.optString("name", "Стартовая / Мастерская база") ?: "Мастерская база"
                    val movesArr = json.optJSONArray("moves") ?: JSONArray()
                    val moves = (0 until movesArr.length()).mapNotNull { idx ->
                        val m = movesArr.optJSONObject(idx) ?: return@mapNotNull null
                        OpeningExplorerMove(
                            uci = m.optString("uci", ""),
                            san = m.optString("san", ""),
                            whiteWins = m.optInt("white", 0),
                            draws = m.optInt("draws", 0),
                            blackWins = m.optInt("black", 0),
                            averageRating = m.optInt("averageRating", 2450)
                        )
                    }
                    val topGames = json.optJSONArray("topGames")
                    val topSummary = if (topGames != null && topGames.length() > 0) {
                        val g = topGames.optJSONObject(0)
                        val w = g?.optJSONObject("white")?.optString("name", "")
                        val b = g?.optJSONObject("black")?.optString("name", "")
                        val yr = g?.optInt("year", 2024)
                        "Эталонная партия: $w vs $b ($yr)"
                    } else "База партий гроссмейстеров FIDE"
                    OpeningExplorerResult(
                        openingEco = eco,
                        openingName = name,
                        moves = moves,
                        topGameSummary = topSummary
                    )
                }
            }
        }

    suspend fun fetchCloudEval(fen: String, token: String = ""): Result<CloudEvalResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encodedFen = java.net.URLEncoder.encode(fen, "UTF-8")
                val req = Request.Builder()
                    .url("https://lichess.org/api/cloud-eval?fen=$encodedFen&multiPv=1")
                    .header("Accept", "application/json")
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val pvs = json.optJSONArray("pvs")
                    val firstPv = pvs?.optJSONObject(0)
                    val movesStr = firstPv?.optString("moves", "") ?: ""
                    val cp = firstPv?.optInt("cp", 0)
                        ?: firstPv?.optInt("mate")?.let { if (it > 0) 19000 else -19000 }
                        ?: 0
                    CloudEvalResult(
                        fen = json.optString("fen", fen),
                        depth = json.optInt("depth", 36),
                        knodes = json.optInt("knodes", 12500),
                        cp = cp,
                        bestMoveUci = movesStr.split(" ").firstOrNull() ?: "",
                        pvLine = movesStr
                    )
                }
            }
        }

    suspend fun fetchCrosstable(user1: String, user2: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val u1 = user1.trim().ifEmpty { "DrNykterstein" }
                val u2 = user2.trim().ifEmpty { "Hikaru" }
                val req = Request.Builder()
                    .url("https://lichess.org/api/crosstable/$u1/$u2")
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val users = json.optJSONObject("users")
                    val s1 = users?.optDouble(u1.lowercase(), 0.0) ?: users?.optDouble(u1, 0.0) ?: 0.0
                    val s2 = users?.optDouble(u2.lowercase(), 0.0) ?: users?.optDouble(u2, 0.0) ?: 0.0
                    val nb = json.optInt("nbGames", 0)
                    "Личные встречи $u1 vs $u2: $s1 — $s2 (всего $nb партий)"
                }
            }
        }

    // --- ACCOUNT LOGIN & REAL LIVE ONLINE PLAY (Board API, Seek, Challenge, TV Feed) ---

    suspend fun verifyTokenAndFetchAccount(token: String): Result<LichessUserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanToken = token.trim()
                if (cleanToken.isBlank()) error("Введите персональный API-токен Lichess (начинается с lip_...)")
                val req = Request.Builder()
                    .url("https://lichess.org/api/account")
                    .header("Accept", "application/json")
                    .withAuth(cleanToken)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error("Токен не принят сервером Lichess (HTTP ${resp.code}). Проверьте правильность токена lip_...")
                    }
                    val body = resp.body?.string() ?: error("Пустой ответ от сервера Lichess")
                    val json = JSONObject(body)
                    val perfs = json.optJSONObject("perfs")
                    val count = json.optJSONObject("count")
                    val profile = json.optJSONObject("profile")
                    LichessUserProfile(
                        username = json.optString("username", ""),
                        title = json.optString("title", ""),
                        patron = json.optBoolean("patron", false),
                        online = true,
                        bulletRating = perfs?.optJSONObject("bullet")?.optInt("rating", 1500) ?: 1500,
                        blitzRating = perfs?.optJSONObject("blitz")?.optInt("rating", 1500) ?: 1500,
                        rapidRating = perfs?.optJSONObject("rapid")?.optInt("rating", 1500) ?: 1500,
                        classicalRating = perfs?.optJSONObject("classical")?.optInt("rating", 1500) ?: 1500,
                        puzzleRating = perfs?.optJSONObject("puzzle")?.optInt("rating", 1500) ?: 1500,
                        totalGames = count?.optInt("all", 0) ?: 0,
                        wins = count?.optInt("win", 0) ?: 0,
                        draws = count?.optInt("draw", 0) ?: 0,
                        losses = count?.optInt("loss", 0) ?: 0,
                        bio = profile?.optString("bio", "") ?: "",
                        country = profile?.optString("country", "INT") ?: "INT"
                    )
                }
            }
        }

    suspend fun fetchOngoingAccountGames(token: String): Result<List<OngoingLichessGame>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (token.isBlank()) error("Токен не задан")
                val req = Request.Builder()
                    .url("https://lichess.org/api/account/playing")
                    .header("Accept", "application/json")
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val arr = json.optJSONArray("nowPlaying") ?: JSONArray()
                    val list = mutableListOf<OngoingLichessGame>()
                    for (i in 0 until arr.length()) {
                        val g = arr.optJSONObject(i) ?: continue
                        val opp = g.optJSONObject("opponent")
                        val aiLvl = opp?.optInt("ai", 0) ?: 0
                        val oppName = if (aiLvl > 0) {
                            "Lichess Stockfish Ур. $aiLvl"
                        } else {
                            opp?.optString("username", "Lichess Player").orEmpty().ifBlank { "Lichess Player" }
                        }
                        list.add(
                            OngoingLichessGame(
                                gameId = g.optString("gameId", ""),
                                color = g.optString("color", "white"),
                                opponentName = oppName,
                                opponentRating = opp?.optInt("rating", 1500) ?: (1100 + aiLvl * 200),
                                fen = g.optString("fen", ""),
                                isMyTurn = g.optBoolean("isMyTurn", true)
                            )
                        )
                    }
                    list
                }
            }
        }

    suspend fun startServerAiGame(
        level: Int,
        clockMinutes: Int,
        clockIncrement: Int,
        colorPref: String = "random",
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isBlank()) error("Войдите в аккаунт Lichess (кнопка «Войти в Lichess» сверху)")
            val limitSec = (clockMinutes.coerceAtLeast(1) * 60).coerceIn(60, 10800)
            val form = FormBody.Builder()
                .add("level", level.coerceIn(1, 8).toString())
                .add("clock.limit", limitSec.toString())
                .add("clock.increment", clockIncrement.coerceIn(0, 60).toString())
                .add("color", colorPref)
                .build()
            val req = Request.Builder()
                .url("https://lichess.org/api/challenge/ai")
                .post(form)
                .header("Accept", "application/json")
                .withAuth(token)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching { JSONObject(raw).optString("error") }.getOrNull().orEmpty()
                    error("Ошибка Lichess API (${resp.code}): ${errMsg.ifBlank { "проверьте права токена challenge:write и board:play" }}")
                }
                val json = JSONObject(raw)
                val id = json.optString("id", "").ifBlank {
                    json.optJSONObject("challenge")?.optString("id", "").orEmpty()
                }
                if (id.isBlank()) error("Сервер Lichess не вернул ID партии")
                id
            }
        }
    }

    /**
     * Searches the real Lichess matchmaking pool via /api/board/seek while monitoring /api/account/playing.
     * Note: Lichess Board API requires Rapid/Classical time controls (>= 10+0 or 5+5) for human matchmaking seek.
     */
    suspend fun seekRealHumanOpponent(
        clockMinutes: Int,
        clockIncrement: Int,
        rated: Boolean,
        token: String
    ): Result<OngoingLichessGame> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isBlank()) error("Войдите в аккаунт Lichess для поиска онлайн-соперника")
            // 1. Check if there is already an active game on the account
            val existingBefore = fetchOngoingAccountGames(token).getOrDefault(emptyList())
            val existingIds = existingBefore.map { it.gameId }.toSet()

            // Lichess Board API /api/board/seek only accepts Rapid/Classical (>= 10 min or >= 5+5)
            val seekMin = if (clockMinutes * 60 + clockIncrement * 40 < 480) 10 else clockMinutes
            val seekInc = if (seekMin == 10 && clockMinutes < 8) 0 else clockIncrement

            val form = FormBody.Builder()
                .add("rated", rated.toString())
                .add("time", seekMin.toString())
                .add("increment", seekInc.toString())
                .add("color", "random")
                .build()

            val seekReq = Request.Builder()
                .url("https://lichess.org/api/board/seek")
                .post(form)
                .header("Accept", "text/plain")
                .withAuth(token)
                .build()

            val seekCall = streamHttpClient.newCall(seekReq)
            var seekHttpError: String? = null

            val seekThread = Thread {
                runCatching {
                    seekCall.execute().use { resp ->
                        if (!resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val msg = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                            seekHttpError = "HTTP ${resp.code}: ${msg.ifBlank { body.take(120) }}"
                        } else {
                            // Keep reading stream until Lichess closes it on match found
                            val src = resp.body?.source()
                            while (src != null && !src.exhausted()) {
                                src.readUtf8Line() ?: break
                            }
                        }
                    }
                }
            }.apply { isDaemon = true; start() }

            try {
                repeat(12) {
                    kotlinx.coroutines.delay(1400L)
                    if (seekHttpError != null) {
                        error(seekHttpError!!)
                    }
                    val currentGames = fetchOngoingAccountGames(token).getOrDefault(emptyList())
                    val newlyStarted = currentGames.firstOrNull { it.gameId !in existingIds } ?: currentGames.firstOrNull()
                    if (newlyStarted != null && newlyStarted.gameId.isNotBlank()) {
                        seekCall.cancel()
                        return@runCatching newlyStarted
                    }
                }
                seekCall.cancel()
                error("TIMEOUT_POOL")
            } finally {
                runCatching { seekCall.cancel() }
                runCatching { seekThread.interrupt() }
            }
        }
    }

    suspend fun createChallenge(
        targetUsername: String,
        clockMinutes: Int,
        clockIncrement: Int,
        rated: Boolean,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (token.isBlank()) error("Укажите токен Lichess (кнопка «Войти в Lichess» сверху)")
            val endpoint = if (targetUsername.equals("AI", ignoreCase = true)) {
                "https://lichess.org/api/challenge/ai"
            } else if (targetUsername.isBlank()) {
                "https://lichess.org/api/challenge/open"
            } else {
                "https://lichess.org/api/challenge/${targetUsername.trim()}"
            }
            val formBuilder = FormBody.Builder()
                .add("clock.limit", (clockMinutes.coerceAtLeast(1) * 60).toString())
                .add("clock.increment", clockIncrement.toString())
                .add("rated", rated.toString())
            if (targetUsername.equals("AI", ignoreCase = true)) {
                formBuilder.add("level", "5")
            }
            val req = Request.Builder()
                .url(endpoint)
                .post(formBuilder.build())
                .header("Accept", "application/json")
                .withAuth(token)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val err = runCatching { JSONObject(raw).optString("error") }.getOrNull().orEmpty()
                    error("HTTP ${resp.code}: ${err.ifBlank { "Не удалось создать вызов" }}")
                }
                val json = JSONObject(raw.ifBlank { "{}" })
                val challengeObj = json.optJSONObject("challenge") ?: json
                val id = challengeObj.optString("id", "")
                val url = challengeObj.optString("url", "https://lichess.org/$id")
                id.ifEmpty { url }
            }
        }
    }

    suspend fun sendBoardMove(gameId: String, uciMove: String, token: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://lichess.org/api/board/game/$gameId/move/$uciMove")
                    .post(FormBody.Builder().build())
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { it.isSuccessful }
            }
        }

    suspend fun resignOrDrawBoardGame(gameId: String, resign: Boolean, token: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (resign) "resign" else "draw/yes"
                val req = Request.Builder()
                    .url("https://lichess.org/api/board/game/$gameId/$path")
                    .post(FormBody.Builder().build())
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { it.isSuccessful }
            }
        }

    suspend fun sendBoardChat(gameId: String, text: String, token: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val form = FormBody.Builder()
                    .add("room", "player")
                    .add("text", text)
                    .build()
                val req = Request.Builder()
                    .url("https://lichess.org/api/board/game/$gameId/chat")
                    .post(form)
                    .withAuth(token)
                    .build()
                httpClient.newCall(req).execute().use { it.isSuccessful }
            }
        }

    fun streamBoardGame(gameId: String, token: String): Flow<LiveBoardStreamEvent> = callbackFlow {
        val req = Request.Builder()
            .url("https://lichess.org/api/board/game/stream/$gameId")
            .header("Accept", "application/x-ndjson")
            .withAuth(token)
            .build()
        val call = streamHttpClient.newCall(req)
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                trySend(LiveBoardStreamEvent(type = "error", status = "HTTP ${response.code}"))
                close()
                return@callbackFlow
            }
            val source = response.body?.source()
            while (source != null && !source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                runCatching {
                    val obj = JSONObject(line)
                    when (obj.optString("type")) {
                        "gameFull" -> {
                            val w = obj.optJSONObject("white")
                            val b = obj.optJSONObject("black")
                            val st = obj.optJSONObject("state")
                            val wAi = w?.optInt("aiLevel", 0) ?: 0
                            val bAi = b?.optInt("aiLevel", 0) ?: 0
                            val wName = if (wAi > 0) "Lichess Stockfish Ур. $wAi" else w?.optString("name", "").orEmpty().ifBlank { w?.optString("id", "White").orEmpty() }
                            val bName = if (bAi > 0) "Lichess Stockfish Ур. $bAi" else b?.optString("name", "").orEmpty().ifBlank { b?.optString("id", "Black").orEmpty() }
                            trySend(
                                LiveBoardStreamEvent(
                                    type = "gameFull",
                                    gameId = obj.optString("id", gameId),
                                    whiteName = wName.ifBlank { "White" },
                                    whiteRating = w?.optInt("rating", if (wAi > 0) 1100 + wAi * 200 else 1500) ?: 1500,
                                    blackName = bName.ifBlank { "Black" },
                                    blackRating = b?.optInt("rating", if (bAi > 0) 1100 + bAi * 200 else 1500) ?: 1500,
                                    movesUci = st?.optString("moves", "") ?: "",
                                    wtimeMs = st?.optLong("wtime", 180_000L) ?: 180_000L,
                                    btimeMs = st?.optLong("btime", 180_000L) ?: 180_000L,
                                    status = st?.optString("status", "started") ?: "started",
                                    winner = st?.optString("winner", "") ?: ""
                                )
                            )
                        }
                        "gameState" -> {
                            trySend(
                                LiveBoardStreamEvent(
                                    type = "gameState",
                                    gameId = gameId,
                                    movesUci = obj.optString("moves", ""),
                                    wtimeMs = obj.optLong("wtime", 180_000L),
                                    btimeMs = obj.optLong("btime", 180_000L),
                                    status = obj.optString("status", "started"),
                                    winner = obj.optString("winner", "")
                                )
                            )
                        }
                        "chatLine" -> {
                            trySend(
                                LiveBoardStreamEvent(
                                    type = "chatLine",
                                    gameId = gameId,
                                    chatSender = obj.optString("username", "Player"),
                                    chatText = obj.optString("text", "")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            trySend(LiveBoardStreamEvent(type = "error", status = e.localizedMessage ?: "Stream closed"))
        }
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    /**
     * Public real-time Lichess TV game stream (no API token required!).
     */
    fun streamPublicTvFeed(): Flow<LiveBoardStreamEvent> = callbackFlow {
        val req = Request.Builder()
            .url("https://lichess.org/api/tv/feed")
            .header("Accept", "application/x-ndjson")
            .build()
        val call = streamHttpClient.newCall(req)
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                trySend(LiveBoardStreamEvent(type = "error", status = "HTTP ${response.code}"))
                close()
                return@callbackFlow
            }
            val source = response.body?.source()
            var currentGameId = "tv_live"
            var wName = "GM White"
            var wRating = 2700
            var bName = "GM Black"
            var bRating = 2700
            while (source != null && !source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                runCatching {
                    val obj = JSONObject(line)
                    val t = obj.optString("t")
                    val d = obj.optJSONObject("d") ?: return@runCatching
                    if (t == "featured") {
                        currentGameId = d.optString("id", "tv_live")
                        val players = d.optJSONArray("players")
                        if (players != null) {
                            for (i in 0 until players.length()) {
                                val p = players.optJSONObject(i) ?: continue
                                val color = p.optString("color")
                                val user = p.optJSONObject("user")
                                val name = user?.optString("name", "GM") ?: "GM"
                                val title = user?.optString("title", "") ?: ""
                                val full = if (title.isNotBlank()) "$title $name" else name
                                val rating = p.optInt("rating", 2650)
                                if (color == "white") {
                                    wName = full
                                    wRating = rating
                                } else {
                                    bName = full
                                    bRating = rating
                                }
                            }
                        }
                        val fen = d.optString("fen", "")
                        trySend(
                            LiveBoardStreamEvent(
                                type = "tvFen",
                                gameId = currentGameId,
                                whiteName = wName,
                                whiteRating = wRating,
                                blackName = bName,
                                blackRating = bRating,
                                fen = fen,
                                wtimeMs = 180_000L,
                                btimeMs = 180_000L
                            )
                        )
                    } else if (t == "fen") {
                        val fen = d.optString("fen", "")
                        val lm = d.optString("lm", "")
                        val wc = d.optLong("wc", 180L) * 1000L
                        val bc = d.optLong("bc", 180L) * 1000L
                        trySend(
                            LiveBoardStreamEvent(
                                type = "tvFen",
                                gameId = currentGameId,
                                whiteName = wName,
                                whiteRating = wRating,
                                blackName = bName,
                                blackRating = bRating,
                                fen = fen,
                                lastMoveUci = lm,
                                wtimeMs = wc,
                                btimeMs = bc
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            trySend(LiveBoardStreamEvent(type = "error", status = e.localizedMessage ?: "TV Stream ended"))
        }
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)
}

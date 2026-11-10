package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.GameAnalyticsEngine
import com.example.chess.PieceColor
import com.example.data.local.UserPreferencesEntity
import com.example.ui.ArenaUiState
import com.example.ui.PlayMode
import com.example.ui.components.InteractiveChessBoardWithEval
import com.example.ui.theme.AppUiTheme
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle

@Composable
fun PlayArenaScreen(
    arenaState: ArenaUiState,
    preferences: UserPreferencesEntity,
    onSquareClick: (com.example.chess.Square) -> Unit,
    onSelectTimeControl: (String, Int, Int, PlayMode, Int) -> Unit,
    onStartChallenge: (String, Int, Int, Boolean) -> Unit,
    onStartOnlineSeek: (Int, Int) -> Unit,
    onStartServerGame: (Int, Int, Int) -> Unit,
    onStartTvStream: () -> Unit,
    onConnectOngoingGame: (String) -> Unit,
    onSaveCredentials: (String, String) -> Unit,
    onLogoutAccount: () -> Unit,
    onFlipBoard: () -> Unit,
    onRequestHint: () -> Unit,
    onResignOrDraw: (Boolean) -> Unit,
    onSendChat: (String) -> Unit,
    onCycleQuickTheme: (AppUiTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUiTheme = AppUiTheme.fromId(preferences.appUiThemeId)
    val boardSkin = BoardSkin.fromId(preferences.boardSkinId)
    val pieceStyle = PieceStyle.fromId(preferences.pieceStyleId)

    var showLobbyDialog by remember { mutableStateOf(false) }
    var showChallengeFriendDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val isLoggedIn = arenaState.loggedInAccount != null || preferences.lichessApiToken.isNotBlank()
    val displayUsername = arenaState.loggedInAccount?.username
        ?: preferences.lichessUsername.ifBlank { "Аккаунт" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("play_arena_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Grandmaster Hero & Quick Pairing + Account Login Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_grandmaster_1794136074440),
                        contentDescription = "Арена LibreChess Studio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(114.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(114.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        activeUiTheme.background.copy(alpha = 0.94f),
                                        activeUiTheme.background.copy(alpha = 0.74f),
                                        activeUiTheme.background.copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LIBRECHESS STUDIO • LICHESS.ORG",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = arenaState.statusBanner,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .clickable {
                                            val next = when (activeUiTheme) {
                                                AppUiTheme.OBSIDIAN_GOLD -> AppUiTheme.TRUE_OLED
                                                AppUiTheme.TRUE_OLED -> AppUiTheme.CHESS_COM
                                                AppUiTheme.CHESS_COM -> AppUiTheme.CYBERPUNK_NEON
                                                else -> AppUiTheme.OBSIDIAN_GOLD
                                            }
                                            onCycleQuickTheme(next)
                                        }
                                        .testTag("quick_theme_switch_chip")
                                ) {
                                    Text(
                                        text = activeUiTheme.badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Prominent Account Login / Profile Button
                                Button(
                                    onClick = { showAccountDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLoggedIn) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isLoggedIn) Color.White else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("lichess_account_login_button")
                                ) {
                                    Icon(
                                        imageVector = if (isLoggedIn) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (isLoggedIn) "@$displayUsername" else "Войти в Lichess",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { showLobbyDialog = true },
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("open_quick_pairing_button")
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Быстрый пул", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showChallengeFriendDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("challenge_player_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Вызов игрока", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Mode Selector Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayMode.values().forEach { mode ->
                    FilterChip(
                        selected = arenaState.playMode == mode,
                        onClick = {
                            onSelectTimeControl(
                                arenaState.timeControlLabel,
                                arenaState.initialSeconds / 60,
                                arenaState.incrementSeconds,
                                mode,
                                arenaState.aiLevel
                            )
                        },
                        label = { Text(mode.labelRu, fontSize = 12.sp) },
                        leadingIcon = {
                            val icon = when (mode) {
                                PlayMode.ONLINE_LICHESS -> Icons.Default.Bolt
                                PlayMode.VS_STOCKFISH -> Icons.Default.SmartToy
                                PlayMode.TWO_PLAYER -> Icons.Default.People
                                PlayMode.FREE_ANALYSIS -> Icons.Default.MenuBook
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        // 2b. Dedicated Online Lichess Control Panel (shown when ONLINE_LICHESS or Live Stream is active)
        if (arenaState.playMode == PlayMode.ONLINE_LICHESS || arenaState.liveOnlineGameId != null || arenaState.isTvLiveStream) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (arenaState.isSearchingOnline) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (arenaState.isTvLiveStream) Icons.Default.LiveTv else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = when {
                                        arenaState.liveOnlineGameId != null ->
                                            "ОНЛАЙН-ПАРТИЯ LICHESS #${arenaState.liveOnlineGameId}"
                                        arenaState.isTvLiveStream ->
                                            "ПРЯМОЙ ЭФИР LICHESS TV LIVE"
                                        else ->
                                            "ОНЛАЙН-ЦЕНТР LICHESS.ORG"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = { showAccountDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = if (isLoggedIn) "Аккаунт: @$displayUsername" else "🔑 Войти в аккаунт",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!isLoggedIn) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAccountDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Для игры онлайн от своего имени войдите в Lichess (токен создаётся в 1 клик). Без входа доступен Lichess TV Live!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = { showAccountDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Войти", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isLoggedIn) {
                                        showAccountDialog = true
                                    } else {
                                        onStartOnlineSeek(
                                            (arenaState.initialSeconds / 60).coerceAtLeast(1),
                                            arenaState.incrementSeconds
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("online_seek_opponent_button")
                            ) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Найти соперника", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isLoggedIn) {
                                        showAccountDialog = true
                                    } else {
                                        onStartServerGame(
                                            (arenaState.initialSeconds / 60).coerceAtLeast(1),
                                            arenaState.incrementSeconds,
                                            arenaState.aiLevel
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("online_server_ai_button")
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Серверная игра Lichess", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onStartTvStream,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("online_tv_stream_button")
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lichess TV Live", fontSize = 12.sp)
                            }
                        }

                        if (arenaState.ongoingGames.isNotEmpty()) {
                            Text(
                                text = "Ваши активные партии на сервере Lichess:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                arenaState.ongoingGames.forEach { g ->
                                    FilterChip(
                                        selected = arenaState.liveOnlineGameId == g.gameId,
                                        onClick = { onConnectOngoingGame(g.gameId) },
                                        label = {
                                            Text(
                                                "vs ${g.opponentName} (${g.opponentRating}) • ${if (g.isMyTurn) "Ваш ход" else "Ход соперника"}",
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Top Player Bar (Opponent) + Digital Clock
        item {
            val topIsBlack = !arenaState.flipped
            val name = if (topIsBlack) arenaState.blackPlayerName else arenaState.whitePlayerName
            val rating = if (topIsBlack) arenaState.blackPlayerRating else arenaState.whitePlayerRating
            val clockSec = if (topIsBlack) arenaState.blackClockSeconds else arenaState.whiteClockSeconds
            val isTurn = (topIsBlack && arenaState.boardState.activeColor == PieceColor.BLACK) ||
                (!topIsBlack && arenaState.boardState.activeColor == PieceColor.WHITE)

            PlayerClockBar(
                playerName = name,
                rating = rating,
                clockSeconds = clockSec,
                isActiveTurn = isTurn,
                materialAdvantage = if (topIsBlack) -arenaState.boardState.materialDiff() else arenaState.boardState.materialDiff(),
                evalLabel = GameAnalyticsEngine.formatEval(arenaState.evalCentipawns)
            )
        }

        // 4. Interactive Chess Board with Vertical Eval Bar, Best-Move Arrows & Move Badges
        item {
            InteractiveChessBoardWithEval(
                boardState = arenaState.boardState,
                boardSkin = boardSkin,
                pieceStyle = pieceStyle,
                selectedSquare = arenaState.selectedSquare,
                legalDestinations = arenaState.legalMovesForSelected,
                flipped = arenaState.flipped,
                showCoordinates = preferences.showCoordinates,
                showEvalBar = preferences.showEvalBar,
                evalCentipawns = arenaState.evalCentipawns,
                lastMoveClassification = if (preferences.showMoveClassificationBadges) arenaState.lastMoveClassification else null,
                bestMoveArrowUci = arenaState.bestMoveHintUci,
                onSquareClick = onSquareClick
            )
        }

        // 5. Bottom Player Bar (User) + Digital Clock
        item {
            val bottomIsWhite = !arenaState.flipped
            val name = if (bottomIsWhite) arenaState.whitePlayerName else arenaState.blackPlayerName
            val rating = if (bottomIsWhite) arenaState.whitePlayerRating else arenaState.blackPlayerRating
            val clockSec = if (bottomIsWhite) arenaState.whiteClockSeconds else arenaState.blackClockSeconds
            val isTurn = (bottomIsWhite && arenaState.boardState.activeColor == PieceColor.WHITE) ||
                (!bottomIsWhite && arenaState.boardState.activeColor == PieceColor.BLACK)

            PlayerClockBar(
                playerName = name,
                rating = rating,
                clockSeconds = clockSec,
                isActiveTurn = isTurn,
                materialAdvantage = if (bottomIsWhite) arenaState.boardState.materialDiff() else -arenaState.boardState.materialDiff(),
                evalLabel = "${boardSkin.title} • ${pieceStyle.title.substringBefore(" ")}"
            )
        }

        // 6. Action Control Bar + Move Notation Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (arenaState.moveHistorySan.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            val pairs = arenaState.moveHistorySan.chunked(2)
                            items(pairs.size) { idx ->
                                val movePair = pairs[idx]
                                val whiteSan = movePair.getOrNull(0) ?: ""
                                val blackSan = movePair.getOrNull(1) ?: ""
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${idx + 1}. $whiteSan $blackSan".trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onFlipBoard,
                            modifier = Modifier.testTag("flip_board_button")
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Перевернуть доску")
                        }
                        IconButton(
                            onClick = onRequestHint,
                            modifier = Modifier.testTag("engine_hint_button")
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Лучший ход Stockfish", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showChatDialog = true },
                            modifier = Modifier.testTag("live_chat_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Игровой чат")
                        }
                        IconButton(
                            onClick = { onResignOrDraw(false) },
                            modifier = Modifier.testTag("offer_draw_button")
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = "Предложить ничью")
                        }
                        IconButton(
                            onClick = { onResignOrDraw(true) },
                            modifier = Modifier.testTag("resign_game_button")
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Сдаться", tint = Color(0xFFFA412D))
                        }
                        IconButton(
                            onClick = {
                                onSelectTimeControl(
                                    arenaState.timeControlLabel,
                                    arenaState.initialSeconds / 60,
                                    arenaState.incrementSeconds,
                                    arenaState.playMode,
                                    arenaState.aiLevel
                                )
                            },
                            modifier = Modifier.testTag("new_game_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Новая партия")
                        }
                    }
                }
            }
        }

        // 7. Live Master Opening Explorer & Cloud Stockfish Bar
        item {
            val exp = arenaState.openingExplorer
            val cloud = arenaState.cloudEval
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ДЕБЮТНАЯ БАЗА МАСТЕРОВ & ОБЛАЧНЫЙ STOCKFISH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (cloud != null) {
                            Text(
                                text = "Глубина ${cloud.depth} • ${GameAnalyticsEngine.formatEval(cloud.cp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = "${exp?.openingEco ?: "A00"} • ${exp?.openingName ?: "Стартовая позиция"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (exp != null && exp.moves.isNotEmpty()) {
                        exp.moves.take(3).forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${m.san} (${m.totalGames} парт.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(105.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(m.whiteWinPct.coerceAtLeast(5f))
                                            .height(12.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(m.drawPct.coerceAtLeast(5f))
                                            .height(12.dp)
                                            .background(Color(0xFF64748B))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(m.blackWinPct.coerceAtLeast(5f))
                                            .height(12.dp)
                                            .background(Color(0xFF1E293B))
                                    )
                                }
                                Text(
                                    text = "${m.whiteWinPct.toInt()}% / ${m.blackWinPct.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Lichess Account Login & Token Verification Modal
    if (showAccountDialog) {
        var usernameInput by remember(preferences.lichessUsername) {
            mutableStateOf(preferences.lichessUsername)
        }
        var tokenInput by remember(preferences.lichessApiToken) {
            mutableStateOf(preferences.lichessApiToken)
        }
        var showRawToken by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Вход в аккаунт Lichess.org")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val account = arenaState.loggedInAccount
                    if (account != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF2ECC71), RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✅ Авторизован: @${account.username}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF2ECC71),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bullet ${account.bulletRating} • Blitz ${account.blitzRating} • Rapid ${account.rapidRating} • Classical ${account.classicalRating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (!arenaState.authStatusMessage.isNullOrBlank()) {
                        Text(
                            text = arenaState.authStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "1. Нажмите кнопку ниже, чтобы открыть Lichess.org с уже выбранными правами (Board Play, Challenges, Tournaments) и скопируйте созданный токен (lip_...):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val tokenUrl = "https://lichess.org/account/oauth/token/create" +
                                "?scopes[]=board:play" +
                                "&scopes[]=challenge:read" +
                                "&scopes[]=challenge:write" +
                                "&scopes[]=tournament:write" +
                                "&scopes[]=team:write" +
                                "&description=LibreChess+Studio+Android"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tokenUrl)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("🔑 Получить токен на Lichess.org в 1 клик", fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Персональный токен Lichess (lip_...)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = { showRawToken = !showRawToken }) {
                                Text(if (showRawToken) "Скрыть" else "Показать", fontSize = 11.sp)
                            }
                        },
                        visualTransformation = if (showRawToken) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_lichess_token_input")
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Никнейм на Lichess (определится автоматически)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_lichess_username_input")
                    )

                    if (arenaState.isVerifyingAccount) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Проверка токена на сервере Lichess.org...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCredentials(usernameInput, tokenInput)
                    },
                    modifier = Modifier.testTag("dialog_verify_login_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Войти и проверить")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isLoggedIn) {
                        TextButton(
                            onClick = {
                                onLogoutAccount()
                                tokenInput = ""
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Выйти", color = Color(0xFFFA412D))
                        }
                    }
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Закрыть")
                    }
                }
            }
        )
    }

    // Quick Pairing Grid Modal (1+0, 2+1, 3+0, 3+2, 5+0, 5+3, 10+0, 10+5, 15+10, 30+0 + AI Level)
    if (showLobbyDialog) {
        var selectedAiLevel by remember { mutableIntStateOf(arenaState.aiLevel) }
        val presets = listOf(
            Triple("1+0 • Bullet", 1, 0),
            Triple("2+1 • Bullet", 2, 1),
            Triple("3+0 • Blitz", 3, 0),
            Triple("3+2 • Blitz", 3, 2),
            Triple("5+0 • Blitz", 5, 0),
            Triple("5+3 • Blitz", 5, 3),
            Triple("10+0 • Rapid", 10, 0),
            Triple("10+5 • Rapid", 10, 5),
            Triple("15+10 • Classical", 15, 10),
            Triple("30+0 • Classical", 30, 0)
        )
        AlertDialog(
            onDismissRequest = { showLobbyDialog = false },
            title = { Text("Быстрый старт Lichess (Лобби)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Выберите классический контроль времени Lichess для старта онлайн-поиска или партии против Stockfish:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    presets.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { (label, min, inc) ->
                                OutlinedButton(
                                    onClick = {
                                        onSelectTimeControl(label, min, inc, arenaState.playMode, selectedAiLevel)
                                        showLobbyDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Text(
                        "Сила Stockfish: Уровень $selectedAiLevel (${1100 + selectedAiLevel * 210} ЭЛО)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = selectedAiLevel.toFloat(),
                        onValueChange = { selectedAiLevel = it.toInt().coerceIn(1, 8) },
                        valueRange = 1f..8f,
                        steps = 6
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLobbyDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Direct Challenge Dialog
    if (showChallengeFriendDialog) {
        var opponentUsername by remember { mutableStateOf("") }
        var clockMinutes by remember { mutableIntStateOf(5) }
        var clockIncrement by remember { mutableIntStateOf(3) }
        AlertDialog(
            onDismissRequest = { showChallengeFriendDialog = false },
            title = { Text("Вызов реального игрока на Lichess") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = opponentUsername,
                        onValueChange = { opponentUsername = it },
                        label = { Text("Никнейм на Lichess (пусто = поиск в пуле)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Контроль: $clockMinutes мин + $clockIncrement сек", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3 to 0, 3 to 2, 5 to 3, 10 to 0).forEach { (m, inc) ->
                            FilterChip(
                                selected = clockMinutes == m && clockIncrement == inc,
                                onClick = {
                                    clockMinutes = m
                                    clockIncrement = inc
                                },
                                label = { Text("$m+$inc") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartChallenge(opponentUsername, clockMinutes, clockIncrement, true)
                        showChallengeFriendDialog = false
                    }
                ) {
                    Text("Отправить вызов")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChallengeFriendDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Live Game Chat Dialog
    if (showChatDialog) {
        var messageDraft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = { Text("Чат партии Lichess") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    arenaState.chatMessages.takeLast(6).forEach { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(msg.sender, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Good game!", "Well played!", "Удачи!", "Спасибо за игру!").forEach { quick ->
                            Text(
                                text = quick,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSendChat(quick) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it },
                        label = { Text("Сообщение сопернику...") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onSendChat(messageDraft)
                                    messageDraft = ""
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun PlayerClockBar(
    playerName: String,
    rating: Int,
    clockSeconds: Int,
    isActiveTurn: Boolean,
    materialAdvantage: Int,
    evalLabel: String
) {
    val mins = clockSeconds / 60
    val secs = clockSeconds % 60
    val timeText = "%02d:%02d".format(mins, secs)
    val isLowTime = clockSeconds < 30

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActiveTurn) 1.5.dp else 1.dp,
                color = if (isActiveTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isActiveTurn) Color(0xFF2ECC71) else Color(0xFF64748B))
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = playerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "($rating)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (materialAdvantage > 0) {
                            Text(
                                text = "+$materialAdvantage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = evalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isLowTime && isActiveTurn -> Color(0xFFFA412D)
                    isActiveTurn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = timeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isActiveTurn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

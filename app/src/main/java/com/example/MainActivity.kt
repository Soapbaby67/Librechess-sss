package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ChessViewModel
import com.example.ui.screens.AnalyticsAndReviewScreen
import com.example.ui.screens.CommunityAndTournamentsScreen
import com.example.ui.screens.PlayArenaScreen
import com.example.ui.screens.SkinsAndThemesScreen
import com.example.ui.theme.AppUiTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LibreChessTheme
import com.example.util.AppDiagnosticLogger
import com.example.util.DiagnosticLogEntry

enum class MainDestination(
    val route: String,
    val labelRu: String,
    val icon: ImageVector
) {
    PLAY("play_arena", "Играть", Icons.Default.SportsEsports),
    COMMUNITY("community_tournaments", "Турниры/Клубы", Icons.Default.EmojiEvents),
    ANALYTICS("analytics_review", "Аналитика", Icons.Default.Analytics),
    SKINS("skins_themes", "Скины и Темы", Icons.Default.Palette);

    companion object {
        fun fromRoute(route: String): MainDestination {
            return values().firstOrNull { it.route == route } ?: PLAY
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnosticLogger.init(this)
        AppDiagnosticLogger.info("MainActivity", "onCreate: старт инициализации окна")

        runCatching {
            enableEdgeToEdge()
            AppDiagnosticLogger.ok("MainActivity", "enableEdgeToEdge выполнен успешно")
        }.onFailure { err ->
            AppDiagnosticLogger.warn("MainActivity", "enableEdgeToEdge пропущен: ${err.message}")
        }

        val viewModelResult = runCatching {
            ViewModelProvider(this)[ChessViewModel::class.java]
        }

        viewModelResult.onFailure { err ->
            AppDiagnosticLogger.error("MainActivity", "Критическая ошибка создания ChessViewModel", err)
        }

        try {
            setContent {
                val vm = viewModelResult.getOrNull()
                if (vm != null) {
                    LibreChessApp(chessViewModel = vm)
                } else {
                    EmergencyDiagnosticScreen(
                        startupError = viewModelResult.exceptionOrNull()
                    )
                }
            }
            AppDiagnosticLogger.ok("MainActivity", "setContent установлен")
        } catch (t: Throwable) {
            AppDiagnosticLogger.error("MainActivity", "Сбой setContent, включен нативный экран логов", t)
            showNativeFallbackLogView(t)
        }
    }

    private fun showNativeFallbackLogView(error: Throwable) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(AndroidColor.parseColor("#0B0E14"))
            setPadding(36, 64, 36, 64)
        }
        val tv = TextView(this).apply {
            setTextColor(AndroidColor.parseColor("#F8FAFC"))
            textSize = 13f
            text = "LIBRECHESS STUDIO — АВАРИЙНЫЙ ЖУРНАЛ ЗАПУСКА\n\n" +
                "Ошибка: ${error.javaClass.name}: ${error.message}\n\n" +
                AppDiagnosticLogger.exportAllLogsAsText()
            setTextIsSelectable(true)
        }
        scroll.addView(tv)
        setContentView(scroll)
    }
}

@Composable
fun LibreChessApp(
    chessViewModel: ChessViewModel
) {
    val context = LocalContext.current
    val preferences by chessViewModel.preferencesFlow.collectAsStateWithLifecycle()
    val arenaState by chessViewModel.arenaState.collectAsStateWithLifecycle()
    val communityState by chessViewModel.communityState.collectAsStateWithLifecycle()
    val analyticsState by chessViewModel.analyticsState.collectAsStateWithLifecycle()
    val savedGames by chessViewModel.savedGamesFlow.collectAsStateWithLifecycle()
    val joinedItems by chessViewModel.joinedItemsFlow.collectAsStateWithLifecycle()

    val logs by AppDiagnosticLogger.logsFlow.collectAsStateWithLifecycle()
    val lastCrash by AppDiagnosticLogger.lastCrashTraceFlow.collectAsStateWithLifecycle()

    val currentUiTheme = AppUiTheme.fromId(preferences.appUiThemeId)
    var currentRoute by rememberSaveable { mutableStateOf(MainDestination.PLAY.route) }
    val currentDestination = MainDestination.fromRoute(currentRoute)

    var showDiagnosticDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppDiagnosticLogger.ok("ComposeUI", "Первый кадр LibreChessApp успешно отрисован (Тема: ${currentUiTheme.title})")
    }

    if (currentDestination != MainDestination.PLAY) {
        BackHandler {
            currentRoute = MainDestination.PLAY.route
        }
    }

    LibreChessTheme(appTheme = currentUiTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                DiagnosticStatusBar(
                    logs = logs,
                    hasCrashReport = !lastCrash.isNullOrBlank(),
                    onOpenLogs = { showDiagnosticDialog = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    MainDestination.values().forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination == dest,
                            onClick = {
                                AppDiagnosticLogger.info("Nav", "Переход на вкладку: ${dest.labelRu}")
                                currentRoute = dest.route
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.labelRu) },
                            label = { Text(dest.labelRu, fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_${dest.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (currentDestination) {
                MainDestination.PLAY -> {
                    PlayArenaScreen(
                        arenaState = arenaState,
                        preferences = preferences,
                        onSquareClick = chessViewModel::onArenaSquareClick,
                        onSelectTimeControl = chessViewModel::selectTimeControlAndStart,
                        onStartChallenge = chessViewModel::startOnlineChallengeOrSeek,
                        onFlipBoard = chessViewModel::flipArenaBoard,
                        onRequestHint = chessViewModel::requestEngineHintOnArena,
                        onResignOrDraw = chessViewModel::resignOrDrawCurrentGame,
                        onSendChat = chessViewModel::sendChatMessage,
                        onCycleQuickTheme = { nextTheme ->
                            chessViewModel.selectAppUiTheme(nextTheme, alsoApplyMatchingBoardAndPieces = true)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainDestination.COMMUNITY -> {
                    CommunityAndTournamentsScreen(
                        state = communityState,
                        preferences = preferences,
                        joinedItems = joinedItems,
                        onRefresh = chessViewModel::loadCommunityData,
                        onToggleJoinTournament = chessViewModel::toggleJoinTournament,
                        onCreateArenaTournament = chessViewModel::createNewArenaTournament,
                        onSearchClubs = chessViewModel::searchClubsByQuery,
                        onToggleJoinClub = chessViewModel::toggleJoinClub,
                        onPuzzleSquareClick = chessViewModel::onPuzzleSquareClick,
                        onPuzzleHint = chessViewModel::showPuzzleHint,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainDestination.ANALYTICS -> {
                    AnalyticsAndReviewScreen(
                        state = analyticsState,
                        savedGames = savedGames,
                        preferences = preferences,
                        onSearchPlayer = chessViewModel::loadLichessPlayerAndGames,
                        onUpdateCrosstableOpponent = chessViewModel::updateCrosstableOpponent,
                        onSelectGame = chessViewModel::selectGameForReview,
                        onImportCustomPgn = chessViewModel::importCustomPgnForReview,
                        onSetPlyIndex = chessViewModel::setReviewPlyIndex,
                        onStartRetryMistake = chessViewModel::startRetryMistakeOnCurrentPly,
                        onRetryMistakeSquareClick = chessViewModel::onRetryMistakeSquareClick,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainDestination.SKINS -> {
                    SkinsAndThemesScreen(
                        preferences = preferences,
                        onSelectAppUiTheme = { theme ->
                            chessViewModel.selectAppUiTheme(theme, alsoApplyMatchingBoardAndPieces = true)
                        },
                        onSelectBoardSkin = chessViewModel::selectBoardSkin,
                        onSelectPieceStyle = chessViewModel::selectPieceStyle,
                        onUpdateToggles = chessViewModel::updateVisualToggles,
                        onSaveCredentials = chessViewModel::saveLichessCredentials,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        if (showDiagnosticDialog) {
            DiagnosticLogDialog(
                logs = logs,
                lastCrash = lastCrash,
                onCopyAll = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("LibreChess Diagnostic Log", AppDiagnosticLogger.exportAllLogsAsText())
                    )
                    AppDiagnosticLogger.ok("Logger", "Полный лог скопирован в буфер обмена")
                },
                onClear = {
                    AppDiagnosticLogger.clearAllLogs(context)
                },
                onDismiss = { showDiagnosticDialog = false }
            )
        }
    }
}

@Composable
private fun DiagnosticStatusBar(
    logs: List<DiagnosticLogEntry>,
    hasCrashReport: Boolean,
    onOpenLogs: () -> Unit
) {
    val latest = logs.firstOrNull()
    val errorCount = logs.count { it.level == "ERROR" }
    val bg = if (hasCrashReport || errorCount > 0) Color(0xFF3B1219) else MaterialTheme.colorScheme.surface
    val dotColor = if (hasCrashReport || errorCount > 0) Color(0xFFFA412D) else Color(0xFF2ECC71)

    Surface(
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLogs() }
            .testTag("open_diagnostic_logs_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = if (hasCrashReport) {
                        "⚠ Найдена запись о сбое — нажмите для просмотра лога"
                    } else {
                        "Лог [+${latest?.elapsedMs ?: 0}мс]: ${latest?.message ?: "Система готова"}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = "Открыть журнал диагностики",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Логи (${logs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DiagnosticLogDialog(
    logs: List<DiagnosticLogEntry>,
    lastCrash: String?,
    onCopyAll: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Журнал запуска и диагностики", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "v2.5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopyAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Скопировать лог", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Очистить", fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!lastCrash.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF3B1219),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFA412D), RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "СТЕКТРЕЙС ПРЕДЫДУЩЕГО СБОЯ:",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFA412D)
                                    )
                                    Text(
                                        text = lastCrash,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFFF8FAFC)
                                    )
                                }
                            }
                        }
                    }

                    items(logs, key = { it.id }) { entry ->
                        val badgeColor = when (entry.level) {
                            "ERROR" -> Color(0xFFFA412D)
                            "WARN" -> Color(0xFFF7C631)
                            "OK" -> Color(0xFF2ECC71)
                            else -> Color(0xFF38BDF8)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "[${entry.level}] ${entry.tag}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = badgeColor
                                    )
                                    Text(
                                        text = "+${entry.elapsedMs}мс • ${entry.timestampLabel}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = entry.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!entry.stackTrace.isNullOrBlank()) {
                                    Text(
                                        text = entry.stackTrace,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun EmergencyDiagnosticScreen(startupError: Throwable?) {
    val context = LocalContext.current
    val logs by AppDiagnosticLogger.logsFlow.collectAsStateWithLifecycle()
    LibreChessTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B0E14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ДИАГНОСТИКА ЗАПУСКА LIBRECHESS STUDIO",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFA412D)
                )
                Text(
                    text = startupError?.let { "${it.javaClass.simpleName}: ${it.message}" }
                        ?: "Неизвестная ошибка инициализации",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("LibreChess Crash", AppDiagnosticLogger.exportAllLogsAsText())
                        )
                    }
                ) {
                    Text("Скопировать полный стектрейс")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs, key = { it.id }) { item ->
                        Text(
                            text = "[+${item.elapsedMs}ms] ${item.tag}: ${item.message}\n${item.stackTrace.orEmpty()}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

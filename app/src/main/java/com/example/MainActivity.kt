package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChessViewModel
import com.example.ui.screens.AnalyticsAndReviewScreen
import com.example.ui.screens.CommunityAndTournamentsScreen
import com.example.ui.screens.PlayArenaScreen
import com.example.ui.screens.SkinsAndThemesScreen
import com.example.ui.theme.AppUiTheme
import com.example.ui.theme.LibreChessTheme

enum class MainDestination(
    val route: String,
    val labelRu: String,
    val icon: ImageVector
) {
    PLAY("play_arena", "Играть", Icons.Default.SportsEsports),
    COMMUNITY("community_tournaments", "Турниры/Клубы", Icons.Default.EmojiEvents),
    ANALYTICS("analytics_review", "Аналитика", Icons.Default.Analytics),
    SKINS("skins_themes", "Скины и Темы", Icons.Default.Palette)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibreChessApp()
        }
    }
}

@Composable
fun LibreChessApp(
    chessViewModel: ChessViewModel = viewModel()
) {
    val preferences by chessViewModel.preferencesFlow.collectAsStateWithLifecycle()
    val arenaState by chessViewModel.arenaState.collectAsStateWithLifecycle()
    val communityState by chessViewModel.communityState.collectAsStateWithLifecycle()
    val analyticsState by chessViewModel.analyticsState.collectAsStateWithLifecycle()
    val savedGames by chessViewModel.savedGamesFlow.collectAsStateWithLifecycle()
    val joinedItems by chessViewModel.joinedItemsFlow.collectAsStateWithLifecycle()

    val currentUiTheme = AppUiTheme.fromId(preferences.appUiThemeId)
    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.PLAY) }

    if (currentDestination != MainDestination.PLAY) {
        BackHandler {
            currentDestination = MainDestination.PLAY
        }
    }

    LibreChessTheme(appTheme = currentUiTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    MainDestination.values().forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination == dest,
                            onClick = { currentDestination = dest },
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
    }
}

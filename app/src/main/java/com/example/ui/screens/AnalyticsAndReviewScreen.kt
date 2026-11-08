package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.AnalyzedPly
import com.example.chess.BoardState
import com.example.chess.ChessEngine
import com.example.chess.GameAnalyticsEngine
import com.example.chess.GameReviewReport
import com.example.chess.MoveClassification
import com.example.chess.Square
import com.example.data.local.SavedGameEntity
import com.example.data.local.UserPreferencesEntity
import com.example.ui.AnalyticsUiState
import com.example.ui.components.InteractiveChessBoardWithEval
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle

@Composable
fun AnalyticsAndReviewScreen(
    state: AnalyticsUiState,
    savedGames: List<SavedGameEntity>,
    preferences: UserPreferencesEntity,
    onSearchPlayer: (String) -> Unit,
    onUpdateCrosstableOpponent: (String) -> Unit,
    onSelectGame: (SavedGameEntity) -> Unit,
    onImportCustomPgn: (String) -> Unit,
    onSetPlyIndex: (Int) -> Unit,
    onStartRetryMistake: () -> Unit,
    onRetryMistakeSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerQuery by remember(state.searchedUsername) { mutableStateOf(state.searchedUsername) }
    var showImportPgnDialog by remember { mutableStateOf(false) }
    var showGamesArchiveSheet by remember { mutableStateOf(false) }

    val boardSkin = BoardSkin.fromId(preferences.boardSkinId)
    val pieceStyle = PieceStyle.fromId(preferences.pieceStyleId)
    val report: GameReviewReport? = state.activeReviewReport
    val currentPly: AnalyzedPly? = report?.plies?.getOrNull(state.currentReviewPlyIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("analytics_review_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Player Search & PGN Import Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Column {
                            Text(
                                text = "GAME REVIEW & ТЕЛЕМЕТРИЯ LICHESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Расширенная аналитика партий",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showImportPgnDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("import_pgn_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PGN", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { showGamesArchiveSheet = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_games_archive_button")
                            ) {
                                Text("Партии (${savedGames.size})", fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = playerQuery,
                            onValueChange = { playerQuery = it },
                            label = { Text("Никнейм на Lichess") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lichess_player_search_input")
                        )
                        Button(
                            onClick = { onSearchPlayer(playerQuery) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("load_player_games_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Загрузить")
                        }
                    }

                    // Quick top player chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DrNykterstein", "Hikaru", "Alireza2003", "Zhigalko_Sergei", "RebeccaHarris").forEach { gm ->
                            FilterChip(
                                selected = state.searchedUsername.equals(gm, ignoreCase = true),
                                onClick = {
                                    playerQuery = gm
                                    onSearchPlayer(gm)
                                },
                                label = { Text(gm, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Player Profile Telemetry & Crosstable Card
        state.userProfile?.let { prof ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Text(
                                text = "${prof.title} ${prof.username}".trim(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Партий: ${prof.totalGames} (В: ${prof.wins} / Н: ${prof.draws} / П: ${prof.losses})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RatingBadge("Bullet", prof.bulletRating)
                            RatingBadge("Blitz", prof.blitzRating)
                            RatingBadge("Rapid", prof.rapidRating)
                            RatingBadge("Classical", prof.classicalRating)
                            RatingBadge("Puzzles", prof.puzzleRating)
                        }
                        Text(
                            text = state.crosstableSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (report != null && currentPly != null) {
            // 3. Interactive Review Board + Coach Commentary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${report.whiteSummary.username} (${report.whiteSummary.rating}) vs ${report.blackSummary.username} (${report.blackSummary.rating})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${report.openingEco} • ${report.openingName} • ${report.resultText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentPly.classification.color.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "${currentPly.classification.badgeSymbol} ${currentPly.classification.titleRu}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = currentPly.classification.color,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Coach Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.retryMistakeMode) {
                                        state.retryFeedbackText ?: "Найдите сильнейший ход на доске!"
                                    } else {
                                        currentPly.coachCommentRu
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentPly.classification in listOf(
                                        MoveClassification.BLUNDER,
                                        MoveClassification.MISTAKE,
                                        MoveClassification.INACCURACY
                                    ) && !state.retryMistakeMode
                                ) {
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = onStartRetryMistake,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("retry_mistake_button")
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Исправить", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        val displayedBoard = if (state.retryMistakeMode && state.retryBoardState != null) {
                            state.retryBoardState
                        } else {
                            BoardState.fromFen(currentPly.fenAfter)
                        }
                        val retryLegals = if (state.retryMistakeMode && state.retrySelectedSquare != null && state.retryBoardState != null) {
                            ChessEngine.legalMovesFrom(state.retryBoardState, state.retrySelectedSquare)
                        } else emptyList()

                        InteractiveChessBoardWithEval(
                            boardState = displayedBoard,
                            boardSkin = boardSkin,
                            pieceStyle = pieceStyle,
                            selectedSquare = state.retrySelectedSquare,
                            legalDestinations = retryLegals,
                            showCoordinates = preferences.showCoordinates,
                            showEvalBar = preferences.showEvalBar,
                            evalCentipawns = currentPly.evalAfterCp,
                            lastMoveClassification = if (preferences.showMoveClassificationBadges && !state.retryMistakeMode) {
                                currentPly.classification
                            } else null,
                            bestMoveArrowUci = if (!state.retryMistakeMode) currentPly.bestMoveUci else null,
                            onSquareClick = { sq ->
                                if (state.retryMistakeMode) {
                                    onRetryMistakeSquareClick(sq)
                                }
                            }
                        )

                        // Ply Navigation Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSetPlyIndex(0) },
                                modifier = Modifier.testTag("review_first_ply_button")
                            ) {
                                Icon(Icons.Default.FirstPage, contentDescription = "В начало")
                            }
                            IconButton(
                                onClick = { onSetPlyIndex(state.currentReviewPlyIndex - 1) },
                                modifier = Modifier.testTag("review_prev_ply_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий ход")
                            }
                            Text(
                                text = "Ход ${currentPly.moveNumber}${if (currentPly.color.name == "WHITE") "." else "..."} ${currentPly.san} (${state.currentReviewPlyIndex + 1}/${report.plies.size})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onSetPlyIndex(state.currentReviewPlyIndex + 1) },
                                modifier = Modifier.testTag("review_next_ply_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий ход")
                            }
                            IconButton(
                                onClick = { onSetPlyIndex(report.plies.lastIndex) },
                                modifier = Modifier.testTag("review_last_ply_button")
                            ) {
                                Icon(Icons.Default.LastPage, contentDescription = "В конец")
                            }
                        }
                    }
                }
            }

            // 4. Dual Accuracy Cards + Estimated Rating + Move Classification Breakdown (Chess.com & Lichess Style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "СВОДКА GAME REVIEW: ТОЧНОСТЬ И КАЧЕСТВО ХОДОВ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayerAccuracyBox(
                                label = "БЕЛЫЕ • ${report.whiteSummary.username}",
                                accuracy = report.whiteSummary.overallAccuracy,
                                estRating = report.whiteSummary.estimatedPerformanceRating,
                                acpl = report.whiteSummary.avgCentipawnLoss,
                                modifier = Modifier.weight(1f)
                            )
                            PlayerAccuracyBox(
                                label = "ЧЕРНЫЕ • ${report.blackSummary.username}",
                                accuracy = report.blackSummary.overallAccuracy,
                                estRating = report.blackSummary.estimatedPerformanceRating,
                                acpl = report.blackSummary.avgCentipawnLoss,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Move Classification Table
                        MoveClassification.values().forEach { cls ->
                            val wCount = report.whiteSummary.counts[cls] ?: 0
                            val bCount = report.blackSummary.counts[cls] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wCount.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = cls.color,
                                    modifier = Modifier.width(36.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(cls.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cls.badgeSymbol,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(cls.titleRu, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = bCount.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = cls.color
                                )
                            }
                        }
                    }
                }
            }

            // 5. Interactive Advantage Evaluation Chart & Time-per-Move Histogram
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ИНТЕРАКТИВНЫЙ ГРАФИК ОЦЕНКИ ПОЗИЦИИ (НАЖМИТЕ ДЛЯ ПЕРЕМОТКИ)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        InteractiveEvalCurveCanvas(
                            plies = report.plies,
                            selectedPlyIndex = state.currentReviewPlyIndex,
                            onSelectPly = onSetPlyIndex
                        )

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "РАСХОД ВРЕМЕНИ НА ХОД И ДЕТЕКТОР ЦЕЙТНОТОВ (СЕКУНДЫ)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        InteractiveMoveTimeHistogram(
                            plies = report.plies,
                            selectedPlyIndex = state.currentReviewPlyIndex,
                            onSelectPly = onSetPlyIndex
                        )

                        // Phase Accuracy Breakdown
                        Text(
                            text = "ТОЧНОСТЬ ПО ФАЗАМ ПАРТИИ (ДЕБЮТ / МИТТЕЛЬШПИЛЬ / ЭНДШПИЛЬ)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PhaseComparisonRow("Дебют", report.whiteSummary.openingAccuracy, report.blackSummary.openingAccuracy)
                        PhaseComparisonRow("Миттельшпиль", report.whiteSummary.middlegameAccuracy, report.blackSummary.middlegameAccuracy)
                        PhaseComparisonRow("Эндшпиль", report.whiteSummary.endgameAccuracy, report.blackSummary.endgameAccuracy)
                    }
                }
            }

            // 6. Key Turning Points & Mistakes List
            if (report.keyTurningPoints.isNotEmpty()) {
                item {
                    Text(
                        text = "Ключевые моменты партии и зевки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(report.keyTurningPoints, key = { it.plyIndex }) { tp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetPlyIndex(tp.plyIndex) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ход ${tp.moveNumber}${if (tp.color.name == "WHITE") "." else "..."} ${tp.san} • ${tp.classification.titleRu}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tp.classification.color
                                )
                                Text(
                                    text = tp.coachCommentRu,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "К ходу →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom PGN Import Dialog
    if (showImportPgnDialog) {
        var pgnText by remember {
            mutableStateOf("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8 10. d4 Nbd7")
        }
        AlertDialog(
            onDismissRequest = { showImportPgnDialog = false },
            title = { Text("Импорт PGN / Нотации для разбора") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Вставьте текст партии в формате PGN или список ходов для мгновенного расчета точности и зевков:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pgnText,
                        onValueChange = { pgnText = it },
                        label = { Text("Текст PGN") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCustomPgn(pgnText)
                        showImportPgnDialog = false
                    }
                ) {
                    Text("Разобрать партию")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPgnDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Saved / Imported Games Archive Dialog
    if (showGamesArchiveSheet) {
        AlertDialog(
            onDismissRequest = { showGamesArchiveSheet = false },
            title = { Text("Архив загруженных и сыгранных партий") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedGames.take(8).forEach { g ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectGame(g)
                                    showGamesArchiveSheet = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${g.whitePlayer} (${g.whiteRating}) vs ${g.blackPlayer} (${g.blackRating})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${g.speed} • ${g.openingEco} ${g.openingName} • Точность: ${g.whiteAccuracy}% / ${g.blackAccuracy}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGamesArchiveSheet = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun RatingBadge(title: String, rating: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                rating.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PlayerAccuracyBox(
    label: String,
    accuracy: Double,
    estRating: Int,
    acpl: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${"%.1f".format(accuracy)}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Перформанс: $estRating • ACPL: $acpl",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InteractiveEvalCurveCanvas(
    plies: List<AnalyzedPly>,
    selectedPlyIndex: Int,
    onSelectPly: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background( Color(0xFF141820))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(plies) {
                    detectTapGestures { offset ->
                        if (plies.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * plies.size).toInt().coerceIn(0, plies.lastIndex)
                            onSelectPly(idx)
                        }
                    }
                }
        ) {
            if (plies.isEmpty()) return@Canvas
            val midY = size.height / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5f
            )
            val path = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, midY)

            plies.forEachIndexed { index, ply ->
                val x = (index.toFloat() / (plies.size - 1).coerceAtLeast(1)) * size.width
                val winPct = GameAnalyticsEngine.centipawnsToWinPercent(ply.evalAfterCp).toFloat()
                val y = size.height * (1f - (winPct / 100f)).coerceIn(0.06f, 0.94f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                fillPath.lineTo(x, y)

                if (ply.classification == MoveClassification.BLUNDER || ply.classification == MoveClassification.BRILLIANT) {
                    drawCircle(
                        color = ply.classification.color,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            fillPath.lineTo(size.width, midY)
            fillPath.close()

            drawPath(fillPath, color = primaryColor.copy(alpha = 0.20f))
            drawPath(path, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))

            val selX = (selectedPlyIndex.toFloat() / (plies.size - 1).coerceAtLeast(1)) * size.width
            drawLine(
                color = Color.White,
                start = Offset(selX, 0f),
                end = Offset(selX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun InteractiveMoveTimeHistogram(
    plies: List<AnalyzedPly>,
    selectedPlyIndex: Int,
    onSelectPly: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141820))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(plies) {
                    detectTapGestures { offset ->
                        if (plies.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * plies.size).toInt().coerceIn(0, plies.lastIndex)
                            onSelectPly(idx)
                        }
                    }
                }
        ) {
            if (plies.isEmpty()) return@Canvas
            val maxSec = (plies.maxOfOrNull { it.timeSpentSeconds } ?: 15.0).coerceAtLeast(5.0).toFloat()
            val barWidth = (size.width / plies.size.toFloat()).coerceAtLeast(3f)

            plies.forEachIndexed { idx, ply ->
                val ratio = (ply.timeSpentSeconds.toFloat() / maxSec).coerceIn(0.08f, 1f)
                val barH = size.height * ratio
                val x = idx * barWidth
                val barColor = when {
                    idx == selectedPlyIndex -> Color.White
                    ply.isZeitnot -> Color(0xFFFA412D)
                    ply.color.name == "WHITE" -> Color(0xFF38BDF8)
                    else -> Color(0xFFA78BFA)
                }
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barH),
                    size = Size((barWidth - 1.5f).coerceAtLeast(2f), barH)
                )
            }
        }
    }
}

@Composable
private fun PhaseComparisonRow(phaseName: String, whiteAcc: Double, blackAcc: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${"%.1f".format(whiteAcc)}%", style = MaterialTheme.typography.labelSmall)
            Text(phaseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${"%.1f".format(blackAcc)}%", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = { (whiteAcc / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

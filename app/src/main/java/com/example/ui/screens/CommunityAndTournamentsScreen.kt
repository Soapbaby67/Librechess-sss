package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.Square
import com.example.data.local.JoinedItemEntity
import com.example.data.local.UserPreferencesEntity
import com.example.data.remote.LichessClubItem
import com.example.data.remote.LichessTournamentItem
import com.example.ui.CommunityUiState
import com.example.ui.components.InteractiveChessBoardWithEval
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.PieceStyle

private enum class CommunityTab(val title: String) {
    TOURNAMENTS("Турниры Arena/Swiss"),
    CLUBS("Клубы и Команды"),
    PUZZLES("Задачи Lichess"),
    TV_AND_FIDE("ТВ, Трансляции и Сеансы")
}

@Composable
fun CommunityAndTournamentsScreen(
    state: CommunityUiState,
    preferences: UserPreferencesEntity,
    joinedItems: List<JoinedItemEntity>,
    onRefresh: () -> Unit,
    onToggleJoinTournament: (LichessTournamentItem) -> Unit,
    onCreateArenaTournament: (String, Int, Int, Int) -> Unit,
    onSearchClubs: (String) -> Unit,
    onToggleJoinClub: (LichessClubItem) -> Unit,
    onPuzzleSquareClick: (Square) -> Unit,
    onPuzzleHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CommunityTab.TOURNAMENTS) }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }

    val boardSkin = BoardSkin.fromId(preferences.boardSkinId)
    val pieceStyle = PieceStyle.fromId(preferences.pieceStyleId)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("community_tournaments_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ТУРНИРЫ, КЛУБЫ, ЗАДАЧИ И ЭФИРЫ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Сообщество Lichess.org",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (selectedTab == CommunityTab.TOURNAMENTS) {
                        Button(
                            onClick = { showCreateTournamentDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("create_tournament_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Создать", fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_community_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            }
        }

        if (!state.feedbackBanner.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = state.feedbackBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Sub-navigation chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CommunityTab.values().forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title, fontSize = 11.sp) },
                        modifier = Modifier.testTag("community_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (selectedTab) {
            CommunityTab.TOURNAMENTS -> {
                items(state.tournaments, key = { it.id }) { tour ->
                    val isJoined = joinedItems.any { it.itemId == tour.id }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = tour.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tour.clockMinute}+${tour.clockIncrement} • ${tour.variant} • ${tour.minutesDuration} мин",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (tour.status == "STARTED") Color(0x262ECC71) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = when (tour.status) {
                                            "STARTED" -> "ИДЕТ СЕЙЧАС"
                                            "CREATED" -> "СКОРО СТАРТ"
                                            else -> "ЗАВЕРШЕН"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (tour.status == "STARTED") Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Участников: ${tour.nbPlayers + (if (isJoined) 1 else 0)} • Лидер: ${tour.topLeader}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isJoined) {
                                    OutlinedButton(
                                        onClick = { onToggleJoinTournament(tour) },
                                        modifier = Modifier.testTag("withdraw_tournament_${tour.id}")
                                    ) {
                                        Text("На паузу / Выйти", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onToggleJoinTournament(tour) },
                                        modifier = Modifier.testTag("join_tournament_${tour.id}")
                                    ) {
                                        Text("Участвовать", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CommunityTab.CLUBS -> {
                item {
                    OutlinedTextField(
                        value = state.clubSearchQuery,
                        onValueChange = onSearchClubs,
                        label = { Text("Поиск клубов и команд на Lichess...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("club_search_input")
                    )
                }

                items(state.clubs, key = { it.id }) { club ->
                    val isMember = joinedItems.any { it.itemId == club.id }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(
                                            text = club.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Участников: ${club.nbMembers + (if (isMember) 1 else 0)} • Капитан: ${club.leaderName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isMember) {
                                    OutlinedButton(
                                        onClick = { onToggleJoinClub(club) },
                                        modifier = Modifier.testTag("quit_club_${club.id}")
                                    ) {
                                        Text("Вы в клубе ✓", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onToggleJoinClub(club) },
                                        modifier = Modifier.testTag("join_club_${club.id}")
                                    ) {
                                        Text("Вступить в клуб", fontSize = 12.sp)
                                    }
                                }
                            }
                            if (club.description.isNotBlank()) {
                                Text(
                                    text = club.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            CommunityTab.PUZZLES -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = state.puzzleStatusText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Серия ★ ${state.puzzleStreak}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            InteractiveChessBoardWithEval(
                                boardState = state.puzzleBoard,
                                boardSkin = boardSkin,
                                pieceStyle = pieceStyle,
                                selectedSquare = state.puzzleSelectedSq,
                                legalDestinations = state.puzzleLegalMoves,
                                showCoordinates = preferences.showCoordinates,
                                showEvalBar = false,
                                onSquareClick = onPuzzleSquareClick
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Темы: ${state.dailyPuzzle?.themes?.joinToString(", ")?.ifBlank { "Тактика, Мат, Двойной удар" } ?: "Тактика"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(onClick = onPuzzleHint) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Подсказка")
                                }
                            }
                        }
                    }
                }
            }

            CommunityTab.TV_AND_FIDE -> {
                item {
                    Text(
                        text = "Прямой эфир Lichess TV (Топ-каналы)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.tvChannels) { ch ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(ch.channelName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${ch.playerTitle} ${ch.playerName} (${ch.rating})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x26FA412D)
                            ) {
                                Text(
                                    text = "LIVE #${ch.gameId.take(6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFA412D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Официальные трансляции FIDE, Сеансы (Simuls) и Стримеры",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(state.broadcastsAndSimuls) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = item.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateTournamentDialog) {
        var name by remember { mutableStateOf("LibreChess Cup") }
        var clockMin by remember { mutableIntStateOf(3) }
        var clockInc by remember { mutableIntStateOf(2) }
        var durationMin by remember { mutableIntStateOf(60) }

        AlertDialog(
            onDismissRequest = { showCreateTournamentDialog = false },
            title = { Text("Создать турнир Lichess Arena") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название турнира") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Контроль времени: $clockMin+$clockInc", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to 0, 3 to 0, 3 to 2, 5 to 3, 10 to 0).forEach { (m, inc) ->
                            FilterChip(
                                selected = clockMin == m && clockInc == inc,
                                onClick = {
                                    clockMin = m
                                    clockInc = inc
                                },
                                label = { Text("$m+$inc") }
                            )
                        }
                    }
                    Text("Длительность: $durationMin мин", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 45, 60, 90).forEach { d ->
                            FilterChip(
                                selected = durationMin == d,
                                onClick = { durationMin = d },
                                label = { Text("$d мин") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateArenaTournament(name, clockMin, clockInc, durationMin)
                        showCreateTournamentDialog = false
                    }
                ) {
                    Text("Создать турнир")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTournamentDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

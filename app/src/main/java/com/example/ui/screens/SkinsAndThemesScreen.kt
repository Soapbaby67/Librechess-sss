package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardState
import com.example.chess.ChessEngine
import com.example.chess.MoveClassification
import com.example.chess.Square
import com.example.data.local.UserPreferencesEntity
import com.example.ui.components.InteractiveChessBoardWithEval
import com.example.ui.theme.AppUiTheme
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.PieceStyle

@Composable
fun SkinsAndThemesScreen(
    preferences: UserPreferencesEntity,
    onSelectAppUiTheme: (AppUiTheme) -> Unit,
    onSelectBoardSkin: (BoardSkin) -> Unit,
    onSelectPieceStyle: (PieceStyle) -> Unit,
    onUpdateToggles: (Boolean?, Boolean?, Boolean?) -> Unit,
    onSaveCredentials: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUiTheme = AppUiTheme.fromId(preferences.appUiThemeId)
    val activeBoardSkin = BoardSkin.fromId(preferences.boardSkinId)
    val activePieceStyle = PieceStyle.fromId(preferences.pieceStyleId)

    var previewBoard by remember {
        mutableStateOf(
            BoardState.fromFen("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4")
        )
    }
    var previewSelectedSq by remember { mutableStateOf<Square?>(Square(2, 3)) }

    var usernameInput by remember(preferences.lichessUsername) { mutableStateOf(preferences.lichessUsername) }
    var tokenInput by remember(preferences.lichessApiToken) { mutableStateOf(preferences.lichessApiToken) }
    var credentialsSavedToast by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("skins_themes_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Interactive Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                        Column {
                            Text(
                                text = "МАСТЕРСКАЯ ТЕМ, СКИНОВ И ФИГУР",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${activeUiTheme.title} • ${activeBoardSkin.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    val legals = previewSelectedSq?.let { ChessEngine.legalMovesFrom(previewBoard, it) } ?: emptyList()

                    InteractiveChessBoardWithEval(
                        boardState = previewBoard,
                        boardSkin = activeBoardSkin,
                        pieceStyle = activePieceStyle,
                        selectedSquare = previewSelectedSq,
                        legalDestinations = legals,
                        showCoordinates = preferences.showCoordinates,
                        showEvalBar = preferences.showEvalBar,
                        evalCentipawns = 145,
                        lastMoveClassification = if (preferences.showMoveClassificationBadges) MoveClassification.BRILLIANT else null,
                        bestMoveArrowUci = "b2b4",
                        onSquareClick = { sq ->
                            val candidate = legals.firstOrNull { it.to == sq }
                            if (candidate != null) {
                                previewBoard = ChessEngine.applyMoveUnchecked(previewBoard, candidate)
                                previewSelectedSq = null
                            } else {
                                previewSelectedSq = sq
                            }
                        }
                    )
                }
            }
        }

        // 2. Global App UI Themes (True OLED, Chess.com Style, and 4 Author Themes)
        item {
            Text(
                text = "Глобальные темы интерфейса (OLED, Chess.com, Авторские)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(AppUiTheme.values().toList(), key = { it.id }) { theme ->
            val isSelected = theme == activeUiTheme
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) theme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectAppUiTheme(theme) }
                    .testTag("app_theme_card_${theme.id.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Color Swatch Preview
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        ) {
                            Box(modifier = Modifier.size(22.dp, 40.dp).background(theme.background))
                            Box(modifier = Modifier.size(22.dp, 40.dp).background(theme.primary))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(theme.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = theme.badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = theme.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Выбрано", tint = theme.primary)
                    }
                }
            }
        }

        // 3. 10 Board Skins Section
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Коллекция тем шахматной доски (10 скинов)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(BoardSkin.values().toList(), key = { it.id }) { skin ->
            val isSelected = skin == activeBoardSkin
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectBoardSkin(skin) }
                    .testTag("board_skin_card_${skin.id.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // 2x2 Mini Checkerboard Swatch
                        Column(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, skin.borderColor, RoundedCornerShape(8.dp))
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize().background(skin.lightSquare))
                                Box(modifier = Modifier.weight(1f).fillMaxSize().background(skin.darkSquare))
                            }
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize().background(skin.darkSquare))
                                Box(modifier = Modifier.weight(1f).fillMaxSize().background(skin.lightSquare))
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(skin.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = skin.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = skin.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Выбрано", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 4. 6 Piece Styles Section
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Наборы шахматных фигур (6 векторных стилей)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(PieceStyle.values().toList(), key = { it.id }) { style ->
            val isSelected = style == activePieceStyle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectPieceStyle(style) }
                    .testTag("piece_style_card_${style.id.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(style.whiteFill)
                                    .border(2.dp, style.whiteStroke, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(style.blackFill)
                                    .border(2.dp, style.blackStroke, CircleShape)
                            )
                        }
                        Column {
                            Text(style.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Выбрано", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 5. Visual Display Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Индикаторы доски и анализа", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Бейджи оценки ходов на доске (!!, !, ★, ?!, ??)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = preferences.showMoveClassificationBadges,
                            onCheckedChange = { onUpdateToggles(null, it, null) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Вертикальная шкала оценки позиции (Eval Bar)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = preferences.showEvalBar,
                            onCheckedChange = { onUpdateToggles(null, null, it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Координаты полей (a–h, 1–8)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = preferences.showCoordinates,
                            onCheckedChange = { onUpdateToggles(it, null, null) }
                        )
                    }
                }
            }
        }

        // 6. Lichess OAuth / Personal API Token Card
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Подключение аккаунта Lichess (OAuth2 / API Token)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Укажите ваш никнейм и персональный токен Lichess для живой онлайн-игры с реальными людьми, отправки вызовов, вступления в турниры и клубы:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Ваш никнейм на Lichess") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lichess_username_input")
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Персональный API Токен (lip_...)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lichess_token_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveCredentials(usernameInput, tokenInput)
                                credentialsSavedToast = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_lichess_credentials_button")
                        ) {
                            Text("Сохранить аккаунт")
                        }
                        OutlinedButton(
                            onClick = {
                                val url = "https://lichess.org/account/oauth/token/create?scopes[]=board:play&scopes[]=challenge:read&scopes[]=challenge:write&scopes[]=tournament:write&scopes[]=team:write&description=LibreChess+Studio"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Создать токен", fontSize = 12.sp)
                        }
                    }
                    if (credentialsSavedToast) {
                        Text(
                            text = "Аккаунт Lichess сохранен и синхронизирован!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.GameAnalyticsEngine
import com.example.chess.MoveClassification
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.ui.theme.BoardSkin
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveChessBoardWithEval(
    boardState: BoardState,
    boardSkin: BoardSkin,
    pieceStyle: PieceStyle,
    selectedSquare: Square?,
    legalDestinations: List<ChessMove>,
    flipped: Boolean = false,
    showCoordinates: Boolean = true,
    showEvalBar: Boolean = true,
    evalCentipawns: Int = 20,
    lastMoveClassification: MoveClassification? = null,
    bestMoveArrowUci: String? = null,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val winPctWhite = (GameAnalyticsEngine.centipawnsToWinPercent(evalCentipawns) / 100.0).toFloat().coerceIn(0.04f, 0.96f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (showEvalBar) 1.07f else 1f)
    ) {
        if (showEvalBar) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, boardSkin.borderColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val whiteHeight = size.height * winPctWhite
                    val blackHeight = size.height - whiteHeight
                    if (!flipped) {
                        drawRect(color = Color(0xFF1E2229), topLeft = Offset.Zero, size = Size(size.width, blackHeight))
                        drawRect(color = Color(0xFFF1F5F9), topLeft = Offset(0f, blackHeight), size = Size(size.width, whiteHeight))
                    } else {
                        drawRect(color = Color(0xFFF1F5F9), topLeft = Offset.Zero, size = Size(size.width, whiteHeight))
                        drawRect(color = Color(0xFF1E2229), topLeft = Offset(0f, whiteHeight), size = Size(size.width, blackHeight))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, boardSkin.borderColor, RoundedCornerShape(12.dp))
                .background(boardSkin.darkSquare)
                .testTag("interactive_chess_board")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(flipped, boardState) {
                        detectTapGestures { offset ->
                            val sqSize = size.width / 8f
                            val col = (offset.x / sqSize).toInt().coerceIn(0, 7)
                            val row = (offset.y / sqSize).toInt().coerceIn(0, 7)
                            val file = if (flipped) 7 - col else col
                            val rank = if (flipped) row else 7 - row
                            onSquareClick(Square(file, rank))
                        }
                    }
            ) {
                val sqSize = size.width / 8f
                val kingInCheckSq = if (ChessEngine.isInCheck(boardState, boardState.activeColor)) {
                    ChessEngine.findKingSquare(boardState, boardState.activeColor)
                } else null

                // 1. Draw 64 squares + skin textures
                for (row in 0..7) {
                    for (col in 0..7) {
                        val file = if (flipped) 7 - col else col
                        val rank = if (flipped) row else 7 - row
                        val sq = Square(file, rank)
                        val isLight = (file + rank) % 2 != 0
                        val topLeft = Offset(col * sqSize, row * sqSize)

                        drawRect(
                            color = if (isLight) boardSkin.lightSquare else boardSkin.darkSquare,
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )

                        // Skin procedural textures
                        if (boardSkin.hasWoodGrain) {
                            val grainColor = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.08f)
                            for (g in 1..3) {
                                val yOff = topLeft.y + (sqSize * g / 4f)
                                drawLine(
                                    color = grainColor,
                                    start = Offset(topLeft.x, yOff),
                                    end = Offset(topLeft.x + sqSize, yOff + (if ((file + g) % 2 == 0) 3f else -3f)),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                        if (boardSkin.hasMarbleVeins && ((file * 3 + rank) % 3 == 0)) {
                            val veinColor = if (isLight) Color(0x22475569) else Color(0x22E2E8F0)
                            drawLine(
                                color = veinColor,
                                start = Offset(topLeft.x + sqSize * 0.15f, topLeft.y + sqSize * 0.8f),
                                end = Offset(topLeft.x + sqSize * 0.85f, topLeft.y + sqSize * 0.2f),
                                strokeWidth = 2f
                            )
                        }
                        if (boardSkin.hasNeonGrid) {
                            drawRect(
                                color = boardSkin.borderColor.copy(alpha = 0.22f),
                                topLeft = topLeft,
                                size = Size(sqSize, sqSize),
                                style = Stroke(width = 1.2f)
                            )
                        }

                        // Last move highlight
                        if (boardState.lastMove?.from == sq || boardState.lastMove?.to == sq) {
                            drawRect(
                                color = boardSkin.lastMoveHighlight,
                                topLeft = topLeft,
                                size = Size(sqSize, sqSize)
                            )
                        }

                        // Selected square highlight
                        if (selectedSquare == sq) {
                            drawRect(
                                color = boardSkin.selectedHighlight,
                                topLeft = topLeft,
                                size = Size(sqSize, sqSize)
                            )
                        }

                        // Check radial danger glow
                        if (kingInCheckSq == sq) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xEEFF1744), Color(0x00FF1744)),
                                    center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f),
                                    radius = sqSize * 0.65f
                                ),
                                center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f),
                                radius = sqSize * 0.65f
                            )
                        }

                        // Coordinates
                        if (showCoordinates) {
                            val coordColor = if (isLight) boardSkin.coordLightColor else boardSkin.coordDarkColor
                            if (col == 0) {
                                val rankText = (rank + 1).toString()
                                val measured = textMeasurer.measure(
                                    rankText,
                                    style = TextStyle(
                                        color = coordColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                )
                                drawText(measured, topLeft = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + 2.dp.toPx()))
                            }
                            if (row == 7) {
                                val fileText = ('a' + file).toString()
                                val measured = textMeasurer.measure(
                                    fileText,
                                    style = TextStyle(
                                        color = coordColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                )
                                drawText(
                                    measured,
                                    topLeft = Offset(
                                        topLeft.x + sqSize - measured.size.width - 3.dp.toPx(),
                                        topLeft.y + sqSize - measured.size.height - 1.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Draw Pieces in selected PieceStyle
                for (row in 0..7) {
                    for (col in 0..7) {
                        val file = if (flipped) 7 - col else col
                        val rank = if (flipped) row else 7 - row
                        val sq = Square(file, rank)
                        val piece = boardState.pieceAt(sq) ?: continue
                        val center = Offset((col + 0.5f) * sqSize, (row + 0.5f) * sqSize)
                        drawCustomStyledPiece(
                            piece = piece,
                            style = pieceStyle,
                            center = center,
                            sqSize = sqSize
                        )
                    }
                }

                // 3. Draw Legal Move markers (dots and capture rings)
                for (move in legalDestinations) {
                    val col = if (flipped) 7 - move.to.file else move.to.file
                    val row = if (flipped) move.to.rank else 7 - move.to.rank
                    val center = Offset((col + 0.5f) * sqSize, (row + 0.5f) * sqSize)
                    if (move.isCapture) {
                        drawCircle(
                            color = pieceStyle.accentGlow.copy(alpha = 0.78f),
                            radius = sqSize * 0.42f,
                            center = center,
                            style = Stroke(width = sqSize * 0.09f)
                        )
                    } else {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.34f),
                            radius = sqSize * 0.16f,
                            center = center
                        )
                        drawCircle(
                            color = pieceStyle.accentGlow.copy(alpha = 0.85f),
                            radius = sqSize * 0.13f,
                            center = center
                        )
                    }
                }

                // 4. Draw Best-Move Arrow if present in Analysis / Review
                if (!bestMoveArrowUci.isNullOrBlank() && bestMoveArrowUci.length >= 4) {
                    val fromSq = Square.fromAlgebraic(bestMoveArrowUci.substring(0, 2))
                    val toSq = Square.fromAlgebraic(bestMoveArrowUci.substring(2, 4))
                    if (fromSq != null && toSq != null) {
                        val fCol = if (flipped) 7 - fromSq.file else fromSq.file
                        val fRow = if (flipped) fromSq.rank else 7 - fromSq.rank
                        val tCol = if (flipped) 7 - toSq.file else toSq.file
                        val tRow = if (flipped) toSq.rank else 7 - toSq.rank
                        drawMoveArrow(
                            start = Offset((fCol + 0.5f) * sqSize, (fRow + 0.5f) * sqSize),
                            end = Offset((tCol + 0.5f) * sqSize, (tRow + 0.5f) * sqSize),
                            color = Color(0xCC2ECC71),
                            sqSize = sqSize
                        )
                    }
                }

                // 5. Draw Chess.com / Lichess Game Review Move Classification Badge (!!, !, ★, ?!, ?, ??)
                val lastTo = boardState.lastMove?.to
                if (lastMoveClassification != null && lastTo != null) {
                    val col = if (flipped) 7 - lastTo.file else lastTo.file
                    val row = if (flipped) lastTo.rank else 7 - lastTo.rank
                    val badgeCenter = Offset((col + 0.82f) * sqSize, (row + 0.20f) * sqSize)
                    val badgeRadius = sqSize * 0.20f
                    drawCircle(
                        color = Color(0xFF0E1116),
                        radius = badgeRadius + 2f,
                        center = badgeCenter
                    )
                    drawCircle(
                        color = lastMoveClassification.color,
                        radius = badgeRadius,
                        center = badgeCenter
                    )
                    val measured = textMeasurer.measure(
                        lastMoveClassification.badgeSymbol,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    )
                    drawText(
                        measured,
                        topLeft = Offset(
                            badgeCenter.x - measured.size.width / 2f,
                            badgeCenter.y - measured.size.height / 2f
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMoveArrow(
    start: Offset,
    end: Offset,
    color: Color,
    sqSize: Float
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLen = sqSize * 0.34f
    val shortenedEnd = Offset(
        end.x - cos(angle) * (headLen * 0.55f),
        end.y - sin(angle) * (headLen * 0.55f)
    )
    drawLine(
        color = color,
        start = start,
        end = shortenedEnd,
        strokeWidth = sqSize * 0.14f,
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            end.x - headLen * cos(angle - 0.55f),
            end.y - headLen * sin(angle - 0.55f)
        )
        lineTo(
            end.x - headLen * cos(angle + 0.55f),
            end.y - headLen * sin(angle + 0.55f)
        )
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawCustomStyledPiece(
    piece: ChessPiece,
    style: PieceStyle,
    center: Offset,
    sqSize: Float
) {
    val fill = if (piece.color == PieceColor.WHITE) style.whiteFill else style.blackFill
    val stroke = if (piece.color == PieceColor.WHITE) style.whiteStroke else style.blackStroke
    val strokeWidth = if (style == PieceStyle.OLED_WIREFRAME) sqSize * 0.055f else sqSize * 0.042f
    val r = sqSize * 0.36f

    // Subtle drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = r * 0.92f,
        center = Offset(center.x, center.y + sqSize * 0.06f)
    )

    // Base pedestal for all pieces
    drawRoundRect(
        color = fill,
        topLeft = Offset(center.x - r * 0.78f, center.y + r * 0.52f),
        size = Size(r * 1.56f, r * 0.32f),
        cornerRadius = CornerRadius(r * 0.15f, r * 0.15f)
    )
    drawRoundRect(
        color = stroke,
        topLeft = Offset(center.x - r * 0.78f, center.y + r * 0.52f),
        size = Size(r * 1.56f, r * 0.32f),
        cornerRadius = CornerRadius(r * 0.15f, r * 0.15f),
        style = Stroke(width = strokeWidth)
    )

    when (piece.type) {
        PieceType.PAWN -> {
            val body = Path().apply {
                moveTo(center.x - r * 0.52f, center.y + r * 0.52f)
                quadraticTo(center.x - r * 0.25f, center.y, center.x - r * 0.22f, center.y - r * 0.18f)
                lineTo(center.x + r * 0.22f, center.y - r * 0.18f)
                quadraticTo(center.x + r * 0.25f, center.y, center.x + r * 0.52f, center.y + r * 0.52f)
                close()
            }
            drawPath(body, color = fill)
            drawPath(body, color = stroke, style = Stroke(width = strokeWidth))
            drawCircle(color = fill, radius = r * 0.34f, center = Offset(center.x, center.y - r * 0.42f))
            drawCircle(color = stroke, radius = r * 0.34f, center = Offset(center.x, center.y - r * 0.42f), style = Stroke(width = strokeWidth))
        }
        PieceType.ROOK -> {
            val tower = Path().apply {
                moveTo(center.x - r * 0.54f, center.y + r * 0.52f)
                lineTo(center.x - r * 0.44f, center.y - r * 0.30f)
                lineTo(center.x - r * 0.58f, center.y - r * 0.30f)
                lineTo(center.x - r * 0.58f, center.y - r * 0.68f)
                lineTo(center.x - r * 0.30f, center.y - r * 0.68f)
                lineTo(center.x - r * 0.30f, center.y - r * 0.48f)
                lineTo(center.x - r * 0.12f, center.y - r * 0.48f)
                lineTo(center.x - r * 0.12f, center.y - r * 0.68f)
                lineTo(center.x + r * 0.12f, center.y - r * 0.68f)
                lineTo(center.x + r * 0.12f, center.y - r * 0.48f)
                lineTo(center.x + r * 0.30f, center.y - r * 0.48f)
                lineTo(center.x + r * 0.30f, center.y - r * 0.68f)
                lineTo(center.x + r * 0.58f, center.y - r * 0.68f)
                lineTo(center.x + r * 0.58f, center.y - r * 0.30f)
                lineTo(center.x + r * 0.44f, center.y - r * 0.30f)
                lineTo(center.x + r * 0.54f, center.y + r * 0.52f)
                close()
            }
            drawPath(tower, color = fill)
            drawPath(tower, color = stroke, style = Stroke(width = strokeWidth))
        }
        PieceType.KNIGHT -> {
            val horse = Path().apply {
                moveTo(center.x - r * 0.56f, center.y + r * 0.52f)
                quadraticTo(center.x - r * 0.50f, center.y - r * 0.15f, center.x - r * 0.68f, center.y - r * 0.22f)
                lineTo(center.x - r * 0.25f, center.y - r * 0.68f)
                lineTo(center.x - r * 0.08f, center.y - r * 0.84f)
                lineTo(center.x + r * 0.10f, center.y - r * 0.65f)
                quadraticTo(center.x + r * 0.62f, center.y - r * 0.45f, center.x + r * 0.54f, center.y + r * 0.52f)
                close()
            }
            drawPath(horse, color = fill)
            drawPath(horse, color = stroke, style = Stroke(width = strokeWidth))
            drawCircle(
                color = stroke,
                radius = r * 0.09f,
                center = Offset(center.x - r * 0.20f, center.y - r * 0.42f)
            )
        }
        PieceType.BISHOP -> {
            val mitre = Path().apply {
                moveTo(center.x - r * 0.54f, center.y + r * 0.52f)
                quadraticTo(center.x - r * 0.62f, center.y - r * 0.25f, center.x, center.y - r * 0.72f)
                quadraticTo(center.x + r * 0.62f, center.y - r * 0.25f, center.x + r * 0.54f, center.y + r * 0.52f)
                close()
            }
            drawPath(mitre, color = fill)
            drawPath(mitre, color = stroke, style = Stroke(width = strokeWidth))
            drawCircle(color = fill, radius = r * 0.13f, center = Offset(center.x, center.y - r * 0.82f))
            drawCircle(color = stroke, radius = r * 0.13f, center = Offset(center.x, center.y - r * 0.82f), style = Stroke(width = strokeWidth))
            drawLine(
                color = stroke,
                start = Offset(center.x - r * 0.18f, center.y - r * 0.22f),
                end = Offset(center.x + r * 0.18f, center.y - r * 0.22f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = stroke,
                start = Offset(center.x, center.y - r * 0.40f),
                end = Offset(center.x, center.y - r * 0.04f),
                strokeWidth = strokeWidth
            )
        }
        PieceType.QUEEN -> {
            val crown = Path().apply {
                moveTo(center.x - r * 0.60f, center.y + r * 0.52f)
                lineTo(center.x - r * 0.78f, center.y - r * 0.52f)
                lineTo(center.x - r * 0.36f, center.y - r * 0.12f)
                lineTo(center.x, center.y - r * 0.68f)
                lineTo(center.x + r * 0.36f, center.y - r * 0.12f)
                lineTo(center.x + r * 0.78f, center.y - r * 0.52f)
                lineTo(center.x + r * 0.60f, center.y + r * 0.52f)
                close()
            }
            drawPath(crown, color = fill)
            drawPath(crown, color = stroke, style = Stroke(width = strokeWidth))
            for (pt in listOf(
                Offset(center.x - r * 0.78f, center.y - r * 0.58f),
                Offset(center.x, center.y - r * 0.75f),
                Offset(center.x + r * 0.78f, center.y - r * 0.58f)
            )) {
                drawCircle(color = style.accentGlow, radius = r * 0.11f, center = pt)
                drawCircle(color = stroke, radius = r * 0.11f, center = pt, style = Stroke(width = strokeWidth * 0.8f))
            }
        }
        PieceType.KING -> {
            val robe = Path().apply {
                moveTo(center.x - r * 0.58f, center.y + r * 0.52f)
                quadraticTo(center.x - r * 0.70f, center.y - r * 0.35f, center.x, center.y - r * 0.48f)
                quadraticTo(center.x + r * 0.70f, center.y - r * 0.35f, center.x + r * 0.58f, center.y + r * 0.52f)
                close()
            }
            drawPath(robe, color = fill)
            drawPath(robe, color = stroke, style = Stroke(width = strokeWidth))
            // Royal Cross
            drawLine(
                color = stroke,
                start = Offset(center.x, center.y - r * 0.92f),
                end = Offset(center.x, center.y - r * 0.48f),
                strokeWidth = strokeWidth * 1.25f
            )
            drawLine(
                color = stroke,
                start = Offset(center.x - r * 0.22f, center.y - r * 0.74f),
                end = Offset(center.x + r * 0.22f, center.y - r * 0.74f),
                strokeWidth = strokeWidth * 1.25f
            )
        }
    }
}

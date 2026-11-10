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
import androidx.compose.ui.graphics.StrokeJoin
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

                // 2. Draw Pieces in authentic Staunton / Lichess cburnett proportions
                for (row in 0..7) {
                    for (col in 0..7) {
                        val file = if (flipped) 7 - col else col
                        val rank = if (flipped) row else 7 - row
                        val sq = Square(file, rank)
                        val piece = boardState.pieceAt(sq) ?: continue
                        val topLeft = Offset(col * sqSize, row * sqSize)
                        drawStauntonPiece(
                            piece = piece,
                            style = pieceStyle,
                            topLeft = topLeft,
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
                            color = pieceStyle.accentGlow.copy(alpha = 0.82f),
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
                            color = pieceStyle.accentGlow.copy(alpha = 0.88f),
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

/**
 * Draws tournament-standard Staunton / Lichess cburnett chess pieces on a 45x45 virtual grid
 * with high-contrast outlines and interior engraving lines so every piece is immediately recognizable.
 */
private fun DrawScope.drawStauntonPiece(
    piece: ChessPiece,
    style: PieceStyle,
    topLeft: Offset,
    sqSize: Float
) {
    fun px(x45: Float): Float = topLeft.x + (x45 / 45f) * sqSize
    fun py(y45: Float): Float = topLeft.y + (y45 / 45f) * sqSize
    fun pr(r45: Float): Float = (r45 / 45f) * sqSize

    val isWhite = piece.color == PieceColor.WHITE
    val fill = if (isWhite) style.whiteFill else style.blackFill
    val primaryStroke = if (isWhite) {
        Color(0xFF141820)
    } else {
        if (style == PieceStyle.OLED_WIREFRAME || style == PieceStyle.IMPERIAL_GOLD) {
            style.blackStroke
        } else {
            Color(0xFF0A0D12)
        }
    }
    val innerDetailColor = if (isWhite) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val outerContrastHalo = if (isWhite) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.45f)

    val sw = pr(1.85f)
    val haloSw = pr(3.2f)
    val strokeStyle = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val haloStrokeStyle = Stroke(width = haloSw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val detailStrokeStyle = Stroke(width = pr(1.35f), cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun drawShapeWithContour(path: Path) {
        drawPath(path = path, color = outerContrastHalo, style = haloStrokeStyle)
        drawPath(path = path, color = fill)
        drawPath(path = path, color = primaryStroke, style = strokeStyle)
    }

    // Subtle ground shadow under pedestal
    drawOval(
        color = Color.Black.copy(alpha = 0.26f),
        topLeft = Offset(px(8.5f), py(37.2f)),
        size = Size(pr(28f), pr(4.2f))
    )

    when (piece.type) {
        PieceType.PAWN -> {
            val pawnPath = Path().apply {
                // Bottom pedestal + tapered stem
                moveTo(px(10.0f), py(39.0f))
                lineTo(px(35.0f), py(39.0f))
                cubicTo(px(35.0f), py(35.2f), px(31.5f), py(33.5f), px(29.5f), py(32.0f))
                cubicTo(px(26.5f), py(27.5f), px(25.5f), py(23.0f), px(25.5f), py(19.0f))
                lineTo(px(28.2f), py(18.0f))
                lineTo(px(28.2f), py(16.2f))
                lineTo(px(16.8f), py(16.2f))
                lineTo(px(16.8f), py(18.0f))
                lineTo(px(19.5f), py(19.0f))
                cubicTo(px(19.5f), py(23.0f), px(18.5f), py(27.5f), px(15.5f), py(32.0f))
                cubicTo(px(13.5f), py(33.5f), px(10.0f), py(35.2f), px(10.0f), py(39.0f))
                close()
            }
            drawShapeWithContour(pawnPath)

            // Round Staunton head
            val headCenter = Offset(px(22.5f), py(11.3f))
            drawCircle(color = outerContrastHalo, radius = pr(5.2f), center = headCenter, style = haloStrokeStyle)
            drawCircle(color = fill, radius = pr(5.2f), center = headCenter)
            drawCircle(color = primaryStroke, radius = pr(5.2f), center = headCenter, style = strokeStyle)

            // Base & collar engravings
            drawLine(
                color = innerDetailColor,
                start = Offset(px(13.0f), py(35.5f)),
                end = Offset(px(32.0f), py(35.5f)),
                strokeWidth = pr(1.25f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(15.5f), py(32.2f)),
                end = Offset(px(29.5f), py(32.2f)),
                strokeWidth = pr(1.2f),
                cap = StrokeCap.Round
            )
        }

        PieceType.ROOK -> {
            // Classic castle tower with 3 distinct battlements
            val rookPath = Path().apply {
                moveTo(px(9.0f), py(39.0f))
                lineTo(px(36.0f), py(39.0f))
                lineTo(px(35.0f), py(35.2f))
                lineTo(px(32.0f), py(31.8f))
                lineTo(px(31.0f), py(18.5f))
                lineTo(px(34.2f), py(15.5f))
                lineTo(px(34.2f), py(9.2f))
                lineTo(px(29.5f), py(9.2f))
                lineTo(px(29.5f), py(12.4f))
                lineTo(px(25.2f), py(12.4f))
                lineTo(px(25.2f), py(9.2f))
                lineTo(px(19.8f), py(9.2f))
                lineTo(px(19.8f), py(12.4f))
                lineTo(px(15.5f), py(12.4f))
                lineTo(px(15.5f), py(9.2f))
                lineTo(px(10.8f), py(9.2f))
                lineTo(px(10.8f), py(15.5f))
                lineTo(px(14.0f), py(18.5f))
                lineTo(px(13.0f), py(31.8f))
                lineTo(px(10.0f), py(35.2f))
                close()
            }
            drawShapeWithContour(rookPath)

            // Horizontal masonry bands
            for (yBand in listOf(15.5f, 18.5f, 31.8f, 35.2f)) {
                val xMargin = when (yBand) {
                    15.5f -> 12.2f
                    18.5f -> 14.5f
                    31.8f -> 13.5f
                    else -> 11.2f
                }
                drawLine(
                    color = innerDetailColor,
                    start = Offset(px(xMargin), py(yBand)),
                    end = Offset(px(45f - xMargin), py(yBand)),
                    strokeWidth = pr(1.25f),
                    cap = StrokeCap.Round
                )
            }
        }

        PieceType.KNIGHT -> {
            // Base pedestal
            val basePath = Path().apply {
                moveTo(px(9.5f), py(39.0f))
                lineTo(px(35.5f), py(39.0f))
                lineTo(px(34.0f), py(33.2f))
                lineTo(px(11.0f), py(33.2f))
                close()
            }
            drawShapeWithContour(basePath)

            // Unmistakable Staunton Horse Head & Arched Neck
            val horsePath = Path().apply {
                moveTo(px(13.5f), py(33.2f))
                // Front chest & throat curve up to jaw
                cubicTo(px(13.8f), py(27.5f), px(17.5f), py(24.2f), px(20.2f), py(21.0f))
                // Lower jaw & mouth notch
                lineTo(px(15.2f), py(23.6f))
                lineTo(px(13.5f), py(22.0f))
                lineTo(px(16.5f), py(19.2f))
                // Long muzzle / nose bridge extending left
                lineTo(px(11.0f), py(21.0f))
                cubicTo(px(8.2f), py(20.0f), px(8.2f), py(16.8f), px(10.8f), py(15.0f))
                lineTo(px(17.0f), py(11.4f))
                // Forelock & twin pointed horse ears
                lineTo(px(16.2f), py(7.2f))
                lineTo(px(19.8f), py(10.0f))
                lineTo(px(22.5f), py(6.2f))
                lineTo(px(25.2f), py(9.8f))
                // Muscular arched neck & mane sweeping down right side
                cubicTo(px(32.5f), py(11.5f), px(35.5f), py(20.0f), px(33.2f), py(33.2f))
                close()
            }
            drawShapeWithContour(horsePath)

            // Horse eye, nostril, and mane contour
            drawCircle(
                color = innerDetailColor,
                radius = pr(1.45f),
                center = Offset(px(17.2f), py(13.8f))
            )
            drawCircle(
                color = innerDetailColor,
                radius = pr(1.0f),
                center = Offset(px(11.2f), py(17.5f))
            )
            val maneLine = Path().apply {
                moveTo(px(25.5f), py(12.2f))
                cubicTo(px(30.2f), py(16.5f), px(31.2f), py(23.5f), px(29.8f), py(31.0f))
            }
            drawPath(maneLine, color = innerDetailColor, style = detailStrokeStyle)
            drawLine(
                color = innerDetailColor,
                start = Offset(px(12.0f), py(36.0f)),
                end = Offset(px(33.0f), py(36.0f)),
                strokeWidth = pr(1.2f),
                cap = StrokeCap.Round
            )
        }

        PieceType.BISHOP -> {
            // Base with flared bishop ribbons
            val basePath = Path().apply {
                moveTo(px(9.0f), py(39.0f))
                lineTo(px(36.0f), py(39.0f))
                lineTo(px(34.2f), py(34.8f))
                lineTo(px(31.0f), py(31.5f))
                lineTo(px(14.0f), py(31.5f))
                lineTo(px(10.8f), py(34.8f))
                close()
            }
            drawShapeWithContour(basePath)

            // Pointed Mitre dome (distinctly taller/narrower than King, with diagonal mitre slash)
            val mitrePath = Path().apply {
                moveTo(px(14.5f), py(31.5f))
                cubicTo(px(11.2f), py(24.0f), px(13.2f), py(14.2f), px(22.5f), py(10.2f))
                cubicTo(px(31.8f), py(14.2f), px(33.8f), py(24.0f), px(30.5f), py(31.5f))
                close()
            }
            drawShapeWithContour(mitrePath)

            // Top finial orb (globus)
            val orbCenter = Offset(px(22.5f), py(7.4f))
            drawCircle(color = outerContrastHalo, radius = pr(2.4f), center = orbCenter, style = haloStrokeStyle)
            drawCircle(color = fill, radius = pr(2.4f), center = orbCenter)
            drawCircle(color = primaryStroke, radius = pr(2.4f), center = orbCenter, style = strokeStyle)

            // Diagonal Bishop Mitre Slash (unmistakable vs King's cross) + collar bands
            drawLine(
                color = innerDetailColor,
                start = Offset(px(18.0f), py(17.0f)),
                end = Offset(px(27.0f), py(22.5f)),
                strokeWidth = pr(1.8f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(14.8f), py(28.5f)),
                end = Offset(px(30.2f), py(28.5f)),
                strokeWidth = pr(1.35f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(12.0f), py(35.2f)),
                end = Offset(px(33.0f), py(35.2f)),
                strokeWidth = pr(1.25f),
                cap = StrokeCap.Round
            )
        }

        PieceType.QUEEN -> {
            // Base pedestal
            val basePath = Path().apply {
                moveTo(px(9.0f), py(39.0f))
                lineTo(px(36.0f), py(39.0f))
                lineTo(px(34.2f), py(34.5f))
                lineTo(px(32.0f), py(30.8f))
                lineTo(px(13.0f), py(30.8f))
                lineTo(px(10.8f), py(34.5f))
                close()
            }
            drawShapeWithContour(basePath)

            // 5-Spiked Royal Coronet
            val coronetPath = Path().apply {
                moveTo(px(13.0f), py(30.8f))
                lineTo(px(8.2f), py(14.0f))   // Spike 1 (left)
                lineTo(px(15.5f), py(23.2f))  // Valley 1
                lineTo(px(15.2f), py(11.2f))  // Spike 2
                lineTo(px(20.0f), py(22.2f))  // Valley 2
                lineTo(px(22.5f), py(9.5f))   // Spike 3 (center)
                lineTo(px(25.0f), py(22.2f))  // Valley 3
                lineTo(px(29.8f), py(11.2f))  // Spike 4
                lineTo(px(29.5f), py(23.2f))  // Valley 4
                lineTo(px(36.8f), py(14.0f))  // Spike 5 (right)
                lineTo(px(32.0f), py(30.8f))
                close()
            }
            drawShapeWithContour(coronetPath)

            // 5 Coronet Pearls on spike tips
            val pearls = listOf(
                Offset(px(8.2f), py(11.8f)),
                Offset(px(15.2f), py(9.0f)),
                Offset(px(22.5f), py(7.4f)),
                Offset(px(29.8f), py(9.0f)),
                Offset(px(36.8f), py(11.8f))
            )
            for (pt in pearls) {
                drawCircle(color = outerContrastHalo, radius = pr(1.9f), center = pt, style = haloStrokeStyle)
                drawCircle(color = fill, radius = pr(1.9f), center = pt)
                drawCircle(color = primaryStroke, radius = pr(1.9f), center = pt, style = strokeStyle)
            }

            // Crown base bands & jewels
            drawLine(
                color = innerDetailColor,
                start = Offset(px(13.5f), py(28.2f)),
                end = Offset(px(31.5f), py(28.2f)),
                strokeWidth = pr(1.35f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(11.8f), py(34.8f)),
                end = Offset(px(33.2f), py(34.8f)),
                strokeWidth = pr(1.3f),
                cap = StrokeCap.Round
            )
        }

        PieceType.KING -> {
            // Bold Imperial Cross on top (drawn as full contoured polygon so it is unmistakable)
            val crossPath = Path().apply {
                moveTo(px(20.8f), py(13.5f))
                lineTo(px(20.8f), py(9.2f))
                lineTo(px(17.2f), py(9.2f))
                lineTo(px(17.2f), py(6.2f))
                lineTo(px(20.8f), py(6.2f))
                lineTo(px(20.8f), py(3.2f))
                lineTo(px(24.2f), py(3.2f))
                lineTo(px(24.2f), py(6.2f))
                lineTo(px(27.8f), py(6.2f))
                lineTo(px(27.8f), py(9.2f))
                lineTo(px(24.2f), py(9.2f))
                lineTo(px(24.2f), py(13.5f))
                close()
            }
            drawShapeWithContour(crossPath)

            // Base pedestal
            val basePath = Path().apply {
                moveTo(px(9.0f), py(39.0f))
                lineTo(px(36.0f), py(39.0f))
                lineTo(px(34.2f), py(34.5f))
                lineTo(px(31.8f), py(30.8f))
                lineTo(px(13.2f), py(30.8f))
                lineTo(px(10.8f), py(34.5f))
                close()
            }
            drawShapeWithContour(basePath)

            // Double-Lobed Heart Royal Crown
            val crownPath = Path().apply {
                moveTo(px(13.2f), py(30.8f))
                cubicTo(px(8.5f), py(22.0f), px(9.8f), py(13.2f), px(16.8f), py(13.2f))
                cubicTo(px(19.8f), py(13.2f), px(21.6f), py(15.2f), px(22.5f), py(17.0f))
                cubicTo(px(23.4f), py(15.2f), px(25.2f), py(13.2f), px(28.2f), py(13.2f))
                cubicTo(px(35.2f), py(13.2f), px(36.5f), py(22.0f), px(31.8f), py(30.8f))
                close()
            }
            drawShapeWithContour(crownPath)

            // Interior royal crown arches & center pillar
            drawLine(
                color = innerDetailColor,
                start = Offset(px(22.5f), py(17.0f)),
                end = Offset(px(22.5f), py(28.0f)),
                strokeWidth = pr(1.4f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(13.8f), py(28.2f)),
                end = Offset(px(31.2f), py(28.2f)),
                strokeWidth = pr(1.4f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = innerDetailColor,
                start = Offset(px(11.8f), py(34.8f)),
                end = Offset(px(33.2f), py(34.8f)),
                strokeWidth = pr(1.3f),
                cap = StrokeCap.Round
            )
        }
    }
}

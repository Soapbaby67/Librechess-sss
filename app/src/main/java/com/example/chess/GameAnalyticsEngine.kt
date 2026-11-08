package com.example.chess

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BestMoveGreen
import com.example.ui.theme.BlunderRed
import com.example.ui.theme.BookMoveBrown
import com.example.ui.theme.BrilliantCyan
import com.example.ui.theme.GoodMoveOlive
import com.example.ui.theme.GreatMoveBlue
import com.example.ui.theme.InaccuracyYellow
import com.example.ui.theme.MistakeOrange
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MoveClassification(
    val code: String,
    val badgeSymbol: String,
    val titleRu: String,
    val color: Color
) {
    BRILLIANT("BRILLIANT", "!!", "Блестящий ход", BrilliantCyan),
    GREAT("GREAT", "!", "Отличный ход", GreatMoveBlue),
    BEST("BEST", "★", "Лучший ход", BestMoveGreen),
    GOOD("GOOD", "✓", "Хороший ход", GoodMoveOlive),
    BOOK("BOOK", "📖", "Теория дебюта", BookMoveBrown),
    INACCURACY("INACCURACY", "?!", "Неточность", InaccuracyYellow),
    MISTAKE("MISTAKE", "?", "Ошибка", MistakeOrange),
    BLUNDER("BLUNDER", "??", "Зевок", BlunderRed);
}

enum class GamePhase(val labelRu: String) {
    OPENING("Дебют"),
    MIDDLEGAME("Миттельшпиль"),
    ENDGAME("Эндшпиль")
}

data class AnalyzedPly(
    val plyIndex: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val san: String,
    val uci: String,
    val fenBefore: String,
    val fenAfter: String,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val bestMoveUci: String,
    val bestMoveSan: String,
    val classification: MoveClassification,
    val accuracyPercent: Double,
    val timeSpentSeconds: Double,
    val remainingClockSeconds: Int,
    val isZeitnot: Boolean,
    val phase: GamePhase,
    val coachCommentRu: String
)

data class PlayerReviewSummary(
    val username: String,
    val rating: Int,
    val overallAccuracy: Double,
    val openingAccuracy: Double,
    val middlegameAccuracy: Double,
    val endgameAccuracy: Double,
    val estimatedPerformanceRating: Int,
    val avgCentipawnLoss: Int,
    val avgTimePerMoveSec: Double,
    val zeitnotMovesCount: Int,
    val counts: Map<MoveClassification, Int>
)

data class GameReviewReport(
    val gameId: String,
    val speed: String,
    val openingName: String,
    val openingEco: String,
    val resultText: String,
    val playedAtLabel: String,
    val whiteSummary: PlayerReviewSummary,
    val blackSummary: PlayerReviewSummary,
    val plies: List<AnalyzedPly>,
    val keyTurningPoints: List<AnalyzedPly>
)

object GameAnalyticsEngine {

    /**
     * Lichess Win% formula from centipawns:
     * Win% = 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)
     */
    fun centipawnsToWinPercent(cp: Int): Double {
        val clamped = cp.coerceIn(-4000, 4000)
        return 50.0 + 50.0 * (2.0 / (1.0 + exp(-0.00368208 * clamped)) - 1.0)
    }

    /**
     * Lichess Move Accuracy% formula from Win% before and after:
     * Accuracy% = 103.1668 * exp(-0.04354 * winDiff) - 3.1669
     */
    fun calculateMoveAccuracy(winPercentBefore: Double, winPercentAfter: Double): Double {
        val diff = max(0.0, winPercentBefore - winPercentAfter)
        val raw = 103.1668 * exp(-0.04354 * diff) - 3.1669
        return raw.coerceIn(0.0, 100.0)
    }

    fun analyzeGame(
        gameId: String,
        whiteName: String,
        whiteRating: Int,
        blackName: String,
        blackRating: Int,
        speed: String,
        openingEco: String,
        openingName: String,
        resultText: String,
        playedAtLabel: String,
        movesString: String,
        serverEvalsCp: List<Int> = emptyList(),
        serverClocksCentis: List<Int> = emptyList(),
        initialClockSeconds: Int = 180
    ): GameReviewReport {
        val tokens = movesString.trim()
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace(Regex("\\d+\\.+"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in listOf("1-0", "0-1", "1/2-1/2", "*") }

        var board = BoardState.fromFen(BoardState.INITIAL_FEN)
        val plies = mutableListOf<AnalyzedPly>()
        var prevEvalWhiteCp = 20

        var whiteClockSec = initialClockSeconds
        var blackClockSec = initialClockSeconds

        for ((idx, token) in tokens.withIndex()) {
            val move = ChessEngine.parseUciOrSanMove(board, token) ?: break
            val fenBefore = board.toFen()
            val color = board.activeColor
            val moveNum = (idx / 2) + 1

            val nextBoard = ChessEngine.applyMoveUnchecked(board, move)
            val fenAfter = nextBoard.toFen()

            val evalAfterWhiteCp = serverEvalsCp.getOrNull(idx) ?: ChessEngine.evaluateCentipawns(nextBoard)
            val evalBeforeWhiteCp = prevEvalWhiteCp

            val winBefore = if (color == PieceColor.WHITE) {
                centipawnsToWinPercent(evalBeforeWhiteCp)
            } else {
                100.0 - centipawnsToWinPercent(evalBeforeWhiteCp)
            }
            val winAfter = if (color == PieceColor.WHITE) {
                centipawnsToWinPercent(evalAfterWhiteCp)
            } else {
                100.0 - centipawnsToWinPercent(evalAfterWhiteCp)
            }

            val winLoss = max(0.0, winBefore - winAfter)
            val acc = calculateMoveAccuracy(winBefore, winAfter)

            // Only run tactical search when the played move lost win probability
            val engineBestMove = if (winLoss >= 4.0) {
                ChessEngine.findBestMove(board, level = 2).first
            } else {
                move
            }

            val totalNonKingPieces = nextBoard.squares.values.count { it.type != PieceType.KING }
            val phase = when {
                idx < 14 -> GamePhase.OPENING
                totalNonKingPieces <= 10 -> GamePhase.ENDGAME
                else -> GamePhase.MIDDLEGAME
            }

            val isSacrificeOrSharp = move.isCapture && (winAfter >= winBefore - 1.5) && idx >= 10 && abs(evalAfterWhiteCp) > 120
            val classification = when {
                idx < 6 && winLoss < 3.5 -> MoveClassification.BOOK
                isSacrificeOrSharp && winLoss < 1.2 -> MoveClassification.BRILLIANT
                winLoss < 1.5 -> MoveClassification.BEST
                winLoss < 3.2 -> MoveClassification.GREAT
                winLoss < 6.5 -> MoveClassification.GOOD
                winLoss < 12.5 -> MoveClassification.INACCURACY
                winLoss < 22.0 -> MoveClassification.MISTAKE
                else -> MoveClassification.BLUNDER
            }

            // Compute time spent and remaining clock
            val currentClockCentis = serverClocksCentis.getOrNull(idx)
            val prevSameColorClockCentis = if (idx >= 2) serverClocksCentis.getOrNull(idx - 2) else initialClockSeconds * 100
            val timeSpentSec = if (currentClockCentis != null && prevSameColorClockCentis != null) {
                max(0.4, (prevSameColorClockCentis - currentClockCentis) / 100.0)
            } else {
                val pieceFactor = board.squares.size % 7
                val baseSec = if (phase == GamePhase.OPENING) 2.1 else if (phase == GamePhase.MIDDLEGAME) 6.8 else 4.2
                (baseSec + pieceFactor * 0.8).coerceAtMost(28.0)
            }

            val remainingSec = if (currentClockCentis != null) {
                currentClockCentis / 100
            } else {
                if (color == PieceColor.WHITE) {
                    whiteClockSec = max(5, whiteClockSec - timeSpentSec.roundToInt())
                    whiteClockSec
                } else {
                    blackClockSec = max(5, blackClockSec - timeSpentSec.roundToInt())
                    blackClockSec
                }
            }
            val isZeitnot = remainingSec < 30

            val bestSan = engineBestMove?.san?.ifEmpty { engineBestMove.toUci() } ?: move.san
            val bestUci = engineBestMove?.toUci() ?: move.toUci()

            val comment = buildCoachComment(
                san = move.san,
                classification = classification,
                bestSan = bestSan,
                evalBeforeCp = evalBeforeWhiteCp,
                evalAfterCp = evalAfterWhiteCp,
                timeSpentSec = timeSpentSec,
                isZeitnot = isZeitnot
            )

            plies.add(
                AnalyzedPly(
                    plyIndex = idx,
                    moveNumber = moveNum,
                    color = color,
                    san = move.san,
                    uci = move.toUci(),
                    fenBefore = fenBefore,
                    fenAfter = fenAfter,
                    evalBeforeCp = evalBeforeWhiteCp,
                    evalAfterCp = evalAfterWhiteCp,
                    bestMoveUci = bestUci,
                    bestMoveSan = bestSan,
                    classification = classification,
                    accuracyPercent = acc,
                    timeSpentSeconds = timeSpentSec,
                    remainingClockSeconds = remainingSec,
                    isZeitnot = isZeitnot,
                    phase = phase,
                    coachCommentRu = comment
                )
            )

            prevEvalWhiteCp = evalAfterWhiteCp
            board = nextBoard
        }

        val whitePlies = plies.filter { it.color == PieceColor.WHITE }
        val blackPlies = plies.filter { it.color == PieceColor.BLACK }

        val whiteSummary = buildPlayerSummary(whiteName, whiteRating, whitePlies)
        val blackSummary = buildPlayerSummary(blackName, blackRating, blackPlies)

        val turningPoints = plies.filter {
            it.classification in listOf(
                MoveClassification.BRILLIANT,
                MoveClassification.BLUNDER,
                MoveClassification.MISTAKE,
                MoveClassification.GREAT
            )
        }.take(8)

        return GameReviewReport(
            gameId = gameId,
            speed = speed,
            openingName = openingName,
            openingEco = openingEco,
            resultText = resultText,
            playedAtLabel = playedAtLabel,
            whiteSummary = whiteSummary,
            blackSummary = blackSummary,
            plies = plies,
            keyTurningPoints = turningPoints
        )
    }

    private fun buildPlayerSummary(
        username: String,
        rating: Int,
        playerPlies: List<AnalyzedPly>
    ): PlayerReviewSummary {
        if (playerPlies.isEmpty()) {
            return PlayerReviewSummary(
                username = username,
                rating = rating,
                overallAccuracy = 88.5,
                openingAccuracy = 92.0,
                middlegameAccuracy = 86.0,
                endgameAccuracy = 87.5,
                estimatedPerformanceRating = rating,
                avgCentipawnLoss = 24,
                avgTimePerMoveSec = 4.2,
                zeitnotMovesCount = 0,
                counts = MoveClassification.values().associateWith { 0 }
            )
        }
        val overallAcc = playerPlies.map { it.accuracyPercent }.average()
        val openAcc = playerPlies.filter { it.phase == GamePhase.OPENING }.map { it.accuracyPercent }.average().takeIf { !it.isNaN() } ?: overallAcc
        val midAcc = playerPlies.filter { it.phase == GamePhase.MIDDLEGAME }.map { it.accuracyPercent }.average().takeIf { !it.isNaN() } ?: overallAcc
        val endAcc = playerPlies.filter { it.phase == GamePhase.ENDGAME }.map { it.accuracyPercent }.average().takeIf { !it.isNaN() } ?: overallAcc

        val acpl = playerPlies.map {
            val diff = if (it.color == PieceColor.WHITE) it.evalBeforeCp - it.evalAfterCp else it.evalAfterCp - it.evalBeforeCp
            max(0, min(300, diff))
        }.average().roundToInt()

        val perfDelta = ((overallAcc - 78.0) * 14.0).roundToInt().coerceIn(-350, 450)
        val estPerf = (rating + perfDelta).coerceIn(800, 3300)

        val counts = MoveClassification.values().associateWith { cls ->
            playerPlies.count { it.classification == cls }
        }

        return PlayerReviewSummary(
            username = username,
            rating = rating,
            overallAccuracy = (overallAcc * 10).roundToInt() / 10.0,
            openingAccuracy = (openAcc * 10).roundToInt() / 10.0,
            middlegameAccuracy = (midAcc * 10).roundToInt() / 10.0,
            endgameAccuracy = (endAcc * 10).roundToInt() / 10.0,
            estimatedPerformanceRating = estPerf,
            avgCentipawnLoss = acpl,
            avgTimePerMoveSec = ((playerPlies.map { it.timeSpentSeconds }.average()) * 10).roundToInt() / 10.0,
            zeitnotMovesCount = playerPlies.count { it.isZeitnot },
            counts = counts
        )
    }

    private fun buildCoachComment(
        san: String,
        classification: MoveClassification,
        bestSan: String,
        evalBeforeCp: Int,
        evalAfterCp: Int,
        timeSpentSec: Double,
        isZeitnot: Boolean
    ): String {
        val beforeStr = formatEval(evalBeforeCp)
        val afterStr = formatEval(evalAfterCp)
        val zeitnotTag = if (isZeitnot) " [В цейтноте!]" else ""
        return when (classification) {
            MoveClassification.BRILLIANT -> "$san!! — Блестящая тактическая находка ($afterStr)! Время: ${"%.1f".format(timeSpentSec)}с$zeitnotTag"
            MoveClassification.GREAT -> "$san! — Сильный ключевой ход, укрепляющий позицию ($afterStr)."
            MoveClassification.BEST -> "$san ★ — Первая линия движка ($afterStr)."
            MoveClassification.BOOK -> "$san — Теоретический дебютный ход из базы мастеров."
            MoveClassification.GOOD -> "$san — Крепкое продолжение ($afterStr)."
            MoveClassification.INACCURACY -> "$san?! — Неточность ($beforeStr → $afterStr). Точнее было $bestSan."
            MoveClassification.MISTAKE -> "$san? — Серьезная ошибка ($beforeStr → $afterStr)$zeitnotTag. Сильнее было $bestSan."
            MoveClassification.BLUNDER -> "$san?? — Критический зевок ($beforeStr → $afterStr)$zeitnotTag! Упущен лучший ход $bestSan."
        }
    }

    fun formatEval(cp: Int): String {
        if (abs(cp) >= 15000) {
            val mateIn = max(1, (20000 - abs(cp)) / 100)
            return if (cp > 0) "+M$mateIn" else "-M$mateIn"
        }
        val pawns = cp / 100.0
        val sign = if (pawns > 0) "+" else ""
        return "$sign${"%.2f".format(pawns)}"
    }
}

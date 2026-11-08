package com.example.chess

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class PieceColor {
    WHITE, BLACK;
    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: Char, val sanChar: String, val centipawns: Int) {
    PAWN('p', "", 100),
    KNIGHT('n', "N", 320),
    BISHOP('b', "B", 330),
    ROOK('r', "R", 500),
    QUEEN('q', "Q", 900),
    KING('k', "K", 20000);
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    fun toFenChar(): Char = if (color == PieceColor.WHITE) type.symbol.uppercaseChar() else type.symbol
}

data class Square(val file: Int, val rank: Int) {
    val isValid: Boolean get() = file in 0..7 && rank in 0..7
    fun toAlgebraic(): String = "${('a' + file)}${rank + 1}"

    companion object {
        fun fromAlgebraic(str: String): Square? {
            if (str.length < 2) return null
            val f = str[0].lowercaseChar() - 'a'
            val r = str[1].digitToIntOrNull()?.minus(1) ?: return null
            val sq = Square(f, r)
            return if (sq.isValid) sq else null
        }
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isEnPassant: Boolean = false,
    val isCastlingKingside: Boolean = false,
    val isCastlingQueenside: Boolean = false,
    val san: String = ""
) {
    fun toUci(): String {
        val promo = promotion?.symbol?.toString() ?: ""
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promo"
    }
}

data class BoardState(
    val squares: Map<Square, ChessPiece>,
    val activeColor: PieceColor = PieceColor.WHITE,
    val whiteCastleKingside: Boolean = true,
    val whiteCastleQueenside: Boolean = true,
    val blackCastleKingside: Boolean = true,
    val blackCastleQueenside: Boolean = true,
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val lastMove: ChessMove? = null
) {
    fun pieceAt(sq: Square): ChessPiece? = squares[sq]
    fun pieceAt(file: Int, rank: Int): ChessPiece? = squares[Square(file, rank)]

    fun toFen(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = pieceAt(file, rank)
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.toFenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(if (activeColor == PieceColor.WHITE) " w " else " b ")
        val castling = buildString {
            if (whiteCastleKingside) append('K')
            if (whiteCastleQueenside) append('Q')
            if (blackCastleKingside) append('k')
            if (blackCastleQueenside) append('q')
        }.ifEmpty { "-" }
        sb.append(castling).append(' ')
        sb.append(enPassantTarget?.toAlgebraic() ?: "-").append(' ')
        sb.append(halfMoveClock).append(' ').append(fullMoveNumber)
        return sb.toString()
    }

    fun materialDiff(): Int {
        var whiteMat = 0
        var blackMat = 0
        for ((_, p) in squares) {
            if (p.type == PieceType.KING) continue
            val value = when (p.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT, PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }
            if (p.color == PieceColor.WHITE) whiteMat += value else blackMat += value
        }
        return whiteMat - blackMat
    }

    companion object {
        const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String): BoardState {
            val parts = fen.trim().split(Regex("\\s+"))
            val rows = parts.getOrNull(0)?.split('/') ?: return fromFen(INITIAL_FEN)
            val map = mutableMapOf<Square, ChessPiece>()
            for ((idx, row) in rows.withIndex()) {
                val rank = 7 - idx
                var file = 0
                for (ch in row) {
                    if (ch.isDigit()) {
                        file += ch.digitToInt()
                    } else {
                        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = when (ch.lowercaseChar()) {
                            'p' -> PieceType.PAWN
                            'n' -> PieceType.KNIGHT
                            'b' -> PieceType.BISHOP
                            'r' -> PieceType.ROOK
                            'q' -> PieceType.QUEEN
                            'k' -> PieceType.KING
                            else -> PieceType.PAWN
                        }
                        if (file in 0..7 && rank in 0..7) {
                            map[Square(file, rank)] = ChessPiece(type, color)
                        }
                        file++
                    }
                }
            }
            val turn = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castling = parts.getOrNull(2) ?: "KQkq"
            val ep = parts.getOrNull(3)?.let { if (it == "-") null else Square.fromAlgebraic(it) }
            val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val full = parts.getOrNull(5)?.toIntOrNull() ?: 1
            return BoardState(
                squares = map,
                activeColor = turn,
                whiteCastleKingside = 'K' in castling,
                whiteCastleQueenside = 'Q' in castling,
                blackCastleKingside = 'k' in castling,
                blackCastleQueenside = 'q' in castling,
                enPassantTarget = ep,
                halfMoveClock = half,
                fullMoveNumber = full
            )
        }
    }
}

object ChessEngine {

    private val pawnTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10,-20,-20, 10, 10,  5,
        5, -5,-10,  0,  0,-10, -5,  5,
        0,  0,  0, 25, 25,  0,  0,  0,
        5,  5, 10, 28, 28, 10,  5,  5,
       15, 15, 22, 35, 35, 22, 15, 15,
       50, 50, 50, 50, 50, 50, 50, 50,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightTable = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -30,  5, 15, 18, 18, 15,  5,-30,
        -30,  0, 18, 25, 25, 18,  0,-30,
        -30,  5, 18, 25, 25, 18,  5,-30,
        -30,  0, 15, 18, 18, 15,  0,-30,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    fun findKingSquare(state: BoardState, color: PieceColor): Square? {
        return state.squares.entries.firstOrNull {
            it.value.type == PieceType.KING && it.value.color == color
        }?.key
    }

    fun isSquareAttacked(state: BoardState, target: Square, byColor: PieceColor): Boolean {
        val pawnDir = if (byColor == PieceColor.WHITE) -1 else 1
        for (df in intArrayOf(-1, 1)) {
            val sq = Square(target.file + df, target.rank + pawnDir)
            val p = state.pieceAt(sq)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }
        val knightJumps = arrayOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((df, dr) in knightJumps) {
            val p = state.pieceAt(Square(target.file + df, target.rank + dr))
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val p = state.pieceAt(Square(target.file + df, target.rank + dr))
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }
        val orthoDirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        for ((df, dr) in orthoDirs) {
            var f = target.file + df
            var r = target.rank + dr
            while (f in 0..7 && r in 0..7) {
                val p = state.pieceAt(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += df
                r += dr
            }
        }
        val diagDirs = arrayOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        for ((df, dr) in diagDirs) {
            var f = target.file + df
            var r = target.rank + dr
            while (f in 0..7 && r in 0..7) {
                val p = state.pieceAt(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += df
                r += dr
            }
        }
        return false
    }

    fun isInCheck(state: BoardState, color: PieceColor = state.activeColor): Boolean {
        val kingSq = findKingSquare(state, color) ?: return false
        return isSquareAttacked(state, kingSq, color.opposite())
    }

    fun generatePseudoMoves(state: BoardState, from: Square): List<ChessMove> {
        val piece = state.pieceAt(from) ?: return emptyList()
        if (piece.color != state.activeColor) return emptyList()
        val moves = mutableListOf<ChessMove>()
        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) 1 else -1
                val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
                val promoRank = if (piece.color == PieceColor.WHITE) 7 else 0
                val oneStep = Square(from.file, from.rank + dir)
                if (oneStep.isValid && state.pieceAt(oneStep) == null) {
                    if (oneStep.rank == promoRank) {
                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(ChessMove(from, oneStep, promotion = promo))
                        }
                    } else {
                        moves.add(ChessMove(from, oneStep))
                        val twoStep = Square(from.file, from.rank + 2 * dir)
                        if (from.rank == startRank && state.pieceAt(twoStep) == null) {
                            moves.add(ChessMove(from, twoStep))
                        }
                    }
                }
                for (df in intArrayOf(-1, 1)) {
                    val capSq = Square(from.file + df, from.rank + dir)
                    if (!capSq.isValid) continue
                    val target = state.pieceAt(capSq)
                    if (target != null && target.color != piece.color) {
                        if (capSq.rank == promoRank) {
                            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                moves.add(ChessMove(from, capSq, promotion = promo, isCapture = true))
                            }
                        } else {
                            moves.add(ChessMove(from, capSq, isCapture = true))
                        }
                    } else if (state.enPassantTarget == capSq) {
                        moves.add(ChessMove(from, capSq, isCapture = true, isEnPassant = true))
                    }
                }
            }
            PieceType.KNIGHT -> {
                val jumps = arrayOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((df, dr) in jumps) {
                    val to = Square(from.file + df, from.rank + dr)
                    if (!to.isValid) continue
                    val target = state.pieceAt(to)
                    if (target == null || target.color != piece.color) {
                        moves.add(ChessMove(from, to, isCapture = target != null))
                    }
                }
            }
            PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                val dirs = mutableListOf<Pair<Int, Int>>()
                if (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                    dirs.addAll(listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1))
                }
                if (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN) {
                    dirs.addAll(listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1))
                }
                for ((df, dr) in dirs) {
                    var f = from.file + df
                    var r = from.rank + dr
                    while (f in 0..7 && r in 0..7) {
                        val to = Square(f, r)
                        val target = state.pieceAt(to)
                        if (target == null) {
                            moves.add(ChessMove(from, to))
                        } else {
                            if (target.color != piece.color) {
                                moves.add(ChessMove(from, to, isCapture = true))
                            }
                            break
                        }
                        f += df
                        r += dr
                    }
                }
            }
            PieceType.KING -> {
                for (df in -1..1) {
                    for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val to = Square(from.file + df, from.rank + dr)
                        if (!to.isValid) continue
                        val target = state.pieceAt(to)
                        if (target == null || target.color != piece.color) {
                            moves.add(ChessMove(from, to, isCapture = target != null))
                        }
                    }
                }
                val homeRank = if (piece.color == PieceColor.WHITE) 0 else 7
                val canKing = if (piece.color == PieceColor.WHITE) state.whiteCastleKingside else state.blackCastleKingside
                val canQueen = if (piece.color == PieceColor.WHITE) state.whiteCastleQueenside else state.blackCastleQueenside
                if (from == Square(4, homeRank) && !isInCheck(state, piece.color)) {
                    if (canKing &&
                        state.pieceAt(5, homeRank) == null &&
                        state.pieceAt(6, homeRank) == null &&
                        !isSquareAttacked(state, Square(5, homeRank), piece.color.opposite()) &&
                        !isSquareAttacked(state, Square(6, homeRank), piece.color.opposite())
                    ) {
                        moves.add(ChessMove(from, Square(6, homeRank), isCastlingKingside = true))
                    }
                    if (canQueen &&
                        state.pieceAt(3, homeRank) == null &&
                        state.pieceAt(2, homeRank) == null &&
                        state.pieceAt(1, homeRank) == null &&
                        !isSquareAttacked(state, Square(3, homeRank), piece.color.opposite()) &&
                        !isSquareAttacked(state, Square(2, homeRank), piece.color.opposite())
                    ) {
                        moves.add(ChessMove(from, Square(2, homeRank), isCastlingQueenside = true))
                    }
                }
            }
        }
        return moves
    }

    fun applyMoveUnchecked(state: BoardState, move: ChessMove): BoardState {
        val nextMap = state.squares.toMutableMap()
        val movingPiece = nextMap.remove(move.from) ?: return state
        if (move.isEnPassant) {
            val capturedPawnSq = Square(move.to.file, move.from.rank)
            nextMap.remove(capturedPawnSq)
        }
        if (move.isCastlingKingside) {
            val rank = move.from.rank
            val rook = nextMap.remove(Square(7, rank))
            if (rook != null) nextMap[Square(5, rank)] = rook
        } else if (move.isCastlingQueenside) {
            val rank = move.from.rank
            val rook = nextMap.remove(Square(0, rank))
            if (rook != null) nextMap[Square(3, rank)] = rook
        }
        val placedPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, movingPiece.color)
        } else {
            movingPiece
        }
        nextMap[move.to] = placedPiece

        val wKing = state.whiteCastleKingside &&
            move.from != Square(4, 0) && move.from != Square(7, 0) && move.to != Square(7, 0)
        val wQueen = state.whiteCastleQueenside &&
            move.from != Square(4, 0) && move.from != Square(0, 0) && move.to != Square(0, 0)
        val bKing = state.blackCastleKingside &&
            move.from != Square(4, 7) && move.from != Square(7, 7) && move.to != Square(7, 7)
        val bQueen = state.blackCastleQueenside &&
            move.from != Square(4, 7) && move.from != Square(0, 7) && move.to != Square(0, 7)

        val epTarget = if (movingPiece.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else null

        val nextHalf = if (movingPiece.type == PieceType.PAWN || move.isCapture) 0 else state.halfMoveClock + 1
        val nextFull = if (state.activeColor == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        return BoardState(
            squares = nextMap,
            activeColor = state.activeColor.opposite(),
            whiteCastleKingside = wKing,
            whiteCastleQueenside = wQueen,
            blackCastleKingside = bKing,
            blackCastleQueenside = bQueen,
            enPassantTarget = epTarget,
            halfMoveClock = nextHalf,
            fullMoveNumber = nextFull,
            lastMove = move
        )
    }

    /**
     * Fast move generation without SAN string formatting — used inside search trees.
     */
    fun generateLegalMovesRaw(state: BoardState): List<ChessMove> {
        val rawMoves = mutableListOf<ChessMove>()
        for ((sq, piece) in state.squares) {
            if (piece.color == state.activeColor) {
                for (m in generatePseudoMoves(state, sq)) {
                    val after = applyMoveUnchecked(state, m)
                    if (!isInCheck(after, state.activeColor)) {
                        rawMoves.add(m)
                    }
                }
            }
        }
        return rawMoves
    }

    private fun hasAnyLegalMove(state: BoardState): Boolean {
        for ((sq, piece) in state.squares) {
            if (piece.color == state.activeColor) {
                for (m in generatePseudoMoves(state, sq)) {
                    val after = applyMoveUnchecked(state, m)
                    if (!isInCheck(after, state.activeColor)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun generateLegalMoves(state: BoardState): List<ChessMove> {
        val rawMoves = generateLegalMovesRaw(state)
        return rawMoves.map { move ->
            move.copy(san = formatSan(state, move, rawMoves))
        }
    }

    fun legalMovesFrom(state: BoardState, from: Square): List<ChessMove> {
        val rawMoves = generateLegalMovesRaw(state)
        return rawMoves.filter { it.from == from }.map { move ->
            move.copy(san = formatSan(state, move, rawMoves))
        }
    }

    private fun formatSan(state: BoardState, move: ChessMove, allLegal: List<ChessMove>): String {
        val after = applyMoveUnchecked(state, move)
        val inCheckAfter = isInCheck(after, after.activeColor)
        val suffix = when {
            inCheckAfter && !hasAnyLegalMove(after) -> "#"
            inCheckAfter -> "+"
            else -> ""
        }
        if (move.isCastlingKingside) return "O-O$suffix"
        if (move.isCastlingQueenside) return "O-O-O$suffix"

        val piece = state.pieceAt(move.from) ?: return move.toUci()
        val sb = StringBuilder()
        if (piece.type == PieceType.PAWN) {
            if (move.isCapture) {
                sb.append('a' + move.from.file).append('x')
            }
            sb.append(move.to.toAlgebraic())
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanChar)
            }
        } else {
            sb.append(piece.type.sanChar)
            val ambiguous = allLegal.filter {
                it != move && it.to == move.to && state.pieceAt(it.from)?.type == piece.type
            }
            if (ambiguous.isNotEmpty()) {
                val sameFile = ambiguous.any { it.from.file == move.from.file }
                val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                if (!sameFile) {
                    sb.append('a' + move.from.file)
                } else if (!sameRank) {
                    sb.append(move.from.rank + 1)
                } else {
                    sb.append(move.from.toAlgebraic())
                }
            }
            if (move.isCapture) sb.append('x')
            sb.append(move.to.toAlgebraic())
        }
        sb.append(suffix)
        return sb.toString()
    }

    fun parseUciOrSanMove(state: BoardState, token: String): ChessMove? {
        val clean = token.trim().replace(Regex("[+#?!]+$"), "")
        if (clean.isEmpty()) return null
        val rawLegal = generateLegalMovesRaw(state)
        // First try UCI match (e.g., e2e4, e7e8q) without computing SAN for all moves
        rawLegal.firstOrNull { it.toUci().equals(clean, ignoreCase = true) }?.let { matched ->
            return matched.copy(san = formatSan(state, matched, rawLegal))
        }
        // Handle Lichess castling UCI (e1h1 / e1a1)
        if (clean == "e1h1" || clean == "e8h8") {
            rawLegal.firstOrNull { it.isCastlingKingside }?.let { matched ->
                return matched.copy(san = formatSan(state, matched, rawLegal))
            }
        }
        if (clean == "e1a1" || clean == "e8a8") {
            rawLegal.firstOrNull { it.isCastlingQueenside }?.let { matched ->
                return matched.copy(san = formatSan(state, matched, rawLegal))
            }
        }
        // Try SAN match
        for (candidate in rawLegal) {
            val san = formatSan(state, candidate, rawLegal)
            if (san.replace(Regex("[+#?!]+$"), "").equals(clean, ignoreCase = false)) {
                return candidate.copy(san = san)
            }
        }
        return null
    }

    /**
     * Fast static evaluation in centipawns from White's perspective (+100 = +1.00 pawn for White).
     */
    fun evaluateCentipawns(state: BoardState): Int {
        var score = 0
        for ((sq, piece) in state.squares) {
            val base = piece.type.centipawns
            val tableIdx = if (piece.color == PieceColor.WHITE) {
                sq.rank * 8 + sq.file
            } else {
                (7 - sq.rank) * 8 + sq.file
            }
            val positional = when (piece.type) {
                PieceType.PAWN -> pawnTable[tableIdx]
                PieceType.KNIGHT -> knightTable[tableIdx]
                PieceType.BISHOP -> knightTable[tableIdx] / 2
                PieceType.QUEEN -> knightTable[tableIdx] / 3
                else -> 0
            }
            val total = base + positional
            if (piece.color == PieceColor.WHITE) score += total else score -= total
        }
        return score
    }

    /**
     * Finds best move for AI or local engine analysis.
     * Level 1..8 controls search depth and tactical sharpness.
     */
    fun findBestMove(state: BoardState, level: Int = 5): Pair<ChessMove?, Int> {
        val rawLegal = generateLegalMovesRaw(state)
        if (rawLegal.isEmpty()) {
            val terminalEval = if (isInCheck(state, state.activeColor)) {
                if (state.activeColor == PieceColor.WHITE) -19000 else 19000
            } else 0
            return null to terminalEval
        }

        val depth = when {
            level <= 3 -> 1
            else -> 2
        }
        val maximizing = state.activeColor == PieceColor.WHITE
        var bestMove: ChessMove = rawLegal.first()
        var bestEval = if (maximizing) Int.MIN_VALUE / 2 else Int.MAX_VALUE / 2

        val ordered = rawLegal.sortedByDescending {
            (if (it.isCapture) 500 else 0) + (it.promotion?.centipawns ?: 0)
        }

        for (move in ordered) {
            val next = applyMoveUnchecked(state, move)
            val score = alphaBeta(next, depth - 1, -25000, 25000, !maximizing)
            val jitter = if (level <= 2) Random.nextInt(-65, 65) else if (level <= 4) Random.nextInt(-20, 20) else 0
            val finalScore = score + jitter
            if (maximizing && finalScore > bestEval) {
                bestEval = finalScore
                bestMove = move
            } else if (!maximizing && finalScore < bestEval) {
                bestEval = finalScore
                bestMove = move
            }
        }
        val formattedBest = bestMove.copy(san = formatSan(state, bestMove, rawLegal))
        return formattedBest to bestEval
    }

    private fun alphaBeta(
        state: BoardState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        maximizing: Boolean
    ): Int {
        if (depth <= 0) return evaluateCentipawns(state)
        val legal = generateLegalMovesRaw(state)
        if (legal.isEmpty()) {
            return if (isInCheck(state, state.activeColor)) {
                if (state.activeColor == PieceColor.WHITE) -19000 else 19000
            } else 0
        }

        var alpha = alphaInit
        var beta = betaInit
        if (maximizing) {
            var value = -24000
            for (m in legal) {
                val next = applyMoveUnchecked(state, m)
                value = max(value, alphaBeta(next, depth - 1, alpha, beta, false))
                alpha = max(alpha, value)
                if (alpha >= beta) break
            }
            return value
        } else {
            var value = 24000
            for (m in legal) {
                val next = applyMoveUnchecked(state, m)
                value = min(value, alphaBeta(next, depth - 1, alpha, beta, true))
                beta = min(beta, value)
                if (beta <= alpha) break
            }
            return value
        }
    }
}

package com.example

import com.example.chess.BoardState
import com.example.chess.ChessEngine
import com.example.chess.GameAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun chessEngine_and_gameAnalytics_runInMilliseconds() {
        val startMs = System.currentTimeMillis()
        val initial = BoardState.fromFen(BoardState.INITIAL_FEN)
        val (bestMove, _) = ChessEngine.findBestMove(initial, level = 5)
        assertNotNull(bestMove)

        val report = GameAnalyticsEngine.analyzeGame(
            gameId = "test_speed",
            whiteName = "DrNykterstein",
            whiteRating = 3295,
            blackName = "Alireza2003",
            blackRating = 3210,
            speed = "Blitz 3+0",
            openingEco = "C54",
            openingName = "Evans Gambit",
            resultText = "1-0",
            playedAtLabel = "Now",
            movesString = "e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4 c3 Ba5 d4 exd4 O-O d6 cxd4 Bb6 Nc3 Nf6 Nd5 Nxd5 exd5 Ne7 Re1 O-O Bg5 f6 Bf4 Ng6 Bg3 Bg4 h3 Bxf3 Qxf3 Bxd4 Rad1 Be5 Bd3"
        )
        val elapsedMs = System.currentTimeMillis() - startMs
        assertEquals(37, report.plies.size)
        assertTrue("Expected fast analysis (< 1000ms), was ${elapsedMs}ms", elapsedMs < 1000L)
    }
}

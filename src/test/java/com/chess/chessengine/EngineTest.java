package com.chess.chessengine;

import com.chess.chessengine.util.Fen;
import com.chess.chessengine.util.Move;
import com.chess.chessengine.util.StaticMoveData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {

    @BeforeAll
    static void precompute() {
        StaticMoveData.PrecomputeMoveData();
    }

    private static Board load(String fen) {
        Board board = new Board();
        board.LoadPosition(Fen.LoadFen(fen));
        return board;
    }

    private static long perft(Board board, int depth) {
        ArrayList<Integer> moves = board.MoveGenerator.GenerateMoves();
        if (depth == 1) {
            return moves.size();
        }

        long count = 0;
        for (int move : moves) {
            board.MakeMove(move);
            count += perft(board, depth - 1);
            board.UnmakeMove(move);
        }
        return count;
    }

    // Reference counts from https://www.chessprogramming.org/Perft_Results and python-chess
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; 4; 197281",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1; 3; 97862",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1; 5; 674624",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1; 4; 422333",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8; 3; 62379",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10; 3; 89890",
            "3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1; 5; 185429",
            "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1; 5; 206379",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1; 5; 266199",
            "8/k1P5/8/1K6/8/8/8/8 w - - 0 1; 6; 43261",
            // Pawn that can be taken en passant sits between the king and a rook on the same file: not a check
            "4r2k/8/8/4p3/8/8/4K3/N7 w - e6 0 1; 3; 1073",
            // ...or on the same rank, next to a knight: the knight isn't pinned
            "7k/8/8/KpN4r/8/8/8/8 w - b6 0 1; 3; 1696",
            // Capturing en passant would expose the king to a bishop on the diagonal
            "8/5b2/8/3pP3/8/1K6/8/7k w - d6 0 1; 3; 506",
            // A rook captured away from its corner must not remove castling rights
            "r3k2r/8/8/7R/8/8/8/R3K2R b KQkq - 0 1; 3; 15134",
    })
    void perftMatchesReference(String fen, int depth, long expected) {
        assertEquals(expected, perft(load(fen), depth));
    }

    @Test
    void loadedPositionRoundTrips() {
        Board board = load("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w Kq f6 7 12");
        Fen.LoadedPosition position = board.GenerateLoadedPosition();

        assertTrue(position.whiteToMove());
        assertTrue(position.whiteCastleKingside());
        assertFalse(position.whiteCastleQueenside());
        assertFalse(position.blackCastleKingside());
        assertTrue(position.blackCastleQueenside());
        assertEquals(6, position.enPassantFile());
        assertEquals(7, position.fiftyMoveCounter());
        assertEquals(22, position.plyCount());
    }

    @Test
    void fiftyMoveCounterSurvivesMakeAndUnmake() {
        Board board = load("4k3/8/8/8/8/8/8/R3K3 w Q - 10 40");
        int move = Move.Construct(Fen.IndexFromSquareName("a1"), Fen.IndexFromSquareName("a5"), Move.FLAG_NONE);

        board.MakeMove(move);
        assertEquals(11, board.GenerateLoadedPosition().fiftyMoveCounter());
        assertFalse(board.GenerateLoadedPosition().whiteCastleQueenside());

        board.UnmakeMove(move);
        assertEquals(10, board.GenerateLoadedPosition().fiftyMoveCounter());
        assertTrue(board.GenerateLoadedPosition().whiteCastleQueenside());
    }

    private static String bestMove(String fen) {
        int move = new AI().getBestMove(load(fen));
        return move == -1 ? "none" : Fen.SquareNameFromIndex(Move.GetCurrentSquare(move)) + Fen.SquareNameFromIndex(Move.GetTargetSquare(move));
    }

    @Test
    void aiFindsMateInOneForBothColors() {
        assertEquals("a1a8", bestMove("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1"));
        assertEquals("a8a1", bestMove("r5k1/8/8/8/8/8/5PPP/6K1 b - - 0 1"));
    }

    @Test
    void aiTakesHangingQueenForBothColors() {
        assertEquals("d1d5", bestMove("4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1"));
        assertEquals("d8d4", bestMove("3rk3/8/8/8/3Q4/8/8/4K3 b - - 0 1"));
    }

    @Test
    void aiReturnsNoMoveWhenGameIsOver() {
        assertEquals("none", bestMove("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"));
    }
}

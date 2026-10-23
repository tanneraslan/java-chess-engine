package com.chess.chessengine;

import com.chess.chessengine.util.Fen;
import com.chess.chessengine.util.Move;

import java.io.IOException;
import java.util.ArrayList;

public class PerftTest {
    private static final String startingPosition = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";
    public static final boolean enabled = false;
    public static final boolean debugging = false;
    public static final boolean bulkCounting = true;
    public static final int depth = 6;

    private static long moveGenerationTest(int d) {
        if (d == 0) {
            return 1;
        }

        ArrayList<Integer> moves = Main.Board.MoveGenerator.GenerateMoves();

        if (bulkCounting && d == 1 && !(debugging && d == depth)) {
            return moves.size();
        }

        long positionCount = 0;
        long c;

        for (Integer move : moves) {
            Main.Board.MakeMove(move);
            c = moveGenerationTest(d - 1);
            Main.Board.UnmakeMove(move);

            if (debugging && d == depth) {
                System.out.println(Move.GetName(move) + ": " + c);
            }

            positionCount += c;
        }

        return positionCount;
    }
    public static void start() throws IOException {
        Main.Board.LoadPosition(Fen.LoadFen(startingPosition));

        if (debugging) {
            long c = moveGenerationTest(depth);

            System.out.println("\nNodes searched: " + c);
        } else {
            System.out.println("Performing perft test... (bulk counting " + (bulkCounting ? "enabled" : "disabled") + ")");
            for (int d = 0; d < depth; d++) {
                long t = System.nanoTime();
                long c = moveGenerationTest(d + 1);
                t = System.nanoTime() - t;
                String o = "Depth: " + (d + 1) + " ply | Result: " + c + " positions | Time: " + (double) (t) / 1000000 + " milliseconds";
                System.out.println(o);
            }
        }
    }
}

package com.chess.chessengine;

import com.chess.chessengine.util.Fen;
import com.chess.chessengine.util.Move;
import com.chess.chessengine.util.Piece;
import javafx.beans.property.adapter.ReadOnlyJavaBeanBooleanProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class PerftTest {
    private static final String startingPosition = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";
    public static final boolean enabled = false;
    public static final boolean debugging = false;
    public static final boolean bulkCounting = true;
    public static final int depth = 6;

    private static Stack<Integer> moveHistory = new Stack<>();

    private static String BoardStringOutput(Board Board) {
        StringBuilder s = new StringBuilder();


        for (int i = 0; i < 64; i++) {
            s.append("|");
            switch (Board.Square[i]) {
                case Piece.WHITE_BINARY | Piece.KING -> s.append("♔");
                case Piece.WHITE_BINARY | Piece.QUEEN -> s.append("♕");
                case Piece.WHITE_BINARY | Piece.ROOK -> s.append("♖");
                case Piece.WHITE_BINARY | Piece.BISHOP -> s.append("♗");
                case Piece.WHITE_BINARY | Piece.KNIGHT -> s.append("♘");
                case Piece.WHITE_BINARY | Piece.PAWN -> s.append("♙");

                case Piece.BLACK_BINARY | Piece.KING -> s.append("♚");
                case Piece.BLACK_BINARY | Piece.QUEEN -> s.append("♛");
                case Piece.BLACK_BINARY | Piece.ROOK -> s.append("♜");
                case Piece.BLACK_BINARY | Piece.BISHOP -> s.append("♝");
                case Piece.BLACK_BINARY | Piece.KNIGHT -> s.append("♞");
                case Piece.BLACK_BINARY | Piece.PAWN -> s.append("♟");

                default -> s.append("  ");
            }

            if ((i + 1) % 8 == 0) {
                s.append("|\n");
            }
        }

        return s.toString();
    }


    private static long moveGenerationTest(int d) {
        if (d == 0) {
            return 1;
        }

        long positionCount = 0;
        long c;

        ArrayList<Integer> moves = Main.Board.MoveGenerator.GenerateMoves();

        for (Integer move : moves) {

            //try {
            //moveHistory.push(move | (Main.Board.Square[Move.GetCurrentSquare(move)] << 16));

            Main.Board.MakeMove(move);
            c = moveGenerationTest(d - 1);
            Main.Board.UnmakeMove(move);

            if (debugging && d == depth) {
                System.out.println(Move.GetName(move) + ": " + c);
            }

            positionCount += c;

            //Main.Board.MoveGenerator.CurrentMoves = moves.c;

            //moveHistory.pop();
            /*} catch (Exception e) {
                StringBuilder s = new StringBuilder("Error occured at depth " + depth + ".\nBoard:\n" + BoardStringOutput(Main.Board) + "\n" + (Main.Board.ColorToMove == Board.WHITE ? "White" : "Black") + " to move\nMoves:\n");

                for (int previousMove : moveHistory) {

                    System.out.println(previousMove >>> 16);
                    s.append(Piece.Name(previousMove >>> 16) + " " + Move.GetName(previousMove)).append("\n");
                }

                System.out.println(s);
                System.exit(0);
            }*/
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
                String o = "Depth: " + (d + 1) + " ply | Result: " + c + " positions | Time: " + (double) (t) / 1000000 + " miliseconds";
                System.out.println(o);
            }
        }
    }
}

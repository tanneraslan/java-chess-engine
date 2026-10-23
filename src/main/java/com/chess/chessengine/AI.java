package com.chess.chessengine;

import com.chess.chessengine.util.Bitboard;
import com.chess.chessengine.util.Move;
import com.chess.chessengine.util.Piece;
import com.chess.chessengine.util.StaticMoveData;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.*;

// TODO: add king safety logic. Bonus for pawns in front of king + castling
public class AI { //extends Thread {
    public boolean Enabled = true;
    public int AIColor = Board.BLACK;
    public int PlayerColor = Board.WHITE;
    public static final int[] PIECE_VALUES = {
            0, // None
            10000, // King
            100, // Pawn
            300, // Knight
            310, // Bishop
            500, // Rook
            900  // Queen
    };
    public volatile boolean InSearch = false;
    private static final int SEARCH_DEPTH = 4;
    private Board board;
    private static final int LOWEST_EVAL = Integer.MIN_VALUE + 1;
    private static final int HIGHEST_EVAL = Integer.MAX_VALUE - 1;
    private static final double ENDGAME_MATERIAL_MULTIPLIER = 1.0 / (PIECE_VALUES[Piece.ROOK] * 2 + PIECE_VALUES[Piece.BISHOP] + PIECE_VALUES[Piece.KNIGHT]);
    public int Evaluation = 0;
    private long numOfMovesEvaluated = 0;
    private int intLerp(int a, int b, double t) {
        return a + (int) ((b - a) * t);
    }
    public int estimateMoveValue(int move) {
        int currentSquare = Move.GetCurrentSquare(move);
        int currentPiece = board.Square[currentSquare];
        int pieceType = Piece.PieceType(currentPiece);
        int targetSquare = Move.GetTargetSquare(move);
        int targetPiece = board.Square[targetSquare];
        int flag = Move.GetFlag(move);
        int value = 0;

        if (targetPiece != 0) {
            int earnedMaterial = PIECE_VALUES[Piece.PieceType(targetPiece)] - PIECE_VALUES[Piece.PieceType(currentPiece)];

            if (Bitboard.GetBit(board.OpponentAttackMap, targetSquare) == 0L) {
                value += earnedMaterial * 10;
            } else {
                value += earnedMaterial * 5;
            }
        }

        if (pieceType == Piece.PAWN) {
            if (flag >= 4) {
                if (Bitboard.GetBit(board.OpponentAttackMap, targetSquare) == 0L) {
                    value += PIECE_VALUES[flag - 1] * 4;
                } else {
                    value += PIECE_VALUES[flag - 1];
                }
            }
        } else if (pieceType == Piece.KING) {
        } else {
            int[] weightMap = StaticMoveData.WEIGHT_MAPS[pieceType - 1];
            value += (readWeightMap(weightMap, targetSquare, board.ColorToMove) - readWeightMap(weightMap, currentSquare, board.ColorToMove));

            if (Bitboard.GetBit(board.MoveGenerator.pawnAttackMap, targetSquare) != 0) {
                value -= 350;
            } else if (Bitboard.GetBit(board.OpponentAttackMap, targetSquare) != 0) {
                value -= 150;
            }
        }

        return value;
    }

    private static void Quicksort(int[] values, int[] scores, int low, int high)
    {
        if (low < high)
        {
            int pivotIndex = Partition(values, scores, low, high);
            Quicksort(values, scores, low, pivotIndex - 1);
            Quicksort(values, scores, pivotIndex + 1, high);
        }
    }

    static int Partition(int[] values, int[] scores, int low, int high)
    {
        int pivotScore = scores[high];
        int i = low - 1;
        int temp;

        for (int j = low; j <= high - 1; j++)
        {
            if (scores[j] > pivotScore)
            {
                i++;

                temp = values[i];
                values[i] = values[j];
                values[j] = temp;

                temp = scores[i];
                scores[i] = scores[j];
                scores[j] = temp;
            }
        }

        temp = values[i + 1];
        values[i + 1] = values[high];
        values[high] = temp;

        temp = scores[i + 1];
        scores[i + 1] = scores[high];
        scores[high] = temp;

        return i + 1;
    }

    private Pair<int[], int[]> orderMoves(ArrayList<Integer> listMoves, int s) {
        int[] moves = new int[s];
        int[] moveValues = new int[s];

        int move;
        for (int i = 0; i < s; i++) {
            move = listMoves.get(i);
            moveValues[i] = estimateMoveValue(move);
            moves[i] = move;
        }

        Quicksort(moves, moveValues, 0, s - 1);

        return new Pair<>(moves, moveValues);
    }
    private int readWeightMap(int[] table, int square, int color) {
        // Tables are laid out from white's point of view with a8 first, matching the board's square indices
        if (color == Board.BLACK) {
            square ^= 56;
        }

        return table[square];
    }
    private int calculatePieceWeightBonus(int pieceIndex, int color, double endgameWeight) {
        int bonus = 0;

        int[] table = StaticMoveData.WEIGHT_MAPS[pieceIndex - 1];

        if (pieceIndex == Piece.KING) {
            int kingSquare = board.KingSquare[color];

            bonus = readWeightMap(table, kingSquare, color);

            if (endgameWeight > 0.0) {
                int[] endgameTable = StaticMoveData.WEIGHT_MAPS[6];
                bonus = intLerp(bonus, readWeightMap(endgameTable, kingSquare, color), endgameWeight);
            }
        } else if (pieceIndex == Piece.PAWN) {
            PieceList pawnList = board.Pieces[color][Piece.PAWN];
            int[] endgameTable = StaticMoveData.WEIGHT_MAPS[7];
            boolean isEndgame = (endgameWeight > 0.0);

            int pawnSquare;
            int pawnBonus;

            for (int i = 0; i < pawnList.Count; i++) {
                pawnSquare = pawnList.get(i);
                pawnBonus = readWeightMap(table, pawnSquare, color);

                if (isEndgame) {
                    pawnBonus = intLerp(pawnBonus, readWeightMap(endgameTable, pawnSquare, color), endgameWeight);
                }

                bonus += pawnBonus;
            }
        } else {
            PieceList pieceList = board.Pieces[color][pieceIndex];

            for (int i = 0; i < pieceList.Count; i++) {
                bonus += readWeightMap(table, pieceList.get(i), color);
            }
        }

        return bonus;
    }
    private double calculateEndgameWeight(int color, int totalMaterial) {
        totalMaterial -= board.Pieces[color][Piece.PAWN].Count * PIECE_VALUES[Piece.PAWN];
        return 1.0 - Math.min(1.0, totalMaterial * ENDGAME_MATERIAL_MULTIPLIER);
    }
    private int calculateMopUp(int friendlyColor, int opponentColor, int friendlyMaterial, int opponentMaterial, double enemyEndgameWeight) {
        if (friendlyMaterial > (opponentMaterial + PIECE_VALUES[Piece.PAWN] * 2) && enemyEndgameWeight > 0.0) {
            int friendlyKingSquare = board.KingSquare[friendlyColor];
            int opponentKingSquare = board.KingSquare[opponentColor];

            // PosEval = 4.7 * CMD + 1.6 * (14 - MD)
            return (int) ((4.7 * StaticMoveData.CENTRAL_MANHATTAN_DISTANCE[opponentKingSquare] + 1.5 * (14 - StaticMoveData.MANHATTAN_DISTANCE[friendlyKingSquare][opponentKingSquare])) * enemyEndgameWeight * 25);
        }
        return 0;
    }
    private int evaluatePosition() {
        int friendlyColor = board.ColorToMove;
        int opponentColor = (friendlyColor + 1) % 2;

        int eval = 0;

        PieceList[] friendlyPieces = board.Pieces[friendlyColor];
        PieceList[] opponentPieces = board.Pieces[opponentColor];

        int friendlyMaterial = 0;
        int opponentMaterial = 0;

        for (int i = 2; i < 7; i++) {
            friendlyMaterial += friendlyPieces[i].Count * PIECE_VALUES[i];
            opponentMaterial += opponentPieces[i].Count * PIECE_VALUES[i];
        }

        eval += (friendlyMaterial - opponentMaterial);

        double friendlyEndgameWeight = calculateEndgameWeight(friendlyColor, friendlyMaterial);
        double opponentEndgameWeight = calculateEndgameWeight(opponentColor, opponentMaterial);


        // Weight maps
        for (int i = 1; i < 7; i++) {
            eval = eval
                + calculatePieceWeightBonus(i, friendlyColor, opponentEndgameWeight)
                - calculatePieceWeightBonus(i, opponentColor, friendlyEndgameWeight);
        }

        // Mop up value
        int mopUp = calculateMopUp(friendlyColor, opponentColor, friendlyMaterial, opponentMaterial, opponentEndgameWeight);
        mopUp -= calculateMopUp(opponentColor, friendlyColor, opponentMaterial, friendlyMaterial, friendlyEndgameWeight);
        eval += mopUp;

        return eval;
    }

    private int quiescence(int alpha, int beta, int minValue) {
        // Moves must be generated before evaluating: checkmate/stalemate detection and the move ordering
        // heuristics both rely on the move generator's state matching the current position
        board.MoveGenerator.GenerateMoves();

        int numOfMoves = board.MoveGenerator.CurrentMoves.size();
        if (numOfMoves == 0) {
            if (board.MoveGenerator.inCheck) {
                return LOWEST_EVAL;
            }
            return 0;
        }

        int standPat = evaluatePosition();
        if (standPat >= beta) {
            return beta;
        }
        if (alpha < standPat) {
            alpha = standPat;
        }

        Pair<int[], int[]> movesAndValues = orderMoves(board.MoveGenerator.CurrentMoves, numOfMoves);
        int[] moves = movesAndValues.getKey();
        int[] values = movesAndValues.getValue();

        int move;
        int score;

        for (int i = 0; i < numOfMoves; i++) {
            if (values[i] < minValue) {
                break;
            }

            move = moves[i];

            board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -quiescence(-beta, -alpha, minValue);
            board.UnmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;

    }
    private int alphaBeta(int alpha, int beta, int depth, int plyFromRoot) {
        if (plyFromRoot > 0) {
            alpha = Math.max(alpha, LOWEST_EVAL + plyFromRoot);
            beta = Math.min(beta, HIGHEST_EVAL - plyFromRoot);

            if (alpha >= beta) {
                return alpha;
            }
        }

        if (depth == 0) {
            return quiescence(alpha, beta, 900);
        }

        board.MoveGenerator.GenerateMoves();

        int numOfMoves = board.MoveGenerator.CurrentMoves.size();
        if (numOfMoves == 0) {
            if (board.MoveGenerator.inCheck) {
                return LOWEST_EVAL + plyFromRoot;
            }
            return 0;
        }

        int move;
        int score;

        Pair<int[], int[]> movesAndValues = orderMoves(board.MoveGenerator.CurrentMoves, numOfMoves);
        int[] moves = movesAndValues.getKey();
        for (int i = 0; i < numOfMoves; i++) {
            move = moves[i];

            board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -alphaBeta(-beta, -alpha,depth - 1, plyFromRoot + 1);
            board.UnmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    public int getBestMove(Board searchBoard) {
        board = searchBoard;
        int bestMove = -1;
        int alpha = LOWEST_EVAL;
        int score;

        ArrayList<Integer> listMoves = board.MoveGenerator.GenerateMoves();

        int s = listMoves.size();
        if (s == 0) {
            return -1;
        }

        Pair<int[], int[]> movesAndValues = orderMoves(listMoves, s);
        int[] moves = movesAndValues.getKey();
        int move;

        for (int i = 0; i < s; i++) {
            move = moves[i];
            board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -alphaBeta(-HIGHEST_EVAL, -alpha, SEARCH_DEPTH, 1);
            board.UnmakeMove(move);

            if (bestMove == -1 || score > alpha) {
                alpha = score;
                bestMove = move;
            }
        }

        Evaluation = alpha;
        return bestMove;
    }

    /**
     * Starts searching for the AI's move on a background thread, so the UI stays responsive.
     * The search runs on a private copy of the board; the chosen move is applied to Main.Board on the JavaFX thread.
     */
    public void run() {
        if (InSearch || Main.Board.ColorToMove != AIColor || Main.currentMoves.isEmpty()) {
            return;
        }

        InSearch = true;
        Board searchBoard = new Board();
        searchBoard.LoadPosition(Main.Board.GenerateLoadedPosition());

        Thread searchThread = new Thread(() -> search(searchBoard), "AI search");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void search(Board searchBoard) {
        int move = -1;

        try {
            long t = System.nanoTime();

            numOfMovesEvaluated = 0;

            move = getBestMove(searchBoard);

            t = Math.max(1, System.nanoTime() - t);
            if (move != -1) {
                System.out.println(
                        "SEARCH DONE" +
                        "\nBest move: " + Piece.Name(board.Square[Move.GetCurrentSquare(move)]) + " " + Move.GetName(move) +
                        "\nEvaluation: " + Evaluation +
                        "\nTime Taken: " + (double) (t) / 1000000 + " milliseconds" +
                        "\nEvaluted: " + numOfMovesEvaluated + " positions" +
                        "\nSpeed: " + (numOfMovesEvaluated * 1000000) / t + " positions per millisecond" +
                        "\n~~~~~~~~~~~~~~~~~~~~~"
                );
            }
        } finally {
            int bestMove = move;

            Platform.runLater(() -> {
                if (bestMove != -1) {
                    Main.Board.MakeMove(bestMove);
                    Main.moveHistory.push(bestMove);
                    Main.mostRecentMove = bestMove;
                    Main.currentMoves = Main.Board.MoveGenerator.GenerateMoves();
                    Main.reportGameOver();
                }

                InSearch = false;
            });
        }
    }
}

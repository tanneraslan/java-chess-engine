package com.chess.chessengine;

import com.chess.chessengine.util.Bitboard;
import com.chess.chessengine.util.Move;
import com.chess.chessengine.util.Piece;
import com.chess.chessengine.util.StaticMoveData;
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
    public boolean InSearch = false;
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
        int currentPiece = Main.Board.Square[currentSquare];
        int pieceType = Piece.PieceType(currentPiece);
        int targetSquare = Move.GetTargetSquare(move);
        int targetPiece = Main.Board.Square[targetSquare];
        int flag = Move.GetFlag(move);
        int value = 0;

        if (targetPiece != 0) {
            int earnedMaterial = PIECE_VALUES[Piece.PieceType(targetPiece)] - PIECE_VALUES[Piece.PieceType(currentPiece)];

            if (Bitboard.GetBit(Main.Board.OpponentAttackMap, targetSquare) == 0L) {
                value += earnedMaterial * 10;
            } else {
                value += earnedMaterial * 5;
            }
        }

        if (pieceType == Piece.PAWN) {
            if (flag >= 4) {
                if (Bitboard.GetBit(Main.Board.OpponentAttackMap, targetSquare) == 0L) {
                    value += PIECE_VALUES[flag - 1] * 4;
                } else {
                    value += PIECE_VALUES[flag - 1];
                }
            }
        } else if (pieceType == Piece.KING) {
        } else {
            int[] weightMap = StaticMoveData.WEIGHT_MAPS[pieceType];
            value += (readWeightMap(weightMap, currentSquare, Main.Board.ColorToMove) - readWeightMap(weightMap, targetSquare, Main.Board.ColorToMove));

            if (Bitboard.GetBit(Main.Board.MoveGenerator.pawnAttackMap, targetSquare) != 0) {
                value -= 350;
            } else if (Bitboard.GetBit(Main.Board.OpponentAttackMap, targetSquare) != 0) {
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
        if (color == Board.WHITE) {
            square = square - (square / 8) * 16 + 56;
        }

        return table[square];
    }
    private int calculatePieceWeightBonus(int pieceIndex, int color, double endgameWeight) {
        int bonus = 0;

        int[] table = StaticMoveData.WEIGHT_MAPS[pieceIndex - 1];

        if (pieceIndex == Piece.KING) {
            int kingSquare = Main.Board.KingSquare[color];

            bonus = readWeightMap(table, kingSquare, color);

            if (endgameWeight > 0.0) {
                int[] endgameTable = StaticMoveData.WEIGHT_MAPS[6];
                bonus = intLerp(bonus, readWeightMap(endgameTable, kingSquare, color), endgameWeight);
            }
        } else if (pieceIndex == Piece.PAWN) {
            PieceList pawnList = Main.Board.Pieces[color][Piece.PAWN];
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
            PieceList pieceList = Main.Board.Pieces[color][Piece.PAWN];

            for (int i = 0; i < pieceList.Count; i++) {
                bonus += readWeightMap(table, pieceList.get(i), color);
            }
        }

        return bonus;
    }
    private double calculateEndgameWeight(int color, int totalMaterial) {
        totalMaterial -= Main.Board.Pieces[color][Piece.PAWN].Count * PIECE_VALUES[Piece.PAWN];
        return 1.0 - Math.min(1.0, totalMaterial * ENDGAME_MATERIAL_MULTIPLIER);
    }
    private int calculateMopUp(int friendlyColor, int opponentColor, int friendlyMaterial, int opponentMaterial, double enemyEndgameWeight) {
        if (friendlyMaterial > (opponentMaterial + PIECE_VALUES[Piece.PAWN] * 2) && enemyEndgameWeight > 0.0) {
            int friendlyKingSquare = Main.Board.KingSquare[friendlyColor];
            int opponentKingSquare = Main.Board.KingSquare[opponentColor];

            // PosEval = 4.7 * CMD + 1.6 * (14 - MD)
            return (int) ((4.7 * StaticMoveData.CENTRAL_MANHATTAN_DISTANCE[opponentKingSquare] + 1.5 * (14 - StaticMoveData.MANHATTAN_DISTANCE[friendlyKingSquare][opponentKingSquare])) * enemyEndgameWeight * 25);
        }
        return 0;
    }
    private int evaluatePosition() {
        int friendlyColor = Main.Board.ColorToMove;
        int opponentColor = (friendlyColor + 1) % 2;

        ArrayList<Integer> moves = Main.Board.MoveGenerator.CurrentMoves;

        if (moves.size() == 0) {
            if (Main.Board.MoveGenerator.inCheck) {
                return LOWEST_EVAL;
            }
            return 0;
        }

        int eval = 0;

        PieceList[] friendlyPieces = Main.Board.Pieces[friendlyColor];
        PieceList[] opponentPieces = Main.Board.Pieces[opponentColor];

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
        int standPat = evaluatePosition();
        if (standPat >= beta) {
            return beta;
        }
        if (alpha < standPat) {
            alpha = standPat;
        }

        Main.Board.MoveGenerator.GenerateMoves();

        int numOfMoves = Main.Board.MoveGenerator.CurrentMoves.size();
        if (numOfMoves == 0) {
            if (Main.Board.MoveGenerator.inCheck) {
                return LOWEST_EVAL;
            }
            return 0;
        }

        Pair<int[], int[]> movesAndValues = orderMoves(Main.Board.MoveGenerator.CurrentMoves, numOfMoves);
        int[] moves = movesAndValues.getKey();
        int[] values = movesAndValues.getValue();

        int move;
        int score;

        for (int i = 0; i < numOfMoves; i++) {
            if (values[i] < minValue) {
                break;
            }

            move = moves[i];

            Main.Board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -quiescence(-beta, -alpha, minValue);
            Main.Board.UnmakeMove(move);

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

        Main.Board.MoveGenerator.GenerateMoves();

        int numOfMoves = Main.Board.MoveGenerator.CurrentMoves.size();
        if (numOfMoves == 0) {
            if (Main.Board.MoveGenerator.inCheck) {
                return LOWEST_EVAL + plyFromRoot;
            }
            return 0;
        }

        int move;
        int score;

        Pair<int[], int[]> movesAndValues = orderMoves(Main.Board.MoveGenerator.CurrentMoves, numOfMoves);
        int[] moves = movesAndValues.getKey();
        //4
        for (int i = 0; i < numOfMoves; i++) {
            move = moves[i];

            Main.Board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -alphaBeta(-beta, -alpha,depth - 1, plyFromRoot + 1);
            Main.Board.UnmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    public int getBestMove() {
        int highestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        int score;

        ArrayList<Integer> listMoves = Main.Board.MoveGenerator.CurrentMoves;

        int s = listMoves.size();
        Pair<int[], int[]> movesAndValues = orderMoves(listMoves, s);
        int[] moves = movesAndValues.getKey();
        int move;

        for (int i = 0; i < s; i++) {
            move = moves[i];
            Main.Board.MakeMove(move);
            numOfMovesEvaluated++;
            score = -alphaBeta(LOWEST_EVAL, HIGHEST_EVAL, 4, 0);
            Main.Board.UnmakeMove(move);

            if (score > highestScore) {
                highestScore = score;
                bestMove = move;
            }
        }

        Evaluation = highestScore;
        return bestMove;
    }

    public void run() {
        if (Main.Board.ColorToMove == AIColor) {
            /*try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            InSearch = true;

            long t = System.nanoTime();

            numOfMovesEvaluated = 0;

            int move = getBestMove();

            t = System.nanoTime() - t;
            System.out.println(
                    "SEARCH DONE" +
                    "\nBest move: " + Piece.Name(Main.Board.Square[Move.GetCurrentSquare(move)]) + " " + Move.GetName(move) +
                    "\nEvaluation: " + Evaluation +
                    "\nTime Taken: " + (double) (t) / 1000000 + " milliseconds" +
                    "\nEvaluted: " + numOfMovesEvaluated + " positions" +
                    "\nSpeed: " + (numOfMovesEvaluated * 1000000) / t + " positions per millisecond" +
                    "\n~~~~~~~~~~~~~~~~~~~~~"
            );
            Main.Board.MakeMove(move);
            Main.moveHistory.push(move);
            Main.mostRecentMove = move;
            Main.currentMoves = Main.Board.MoveGenerator.GenerateMoves();

            InSearch = false;
        }
    }
}

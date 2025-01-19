package com.chess.chessengine.util;

import com.chess.chessengine.Main;

public final class Move {
    public static final int FLAG_NONE = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_DOUBLE_PAWN = 2;
    public static final int FLAG_CASTLING = 3;
    public static final int FLAG_P_KNIGHT= 4;
    public static final int FLAG_P_BISHOP = 5;
    public static final int FLAG_P_ROOK = 6;
    public static final int FLAG_P_QUEEN = 7;

    private static final int CURRENT_MASK = 0b0000000000111111;
    private static final int TARGET_MASK = 0b0000111111000000;
    private static final int FLAG_MASK = 0b1111000000000000;

    public static int Construct(int currentSquare, int targetSquare, int flag){
        return currentSquare | targetSquare << 6 | flag << 12;
    }

    public static int GetCurrentSquare(int n) {
        return n & CURRENT_MASK;
    }

    public static int GetTargetSquare(int n) {
        return (n & TARGET_MASK) >>> 6;
    }

    public static int GetFlag(int n) {
        return (n & FLAG_MASK) >>> 12;
    }

    public static String GetName(int move) {
        return Fen.SquareNameFromIndex(Move.GetCurrentSquare(move)) + " -> " + Fen.SquareNameFromIndex(Move.GetTargetSquare(move));
    }


}


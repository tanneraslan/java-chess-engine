package com.chess.chessengine.util;

import com.chess.chessengine.Board;

public final class Piece {
    public static final int NONE = 0;
    public static final int KING = 1;
    public static final int PAWN = 2;
    public static final int KNIGHT = 3;
    public static final int BISHOP = 4;
    public static final int ROOK = 5;
    public static final int QUEEN = 6;
    public static final int BLACK_BINARY = 0;
    public static final int WHITE_BINARY = 8;

    private static final int TYPE_MASK = 0b0111;
    private static final int COLOR_MASK = 0b1000;

    private static final String[] NAMES = {"None", "King", "Pawn", "Knight", "Bishop", "Rook", "Queen"};

    public static boolean IsColor(int p, int c) {
        return (p >>> 3) == c;
    }

    public static boolean IsType(int p, int t) {
        return (p & TYPE_MASK) == t;
    }

    public static int Color(int p) {
        return  (p >>> 3);
    }

    public static int PieceType(int p) {
        return  (p & TYPE_MASK);
    }
    public static int Construct(int c, int p) {
        return (p | (c << 3));
    }

    //public static int BinaryColorToIndex(int n) {
    //    return  (n / 8 - 1);
    //}

    //public static int IndexColorToBinary(int n) {
    //    return  ((n + 1) * 8);
    //}

    public static String Name(int p) {
        int t = Piece.PieceType(p);

        if (t == Piece.NONE) {
            return "None";
        } else {
            return (Piece.Color(p) == Board.WHITE ? "White " : "Black ").concat(NAMES[t]);
        }
    }
}

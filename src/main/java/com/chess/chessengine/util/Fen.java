package com.chess.chessengine.util;

import com.chess.chessengine.Board;

import java.util.Objects;
public final class Fen {
    //
    public static String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
    public static String SquareNameFromIndex(int index) {
        return (char) ((int) ('a') + index % 8) + String.valueOf(8 - index / 8);
    }
    public static int IndexFromSquareName(String name) {
        return ((name.charAt(0) - 'a') + (8 - (name.charAt(1) - '0')) * 8);
    }

    public record LoadedPosition(
            boolean whiteToMove,
            int[] Squares,
            boolean whiteCastleKingside,
            boolean whiteCastleQueenside,
            boolean blackCastleKingside,
            boolean blackCastleQueenside,
            int enPassantFile,
            int plyCount,
            int fiftyMoveCounter
    ) {
    }
    public static LoadedPosition LoadFen(String fen) {
        String[] sections = fen.split(" ");

        int index  = 0;
        int[] squares = new int[64];
        String[] pieces = sections[0].split("(?!^)");

        // put all the pieces in the right squares
        for (String piece : pieces) {
            if (isInt(piece)) {
                index += Integer.parseInt(piece);
            } else if (Objects.equals(piece, "/")) {
                continue;
            } else {
                int color = (Objects.equals(piece, piece.toLowerCase())) ? Board.BLACK : Board.WHITE;
                int finalPiece = switch (piece.toLowerCase()) {
                    case "r" -> Piece.ROOK;
                    case "n" -> Piece.KNIGHT;
                    case "b" -> Piece.BISHOP;
                    case "q" -> Piece.QUEEN;
                    case "k" -> Piece.KING;
                    case "p" -> Piece.PAWN;
                    default -> 0;
                };

                if (finalPiece != 0) {
                    squares[index] = Piece.Construct(color, finalPiece);
                }

                index += 1;
            }
        }

        boolean whiteToMove = Objects.equals(sections[1], "w");

        // castling rights
        String castlingString = new String(sections.length > 2 ? sections[2] : "KQkq");
        boolean whiteCastleKingside = false;
        boolean whiteCastleQueenside = false;
        boolean blackCastleKingside = false;
        boolean blackCastleQueenside = false;

        if (castlingString.contains("K")) {
            whiteCastleKingside = true;
        }
        if (castlingString.contains("Q")) {
            whiteCastleQueenside = true;
        }
        if (castlingString.contains("k")) {
            blackCastleKingside = true;
        }
        if (castlingString.contains("q")) {
            blackCastleQueenside = true;
        }

        int epFile = 0;

        if (sections.length > 3) {
            if (Objects.equals(sections[3], "-")) {
                epFile = 0;
            } else {
                int squareIndex = IndexFromSquareName(sections[3]);

                if (squareIndex >= 0 && squareIndex < 64) {
                    epFile = (squareIndex % 8 + 1);
                }
            }
        }

        int fiftyMoveCounter = 0;
        int plyCount = 0;

        if (sections.length > 4 && isInt(sections[4]) && isInt(sections[5])) {
            fiftyMoveCounter = Integer.parseInt(sections[4]);
            plyCount = Integer.parseInt(sections[5]);
        }

        return new LoadedPosition(whiteToMove, squares, whiteCastleKingside, whiteCastleQueenside, blackCastleKingside, blackCastleQueenside, epFile, plyCount, fiftyMoveCounter);
    }
}

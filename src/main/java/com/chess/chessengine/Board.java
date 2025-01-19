package com.chess.chessengine;

import com.chess.chessengine.util.*;
import com.chess.chessengine.PieceList;
import javafx.geometry.HorizontalDirection;
import javafx.scene.paint.Color;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.LongBinaryOperator;

public class Board {

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    private static final int CASTLING_MASK     = 0b00000000000000001111;
    private static final int EPFILE_MASK       = 0b00000000000011110000;
    private static final int FIFTY_MOVE_MASK   = 0b00000111111100000000;
    private static final int LAST_CAPTURE_MASK = 0b11111000000000000000;

    public int[] Square;
    public int[] KingSquare;
    public PieceList[][] Pieces;

    public long OpponentAttackMap;
    public long FriendlyPieceMap;
    public long OpponentPieceMap;
    public int ColorToMove;
    public int PlyCount;
    public int GameState;
    public Stack<Integer> GameStateHistory;
    public MoveGenerator MoveGenerator;

    private int nextCastlingRights;
    private int nextEpFile;
    private int nextFiftyMoveCounter;

    public void LoadPosition(Fen.LoadedPosition position) {
        Square = new int[64];
        KingSquare = new int[2];
        OpponentAttackMap = 0L;
        FriendlyPieceMap = 0L;
        OpponentPieceMap = 0L;
        GameState = 0;
        PlyCount = 0;
        GameStateHistory = new Stack<>();
        MoveGenerator = new MoveGenerator();

        ColorToMove = position.whiteToMove() ? WHITE : BLACK;
        PlyCount = position.plyCount();

        PieceList empty = new PieceList(0);

        Pieces = new PieceList[][] {
                new PieceList[] {
                        empty,                      // None
                        empty,                      // Kings
                        new PieceList(8),  // Pawns
                        new PieceList(10), // Knights
                        new PieceList(10), // Bishops
                        new PieceList(10), // Rooks
                        new PieceList(10)  // Queens
                },
                new PieceList[] {
                        empty,
                        empty,
                        new PieceList(8),
                        new PieceList(10),
                        new PieceList(10),
                        new PieceList(10),
                        new PieceList(10)
                }
        };

        for (int i = 0; i < 64; i++) {
            int piece = position.Squares()[i];
            int c;
            int t;

            if (piece != 0) {
                c = Piece.Color(piece);
                t = Piece.PieceType(piece);

                if (t == Piece.KING) {
                    KingSquare[c] = i;
                } else {
                    Pieces[c][t].AddPiece(i);
                }

                if (c == ColorToMove) {
                    FriendlyPieceMap = Bitboard.SetBit(FriendlyPieceMap, i);
                } else {
                    OpponentPieceMap = Bitboard.SetBit(OpponentPieceMap, i);
                }

                Square[i] = piece;
            }
        }

        byte castling = 0b0000;
        if (position.whiteCastleKingside()) {
            castling |= 0b0001;
        }
        if (position.whiteCastleQueenside()) {
            castling |= 0b0010;
        }
        if (position.blackCastleKingside()) {
            castling |= 0b0100;
        }
        if (position.blackCastleQueenside()) {
            castling |= 0b1000;
        }

        GameState = castling | position.enPassantFile() << 4 | position.fiftyMoveCounter() << 8;
    }

    private void RemovePiece(int square, int piece) {
        //System.out.println("REMOVING: " + square + " | " + Piece.Name(piece));
        Square[square] = 0;

        if (!Piece.IsType(piece, Piece.KING)) {
            Pieces[Piece.Color(piece)][Piece.PieceType(piece)].RemovePiece(square);
        }

        // Update bitboards
        if (Piece.IsColor(piece, ColorToMove)) {
            FriendlyPieceMap = Bitboard.ClearBit(FriendlyPieceMap, square);
        } else {
            OpponentPieceMap = Bitboard.ClearBit(OpponentPieceMap, square);
        }

        // Update castling rights if a rook is removed
        if (Piece.IsType(piece, Piece.ROOK)) {
            int pieceColor = Piece.Color(piece);
            int castlingRights = (nextCastlingRights & (3 + pieceColor * 9)) >>> pieceColor * 2;

            if (castlingRights != 0) {
                int file = square % 8;

                if (file == 7 && (castlingRights & 1) != 0) {
                    nextCastlingRights &= ~(1 << (pieceColor * 2));
                } else if (file == 0 && (castlingRights & 2) != 0) {
                    nextCastlingRights &= ~(1 << (pieceColor * 2 + 1));
                }
            }
        } else if (Piece.IsType(piece, Piece.KING)) {
            // Remove all castling rights if the king has been moved
            nextCastlingRights &= (3 + (1 - Piece.Color(piece)) * 9);
        }
    }
    private void AddPiece(int square, int piece) {
        //System.out.println("ADDING: " + square + " | " + Piece.Name(piece));
        Square[square] = piece;

        if (Piece.IsType(piece, Piece.KING)) {
            KingSquare[Piece.Color(piece)] = square;
        } else {
            Pieces[Piece.Color(piece)][Piece.PieceType(piece)].AddPiece(square);
        }

        if (Piece.IsColor(piece, ColorToMove)) {
            FriendlyPieceMap = Bitboard.SetBit(FriendlyPieceMap, square);
        } else {
            OpponentPieceMap = Bitboard.SetBit(OpponentPieceMap, square);
        }
    }
    private void ReplacePiece(int square, int piece, int replacementPiece) {
        RemovePiece(square, piece);
        AddPiece(square, replacementPiece);
    }
    private void MovePiece(int square, int targetSquare, int piece) {
        RemovePiece(square, piece);
        AddPiece(targetSquare, piece);

        if (Piece.IsType(piece, Piece.PAWN)) {
            // Reset fifty move counter since a pawn has been pushed
            nextFiftyMoveCounter = 0;
        }
    }
    private void CapturePiece(int square, int targetSquare, int piece, int targetPiece) {
        RemovePiece(square, piece);
        RemovePiece(targetSquare, targetPiece);
        AddPiece(targetSquare, piece);
        // Reset fifty move counter, piece has been captured
        nextFiftyMoveCounter = 0;
    }

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


    public void MakeMove(int move) {
        int currentSquare = Move.GetCurrentSquare(move);
        int targetSquare = Move.GetTargetSquare(move);
        int flag = Move.GetFlag(move);
        int currentPiece = Square[currentSquare];
        int targetPiece = Square[targetSquare];
        int opponentColor = (ColorToMove + 1) % 2;

        nextCastlingRights = GameState & CASTLING_MASK;
        nextEpFile = 0;
        nextFiftyMoveCounter = GameState & FIFTY_MOVE_MASK >>> 8 + 1;

        //System.out.println("~~~~~~~~~~~~~~MAKE MOVE~~~~~~~~~~~~~~~");

        if (flag == Move.FLAG_DOUBLE_PAWN) {
            //System.out.println("DOUBLE PAWN");
            nextEpFile = currentSquare % 8 + 1;
            // Since a double pawn move can't capture anything, we can safety just move piece here
            MovePiece(currentSquare, targetSquare, currentPiece);
        } else if (flag == Move.FLAG_EN_PASSANT) {
            //System.out.println("EN PASSANT");
            int targetPawnSquare = targetSquare - 8 * (2 * ColorToMove - 1);
            // Handle en passant movement within here
            RemovePiece(targetPawnSquare, Piece.Construct(opponentColor, Piece.PAWN));
            MovePiece(currentSquare, targetSquare, currentPiece);
        } else if (flag == Move.FLAG_CASTLING) {
            //System.out.println("CASTLING");
            int rookSquare;
            int newRookSquare;

            if (targetSquare > currentSquare) {
                // King Side
                rookSquare = currentSquare + 3;
                newRookSquare = targetSquare - 1;
            } else {
                // Queen Side
                rookSquare = currentSquare - 4;
                newRookSquare = targetSquare + 1;
            }
            // Since castling can't capture a piece legally, we can safely avoid checking the target piece here
            // Move the rook
            MovePiece(rookSquare, newRookSquare, Piece.Construct(ColorToMove, Piece.ROOK));
            // Move the king
            MovePiece(currentSquare, targetSquare, currentPiece);
        } else if (flag >= 4) {
            //System.out.println("PROMOTION");
            // Check if a piece is on the square we're promoting to, and capture it if it is
            if (!Piece.IsType(targetPiece, Piece.NONE)) {
                CapturePiece(currentSquare, targetSquare, currentPiece, targetPiece);
            } else {
                // Just move the pawn regularly if there's nothing to capture
                MovePiece(currentSquare, targetSquare, currentPiece);
            }
            // Replace our piece with our newly promoted one
            ReplacePiece(targetSquare, currentPiece, Piece.Construct(ColorToMove, Move.GetFlag(move) - 1));
        } else {
            //System.out.println("REGULAR");
            if (Piece.IsType(targetPiece, Piece.NONE)) {
                MovePiece(currentSquare, targetSquare, currentPiece);
            } else {
                CapturePiece(currentSquare, targetSquare, currentPiece, targetPiece);
            }
        }

        GameStateHistory.push(GameState);
        GameState = (nextCastlingRights) | (nextEpFile << 4) | (nextFiftyMoveCounter << 8) | (targetPiece << 15);
        PlyCount += 1;
        ColorToMove = opponentColor;

        long temp = OpponentPieceMap;
        OpponentPieceMap = FriendlyPieceMap;
        FriendlyPieceMap = temp;


        /*for (int color = 0; color < 2; color++) {
            System.out.println(color == 0 ? "WHITE:" : "BLACK:");
            for (int pieceType = 2; pieceType < 7; pieceType++) {
                System.out.println(Piece.Name(Piece.Construct(color * 8, pieceType)) + ": " + Pieces[color][pieceType].Count);
            }
        }*/

    }

    public void UnmakeMove(int move) {
        int previousGameState = GameState;
        GameState = GameStateHistory.peek();
        GameStateHistory.pop();
        PlyCount--;
        ColorToMove = (ColorToMove + 1) % 2;

        long temp = FriendlyPieceMap;
        FriendlyPieceMap = OpponentPieceMap;
        OpponentPieceMap = temp;

        int currentSquare = Move.GetTargetSquare(move);
        int previousSquare = Move.GetCurrentSquare(move);
        int flag = Move.GetFlag(move);
        int currentPiece = Square[currentSquare];
        int opponentColor = (ColorToMove + 1) % 2;
        int lastCapture = (previousGameState & LAST_CAPTURE_MASK) >>> 15;

        //System.out.println("~~~~~~~~~~~~~~UNMAKE MOVE~~~~~~~~~~~~~~~");

        if (flag == Move.FLAG_EN_PASSANT) {
            //System.out.println("EN PASSANT");
            int targetPawnSquare = currentSquare - 8 * (2 * ColorToMove - 1);
            // Move the en passant pawn back to its original square
            MovePiece(currentSquare, previousSquare, currentPiece);
            // Add the captured pawn back
            AddPiece(targetPawnSquare, Piece.Construct(opponentColor, Piece.PAWN));
        } else if (flag == Move.FLAG_CASTLING) {
            //System.out.println("CASTLING");
            int rookSquare;
            int previousRookSquare;

            if (currentSquare > previousSquare) {
                // King side
                previousRookSquare = previousSquare + 3;
                rookSquare = currentSquare - 1;
            } else {
                // Queen side
                previousRookSquare = previousSquare - 4;
                rookSquare = currentSquare + 1;
            }

            // Since castling can't capture a piece legally, we can safely avoid checking the target piece here
            // Move the rook back
            MovePiece(rookSquare, previousRookSquare, Piece.Construct(ColorToMove, Piece.ROOK));
            // Move the king back
            MovePiece(currentSquare, previousSquare, currentPiece);
        } else if (flag >= 4) {
            //System.out.println("PROMOTION");
            // Move the promoted pawn back to its original square
            MovePiece(currentSquare, previousSquare, currentPiece);
            // Replace the promoted piece with a regular pawn
            ReplacePiece(previousSquare, currentPiece, Piece.Construct(ColorToMove, Piece.PAWN));
            // Check if a piece was on the square we promoted to, and add it back if it is
            if (!Piece.IsType(lastCapture, Piece.NONE)) {
                AddPiece(currentSquare, lastCapture);
            }
        } else {
            //System.out.println("NORMAL");
            // Move the piece back to its original square
            MovePiece(currentSquare, previousSquare, currentPiece);

            // If we did capture a piece, add it back to the board
            if (!Piece.IsType(lastCapture, Piece.NONE)) {
                AddPiece(currentSquare, lastCapture);
            }
        }

        StringBuilder s = new StringBuilder("Error occured at depth " + 3 + ".\nBoard:\n" + BoardStringOutput(Main.Board) + "\n" + (Main.Board.ColorToMove == Board.WHITE ? "White" : "Black") + " to move\nMoves:\n");

        for (int previousMove : Main.moveHistory) {


            s.append(Piece.Name(previousMove >>> 16) + " " + Move.GetName(previousMove)).append("\n");
        }
        /*for (int color = 0; color < 2; color++) {
            System.out.println(color == 0   ? "WHITE:" : "BLACK:");
            for (int pieceType = 2; pieceType < 7; pieceType++) {
                System.out.println(Piece.Name(Piece.Construct(color * 8, pieceType)) + ": " + Pieces[color][pieceType].Count);
            }
        }*/

        /*if (flag != 0) {
            if (flag >= 4) {
                Pieces[opponentColor][Piece.PieceType(currentPiece)].RemovePiece(currentSquare);
                currentPiece = Piece.Construct(opponentColor, Piece.PAWN);
            } else if (flag == Move.FLAG_EN_PASSANT) {
                int forwardDir = opponentColor == WHITE ? 1 : -1;
                int targetPawnSquare = currentSquare + 8 * forwardDir;

                Pieces[ColorToMove][Piece.PAWN].AddPiece(targetPawnSquare);
                FriendlyPieceMap = Bitboard.SetBit(FriendlyPieceMap, targetPawnSquare);
                Square[targetPawnSquare] = Piece.Construct(ColorToMove, Piece.PAWN);
            } else if (flag == Move.FLAG_CASTLING) {
                int rookSquare;
                int originalRookSquare;

                if (currentSquare > previousSquare) { // Kingside
                    originalRookSquare = previousSquare + 3;
                    rookSquare = currentSquare - 1;
                } else {
                    originalRookSquare = previousSquare - 4;
                    rookSquare = currentSquare + 1;
                }

                Pieces[opponentColor][Piece.ROOK].MovePiece(rookSquare, originalRookSquare);
                Square[originalRookSquare] = Piece.Construct(opponentColor, Piece.ROOK);
                Square[rookSquare] = 0;

                OpponentPieceMap = Bitboard.SetBit(Bitboard.ClearBit(OpponentPieceMap, rookSquare), originalRookSquare);
            }
        }

        Square[currentSquare] = 0;
        Square[previousSquare] = currentPiece;

        OpponentPieceMap = Bitboard.SetBit(Bitboard.ClearBit(OpponentPieceMap, currentSquare), previousSquare);

        if (Piece.IsType(currentPiece, Piece.KING)) {
            KingSquare[Piece.Color(currentPiece)] = previousSquare;
        } else {
            PieceList pieceList = Pieces[Piece.Color(currentPiece)][Piece.PieceType(currentPiece)];

            if (flag < 4) {
                pieceList.MovePiece(currentSquare, previousSquare);
            } else {
                pieceList.AddPiece(previousSquare);
            }
        }

        if (lastCapture != 0 && flag != Move.FLAG_EN_PASSANT) {
            Pieces[Piece.Color(lastCapture)][Piece.PieceType(lastCapture)].AddPiece(currentSquare);
            Square[currentSquare] = lastCapture;

            FriendlyPieceMap = Bitboard.SetBit(FriendlyPieceMap, currentSquare);
        }
        */
    }

    public Fen.LoadedPosition GenerateLoadedPosition() {

        boolean whiteToMove = ColorToMove == WHITE;
        int[] squares = Square.clone();

        int castlingRights = GameState & CASTLING_MASK;
        boolean whiteCastleKingside     = (castlingRights & 0b0001) != 0;
        boolean whiteCastleQueenside    = (castlingRights & 0b0010) != 0;
        boolean blackCastleKingside     = (castlingRights & 0b0100) != 0;
        boolean blackCastleQueenside    = (castlingRights & 0b1000) != 0;

        int epFile = GameState & EPFILE_MASK >>> 4;
        int fiftyMoveCounter = GameState & FIFTY_MOVE_MASK >>> 8;

        return new Fen.LoadedPosition(whiteToMove, squares, whiteCastleKingside, whiteCastleQueenside, blackCastleKingside, blackCastleQueenside, epFile, PlyCount, fiftyMoveCounter);
    }
}

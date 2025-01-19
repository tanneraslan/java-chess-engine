package com.chess.chessengine;

import com.chess.chessengine.util.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MoveGenerator {
    private int friendlyColor;
    private int opponentColor;
    private int friendlyKingSquare;
    private int epSquare;
    private int epPawnSquare;

    private int[] pinnedDirections = new int[64];
    private long pinnedPieces = 0;
    private long negatedOpponentPieceMap = 0;
    private long negatedFriendlyPieceMap = 0;
    public long diagonalSlidingMap = 0;
    public long straightSlidingMap = 0;
    public long pawnAttackMap = 0;

    long checkRay = 0;
    boolean inCheck = false;
    private boolean inDoubleCheck = false;
    private boolean epCanResolveCheck = false;

    public ArrayList<Integer> CurrentMoves;

    private void GeneratePins() {
        int[] friendlyKingEdgeDistance = StaticMoveData.EDGE_DISTANCE[friendlyKingSquare];
        long[] edgeRays = StaticMoveData.EDGE_RAYS[friendlyKingSquare];

        int friendlyPieceInDirection;
        boolean enPassantSquareInDirection;
        boolean isDiagonal;
        int offset;

        int targetSquare;

        for (int dir = 0; dir < 8; dir++) {
            isDiagonal = dir >= 4;

            if (isDiagonal && (diagonalSlidingMap & edgeRays[dir]) == 0) {
                continue;
            } else if (!isDiagonal && (straightSlidingMap & edgeRays[dir]) == 0) {
                continue;
            }

            friendlyPieceInDirection = -1;
            enPassantSquareInDirection = false;
            offset = StaticMoveData.DIRECTIONAL_OFFSETS[dir];

            for (int i = 0; i < friendlyKingEdgeDistance[dir]; i++) {
                targetSquare = friendlyKingSquare + offset * (i + 1);

                if (Bitboard.GetBit(Main.Board.FriendlyPieceMap, targetSquare) != 0) {
                    if (friendlyPieceInDirection == -1) {
                        friendlyPieceInDirection = targetSquare;
                    } else {
                        // If there's two friendly pieces in the way, there's no chance of a pin in this direction
                        break;
                    }
                } else if (Bitboard.GetBit(Main.Board.OpponentPieceMap, targetSquare) != 0) {
                    if ((isDiagonal && Bitboard.GetBit(diagonalSlidingMap, targetSquare) != 0) || (!isDiagonal && Bitboard.GetBit(straightSlidingMap, targetSquare) != 0)) {
                        // Sliding pieces
                        if (friendlyPieceInDirection != -1) {
                            if (enPassantSquareInDirection && Piece.IsType(Main.Board.Square[friendlyPieceInDirection], Piece.PAWN)) {
                                // Prevent en passant capture if pawn is pinned
                                epSquare = -1;
                            } else {
                                // Update the pinned directions to be the direction and its opposite
                                pinnedDirections[friendlyPieceInDirection] |= (1 << dir) | (1 << StaticMoveData.OPPOSITE_DIRECTIONS[dir]);
                                pinnedPieces = Bitboard.SetBit(pinnedPieces, friendlyPieceInDirection);
                            }
                        } else {
                            // We must be in check if there's nothing blocking

                            inDoubleCheck = inCheck;
                            inCheck = true;

                            // If we are in double check, the king has to move so there's no chance of another piece blocking it
                            if (!inDoubleCheck) {
                                for (int j = 0; j < (i + 1); j++) {
                                    checkRay = Bitboard.SetBit(checkRay, friendlyKingSquare + offset * (j + 1));
                                }
                            }

                            // No other sliding pieces in this direction can influence the attack
                            break;
                        }
                    } else if (!isDiagonal && targetSquare == epPawnSquare) {
                        enPassantSquareInDirection = true;
                    } else {
                        // If an enemy piece is blocking the attack, any other enemy sliding pieces are blocked as well
                        break;
                    }
                }
            }

            if (inDoubleCheck) {
                // If in double check, the only legal moves will involve the king, so we can ignore all other squares
                break;
            }
        }
    }

    private long GenerateSlidingAttackMap(int square, int startDir, int endDir) {
        long attackMap = 0L;
        int[] edgeDistances = StaticMoveData.EDGE_DISTANCE[square];

        int offset;
        int targetSquare;
        int targetPiece;

        for (int dir = startDir; dir < endDir; dir++) {
            offset = StaticMoveData.DIRECTIONAL_OFFSETS[dir];

            for (int i = 0; i < edgeDistances[dir]; i++) {
                targetSquare = square + offset * (i + 1);
                targetPiece = Main.Board.Square[targetSquare];

                attackMap = Bitboard.SetBit(attackMap, targetSquare);

                if (targetSquare != friendlyKingSquare && targetPiece != Piece.NONE) {
                    break;
                }
            }
        }

        return attackMap;
    }

    private void GenerateAttackMap() {
        long slidingAttackMap = 0;

        int square;

        PieceList[] opponentPieces = Main.Board.Pieces[opponentColor];

        PieceList opponentQueens = opponentPieces[Piece.QUEEN];
        for (int i = 0; i < opponentQueens.Count; i++) {
            square = opponentQueens.get(i);
            slidingAttackMap |= GenerateSlidingAttackMap(square, 0, 8);
            diagonalSlidingMap = Bitboard.SetBit(diagonalSlidingMap, square);
            straightSlidingMap = Bitboard.SetBit(straightSlidingMap, square);
        }

        PieceList opponentRooks = opponentPieces[Piece.ROOK];
        for (int i = 0; i < opponentRooks.Count; i++) {
            square = opponentRooks.get(i);
            slidingAttackMap |= GenerateSlidingAttackMap(square, 0, 4);
            straightSlidingMap = Bitboard.SetBit(straightSlidingMap, square);
        }

        PieceList opponentBishops = opponentPieces[Piece.BISHOP];
        for (int i = 0; i < opponentBishops.Count; i++) {
            square = opponentBishops.get(i);
            slidingAttackMap |= GenerateSlidingAttackMap(square, 4, 8);
            diagonalSlidingMap = Bitboard.SetBit(diagonalSlidingMap, square);
        }

        long map;
        long knightAttackMap = 0;

        PieceList opponentKnights = opponentPieces[Piece.KNIGHT];
        for (int i = 0; i < opponentKnights.Count; i++) {
            square = opponentKnights.get(i);
            map = StaticMoveData.KNIGHT_BITBOARDS[square];

            if (!inDoubleCheck && Bitboard.GetBit(map, friendlyKingSquare) == 1) {
                inDoubleCheck = inCheck;
                inCheck = true;

                if (!inDoubleCheck) {
                    checkRay = Bitboard.SetBit(checkRay, square);
                }
            }

            knightAttackMap |= map;
        }

        long pawnAttackMap = 0;

        PieceList opponentPawns = opponentPieces[Piece.PAWN];
        long[] pawnBitboards = StaticMoveData.PAWN_BITBOARDS[opponentColor];

        for (int i = 0; i < opponentPawns.Count; i++) {
            square = opponentPawns.get(i);
            map = pawnBitboards[square];

            if (Bitboard.GetBit(map, friendlyKingSquare) == 1) {
                inDoubleCheck = inCheck;
                inCheck = true;

                if (!inDoubleCheck) {
                    checkRay = Bitboard.SetBit(checkRay, square);

                    if (epPawnSquare == square) {
                        epCanResolveCheck = true;
                    }
                } else {
                    epCanResolveCheck = false;
                }
            }

            pawnAttackMap |= map;
        }

        int opponentKingSquare = Main.Board.KingSquare[opponentColor];
        long kingAttackMap = StaticMoveData.KING_BITBOARDS[opponentKingSquare];

        Main.Board.OpponentAttackMap = slidingAttackMap | knightAttackMap | pawnAttackMap | kingAttackMap;
    }

    boolean IsPinned(int square) {
        return Bitboard.GetBit(pinnedPieces, square) != 0;
    }

    private void GenerateKingMoves() {
        long moves = StaticMoveData.KING_BITBOARDS[friendlyKingSquare] & ~Main.Board.OpponentAttackMap & negatedFriendlyPieceMap;
        long castlingBitboard;

        if (inDoubleCheck && moves == 0) {
            return;
        }

        while (moves != 0) {
            CurrentMoves.add(Move.Construct(friendlyKingSquare, Bitboard.BitScanForward(moves), Move.FLAG_NONE));
            moves &= moves - 1;
        }

        // Castling
        if (!inCheck) {
            int castlingRights = (Main.Board.GameState & (3 + friendlyColor * 9)) >>> friendlyColor * 2;

            // King side
            if ((castlingRights & 1) != 0) {
                castlingBitboard = StaticMoveData.CASTLING_BITBOARDS[friendlyColor * 2];

                if ((castlingBitboard & negatedOpponentPieceMap & negatedFriendlyPieceMap & ~Main.Board.OpponentAttackMap) == castlingBitboard) {
                    CurrentMoves.add(Move.Construct(friendlyKingSquare, friendlyKingSquare + 2, Move.FLAG_CASTLING));
                }
            }

            // Queen side
            if ((castlingRights & 2) != 0) {
                castlingBitboard = StaticMoveData.CASTLING_BITBOARDS[friendlyColor * 2 + 1];

                if ((castlingBitboard & negatedFriendlyPieceMap & negatedOpponentPieceMap) == castlingBitboard) {
                    castlingBitboard = Bitboard.ClearBit(castlingBitboard, friendlyKingSquare - 3);

                    if ((castlingBitboard & ~Main.Board.OpponentAttackMap) == castlingBitboard) {
                        CurrentMoves.add(Move.Construct(friendlyKingSquare, friendlyKingSquare - 2, Move.FLAG_CASTLING));
                    }
                }
            }
        }
    }

    private void GenerateSlidingMoves(int square, int startDir, int endDir) {
        boolean pinned = IsPinned(square);
        int pinnedDir = 0;

        if (pinned) {
            pinnedDir = pinnedDirections[square];
        }

        int[] edgeDistances = StaticMoveData.EDGE_DISTANCE[square];
        int offset;
        int targetSquare;

        for (int dir = startDir; dir < endDir; dir++) {
            if (pinned && ((pinnedDir >>> dir) & 1) == 0) {
                continue;
            }

            offset = StaticMoveData.DIRECTIONAL_OFFSETS[dir];

            for (int i = 0; i < edgeDistances[dir]; i++) {
                targetSquare = square + offset * (i + 1);

                if (Bitboard.GetBit(Main.Board.FriendlyPieceMap, targetSquare) != 0) {
                    break;
                }

                if (!inCheck || Bitboard.GetBit(checkRay, targetSquare) != 0) {
                    CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_NONE));
                }



                if (Bitboard.GetBit(Main.Board.OpponentPieceMap, targetSquare) != 0) {
                    break;
                }
            }
        }
    }

    private void GenerateKnightMoves() {
        PieceList knights = Main.Board.Pieces[friendlyColor][Piece.KNIGHT];

        int square;

        long moves;

        for (int i = 0; i < knights.Count; i++) {
            square = knights.get(i);

            if (!IsPinned(square)) {
                moves = StaticMoveData.KNIGHT_BITBOARDS[square] & negatedFriendlyPieceMap;

                if (inCheck) {
                    moves &= checkRay;
                }
            } else {
                moves = 0;
            }

            while (moves != 0) {
                CurrentMoves.add(Move.Construct(square, Bitboard.BitScanForward(moves), Move.FLAG_NONE));
                moves &= moves - 1;
            }
        }
    }

    // TODO: ai crashing when analyzing promotions. Probably something wrong with the make move function
    private void AddPromotionMoves(int square, int targetSquare) {
        CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_P_QUEEN));
        CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_P_KNIGHT));
        CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_P_ROOK));
        CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_P_BISHOP));
    }

    private void GeneratePawnMoves() {
        int forwardOffset = friendlyColor == Board.WHITE ? -8 : 8;
        int homeRank =  friendlyColor == Board.WHITE ? 6 : 1;
        int doublePawnRank = friendlyColor == Board.WHITE ? 4 : 3;
        int promotionRank = friendlyColor == Board.WHITE ? 1 : 6;

        long epBitboard = Main.Board.OpponentPieceMap;
        long pawnCheckRay = checkRay;

        if (epSquare != -1) {
            epBitboard = Bitboard.SetBit(epBitboard, epSquare);
        }

        if (inCheck && epCanResolveCheck) {
            pawnCheckRay = Bitboard.SetBit(pawnCheckRay, epSquare);
        }

        PieceList pawns = Main.Board.Pieces[friendlyColor][Piece.PAWN];

        int square;
        int targetSquare;
        int rank;
        long moves;
        boolean pinned;
        int pinnedDir;
        int dir;
        boolean atPromotionRank;
        boolean atHomeRank;

        for (int i = 0; i < pawns.Count; i++) {
            square = pawns.get(i);
            rank = square / 8;
            moves = 0;

            pinned = IsPinned(square);
            pinnedDir = 0;

            atPromotionRank = rank == promotionRank;
            atHomeRank = rank == homeRank;

            if (pinned) {
                pinnedDir = pinnedDirections[square];
            }

            if (!pinned || ((pinnedDir >>> opponentColor) & 1) != 0) {
                moves = Bitboard.SetBit(moves, square + forwardOffset) & negatedFriendlyPieceMap & negatedOpponentPieceMap;

                if (moves != 0 && atHomeRank) {
                    moves = Bitboard.SetBit(moves, square + forwardOffset * 2) & negatedFriendlyPieceMap & negatedOpponentPieceMap;
                }
            }

            moves |= StaticMoveData.PAWN_BITBOARDS[friendlyColor][square] & epBitboard;

            if (pinned) {
                for (int j = 0; j < 2; j++) {
                    dir = StaticMoveData.PAWN_ATTACK_DIRECTIONS[friendlyColor][j];

                    if (((pinnedDir >>> dir) & 1) == 0) {
                        moves = Bitboard.ClearBit(moves, square + StaticMoveData.DIRECTIONAL_OFFSETS[dir]);
                    }
                }
            }

            if (inCheck) {
                moves &= pawnCheckRay;
            }

            while (moves != 0) {
                targetSquare = Bitboard.BitScanForward(moves);

               if (atPromotionRank) {
                   AddPromotionMoves(square, targetSquare);
               } else if (atHomeRank && targetSquare / 8 == doublePawnRank) {
                   CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_DOUBLE_PAWN));
               } else if (targetSquare == epSquare) {
                   CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_EN_PASSANT));
               } else {
                   CurrentMoves.add(Move.Construct(square, targetSquare, Move.FLAG_NONE));
               }

                moves &= moves - 1;
            }
        }
    }
    public ArrayList<Integer> GenerateMoves() {
        CurrentMoves = new ArrayList<>();

        friendlyColor = Main.Board.ColorToMove;
        opponentColor = (Main.Board.ColorToMove + 1) % 2;

        friendlyKingSquare = Main.Board.KingSquare[friendlyColor];
        epSquare = Main.Board.GameState >>> 4 & 0b1111;

        if (epSquare == 0) {
            epSquare = -1;
        } else {
            epSquare = epSquare - 1 + (friendlyColor == Board.WHITE ? 2 : 5) * 8;
        }

        pinnedDirections = new int[64];
        pinnedPieces = 0;
        diagonalSlidingMap = 0;
        straightSlidingMap = 0;
        pawnAttackMap = 0;
        checkRay = 0;
        inCheck = false;
        inDoubleCheck = false;
        epCanResolveCheck = false;

        epPawnSquare = -1;
        if (epSquare != -1) {
            epPawnSquare = epSquare - 8 * (2 * friendlyColor - 1);
        }

        GenerateAttackMap();

        negatedFriendlyPieceMap = ~Main.Board.FriendlyPieceMap;
        negatedOpponentPieceMap = ~Main.Board.OpponentPieceMap;

        GeneratePins();

        GenerateKingMoves();

        if (!inDoubleCheck) {
            PieceList queens = Main.Board.Pieces[friendlyColor][Piece.QUEEN];
            for (int i = 0; i < queens.Count; i++) {
                GenerateSlidingMoves(queens.get(i), 0, 8);
            }

            PieceList rooks = Main.Board.Pieces[friendlyColor][Piece.ROOK];
            for (int i = 0; i < rooks.Count; i++) {
                GenerateSlidingMoves(rooks.get(i), 0, 4);
            }

            PieceList bishops = Main.Board.Pieces[friendlyColor][Piece.BISHOP];
            for (int i = 0; i < bishops.Count; i++) {
                GenerateSlidingMoves(bishops.get(i), 4, 8);
            }

            GenerateKnightMoves();
            GeneratePawnMoves();
        }

        return CurrentMoves;
    }
}

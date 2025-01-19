package com.chess.chessengine.util;

public final class StaticMoveData {
    public static final int[] DIRECTIONAL_OFFSETS = {8, -8, -1, 1, 7, -7, 9, -9};
    public static final int[] OPPOSITE_DIRECTIONS = {1, 0, 3, 2, 5, 4, 7, 6};
    public static final long[] CASTLING_BITBOARDS = {
            0b0110000000000000000000000000000000000000000000000000000000000000L,
            0b0000111000000000000000000000000000000000000000000000000000000000L,

            0b0000000000000000000000000000000000000000000000000000000001100000L,
            0b0000000000000000000000000000000000000000000000000000000000001110L,
    };
    public static final int[][] PAWN_ATTACK_DIRECTIONS = {{5, 7}, {4, 6}};
    private static final int[][] KNIGHT_OFFSETS = {{-2, 1}, {-1, 2}, {1, 2}, {2, 1}, {-2,-1},{-1,-2},{1,-2},{2,-1}};
    public static final int[][] WEIGHT_MAPS = {
            new int[] { // KING
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -30,-40,-40,-50,-50,-40,-40,-30,
                    -20,-30,-30,-40,-40,-30,-30,-20,
                    -10,-20,-20,-20,-20,-20,-20,-10,
                    20, 20,  0,  0,  0,  0, 20, 20,
                    20, 30, 10,  0,  0, 10, 30, 20
            },
            new int[] { // PAWN
                    0,   0,   0,   0,   0,   0,   0,   0,
                    50,  50,  50,  50,  50,  50,  50,  50,
                    10,  10,  20,  30,  30,  20,  10,  10,
                    5,   5,  10,  25,  25,  10,   5,   5,
                    0,   0,   0,  20,  20,   0,   0,   0,
                    5,  -5, -10,   0,   0, -10,  -5,   5,
                    5,  10,  10, -20, -20,  10,  10,   5,
                    0,   0,   0,   0,   0,   0,   0,   0
            },
            new int[] { // KNIGHT
                    -50,-40,-30,-30,-30,-30,-40,-50,
                    -40,-20,  0,  0,  0,  0,-20,-40,
                    -30,  0, 10, 15, 15, 10,  0,-30,
                    -30,  5, 15, 20, 20, 15,  5,-30,
                    -30,  0, 15, 20, 20, 15,  0,-30,
                    -30,  5, 10, 15, 15, 10,  5,-30,
                    -40,-20,  0,  5,  5,  0,-20,-40,
                    -50,-40,-30,-30,-30,-30,-40,-50,
            },
            new int[] { // BISHOP
                    -20,-10,-10,-10,-10,-10,-10,-20,
                    -10,  0,  0,  0,  0,  0,  0,-10,
                    -10,  0,  5, 10, 10,  5,  0,-10,
                    -10,  5,  5, 10, 10,  5,  5,-10,
                    -10,  0, 10, 10, 10, 10,  0,-10,
                    -10, 10, 10, 10, 10, 10, 10,-10,
                    -10,  5,  0,  0,  0,  0,  5,-10,
                    -20,-10,-10,-10,-10,-10,-10,-20,
            },
            new int[] { // ROOK
                    0,  0,  0,  0,  0,  0,  0,  0,
                    5, 10, 10, 10, 10, 10, 10,  5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    -5,  0,  0,  0,  0,  0,  0, -5,
                    0,  0,  0,  5,  5,  0,  0,  0
            },
            new int[] { // QUEEN
                    -20,-10,-10, -5, -5,-10,-10,-20,
                    -10,  0,  0,  0,  0,  0,  0,-10,
                    -10,  0,  5,  5,  5,  5,  0,-10,
                    -5,  0,  5,  5,  5,  5,  0, -5,
                    0,  0,  5,  5,  5,  5,  0, -5,
                    -10,  5,  5,  5,  5,  5,  0,-10,
                    -10,  0,  5,  0,  0,  0,  0,-10,
                    -20,-10,-10, -5, -5,-10,-10,-20
            },
            new int[] { // KING END
                    -50,-40,-30,-20,-20,-30,-40,-50,
                    -30,-20,-10,  0,  0,-10,-20,-30,
                    -30,-10, 20, 30, 30, 20,-10,-30,
                    -30,-10, 30, 40, 40, 30,-10,-30,
                    -30,-10, 30, 40, 40, 30,-10,-30,
                    -30,-10, 20, 30, 30, 20,-10,-30,
                    -30,-30,  0,  0,  0,  0,-30,-30,
                    -50,-30,-30,-30,-30,-30,-30,-50
            },
            new int[] { // PAWN END
                    0,   0,   0,   0,   0,   0,   0,   0,
                    80,  80,  80,  80,  80,  80,  80,  80,
                    50,  50,  50,  50,  50,  50,  50,  50,
                    30,  30,  30,  30,  30,  30,  30,  30,
                    20,  20,  20,  20,  20,  20,  20,  20,
                    10,  10,  10,  10,  10,  10,  10,  10,
                    10,  10,  10,  10,  10,  10,  10,  10,
                    0,   0,   0,   0,   0,   0,   0,   0
            }
    };
    public static final int[] CENTRAL_MANHATTAN_DISTANCE = {
            6, 5, 4, 3, 3, 4, 5, 6,
            5, 4, 3, 2, 2, 3, 4, 5,
            4, 3, 2, 1, 1, 2, 3, 4,
            3, 2, 1, 0, 0, 1, 2, 3,
            3, 2, 1, 0, 0, 1, 2, 3,
            4, 3, 2, 1, 1, 2, 3, 4,
            5, 4, 3, 2, 2, 3, 4, 5,
            6, 5, 4, 3, 3, 4, 5, 6
    };
    public static byte[][] MANHATTAN_DISTANCE = new byte[64][64];
    public static int[][] EDGE_DISTANCE = new int[64][8];
    public static long[][] EDGE_RAYS = new long[64][8];
    public static long[] KNIGHT_BITBOARDS = new long[64];
    public static long[] KING_BITBOARDS = new long[64];
    public static long[][] PAWN_BITBOARDS = new long[2][64];

    public static void PrecomputeMoveData() {
        int top; int bottom; int left; int right; int b;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                top = 7 - y;
                bottom = y;
                left = x;
                right = 7 - x;
                b = x + y * 8;

                EDGE_DISTANCE[b] = new int[] {top, bottom, left, right, Math.min(top, left), Math.min(bottom, right), Math.min(top, right), Math.min(bottom, left)};

                for (int dir = 0; dir < 8; dir++) {
                    EDGE_RAYS[b][dir] = 0L;
                    if (EDGE_DISTANCE[b][dir] > 0) {
                        for (int i = 1; i < EDGE_DISTANCE[b][dir] + 1; i++) {
                            EDGE_RAYS[b][dir] = Bitboard.SetBit(EDGE_RAYS[b][dir], b + DIRECTIONAL_OFFSETS[dir] * i);
                        }
                    }
                }

                // For knights
                KNIGHT_BITBOARDS[b] = 0L;
                for (int j = 0; j < 8; j++) {
                    int targetX = x + KNIGHT_OFFSETS[j][0];
                    int targetY = y + KNIGHT_OFFSETS[j][1];

                    if (targetX >= 0 && targetX <= 7 && targetY >= 0 && targetY <= 7) {
                        KNIGHT_BITBOARDS[b] = Bitboard.SetBit(KNIGHT_BITBOARDS[b], targetX + targetY * 8);
                    }
                }

                // For pawns
                for (int color = 0; color < 2; color++) {
                    int forwardDir = (color + 1) % 2;

                    PAWN_BITBOARDS[color][b] = 0L;

                    for (int j = 0; j < 2; j++) {
                        int dir = 4 + j * 2 + forwardDir;

                        if (EDGE_DISTANCE[b][dir] > 0) {
                            PAWN_BITBOARDS[color][b] = Bitboard.SetBit(PAWN_BITBOARDS[color][b], b + DIRECTIONAL_OFFSETS[dir]);
                        }
                    }
                }

                // For kings
                KING_BITBOARDS[b] = 0L;
                for (int dir = 0; dir < 8; dir++) {
                    if (EDGE_DISTANCE[b][dir] > 0) {
                        KING_BITBOARDS[b] = Bitboard.SetBit(KING_BITBOARDS[b], b + DIRECTIONAL_OFFSETS[dir]);
                    }
                }

                // For manhattan distance
                int file1, file2, rank1, rank2;
                int rankDistance, fileDistance;
                for (int square = 0; square < 64; square++) {
                    file1 = b & 7;
                    file2 = square  & 7;
                    rank1 = b >> 3;
                    rank2 = square >> 3;
                    MANHATTAN_DISTANCE[b][square] = (byte) (Math.abs(rank2 - rank1) + Math.abs(file2 - file1));
                }
            }
        }
    }
}

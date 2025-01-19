package com.chess.chessengine.util;

import java.util.Collections;

public final class Bitboard {
    static private final long deBruijn = 0x03f79d71b4cb0a89L;
    static private final int[] magicTable = {
            0, 1,48, 2,57,49,28, 3,
            61,58,50,42,38,29,17, 4,
            62,55,59,36,53,51,43,22,
            45,39,33,30,24,18,12, 5,
            63,47,56,27,60,41,37,16,
            54,35,52,21,44,32,23,11,
            46,26,40,15,34,20,31,10,
            25,14,19, 9,13, 8, 7, 6,
    };

    public static long SetBit(long b, int i) {
        return b | (1L << i);
    }

    public static long ClearBit(long b, int i) {
        return b & ~(1L << i);
    }

    public static long GetBit(long b, int i) {
        return (b >>> i) & 1;
    }

    /**
     * @author Charles E. Leiserson
     *         Harald Prokop
     *         Keith H. Randall
     *
     * "Using de Bruijn Sequences to Index a 1 in a Computer Word"
     * @return index 0..63
     * @param bb a 64-bit word to bitscan, should not be zero
     */
    static public int BitScanForward(long b) {
        int idx = (int)(((b & -b) * deBruijn) >>> 58);
        return magicTable[idx];
    }

    public static String convertToString(long b) {
        String s = Long.toBinaryString(b);

        s = String.join("", Collections.nCopies((64 - s.length()), "0")) + s;

        String l = "";

        for (int i = 0; i < 64; i++) {
            if ((i % 8) == 0) {
                l = l + "\n";
            }
            l = l + " " + s.charAt(63 - i) + " ";
        }

        return l;
    }
}

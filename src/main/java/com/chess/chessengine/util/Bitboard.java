package com.chess.chessengine.util;

import java.util.Collections;

public final class Bitboard {
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
     * @return index 0..63 of the lowest set bit
     * @param b a 64-bit word to bitscan, should not be zero
     */
    static public int BitScanForward(long b) {
        return Long.numberOfTrailingZeros(b);
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

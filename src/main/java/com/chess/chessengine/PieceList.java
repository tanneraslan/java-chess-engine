package com.chess.chessengine;

public class PieceList {
    private final int[] occupiedSquares;
    private final int[] map;
    public int Count;

    public PieceList(int maxCount) {
        occupiedSquares = new int[maxCount];
        map = new int[64];
        Count = 0;
    }

    public void AddPiece(int square) {
        occupiedSquares[Count] = square;
        map[square] = Count;
        Count++;
    }

    public void RemovePiece(int square) {
        int index = map[square];
        occupiedSquares[index] = occupiedSquares[Count - 1];
        map[occupiedSquares[index]] = index;
        Count--;
    }

    public void MovePiece(int currentSquare, int targetSquare) {
        int pieceIndex = map[currentSquare];
        occupiedSquares[pieceIndex] = targetSquare;
        map[targetSquare] = pieceIndex;
    }

    public int get(int index) {
        return occupiedSquares[index];
    }
}

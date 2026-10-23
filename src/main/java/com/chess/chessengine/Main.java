package com.chess.chessengine;

import com.chess.chessengine.util.*;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.canvas.*;
import javafx.util.Duration;

import javafx.animation.Timeline;

public class Main extends Application {
    private Group root;
    private Scene scene;
    private Canvas canvas;
    private GraphicsContext renderer;
    private final Image spriteSheet = new Image(Objects.requireNonNull(Main.class.getResource("Pieces.png")).toExternalForm());
    private final double spriteSourceWidth = spriteSheet.getWidth() / 6;
    private final double spriteSourceHeight = spriteSheet.getHeight() / 2;

    private int selectedSquare = -1;
    private long targetSquares = 0L;
    private int promotingSquare = -1;
    private double mouseX = 0;
    private double mouseY = 0;
    public static int mostRecentMove = -1;
    public static ArrayList<Integer> currentMoves;
    public static final Stack<Integer> moveHistory = new Stack<>();

    private final static int WINDOW_SIZE = 768;
    private final static int SPRITE_SIZE = 96;
    private final static int[] PIECE_IMAGE_ORDER = {0, 5, 3, 2, 4, 1};
    private final static int[] PROMOTION_PIECE_ORDER = {Piece.QUEEN, Piece.KNIGHT, Piece.ROOK, Piece.BISHOP};
    private final static Font SQUARE_INDEX_FONT = Font.font("Book Antiqua", FontWeight.BOLD, SPRITE_SIZE * 0.2);
    // Shows move generator internals (pins, piece maps, square indices) on the board
    private final static boolean DEBUG_OVERLAY = false;

    public static Board Board;
    public static AI AI;

    private Color getSquareColor(int index, int x, int y) {
        boolean isEven = (x + y) % 2 == 0;

        if (selectedSquare == index) {
            return isEven ? Color.rgb(246,236,134) : Color.rgb(215,196,94);
        } else if (Bitboard.GetBit(targetSquares, index) != 0) {
            return isEven ? Color.rgb(222, 61, 75) : Color.rgb(176, 39, 47);
        } else if (mostRecentMove > 0 && (Move.GetCurrentSquare(mostRecentMove) == index || Move.GetTargetSquare(mostRecentMove) == index)) {
            return isEven ? Color.rgb(246,236,134) : Color.rgb(215,196,94);
        } else if (Board.MoveGenerator.inCheck && Bitboard.GetBit(Board.MoveGenerator.checkRay, index) != 0) {
            return isEven ? Color.rgb(247, 150, 90) : Color.rgb(204, 119, 22);
        } else if (DEBUG_OVERLAY && Board.MoveGenerator.IsPinned(index)) {
            return isEven ?  Color.rgb(210, 5, 118) : Color.rgb(150, 20, 107);
        } else {
            if (DEBUG_OVERLAY && Bitboard.GetBit(Board.FriendlyPieceMap, index) != 0) {
                return isEven ? Color.rgb(50,150,230) : Color.rgb(50,120,230);
            }
            if (DEBUG_OVERLAY && Bitboard.GetBit(Board.OpponentPieceMap, index) != 0) {
                return isEven ? Color.rgb(85,230,120) : Color.rgb(50,200,25);
            }
            return isEven ? Color.rgb(240, 217, 181) : Color.rgb(181, 136, 99);
        }
    }

    private void drawPiece(int piece, double x, double y) {
        double sourceX = PIECE_IMAGE_ORDER[Piece.PieceType(piece) - 1] * spriteSourceWidth;
        double sourceY = Piece.Color(piece) * spriteSourceHeight;
        renderer.drawImage(spriteSheet, sourceX, sourceY, spriteSourceWidth, spriteSourceHeight, x, y, SPRITE_SIZE, SPRITE_SIZE);
    }

    static void reportGameOver() {
        if (!currentMoves.isEmpty()) {
            return;
        }

        if (Board.MoveGenerator.inCheck) {
            System.out.println("Checkmate! " + (Board.ColorToMove == Board.WHITE ? "Black" : "White") + " wins");
        } else {
            System.out.println("Stalemate!");
        }
    }

    private void makePlayerMove(int move) {
        moveHistory.push(move);
        Board.MakeMove(move);
        mostRecentMove = move;
        currentMoves = Board.MoveGenerator.GenerateMoves();
        reportGameOver();

        if (AI.Enabled) {
            AI.run();
        }
    }
    private void gameLoop() {
        renderer.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int index = (x + y * 8);
                int piece = Board.Square[index];

                Color squareColor = getSquareColor(index, x, y);
                Color alternativeColor = DEBUG_OVERLAY ? getSquareColor(index, x + 1, y) : null;

                renderer.setFill(squareColor);
                renderer.fillRect(x * SPRITE_SIZE, y * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);

                if (DEBUG_OVERLAY) {
                    renderer.setFill(alternativeColor);
                    renderer.setTextAlign(TextAlignment.LEFT);
                    renderer.setFont(SQUARE_INDEX_FONT);
                    renderer.fillText(String.valueOf(index), x * SPRITE_SIZE, (y + 1) * SPRITE_SIZE);
                }

                if (piece != 0 && index != selectedSquare) {
                    drawPiece(piece, x * SPRITE_SIZE, y * SPRITE_SIZE);
                }
            }
        }

        if (promotingSquare != -1) {
            for (int i = 0; i < 4; i++) {
                int xPosition = (promotingSquare % 8) * SPRITE_SIZE;
                int yPosition;

                if ( (promotingSquare / 8) == 0) {
                    yPosition = SPRITE_SIZE * i;
                } else {
                    yPosition = WINDOW_SIZE - SPRITE_SIZE * (i + 1);
                }

                renderer.setFill(Color.WHITE);
                renderer.fillRect(xPosition, yPosition, SPRITE_SIZE, SPRITE_SIZE);

                drawPiece(Piece.Construct(Board.ColorToMove, PROMOTION_PIECE_ORDER[i]), xPosition, yPosition);
            }
        } else if (selectedSquare != -1) {
            drawPiece(Board.Square[selectedSquare], mouseX - (double) (SPRITE_SIZE / 2), mouseY - (double) (SPRITE_SIZE / 2));
        }
    }

    public void start(Stage stage) throws IOException {
        StaticMoveData.PrecomputeMoveData();

        if (PerftTest.enabled) {
            Board = new Board();
            PerftTest.start();
            Platform.exit();
        } else {

            root = new Group();
            scene = new Scene(root, WINDOW_SIZE, WINDOW_SIZE);
            canvas = new Canvas(WINDOW_SIZE, WINDOW_SIZE);

            renderer = canvas.getGraphicsContext2D();

            root.getChildren().add(canvas);

            stage.setResizable(false);
            stage.setTitle("Trash Chess Engine");
            stage.setScene(scene);

            Board = new Board();
            Board.LoadPosition(Fen.LoadFen(Fen.START_FEN));
            currentMoves = Board.MoveGenerator.GenerateMoves();

            AI = new AI();

            Timeline timeline = new Timeline();

            stage.setOnCloseRequest(event -> {
                timeline.stop();
                System.out.println("closing app..");
            });

            canvas.setOnMousePressed(mouseEvent -> {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();

                if (AI.InSearch || (AI.Enabled && Board.ColorToMove != AI.PlayerColor)) {
                    return;
                }

                if (promotingSquare != -1) {
                    int targetSquare = (int) (mouseX / SPRITE_SIZE) + (int) (mouseY / SPRITE_SIZE) * 8;

                    for (int i = 0; i < 4; i++) {
                        int squareY = promotingSquare / 8 == 0 ? i : 7 - i;

                        if (targetSquare == promotingSquare % 8 + squareY * 8) {
                            makePlayerMove(Move.Construct(selectedSquare, promotingSquare, PROMOTION_PIECE_ORDER[i] + 1));
                            break;
                        }
                    }

                    promotingSquare = -1;
                    selectedSquare = -1;
                    targetSquares = 0;
                } else if (selectedSquare == -1) {
                    selectedSquare = (int) (mouseX / SPRITE_SIZE) + (int) (mouseY / SPRITE_SIZE) * 8;

                    if (selectedSquare < 0 || selectedSquare > 63) {
                        selectedSquare = -1;
                        return;
                    }

                    int piece = Board.Square[selectedSquare];

                    if (Piece.IsType(piece, Piece.NONE) || !Piece.IsColor(piece, Board.ColorToMove)) {
                        selectedSquare = -1;
                        return;
                    }

                    targetSquares = 0;

                    for (int move : currentMoves) {
                        if (Move.GetCurrentSquare(move) == selectedSquare) {
                            targetSquares = Bitboard.SetBit(targetSquares, Move.GetTargetSquare(move));
                        }
                    }
                }
            });
            canvas.setOnMouseReleased(mouseEvent -> {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();

                if (selectedSquare == -1 || promotingSquare != -1) {
                    return;
                }

                int targetSquare = (int) (mouseX / SPRITE_SIZE) + (int) (mouseY / SPRITE_SIZE) * 8;

                if (targetSquare < 0 || targetSquare > 63) {
                    selectedSquare = -1;
                    targetSquares = 0;
                } else if (Bitboard.GetBit(targetSquares, targetSquare) != 0) {
                    if (Piece.IsType(Board.Square[selectedSquare], Piece.PAWN)) {
                        int promotionRow = (Board.ColorToMove == Board.WHITE ? 0 : 7);

                        if (targetSquare / 8 == promotionRow) {
                            promotingSquare = targetSquare;
                        }
                    }

                    if (promotingSquare == -1) {
                        for (int move : currentMoves) {
                            if (Move.GetCurrentSquare(move) == selectedSquare && Move.GetTargetSquare(move) == targetSquare) {
                                makePlayerMove(move);
                                break;
                            }
                        }

                        selectedSquare = -1;
                        targetSquares = 0;
                    }
                } else {
                    selectedSquare = -1;
                    targetSquares = 0;
                }
            });
            scene.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.SPACE && !moveHistory.isEmpty() && !AI.InSearch) {
                    selectedSquare = -1;
                    promotingSquare = -1;
                    targetSquares = 0;

                    // Against the AI, also take back the AI's reply so it's the player's turn again
                    do {
                        Board.UnmakeMove(moveHistory.pop());
                    } while (AI.Enabled && Board.ColorToMove != AI.PlayerColor && !moveHistory.isEmpty());

                    currentMoves = Board.MoveGenerator.GenerateMoves();

                    if (moveHistory.size() > 0) {
                        mostRecentMove = moveHistory.peek();
                    } else {
                        mostRecentMove = -1;
                    }

                    if (AI.Enabled && AI.AIColor == Board.ColorToMove) {
                        AI.run();
                    }
                }
            });

            canvas.setOnMouseDragged(mouseEvent -> {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            });

            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(10), actionEvent -> gameLoop()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

            stage.show();

            if (AI.Enabled && AI.AIColor == Board.ColorToMove) {
                AI.run();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
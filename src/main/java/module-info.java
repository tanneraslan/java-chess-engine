module com.chess.chessengine {
    requires javafx.controls;
    requires javafx.fxml;
            
                            
    opens com.chess.chessengine to javafx.fxml;
    exports com.chess.chessengine;
}
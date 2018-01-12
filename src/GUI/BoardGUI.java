package GUI;

import game.Board;
import game.Piece;
import game.Position;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import socket.MSocket;

/**
 * Class {@code BoardGUI}
 * Chessboard interface control,
 * visualized chessboard.
 * It's the main area of the game.
 *
 * @see Board
 * @see MainGUI
 * @see MSocket
 */
public class BoardGUI {

    /**
     * pixel length of one tile
     */
    public static final int TILE_SIZE = 50;

    /**
     * value of colors
     */
    public static final Color WHITE_BOARD_COLOR = Color.rgb(255, 250, 205),
            BLACK_BOARD_COLOR = Color.rgb(165, 42, 42),
            GREEN_BOARD_COLOR = Color.rgb(128, 255, 0),
            YELLOW_BOARD_COLOR = Color.rgb(255, 255, 0);

    private GraphicsContext context;

    /**
     * Main interface GUI,
     * interactive time use
     */
    private MainGUI mainGUI;

    /**
     * board logic class
     */
    private Board board;

    /**
     * moving piece
     */
    private Piece movingPiece;
    private boolean isDragging;

    /**
     * Record the position of dragging a piece
     */
    private int draggedPieceX, draggedPieceY;

    /**
     * online-play arguments
     */
    private boolean online;
    private Piece.Color playerColor;
    private MSocket socket;
    private Piece.Type promotion;

    /**
     * record-read arguments
     */
    private String[] records;
    private int count;

    @FXML
    private Canvas canvas;
    @FXML
    private Label whiteLabel;
    @FXML
    private Label blackLabel;
    @FXML
    private TextArea blackOutput;
    @FXML
    private TextArea whiteOutput;

    @FXML
    private void initialize() {
        context = canvas.getGraphicsContext2D();
        online = false;
        isDragging = false;
        movingPiece = null;
        setListeners();
        drawBoard();
        setBoardDisable(true);
        promotion = null;
    }

    /**
     * draw the canvas
     */
    private void draw() {
        drawBoard();

        for (Piece piece : board.getAllPieces()) {
            drawPiece(piece);
        }

        if (isDragging) {
            context.setFill(YELLOW_BOARD_COLOR);
            context.fillRect(movingPiece.getPosition().x * TILE_SIZE, movingPiece.getPosition().y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            context.drawImage(movingPiece.getImage(), draggedPieceX - TILE_SIZE / 2, draggedPieceY - TILE_SIZE / 2);
        }
    }

    private void drawPiece(Piece piece) {
        context.drawImage(piece.getImage(), piece.getPosition().x * TILE_SIZE, piece.getPosition().y * TILE_SIZE);
    }

    private void drawBoard() {
        context.setFill(WHITE_BOARD_COLOR);
        context.fillRect(0, 0, TILE_SIZE * 8, TILE_SIZE * 8);
        context.setFill(BLACK_BOARD_COLOR);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (!(x % 2 == y % 2)) {
                    context.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        if (movingPiece != null) {
            context.setFill(YELLOW_BOARD_COLOR);
            context.fillRect(movingPiece.getPosition().x * TILE_SIZE, movingPiece.getPosition().y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            context.setFill(GREEN_BOARD_COLOR);
            for (Position position : board.getAllMoves(movingPiece)) {
                context.fillRect(position.x * TILE_SIZE, position.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    /**
     * Execution function for interface operation
     *
     * @param position operation position
     */
    private void click(Position position) {
        if (movingPiece == null) {
            if (board.getPiece(position) != null && board.getPiece(position).getColor() == board.getTurn()) {
                movingPiece = board.getPiece(position);
            }
        } else {
            move(movingPiece.getPosition(), position);
        }
        draw();
    }

    /**
     * Execution function for interface operation
     */
    private void drag(int x, int y) {
        if (movingPiece == null || !isDragging) {
            return;
        }
        draggedPieceX = x;
        draggedPieceY = y;
        draw();
    }

    /**
     * Execution function for interface operation
     *
     * @param position operation position
     */
    private void dragEntered(Position position) {
        if (movingPiece == null) {
            Piece piece = board.getPiece(position);
            if (piece != null && piece.getColor() == board.getTurn()) {
                movingPiece = piece;
                isDragging = true;
            }
        } else {
            movingPiece = null;
        }
        draw();
    }

    /**
     * Execution function for interface operation
     *
     * @param position operation position
     */
    private void dragExited(Position position) {
        if (movingPiece.getPosition().equals(position)) {
            movingPiece = null;
        } else {
            move(movingPiece.getPosition(), position);
        }
        isDragging = false;
        draggedPieceX = draggedPieceY = -1000;
        draw();
    }

    /**
     * Move a piece and redraw on the interface
     *
     * @param start start move position
     * @param end   end move position
     */
    private void move(Position start, Position end) {
        if (board.move(start, end)) {
            check();
            changeTurn();
            if (online && board.getTurn() != playerColor) {
                sendMove(start, end);
                draw();
            }
        } else {
            checkGameEnd();
        }
        movingPiece = null;
    }

    /**
     * only used when online-game
     *
     * @param message move message
     */
    private void move(String message) {
        Position start = new Position(Integer.parseInt(message.substring(0, 1)), Integer.parseInt(message.substring(1, 2)));
        Position end = new Position(Integer.parseInt(message.substring(3, 4)), Integer.parseInt(message.substring(4, 5)));
        if (message.length() == 6) {
            switch (message.substring(5, 6)) {
                case "q":
                    promotion = Piece.Type.queen;
                    break;
                case "n":
                    promotion = Piece.Type.knight;
                    break;
                case "b":
                    promotion = Piece.Type.bishop;
                    break;
                case "r":
                    promotion = Piece.Type.rook;
                    break;
            }
        }
        if (board.move(start, end)) {
            check();
            changeTurn();
        } else {
            checkGameEnd();
        }
        movingPiece = null;
        //回合已经交换
        if (online && board.getTurn() != playerColor) {
            sendMove(start, end);
        }
        draw();
    }

    public void record(String str, Piece.Color color) {
        if (color == Piece.Color.white) {
            String text = whiteOutput.getText() + str + '\n';
            whiteOutput.setText(text);
            whiteOutput.setScrollTop(Double.MAX_VALUE);
        } else {
            String text = blackOutput.getText() + str + '\n';
            blackOutput.setText(text);
            blackOutput.setScrollTop(Double.MAX_VALUE);
        }
    }

    /**
     * Pop-up selection type window and
     * return the result
     *
     * @return the chosen result
     */
    public Piece.Type promotion() {
        if (online && promotion != null) {
            Piece.Type tmp = promotion;
            promotion = null;
            return tmp;
        } else {
            promotion = new PromotionBox().display();
            return promotion;
        }
    }

    /**
     * change the turn color
     * and change the border on label
     */
    private void changeTurn() {
        if (board.getTurn() == Piece.Color.white) {
            whiteLabel.getStyleClass().add("border");
            while (blackLabel.getStyleClass().contains("border")) {
                blackLabel.getStyleClass().remove("border");
            }
        } else {
            while (whiteLabel.getStyleClass().contains("border")) {
                whiteLabel.getStyleClass().remove("border");
            }
            blackLabel.getStyleClass().add("border");
        }
        if (online && board.getTurn() != playerColor) {
            setBoardDisable(true);
            waitMove();
        } else {
            setBoardDisable(false);
        }
    }

    /**
     * if one player is checked,
     * send message to warn
     */
    private void check() {
        if (board.isChecked(board.getTurn())) {
            mainGUI.sendMessage((Piece.changeColor(board.getTurn()) == Piece.Color.white ? "白方：" : "黑方：") + "将军！");
        }
    }

    public void gameStart(MainGUI mainGUI) {
        board = new Board(this);
        blackLabel.getStyleClass().remove("border");
        setBoardDisable(false);
        isDragging = false;
        movingPiece = null;
        this.mainGUI = mainGUI;
        whiteOutput.setText("");
        blackOutput.setText("");
        draw();
    }

    public void onlineStart(MainGUI mainGUI, Piece.Color color, MSocket socket) {
        online = true;
        playerColor = color;
        this.socket = socket;
        gameStart(mainGUI);

        if (color != Piece.Color.white) {
            setBoardDisable(true);
            waitMove();
        }
    }

    private void checkGameEnd() {
        if (board.gameEnd() != null) {
            setBoardDisable(true);
            mainGUI.gameEnd(board.gameEnd());
        }
    }

    public void setBoardDisable(boolean disable) {
        canvas.setDisable(disable);
    }

    /**
     * create a new thread and receive the message form socket to get move
     */
    private void waitMove() {
        mainGUI.sendMessage("等待对方走子...");
        new Thread(() -> {
            String result = socket.receive();
            switch (result) {
                case "Quit":
                    mainGUI.sendMessage("对方已退出房间");
                    Platform.runLater(() -> mainGUI.exitRoom());
                    break;
                case "Error":
                    mainGUI.sendMessage("连接中断");
                    Platform.runLater(() -> mainGUI.exitRoom());
                    break;
                default:
                    Platform.runLater(() -> move(result));
            }
        }).start();
    }

    private void sendMove(Position start, Position end) {
        String send = start.toString() + "+" + end.toString();
        if (promotion != null) {
            switch (promotion) {
                case queen:
                    send += "q";
                    break;
                case knight:
                    send += "k";
                    break;
                case rook:
                    send += "r";
                    break;
                case bishop:
                    send += "b";
                    break;
            }
            promotion = null;
        }
        socket.send(send);
    }

    /**
     * set canvas listeners
     */
    private void setListeners() {
        canvas.setOnMouseClicked(event -> {
            if (!isDragging)
                click(new Position((int) (event.getX() / TILE_SIZE), (int) (event.getY() / TILE_SIZE)));
        });

        canvas.setOnMousePressed(event ->
                isDragging = false
        );

        canvas.setOnMouseReleased(event -> {
            if (isDragging) {
                dragExited(new Position((int) event.getX() / 50, (int) event.getY() / 50));
            }
        });

        canvas.setOnMouseDragged(event -> {
            if (isDragging) {
                drag((int) event.getX(), (int) event.getY());
            } else {
                dragEntered(new Position((int) event.getX() / 50, (int) event.getY() / 50));
            }
        });
    }

    /**
     * Class {@code PromotionBox}
     * Selection type window that pops up when promotion
     */
    private class PromotionBox {
        public Piece.Type display() {
            final Piece.Type[] type = {Piece.Type.queen};
            Stage window = new Stage();
            window.setTitle("晋升！");
            window.initModality(Modality.APPLICATION_MODAL);
            window.setMinWidth(300);
            window.setMinHeight(100);

            Button queen = new Button("后");
            Button rook = new Button("车");
            Button bishop = new Button("象");
            Button knight = new Button("马");

            queen.setOnAction(event -> {
                type[0] = Piece.Type.queen;
                window.close();
            });
            rook.setOnAction(event -> {
                type[0] = Piece.Type.rook;
                window.close();
            });
            bishop.setOnAction(event -> {
                type[0] = Piece.Type.bishop;
                window.close();
            });
            knight.setOnAction(event -> {
                type[0] = Piece.Type.knight;
                window.close();
            });

            HBox layout = new HBox(10);
            layout.getChildren().addAll(queen, rook, bishop, knight);
            layout.setAlignment(Pos.CENTER);

            Scene scene = new Scene(layout);
            window.setScene(scene);
            window.showAndWait();
            return type[0];
        }
    }

    public String getRecord() {
        return board.record;
    }

    /**
     * @param record  the record
     * @param mainGUI mainGUI
     */
    public void recordStart(String record, MainGUI mainGUI) {
        count = 0;
        records = record.split("#");
        board = new Board(this);
        board.setBoard(records[count], count);
        setBoardDisable(true);
        this.mainGUI = mainGUI;
        draw();
    }

    /**
     * Watch the corresponding operation manuals
     */
    public void previous() {
        if (count != 0) {
            count--;
            board.setBoard(records[count], count);
            changeTurn();
            draw();
        } else {
            mainGUI.sendMessage("已经是第一步了");
        }
    }

    /**
     * Watch the corresponding operation manuals
     */
    public void next() {
        if (count < records.length - 1) {
            count++;
            board.setBoard(records[count], count);
            changeTurn();
            draw();
        } else {
            mainGUI.sendMessage("已经是最后一步了");
        }
    }

    /**
     * Watch the corresponding operation manuals
     */
    public void restart() {
        count = 0;
        board.setBoard(records[count], count);
        changeTurn();
        draw();
    }

    /**
     * Watch the corresponding operation manuals
     */
    public void last() {
        count = records.length - 1;
        board.setBoard(records[count], count);
        changeTurn();
        draw();
    }

    /**
     * Watch the corresponding operation manuals
     */
    public void recordEnd() {
        board = new Board(this);
        setBoardDisable(true);
        drawBoard();
    }

    /**
     * continue game
     */
    public void continueGame() {
        mainGUI.sendMessage("\n---------------\n游戏开始！");
        setBoardDisable(false);
        String tmp = "";
        for (int i = 0; i <= count; i++) {
            tmp += records[i] + "#";
        }
        board.continueBoard(records[count], count, tmp);
    }
}


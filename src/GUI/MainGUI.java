package GUI;

import game.Piece;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import socket.MSocket;

import java.io.*;

/**
 * The main interface control,
 * mainly to choose the game mode,
 * access to the chess game player,
 * and display the game interactive information.
 * includes the Class {@code BoardGUI}
 *
 * @see BoardGUI
 */
public class MainGUI {

    private BoardGUI boardController;
    private MSocket socket;

    @FXML
    private AnchorPane boardContainer;
    @FXML
    private TextArea messageText;
    @FXML
    private VBox menuBox;

    @FXML
    private void initialize() throws IOException {
        FXMLLoader boardLoader = new FXMLLoader(getClass().getResource("board.fxml"));
        boardContainer.getChildren().setAll((AnchorPane) boardLoader.load());
        boardController = boardLoader.getController();
        socket = null;
        messageText.setText("系统消息\n");
        mainButtons();
    }

    /**
     * Replace buttons and set listeners and attributes
     * the main buttons
     */
    private void mainButtons() {
        menuBox.getChildren().removeAll(menuBox.getChildren());

        Button offlineStart = new Button();
        offlineStart.setPrefHeight(25);
        offlineStart.setPrefWidth(150);
        offlineStart.setText("离线对战");
        offlineStart.setOnAction(event -> {
            offlineStart.setText("重新开始");
            clearMessage();
            boardController.gameStart(this);
            sendMessage("---------------\n游戏开始！");
        });

        Button onlineStart = new Button();
        onlineStart.setPrefHeight(25);
        onlineStart.setPrefWidth(150);
        onlineStart.setText("在线对战");
        onlineStart.setOnAction(event -> onlineButtons());

        Button record = new Button();
        record.setPrefHeight(25);
        record.setPrefWidth(150);
        record.setText("记录棋谱");
        record.setOnAction(event -> saveFile());

        Button read = new Button();
        read.setPrefHeight(25);
        read.setPrefWidth(150);
        read.setText("读取棋谱");
        read.setOnAction(event -> readFile());

        menuBox.getChildren().addAll(offlineStart, onlineStart, record, read);
    }

    /**
     * Replace buttons and set listeners and attributes
     * the buttons to enter online-play
     */
    private void onlineButtons() {
        menuBox.getChildren().removeAll(menuBox.getChildren());

        Button createRoom = new Button();
        createRoom.setPrefHeight(25);
        createRoom.setPrefWidth(150);
        createRoom.setText("创建房间");
        createRoom.setOnAction(event -> createRoom());

        TextField roomNumber = new TextField();
        roomNumber.setPrefHeight(25);
        roomNumber.setPrefWidth(150);

        Button addRoom = new Button();
        addRoom.setPrefHeight(25);
        addRoom.setPrefWidth(150);
        addRoom.setText("加入房间");
        addRoom.setOnAction(event -> addRoom(roomNumber.getText()));

        Button quit = new Button();
        quit.setPrefHeight(25);
        quit.setPrefWidth(150);
        quit.setText("返回");
        quit.setOnAction(event -> mainButtons());

        menuBox.getChildren().addAll(createRoom, roomNumber, addRoom, quit);
    }

    /**
     * Replace buttons and set listeners and attributes
     * the button to quit online-play
     */
    private void onlinePlayButtons() {
        menuBox.getChildren().removeAll(menuBox.getChildren());

        Button quit = new Button();
        quit.setPrefHeight(25);
        quit.setPrefWidth(150);
        quit.setText("退出");
        quit.setOnAction(event -> {
            socket.send("Quit");
            exitRoom();
        });

        menuBox.getChildren().addAll(quit);
    }

    /**
     * Replace buttons and set listeners and attributes
     * the buttons to watch the record
     */
    private void recordButtons() {
        menuBox.getChildren().removeAll(menuBox.getChildren());

        Button previous = new Button();
        previous.setPrefHeight(25);
        previous.setPrefWidth(150);
        previous.setText("上一步");
        previous.setOnAction(event -> boardController.previous());

        Button next = new Button();
        next.setPrefHeight(25);
        next.setPrefWidth(150);
        next.setText("下一步");
        next.setOnAction(event -> boardController.next());

        Button first = new Button();
        first.setPrefHeight(25);
        first.setPrefWidth(150);
        first.setText("重新开始");
        first.setOnAction(event -> boardController.restart());

        Button last = new Button();
        last.setPrefHeight(25);
        last.setPrefWidth(150);
        last.setText("最后一步");
        last.setOnAction(event -> boardController.last());

        Button continueGame = new Button();
        continueGame.setPrefHeight(25);
        continueGame.setPrefWidth(150);
        continueGame.setText("继续游戏");
        continueGame.setOnAction(event -> {
            boardController.continueGame();
            clearMessage();
            mainButtons();
        });

        Button quit = new Button();
        quit.setPrefHeight(25);
        quit.setPrefWidth(150);
        quit.setText("退出");
        quit.setOnAction(event -> {
            boardController.recordEnd();
            mainButtons();
        });

        menuBox.getChildren().addAll(previous, next, last, continueGame, quit);
    }

    /**
     * end the game and send winner message
     *
     * @param color winner color
     */
    public void gameEnd(Piece.Color color) {
        sendMessage("游戏结束！\n胜利者：" + (color == Piece.Color.white ? "白方" : "黑方"));
        if (socket != null) {
            exitRoom();
        }
    }

    /**
     * Display information to the message box below
     *
     * @param str message
     */
    public void sendMessage(String str) {
        String text = messageText.getText() + str + '\n';
        messageText.setText(text);
        messageText.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Clear information in the message box below
     */
    public void clearMessage() {
        messageText.setText("系统消息\n");
    }

    /**
     * create game room and
     * create a thread to wait for another player join the room
     */
    private void createRoom() {
        socket = new MSocket();
        String roomNum = socket.createRoom();
        if (roomNum.equals("Error")) {
            sendMessage("创建房间失败，请检查网络");
            return;
        }
        sendMessage("创建房间成功，房间号：" + roomNum + ", 等待加入...");

        new Thread(() -> {
            if (socket.connect()) {
                Platform.runLater(() -> {
                    clearMessage();
                    boardController.onlineStart(this, Piece.Color.white, socket);
                    sendMessage("\n---------------\n游戏开始！");
                    onlinePlayButtons();
                });
            } else {
                sendMessage("连接失败，请重新创建房间");
            }
        }).start();
    }

    /**
     * join the game room
     *
     * @param roomNum room number
     */
    private void addRoom(String roomNum) {
        socket = new MSocket();
        if (socket.enterRoom(roomNum)) {
            clearMessage();
            sendMessage("加入房间" + roomNum + "成功");
            sendMessage("\n---------------\n游戏开始！");
            boardController.onlineStart(this, Piece.Color.black, socket);
            onlinePlayButtons();
        } else {
            sendMessage("加入房间" + roomNum + "失败， 请检查房间号， 并确认主机IP能与本机直接连通");
        }
    }

    public void exitRoom() {
        socket.disconnect();
        socket = null;
        sendMessage("已退出房间");
        mainButtons();
        boardController.setBoardDisable(true);
    }

    /**
     * open the file dialog and choose a file to save current board
     */
    private void saveFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("记录棋谱");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        File file = fileChooser.showSaveDialog(boardContainer.getScene().getWindow());
        if (file != null) {
            try {
                BufferedWriter output = new BufferedWriter(new FileWriter(file));
                output.write(boardController.getRecord());
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * open the file dialog and choose a record file to open and watch
     */
    public void readFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("读取棋谱");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        File file = fileChooser.showOpenDialog(boardContainer.getScene().getWindow());
        String result = "";
        if (file != null) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String str;
                result += br.readLine();
                while ((str = br.readLine()) != null) {
                    result += '\n' + str;
                }
                boardController.recordStart(result, this);
                recordButtons();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

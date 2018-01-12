package game;

import GUI.BoardGUI;

import java.util.ArrayList;

/**
 * Class {@code Board}
 * Chessboard, storage and management of chess,
 * chess logic, game logic, recording, and so on.
 * Class {@code BoardGUI} shall be fully
 * for the control and refresh of the board.
 *
 * @see BoardGUI
 * @see Piece
 * @see Position
 */

public class Board {

    /**
     * A chessman array that records the chessboard
     */
    private Piece[][] board = new Piece[8][8];

    /**
     * Record the party that is currently moving
     */
    private Piece.Color turn;

    /**
     * Whether the black or white is being checked
     */
    private boolean blackChecked, whiteChecked;

    private Piece.Color winner;

    private BoardGUI boardGUI;

    /**
     * The variable is only for the purpose of en Passant.
     */
    private String lastMove;

    private int step;

    /**
     * convert the board into string type chess and connection them
     */
    public String record;

    public Board(BoardGUI boardGUI) {
        setSide(0);
        setSide(7);
        turn = Piece.Color.white;
        blackChecked = whiteChecked = false;
        winner = null;
        step = 1;
        record = "";
        this.boardGUI = boardGUI;
    }

    private void setSide(int y) {
        Piece.Color color = y < 4 ? Piece.Color.black : Piece.Color.white;
        int pawnY = y < 4 ? 1 : 6;

        for (int x = 0; x < 8; x++)
            board[x][pawnY] = new Piece(new Position(x, pawnY), color, Piece.Type.pawn);

        board[0][y] = new Piece(new Position(0, y), color, Piece.Type.rook);
        board[7][y] = new Piece(new Position(7, y), color, Piece.Type.rook);
        board[1][y] = new Piece(new Position(1, y), color, Piece.Type.knight);
        board[6][y] = new Piece(new Position(6, y), color, Piece.Type.knight);
        board[2][y] = new Piece(new Position(2, y), color, Piece.Type.bishop);
        board[5][y] = new Piece(new Position(5, y), color, Piece.Type.bishop);
        board[3][y] = new Piece(new Position(3, y), color, Piece.Type.queen);
        board[4][y] = new Piece(new Position(4, y), color, Piece.Type.king);
    }

    public Piece getPiece(int x, int y) {
        if (x >= 0 && x < 8 && y >= 0 && y < 8)
            return board[x][y];
        return null;
    }

    public Piece getPiece(Position position) {
        return getPiece(position.x, position.y);
    }


    public ArrayList<Piece> getAllPieces() {
        ArrayList<Piece> result = new ArrayList<>();
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) {
                Piece piece = getPiece(x, y);
                if (piece != null) {
                    result.add(piece);
                }
            }
        return result;
    }

    /**
     * Get all the pieces of a certain color
     *
     * @param color color to get
     * @return array of the pieces
     */
    public ArrayList<Piece> getAllPiecesOfType(Piece.Color color) {
        ArrayList<Piece> pieces = getAllPieces();
        ArrayList<Piece> result = new ArrayList<>();
        for (Piece piece : pieces)
            if (piece.getColor() == color)
                result.add(piece);
        return result;
    }

    /**
     * Get all the possible positions of a piece
     *
     * @param piece piece to get
     * @return array of the positions
     */
    public ArrayList<Position> getAllMoves(Piece piece) {
        switch (piece.getType()) {
            case king:
                return moveKing(piece);
            case queen:
                return moveQueen(piece);
            case rook:
                return moveRook(piece);
            case bishop:
                return moveBishop(piece);
            case knight:
                return moveKnight(piece);
            case pawn:
                return movePawn(piece);
        }
        return new ArrayList<>();
    }

    /**
     * Get the range of the attack of all the pieces of a color
     * Attention, the soldier can only go forward and
     * the range of the attack is OBLIQUE.
     *
     * @param color color to get
     * @return array of the positions
     */
    public ArrayList<Position> getAttackFieldOfType(Piece.Color color) {
        ArrayList<Piece> pieces = getAllPiecesOfType(color);
        ArrayList<Position> result = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.getType() == Piece.Type.king) {
                result.addAll(kingAttackField(p));
                continue;
            }
            if (p.getType() == Piece.Type.pawn) {
                result.addAll(pawnAttackField(p));
                continue;
            }
            result.addAll(getAllMoves(p));
        }
        return result;
    }

    /**
     * move piece
     * Determine whether the movement is correct,
     * check whether the game is finished after moving,
     * and then move according to the type of piece.
     * Check the check, record the movement
     * and change the rounds, if move correctly,
     * the return will be {@code true} or it will return {@code false}
     *
     * @param start start position
     * @param end   end position
     * @return if moving is correct
     */
    public boolean move(Position start, Position end) {
        Piece piece = getPiece(start);
        ArrayList<Position> moves = getAllMoves(piece);


        //非法移动
        if (!moves.contains(end)) {
            return false;
        }

        //检测游戏结束
        if (getPiece(end) != null && getPiece(end).getType() == Piece.Type.king) {
            board[end.x][end.y] = piece;
            board[piece.getPosition().x][piece.getPosition().y] = null;
            piece.moveTo(end);
            winner = piece.getColor();
            //for online
            changeTurn();
            return false;
        }

        //移动逻辑
        Piece.Type promotion = null;
        switch (piece.getType()) {
            case pawn:
                //吃过路兵
                if (enPassant(piece).contains(end)) {
                    if (piece.getPosition().x < end.x) {
                        remove(getPiece(piece.getPosition().changeX(1)));
                    } else {
                        remove(getPiece(piece.getPosition().changeX(-1)));
                    }
                }
                //晋升
                if (end.y == 0 || end.y == 7) {
                    promotion = boardGUI.promotion();
                    piece.setType(promotion);
                }
                break;
            case king:
                //王车易位
                if (castling(piece).contains(end)) {
                    if (end.x > piece.getPosition().x) {
                        board[7][end.y].moveTo(end.changeX(-1));
                        board[end.x - 1][end.y] = board[7][end.y];
                        board[7][end.y] = null;
                    } else {
                        board[0][end.y].moveTo(end.changeX(1));
                        board[end.x + 1][end.y] = board[0][end.y];
                        board[0][end.y] = null;
                    }
                }
                break;
        }
        //吃子
        if (getPiece(end) != null) {
            remove(getPiece(end));
        }
        //位移
        board[end.x][end.y] = piece;
        board[piece.getPosition().x][piece.getPosition().y] = null;
        piece.moveTo(end);

        check();
        record(start, end, promotion);
        changeTurn();
        return true;
    }

    /**
     * The next few functions are the moving logic
     * of a single piece and have a similar structure.
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> moveKing(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if ((x == 0 && y == 0)
                        || position.x + x < 0 || position.x + x > 7
                        || position.y + y < 0 || position.y + y > 7) {
                    continue;
                }
                Piece other = getPiece(position.x + x, position.y + y);
                if (other != null) {
                    if (other.getColor() == piece.getColor()) {
                        continue;
                    }
                }
                result.add(new Position(position.x + x, position.y + y));
            }
        }

        result.removeAll(getAttackFieldOfType(Piece.changeColor(piece.getColor())));

        result.addAll(castling(piece));
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> kingAttackField(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if ((x == 0 && y == 0)
                        || position.x + x < 0 || position.x + x > 7
                        || position.y + y < 0 || position.y + y > 7) {
                    continue;
                }
                result.add(new Position(position.x + x, position.y + y));
            }
        }
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> castling(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        ArrayList<Position> attacked = getAttackFieldOfType(Piece.changeColor(piece.getColor()));
        Position position = piece.getPosition();
        int y = piece.getColor() == Piece.Color.white ? 7 : 0;
        if (piece.isMoved()) {
            return result;
        }
        Piece rrook = getPiece(7, y);
        Piece lrook = getPiece(0, y);
        //短易位
        if (rrook.getType() == Piece.Type.rook && !rrook.isMoved()) {
            if (getPiece(position.changeX(1)) == null && !attacked.contains(position.changeX(1)) &&
                    getPiece(position.changeX(2)) == null && !attacked.contains(position.changeX(2))) {
                result.add(position.changeX(2));
            }
        }
        //长易位
        if (lrook.getType() == Piece.Type.rook && !lrook.isMoved()) {
            if (getPiece(position.changeX(-1)) == null && !attacked.contains(position.changeX(-1)) &&
                    getPiece(position.changeX(-2)) == null && !attacked.contains(position.changeX(-2))) {
                result.add(position.changeX(-2));
            }
        }
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> moveQueen(Piece piece) {
        ArrayList<Position> result = moveRook(piece);
        result.addAll(moveBishop(piece));
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> moveRook(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();

        while (position.x < 7) {
            position = position.changeX(1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.x > 0) {
            position = position.changeX(-1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.y < 7) {
            position = position.changeY(1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.y > 0) {
            position = position.changeY(-1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> moveBishop(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();

        while (position.x < 7 && position.y < 7) {
            position = position.changeX(1).changeY(1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.x > 0 && position.y < 7) {
            position = position.changeX(-1).changeY(1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.x < 7 && position.y > 0) {
            position = position.changeX(1).changeY(-1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        position = piece.getPosition();
        while (position.x > 0 && position.y > 0) {
            position = position.changeX(-1).changeY(-1);
            if (getPiece(position) != null) {
                if (getPiece(position).getColor() != piece.getColor()) {
                    result.add(position);
                }
                break;
            }
            result.add(position);
        }
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> moveKnight(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();

        result.add(position.changeX(2).changeY(1));
        result.add(position.changeX(2).changeY(-1));
        result.add(position.changeX(1).changeY(2));
        result.add(position.changeX(1).changeY(-2));
        result.add(position.changeX(-2).changeY(1));
        result.add(position.changeX(-2).changeY(-1));
        result.add(position.changeX(-1).changeY(2));
        result.add(position.changeX(-1).changeY(-2));

        for (int i = result.size() - 1; i >= 0; i--) {
            Piece other = getPiece(result.get(i));
            if (other != null) {
                if (other.getColor() == piece.getColor()) {
                    result.remove(i);
                }
            }
        }

        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> movePawn(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        int row = piece.getColor() == Piece.Color.white ? 6 : 1;
        int one = piece.getColor() == Piece.Color.white ? -1 : 1;
        //单步
        Position next = piece.getPosition().changeY(one);
        if (getPiece(next) == null) {
            result.add(next);
        }
        //双步
        if (!piece.isMoved() && piece.getPosition().y == row) {
            next = piece.getPosition().changeY(2 * one);
            if (getPiece(next) == null) {
                result.add(next);
            }
        }
        //斜吃
        Piece left = getPiece(piece.getPosition().changeX(-1).changeY(one));
        Piece right = getPiece(piece.getPosition().changeX(1).changeY(one));
        if (left != null && left.getColor() != piece.getColor()) {
            result.add(left.getPosition());
        }
        if (right != null && right.getColor() != piece.getColor()) {
            result.add(right.getPosition());
        }
        //过路
        result.addAll(enPassant(piece));
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> pawnAttackField(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        int one = piece.getColor() == Piece.Color.white ? -1 : 1;
        if (piece.getPosition().x - 1 >= 0)
            result.add(piece.getPosition().changeX(-1).changeY(one));
        if (piece.getPosition().x + 1 <= 7)
            result.add(piece.getPosition().changeX(1).changeY(one));
        return result;
    }

    /**
     * moving logic
     *
     * @param piece moving piece
     * @return position can move to
     */
    private ArrayList<Position> enPassant(Piece piece) {
        ArrayList<Position> result = new ArrayList<>();
        Position position = piece.getPosition();
        int row = piece.getColor() == Piece.Color.white ? 3 : 4;
        int one = piece.getColor() == Piece.Color.white ? -1 : 1;

        if (position.y == row) {
            Piece left = getPiece(position.changeX(-1));
            Piece right = getPiece(position.changeX(1));
            if (left != null && lastMove.substring(5, 7).equals(left.getPosition().format())) {
                result.add(left.getPosition().changeY(one));
            }
            if (right != null && lastMove.substring(5, 7).equals(right.getPosition().format())) {
                result.add(right.getPosition().changeY(one));
            }
        }
        return result;
    }

    /**
     * Check for checking and change
     * {@code blackChecked} and {@code whiteChecked}
     */
    private void check() {
        //找到对方的王
        ArrayList<Piece> pieces = getAllPiecesOfType(Piece.changeColor(turn));
        Piece king = null;
        for (Piece p : pieces) {
            if (p.getType() == Piece.Type.king) {
                king = p;
                break;
            }
        }

        if (getAttackFieldOfType(turn).contains(king.getPosition())) {
            if (turn == Piece.Color.white) {
                blackChecked = true;
            } else {
                whiteChecked = true;
            }
            return;
        }
        blackChecked = whiteChecked = false;
    }

    public boolean isChecked(Piece.Color color) {
        if ((color == Piece.Color.white && whiteChecked) ||
                (color == Piece.Color.black && blackChecked)) {
            return true;
        }
        return false;
    }

    private void remove(Piece piece) {
        Position position = piece.getPosition();
        board[position.x][position.y] = null;
    }

    public Piece.Color getTurn() {
        return turn;
    }

    public void changeTurn() {
        turn = Piece.changeColor(turn);
    }

    /**
     * Record single step
     *
     * @param start start of step
     * @param end   end of step
     * @param type  If the pawn is promoted, this is needed
     */
    public void record(Position start, Position end, Piece.Type type) {
        String result = start.format() + " - " + end.format();
        lastMove = result;
        result = step++ + "." + result;
        if (type != null) {
            result = result + "\npro:" + type;
        }
        boardGUI.record(result, turn);
        record += this.toString();
    }

    public Piece.Color gameEnd() {
        return winner;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null) {
                    result += board[i][j].getLetter();
                } else {
                    result += "**";
                }
            }
            result += "\n";
        }
        result += "#";
        return result;
    }

    /**
     * Change the position of a chessboard by string,
     * will be used only to read the chess manual
     *
     * @param str  the chess manual string
     * @param step the step
     */
    public void setBoard(String str, int step) {
        Piece[][] board = new Piece[8][8];
        String[] strings = str.split("\n");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 16; j = j + 2) {
                if (strings[i].charAt(j) != '*') {
                    Piece.Type type = Piece.Type.pawn;
                    switch (strings[i].charAt(j)) {
                        case 'K':
                            type = Piece.Type.king;
                            break;
                        case 'Q':
                            type = Piece.Type.queen;
                            break;
                        case 'B':
                            type = Piece.Type.bishop;
                            break;
                        case 'N':
                            type = Piece.Type.knight;
                            break;
                        case 'R':
                            type = Piece.Type.rook;
                            break;
                        case 'P':
                            type = Piece.Type.pawn;
                            break;
                    }
                    board[i][j / 2] = new Piece(new Position(i, j / 2), strings[i].charAt(j + 1) == 'W' ? Piece.Color.white : Piece.Color.black, type);
                }
            }
        }
        this.board = board;
        this.step = step + 1;
        this.turn = step % 2 == 0 ? Piece.Color.black : Piece.Color.white;
    }

    public void continueBoard(String record, int step, String records) {
        this.record = records;
        setBoard(record, step);
    }
}

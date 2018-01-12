package game;

import javafx.scene.image.Image;

/**
 * Class {@code Piece}
 * represents the chessmen in all games,
 * and stores the basic properties of each chessman,
 * including the color, type, location, picture
 * and some auxiliary functions of chessmen.
 */

public class Piece {

    /**
     * Enum {@code Color}
     * used to represent two kinds of chessmen in black and white
     */
    public enum Color {
        white, black
    }

    /**
     * Auxiliary function, converting black and white
     *
     * @param color color to change
     * @return opposite color
     */
    public static Color changeColor(Color color) {
        return color == Color.white ? Color.black : Color.white;
    }

    /**
     * Enum {@code Type}
     * used to represent different kinds of chessmen
     */
    public enum Type {
        king, queen, rook, bishop, knight, pawn
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        this.image = new Image("file:assets/" + color + type + ".png");
    }

    private Type type;

    /**
     * position of chessmen
     *
     * @see game.Position
     */
    private Position position;

    public Position getPosition() {
        return position;
    }

    /**
     * picture of a chessman
     */
    private Image image;

    public Image getImage() {
        return image;
    }

    private Color color;

    public Color getColor() {
        return color;
    }

    /**
     * To obtain a representative letter,
     * when recording it will be used
     *
     * @return representative letter
     */
    public String getLetter() {
        String letter = "";
        switch (type) {
            case king:
                letter = "K";
                break;
            case queen:
                letter = "Q";
                break;
            case bishop:
                letter = "B";
                break;
            case knight:
                letter = "N";
                break;
            case rook:
                letter = "R";
                break;
            case pawn:
                letter = "P";
                break;
        }
        letter += color == Color.white ? "W" : "B";
        return letter;
    }

    /**
     * Whether or not a chessman has been moved
     */
    private boolean moved;

    public boolean isMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    public Piece(Position position, Color color, Type type) {
        this.type = type;
        this.position = position;
        this.color = color;
        this.image = new Image("file:assets/" + color + type + ".png");
        moved = false;
    }

    @Override
    public String toString() {
        return "Piece{" +
                "color=" + color +
                ", position=" + position +
                '}';
    }

    /**
     * Move a piece to a certain position
     *
     * @param position the target position
     */
    public void moveTo(Position position) {
        this.position = position;
        moved = true;
    }
}

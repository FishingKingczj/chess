package game;

/**
 * Class {@code Position}
 * used to represent the position of the board on the board
 */

public class Position {
    public int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "" + x + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Position position = (Position) obj;

        if (x != position.x) return false;
        return y == position.y;
    }

    /**
     * Two auxiliary functions,
     * displace in the original position to get a new position
     *
     * @param d Displacement lattice number
     * @return new position
     */
    public Position changeX(int d) {
        return new Position(x + d, y);
    }

    public Position changeY(int d) {
        return new Position(x, y + d);
    }

    /**
     * Transforming Position into a standard representation method,
     * like 'A1'
     *
     * @return string
     */
    public String format() {
        String x = Character.toString((char) (this.x + 65));
        return x + (8 - y);
    }
}

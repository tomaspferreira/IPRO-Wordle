/**
 * Centralized JavaFX CSS style constants and helpers for the game UI.
 * <p>
 * This is a utility class and must not be instantiated.
 */
public final class GameStyles {

    /**
     * Background color for an empty tile.
     */
    public static final String TILE_EMPTY_BG = "#2A2B2E";

    /**
     * Default border color for tiles.
     */
    public static final String BORDER_COLOR = "#3A3A3C";

    /**
     * Tile color for a correct letter in the correct position.
     */
    public static final String GREEN = "#4CAF50";

    /**
     * Tile color for a correct letter in the wrong position.
     */
    public static final String YELLOW = "#C9B458";

    /**
     * Tile color for an incorrect letter.
     */
    public static final String GREY = "#787C7E";

    /**
     * Tile color used for the "blue" state (game-specific).
     */
    public static final String BLUE = "#3B82F6";

    /**
     * Primary text color.
     */
    public static final String TEXT_PRIMARY = "#FFFFFF";

    /**
     * Secondary text color.
     */
    public static final String TEXT_SECONDARY = "#C9C9C9";

    /**
     * Error message text color.
     */
    public static final String TEXT_ERROR = "#FF6B6B";

    /**
     * Success message text color.
     */
    public static final String TEXT_SUCCESS = "#6BCB77";

    /**
     * CSS style for the main title label.
     */
    public static final String TITLE =
            "-fx-font-size: 48px; "
                    + "-fx-font-weight: bold; "
                    + "-fx-text-fill: " + TEXT_PRIMARY + ";";

    /**
     * CSS style for informational text labels.
     */
    public static final String INFO =
            "-fx-font-size: 16px; "
                    + "-fx-text-fill: " + TEXT_SECONDARY + ";";

    /**
     * CSS style for error messages (red).
     */
    public static final String MSG_RED =
            "-fx-font-size: 16px; "
                    + "-fx-text-fill: " + TEXT_ERROR + ";";

    /**
     * CSS style for success messages (green).
     */
    public static final String MSG_GREEN =
            "-fx-font-size: 16px; "
                    + "-fx-text-fill: " + TEXT_SUCCESS + ";";

    /**
     * Default pixel size for each tile (width/height).
     */
    public static final int TILE_SIZE = 55;

    private GameStyles() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Base tile CSS shared by all tiles (border, radius, font, alignment).
     *
     * @return CSS string for the base tile style
     */
    public static String tileBase() {
        return "-fx-border-color: " + BORDER_COLOR + ";"
                + "-fx-border-width: 2;"
                + "-fx-background-radius: 6;"
                + "-fx-border-radius: 6;"
                + "-fx-alignment: center;"
                + "-fx-font-size: 22px;"
                + "-fx-font-weight: bold;";
    }

    /**
     * CSS for an empty tile.
     *
     * @return CSS string for empty tile style
     */
    public static String tileEmpty() {
        return "-fx-background-color: " + TILE_EMPTY_BG + ";"
                + "-fx-text-fill: " + TEXT_PRIMARY + ";";
    }

    /**
     * CSS for a grey tile.
     *
     * @return CSS string for grey tile style
     */
    public static String tileGrey() {
        return "-fx-background-color: " + GREY + ";"
                + "-fx-text-fill: white;";
    }

    /**
     * CSS for a yellow tile.
     *
     * @return CSS string for yellow tile style
     */
    public static String tileYellow() {
        return "-fx-background-color: " + YELLOW + ";"
                + "-fx-text-fill: white;";
    }

    /**
     * CSS for a green tile.
     *
     * @return CSS string for green tile style
     */
    public static String tileGreen() {
        return "-fx-background-color: " + GREEN + ";"
                + "-fx-text-fill: white;";
    }

    /**
     * CSS for a blue tile.
     *
     * @return CSS string for blue tile style
     */
    public static String tileBlue() {
        return "-fx-background-color: " + BLUE + ";"
                + "-fx-text-fill: white;";
    }

    /**
     * Base CSS for keyboard keys.
     *
     * @return CSS string for base key style
     */
    public static String keyBase() {
        return "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-color: #3A3A3C;"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 6;";
    }

    /**
     * CSS for a grey keyboard key.
     *
     * @return CSS string for grey key style
     */
    public static String keyGrey() {
        return keyColored(GREY);
    }

    /**
     * CSS for a yellow keyboard key.
     *
     * @return CSS string for yellow key style
     */
    public static String keyYellow() {
        return keyColored(YELLOW);
    }

    /**
     * CSS for a green keyboard key.
     *
     * @return CSS string for green key style
     */
    public static String keyGreen() {
        return keyColored(GREEN);
    }

    /**
     * CSS for a blue keyboard key.
     *
     * @return CSS string for blue key style
     */
    public static String keyBlue() {
        return keyColored(BLUE);
    }

    private static String keyColored(String color) {
        return "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 6;";
    }
}

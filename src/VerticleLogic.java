public class VerticleLogic {

    public enum Tile { GREY, YELLOW, GREEN }

    public static class TurnResult {
        public final String guess;          // uppercase
        public final Tile[] tiles;          // length = letters (one tile per row)
        public final int tryIndex;          // which column was filled
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess, Tile[] tiles, int tryIndex, int remainingGuesses, boolean gameWon, boolean gameOver) {
            this.guess = guess;
            this.tiles = tiles;
            this.tryIndex = tryIndex;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int letters;
    private final Language lang;

    private final String word;     // uppercase
    private final int chances;     // = letters
    private int tries = 0;
    private boolean solved = false;

    public VerticleLogic(int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);
        if (list == null || list.length == 0) {
            throw new IllegalArgumentException("No words for length " + letters);
        }

        int random = (int) (Math.random() * list.length);
        this.word = list[random].toUpperCase();
        this.chances = letters;
    }

    public int getLetters() { return letters; }
    public int getChances() { return chances; }
    public int getTries() { return tries; }
    public String getWord() { return word; }

    public boolean isGameWon() { return solved; }
    public boolean isGameOver() { return solved || tries >= chances; }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) empty[i] = Tile.GREY;
            return new TurnResult("", empty, tries, 0, isGameWon(), true);
        }

        String guess = guessRaw.trim().toUpperCase();

        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be " + letters + " letters.");
        }

        int thisTry = tries; // column index we are about to fill
        tries++;

        // If guessed full word -> whole column GREEN
        if (guess.equals(word)) {
            solved = true;
            Tile[] allGreen = new Tile[letters];
            for (int r = 0; r < letters; r++) allGreen[r] = Tile.GREEN;

            return new TurnResult(
                    guess,
                    allGreen,
                    thisTry,
                    Math.max(0, chances - tries),
                    true,
                    true
            );
        }

        // Target letter for THIS column
        char target = word.charAt(thisTry);

        // Duplicate-safe consuming
        char[] remaining = word.toCharArray();
        int[] status = new int[letters]; // 0 grey, 1 yellow, 2 green

        // PASS 1: exactly ONE GREEN if possible (letter equals target)
        boolean greenUsed = false;
        for (int r = 0; r < letters; r++) {
            char c = guess.charAt(r);
            if (!greenUsed && c == target) {
                // consume ONE occurrence of that char from remaining
                for (int j = 0; j < letters; j++) {
                    if (remaining[j] == c) {
                        remaining[j] = 0;
                        status[r] = 2;
                        greenUsed = true;
                        break;
                    }
                }
            }
        }

        // PASS 2: YELLOWS from remaining
        for (int r = 0; r < letters; r++) {
            if (status[r] == 2) continue;
            char c = guess.charAt(r);

            boolean found = false;
            for (int j = 0; j < letters; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0;
                    found = true;
                    break;
                }
            }

            status[r] = found ? 1 : 0;
        }

        Tile[] out = new Tile[letters];
        for (int r = 0; r < letters; r++) {
            out[r] = (status[r] == 2) ? Tile.GREEN : (status[r] == 1 ? Tile.YELLOW : Tile.GREY);
        }

        boolean over = isGameOver();
        return new TurnResult(
                guess,
                out,
                thisTry,
                Math.max(0, chances - tries),
                false,
                over
        );
    }
}

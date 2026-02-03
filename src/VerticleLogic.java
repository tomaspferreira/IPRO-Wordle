public class VerticleLogic {

    // UI colors for Verticle
    public enum Tile {
        GREY, YELLOW, GREEN
    }

    // Result returned after submitting a guess
    public static class TurnResult {
        public final String guess;      // uppercase guess
        public final Tile[] tiles;      // length = letters (top->bottom)
        public final int colIndex;      // which column was just filled
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess, Tile[] tiles, int colIndex, int remainingGuesses, boolean gameWon, boolean gameOver) {
            this.guess = guess;
            this.tiles = tiles;
            this.colIndex = colIndex;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int letters;
    private final Language lang;

    private final String word;  // uppercase
    private final int chances;  // = letters
    private int tries;          // number of valid guesses used
    private boolean solved;

    public VerticleLogic(int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);
        if (list == null || list.length == 0) {
            throw new IllegalArgumentException("Not enough words for length " + letters);
        }

        int random = (int) (Math.random() * list.length);
        this.word = list[random].toUpperCase();

        this.chances = letters; // IMPORTANT: one column per try
        this.tries = 0;
        this.solved = false;
    }

    public int getLetters() {
        return letters;
    }

    public int getChances() {
        return chances;
    }

    public int getTries() {
        return tries;
    }

    public String getWord() {
        return word;
    }

    public boolean isGameWon() {
        return solved;
    }

    public boolean isGameOver() {
        return solved || tries >= chances;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) empty[i] = Tile.GREY;
            return new TurnResult("", empty, Math.max(0, tries - 1), 0, solved, true);
        }

        String guess = guessRaw.trim().toUpperCase();

        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be exactly " + letters + " letters.");
        }

        // column we are filling now (same as tries in console before increment)
        int colIndex = tries;

        // If guessed the full word correctly, win: store all green in this column
        if (guess.equals(word)) {
            solved = true;
            tries++; // consume the try
            Tile[] greens = new Tile[letters];
            for (int i = 0; i < letters; i++) greens[i] = Tile.GREEN;

            return new TurnResult(
                    guess,
                    greens,
                    colIndex,
                    Math.max(0, chances - tries),
                    true,
                    true
            );
        }

        // Target letter for THIS column (exactly like your console)
        char target = word.charAt(colIndex);

        // Make mutable copy of solution word to "consume" letters
        char[] remaining = word.toCharArray();

        // Status for each row: GREY/YELLOW/GREEN
        Tile[] out = new Tile[letters];
        for (int i = 0; i < letters; i++) out[i] = Tile.GREY;

        // PASS 1: mark EXACTLY ONE GREEN for this column (if possible)
        boolean greenUsed = false;

        for (int r = 0; r < letters; r++) {
            char c = guess.charAt(r);

            if (!greenUsed && c == target) {
                // consume ONE occurrence of target from remaining (must exist)
                for (int j = 0; j < letters; j++) {
                    if (remaining[j] == c) {
                        remaining[j] = 0;     // consume
                        out[r] = Tile.GREEN;  // green
                        greenUsed = true;     // only one green allowed
                        break;
                    }
                }
            }
        }

        // PASS 2: mark YELLOWS using remaining letters (duplicate-safe)
        for (int r = 0; r < letters; r++) {
            if (out[r] == Tile.GREEN) {
                continue;
            }

            char c = guess.charAt(r);
            boolean found = false;

            for (int j = 0; j < letters; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0;
                    found = true;
                    break;
                }
            }

            out[r] = found ? Tile.YELLOW : Tile.GREY;
        }

        // consume try AFTER scoring (same effect as your console)
        tries++;

        boolean over = tries >= chances;
        return new TurnResult(
                guess,
                out,
                colIndex,
                Math.max(0, chances - tries),
                false,
                over
        );
    }
}

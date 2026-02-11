public class VerticleLogic {

    public enum Tile {
        /** Incorrect letter. */
        GREY,
        /** Letter exists in the word but not in this position. */
        YELLOW,
        /** Correct letter in the correct position. */
        GREEN
    }

    public static class TurnResult {

        /** Submitted guess (uppercase). */
        private final String guess;

        /** Tile feedback for each row. */
        private final Tile[] tiles;

        /** Index of the filled column (try index). */
        private final int tryIndex;

        /** Remaining guesses after this turn. */
        private final int remainingGuesses;

        /** Whether the game was won on this turn. */
        private final boolean gameWon;

        /** Whether the game is over after this turn. */
        private final boolean gameOver;

        TurnResult(
                String guessValue,
                Tile[] tilesValue,
                int tryIndexValue,
                int remainingGuessesValue,
                boolean gameWonValue,
                boolean gameOverValue
        ) {
            this.guess = guessValue;
            this.tiles = tilesValue;
            this.tryIndex = tryIndexValue;
            this.remainingGuesses = remainingGuessesValue;
            this.gameWon = gameWonValue;
            this.gameOver = gameOverValue;
        }

        public String getGuess() {
            return guess;
        }

        public Tile[] getTiles() {
            return tiles;
        }

        public int getTryIndex() {
            return tryIndex;
        }

        public int getRemainingGuesses() {
            return remainingGuesses;
        }

        public boolean isGameWon() {
            return gameWon;
        }

        public boolean isGameOver() {
            return gameOver;
        }
    }
    // Deterministic constructor for unit tests (no Language, no randomness).
    VerticleLogic(String fixedWord) {
        if (fixedWord == null) {
            throw new IllegalArgumentException("Word must not be null.");
        }
        String w = fixedWord.trim();
        if (w.isEmpty()) {
            throw new IllegalArgumentException("Word must not be empty.");
        }

        this.letters = w.length();
        this.lang = null; // not used in this constructor
        this.word = w.toUpperCase();
        this.chances = letters;
    }


    /** Number of letters in the secret word. */
    private final int letters;

    /** Language provider used to fetch word lists. */
    private final Language lang;

    /** Secret word (uppercase). */
    private final String word;

    /** Total number of chances (equals {@link #letters}). */
    private final int chances;

    /** Number of tries used so far. */
    private int tries = 0;

    /** Whether the game has been solved. */
    private boolean solved = false;

    public VerticleLogic(int lettersValue, Language langValue) {
        this.letters = lettersValue;
        this.lang = langValue;

        String[] list = lang.getWordList(letters);
        if (list == null || list.length == 0) {
            throw new IllegalArgumentException("No words for length " + letters);
        }

        int random = (int) (Math.random() * list.length);
        this.word = list[random].toUpperCase();
        this.chances = letters;
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

    /**
     * Submits one guess for Verticle.
     * Verticle twist:
     * - On try #0 you are trying to match the secret word's letter at position 0,
     * - on try #1 you are trying to match the letter at position 1,
     * - etc.
     * So each column/try has ONE special "target letter" = word.charAt(thisTry).
     * This method returns a Tile per row (per character in the guess).
     */
    public TurnResult submitGuess(String guessRaw) {

        // ----- If the game already ended, return a safe "empty" result -----
        if (isGameOver()) {
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) {
                empty[i] = Tile.GREY;
            }
            return new TurnResult("", empty, tries, 0, isGameWon(), true);
        }

        // ----- Normalize input (trim + uppercase) -----
        String guess = guessRaw.trim().toUpperCase();

        // ----- Validate guess length -----
        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be " + letters + " letters.");
        }

        // ----- Store which try/column this guess belongs to, then consume it -----
        int thisTry = tries;
        tries++;

        // ----- Exact match ends the game immediately (all green) -----
        if (guess.equals(word)) {
            solved = true;

            Tile[] allGreen = new Tile[letters];
            for (int r = 0; r < letters; r++) {
                allGreen[r] = Tile.GREEN;
            }

            return new TurnResult(
                    guess,
                    allGreen,
                    thisTry,
                    Math.max(0, chances - tries),
                    true,
                    true
            );
        }

        // ----- Verticle rule: only ONE position (based on try index) is "the target" -----
        // Example: if thisTry==2, you're targeting word.charAt(2).
        char target = word.charAt(thisTry);

        // ----- Prepare remaining letters pool for "yellow" matching (duplicate-safe) -----
        char[] remaining = word.toCharArray();

        // status: 2=green, 1=yellow, 0=grey for each row/character in the guess
        int[] status = new int[letters];

        // ----- Pass 1 (green): mark exactly ONE guess letter as green if it matches target -----
        // The green is special: it's NOT "same position", it's "matches the target letter for this try".
        // greenUsed ensures we only award one green even if the guess contains the target letter multiple times.
        boolean greenUsed = false;
        for (int r = 0; r < letters; r++) {
            char c = guess.charAt(r);

            // Candidate for green: guess contains the target letter anywhere
            if (!greenUsed && c == target) {

                // Only mark it green if that letter still exists in remaining pool
                // (helps with duplicate letters)
                for (int j = 0; j < letters; j++) {
                    if (remaining[j] == c) {
                        remaining[j] = 0;   // consume that letter
                        status[r] = 2;      // green
                        greenUsed = true;
                        break;
                    }
                }
            }
        }

        // ----- Pass 2 (yellow/grey): mark remaining letters using the remaining pool -----
        for (int r = 0; r < letters; r++) {

            // Skip the one cell already marked green
            if (status[r] == 2) {
                continue;
            }

            char c = guess.charAt(r);

            // Check if this letter exists somewhere unused in the secret word
            boolean found = false;
            for (int j = 0; j < letters; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0; // consume that letter
                    found = true;
                    break;
                }
            }

            // Yellow if found, otherwise grey
            if (found) {
                status[r] = 1;
            } else {
                status[r] = 0;
            }
        }

        // ----- Convert int status codes to Tile enums -----
        Tile[] out = new Tile[letters];
        for (int r = 0; r < letters; r++) {
            if (status[r] == 2) {
                out[r] = Tile.GREEN;
            } else if (status[r] == 1) {
                out[r] = Tile.YELLOW;
            } else {
                out[r] = Tile.GREY;
            }
        }

        // ----- Return final result snapshot for this turn -----
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

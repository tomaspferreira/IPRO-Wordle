public class XordleLogic {

    /**
     * Tile color for Xordle.
     */
    public enum Tile {

        /** Letter not present in either word. */
        GREY,

        /** Letter present somewhere (yellow in at least one word). */
        YELLOW,

        /** Letter green in exactly one word at this position. */
        GREEN,

        /** Letter green in both words at this position. */
        BLUE
    }

    /**
     * Result object returned after submitting a guess.
     */
    public static class TurnResult {

        /** The submitted guess (uppercase). */
        private final String guess;

        /** Per-position tiles for this guess. */
        private final Tile[] tiles;

        /** Current solved state after this guess. */
        private final boolean[] solved;

        /** True only for words that became solved on THIS guess. */
        private final boolean[] newlySolved;

        /** Remaining guesses after this turn. */
        private final int remainingGuesses;

        /** True if both words are solved. */
        private final boolean gameWon;

        /** True if game is finished (won or out of guesses). */
        private final boolean gameOver;

        TurnResult(
                String guessValue,
                Tile[] tilesValue,
                boolean[] solvedValue,
                boolean[] newlySolvedValue,
                int remainingGuessesValue,
                boolean gameWonValue,
                boolean gameOverValue
        ) {
            this.guess = guessValue;
            this.tiles = tilesValue.clone();
            this.solved = solvedValue.clone();
            this.newlySolved = newlySolvedValue.clone();
            this.remainingGuesses = remainingGuessesValue;
            this.gameWon = gameWonValue;
            this.gameOver = gameOverValue;
        }

        public String getGuess() {
            return guess;
        }

        public Tile[] getTiles() {
            return tiles.clone();
        }

        public boolean[] getSolved() {
            return solved.clone();
        }

        public boolean[] getNewlySolved() {
            return newlySolved.clone();
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
    XordleLogic(String word1, String word2) {
        if (word1 == null || word2 == null) {
            throw new IllegalArgumentException("Words must not be null.");
        }
        if (word1.length() != word2.length()) {
            throw new IllegalArgumentException("Words must have same length.");
        }

        this.letters = word1.length();
        this.words = new String[]{word1.toUpperCase(), word2.toUpperCase()};
        this.solved = new boolean[]{false, false};
        this.chances = letters + 4;
        this.tries = 0;
    }


    /** Number of letters per word. */
    private final int letters;

    /** The two secret target words (uppercase). */
    private final String[] words;

    /** Solved flags for the two words. */
    private final boolean[] solved;

    /** Maximum number of guesses. */
    private final int chances;

    /** Number of guesses used so far. */
    private int tries;

    public XordleLogic(int lettersValue, Language langValue) {
        this.letters = lettersValue;

        String[] list = langValue.getWordList(letters);
        if (list == null || list.length < 2) {
            throw new IllegalArgumentException("Not enough words for length " + letters);
        }

        boolean[] used = new boolean[list.length];
        this.words = new String[2];
        this.solved = new boolean[2];

        for (int i = 0; i < 2; i++) {
            int r;
            do {
                r = (int) (Math.random() * list.length);
            } while (used[r]);

            used[r] = true;
            words[i] = list[r].toUpperCase();
            solved[i] = false;
        }

        this.chances = letters + 4;
        this.tries = 0;
    }


    public String[] getWords() {
        return new String[]{words[0], words[1]};
    }

    public int getLetters() {
        return letters;
    }

    public int getChances() {
        return chances;
    }

    public boolean[] getSolved() {
        return new boolean[]{solved[0], solved[1]};
    }

    public boolean isGameWon() {
        return solved[0] && solved[1];
    }

    public boolean isGameOver() {
        return isGameWon() || tries >= chances;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) {
                empty[i] = Tile.GREY;
            }

            boolean[] solvedNow = getSolved();
            boolean[] newly = new boolean[2];

            return new TurnResult(
                    "",
                    empty,
                    solvedNow,
                    newly,
                    0,
                    isGameWon(),
                    true
            );
        }

        String guess = guessRaw.trim().toUpperCase();
        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be " + letters + " letters.");
        }

        boolean[] wasSolved = getSolved();

        tries++;

        for (int w = 0; w < 2; w++) {
            if (!solved[w] && guess.equals(words[w])) {
                solved[w] = true;
            }
        }

        boolean[] nowSolved = getSolved();
        boolean[] newlySolved = new boolean[2];
        newlySolved[0] = !wasSolved[0] && nowSolved[0];
        newlySolved[1] = !wasSolved[1] && nowSolved[1];

        int[] s1 = scoreWordle(words[0], guess);
        int[] s2 = scoreWordle(words[1], guess);

        Tile[] out = new Tile[letters];
        for (int i = 0; i < letters; i++) {
            boolean bothGreen = (s1[i] == 2 && s2[i] == 2);
            boolean oneGreen = (s1[i] == 2) ^ (s2[i] == 2);
            boolean anyYellow = (s1[i] == 1 || s2[i] == 1);

            if (bothGreen) {
                out[i] = Tile.BLUE;
            } else if (oneGreen) {
                out[i] = Tile.GREEN;
            } else if (anyYellow) {
                out[i] = Tile.YELLOW;
            } else {
                out[i] = Tile.GREY;
            }
        }

        return new TurnResult(
                guess,
                out,
                nowSolved,
                newlySolved,
                Math.max(0, chances - tries),
                isGameWon(),
                isGameOver()
        );
    }

    private static int[] scoreWordle(String word, String guess) {
        int n = word.length();
        int[] status = new int[n];
        char[] remaining = word.toCharArray();
        boolean[] green = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (guess.charAt(i) == word.charAt(i)) {
                status[i] = 2;
                green[i] = true;
                remaining[i] = 0;
            }
        }

        for (int i = 0; i < n; i++) {
            if (green[i]) {
                continue;
            }
            char c = guess.charAt(i);
            for (int j = 0; j < n; j++) {
                if (remaining[j] == c) {
                    status[i] = 1;
                    remaining[j] = 0;
                    break;
                }
            }
        }

        return status;
    }
}

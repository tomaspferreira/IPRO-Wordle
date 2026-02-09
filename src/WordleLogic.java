public class WordleLogic {

    public enum Tile {
        /** Incorrect letter. */
        GREY,
        /** Letter exists in the word but is in the wrong position. */
        YELLOW,
        /** Correct letter in the correct position. */
        GREEN
    }

    public static class TurnResult {

        /** Submitted guess (uppercase). */
        private final String guess;

        /** Tile feedback for each secret word [wordIndex][letterIndex]. */
        private final Tile[][] tilesByWord;

        /** Which secret words are solved after this turn. */
        private final boolean[] solved;

        /** Remaining guesses after this turn. */
        private final int remainingGuesses;

        /** Whether the game is won after this turn. */
        private final boolean gameWon;

        /** Whether the game is over after this turn. */
        private final boolean gameOver;

        TurnResult(
                String guessValue,
                Tile[][] tilesByWordValue,
                boolean[] solvedValue,
                int remainingGuessesValue,
                boolean gameWonValue,
                boolean gameOverValue
        ) {
            this.guess = guessValue;
            this.tilesByWord = tilesByWordValue;
            this.solved = solvedValue;
            this.remainingGuesses = remainingGuessesValue;
            this.gameWon = gameWonValue;
            this.gameOver = gameOverValue;
        }

        public String getGuess() {
            return guess;
        }

        public Tile[][] getTilesByWord() {
            return tilesByWord;
        }

        public boolean[] getSolved() {
            return solved;
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

    /** Number of secret words to solve. */
    private final int wordsCount;

    /** Number of letters per word. */
    private final int letters;

    /** Secret words (uppercase). */
    private final String[] words;

    /** Which secret words are solved. */
    private final boolean[] solved;

    /** Total number of allowed guesses. */
    private final int chances;

    /** Number of tries already used. */
    private int tries;

    public WordleLogic(int wordsCountValue, int lettersValue, Language langValue) {
        this.wordsCount = wordsCountValue;
        this.letters = lettersValue;

        String[] list = langValue.getWordList(letters);
        if (list.length < wordsCount) {
            throw new IllegalArgumentException("Not enough words for length " + letters);
        }

        this.words = new String[wordsCount];
        this.solved = new boolean[wordsCount];

        boolean[] used = new boolean[list.length];
        for (int i = 0; i < wordsCount; i++) {
            int r;
            do {
                r = (int) (Math.random() * list.length);
            } while (used[r]);

            used[r] = true;
            words[i] = list[r].toUpperCase();
            solved[i] = false;
        }

        this.chances = wordsCount + 4;
        this.tries = 0;
    }

    public int getChances() {
        return chances;
    }

    public int getLetters() {
        return letters;
    }

    public String[] getWords() {
        String[] out = new String[words.length];
        System.arraycopy(words, 0, out, 0, words.length);
        return out;
    }

    public boolean[] getSolved() {
        boolean[] out = new boolean[solved.length];
        System.arraycopy(solved, 0, out, 0, solved.length);
        return out;
    }

    public boolean isGameWon() {
        for (boolean b : solved) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean isGameOver() {
        return isGameWon() || tries >= chances;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[][] empty = new Tile[wordsCount][letters];
            for (int w = 0; w < wordsCount; w++) {
                for (int i = 0; i < letters; i++) {
                    empty[w][i] = Tile.GREY;
                }
            }
            return new TurnResult("", empty, getSolved(), 0, isGameWon(), true);
        }

        String guess = guessRaw.trim().toUpperCase();
        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be exactly " + letters + " letters.");
        }

        tries++;

        Tile[][] tilesByWord = new Tile[wordsCount][letters];

        for (int w = 0; w < wordsCount; w++) {
            if (!solved[w] && guess.equals(words[w])) {
                solved[w] = true;
            }

            int[] status = scoreWordle(words[w], guess);

            for (int i = 0; i < letters; i++) {
                if (status[i] == 2) {
                    tilesByWord[w][i] = Tile.GREEN;
                } else if (status[i] == 1) {
                    tilesByWord[w][i] = Tile.YELLOW;
                } else {
                    tilesByWord[w][i] = Tile.GREY;
                }
            }

            if (solved[w]) {
                for (int i = 0; i < letters; i++) {
                    tilesByWord[w][i] = Tile.GREEN;
                }
            }
        }

        boolean won = isGameWon();
        boolean over = isGameOver();

        return new TurnResult(
                guess,
                tilesByWord,
                getSolved(),
                Math.max(0, chances - tries),
                won,
                over
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

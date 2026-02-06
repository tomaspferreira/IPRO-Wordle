public class WordleLogic {

    public enum Tile { GREY, YELLOW, GREEN }

    public static class TurnResult {
        public final String guess;
        public final Tile[][] tilesByWord; // [word][letters]
        public final boolean[] solved;
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess, Tile[][] tilesByWord, boolean[] solved,
                   int remainingGuesses, boolean gameWon, boolean gameOver) {
            this.guess = guess;
            this.tilesByWord = tilesByWord;
            this.solved = solved;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int wordsCount;
    private final int letters;
    private final Language lang;

    private final String[] words;
    private final boolean[] solved;

    private final int chances;
    private int tries;

    public WordleLogic(int wordsCount, int letters, Language lang) {
        this.wordsCount = wordsCount;
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);
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

    public int getChances() { return chances; }
    public int getTries() { return tries; }
    public int getLetters() { return letters; }
    public int getWordsCount() { return wordsCount; }

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
        for (boolean b : solved) if (!b) return false;
        return true;
    }

    public boolean isGameOver() {
        return isGameWon() || tries >= chances;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[][] empty = new Tile[wordsCount][letters];
            for (int w = 0; w < wordsCount; w++) {
                for (int i = 0; i < letters; i++) empty[w][i] = Tile.GREY;
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

            // score, even if solved this turn (so UI can paint)
            int[] status = scoreWordle(words[w], guess);

            for (int i = 0; i < letters; i++) {
                if (status[i] == 2) tilesByWord[w][i] = Tile.GREEN;
                else if (status[i] == 1) tilesByWord[w][i] = Tile.YELLOW;
                else tilesByWord[w][i] = Tile.GREY;
            }

            // if solved after this turn -> force all green for that row
            if (solved[w]) {
                for (int i = 0; i < letters; i++) tilesByWord[w][i] = Tile.GREEN;
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

    // duplicate-safe
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
            if (green[i]) continue;
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

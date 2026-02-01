public class XordleLogic {

    // 4 UI colors we need for Xordle
    public enum Tile {
        GREY, YELLOW, GREEN, BLUE
    }

    // Result returned after submitting a guess
    public static class TurnResult {
        public final String guess;      // uppercase guess
        public final Tile[] tiles;      // length = letters
        public final boolean[] solved;  // length = 2
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess,
                   Tile[] tiles,
                   boolean[] solved,
                   int remainingGuesses,
                   boolean gameWon,
                   boolean gameOver) {

            this.guess = guess;
            this.tiles = tiles;
            this.solved = solved;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int letters;
    private final Language lang;

    private final String[] words;     // 2 mystery words (uppercase)
    private final boolean[] solved;   // solved flags
    private final int chances;        // e.g. letters + 4
    private int tries;                // number of valid guesses used

    public XordleLogic(int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);
        if (list.length < 2) {
            throw new IllegalArgumentException("Not enough words for length " + letters);
        }

        // Pick 2 distinct random words
        boolean[] used = new boolean[list.length];
        words = new String[2];
        solved = new boolean[2];

        for (int i = 0; i < 2; i++) {
            int random;
            do {
                random = (int) (Math.random() * list.length);
            } while (used[random]);

            used[random] = true;
            words[i] = list[random].toUpperCase();
            solved[i] = false;
        }

        chances = letters + 4;
        tries = 0;
    }

    public String[] getWords(){
        return words;
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

    public boolean[] getSolved() {
        // return a copy so UI can't mutate internal state
        return new boolean[]{solved[0], solved[1]};
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            // If game already ended, just return a "no-op" result
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) empty[i] = Tile.GREY;
            return new TurnResult("", empty, getSolved(), 0, isGameWon(), true);
        }

        String guess = guessRaw.trim().toUpperCase();

        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be " + letters + " letters.");
        }

        // consume a try (only after length is valid)
        tries++;

        // update solved flags if exact match
        for (int w = 0; w < 2; w++) {
            if (!solved[w] && guess.equals(words[w])) {
                solved[w] = true;
            }
        }

        // score guess against each word like Wordle: 0 grey, 1 yellow, 2 green
        int[] s1 = scoreWordle(words[0], guess);
        int[] s2 = scoreWordle(words[1], guess);

        // combine into Xordle tiles
        Tile[] out = new Tile[letters];

        for (int i = 0; i < letters; i++) {
            boolean bothGreen = (s1[i] == 2 && s2[i] == 2);
            boolean oneGreen =
                    (s1[i] == 2 && s2[i] != 2) ||
                            (s1[i] != 2 && s2[i] == 2);

            boolean anyYellow = (s1[i] == 1 || s2[i] == 1);

            if (bothGreen) out[i] = Tile.BLUE;
            else if (oneGreen) out[i] = Tile.GREEN;
            else if (anyYellow) out[i] = Tile.YELLOW;
            else out[i] = Tile.GREY;
        }

        boolean won = isGameWon();
        boolean over = isGameOver();

        return new TurnResult(
                guess,
                out,
                getSolved(),
                Math.max(0, chances - tries),
                won,
                over
        );
    }

    public boolean isGameWon() {
        return solved[0] && solved[1];
    }

    public boolean isGameOver() {
        return isGameWon() || tries >= chances;
    }

    // Duplicate-safe Wordle scoring
    // returns 0 grey, 1 yellow, 2 green
    private static int[] scoreWordle(String word, String guess) {
        int n = word.length();
        int[] status = new int[n];
        char[] remaining = word.toCharArray();
        boolean[] green = new boolean[n];

        // pass 1: greens
        for (int i = 0; i < n; i++) {
            char c = guess.charAt(i);
            if (c == word.charAt(i)) {
                status[i] = 2;
                green[i] = true;
                remaining[i] = 0;
            }
        }

        // pass 2: yellows
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

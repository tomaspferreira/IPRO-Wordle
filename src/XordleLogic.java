public class XordleLogic {

    public enum Tile { GREY, YELLOW, GREEN, BLUE }

    public static class TurnResult {
        public final String guess;
        public final Tile[] tiles;
        public final boolean[] solved;
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess, Tile[] tiles, boolean[] solved,
                   int remainingGuesses, boolean gameWon, boolean gameOver) {
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

    private final String[] words;
    private final boolean[] solved;
    private final int chances;
    private int tries;

    public XordleLogic(int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);
        if (list.length < 2) throw new IllegalArgumentException("Not enough words for length " + letters);

        boolean[] used = new boolean[list.length];
        words = new String[2];
        solved = new boolean[2];

        for (int i = 0; i < 2; i++) {
            int r;
            do { r = (int) (Math.random() * list.length); } while (used[r]);
            used[r] = true;
            words[i] = list[r].toUpperCase();
            solved[i] = false;
        }

        chances = letters + 4;
        tries = 0;
    }

    public String[] getWords() { return new String[]{words[0], words[1]}; }
    public int getLetters() { return letters; }
    public int getChances() { return chances; }
    public int getTries() { return tries; }
    public boolean[] getSolved() { return new boolean[]{solved[0], solved[1]}; }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[letters];
            for (int i = 0; i < letters; i++) empty[i] = Tile.GREY;
            return new TurnResult("", empty, getSolved(), 0, isGameWon(), true);
        }

        String guess = guessRaw.trim().toUpperCase();
        if (guess.length() != letters) throw new IllegalArgumentException("Guess must be " + letters + " letters.");

        tries++;

        for (int w = 0; w < 2; w++) {
            if (!solved[w] && guess.equals(words[w])) solved[w] = true;
        }

        int[] s1 = scoreWordle(words[0], guess);
        int[] s2 = scoreWordle(words[1], guess);

        Tile[] out = new Tile[letters];
        for (int i = 0; i < letters; i++) {
            boolean bothGreen = (s1[i] == 2 && s2[i] == 2);
            boolean oneGreen = (s1[i] == 2) ^ (s2[i] == 2);
            boolean anyYellow = (s1[i] == 1 || s2[i] == 1);

            if (bothGreen) out[i] = Tile.BLUE;
            else if (oneGreen) out[i] = Tile.GREEN;
            else if (anyYellow) out[i] = Tile.YELLOW;
            else out[i] = Tile.GREY;
        }

        return new TurnResult(
                guess, out, getSolved(),
                Math.max(0, chances - tries),
                isGameWon(), isGameOver()
        );
    }

    public boolean isGameWon() { return solved[0] && solved[1]; }
    public boolean isGameOver() { return isGameWon() || tries >= chances; }

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

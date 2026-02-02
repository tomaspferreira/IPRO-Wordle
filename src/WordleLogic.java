public class WordleLogic {

    public enum Tile {
        GREY, YELLOW, GREEN
    }

    public static class TurnResult {
        public final String guess;
        public final Tile[][] tilesByWord;   // [wordIndex][letterIndex]
        public final boolean[] solved;       // [wordIndex]
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess,
                   Tile[][] tilesByWord,
                   boolean[] solved,
                   int remainingGuesses,
                   boolean gameWon,
                   boolean gameOver) {

            this.guess = guess;
            this.tilesByWord = tilesByWord;
            this.solved = solved;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int letters;
    private final int wordsCount;
    private final int chances;

    private final String[] words;      // uppercase
    private final boolean[] solved;
    private int tries;

    public WordleLogic(int wordsCount, int letters, Language lang) {
        this.letters = letters;
        this.wordsCount = wordsCount;

        String[] list = lang.getWordList(letters);
        if (list.length < wordsCount) {
            throw new IllegalArgumentException("Not enough words in list for " + wordsCount + " words.");
        }

        boolean[] used = new boolean[list.length];

        words = new String[wordsCount];
        solved = new boolean[wordsCount];

        for (int i = 0; i < wordsCount; i++) {
            int random;
            do {
                random = (int) (Math.random() * list.length);
            } while (used[random]);

            used[random] = true;
            words[i] = list[random].toUpperCase();
            solved[i] = false;
        }

        // Same rule as your console Wordle
        chances = wordsCount + 5;
        tries = 0;
    }

    public int getLetters() {
        return letters;
    }

    public int getWordsCount() {
        return wordsCount;
    }

    public int getChances() {
        return chances;
    }

    public int getTries() {
        return tries;
    }

    public boolean isGameWon() {
        for (int i = 0; i < solved.length; i++) {
            if (!solved[i]) return false;
        }
        return true;
    }

    public boolean isGameOver() {
        return isGameWon() || tries >= chances;
    }

    public String[] getWords() {
        // return copy
        String[] out = new String[words.length];
        for (int i = 0; i < words.length; i++) out[i] = words[i];
        return out;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[][] empty = new Tile[wordsCount][letters];
            for (int w = 0; w < wordsCount; w++) {
                for (int i = 0; i < letters; i++) empty[w][i] = Tile.GREY;
            }
            return new TurnResult("", empty, getSolvedCopy(), 0, isGameWon(), true);
        }

        String guess = guessRaw.trim().toUpperCase();

        if (guess.length() != letters) {
            throw new IllegalArgumentException("Guess must be " + letters + " letters.");
        }

        tries++;

        // Update solved flags if exact match
        for (int w = 0; w < wordsCount; w++) {
            if (!solved[w] && guess.equals(words[w])) {
                solved[w] = true;
            }
        }

        Tile[][] tilesByWord = new Tile[wordsCount][letters];

        for (int w = 0; w < wordsCount; w++) {
            if (solved[w]) {
                // If already solved, show the row as green (nice UX)
                for (int i = 0; i < letters; i++) tilesByWord[w][i] = Tile.GREEN;
            } else {
                int[] s = scoreWordle(words[w], guess); // 0/1/2
                for (int i = 0; i < letters; i++) {
                    if (s[i] == 2) tilesByWord[w][i] = Tile.GREEN;
                    else if (s[i] == 1) tilesByWord[w][i] = Tile.YELLOW;
                    else tilesByWord[w][i] = Tile.GREY;
                }
            }
        }

        boolean won = isGameWon();
        boolean over = isGameOver();

        return new TurnResult(
                guess,
                tilesByWord,
                getSolvedCopy(),
                Math.max(0, chances - tries),
                won,
                over
        );
    }

    private boolean[] getSolvedCopy() {
        boolean[] out = new boolean[solved.length];
        for (int i = 0; i < solved.length; i++) out[i] = solved[i];
        return out;
    }

    // Duplicate-safe Wordle scoring: 0 grey, 1 yellow, 2 green
    private static int[] scoreWordle(String word, String guess) {
        int n = word.length();
        int[] status = new int[n];
        char[] remaining = word.toCharArray();
        boolean[] green = new boolean[n];

        // greens
        for (int i = 0; i < n; i++) {
            char c = guess.charAt(i);
            if (c == word.charAt(i)) {
                status[i] = 2;
                green[i] = true;
                remaining[i] = 0;
            }
        }

        // yellows
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

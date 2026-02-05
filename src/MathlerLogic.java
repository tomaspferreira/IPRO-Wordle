public class MathlerLogic {

    public enum Tile { GREY, YELLOW, GREEN }

    public static class TurnResult {
        public final String guess;
        public final Tile[] tiles; // length = equationLength
        public final int remainingGuesses;
        public final boolean gameWon;
        public final boolean gameOver;

        TurnResult(String guess, Tile[] tiles, int remainingGuesses, boolean gameWon, boolean gameOver) {
            this.guess = guess;
            this.tiles = tiles;
            this.remainingGuesses = remainingGuesses;
            this.gameWon = gameWon;
            this.gameOver = gameOver;
        }
    }

    private final int numbersCount;      // how many numbers in equation
    private final String equation;       // the hidden equation
    private final int targetResult;      // result to show player
    private final int chances;           // equation.length + 2
    private int tries;

    public MathlerLogic(int numbersCount) {
        this.numbersCount = numbersCount;

        Generated g = generateEquation(numbersCount);
        this.equation = g.equation;
        this.targetResult = g.result;

        this.chances = equation.length() + 2;
        this.tries = 0;
    }

    public int getNumbersCount() { return numbersCount; }
    public String getEquation() { return equation; } // only reveal on lose/give up
    public int getTargetResult() { return targetResult; }
    public int getEquationLength() { return equation.length(); }
    public int getChances() { return chances; }
    public int getTries() { return tries; }

    public boolean isGameWon() { return tries > 0 && lastGuessWasExact; }
    public boolean isGameOver() { return lastGuessWasExact || tries >= chances; }

    private boolean lastGuessWasExact = false;

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[equation.length()];
            for (int i = 0; i < empty.length; i++) empty[i] = Tile.GREY;
            return new TurnResult("", empty, 0, lastGuessWasExact, true);
        }

        String guess = guessRaw.trim();

        if (guess.length() != equation.length()) {
            throw new IllegalArgumentException("Your guess must be " + equation.length() + " characters long.");
        }

        tries++;

        if (guess.equals(equation)) {
            lastGuessWasExact = true;
            Tile[] allGreen = new Tile[equation.length()];
            for (int i = 0; i < allGreen.length; i++) allGreen[i] = Tile.GREEN;
            return new TurnResult(guess, allGreen, Math.max(0, chances - tries), true, true);
        }

        // Wordle-like duplicate-safe scoring for characters
        char[] remaining = equation.toCharArray();
        boolean[] green = new boolean[equation.length()];
        Tile[] out = new Tile[equation.length()];

        // pass 1: greens
        for (int i = 0; i < equation.length(); i++) {
            char c = guess.charAt(i);
            if (c == equation.charAt(i)) {
                green[i] = true;
                remaining[i] = 0;
                out[i] = Tile.GREEN;
            }
        }

        // pass 2: yellows/greys
        for (int i = 0; i < equation.length(); i++) {
            if (green[i]) continue;

            char c = guess.charAt(i);
            boolean found = false;

            for (int j = 0; j < remaining.length; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0;
                    found = true;
                    break;
                }
            }

            out[i] = found ? Tile.YELLOW : Tile.GREY;
        }

        boolean over = tries >= chances;
        return new TurnResult(guess, out, Math.max(0, chances - tries), false, over);
    }

    // ----------------- equation generation (adapted from your console) -----------------

    private static class Generated {
        final int result;
        final String equation;
        Generated(int result, String equation) {
            this.result = result;
            this.equation = equation;
        }
    }

    private static Generated generateEquation(int length) {
        int result;
        String equation;
        int[] numbers = new int[length];
        char[] ops = new char[length - 1];
        char[] operators = {'+', '-', '*', '/'};

        do {
            equation = "";
            numbers[0] = 1 + (int)(Math.random() * 99);

            int running = numbers[0];

            for (int i = 0; i < ops.length; i++) {
                char op = operators[(int)(Math.random() * operators.length)];
                ops[i] = op;

                if (op == '+' || op == '-') {
                    numbers[i + 1] = 1 + (int)(Math.random() * 199);
                    running = numbers[i + 1];

                } else if (op == '*') {
                    int factor = 2 + (int)(Math.random() * 10);
                    numbers[i + 1] = factor;
                    running *= factor;

                } else { // '/'
                    int[] divisors = new int[20];
                    int count = 0;
                    for (int d = 2; d <= 20; d++) {
                        if (running % d == 0) divisors[count++] = d;
                    }
                    int divisor = (count == 0) ? 1 : divisors[(int)(Math.random() * count)];
                    numbers[i + 1] = divisor;
                    running /= divisor;
                }
            }

            for (int i = 0; i < numbers.length; i++) {
                equation += numbers[i];
                if (i < ops.length) equation += ops[i];
            }

            // evaluate with precedence
            int[] numbersNew = new int[numbers.length];
            char[] opsNew = new char[ops.length];
            int nrCount = 0;
            int opCount = 0;

            numbersNew[nrCount++] = numbers[0];

            for (int i = 0; i < ops.length; i++) {
                char op = ops[i];
                int right = numbers[i + 1];

                if (op == '*' || op == '/') {
                    int left = numbersNew[nrCount - 1];
                    numbersNew[nrCount - 1] = (op == '*') ? (left * right) : (left / right);
                } else {
                    opsNew[opCount++] = op;
                    numbersNew[nrCount++] = right;
                }
            }

            result = numbersNew[0];
            for (int i = 0; i < opCount; i++) {
                result = (opsNew[i] == '+') ? (result + numbersNew[i + 1]) : (result - numbersNew[i + 1]);
            }

        } while (result < 0);

        return new Generated(result, equation);
    }
}
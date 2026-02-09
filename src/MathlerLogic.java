import java.util.Arrays;


public class MathlerLogic {

    public enum Tile {
        /** Incorrect character. */
        GREY,
        /** Character exists but is in the wrong position. */
        YELLOW,
        /** Correct character in the correct position. */
        GREEN
    }

    public static class TurnResult {

        /** The submitted guess after trimming. */
        private final String guess;

        /** Tile feedback for each position in the guess. */
        private final Tile[] tiles;

        /** Remaining guesses after this turn. */
        private final int remainingGuesses;

        /** Whether the game was won on this turn. */
        private final boolean gameWon;

        /** Whether the game is over after this turn. */
        private final boolean gameOver;

        TurnResult(
                String guessValue,
                Tile[] tilesValue,
                int remainingGuessesValue,
                boolean gameWonValue,
                boolean gameOverValue
        ) {
            this.guess = guessValue;
            this.tiles = tilesValue;
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

    /** Target value of the secret expression. */
    private final int target;

    /** Secret equation/expression as a string. */
    private final String equation;

    /** Total number of allowed attempts. */
    private final int chances;

    /** Number of tries already used. */
    private int tries = 0;

    /** Whether the equation has been solved. */
    private boolean solved = false;

    public MathlerLogic(int numbersCountValue) {

        char[] operators = {'+', '-', '*', '/'};
        int result;
        String eq;

        do {
            int[] numbers = new int[numbersCountValue];
            char[] ops = new char[numbersCountValue - 1];

            numbers[0] = 1 + (int) (Math.random() * 99);

            int running = numbers[0];

            for (int i = 0; i < ops.length; i++) {
                char op = operators[(int) (Math.random() * operators.length)];
                ops[i] = op;

                if (op == '+' || op == '-') {
                    numbers[i + 1] = 1 + (int) (Math.random() * 199);
                    running = numbers[i + 1];
                } else if (op == '*') {
                    int factor = 2 + (int) (Math.random() * 10);
                    numbers[i + 1] = factor;
                    running *= factor;
                } else {
                    int[] divisors = new int[20];
                    int count = 0;

                    for (int d = 2; d <= 20; d++) {
                        if (running % d == 0) {
                            divisors[count++] = d;
                        }
                    }

                    int divisor;
                    if (count == 0) {
                        divisor = 1;
                    } else {
                        divisor = divisors[(int) (Math.random() * count)];
                    }

                    numbers[i + 1] = divisor;
                    running /= divisor;
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numbers.length; i++) {
                sb.append(numbers[i]);
                if (i < ops.length) {
                    sb.append(ops[i]);
                }
            }
            eq = sb.toString();

            int[] numbersNew = new int[numbers.length];
            char[] opsNew = new char[ops.length];

            int nrCount = 0;
            int oCount = 0;

            numbersNew[nrCount++] = numbers[0];

            for (int i = 0; i < ops.length; i++) {
                char op = ops[i];
                int right = numbers[i + 1];

                if (op == '*' || op == '/') {
                    int left = numbersNew[nrCount - 1];
                    if (op == '*') {
                        numbersNew[nrCount - 1] = left * right;
                    } else {
                        numbersNew[nrCount - 1] = left / right;
                    }
                } else {
                    opsNew[oCount++] = op;
                    numbersNew[nrCount++] = right;
                }
            }

            result = numbersNew[0];
            for (int i = 0; i < oCount; i++) {
                if (opsNew[i] == '+') {
                    result += numbersNew[i + 1];
                } else {
                    result -= numbersNew[i + 1];
                }
            }

        } while (result < 0);

        this.target = result;
        this.equation = eq;
        this.chances = equation.length() + 2;
    }

    public int getTarget() {
        return target;
    }

    public String getEquation() {
        return equation;
    }

    public int getChances() {
        return chances;
    }

    public int getEquationLength() {
        return equation.length();
    }

    public boolean isGameWon() {
        return solved;
    }

    public boolean isGameOver() {
        return solved || tries >= chances;
    }

    public TurnResult submitGuess(String guessRaw) {
        if (isGameOver()) {
            Tile[] empty = new Tile[equation.length()];
            Arrays.fill(empty, Tile.GREY);
            return new TurnResult("", empty, 0, isGameWon(), true);
        }

        String guess = guessRaw.trim();

        if (guess.length() != equation.length()) {
            throw new IllegalArgumentException(
                    "Your guess must be " + equation.length() + " characters long."
            );
        }

        for (int i = 0; i < guess.length(); i++) {
            char ch = guess.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9')
                    || ch == '+'
                    || ch == '-'
                    || ch == '*'
                    || ch == '/';
            if (!ok) {
                throw new IllegalArgumentException("Only digits and + - * / allowed.");
            }
        }

        tries++;

        if (guess.equals(equation)) {
            solved = true;

            Tile[] allGreen = new Tile[equation.length()];
            Arrays.fill(allGreen, Tile.GREEN);


            return new TurnResult(
                    guess,
                    allGreen,
                    Math.max(0, chances - tries),
                    true,
                    true
            );
        }

        char[] remaining = equation.toCharArray();
        boolean[] green = new boolean[equation.length()];
        Tile[] out = new Tile[equation.length()];

        for (int i = 0; i < equation.length(); i++) {
            if (guess.charAt(i) == equation.charAt(i)) {
                green[i] = true;
                remaining[i] = 0;
                out[i] = Tile.GREEN;
            }
        }

        for (int i = 0; i < equation.length(); i++) {
            if (green[i]) {
                continue;
            }

            char c = guess.charAt(i);
            boolean found = false;

            for (int j = 0; j < remaining.length; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0;
                    found = true;
                    break;
                }
            }

            if (found) {
                out[i] = Tile.YELLOW;
            } else {
                out[i] = Tile.GREY;
            }
        }

        boolean over = isGameOver();
        return new TurnResult(guess, out, Math.max(0, chances - tries), false, over);
    }
}

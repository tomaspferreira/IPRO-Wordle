public class Mathler {
    private int length;

    Mathler(int lengthInput) {
        this.length = lengthInput;

        int result = 0;
        String equation = "";
        int[] numbers = new int[length];
        char[] eqOps = new char[length - 1];
        char[] operators = {'+', '-', '*', '/'};

        do {
            equation = "";
            numbers[0] = 1 + (int) (Math.random() * 99);

            // "running" keeps track of the current * / segment value
            int running = numbers[0];

            // Generate operators and following numbers
            for (int i = 0; i < eqOps.length; i++) {
                char op = operators[(int) (Math.random() * operators.length)];
                eqOps[i] = op;

                // Case 1: + or - (start a new segment)
                if (op == '+' || op == '-') {
                    numbers[i + 1] = 1 + (int) (Math.random() * 199);
                    running = numbers[i + 1];

                    // Case 2: * (multiply by a small factor)
                } else if (op == '*') {
                    int factor = 2 + (int) (Math.random() * 10);
                    numbers[i + 1] = factor;
                    running *= factor;

                    // Case 3: / (choose a divisor that divides running exactly)
                } else {
                    int[] divisors = new int[20];
                    int count = 0;

                    for (int d = 2; d <= 20; d++) {
                        if (running % d == 0) {
                            divisors[count] = d;
                            count++;
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

            // Build the equation string from numbers and operators
            for (int i = 0; i < numbers.length; i++) {
                equation += numbers[i];
                if (i < eqOps.length) {
                    equation += eqOps[i];
                }
            }

            // Arrays used to evaluate the equation with precedence
            int[] numbers2 = new int[numbers.length];
            char[] ops2 = new char[eqOps.length];

            int nrCount = 0;
            int oCount = 0;

            numbers2[nrCount] = numbers[0];
            nrCount++;

            // First evaluation pass: collapse all * and / operations
            for (int i = 0; i < eqOps.length; i++) {
                char op = eqOps[i];
                int right = numbers[i + 1];

                if (op == '*' || op == '/') {
                    int left = numbers2[nrCount - 1];
                    if (op == '*') {
                        numbers2[nrCount - 1] = left * right;
                    } else {
                        numbers2[nrCount - 1] = left / right;
                    }
                } else {
                    ops2[oCount] = op;
                    oCount++;
                    numbers2[nrCount] = right;
                    nrCount++;
                }
            }

            // Second evaluation pass: only + and - remain
            result = numbers2[0];
            for (int i = 0; i < oCount; i++) {
                if (ops2[i] == '+') {
                    result += numbers2[i + 1];
                } else {
                    result -= numbers2[i + 1];
                }
            }
        } while (result < 0);

        IO.println(equation);
        IO.println(result);

        boolean solved = false;

        int tries = 0;
        int chances = equation.length() + 2;

        IO.println(
                "The result you are trying to get is " + result
                        + " and your equation has " + equation.length()
                        + " characters: "
        );

        for (int i = 0; i < equation.length(); i++) {
            IO.print(ConsoleColors.WHITE_BACKGROUND + " " + ConsoleColors.RESET);
            IO.print(" ");
        }
        IO.println("");

        while (tries < chances) {
            String guess = IO.readln("Guess the equation: ").trim();

            if (guess.length() != equation.length()) {
                IO.println("Your guess must be " + equation.length() + " characters long.");
                continue;
            }

            tries++;

            if (guess.equals(equation)) {
                solved = true;
                IO.println("CORRECT!");
                break;
            }

            // Remaining chars that can still be matched as yellow
            char[] charsInWord = equation.toCharArray();

            // Tracks which positions are green
            boolean[] green = new boolean[equation.length()];

            // First pass: mark GREEN chars
            for (int i = 0; i < equation.length(); i++) {
                char c = guess.charAt(i);
                if (c == equation.charAt(i)) {
                    green[i] = true;
                    charsInWord[i] = 0; // consume this char
                }
            }

            // Second pass: print colors for each char
            for (int i = 0; i < equation.length(); i++) {
                char c = guess.charAt(i);

                if (green[i]) {
                    IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
                } else {
                    boolean found = false;

                    for (int j = 0; j < equation.length(); j++) {
                        if (charsInWord[j] == c) {
                            charsInWord[j] = 0; // consume one occurrence
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        IO.print(ConsoleColors.YELLOW_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else {
                        IO.print(ConsoleColors.BLACK_BACKGROUND + c + ConsoleColors.RESET + " ");
                    }
                }
            }

            IO.println(" Remaining guesses: " + (chances - tries));
        }

        if (solved) {
            IO.println("Congratulations! You solved the equation!");
        } else {
            IO.println("You lost. The equation was:");
            IO.println(equation);
        }
    }

    // Accessor method required by your VisibilityModifier rule
    int getLength() {
        return length;
    }
}

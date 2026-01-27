public class Xordle {
    /**
     * Amount of letter that the word should have
     */
    private final int letters;
    /**
     *
     */
    private final Language lang;

    Xordle(int letterCount, Language language) {
        this.letters = letterCount;
        this.lang = language;

        String[] list = lang.getWordList(letters);

        String[] words = new String[2];
        boolean[] solved = new boolean[2];
        boolean[] used = new boolean[list.length];

        for (int i = 0; i < 2; i++) {
            int random;
            do {
                random = (int) (Math.random() * list.length);
            } while (used[random]);

            used[random] = true;
            words[i] = list[random];
            solved[i] = false;
        }

        int tries = 0;
        int chances = letters + 4;

        // Main game loop
        while (tries < chances) {

            // Check if all words are solved
            boolean allSolved = true;
            for (int i = 0; i < solved.length; i++) {
                if (!solved[i]) {
                    allSolved = false;
                    break;
                }
            }
            if (allSolved) {
                break;
            }

            String guessRaw = IO.readln("Guess the word: ").trim();

            if (guessRaw.length() != letters) {
                IO.println("Your guess must be " + letters + " letters long.");
                continue;
            }

            if (!HunspellChecker.isValidWord(guessRaw)) {
                IO.println("Not a valid word.");
                continue;
            }

            String guess = guessRaw.toUpperCase();
            tries++;

            for (int w = 0; w < 2; w++) {
                if (!solved[w] && guess.equals(words[w])) {
                    solved[w] = true;
                    IO.println("Word " + (w + 1) + " solved!");
                }
            }

            // Print the Xordle hint (combined feedback of both words)
            printXorHintSimple(guess, words[0], words[1]);

            // Show remaining attempts
            IO.println("Remaining guesses: " + (chances - tries));
        }

        // Final check
        boolean allSolved = true;
        for (int i = 0; i < solved.length; i++) {
            if (!solved[i]) {
                allSolved = false;
                break;
            }
        }

        if (allSolved) {
            IO.println("Congratulations! You solved both words!");
        } else {
            IO.println("You lost. The words were:");
            IO.println(words[0]);
            IO.println(words[1]);
        }
    }

    static int[] scoreWordle(String word, String guess) {
        int n = word.length();

        int[] status = new int[n];
        char[] remaining = word.toCharArray();
        boolean[] green = new boolean[n];

        // Greens
        for (int i = 0; i < n; i++) {
            char c = guess.charAt(i);
            if (c == word.charAt(i)) {
                status[i] = 2;
                green[i] = true;
                remaining[i] = 0;
            }
        }

        // Yellows
        for (int i = 0; i < n; i++) {
            if (green[i]) {
                continue;
            }

            char c = guess.charAt(i);
            boolean found = false;

            for (int j = 0; j < n; j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0;
                    found = true;
                    break;
                }
            }

            if (found) {
                status[i] = 1;
            }
        }

        return status;
    }

    static void printXorHintSimple(String guess, String w1, String w2) {
        int[] s1 = scoreWordle(w1, guess);
        int[] s2 = scoreWordle(w2, guess);

        IO.print(" | ");

        for (int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);

            boolean bothGreen = (s1[i] == 2 && s2[i] == 2);
            boolean oneGreen = (s1[i] == 2 && s2[i] != 2)
                    || (s1[i] != 2 && s2[i] == 2);
            boolean anyYellow = (s1[i] == 1 || s2[i] == 1);

            if (bothGreen) {
                IO.print(ConsoleColors.BLUE_BACKGROUND + c + ConsoleColors.RESET + " ");
            } else if (oneGreen) {
                IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
            } else if (anyYellow) {
                IO.print(ConsoleColors.YELLOW_BACKGROUND + c + ConsoleColors.RESET + " ");
            } else {
                IO.print(ConsoleColors.BLACK_BACKGROUND + c + ConsoleColors.RESET + " ");
            }
        }

        IO.print(" | ");
        IO.println("");
    }

    // Accessor methods (required by VisibilityModifier rule)
    int getLetters() {
        return letters;
    }

    Language getLang() {
        return lang;
    }
}

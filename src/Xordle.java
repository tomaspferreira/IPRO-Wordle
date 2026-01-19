public class Xordle {

    int letters;

    Language lang;

    Xordle(int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;

        String[] list = lang.getWordList(letters);

        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].toUpperCase();
        }

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
            if (allSolved) break;

            String guess = IO.readln("Guess the word: ").trim().toUpperCase();

            if (guess.length() != letters) {
                IO.println("Your guess must be " + letters + " letters long.");
                continue;
            }

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

    /**
     * Wordle-style scoring for ONE word. Returns an array where:
     * 0 = grey
     * 1 = yellow
     * 2 = green
     */
    static int[] scoreWordle(String word, String guess) {

        // Result color for each position
        int[] status = new int[word.length()];

        // Remaining letters that can still be matched as yellow
        char[] remaining = word.toCharArray();

        // Tracks which positions are green
        boolean[] green = new boolean[word.length()];

        // Find GREEN letters
        for (int i = 0; i < word.length(); i++) {
            char c = guess.charAt(i);
            if (c == word.charAt(i)) {
                status[i] = 2;      // green
                green[i] = true;
                remaining[i] = 0;  // consume this letter
            }
        }

        // Find YELLOW letters
        for (int i = 0; i < word.length(); i++) {
            if (green[i]) {
                continue;
            }

            char c = guess.charAt(i);
            boolean found = false;

            // Search remaining letters
            for (int j = 0; j < word.length(); j++) {
                if (remaining[j] == c) {
                    remaining[j] = 0; // consume
                    found = true;
                    break;
                }
            }

            if (found) {
                status[i] = 1; // yellow
            }
        }

        return status;
    }

    /**
     * Prints the Xordle hint for a guess.
     *
     * Color rules:
     * - BLUE   → both words have GREEN at this position
     * - GREEN  → exactly one word has GREEN
     * - YELLOW → at least one word has YELLOW (and no green rule above)
     * - BLACK  → otherwise
     */
    static void printXorHintSimple(String guess, String w1, String w2) {

        // Score the guess against both words
        int[] s1 = scoreWordle(w1, guess);
        int[] s2 = scoreWordle(w2, guess);

        IO.print(" | ");

        // Decide color for each letter position
        for (int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);

            boolean bothGreen = (s1[i] == 2 && s2[i] == 2);
            boolean oneGreen = (s1[i] == 2 && s2[i] != 2) || (s1[i] != 2 && s2[i] == 2);
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
}

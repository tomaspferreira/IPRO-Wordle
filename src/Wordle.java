public class Wordle {
    private int letters;
    private Language lang;
    private int wordsCount;

    Wordle(int wordsCountInput, int letterCount, Language language) {
        this.letters = letterCount;
        this.lang = language;
        this.wordsCount = wordsCountInput;

        String[] list = lang.getWordList(letters);

        // Hidden words for the game
        String[] words = new String[wordsCount];

        // Tracks whether each word has already been solved
        boolean[] solved = new boolean[wordsCount];

        // Used to avoid selecting the same word twice
        boolean[] used = new boolean[list.length];

        for (int i = 0; i < wordsCount; i++) {
            int random;
            do {
                random = (int) (Math.random() * list.length);
            } while (used[random]);

            used[random] = true;
            words[i] = list[random];
            solved[i] = false;
        }

        int tries = 0;
        int chances = wordsCount + letters;

        // Print a header line for each word column
        for (int i = 0; i < wordsCount; i++) {
            IO.print(" |   ");
            IO.print("Word: " + (i + 1));
            IO.print("  | ");
        }
        IO.println("");

        // Main game loop
        while (tries < chances) {

            // Check if all words are already solved
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

            // Process the guess for each hidden word
            for (int w = 0; w < wordsCount; w++) {

                if (solved[w]) {
                    IO.print("Word " + (w + 1) + ": SOLVED! ");
                    continue;
                }

                if (guess.equals(words[w])) {
                    solved[w] = true;
                    IO.print("Word " + (w + 1) + ": CORRECT!");
                    continue;
                }

                String word = words[w];

                // Remaining letters that can still be matched as yellow
                char[] lettersInWord = word.toCharArray();

                // Tracks which positions are green
                boolean[] green = new boolean[letters];

                // First pass: mark GREEN letters
                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);
                    if (c == word.charAt(i)) {
                        green[i] = true;
                        lettersInWord[i] = 0; // consume this letter
                    }
                }

                IO.print(" | ");

                // Second pass: print colors for each letter
                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);

                    if (green[i]) {
                        IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else {
                        boolean found = false;

                        // Check if the letter exists elsewhere in the word
                        for (int j = 0; j < letters; j++) {
                            if (lettersInWord[j] == c) {
                                lettersInWord[j] = 0; // consume one occurrence
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
                IO.print(" | ");
            }

            IO.println(" Remaining guesses: " + (chances - tries));
        }

        boolean allSolved = true;
        for (int i = 0; i < solved.length; i++) {
            if (!solved[i]) {
                allSolved = false;
            }
        }

        if (allSolved) {
            IO.println("Congratulations! You solved all words!");
        } else {
            IO.println("You lost. The words were:");
            for (int i = 0; i < words.length; i++) {
                IO.println(words[i]);
            }
        }
    }

    // Accessor methods (required by your VisibilityModifier rule)
    int getLetters() {
        return letters;
    }

    Language getLang() {
        return lang;
    }

    int getWordsCount() {
        return wordsCount;
    }
}

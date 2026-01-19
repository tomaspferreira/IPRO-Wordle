public class Wordle {
    int letters;
    Language lang;
    int words_count;

    Wordle(int words_count, int letters, Language lang) {
        this.letters = letters;
        this.lang = lang;
        this.words_count = words_count;

        String[] list = lang.getWordList(letters);

        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].toUpperCase();
        }
        // Hidden words for the game
        String[] words = new String[words_count];

        // Tracks whether each word has already been solved
        boolean[] solved = new boolean[words_count];

        // Used to avoid selecting the same word twice
        boolean[] used = new boolean[list.length];

        for (int i = 0; i < words_count; i++) {
            int random;
            do {
                random = (int) (Math.random() * list.length);
            } while (used[random]);

            used[random] = true;
            words[i] = list[random];
            solved[i] = false;
        }
        int tries = 0;
        int chances = words_count + 5;
        String guess = "";

        // Print a header line for each word column
        for (int i = 0; i < words_count; i++){
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

            guess = IO.readln("Guess the word: ").trim().toUpperCase();

            if (guess.length() != letters) {
                IO.println("Your guess must be " + letters + " letters long.");
                continue;
            }

            tries++;

            // Process the guess for each hidden word
            for (int w = 0; w < words_count; w++) {


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
                char[] letter_in_word = word.toCharArray();

                // Tracks which positions are green
                boolean[] green = new boolean[letters];

                // First pass: mark GREEN letters
                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);
                    if (c == word.charAt(i)) {
                        green[i] = true;
                        letter_in_word[i] = 0; // consume this letter
                    }
                }

                IO.print(" | ");

                // Second pass: print colors for each letter
                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);

                    if (green[i]) {
                        // Correct letter in correct position
                        IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else {
                        boolean found = false;

                        // Check if the letter exists elsewhere in the word
                        for (int j = 0; j < letters; j++) {
                            if (letter_in_word[j] == c) {
                                letter_in_word[j] = 0; // consume one occurrence
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
}
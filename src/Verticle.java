public class Verticle {
    private int letters;
    private Language lang;

    Verticle(int letterCount, Language language) {
        this.letters = letterCount;
        this.lang = language;

        String[] list = lang.getWordList(letters);
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].toUpperCase();
        }

        int random = (int) (Math.random() * list.length);
        String word = list[random];

        boolean solved = false;

        int tries = 0;
        int chances = letters; // one column per try

        // Board: rows = letters, columns = tries
        char[][] boardChars = new char[letters][chances]; // [row][col]
        int[][] boardColor = new int[letters][chances];   // 0=grey, 1=yellow, 2=green

        while (tries < chances) {
            String guess = IO.readln("Guess the word (column " + (tries + 1) + "): ")
                    .trim().toUpperCase();

            if (guess.length() != letters) {
                IO.println("Your guess must be " + letters + " letters long.");
                continue;
            }

            // If guessed the full word correctly, win
            if (guess.equals(word)) {
                solved = true;

                // Store final column as all green
                for (int r = 0; r < letters; r++) {
                    boardChars[r][tries] = guess.charAt(r);
                    boardColor[r][tries] = 2;
                }

                // Print the board so far
                for (int r = 0; r < letters; r++) {
                    for (int c = 0; c <= tries; c++) {
                        char letter = boardChars[r][c];
                        int color = boardColor[r][c];

                        if (color == 2) {
                            IO.print(ConsoleColors.GREEN_BACKGROUND + letter + ConsoleColors.RESET + " ");
                        } else if (color == 1) {
                            IO.print(ConsoleColors.YELLOW_BACKGROUND + letter + ConsoleColors.RESET + " ");
                        } else {
                            IO.print(ConsoleColors.BLACK_BACKGROUND + letter + ConsoleColors.RESET + " ");
                        }
                    }
                    IO.println("");
                }

                IO.println("CORRECT!");
                break;
            }

            // Target letter for THIS column
            char target = word.charAt(tries);

            // Make a mutable copy of the solution word (we will "consume" letters)
            char[] remaining = word.toCharArray();

            // Status: 0 grey, 1 yellow, 2 green
            int[] status = new int[letters];

            // PASS 1: mark EXACTLY ONE GREEN for this column (if possible)
            boolean greenUsed = false;

            for (int r = 0; r < letters; r++) {
                char c = guess.charAt(r);

                if (!greenUsed && c == target) {
                    // consume ONE occurrence of target from remaining (must exist)
                    for (int j = 0; j < letters; j++) {
                        if (remaining[j] == c) {
                            remaining[j] = 0;  // consume
                            status[r] = 2;     // green
                            greenUsed = true;  // only one green allowed
                            break;
                        }
                    }
                }
            }

            // PASS 2: mark YELLOWS using remaining letters (duplicate-safe)
            for (int r = 0; r < letters; r++) {
                if (status[r] == 2) {
                    continue; // already green
                }

                char c = guess.charAt(r);
                boolean found = false;

                for (int j = 0; j < letters; j++) {
                    if (remaining[j] == c) {
                        remaining[j] = 0; // consume
                        found = true;
                        break;
                    }
                }

                if (found) {
                    status[r] = 1; // yellow
                } else {
                    status[r] = 0; // grey
                }
            }

            // Store this guess as the next column
            for (int r = 0; r < letters; r++) {
                boardChars[r][tries] = guess.charAt(r);
                boardColor[r][tries] = status[r];
            }

            // Print the board so far (columns side-by-side)
            for (int r = 0; r < letters; r++) {
                for (int c = 0; c <= tries; c++) {
                    char ch = boardChars[r][c];
                    int col = boardColor[r][c];

                    if (col == 2) {
                        IO.print(ConsoleColors.GREEN_BACKGROUND + ch + ConsoleColors.RESET + " ");
                    } else if (col == 1) {
                        IO.print(ConsoleColors.YELLOW_BACKGROUND + ch + ConsoleColors.RESET + " ");
                    } else {
                        IO.print(ConsoleColors.BLACK_BACKGROUND + ch + ConsoleColors.RESET + " ");
                    }
                }
                IO.println("");
            }

            tries++;
            IO.println("Remaining guesses: " + (chances - tries));
        }

        if (solved) {
            IO.println("Congratulations! You got the word!");
        } else {
            IO.println("You lost. The word was " + word);
        }
    }

    // Accessor methods (required by your VisibilityModifier rule)
    int getLetters() {
        return letters;
    }

    Language getLang() {
        return lang;
    }
}

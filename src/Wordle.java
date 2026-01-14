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
        String[] words = new String[words_count];
        boolean[] solved = new boolean[words_count];

        for (int i = 0; i < words_count; i++) {
            int random = (int) (Math.random() * list.length);
            words[i] = list[random];
            solved[i] = false;
        }
        int tries = 0;
        int chances = words_count + 5;
        String guess = "";
        for (int i = 0; i < words_count; i++){
            IO.print(" |   ");
            IO.print("Word: " + (i + 1));
            IO.print("  | ");
        }
        IO.println("");
        while (tries < chances) {
            boolean allSolved = true;
            for (int i = 0; i < solved.length; i++) {
                if (!solved[i]) {
                    allSolved = false;
                    break;
                }
            }
            if (allSolved) break;

            guess = IO.readln("Guess the word: ").trim().toUpperCase();

            if (guess.length() != letters) {
                IO.println("Your guess must be " + letters + " letters long.");
                continue;
            }

            tries++;

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

                char[] letter_in_word = word.toCharArray();
                boolean[] green = new boolean[letters];

                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);
                    if (c == word.charAt(i)) {
                        green[i] = true;
                        letter_in_word[i] = 0;
                    }
                }

                IO.print(" | ");

                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);

                    if (green[i]) {
                        IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else {
                        boolean found = false;
                        for (int j = 0; j < letters; j++) {
                            if (letter_in_word[j] == c) {
                                letter_in_word[j] = 0;
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
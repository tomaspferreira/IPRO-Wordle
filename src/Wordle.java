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
                    IO.println("Word " + (w + 1) + ": SOLVED!");
                    continue;
                }

                if (guess.equals(words[w])) {
                    solved[w] = true;
                    IO.println("Word " + (w + 1) + ": CORRECT!");
                    continue;
                }

                IO.print("Word " + (w + 1) + ": ");
                for (int i = 0; i < letters; i++) {
                    char c = guess.charAt(i);

                    if (c == words[w].charAt(i)) {
                        IO.print(ConsoleColors.GREEN_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else if (words[w].indexOf(c) != -1) {
                        IO.print(ConsoleColors.YELLOW_BACKGROUND + c + ConsoleColors.RESET + " ");
                    } else {
                        IO.print(ConsoleColors.BLACK_BACKGROUND + c + ConsoleColors.RESET + " ");
                    }
                }
                IO.println("");
            }

            IO.println("Remaining guesses: " + (chances - tries));
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
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
        String guess = "";
        IO.println(words);
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


            for (int w = 0; w < 2; w++) {


                if (solved[w]) {
                    IO.print("Word " + (w + 1) + ": SOLVED! (" + words[w] + ")");
                    continue;
                }

                if (guess.equals(words[w])) {
                    solved[w] = true;
                    IO.print("Word " + (w + 1) + ": CORRECT! (" + words[w] + ")");
                    continue;
                }
            }
        }
    }
}

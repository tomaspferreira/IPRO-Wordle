public class Gamemode {
    private String game;
    private Language lang;

    Gamemode(String gameMode, Language language) {
        this.game = gameMode;
        this.lang = language;

        if (this.game.equals("wordle")) {
            int wordsInput = Integer.parseInt(
                    IO.readln("How many mystery words do you want to guess? (1, 2, 4, 8, 16, 32) ")
            );

            while (wordsInput != 1
                    && wordsInput != 2
                    && wordsInput != 4
                    && wordsInput != 8
                    && wordsInput != 16
                    && wordsInput != 32) {
                wordsInput = Integer.parseInt(
                        IO.readln("Please select one of the possible options: ")
                );
            }

            int letterInput = Integer.parseInt(
                    IO.readln("How many letters should the mystery word have? ")
            );

            while (letterInput < 4 || letterInput > 8) {
                letterInput = Integer.parseInt(
                        IO.readln("It is only possible for the word to have between 4 and 8 letters. ")
                );
            }

            new Wordle(wordsInput, letterInput, lang);

        } else if (this.game.equals("xordle")) {
            int letterInput = Integer.parseInt(
                    IO.readln("How many letters should the mystery words have? ")
            );

            while (letterInput < 4 || letterInput > 5) {
                letterInput = Integer.parseInt(
                        IO.readln("It is only possible for the word to have 4 or 5 letters. ")
                );
            }

            new Xordle(letterInput, lang);

        } else if (this.game.equals("verticle")) {
            int letterInput = Integer.parseInt(
                    IO.readln("How many letters should the mystery words have? ")
            );

            while (letterInput < 4 || letterInput > 5) {
                letterInput = Integer.parseInt(
                        IO.readln("It is only possible for the word to have 4 or 5 letters. ")
                );
            }

            new Verticle(letterInput, lang);

        } else if (this.game.equals("mathler")) {
            int length = Integer.parseInt(
                    IO.readln("How many numbers would you like the equation to have? (2 - 4) ")
            );

            while (length < 2 || length > 4) {
                length = Integer.parseInt(
                        IO.readln("Please choose between 2 and 4: ")
                );
            }

            new Mathler(length);
        }
    }

    String getGame() {
        return game;
    }

    Language getLang() {
        return lang;
    }
}

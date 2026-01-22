public class Gamemode {
    String game;
    Language lang;

    Gamemode(String game, Language lang) {
        this.game = game;
        this.lang = lang;

        if (this.game.equals("wordle")) {
            int words_input = Integer.parseInt(IO.readln("How many mystery words do you want to guess? (1, 2, 4, 8, 16, 32) "));
            while (words_input != 1 && words_input != 2 && words_input != 4 && words_input != 8 && words_input != 16 && words_input != 32){
                words_input = Integer.parseInt(IO.readln("Please select one of the possible options: "));
            }
            int letter_input = Integer.parseInt(IO.readln("How many letters should the mystery word have? "));
            while (letter_input < 4 || letter_input > 8) {
                letter_input = Integer.parseInt(IO.readln("It is only possible for the word to have between 4 and 8 letters. "));
            }
            Wordle wordle = new Wordle(words_input, letter_input, lang);
        } else if (this.game.equals("xordle")) {
            int letter_input = Integer.parseInt(IO.readln("How many letters should the mystery words have? "));
            while (letter_input < 4 || letter_input > 5) {
                letter_input = Integer.parseInt(IO.readln("It is only possible for the word to have 4 or 5 letters. "));
            }
            Xordle xordle = new Xordle(letter_input, lang);

        } else if (this.game.equals("verticle")) {
            int letter_input = Integer.parseInt(IO.readln("How many letters should the mystery words have? "));
            while (letter_input < 4 || letter_input > 5) {
                letter_input = Integer.parseInt(IO.readln("It is only possible for the word to have 4 or 5 letters. "));
            }
            Verticle verticle = new Verticle(letter_input, lang);

        } else if (this.game.equals("mathler")) {
            int length = Integer.parseInt(IO.readln("How many numbers would you like the equation to have? (2 - 4) "));
            while (length < 2 || length > 4) {
                length = Integer.parseInt(IO.readln("Please choose between 2 and 4: "));
            }
            Mathler mathler = new Mathler(length);
        }
    }
}

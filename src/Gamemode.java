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

        } else if (this.game.equals("verticle")) {

        } else if (this.game.equals("mathler")) {

        }
    }
}

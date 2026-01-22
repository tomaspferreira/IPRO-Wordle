public class Main {
    public static void main(String[] args) {
        String language = IO.readln("Which language do you prefer? (en/de): ").toLowerCase();
        while (!language.equals("en") && !language.equals("de")) {
            language = IO.readln("That option doesn't exist. Please make sure you typed one of the options. \n"
            ).toLowerCase();
        }

        Language lang = new Language(language);
        String gamemode = IO.readln("Which game do you want to play? \n" +
                "Wordle   Xordle   Verticle   Mathler \n").toLowerCase();
        while (!gamemode.equals("wordle") && !gamemode.equals("xordle") && !gamemode.equals("verticle") && !gamemode.equals("mathler")) {
            gamemode = IO.readln("That option doesn't exist. Please make sure you typed it correctly. \n"
            ).toLowerCase();
        }

        Gamemode game = new Gamemode(gamemode, lang);
    }
}
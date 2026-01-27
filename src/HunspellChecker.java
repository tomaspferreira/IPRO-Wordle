import com.nikialeksey.hunspell.Hunspell;

public final class HunspellChecker {

    private static Hunspell hunspell;

    private HunspellChecker() {
    }

    static void init(String language) {
        // Avoid reloading if init is called multiple times
        if (hunspell != null) {
            return;
        }

        String base = System.getProperty("user.dir") + "/resources/hunspell/";

        String dicPath;
        String affPath;

        if ("de".equals(language)) {
            dicPath = base + "de_DE.dic";
            affPath = base + "de_DE.aff";
        } else {
            dicPath = base + "en_US.dic";
            affPath = base + "en_US.aff";
        }

        // AFF first, then DIC (this fixes the 'the'/'and'/'house' false problem)
        hunspell = new Hunspell(dicPath, affPath);
    }

    static boolean isValidWord(String word) {
        if (hunspell == null) {
            return true; // fail-safe if init wasn't called
        }
        return hunspell.spell(word)
                || hunspell.spell(word.toLowerCase())
                || hunspell.spell(word.toUpperCase());
    }
}


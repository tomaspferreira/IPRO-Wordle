import javafx.scene.control.Button;
import java.util.HashMap;
import java.util.Map;

public class KeyboardColorManager {

    /**
     * Maps each keyboard character to its corresponding button in the UI.
     */
    private final Map<Character, Button> keyButtons = new HashMap<>();

    /**
     * Stores the current rank/color state for each key.
     */
    private final Map<Character, Integer> keyRank = new HashMap<>();

    public void registerKey(char ch, Button button) {
        ch = Character.toUpperCase(ch);
        keyButtons.put(ch, button);
        keyRank.put(ch, 0);
    }

    public void promoteKey(char ch, int newRank) {
        ch = Character.toUpperCase(ch);

        Button b = keyButtons.get(ch);
        if (b == null) {
            return;
        }

        int old = keyRank.getOrDefault(ch, 0);

        if (newRank > old) {
            keyRank.put(ch, newRank);
            b.setStyle(styleForRank(newRank));
        }
    }

    private String styleForRank(int rank) {
        if (rank == 1) {
            return GameStyles.keyGrey();
        } else if (rank == 2) {
            return GameStyles.keyYellow();
        } else if (rank == 3) {
            return GameStyles.keyGreen();
        } else if (rank == 4) {
            return GameStyles.keyBlue();
        }
        return GameStyles.keyBase();
    }
}

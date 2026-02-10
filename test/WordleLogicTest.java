import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WordleLogicTest {

    @Test
    void exactMatch_allGreen_andGameWon() {
        WordleLogic g = new WordleLogic("APPLE");

        WordleLogic.TurnResult r = g.submitGuess("APPLE");

        assertTrue(r.isGameWon());
        assertTrue(r.isGameOver());

        WordleLogic.Tile[][] tiles = r.getTilesByWord();
        for (int i = 0; i < 5; i++) {
            assertEquals(WordleLogic.Tile.GREEN, tiles[0][i]);
        }
    }

    @Test
    void wrongLength_throws() {
        WordleLogic g = new WordleLogic("APPLE");
        assertThrows(IllegalArgumentException.class, () -> g.submitGuess("APP"));
    }

    @Test
    void noLettersMatch_allGrey() {
        WordleLogic g = new WordleLogic("APPLE");

        WordleLogic.TurnResult r = g.submitGuess("ZZZZZ");

        WordleLogic.Tile[][] tiles = r.getTilesByWord();
        for (int i = 0; i < 5; i++) {
            assertEquals(WordleLogic.Tile.GREY, tiles[0][i]);
        }
        assertFalse(r.isGameWon());
        assertFalse(r.isGameOver()); // should still have guesses left
    }

    @Test
    void duplicateHandling_doesNotOvermarkYellow() {
        // secret has only one 'A'
        WordleLogic g = new WordleLogic("BANJO");

        // guess contains two 'A' (actually 1, but we can do better:)
        // Let's use a secret with exactly one 'L' and guess with two L's:
        WordleLogic g2 = new WordleLogic("PLANT"); // only one 'A' and one 'L'

        WordleLogic.TurnResult r = g2.submitGuess("ALLEY");
        WordleLogic.Tile[][] tiles = r.getTilesByWord();

        // Word: P L A N T
        // Guess: A L L E Y
        // Index1 'L' is GREEN (matches L at pos1)
        assertEquals(WordleLogic.Tile.GREEN, tiles[0][1]);

        // There is only ONE 'L' in PLANT, so the second 'L' (pos2) must NOT become yellow.
        assertNotEquals(WordleLogic.Tile.YELLOW, tiles[0][2]);
    }

    @Test
    void solvedWord_staysAllGreen_onLaterGuesses() {
        WordleLogic g = new WordleLogic("APPLE", "BERRY");

        // Solve first word
        WordleLogic.TurnResult r1 = g.submitGuess("APPLE");
        assertTrue(r1.getSolved()[0]);
        assertFalse(r1.getSolved()[1]);

        // Next guess should keep word0 all green regardless of guess content
        WordleLogic.TurnResult r2 = g.submitGuess("ZZZZZ");
        WordleLogic.Tile[][] tiles2 = r2.getTilesByWord();

        for (int i = 0; i < 5; i++) {
            assertEquals(WordleLogic.Tile.GREEN, tiles2[0][i], "Solved word must remain green");
        }
    }

    @Test
    void allWordsMustBeSolvedToWin() {
        Language dummy = new Language("en"); // only used for constructor

        WordleLogic g = new WordleLogic(1, 5, dummy);

        assertFalse(g.isGameWon());
    }

}

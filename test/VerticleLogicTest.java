import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VerticleLogicTest {

    @Test
    void chancesEqualsLetters_andGameOverAfterLettersTries() {
        VerticleLogic g = new VerticleLogic("CATS");
        assertEquals(4, g.getLetters());
        assertEquals(4, g.getChances());
        assertFalse(g.isGameOver());

        g.submitGuess("AAAA");
        g.submitGuess("BBBB");
        g.submitGuess("CCCC");
        assertFalse(g.isGameOver());

        g.submitGuess("DDDD");
        assertTrue(g.isGameOver());
    }

    @Test
    void tryIndexInResultMatchesColumnUsed() {
        VerticleLogic g = new VerticleLogic("CATS");

        VerticleLogic.TurnResult r0 = g.submitGuess("AAAA");
        assertEquals(0, r0.getTryIndex());

        VerticleLogic.TurnResult r1 = g.submitGuess("BBBB");
        assertEquals(1, r1.getTryIndex());
    }

    @Test
    void exactMatchWins_allGreen() {
        VerticleLogic g = new VerticleLogic("CATS");

        VerticleLogic.TurnResult r = g.submitGuess("CATS");
        assertTrue(r.isGameWon());
        assertTrue(r.isGameOver());

        for (VerticleLogic.Tile t : r.getTiles()) {
            assertEquals(VerticleLogic.Tile.GREEN, t);
        }
    }

    @Test
    void perTryTargetsCorrectSecretCharacter() {
        // word: C A T S
        VerticleLogic g = new VerticleLogic("CATS");

        // Try 0 targets 'C' somewhere in guess => exactly one GREEN allowed in your logic
        VerticleLogic.TurnResult r0 = g.submitGuess("CCCC");
        int greens0 = 0;
        for (VerticleLogic.Tile t : r0.getTiles()) {
            if (t == VerticleLogic.Tile.GREEN) greens0++;
        }
        assertEquals(1, greens0, "Only one GREEN should be assigned for the target character on a try.");

        // Try 1 targets 'A'
        VerticleLogic.TurnResult r1 = g.submitGuess("AAAA");
        int greens1 = 0;
        for (VerticleLogic.Tile t : r1.getTiles()) {
            if (t == VerticleLogic.Tile.GREEN) greens1++;
        }
        assertEquals(1, greens1);
    }

    @Test
    void rejectsWrongLengthGuess() {
        VerticleLogic g = new VerticleLogic("CATS");

        assertThrows(
                IllegalArgumentException.class,
                () -> g.submitGuess("CAT")
        );
    }

    @Test
    void remainingGuessesDecrease() {
        VerticleLogic g = new VerticleLogic("CATS");

        VerticleLogic.TurnResult r = g.submitGuess("AAAA");

        assertEquals(3, r.getRemainingGuesses());
    }

    @Test
    void yellowWhenLetterExistsElsewhere() {
        VerticleLogic g = new VerticleLogic("CATS");

        VerticleLogic.TurnResult r = g.submitGuess("SCAR");

        boolean hasYellow = false;
        for (VerticleLogic.Tile t : r.getTiles()) {
            if (t == VerticleLogic.Tile.YELLOW) {
                hasYellow = true;
            }
        }

        assertTrue(hasYellow);
    }

}

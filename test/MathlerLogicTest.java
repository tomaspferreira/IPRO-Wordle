import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MathlerLogicTest {

    @Test
    void targetIsComputedWithPrecedence() {
        MathlerLogic g = new MathlerLogic("2+3*4"); // 2 + 12 = 14
        assertEquals(14, g.getTarget());
        assertEquals("2+3*4", g.getEquation());
        assertEquals("2+3*4".length() + 2, g.getChances());
    }

    @Test
    void wrongLengthThrows() {
        MathlerLogic g = new MathlerLogic("12+3"); // length 4
        assertThrows(IllegalArgumentException.class, () -> g.submitGuess("1+2"));
    }

    @Test
    void invalidCharThrows() {
        MathlerLogic g = new MathlerLogic("12+3");
        assertThrows(IllegalArgumentException.class, () -> g.submitGuess("12=a"));
    }

    @Test
    void exactMatchWins_allGreen() {
        MathlerLogic g = new MathlerLogic("12+3");

        MathlerLogic.TurnResult r = g.submitGuess("12+3");
        assertTrue(r.isGameWon());
        assertTrue(r.isGameOver());
        assertEquals(0, r.getRemainingGuesses() >= 0 ? 0 : 1); // just sanity

        for (MathlerLogic.Tile t : r.getTiles()) {
            assertEquals(MathlerLogic.Tile.GREEN, t);
        }
    }

    @Test
    void scoringProducesYellowAndGrey() {
        MathlerLogic g = new MathlerLogic("12+3");

        // same chars but moved: should produce some yellow
        MathlerLogic.TurnResult r = g.submitGuess("21+3");
        assertFalse(r.isGameWon());

        // positions:
        // eq: 1 2 + 3
        // g : 2 1 + 3
        // => pos2 '+' green, pos3 '3' green, first two should be yellow (swapped)
        assertEquals(MathlerLogic.Tile.YELLOW, r.getTiles()[0]);
        assertEquals(MathlerLogic.Tile.YELLOW, r.getTiles()[1]);
        assertEquals(MathlerLogic.Tile.GREEN, r.getTiles()[2]);
        assertEquals(MathlerLogic.Tile.GREEN, r.getTiles()[3]);
    }

    @Test
    void divisionAndMultiplicationPrecedence() {
        MathlerLogic g = new MathlerLogic("8/2*3"); // (8/2)*3 = 12
        assertEquals(12, g.getTarget());
    }

    @Test
    void supportsMultiDigitNumbers() {
        MathlerLogic g = new MathlerLogic("12+34");
        assertEquals(46, g.getTarget());
    }

    @Test
    void noFurtherGuessesAfterWin() {
        MathlerLogic g = new MathlerLogic("12+3");

        g.submitGuess("12+3");

        MathlerLogic.TurnResult r = g.submitGuess("12+3");

        assertTrue(r.isGameOver());
    }
}

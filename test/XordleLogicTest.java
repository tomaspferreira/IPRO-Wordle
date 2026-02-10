import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XordleLogicTest {

    @Test
    void blueWhenBothWordsGreenAtPosition() {
        XordleLogic g = new XordleLogic("COLD", "CORD"); // positions: C O _ D match at 0,1,3

        XordleLogic.TurnResult r = g.submitGuess("COLD");
        XordleLogic.Tile[] t = r.getTiles();

        // pos0 'C' green for both => BLUE
        assertEquals(XordleLogic.Tile.BLUE, t[0]);
        // pos1 'O' green for both => BLUE
        assertEquals(XordleLogic.Tile.BLUE, t[1]);
        // pos3 'D' green for both => BLUE
        assertEquals(XordleLogic.Tile.BLUE, t[3]);
    }

    @Test
    void greenWhenExactlyOneWordGreenAtPosition() {
        XordleLogic g = new XordleLogic("COLD", "WARM");
        XordleLogic.TurnResult r = g.submitGuess("CXXX"); // C matches only first word at pos0

        assertEquals(XordleLogic.Tile.GREEN, r.getTiles()[0]);
    }

    @Test
    void rejectsWrongLength() {
        XordleLogic g = new XordleLogic("COLD", "WARM");
        assertThrows(IllegalArgumentException.class, () -> g.submitGuess("TOO"));
    }

    @Test
    void newlySolvedFlagsWork() {
        XordleLogic g = new XordleLogic("COLD", "WARM");

        XordleLogic.TurnResult r1 = g.submitGuess("COLD");
        assertTrue(r1.getSolved()[0]);
        assertFalse(r1.getSolved()[1]);
        assertTrue(r1.getNewlySolved()[0]);
        assertFalse(r1.getNewlySolved()[1]);

        XordleLogic.TurnResult r2 = g.submitGuess("WARM");
        assertTrue(r2.getSolved()[0]);
        assertTrue(r2.getSolved()[1]);
        assertFalse(r2.getNewlySolved()[0]); // already solved in r1
        assertTrue(r2.getNewlySolved()[1]);
        assertTrue(r2.isGameWon());
        assertTrue(r2.isGameOver());
    }
}

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MathlerView extends BorderPane {

    private final Navigator nav;
    private final MathlerLogic game;

    private final int len;
    private final int chances;

    private int rowIndex = 0;
    private int colIndex = 0;
    private final char[] current;

    private final Label message = new Label("");
    private final Label remaining;

    private final GridPane grid = new GridPane();
    private final Label[][] tiles; // [row][col], we add rows progressively

    // Keyboard coloring (never downgrade)
    private final java.util.Map<Character, Button> keyButtons = new java.util.HashMap<>();
    private final java.util.Map<Character, Integer> keyRank = new java.util.HashMap<>();

    private Runnable enterAction;

    public MathlerView(Navigator nav, int numbersCount) {
        this.nav = nav;
        this.game = new MathlerLogic(numbersCount);

        this.len = game.getEquationLength();
        this.chances = game.getChances();
        this.current = new char[len];

        this.remaining = new Label("Guesses left: " + chances);

        // ---------- TOP ----------
        Label title = new Label("Mathler");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label target = new Label("Target result: " + game.getTarget());
        target.setStyle("-fx-font-size: 24px;");

        VBox top = new VBox(8, title, target);
        top.setAlignment(Pos.CENTER);
        setTop(top);

        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        remaining.setStyle("-fx-font-size: 16px;");

        // ---------- GRID ----------
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));

        tiles = new Label[chances][len];

        // Create all labels but DO NOT add them yet
        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < len; c++) {
                Label t = new Label(" ");
                t.setMinSize(55, 55);
                t.setAlignment(Pos.CENTER);
                t.setStyle(baseStyle());
                tiles[r][c] = t;
            }
        }

        // Add ONLY first row so grid shows immediately
        addRowToGrid(0);

        ScrollPane centerScroll = new ScrollPane(grid);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setPannable(true);
        setCenter(centerScroll);

        // ---------- BOTTOM ----------
        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(160);
        backBtn.setPrefHeight(50);
        backBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(160);
        giveUpBtn.setPrefHeight(50);
        giveUpBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox keyboard = buildMathKeyboard(); // includes ENTER + ⌫

        VBox bottom = new VBox(10, keyboard, giveUpBtn, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(15));
        setBottom(bottom);

        // If you want language-aware settings later, store language in this class
        backBtn.setOnAction(e -> nav.goToSettings("en", "Mathler"));

        giveUpBtn.setOnAction(e -> showLose(backBtn, giveUpBtn, "You gave up."));

        // ---------- SUBMIT ----------
        Runnable submit = () -> {
            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }
            if (rowIndex >= chances) {
                message.setText("No guesses left.");
                return;
            }
            if (colIndex < len) {
                message.setText("Not enough characters.");
                return;
            }

            String guess = new String(current);

            MathlerLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            // paint row + update keyboard colors
            for (int c = 0; c < len; c++) {
                Label t = tiles[rowIndex][c];
                char ch = r.guess.charAt(c);
                t.setText(String.valueOf(ch));
                t.setStyle(baseStyle() + styleFor(r.tiles[c]));

                promoteKey(ch, rankForMathTile(r.tiles[c]));
            }

            remaining.setText("Guesses left: " + r.remainingGuesses);
            message.setText("");

            if (r.gameWon) {
                showWin(backBtn, giveUpBtn);
                return;
            }
            if (r.gameOver) {
                showLose(backBtn, giveUpBtn, "Out of guesses.");
                return;
            }

            // next row
            rowIndex++;
            colIndex = 0;
            for (int i = 0; i < len; i++) current[i] = 0;

            if (rowIndex < chances) {
                addRowToGrid(rowIndex);
            }
        };

        // ENTER handler used by GUI keyboard + physical keyboard
        this.enterAction = submit;

        // Physical keyboard support too (GUI keyboard solves shift issues anyway)
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;

            // ENTER submits
            if (e.getCode() == KeyCode.ENTER) {
                if (enterAction != null) enterAction.run();
                return;
            }

            // BACKSPACE deletes
            if (e.getCode() == KeyCode.BACK_SPACE) {
                backspace();
                return;
            }

            boolean shift = e.isShiftDown();

            // Map digit keys + shift to operators (layout-safe)
            if (e.getCode() == KeyCode.DIGIT1) {
                if (shift) typeChar('+'); else typeChar('1');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT3) {
                if (shift) typeChar('*'); else typeChar('3');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT7) {
                if (shift) typeChar('/'); else typeChar('7');
                return;
            }

            // Other digits (0,2,4,5,6,8,9)
            if (e.getCode() == KeyCode.DIGIT0) { typeChar('0'); return; }
            if (e.getCode() == KeyCode.DIGIT2) { typeChar('2'); return; }
            if (e.getCode() == KeyCode.DIGIT4) { typeChar('4'); return; }
            if (e.getCode() == KeyCode.DIGIT5) { typeChar('5'); return; }
            if (e.getCode() == KeyCode.DIGIT6) { typeChar('6'); return; }
            if (e.getCode() == KeyCode.DIGIT8) { typeChar('8'); return; }
            if (e.getCode() == KeyCode.DIGIT9) { typeChar('9'); return; }

            // Numpad support
            if (e.getCode() == KeyCode.NUMPAD0) { typeChar('0'); return; }
            if (e.getCode() == KeyCode.NUMPAD1) { typeChar('1'); return; }
            if (e.getCode() == KeyCode.NUMPAD2) { typeChar('2'); return; }
            if (e.getCode() == KeyCode.NUMPAD3) { typeChar('3'); return; }
            if (e.getCode() == KeyCode.NUMPAD4) { typeChar('4'); return; }
            if (e.getCode() == KeyCode.NUMPAD5) { typeChar('5'); return; }
            if (e.getCode() == KeyCode.NUMPAD6) { typeChar('6'); return; }
            if (e.getCode() == KeyCode.NUMPAD7) { typeChar('7'); return; }
            if (e.getCode() == KeyCode.NUMPAD8) { typeChar('8'); return; }
            if (e.getCode() == KeyCode.NUMPAD9) { typeChar('9'); return; }

            // Direct operator keys (if they exist on the keyboard)
            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD) { typeChar('+'); return; }
            if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) { typeChar('-'); return; }
            if (e.getCode() == KeyCode.SLASH || e.getCode() == KeyCode.DIVIDE) { typeChar('/'); return; }
            if (e.getCode() == KeyCode.ASTERISK || e.getCode() == KeyCode.MULTIPLY) { typeChar('*'); return; }

            // As fallback, still try text for other cases
            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = txt.charAt(0);
                if (isAllowedMathChar(ch)) typeChar(ch);
            }
        });


        // Ensure first render + focus
        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
        });
    }

    // ---------------- grid progressive reveal ----------------
    private void addRowToGrid(int row) {
        for (int c = 0; c < len; c++) {
            if (!grid.getChildren().contains(tiles[row][c])) {
                grid.add(tiles[row][c], c, row);
            }
        }
    }

    // ---------------- typing ----------------
    private void typeChar(char ch) {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex >= len) return;

        // Normalize: allow both 'x' and 'X' -> '*'
        if (ch == 'x' || ch == 'X') ch = '*';

        // Keep exactly what user typed for operators; for digits keep digit
        current[colIndex] = ch;

        tiles[rowIndex][colIndex].setText(String.valueOf(ch));
        colIndex++;
    }

    private void backspace() {
        if (colIndex <= 0) return;

        colIndex--;
        current[colIndex] = 0;
        tiles[rowIndex][colIndex].setText(" ");
    }

    private boolean isAllowedMathChar(char ch) {
        return (ch >= '0' && ch <= '9') || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == 'x' || ch == 'X';
    }

    // ---------------- styles ----------------
    private String baseStyle() {
        return "-fx-border-color: #444; -fx-font-size: 22px; -fx-font-weight: bold; -fx-alignment: center;";
    }

    private String styleFor(MathlerLogic.Tile tile) {
        return switch (tile) {
            case GREEN -> "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            case YELLOW -> "-fx-background-color: #C9B458; -fx-text-fill: white;";
            default -> "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };
    }

    // ---------------- win/lose screens ----------------
    private void showLose(Button backBtn, Button giveUpBtn, String msg) {
        Label lost = new Label("You lost. The equation was:");
        lost.setStyle("-fx-font-size: 36px;");

        Label answer = new Label(game.getEquation());
        answer.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        VBox losing = new VBox(15, lost, answer);
        losing.setAlignment(Pos.CENTER);
        losing.setPadding(new Insets(20));

        setCenter(losing);

        message.setText(msg);
        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        VBox bottom = (VBox) getBottom();
        if (!bottom.getChildren().contains(backBtn)) {
            bottom.getChildren().add(1, backBtn);
        }
    }

    private void showWin(Button backBtn, Button giveUpBtn) {
        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        setCenter(won);

        message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
        message.setText("Correct!");

        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        VBox bottom = (VBox) getBottom();
        if (!bottom.getChildren().contains(backBtn)) {
            bottom.getChildren().add(1, backBtn);
        }
    }

    // ---------------- keyboard UI (smaller) ----------------
    private VBox buildMathKeyboard() {
        // Keep it compact and include operators directly (no shift needed)
        String[] r1 = {"1","2","3","+","-"};
        String[] r2 = {"4","5","6","*","/"};
        String[] r3 = {"7","8","9","0"};
        String[] r4 = {"ENTER","⌫"};

        VBox kb = new VBox(6,
                mathRow(r1),
                mathRow(r2),
                mathRow(r3),
                mathRow(r4)
        );
        kb.setAlignment(Pos.CENTER);
        kb.setPadding(new Insets(5, 0, 0, 0));
        return kb;
    }

    private HBox mathRow(String[] keys) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);

        for (String k : keys) {
            Button b = new Button(k);
            b.setPrefHeight(40);

            if (k.equals("ENTER")) b.setPrefWidth(140);
            else if (k.equals("⌫")) b.setPrefWidth(90);
            else b.setPrefWidth(54);

            b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // store for coloring (only single-char keys)
            if (k.length() == 1) {
                char ch = k.charAt(0);
                keyButtons.put(ch, b);
                keyRank.put(ch, 0);
            }

            b.setOnAction(e -> {
                if (k.equals("ENTER")) {
                    if (enterAction != null) enterAction.run();
                } else if (k.equals("⌫")) {
                    backspace();
                } else {
                    typeChar(k.charAt(0));
                }
            });

            row.getChildren().add(b);
        }

        return row;
    }

    // ---------------- keyboard coloring ----------------
    // rank: 0 none, 1 grey, 2 yellow, 3 green
    private int rankForMathTile(MathlerLogic.Tile t) {
        return switch (t) {
            case GREY -> 1;
            case YELLOW -> 2;
            case GREEN -> 3;
        };
    }

    private void promoteKey(char ch, int newRank) {
        // Normalize key for '*' if someone used x/X
        if (ch == 'x' || ch == 'X') ch = '*';

        Button b = keyButtons.get(ch);
        if (b == null) return;

        int old = keyRank.getOrDefault(ch, 0);
        if (newRank > old) {
            keyRank.put(ch, newRank);
            b.setStyle(styleForRank(newRank));
        }
    }

    private String styleForRank(int rank) {
        String base = "-fx-font-size: 14px; -fx-font-weight: bold;";
        return switch (rank) {
            case 1 -> base + "-fx-background-color: #787C7E; -fx-text-fill: white;";
            case 2 -> base + "-fx-background-color: #C9B458; -fx-text-fill: white;";
            case 3 -> base + "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            default -> base;
        };
    }
}

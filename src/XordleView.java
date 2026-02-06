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

public class XordleView extends BorderPane {

    private final Navigator nav;
    private final String language;

    private final XordleLogic game;

    private final int letters;
    private final int chances;

    private int rowIndex = 0;
    private int colIndex = 0;
    private final char[] current;

    private final Label message = new Label("");
    private final Label remaining;

    private final GridPane grid = new GridPane();
    private final Label[][] tiles; // [row][col], but we only ADD rows as we go

    // Keyboard coloring
    private final java.util.Map<Character, Button> keyButtons = new java.util.HashMap<>();
    private final java.util.Map<Character, Integer> keyRank = new java.util.HashMap<>();

    public XordleView(Navigator nav, String language, int letters) {
        this.nav = nav;
        this.language = language;

        Language lang = new Language(language);
        this.game = new XordleLogic(letters, lang);

        this.letters = game.getLetters();
        this.chances = game.getChances();
        this.current = new char[this.letters];

        this.remaining = new Label("Guesses left: " + chances);

        // ---------- TOP ----------
        Label title = new Label("Xordle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        remaining.setStyle("-fx-font-size: 16px;");

        VBox top = new VBox(6, title);
        top.setAlignment(Pos.CENTER);
        setTop(top);

        // ---------- GRID ----------
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));

        tiles = new Label[chances][letters];

        // Create all labels, but DO NOT add to grid yet
        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < letters; c++) {
                Label t = new Label(" ");
                t.setMinSize(55, 55);
                t.setAlignment(Pos.CENTER);
                t.setStyle(baseStyle());
                tiles[r][c] = t;
            }
        }

        // Add ONLY the first row initially so grid is visible
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

        VBox keyboard = buildLetterKeyboard(); // includes ENTER + ⌫

        VBox bottom = new VBox(10, keyboard, giveUpBtn, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(15));
        setBottom(bottom);

        backBtn.setOnAction(e -> nav.goToSettings(language, "Xordle"));

        // Give up → lose layout like before
        giveUpBtn.setOnAction(e -> showLose(backBtn, giveUpBtn, "You gave up."));

        // ENTER submit action
        Runnable submit = () -> {
            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }
            if (rowIndex >= chances) {
                message.setText("No guesses left.");
                return;
            }
            if (colIndex < letters) {
                message.setText("Not enough letters.");
                return;
            }

            String guess = new String(current);

            if (!HunspellChecker.isValidWord(guess)) {
                message.setText("Not a valid word.");
                return;
            }

            XordleLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            // Paint row
            for (int c = 0; c < letters; c++) {
                Label t = tiles[rowIndex][c];
                t.setText(String.valueOf(r.guess.charAt(c)));
                t.setStyle(baseStyle() + styleFor(r.tiles[c]));

                // keyboard color update
                promoteKey(r.guess.charAt(c), rankForXordleTile(r.tiles[c]));
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

            // Next row appears now
            rowIndex++;
            colIndex = 0;
            for (int i = 0; i < letters; i++) current[i] = 0;

            if (rowIndex < chances) {
                addRowToGrid(rowIndex);
            }
        };

        // Hook keyboard actions
        setKeyHandler(submit);

        // Ensure first render + focus
        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
        });
    }

    // ----- Grid helpers -----
    private void addRowToGrid(int row) {
        for (int c = 0; c < letters; c++) {
            if (!grid.getChildren().contains(tiles[row][c])) {
                grid.add(tiles[row][c], c, row);
            }
        }
    }

    // ----- typing -----
    private void typeChar(char ch) {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex >= letters) return;

        ch = Character.toUpperCase(ch);
        current[colIndex] = ch;

        Label t = tiles[rowIndex][colIndex];
        t.setText(String.valueOf(ch));

        colIndex++;
    }

    private void backspace() {
        if (game.isGameOver()) return;
        if (colIndex <= 0) return;

        colIndex--;
        current[colIndex] = 0;
        tiles[rowIndex][colIndex].setText(" ");
    }

    // ----- styles -----
    private String baseStyle() {
        return "-fx-border-color: #444; -fx-font-size: 22px; -fx-font-weight: bold; -fx-alignment: center;";
    }

    private String styleFor(XordleLogic.Tile tile) {
        return switch (tile) {
            case BLUE -> "-fx-background-color: #3B82F6; -fx-text-fill: white;";
            case GREEN -> "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            case YELLOW -> "-fx-background-color: #C9B458; -fx-text-fill: white;";
            default -> "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };
    }

    // ----- lose/win layouts -----
    private void showLose(Button backBtn, Button giveUpBtn, String msg) {
        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 36px;");

        Label w1 = new Label(game.getWords()[0]);
        w1.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label w2 = new Label(game.getWords()[1]);
        w2.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        VBox losing = new VBox(15, lost, w1, w2);
        losing.setAlignment(Pos.CENTER);
        losing.setPadding(new Insets(20));

        setCenter(losing);

        message.setText(msg);
        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        // Put back button under keyboard by replacing bottom
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
        message.setText("Solved both words!");

        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        VBox bottom = (VBox) getBottom();
        if (!bottom.getChildren().contains(backBtn)) {
            bottom.getChildren().add(1, backBtn);
        }
    }

    // ----- Keyboard (smaller) -----
    private VBox buildLetterKeyboard() {
        String[] r1 = {"Q","W","E","R","T","Z","U","I","O","P"};
        String[] r2 = {"A","S","D","F","G","H","J","K","L"};
        String[] r3 = {"ENTER","Y","X","C","V","B","N","M","⌫"};

        VBox kb = new VBox(6,
                keyboardRow(r1),
                keyboardRow(r2),
                keyboardRow(r3)
        );
        kb.setAlignment(Pos.CENTER);
        kb.setPadding(new Insets(5, 0, 0, 0));
        return kb;
    }

    private HBox keyboardRow(String[] keys) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);

        for (String k : keys) {
            Button b = new Button(k);
            b.setPrefHeight(40); // smaller
            if (k.equals("ENTER")) b.setPrefWidth(110);
            else if (k.equals("⌫")) b.setPrefWidth(70);
            else b.setPrefWidth(44);

            b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            if (k.length() == 1) {
                char ch = k.charAt(0);
                keyButtons.put(ch, b);
                keyRank.put(ch, 0);
            }

            b.setOnAction(e -> handleVirtualKey(k));
            row.getChildren().add(b);
        }
        return row;
    }

    private void handleVirtualKey(String k) {
        if (k.equals("ENTER")) {
            // handled in setKeyHandler() by firing submit via KeyCode.ENTER path
            // we simulate it:
            fireEnter();
        } else if (k.equals("⌫")) {
            backspace();
        } else {
            typeChar(k.charAt(0));
        }
    }

    // ----- Input wiring -----
    private Runnable enterAction;

    private void setKeyHandler(Runnable submit) {
        this.enterAction = submit;

        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;

            if (e.getCode() == KeyCode.ENTER) {
                submit.run();
                return;
            }
            if (e.getCode() == KeyCode.BACK_SPACE) {
                backspace();
                return;
            }

            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = Character.toUpperCase(txt.charAt(0));
                if (ch >= 'A' && ch <= 'Z') typeChar(ch);
            }
        });
    }

    private void fireEnter() {
        if (enterAction != null) enterAction.run();
    }

    // ----- Keyboard coloring (never downgrade) -----
    private int rankForXordleTile(XordleLogic.Tile t) {
        return switch (t) {
            case GREY -> 1;
            case YELLOW -> 2;
            case GREEN -> 3;
            case BLUE -> 4;
        };
    }

    private void promoteKey(char ch, int newRank) {
        ch = Character.toUpperCase(ch);
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
            case 4 -> base + "-fx-background-color: #3B82F6; -fx-text-fill: white;";
            default -> base;
        };
    }
}

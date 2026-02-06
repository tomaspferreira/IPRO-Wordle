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

public class VerticleView extends BorderPane {

    private final Navigator nav;
    private final String language;

    private final VerticleLogic game;

    private final int letters;
    private final int chances;

    private int typedIndex = 0;              // typing goes DOWN the current column
    private final char[] current;

    private final Label message = new Label("");
    private final Label remaining;

    private final GridPane grid = new GridPane();
    private final Label[][] tiles; // [row][col] rows=letters, cols=tries, but we only ADD columns

    // Keyboard coloring
    private final java.util.Map<Character, Button> keyButtons = new java.util.HashMap<>();
    private final java.util.Map<Character, Integer> keyRank = new java.util.HashMap<>();

    private Runnable enterAction;

    public VerticleView(Navigator nav, String language, int lettersInput) {
        this.nav = nav;
        this.language = language;

        Language lang = new Language(language);
        this.game = new VerticleLogic(lettersInput, lang);

        this.letters = game.getLetters();
        this.chances = game.getChances(); // must be == letters
        this.current = new char[this.letters];

        this.remaining = new Label("Guesses left: " + chances);

        // ---------- TOP ----------
        Label title = new Label("Verticle");
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

        tiles = new Label[letters][chances];

        // Create labels but don't add to grid
        for (int r = 0; r < letters; r++) {
            for (int c = 0; c < chances; c++) {
                Label t = new Label(" ");
                t.setMinSize(55, 55);
                t.setAlignment(Pos.CENTER);
                t.setStyle(baseStyle());
                tiles[r][c] = t;
            }
        }

        // Add ONLY the first column at start so grid is visible
        addColumnToGrid(0);

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

        VBox keyboard = buildLetterKeyboard();

        VBox bottom = new VBox(10, keyboard, giveUpBtn, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(15));
        setBottom(bottom);

        backBtn.setOnAction(e -> nav.goToSettings(language, "Verticle"));

        giveUpBtn.setOnAction(e -> showLose(backBtn, giveUpBtn, "You gave up."));

        Runnable submit = () -> {
            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }

            if (typedIndex < letters) {
                message.setText("Not enough letters.");
                return;
            }

            String guess = new String(current);

            if (!HunspellChecker.isValidWord(guess)) {
                message.setText("Not a valid word.");
                return;
            }

            VerticleLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            int col = r.tryIndex;

            // paint that column
            for (int row = 0; row < letters; row++) {
                Label t = tiles[row][col];
                t.setText(String.valueOf(r.guess.charAt(row)));
                t.setStyle(baseStyle() + styleFor(r.tiles[row]));

                promoteKey(r.guess.charAt(row), rankForVertTile(r.tiles[row]));
            }

            remaining.setText("Guesses left: " + r.remainingGuesses);
            message.setText("");

            // reset typing for next column
            typedIndex = 0;
            for (int i = 0; i < letters; i++) current[i] = 0;

            // add next column if exists
            if (!r.gameOver && col + 1 < chances) {
                addColumnToGrid(col + 1);
            }

            if (r.gameWon) {
                showWin(backBtn, giveUpBtn);
            } else if (r.gameOver) {
                showLose(backBtn, giveUpBtn, "Out of guesses.");
            }
        };

        // Key handlers
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

        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
        });
    }

    // ----- Grid helpers -----
    private void addColumnToGrid(int col) {
        for (int r = 0; r < letters; r++) {
            if (!grid.getChildren().contains(tiles[r][col])) {
                grid.add(tiles[r][col], col, r);
            }
        }
    }

    // ----- typing goes DOWN the current column -----
    private void typeChar(char ch) {
        if (game.isGameOver()) return;

        int col = game.getTries(); // current try column
        if (col >= chances) return;
        if (typedIndex >= letters) return;

        ch = Character.toUpperCase(ch);
        current[typedIndex] = ch;

        tiles[typedIndex][col].setText(String.valueOf(ch));
        typedIndex++;
    }

    private void backspace() {
        if (typedIndex <= 0) return;

        typedIndex--;
        current[typedIndex] = 0;

        int col = game.getTries();
        tiles[typedIndex][col].setText(" ");
    }

    // ----- styles -----
    private String baseStyle() {
        return "-fx-border-color: #444; -fx-font-size: 22px; -fx-font-weight: bold; -fx-alignment: center;";
    }

    private String styleFor(VerticleLogic.Tile tile) {
        return switch (tile) {
            case GREEN -> "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            case YELLOW -> "-fx-background-color: #C9B458; -fx-text-fill: white;";
            default -> "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };
    }

    // ----- lose/win layouts -----
    private void showLose(Button backBtn, Button giveUpBtn, String msg) {
        Label lost = new Label("You lost. The word was:");
        lost.setStyle("-fx-font-size: 36px;");

        Label answer = new Label(game.getWord());
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
            b.setPrefHeight(40);
            if (k.equals("ENTER")) b.setPrefWidth(110);
            else if (k.equals("⌫")) b.setPrefWidth(70);
            else b.setPrefWidth(44);

            b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

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

    // ----- Keyboard coloring -----
    private int rankForVertTile(VerticleLogic.Tile t) {
        return switch (t) {
            case GREY -> 1;
            case YELLOW -> 2;
            case GREEN -> 3;
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
            default -> base;
        };
    }
}

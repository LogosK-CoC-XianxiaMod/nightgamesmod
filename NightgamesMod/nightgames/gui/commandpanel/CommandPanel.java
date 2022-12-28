package nightgames.gui.commandpanel;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import nightgames.gui.GUIColors;
import nightgames.skills.SkillGroup;
import nightgames.skills.SkillInstance;
import nightgames.skills.Tactics;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandPanel extends JFXPanel {
    private HBox commandPanel;

    private Map<Tactics, SkillGroup> skills;
    private Tactics selectedTactic;
    private SkillInstance selectedSkill;
    private Button submitButton;
    private ToggleGroup buttonGroup;
    private Button backButton;
    private WebView detailText;
    private Node focusTarget;

    public CommandPanel() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            commandPanel = new HBox();
            commandPanel.setAlignment(Pos.CENTER);
            commandPanel.setBackground(new Background(new BackgroundFill(GUIColors.PAINT_BG_DARK, CornerRadii.EMPTY, Insets.EMPTY)));
            commandPanel.setBorder(new Border(new BorderStroke(
                    new Color(0, 0, 0, 0),
                    BorderStrokeStyle.NONE,
                    CornerRadii.EMPTY,
                    BorderStroke.DEFAULT_WIDTHS)));

            ScrollPane scrollPane = new ScrollPane(commandPanel);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setFitToWidth(true);
            scrollPane.setBackground(new Background(new BackgroundFill(GUIColors.PAINT_BG_DARK, CornerRadii.EMPTY, Insets.EMPTY)));

            buttonGroup = new ToggleGroup();
            buttonGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    submitButton.setVisible(true);
                    var data = (CommandPanelData) buttonGroup.getSelectedToggle().getUserData();
                    detailText.setVisible(data.detail.length() > 0);
                    detailText.getEngine().loadContent(((CommandPanelData) buttonGroup.getSelectedToggle().getUserData()).detail);
                } else {
                    submitButton.setVisible(false);
                }
            });

            var buttonBG = new Background(new BackgroundFill(GUIColors.PAINT_BG_GREY, CornerRadii.EMPTY, Insets.EMPTY));
            var buttonFont = new Font("Baskerville Old Face", 18);
            submitButton = new Button();
            submitButton.setText("Do it!");
            submitButton.setTextFill(Color.WHITE);
            submitButton.setBackground(buttonBG);
            submitButton.setFont(buttonFont);
            submitButton.setOnAction(event -> {
                var userData = (CommandPanelData) buttonGroup.getSelectedToggle().getUserData();
                SwingUtilities.invokeLater(() -> userData.action.actionPerformed(null));
            });
            submitButton.setVisible(false);
            submitButton.setAlignment(Pos.CENTER);

            var submitButtonPane = new StackPane(submitButton);

            var self = this;
            backButton = new Button();
            backButton.setText("Something else...");
            backButton.setTextFill(Color.WHITE);
            backButton.setBackground(buttonBG);
            backButton.setFont(buttonFont);
            backButton.setOnAction(event -> {
                self.upOneLevel();
                backButton.setVisible(false);
            });
            backButton.setVisible(false);
            backButton.setAlignment(Pos.CENTER);

            var backButtonPane = new StackPane(backButton);

            var rightButton = new Button();
            rightButton.setText("->");
            rightButton.setTextFill(Color.WHITE);
            rightButton.setBackground(buttonBG);
            rightButton.setFont(buttonFont);
            rightButton.setOnAction(event -> {
                int selectedIndex;
                if (buttonGroup.getSelectedToggle() == null) {
                    selectedIndex = buttonGroup.getToggles().size() / 2;
                } else {
                    selectedIndex = buttonGroup.getToggles().indexOf(buttonGroup.getSelectedToggle());
                }
                var nextSelectedToggle = buttonGroup.getToggles().get((selectedIndex + 1) % buttonGroup.getToggles().size());
                buttonGroup.selectToggle(nextSelectedToggle);
            });
            rightButton.setAlignment(Pos.CENTER);

            var rightButtonPane = new StackPane(rightButton);

            var leftButton = new Button();
            leftButton.setText("<-");
            leftButton.setTextFill(Color.WHITE);
            leftButton.setBackground(buttonBG);
            leftButton.setFont(buttonFont);
            leftButton.setOnAction(event -> {
                int selectedIndex;
                if (buttonGroup.getSelectedToggle() == null) {
                    selectedIndex = buttonGroup.getToggles().size() / 2;
                } else {
                    selectedIndex = buttonGroup.getToggles().indexOf(buttonGroup.getSelectedToggle());
                }
                var nextSelectedToggle = buttonGroup.getToggles().get((selectedIndex - 1 + buttonGroup.getToggles().size()) % buttonGroup.getToggles().size());
                buttonGroup.selectToggle(nextSelectedToggle);
            });
            leftButton.setAlignment(Pos.CENTER);

            var leftButtonPane = new StackPane(leftButton);

            detailText = new WebView();
            var detailPane = new StackPane(detailText);

            var innerPane = new BorderPane();
            innerPane.setCenter(detailPane);
            innerPane.setBottom(scrollPane);

            var outerPane = new BorderPane();
            outerPane.setTop(submitButtonPane);
            outerPane.setLeft(leftButtonPane);
            outerPane.setCenter(innerPane);
            outerPane.setRight(rightButtonPane);
            outerPane.setBottom(backButtonPane);
            outerPane.setOnKeyPressed(event -> {
                System.out.println(event.getCode());
                switch (event.getCode()) {
                    case UP: {
                        if (submitButton.isVisible()) {
                            submitButton.fire();
                        }
                    }
                    break;
                    case RIGHT: {
                        rightButton.fire();
                    }
                    break;
                    case LEFT: {
                        leftButton.fire();
                    }
                    break;
                    case DOWN: {
                        if (backButton.isVisible()) {
                            backButton.fire();
                        }
                    }
                }
            });
            outerPane.setFocusTraversable(false);
            outerPane.setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, 0), CornerRadii.EMPTY, Insets.EMPTY)));
            outerPane.setBorder(new Border(new BorderStroke(
                    new Color(0, 0, 0, 0),
                    BorderStrokeStyle.NONE,
                    CornerRadii.EMPTY,
                    BorderStroke.DEFAULT_WIDTHS)));
            focusTarget = outerPane;

            var scene = new Scene(outerPane);
            scene.setFill(GUIColors.PAINT_BG_DARK);
            setScene(scene);
        });

        setOpaque(false);
        setBorder(new CompoundBorder());

        skills = new HashMap<>();
    }

    public void reset() {
        skills.clear();
        Platform.runLater(() -> {
            submitButton.setVisible(false);
            backButton.setVisible(false);
        });
        clear();
        refresh();
    }

    private void clear() {
        Platform.runLater(() -> {
            submitButton.setVisible(false);
            commandPanel.getChildren().clear();
            buttonGroup.getToggles().clear();
            refresh();
        });
    }

    private void refresh() {
            SwingUtilities.invokeLater(() -> {
                repaint();
                revalidate();
            });
    }

    private void add(List<CommandPanelButton> buttons) {
        Platform.runLater(() -> {
            commandPanel.getChildren().addAll(buttons);
            buttons.forEach(b -> b.setToggleGroup(buttonGroup));
            buttonGroup.selectToggle(buttons.get(buttons.size() / 2));
            focusTarget.requestFocus();
        });
    }

    public void present(List<CommandPanelOption> options) {
        presentNoReset(options.stream()
            .map(option -> option.wrap(event -> reset(), event -> {}))
            .collect(Collectors.toList()));
    }

    public void presentNoReset(List<CommandPanelOption> options) {
        clear();
        add(options.stream().map(CommandPanelOption::toButton).collect(Collectors.toList()));
        refresh();
    }

    void addNoReset(List<CommandPanelOption> options) {
        present(options);
    }

    private void upOneLevel() {
        if (selectedSkill != null) {
            selectedSkill = null;
            switchTactics(selectedTactic);
            return;
        }
        if (selectedTactic != null) {
            switchTactics(null);
        }
        selectedTactic = null;
    }

    void setSelectedSkill(SkillInstance s) {
        selectedSkill = s;
    }

    private void addTactics() {
        add(Arrays.stream(Tactics.values())
            .filter(t -> this.skills.containsKey(t))
            .map(t -> CommandPanelButton.SwitchTacticsButton(t, event -> switchTactics(t)))
            .collect(Collectors.toList()));
    }

    public void chooseSkills(List<SkillGroup> skills) {
        reset();
        if (skills.isEmpty()) {
            throw new IllegalArgumentException("skills cannot be empty");
        }
        skills.forEach(group -> this.skills.put(group.tactics, group));
        addTactics();
        refresh();
    }

    private void switchTactics(Tactics tactics) {
        clear();
        if (tactics != null) {
            add(this.skills.get(tactics).skills.stream()
                .map(skill -> CommandPanelButton.SkillButton(skill, this))
                .collect(Collectors.toList()));
            selectedTactic = tactics;
            backButton.setVisible(true);
        } else {
            addTactics();
        }
        refresh();
    }
}

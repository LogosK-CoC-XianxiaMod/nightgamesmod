package nightgames.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import nightgames.characters.Attribute;
import nightgames.characters.Player;
import nightgames.global.Global;

class GUIPlayerAttributes {

    private Player player;
    private JPanel targetPanel;
    private JPanel panel;
    private JPanel statsPanel;
    private JTextPane appearance;
    private JToggleButton button;
    private Integer fontSize;

    GUIPlayerAttributes(Player player, JPanel targetPanel, GUI refreshTarget) {
        this.player = player;
        this.targetPanel = targetPanel;

        // resolution resolver

        Integer height = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.85);
        Integer width = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.85);
        fontSize = 5;

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        panel.setPreferredSize(new Dimension(width / 8, targetPanel.getHeight()));
        if (width < 720) {
            panel.setMaximumSize(new Dimension(height, width / 6));
        }
        statsPanel = new JPanel(new GridLayout(0, 3));
        panel.add(statsPanel);
        statsPanel.setPreferredSize(new Dimension(width / 8, 200));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(panel.getWidth(), 2));
        panel.add(sep);
        statsPanel.setBackground(GUIColors.bgLight);

        appearance = new JTextPane();
        DefaultCaret caret = (DefaultCaret) appearance.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        appearance.setBackground(GUIColors.bgLight);
        appearance.setEditable(false);
        appearance.setContentType("text/html");

        JScrollPane appearanceScrollPane = new JScrollPane(appearance);
        appearanceScrollPane.setBackground(GUIColors.bgLight);
        appearanceScrollPane.setOpaque(false);
        appearanceScrollPane.getViewport().setOpaque(false);
        JPanel appearancePanel = new JPanel(new GridLayout());
        appearancePanel.setMaximumSize(new Dimension(width / 8, 2000));
        appearancePanel.setPreferredSize(new Dimension(width / 8, 2000));

        appearancePanel.setBackground(GUIColors.bgLight);
        panel.add(appearancePanel);
        appearancePanel.add(appearanceScrollPane);
        panel.setBackground(GUIColors.bgLight);

        button = new JToggleButton("Status");
        button.addActionListener(arg0 -> {
            if (button.isSelected()) {
                targetPanel.add(panel, BorderLayout.EAST);
            } else {
                targetPanel.remove(panel);
            }
            refreshTarget.refresh();
            targetPanel.validate();
        });
    }

    JToggleButton getButton() {
        return button;
    }

    void displayStatus() {

        statsPanel.removeAll();
        int descFontSize = fontSize - 1;
        for (Attribute a : Attribute.values()) {
            int amt = player.get(a);
            int pure = player.getPure(a);
            if (pure > 0 || amt > 0) {
                if (amt == pure) {
                    JLabel label = new JLabel(String
                        .format("<html><font face='Georgia' size=%d>%s: %s</font></html>",
                            descFontSize, a.name(), amt));
                    label.setForeground(GUIColors.textColorLight);
                    statsPanel.add(label);
                } else {
                    String attrColor;
                    String bonusColor;
                    if (amt < pure) {
                        attrColor = "255,100,100";
                        bonusColor = "255,0,0";
                    } else {
                        attrColor = "100,255,255";
                        bonusColor = "0,255,0";
                    }
                    int statBonusFontSize = descFontSize - 1;
                    String labelString = String.format(
                        "<html><font face='Georgia' size=%d>%s: <font color='rgb(%s)'>%d</font> <font size=%d color='rgb(%s)'>(%+d)</font></font></html>",
                        descFontSize, a.name(), attrColor, amt, statBonusFontSize, bonusColor,
                        amt - pure);
                    JLabel label = new JLabel(labelString);
                    label.setForeground(GUIColors.textColorLight);
                    statsPanel.add(label);
                }
            }
        }

        HTMLDocument doc = (HTMLDocument) appearance.getDocument();
        try {
            doc.remove(0, doc.getLength());
            HTMLEditorKit editorKit = (HTMLEditorKit) appearance.getEditorKit();
            Global.freezeRNG();

            editorKit.insertHTML(doc, doc.getLength(),
                "<font face='Georgia' color='white' size='" + descFontSize + "'>"
                    + player.outfit.describe(player) + "<br/>" + player.describeStatus()
                    + "</font><br/>",
                0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
        Global.unfreezeRNG();
    }
}
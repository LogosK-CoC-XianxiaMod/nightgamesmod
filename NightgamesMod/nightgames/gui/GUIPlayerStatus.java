package nightgames.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.JPanel;
import nightgames.characters.Player;

class GUIPlayerStatus {
    private ArrayList<GUICoreStat> meters = new ArrayList<GUICoreStat>();
    private JPanel panel;

    private static GUICoreStat newStaminaMeter(Player player) {
        return new GUICoreStat(
            "Stamina",
                player.stamina,
            new Color(164, 8, 2),
            "Stamina represents your endurance and ability to keep fighting. If it drops to zero, you'll be temporarily stunned."
        );
    }

    private static GUICoreStat newArousalMeter(Player player) {
        return new GUICoreStat(
            "Arousal",
                player.arousal,
            new Color(254, 1, 107),
            "Arousal is raised when your opponent pleasures or seduces you. If it hits your max, you'll orgasm and lose the fight."
        );
    }

    private static GUICoreStat newMojoMeter(Player player) {
        return new GUICoreStat(
            "Mojo",
            player.getMojo(),
            new Color(51, 153, 255),
            "Mojo is the abstract representation of your momentum and style. It increases with normal techniques and is used to power special moves"
        );
    }

    private static GUICoreStat newWillpowerMeter(Player player) {
        return new GUICoreStat(
            "Willpower",
                player.willpower,
            new Color(68, 170, 85),
            "Willpower is a representation of your will to fight. When this reaches 0, you lose."
        );
    }

    GUIPlayerStatus(Player player) {
        GUICoreStat stamina = newStaminaMeter(player);
        GUICoreStat arousal = newArousalMeter(player);
        GUICoreStat mojo = newMojoMeter(player);
        GUICoreStat willpower = newWillpowerMeter(player);

        meters.add(stamina);
        meters.add(arousal);
        meters.add(mojo);
        meters.add(willpower);

        panel = new JPanel();
        panel.setBackground(GUIColors.bgDark);
        panel.setLayout(new GridLayout(0, meters.size(), 0, 0));

        panel.add(stamina.getPanel());
        panel.add(arousal.getPanel());
        panel.add(mojo.getPanel());
        panel.add(willpower.getPanel());
    }

    JPanel getPanel() {
        return panel;
    }

    void refresh() {
        for (GUICoreStat meter : meters) {
            meter.refresh();
        }
    }
}
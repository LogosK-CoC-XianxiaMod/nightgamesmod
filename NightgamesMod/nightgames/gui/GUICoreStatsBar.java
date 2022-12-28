package nightgames.gui;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import nightgames.characters.NPC;

class GUICoreStatsBar extends JPanel {
    private GUICoreStatPanel stamina;
    private GUICoreStatPanel arousal;
    private GUICoreStatPanel mojo;
    private GUICoreStatPanel willpower;

    GUICoreStatsBar() {
        stamina = new GUICoreStatPanel("stamina.png", GUIColors.staminaColor);
        arousal = new GUICoreStatPanel("arousal.png", GUIColors.arousalColor);
        mojo = new GUICoreStatPanel("mojo.png", GUIColors.mojoColor);
        willpower = new GUICoreStatPanel("willpower.png", GUIColors.willpowerColor);

        setOpaque(false);

        var layout = new GroupLayout(this);
        layout.setAutoCreateGaps(false);
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            .addComponent(stamina)
            .addComponent(arousal)
            .addComponent(mojo)
            .addComponent(willpower)
        );
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.CENTER)
            .addComponent(stamina)
            .addComponent(arousal)
            .addComponent(mojo)
            .addComponent(willpower)
        );
        setLayout(layout);
    }

    void setTarget(NPC target) {
        stamina.setTargetMeter(target.stamina);
        arousal.setTargetMeter(target.arousal);
        mojo.setTargetMeter(target.getMojo());
        willpower.setTargetMeter(target.willpower);
    }

    void refresh() {
        stamina.refresh();
        arousal.refresh();
        mojo.refresh();
        willpower.refresh();
    }

    void clear() {
        stamina.clear();
        arousal.clear();
        mojo.clear();
        willpower.clear();
    }
}

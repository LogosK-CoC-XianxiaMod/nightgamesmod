package nightgames.characters.body.mods.catcher;

import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.mods.pitcher.CockMod;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.status.Trance;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class PlantMod extends CatcherMod {
    public static final String TYPE = "plant";

    public PlantMod() {
        super(TYPE, .3, 1, .2);
    }

    public double applyReceiveBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) {
        if (damage > self.arousal.max()/ 5.0 && Global.random(4) == 0) {
            var model = JtwigModel.newModel()
                .with("self", self)
                .with("opponent", opponent)
                .with("part", part);
            c.write(self, RECEIVE_TEMPLATE.render(model));
            opponent.add(c, new Trance(opponent));
        }
        return 0;
    }

    public void onStartPenetration(Combat c, Character self, Character opponent, BodyPart part, BodyPart target) {}

    public void tickHolding(Combat c, Character self, Character opponent, BodyPart part, BodyPart target) {
        if (c.getStance().isPartFuckingPartInserted(c, opponent, target, self, part)) {
            JtwigModel model = JtwigModel.newModel()
                .with("self", self)
                .with("opponent", opponent)
                .with("part", part)
                .with("target", target);
            c.write(self, TICK_HOLDING_TEMPLATE.render(model));
            opponent.drainStaminaAsMojo(c, self, 20, 1.25f);
            opponent.loseWillpower(c, 5);
        }
    }

    @Override
    public String describeAdjective(String partType) {
        return "floral appearance";
    }

    private static final JtwigTemplate RECEIVE_TEMPLATE = JtwigTemplate.inlineTemplate(
        "An intoxicating scent emanating from {{ self.possessiveAdjective() }} "
            + "{{ part.describe(self) }} leaves {{ opponent.objectPronoun() }} in a "
            + "trance!"
    );

    private static final JtwigTemplate TICK_HOLDING_TEMPLATE = JtwigTemplate.inlineTemplate(
        "The small rough fibrous filaments inside {{ self.nameOrPossessivePronoun() }} "
            + "flower {{ part.getType() }} wrap around "
            + "{{ other.nameOrPossessivePronoun() }} cock. A profound exhaustion settles "
            + "on {{ other.objectPronoun() }}, as {{ other.subject() }} "
            + "{{ other.action('feel') }} {{ self.nameOrPossessivePronoun() }} insidious "
            + "flower leeching {{ other.possessiveAdjective() }} strength.");

    @Override
    public CockMod getCorrespondingCockMod() {
        throw new UnsupportedOperationException();
    }
}

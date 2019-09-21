package nightgames.characters.body.mods;

import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.CockMod;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.status.DivineCharge;
import nightgames.status.Stsflag;

public class BlessedCockMod extends CockMod {
    public static final String TYPE = "blessed";

    public BlessedCockMod() {
        super(TYPE, 1.0, 1.0, .75);
    }

    @Override
    public double applyBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) {
        double bonus = super.applyBonuses(c, self, opponent, part, target, damage);
        if (target.isType("cock")) {
            if (self.getStatus(Stsflag.divinecharge) != null) {
                c.write(self, Global.format(
                    "{self:NAME-POSSESSIVE} concentrated divine energy in {self:possessive} cock rams into {other:name-possessive} pussy, sending unimaginable pleasure directly into {other:possessive} soul.",
                    self, opponent));
            }
            // no need for any effects, the bonus is in the pleasure mod
        }
        return bonus;
    }

    public double applyReceiveBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) {
        if (c.getStance().inserted(self)) {
            DivineCharge charge = (DivineCharge) self.getStatus(Stsflag.divinecharge);
            if (charge == null) {
                c.write(self, Global.format(
                    "{self:NAME-POSSESSIVE} " + part.fullDescribe(self)
                        + " radiates a golden glow as {self:subject-action:groan|groans}. "
                        + "{other:SUBJECT-ACTION:realize|realizes} {self:subject-action:are|is} feeding on {self:possessive} own pleasure to charge up {self:possessive} divine energy.",
                    self, opponent));
                self.add(c, new DivineCharge(self, .25));
            } else {
                c.write(self, Global.format(
                    "{self:SUBJECT-ACTION:continue|continues} feeding on {self:possessive} own pleasure to charge up {self:possessive} divine energy.",
                    self, opponent));
                self.add(c, new DivineCharge(self, charge.magnitude));
            }
        }
        return 0;
    }

    @Override
    public void onStartPenetration(Combat c, Character self, Character opponent, BodyPart part, BodyPart target) {
        if (target.isErogenous()) {
            if (!self.human()) {
                c.write(self, Global.format(
                    "As soon as {self:subject} penetrates you, you realize you're screwed. Both literally and figuratively. While it looks innocuous enough, {self:name-possessive} {self:body-part:cock} "
                        + "feels like pure ecstasy. {self:SUBJECT} hasn't even begun moving yet, but {self:possessive} cock simply sitting within you radiates a heat that has you squirming uncontrollably.",
                    self, opponent));
            }
        }
    }

    @Override
    public String describeAdjective(String partType) {
            return "holy aura";
    }
}
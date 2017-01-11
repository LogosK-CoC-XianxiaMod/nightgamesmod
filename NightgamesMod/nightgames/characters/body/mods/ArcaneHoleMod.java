package nightgames.characters.body.mods;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.CockMod;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.pet.PetCharacter;
import nightgames.status.Enthralled;

public class ArcaneHoleMod extends PartMod {
    public ArcaneHoleMod() {
        super("arcane", .05, .1, 0, -5);
    }

    public double applyBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) { 
        int strength;
        boolean fucking = c.getStance().isPartFuckingPartInserted(c, opponent, target, self, part);
        if (!target.moddedPartCountsAs(opponent, CockMod.bionic)) {
            String message;
            if (target.moddedPartCountsAs(opponent, CockMod.primal)) {
                message = String.format(
                                "The tattoos around %s %s flare up with a new intensity, responding to the energy flow from %s %s."
                                                + " The magic within them latches onto it and pulls fiercly, drawing %s strength into %s with great gulps.",
                                self.nameOrPossessivePronoun(), part.describe(self), opponent.nameOrPossessivePronoun(),
                                target.describe(opponent), opponent.possessiveAdjective(), self.directObject());
                strength = 10 + self.get(Attribute.Arcane) / 4;
            } else {
                message = self.nameOrPossessivePronoun() + " tattoos surrounding " + self.possessiveAdjective()
                                + " " + part.getType() + " light up with arcane energy as " + 
                                (fucking ? opponent.subjectAction("are", "is") + " inside " + self.directObject() : self.subjectAction("touch") + " " + opponent.directObject()) + ", channeling some of "
                                + opponent.possessiveAdjective() + " energies back to its master.";
                strength = 5 + self.get(Attribute.Arcane) / 6;
            }
            c.write(self, message);
            opponent.drainMojo(c, self, strength);
            if (self.isPet()) {
                Character master = ((PetCharacter) self).getSelf().owner();
                c.write(self, Global.format("The energy seems to flow through {self:direct-object} and into {self:possessive} {other:master}.", self, master));
                master.buildMojo(c, strength);
            }
            if (Global.random(8) == 0 && !opponent.wary()) {
                c.write(self, " The light seems to seep into " + opponent.possessiveAdjective() + " "
                                + target.describe(opponent) + ", leaving " + opponent.directObject() + " enthralled to "
                                + self.possessiveAdjective() + " will.");
                opponent.add(c, new Enthralled(opponent, self, 3));
            }
        } else {
            String message = String.format(
                            "%s tattoos shine with an eldritch light, but they do not seem to be able to affect %s only partially-organic %s",
                            self.nameOrPossessivePronoun(), opponent.nameOrPossessivePronoun(),
                            target.describe(opponent));
            c.write(self, message);
        }
        return 0;
    }

    public int counterValue(BodyPart part, BodyPart otherPart, Character self, Character other) { 
        return otherPart.moddedPartCountsAs(other, CockMod.primal) ? 1 : otherPart.moddedPartCountsAs(other, CockMod.bionic) ? -1 : 0;
    }
}

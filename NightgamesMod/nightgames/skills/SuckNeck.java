package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.SkinPart;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.nskills.tags.SkillTag;
import nightgames.skills.damage.DamageType;
import nightgames.stance.Stance;

public class SuckNeck extends Skill {

    public SuckNeck(Character self) {
        super("Suck Neck", self);
        addTag(SkillTag.usesMouth);
        addTag(SkillTag.pleasure);
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return (c.getStance().kiss(getSelf(), target) 

                        || (getSelf().get(Attribute.Dark) >= 1 
                            && c.getStance().dom(getSelf()) 
                            && c.getStance().en == Stance.oralpin
                            ))

                        && getSelf().canAct();
    }

    @Override
    public int getMojoBuilt(Combat c) {
        return 7;
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        if (target.roll(getSelf(), c, accuracy(c, target))) {
            if (getSelf().get(Attribute.Dark) >= 1) {
                int m = target.stamina.max() / 8;
                if (c.getStance().en == Stance.oralpin) {
                    writeOutput(c, Result.critical, target);
                    m *= 1.25;
                    target.drain(c, getSelf(), (int) getSelf().modifyDamage(DamageType.drain, target, m));
                    target.body.pleasure(getSelf(), getSelf().body.getRandomMouth(),
                                    c.getStance().bottomParts().get(0), m, c);
                    return true;
                } else {
                    writeOutput(c, Result.special, target);
                    target.drain(c, getSelf(), (int) getSelf().modifyDamage(DamageType.drain, target, m));
                }
            } else {
                writeOutput(c, Result.normal, target);
            }
            int m = 1 + Global.random(8);
            target.body.pleasure(getSelf(), getSelf().body.getRandomMouth(), target.body.getRandom(
                SkinPart.TYPE), m, c, this);
        } else {
            writeOutput(c, Result.miss, target);
            return false;
        }
        return true;
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return getSelf().get(Attribute.Seduction) >= 12;
    }

    @Override
    public Skill copy(Character user) {
        return new SuckNeck(user);
    }

    @Override
    public int speed() {
        return 5;
    }

    @Override
    public int accuracy(Combat c, Character target) {
        return c.getStance().dom(getSelf()) ? 100 : 70;
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.pleasure;
    }

    @Override
    public String getLabel(Combat c) {
        if (getSelf().get(Attribute.Dark) >= 1) {
            return "Drain energy";
        } else {
            return getName(c);
        }
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return "You lean in to kiss " + target.getName() + "'s neck, but she slips away.";
        } else if (modifier == Result.special) {
            return "You draw close to " + target.getName()
                            + " as she's momentarily too captivated to resist. You run your tongue along her neck and bite gently. She shivers and you "
                            + "can feel the energy of her pleasure flow into you, giving you strength.";
        } else if (modifier == Result.critical) {
            if (target.hasDick()) {
                return Global.format("You lock your lips around {other:name-possessive}"
                                + " {other:body-part:cock} and <i>inhale</i> sharply."
                                + " {other:PRONOUN-ACTION:groan} as {other:possessive} energy"
                                + " is drawn out through {other:possessive} shaft.",
                                getSelf(), target);
            } else {
                return Global.format("You purse your lips and lock them onto {other:name-possessive}"
                                + " clit. Then you <i>inhale</i>, sucking out {other:possessive}"
                                + " energy, at the same time eliciting a groan of pleasure"
                                + " from {other:direct-object}.", getSelf(), target);
            }
        } else {
            return "You lick and suck " + target.getName() + "'s neck hard enough to leave a hickey.";
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return String.format("%s goes after %s neck, but %s %s %s back.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.pronoun(), target.action("push", "pushes"),
                            getSelf().possessiveAdjective());
        } else if (modifier == Result.special) {
            return String.format("%s presses %s lips against %s neck. %s gives %s a "
                            + "hickey and %s knees start to go weak. It's like %s strength"
                            + " is being sucked out through "
                            + "%s skin.", getSelf().subject(), getSelf().possessiveAdjective(),
                            target.nameOrPossessivePronoun(), getSelf().subject(),
                            target.objectPronoun(), target.possessiveAdjective(), target.possessiveAdjective(),
                            target.possessiveAdjective());
        } else if (modifier == Result.critical) {
            if (target.hasDick()) {
                return Global.format("Even with {self:possessive} lips around your"
                                + " {other:body-part:cock}, {self:subject-action:manage}"
                                + " to give you a smile. Then, {self:pronoun-action:suck}"
                                + " powerfully. It feels even better than a blowjob normally"
                                + " would, and as the suction dies down a profound lethargy"
                                + " settles over you. {self:PRONOUN} sucked your energy right"
                                + " out of you!",
                                getSelf(), target);
            } else {
                return Global.format("{self:NAME-POSSESSIVE} tongue stops moving for a blessed"
                                + " second, giving you a chance to catch your breath. But then"
                                + " {self:pronoun} sucks your clit into {self:possessive} mouth,"
                                + " flooding with yet more pleasure. There's more to it, though."
                                + " You feel sluggish and weak, as if {self:subject} somehow"
                                + " inhaled your energy when {self:pronoun} sucked. Which"
                                + " {self:pronoun} probably did.", getSelf(), target);
            }
        } else {
            return String.format("%s licks and sucks %s neck, biting lightly when %s %s expecting it.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.pronoun(), target.action("aren't", "isn't"));
        }
    }

    @Override
    public String describe(Combat c) {
        return "Suck on opponent's neck. Highly variable effectiveness";
    }

    @Override
    public boolean makesContact(Combat c) {
        return true;
    }
    
    @Override
    public Stage getStage() {
        return Stage.FOREPLAY;
    }
}

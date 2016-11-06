package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.nskills.tags.SkillTag;
import nightgames.skills.damage.DamageType;
import nightgames.stance.Mount;

public class Tackle extends Skill {

    public Tackle(Character self) {
        super("Tackle", self);

        addTag(SkillTag.positioning);
        addTag(SkillTag.hurt);
        addTag(SkillTag.staminaDamage);
        addTag(SkillTag.knockdown);
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return !target.wary() && c.getStance().mobile(getSelf()) && c.getStance().mobile(target)
                        && !c.getStance().prone(getSelf()) && getSelf().canAct();
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        if (target.roll(this, c, accuracy(c))
                        && getSelf().check(Attribute.Power, target.knockdownDC() - getSelf().get(Attribute.Animism))) {
            if (getSelf().get(Attribute.Animism) >= 1) {
                writeOutput(c, Result.special, target);
                target.pain(c, (int) getSelf().modifyDamage(DamageType.physical, target, Global.random(15, 30)));
            } else {
                writeOutput(c, Result.normal, target);
                target.pain(c, (int) getSelf().modifyDamage(DamageType.physical, target, Global.random(10, 25)));
            }
            c.setStance(new Mount(getSelf(), target));
        } else {
            writeOutput(c, Result.miss, target);
            return false;
        }
        return true;
    }

    @Override
    public int getMojoCost(Combat c) {
        return 15;
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.get(Attribute.Power) >= 26 && !user.has(Trait.petite) || user.get(Attribute.Animism) >= 1;
    }

    @Override
    public Skill copy(Character user) {
        return new Tackle(user);
    }

    @Override
    public int speed() {
        if (getSelf().get(Attribute.Animism) >= 1) {
            return 3;
        } else {
            return 1;
        }
    }

    @Override
    public int accuracy(Combat c) {
        int base = 80;
        if (getSelf().get(Attribute.Animism) >= 1) {
            base = 120 + (getSelf().getArousal().getReal() / 10);
        }
        return Math.round(Math.max(Math.min(150,
                        2.5f * (getSelf().get(Attribute.Power) - c.getOther(getSelf()).get(Attribute.Power)) + base),
                        40));
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.positioning;
    }

    @Override
    public String getLabel(Combat c) {
        if (getSelf().get(Attribute.Animism) >= 1) {
            return "Pounce";
        } else {
            return getName(c);
        }
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.special) {
            return "You let your instincts take over and you pounce on " + target.name()
                            + " like a predator catching your prey.";
        } else if (modifier == Result.normal) {
            return "You tackle " + target.name() + " to the ground and straddle her.";
        } else {
            return "You lunge at " + target.name() + ", but she dodges out of the way.";
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.special) {
            return String.format("%s wiggles her butt cutely before leaping at %s and pinning %s to the floor.",
                            getSelf().subject(), target.nameDirectObject(), target.directObject());
        }
        if (modifier == Result.miss) {
            return String.format("%s tries to tackle %s, but %s %s out of the way.",
                            getSelf().subject(), target.nameDirectObject(),
                            target.pronoun(), target.action("sidestep"));
        } else {
            return String.format("%s bowls %s over and sits triumphantly on %s chest.",
                            getSelf().subject(), target.nameDirectObject(), target.possessivePronoun());
        }
    }

    @Override
    public String describe(Combat c) {
        return "Knock opponent to ground and get on top of her";
    }

    @Override
    public boolean makesContact() {
        return true;
    }
}

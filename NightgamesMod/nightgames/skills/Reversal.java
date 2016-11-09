package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Emotion;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.nskills.tags.SkillTag;
import nightgames.stance.Anal;
import nightgames.stance.AnalCowgirl;
import nightgames.stance.Cowgirl;
import nightgames.stance.Doggy;
import nightgames.stance.Missionary;
import nightgames.stance.Pin;
import nightgames.stance.ReverseCowgirl;
import nightgames.stance.Stance;

public class Reversal extends Skill {

    public Reversal(Character self) {
        super("Reversal", self);
        addTag(SkillTag.escaping);
        addTag(SkillTag.positioning);
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return !target.wary() && !c.getStance().mobile(getSelf()) && c.getStance().sub(getSelf()) && getSelf().canAct();
    }

    @Override
    public int getMojoCost(Combat c) {
        return 20;
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        if (target.roll(this, c, accuracy(c))) {
            writeOutput(c, Result.normal, target);
/*
            if (c.getStance().en==Stance.behind) {c.setStance(new Pin(getSelf(), target));}
            if (c.getStance().en==Stance.missionary) {c.setStance(new Cowgirl(getSelf(), target));}
            if (c.getStance().en==Stance.cowgirl) {c.setStance(new Missionary(getSelf(), target));}
            if (c.getStance().en==Stance.anal && c.getStance().anallyPenetrated(getSelf())) {c.setStance(new AnalCowgirl(getSelf(), target));}
            if (c.getStance().en==Stance.anal && c.getStance().anallyPenetrated(target)) {c.setStance(new Anal(getSelf(), target));}
            if (c.getStance().en==Stance.reversecowgirl) {c.setStance(new Doggy(getSelf(), target));}
            if (c.getStance().en==Stance.doggy) {c.setStance(new ReverseCowgirl(getSelf(), target));}
*/
            c.setStance(c.getStance().reverse(c));

            target.emote(Emotion.nervous, 10);
            getSelf().emote(Emotion.dominant, 10);
        } else {
            writeOutput(c, Result.miss, target);
            return false;
        }
        return true;
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.get(Attribute.Cunning) >= 24;
    }

    @Override
    public Skill copy(Character user) {
        return new Reversal(user);
    }

    @Override
    public int speed() {
        return 4;
    }

    @Override
    public int accuracy(Combat c) {
        return Math.round(Math.max(Math.min(150,
                        2.5f * (getSelf().get(Attribute.Cunning) - c.getOther(getSelf()).get(Attribute.Cunning)) + 75),
                        40));
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.positioning;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return "You try to get on top of " + target.name()
                            + ", but she's apparently more ready for it than you realized.";
        } else {
            return "You take advantage of " + target.name() + "'s distraction and put her in a pin.";
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return String.format("%s tries to reverse %s hold, but %s %s %s.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.pronoun(), target.action("stop"),
                            getSelf().directObject());
        } else {
            return String.format("%s rolls %s over and ends up on top.",
                            getSelf().subject(), target.nameDirectObject());
        }
    }

    @Override
    public String describe(Combat c) {
        return "Take dominant position: 10 Mojo";
    }

    @Override
    public boolean makesContact() {
        return true;
    }
}

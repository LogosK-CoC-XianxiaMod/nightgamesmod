package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.stance.Anal;
import nightgames.status.Shamed;
import nightgames.status.addiction.Addiction;
import nightgames.status.addiction.AddictionType;

public class OfferAss extends Skill {

    public OfferAss(Character self) {
        super("Offer Ass", self);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return getSelf().getPure(Attribute.Submissive) >= 10 && getSelf().has(Trait.oiledass) && getSelf().hasPussy();
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return (target.pantsless() || target.has(Trait.strapped)) && c.getStance().mobile(target)
                        && !c.getStance().mobile(getSelf())
                        && (target.hasDick() || target.has(Trait.strapped))
                        && getSelf().canAct() && target.canAct() && !c.getStance().inserted();
    }

    @Override
    public String describe(Combat c) {
        Character other = c.getOpponentCharacter(getSelf());
        return "Offer your ass to " + other.possessiveAdjective() + "'s " + other.body.getRandomInsertable().describe(other);
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        if (target.arousal.get() < 15) {
            writeOutput(c, Result.miss, target);
            getSelf().add(c, new Shamed(getSelf()));
            if (target.hasDick() || target.has(Trait.strapped)) {
                new Spank(target).resolve(c, getSelf());
            }
            return false;
        }
        if (target.hasDick() || target.has(Trait.strapped)) {
            // offer ass to dick/strapon
            writeOutput(c, Result.anal, target);
            c.setStance(new Anal(target, getSelf()), target, true);
            getSelf().body.pleasure(target, target.body.getRandomInsertable(), getSelf().body.getRandomAss(),
                            Global.random(5) + getSelf().get(Attribute.Perception), c, this);
            if (!target.has(Trait.strapped)) {
                target.body.pleasure(getSelf(), getSelf().body.getRandomAss(), target.body.getRandomCock(),
                                Global.random(5) + getSelf().get(Attribute.Perception), c, this);
            }
        }

        if (getSelf().checkAddiction(AddictionType.MIND_CONTROL, target)) {
            getSelf().unaddictCombat(AddictionType.MIND_CONTROL, 
                            target, Addiction.LOW_INCREASE, c);
            c.write(getSelf(), "Acting submissively voluntarily reduces Mara's control over " + getSelf().nameDirectObject());
        }
        return true;
    }

    @Override
    public Skill copy(Character user) {
        return new OfferAss(user);
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.misc;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        switch (modifier) {
            case anal:
                return String.format(
                                "You get on the ground with "
                                                + "your shoulders on the ground and your ass in the air, pointing towards %s."
                                                + " Reaching back, you spread your butt and softly whimper an invitation for %s"
                                                + " to stick %s %s into your ass. %s takes pity on you, and plunges in.",
                                target.getName(), target.objectPronoun(), target.possessiveAdjective(),
                                target.body.getRandomInsertable().describe(target),
                                Global.capitalizeFirstLetter(target.pronoun()));
            default: // special
                return String.format(
                                "You lay down on your back and spread your legs,"
                                                + "offering your %s to %s while gazing up at %s powerful %s, hoping "
                                                + "that %s will soon fill you with it.",
                                getSelf().body.getRandomPussy().describe(getSelf()), target.getName(),
                                target.possessiveAdjective(), target.body.getRandomInsertable().describe(target),
                                target.pronoun());
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        switch (modifier) {
            case miss:
                return String.format(
                                "%s gets down and sticks %s ass in the air, offering it up to %s. "
                                                + "%s not interested, however, and just %s %s. %s seemed to enjoy that, but"
                                                + " is still disappointed over not getting the fucking %s wanted.",
                                getSelf().getName(), getSelf().possessiveAdjective(), target.nameDirectObject(),
                                Global.capitalizeFirstLetter(target.subjectAction("are","is")),
                                target.action("spank"), getSelf().objectPronoun(),
                                Global.capitalizeFirstLetter(getSelf().pronoun()), getSelf().pronoun());
            case anal:
                return String.format(
                                "%s gets on the ground and spreads %s ass for %s viewing pleasure,"
                                                + " practically begging %s to fuck %s. Well, someone has to do it. %s on %s"
                                                + " knees and %s to it.",
                                getSelf().getName(), getSelf().possessiveAdjective(), target.nameOrPossessivePronoun(),
                                target.objectPronoun(), getSelf().objectPronoun(),
                                Global.capitalizeFirstLetter(target.subjectAction("get")),
                                target.possessiveAdjective(), target.action("get"));
            default: // special
                return String.format(
                                "%s lays flat on %s back, spreading %s legs and then using"
                                                + " %s fingers to open up %s labia to %s. %s beady eyes, staring longingly"
                                                + " at %s %s overwhelm %s, and %s can't help but oblige, getting "
                                                + "between %s legs and sheathing %s shaft.",
                                getSelf().getName(), getSelf().possessiveAdjective(), getSelf().possessiveAdjective(),
                                getSelf().possessiveAdjective(), getSelf().possessiveAdjective(),
                                target.nameDirectObject(),
                                Global.capitalizeFirstLetter(getSelf().possessiveAdjective()), target.possessiveAdjective(),
                                target.body.getRandomCock().describe(target), target.objectPronoun(),
                                target.pronoun(), getSelf().possessiveAdjective(), getSelf().possessiveAdjective());
        }
    }
}

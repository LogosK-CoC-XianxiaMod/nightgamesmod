package nightgames.skills;

import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.items.clothing.ClothingSlot;
import nightgames.stance.Stance;

public class FondleBreasts extends Skill {

    public FondleBreasts(Character self) {
        super("Fondle Breasts", self);
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return c.getStance().reachTop(getSelf()) && target.hasBreasts() && getSelf().canAct();
    }

    @Override
    public int getMojoBuilt(Combat c) {
        return 7;
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        int m = 6 + Global.random(4);
        Result result = Result.normal;
        if (target.roll(getSelf(), c, accuracy(c, target))) {

            if (getSelf().type.equals("Cassie") && target.body.getRandomBreasts().getSensitivity(target) > 4) {

                result=Result.critical;
                //c.write(getSelf(), deal(c, m, Result.critical, target));
            } else if (target.breastsAvailable()) {
                m += 4;
                result = Result.strong;
            } else if (target.outfit.getTopOfSlot(ClothingSlot.top).getLayer() <= 1 && getSelf().has(Trait.dexterous)) {
                m += 4;
                result = Result.special;
            }
        } else {
            writeOutput(c, Result.miss, target);
            return false;
        }

        writeOutput(c, result, target);
        target.body.pleasure(getSelf(), getSelf().body.getRandomHands(), target.body.getRandomBreasts(), m,
                        c, this);

        return true;
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return true;
    }

    @Override
    public Skill copy(Character user) {
        return new FondleBreasts(user);
    }

    @Override
    public int speed() {
        return 6;
    }

    @Override
    public int accuracy(Combat c, Character target) {
        return c.getStance().en == Stance.neutral ? 70 : 100;
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.pleasure;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return "You grope at " + target.getName() + "'s breasts, but miss. (Maybe you should get closer?)";
        } else if (modifier == Result.strong) {
            return "You massage " + target.getName()
            + "'s soft breasts and pinch her nipples, causing her to moan with desire.";
        } else if (modifier == Result.special) {
            return "You slip your hands into " + target.nameOrPossessivePronoun() + " " + target.outfit.getTopOfSlot(ClothingSlot.top).getName() + ", massaging " + target.getName()
            + "'s soft breasts and pinching her nipples.";
        } else {
            return "You massage " + target.getName() + "'s breasts over her "
                            + target.outfit.getTopOfSlot(ClothingSlot.top).getName() + ".";
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return String.format("%s gropes at %s %s, but misses the mark.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.body.getRandomBreasts().describe(target));
        } else if (modifier == Result.critical) {
            return "Cassie grabs your "+ target.body.getRandomBreasts().describe(target)+" and caresses them, pinching your exquisitely sensitive nipples. An incredibly"
                            + " sensual moan is torn from your lips as you instinctively arch your back, pushing your breasts and rock-hard, massive nipples into Cassie's hands. \"Damn, you "
                            + "really are a total cow-slut now, this is great!\" Cassie exclaims. \"I'll have to see if I can train you to moo though, that would be even hotter"
                            + " than you just moaning. I can't wait to see what happens when I jack up your sensitivity some more.\" She leers at you, a look that seems out of place"
                            + " on her usually-demure face. \"The Benefactor said I can't pump up your breasts until they're so big you can't move, but I've been talking with him"
                            + " and I think he's going to let me do it if I can make sure it's temporary. Doesn'\t that sound amazing!\" She punctuates her comment with a tug on"
                            + " your nipples and a finger rubbing across your aereolae that draws a shockingly loud moan from your mouth that sounds a lot like an affirmation.";
        } else if (modifier == Result.strong) {
            return String.format("%s massages %s %s, and pinches %s nipples, causing %s to moan with desire.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.body.getRandomBreasts().describe(target),
                            target.possessiveAdjective(), target.objectPronoun());
        } else if (modifier == Result.special) {
            return Global.format("{self:SUBJECT-ACTION:slip|slips} {self:possessive} agile fingers into {other:name-possessive} bra, massaging and pinching at {other:possessive} nipples.",
                            getSelf(), target);
        } else {
            return String.format("%s massages %s %s over %s %s.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            target.body.getRandomBreasts().describe(target), target.possessiveAdjective(),
                            target.outfit.getTopOfSlot(ClothingSlot.top).getName());
        }
    }

    @Override
    public String describe(Combat c) {
        return "Grope your opponents breasts. More effective if she's topless";
    }

    @Override
    public String getLabel(Combat c) {
        return c.getOpponentCharacter(getSelf()).body.getRandomBreasts().getSize().compareTo(Size.FlatChest) > 0 ? "Fondle Breasts"
                        : "Tease Chest";
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

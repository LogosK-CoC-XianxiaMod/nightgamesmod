package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.PussyPart;
import nightgames.characters.body.TailPart;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.status.BodyFetish;

public class TailJob extends Skill {

    public TailJob(Character self) {
        super("Tailjob", self);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        boolean enough = getSelf().get(Attribute.Seduction) >= 20 || getSelf().get(Attribute.Animism) >= 1;
        return enough && user.body.getRandom(TailPart.TYPE) != null;
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return getSelf().canAct() && target.crotchAvailable() && c.getStance().mobile(getSelf())
                        && !c.getStance().mobile(target) && !c.getStance().inserted(target);
    }

    @Override
    public String describe(Combat c) {
        return "Use your tail to tease your opponent";
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        writeOutput(c, Result.normal, target);
        int m = (5 + Global.random(10))
                        + Math.min(getSelf().arousal.getReal() / 20, getSelf().get(Attribute.Animism));
        String receiver;
        if (target.hasDick()) {
            receiver = CockPart.TYPE;
        } else {
            receiver = PussyPart.TYPE;
        }
        if (Global.random(100) < 5 + 2 * getSelf().get(Attribute.Fetish)) {
            target.add(c, new BodyFetish(target, getSelf(), TailPart.TYPE, .25));
        }
        target.body.pleasure(getSelf(), getSelf().body.getRandom(TailPart.TYPE), target.body.getRandom(receiver), m, c, this);
        return true;
    }

    @Override
    public Skill copy(Character user) {
        return new TailJob(user);
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.pleasure;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (target.hasDick()) {
            return "You skillfully use your flexible " + getSelf().body.getRandom(TailPart.TYPE).describe(getSelf())
                            + " to stroke and tease " + target.getName() + "'s sensitive girl-cock.";
        } else {
            return "You skillfully use your flexible " + getSelf().body.getRandom(TailPart.TYPE).describe(getSelf())
                            + " to stroke and tease " + target.getName() + "'s sensitive girl parts.";
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (target.hasDick()) {
            return String.format("%s teases %s sensitive dick and balls with %s %s. "
                            + "It wraps completely around %s shaft and strokes firmly.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            getSelf().possessiveAdjective(),
                            getSelf().body.getRandom(TailPart.TYPE).describe(getSelf()),
                            target.possessiveAdjective());
        } else {
            return String.format("%s teases %s sensitive pussy with %s %s. "
                            + "It runs along %s nether lips and leaves %s gasping.",
                            getSelf().subject(), target.nameOrPossessivePronoun(),
                            getSelf().possessiveAdjective(),
                            getSelf().body.getRandom(TailPart.TYPE).describe(getSelf()),
                            target.possessiveAdjective(), target.objectPronoun());
        }
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

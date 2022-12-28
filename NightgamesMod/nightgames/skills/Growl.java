package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Player;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.stance.Behind;
import nightgames.stance.StandingOver;
import nightgames.status.Stsflag;
import nightgames.status.addiction.AddictionType;

public class Growl extends Skill {

    public Growl(Character self) {
        super("Growl", self);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.get(Attribute.Animism) >= 15;
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return !(target.is(Stsflag.wary) || target.is(Stsflag.charmed)) && getSelf().canAct() && getSelf().crotchAvailable() && target.crotchAvailable()
                        && c.getStance().mobile(getSelf()) && getSelf().arousal.percent() >= 20 && (target instanceof Player) && target
            .checkAddiction(AddictionType.BREEDER);
    }
    
    @Override
    public float priorityMod(Combat c) {
        return 5.0f;
    }

    @Override
    public String describe(Combat c) {
        return "Growl threateningly to intimidate your opponent into submitting to sex";
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        if (Global.random(target.getProgression().getLevel()) <= getSelf().get(Attribute.Animism) * ((Player)target).getAddiction(AddictionType.BREEDER).getMagnitude()
                        * target.arousal.percent() / 100 && !target.wary()) {
            int damage = getSelf().arousal.getReal() / 10;
            if (damage < 10) {
                damage = 0;
            }
            target.temptNoSource(c, getSelf(), damage, this);
            if (getSelf().hasDick() && target.hasPussy() && Global.random(3)<2) {c.setStance(new Behind(getSelf(), target));writeOutput(c, damage, Result.reverse, target);}
            else {c.setStance(new StandingOver(getSelf(), target));writeOutput(c, damage, Result.normal, target);}
        } else {
            writeOutput(c, Result.miss, target);
            return false;
        }
        return true;
    }

    @Override
    public Skill copy(Character user) {
        return new Growl(user);
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.debuff;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return "You let out a soft purr and give " + target.getName()
                            + " your best puppy dog eyes. She smiles, but then aims a quick punch at your groin, which you barely avoid. "
                            + "Maybe you shouldn't have mixed your animal metaphors.";
        } else {
            String message = "You give " + target.getName()
                            + " an affectionate purr and your most disarming smile. Her battle aura melts away and she pats your head, completely taken with your "
                            + "endearing behavior.";
            if (damage > 0) {
                message += "\nSome of your apparent arousal seems to have affected her, her breath seems shallower than before.";
            }
            return message;
        }
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.miss) {
            return String.format("%s growls assertively and thrusts out %s hips, eyes fixed on %s crotch, clearly demanding that %s %s for breeding. Thankfully, %s"
                            + " instincts and arounsal aren't strong enough to lead %s to submit.", getSelf().getName(), getSelf().possessivePronoun(), target.possessivePronoun(),
                            getSelf().subject(), getSelf().action("submit"), getSelf().possessivePronoun(), getSelf().pronoun());
        } else if (modifier == Result.normal) {
            return String.format("Kat growls assertively and thrusts out her hips, eyes fixed on your crotch, clearly demanding that you submit for breeding. You try to resist "
                            + "your instincts to submit, but your body yields to Kat's demands, and you fall onto your back, offering yourself up to her.",
                            getSelf().subject(), target.nameDirectObject(),
                            getSelf().pronoun(), getSelf().objectPronoun(), target.subject(),
                            target.pronoun(), target.action("enjoy"), getSelf().possessivePronoun());
        } else if (modifier == Result.reverse) {
            return String.format("Kat growls assertively and thrusts out her hips, eyes fixed on your crotch, clearly demanding that you submit for breeding. You try to resist "
                            + "your instincts to submit, but your body yields to Kat's demands, and you drop to your hands and knees, spreading your legs and lifting your hips, offering up your "
                            + "pussy so she can penetrate you from behind. She quickly takes advantage of your position, and grabs you from behind, rubbing her errect girl-dick"
                            + " against the opening to your breeding hole.", getSelf().subject());
        }
        return "something broke: invalid Result in growl";
    }
}

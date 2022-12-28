package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Emotion;
import nightgames.characters.Trait;
import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.nskills.tags.SkillTag;
import nightgames.skills.damage.DamageType;
import nightgames.stance.BreastSmothering;
import nightgames.stance.Stance;
import nightgames.status.BodyFetish;
import nightgames.status.Charmed;

public class BreastSmother extends Skill {
    public BreastSmother(Character self) {
        super("BreastSmother", self);
        addTag(SkillTag.usesBreasts);
        addTag(SkillTag.positioning);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.getProgression().getLevel() >= 15 || user.get(Attribute.Seduction) >= 30 && user.hasBreasts();
    }

    @Override
    public float priorityMod(Combat c) {
        if (c.getStance().havingSex(c)) {
            return 1; 
        } else {
            return 3;
        }
    }

    static BreastsPart.Size MIN_REQUIRED_BREAST_SIZE = Size.DCup;
    
    @Override
    public boolean usable(Combat c, Character target) {
        return getSelf().breastsAvailable()
            && c.getStance().reachTop(getSelf())
            && c.getStance().front(getSelf())
            && getSelf().body.getRandomBreasts().getSize().compareTo(MIN_REQUIRED_BREAST_SIZE) >= 0
            && c.getStance().mobile(getSelf())
            && (!c.getStance().mobile(target) || c.getStance().prone(target))
            && getSelf().canAct();
    }

    @Override
    public String describe(Combat c) {
        return "Shove your opponent's face between your tits to crush her resistance.";
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        boolean special = c.getStance().en != Stance.breastsmothering && !c.getStance().havingSex(c);        
        writeOutput(c, special ? Result.special : Result.normal, target);

        double n = getSelf().body.getRandomBreasts().attributeModifier(Attribute.Seduction);

        if (target.has(Trait.temptingtits)) {
            n += Global.random(5, 10);
        }
        if (target.has(Trait.beguilingbreasts)) {
            n *= 1.5;
            target.add(c, new Charmed(target));
        }
        if (target.has(Trait.imagination)) {
            n *= 1.5;
        }

        target.temptWithSkill(c, getSelf(), getSelf().body.getRandomBreasts(), (int) Math.round(n / 2), this);
        target.weaken(c, (int) getSelf().modifyDamage(DamageType.physical, target, Global.random(5, 15)));

        target.loseWillpower(c, Math.min(5, target.willpower.max() * 10 / 100 ));

        if (special) {
            c.setStance(new BreastSmothering(getSelf(), target), getSelf(), true);
            getSelf().emote(Emotion.dominant, 20);
        } else {
            getSelf().emote(Emotion.dominant, 10);
        }
        if (Global.random(100) < 15 + 2 * getSelf().get(Attribute.Fetish)) {
            target.add(c, new BodyFetish(target, getSelf(), BreastsPart.TYPE, .25));
        }

        return true;
    }

    @Override
    public int getMojoBuilt(Combat c) {
        return 0;
    }

    @Override
    public Skill copy(Character user) {
        return new BreastSmother(user);
    }

    @Override
    public Tactics type(Combat c) {
        if (c.getStance().enumerate() != Stance.breastsmothering) {
            return Tactics.positioning;
        } else {
            return Tactics.pleasure;
        }
    }

    @Override
    public String getLabel(Combat c) {
        return "BreastSmother";
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        StringBuilder b = new StringBuilder();
        
        if (modifier == Result.special) {
            b.append( "You quickly wrap up " + target.getName() + "'s head in your arms and press your "
                            + getSelf().body.getRandomBreasts().fullDescribe(getSelf()) + " into " + target.nameOrPossessivePronoun() + " face. ");
        }
        else {
            b.append( "You rock " + target.getName() + "'s head between your "
                            + getSelf().body.getRandomBreasts().fullDescribe(getSelf()) + " trying to force " + target.objectPronoun() + " to gasp. ");
        }
        
        if (getSelf().has(Trait.temptingtits)) {
            b.append(Global.capitalizeFirstLetter(target.pronoun()) + " can't help but groan in pleasure from having " + target.possessiveAdjective() + " face stuck between your perfect tits");                          
            if (getSelf().has(Trait.beguilingbreasts)) {
                b.append(", and you smile as " + target.pronoun() + " snuggles deeper into your cleavage");
            } 
            b.append(".");
            
        } else{
            b.append(" " + target.getName() + " muffles something in confusion into your breasts before " + target.pronoun() + " begins to panic as " + target.pronoun() + " realizes " + target.pronoun() + " cannot breathe!");            
        }   
        return b.toString();
}

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        StringBuilder b = new StringBuilder();
        if (modifier == Result.special) {
            b.append( getSelf().subject()+ " quickly wraps up your head between " + getSelf().possessiveAdjective() + " "
                            + getSelf().body.getRandomBreasts().fullDescribe(getSelf()) + ", filling your vision instantly with them. ");
        } else {
            b.append( getSelf().subject()+ " rocks your head between " + getSelf().possessiveAdjective() + " "
                            + getSelf().body.getRandomBreasts().fullDescribe(getSelf()) + " trying to force you to gasp for air. ");
        }
        
        if (getSelf().has(Trait.temptingtits)) {
            b.append("You can't help but groan in pleasure from having your face stuck between ");
            b.append(getSelf().possessiveAdjective());
            b.append(" perfect tits as they take your breath away");             
            if (getSelf().has(Trait.beguilingbreasts)) {
                b.append(", and due to their beguiling nature you can't help but want to stay there as long as possible");
            }
            b.append(".");
        } else {
            b.append(" You let out a few panicked sounds muffled by the breasts now covering your face as you realize you cannot breathe!");
        }

        return b.toString();
    }

    @Override
    public boolean makesContact(Combat c) {
        return true;
    }
}

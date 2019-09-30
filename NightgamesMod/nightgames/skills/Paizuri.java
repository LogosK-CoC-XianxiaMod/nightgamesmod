package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.characters.body.CockPart;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.nskills.tags.SkillTag;
import nightgames.stance.Stance;
import nightgames.status.BodyFetish;
import nightgames.status.Stsflag;


public class Paizuri extends Skill {
    public Paizuri(String name, Character self) {
        super(name, self);
    }
    
    public Paizuri(Character self) {
        super("Titfuck", self);
        addTag(SkillTag.positioning);
        addTag(SkillTag.pleasure);
        addTag(SkillTag.oral);
        addTag(SkillTag.foreplay);
        addTag(SkillTag.usesBreasts);
    }

    static BreastsPart.Size MIN_REQUIRED_BREAST_SIZE = Size.CCup;

    @Override
    public boolean usable(Combat c, Character target) {
        return getSelf().hasBreasts()
                        && getSelf().body.getLargestBreasts().getSize().compareTo(MIN_REQUIRED_BREAST_SIZE) >= 0
                        && target.hasDick() && getSelf().breastsAvailable() && target.crotchAvailable()
                        && c.getStance().paizuri(getSelf(), target)
                        && c.getStance().front(getSelf()) && getSelf().canAct()
                        && c.getStance().en != Stance.oralpin;
    }

    @Override
    public int getMojoBuilt(Combat c) {
        return 15;
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        BreastsPart breasts = getSelf().body.getLargestBreasts();
        // try to find a set of breasts large enough, if none, default to
        // largest.
        for (int i = 0; i < 3; i++) {
            BreastsPart otherbreasts = getSelf().body.getRandomBreasts();
            if (otherbreasts.getSize().compareTo(MIN_REQUIRED_BREAST_SIZE) > 0) {
                breasts = otherbreasts;
                break;
            }
        }
        
        int fetishChance = 7 + getSelf().get(Attribute.Fetish) / 2;

        int m = 5 + breasts.mod(Attribute.Seduction,Global.random(5));
        
        if(getSelf().is(Stsflag.oiled)) {
            m += Global.random(2, 5);
        }
        
        if( getSelf().has(Trait.lactating)) {
            m += Global.random(3, 5);
            fetishChance += 5;
        }
        
        if (getSelf().has(Trait.temptingtits)) {
            
            m += Global.random(4, 8);
            fetishChance += 10;
        }
        
        if (getSelf().has(Trait.beguilingbreasts)) {
            m *= 1.5;            
            fetishChance *= 2;
        }

        if (target.human()) {
            c.write(getSelf(), receive(0, Result.normal, target, breasts));
        } else {
            c.write(getSelf(), deal(c, 0, Result.normal, target));
        }
        target.body.pleasure(getSelf(), getSelf().body.getRandomBreasts(), target.body.getRandomCock(), m, c, this);
        if (Global.random(100) < fetishChance) {
            target.add(c, new BodyFetish(target, getSelf(), BreastsPart.TYPE, 1.5 + getSelf().get(Attribute.Fetish) * .1));
        }
        if (getSelf().has(Trait.temptingtits)) {
            target.temptWithSkill(c, getSelf(), getSelf().body.getRandomBreasts(), m/5, this);
        }
        return true;
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.get(Attribute.Seduction) >= 20 && user.hasBreasts();
    }

    @Override
    public Skill copy(Character user) {
        return new Paizuri(user);
    }

    @Override
    public int speed() {
        return 4;
    }

    @Override
    public Tactics type(Combat c) {
        return Tactics.pleasure;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        StringBuilder b = new StringBuilder();
        b.append("You squeeze ");
        b.append(target.possessivePronoun());
        b.append(" dick between your ");
        
        b.append(getSelf().body.getRandomBreasts().describe(getSelf()));
        if( getSelf().has(Trait.lactating))
        {
            b.append(" and milk squirts from your lactating teats");
        }
        b.append(". ");       
        
        if(getSelf().is(Stsflag.oiled)){
            b.append("You rub your oiled tits up and down ");
            b.append(target.possessivePronoun()) ;
            b.append(" shaft and teasingly lick the tip.");
        }else{
            b.append("You rub them up and down ");
            b.append(target.possessivePronoun());
            b.append(" shaft and teasingly lick the tip.");
        }
        
        if (getSelf().has(Trait.temptingtits)) {
            b.append(" Upon seeing your perfect tits around ");
            b.append(target.possessivePronoun());
            b.append(" cock, ");
            b.append(target.getName());
            b.append(" shudders with lust");
       
            if (getSelf().has(Trait.beguilingbreasts)) {
                b.append(" and due to your beguiling nature, " + target.possessiveAdjective() + " can't help drooling at the show.");
            }
            else  {
                b.append(".");
            }
        }
        
        return b.toString();
    }

    public String receive(int damage, Result modifier, Character target, BreastsPart breasts) {
        StringBuilder b = new StringBuilder();
        b.append(getSelf().getName() + " squeezes your dick between her ");
        b.append(breasts.describe(getSelf()));
        if( getSelf().has(Trait.lactating))
        {
            b.append(" and milk squirts from her lactating teats");
        }
        b.append(". ");       
        
        if(getSelf().is(Stsflag.oiled)){
            b.append("She rubs her oiled tits up and down your shaft and teasingly licks your tip.");
        }
        else{
            b.append("She rubs them up and down your shaft and teasingly licks your tip.");
        }
        
        if (getSelf().has(Trait.temptingtits)) {
            b.append(" The sight of those perfect tits around your cock causes you to shudder with lust");
            
            if (getSelf().has(Trait.beguilingbreasts)) {
                b.append(" and due to ");
                b.append(getSelf().getName()) ;
                b.append("'s breasts beguiling nature, you can't help but enjoy the show.");
            }
            else  {
                b.append(".");
            }
        }
        
        return b.toString();
    }

    @Override
    public String describe(Combat c) {
        return "Rub your opponent's dick between your boobs";
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean makesContact(Combat c) {
        return true;
    }
}

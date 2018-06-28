package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Emotion;
import nightgames.characters.Trait;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.nskills.tags.SkillTag;
import nightgames.skills.EngulfedFuck.Pairing;
import nightgames.skills.damage.DamageType;
import nightgames.stance.FlyingCarry;
import nightgames.stance.HeldPaizuri;
import nightgames.stance.IncubusEmbrace;
import nightgames.stance.Stance;
import nightgames.status.Abuff;
import nightgames.status.BodyFetish;
import nightgames.status.Charmed;
import nightgames.status.FiredUp;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.EarPart;
import nightgames.characters.body.TailPart;
import nightgames.characters.body.WingsPart;
import nightgames.characters.body.mods.ArcaneMod;
import nightgames.characters.body.mods.DemonicMod;
import nightgames.characters.body.mods.SizeMod;
import nightgames.status.Hypersensitive;
import nightgames.status.WingWrapped;

public class FutaFight extends Skill {
    public FutaFight(Character self) {
        super("FutaFight", self);
        addTag(SkillTag.dominant);
        addTag(SkillTag.usesBreasts);
        addTag(SkillTag.weaken);
        addTag(SkillTag.breastfeed);
        addTag(SkillTag.perfectAccuracy);
        addTag(SkillTag.positioning);
        addTag(SkillTag.pleasure);
        addTag(SkillTag.oral);
        addTag(SkillTag.foreplay);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return user.human();
    }

    @Override
    public float priorityMod(Combat c) {
        if (c.getStance().havingSex(c)) {
            return 1; 
        } else {
            return 3;
        }
    }

    static int MIN_REQUIRED_BREAST_SIZE = 4;
    
    @Override
    public boolean usable(Combat c, Character target) {
        return true;
    }

    @Override
    public String describe(Combat c) {
        return "FutaFight!";
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        boolean special = c.getStance().en != Stance.breastsmothering && !c.getStance().havingSex(c);        
        writeOutput(c, special ? Result.special : Result.normal, target);

        double n = 20 + Global.random(5) + getSelf().body.getLargestBreasts().getSize();

        //if (target.has(Trait.temptingtits)) {
            n += Global.random(5, 10);
        //}
        //if (target.has(Trait.beguilingbreasts)) {
            n *= 1.5;
            target.add(c, new Charmed(target));
        //}
        //if (target.has(Trait.imagination)) {
            n *= 1.5;
        //}

        target.temptWithSkill(c, getSelf(), getSelf().body.getRandom("breasts"), (int) Math.round(n / 2), this);
        target.weaken(c, (int) getSelf().modifyDamage(DamageType.physical, target, Global.random(5, 15)));

        target.loseWillpower(c, Math.min(5, target.getWillpower().max() * 10 / 100 ));     

        //if (special) {
            //c.setStance(new BreastSmothering(getSelf(), target), getSelf(), true);      
            getSelf().emote(Emotion.dominant, 20);
        //} else {
            //getSelf().emote(Emotion.dominant, 10);
        //}
        //if (Global.random(100) < 15 + 2 * getSelf().get(Attribute.Fetish)) {
            target.add(c, new BodyFetish(target, getSelf(), "breasts", 10));
        //}
            
            target.add(c, new Hypersensitive(target));
            BreastsPart part = target.body.getRandomBreasts();
            if (part != null) {
                BreastsPart realPart = target.body.getRandomBreasts();
                target.body.addReplace((BreastsPart)(realPart.upgrade().upgrade().upgrade().upgrade().upgrade().upgrade()), 1);
            }
            
            getSelf().add(c, new FiredUp(getSelf(), target, "breasts"));
            
            target.add(c, new Abuff(target, Attribute.Power, -2, 5));
            new Suckle(target).resolve(c, getSelf(), true);
            
            new Paizuri(getSelf()).resolve(c, target);
            
            target.add(c, new nightgames.status.OrgasmSeal(target, 50));
            
            getSelf().addTemporaryTrait(Trait.witch, 100);
            getSelf().addTemporaryTrait(Trait.lactating, 100);
            getSelf().addTemporaryTrait(Trait.responsive, 100);
            getSelf().addTemporaryTrait(Trait.temptingtits, 100);
            getSelf().addTemporaryTrait(Trait.beguilingbreasts, 100);
            getSelf().addTemporaryTrait(Trait.sedativecream, 100);
            getSelf().body.temporaryAddPartMod("mouth", ArcaneMod.INSTANCE, 100);
            
            BreastsPart partB = getSelf().body.getBreastsBelow(BreastsPart.h.getSize());
            if (partB != null) {
                getSelf().body.temporaryAddOrReplacePartWithType(partB.upgrade().upgrade().upgrade().upgrade(), 100);
            }
            
            CockPart partC = target.body.getCockBelow(SizeMod.getMaximumSize("cock"));
                if (partC != null) {
                    target.body.addReplace(partC.upgrade().upgrade().upgrade().upgrade().upgrade(), 1);
                } else {
                    target.body.addReplace(new CockPart().applyMod(new SizeMod(SizeMod.COCK_SIZE_SMALL)).upgrade().upgrade().upgrade().upgrade().upgrade(), 1);
                }
                
            
                CockPart partD = getSelf().body.getCockBelow(SizeMod.getMaximumSize("cock"));
                if (partD != null) {
                    getSelf().body.addReplace(partD.upgrade().upgrade().upgrade().upgrade().upgrade(), 1);
                } else {
                    getSelf().body.addReplace(new CockPart().applyMod(new SizeMod(SizeMod.COCK_SIZE_SMALL)).upgrade().upgrade().upgrade().upgrade().upgrade(), 1);
                }
                
                
            getSelf().addTemporaryTrait(Trait.divinity, 100);
            getSelf().addTemporaryTrait(Trait.objectOfWorship, 100);
            getSelf().addTemporaryTrait(Trait.lastStand, 100);
            getSelf().addTemporaryTrait(Trait.erophage, 100);
            getSelf().addTemporaryTrait(Trait.sacrosanct, 100);
            getSelf().addTemporaryTrait(Trait.genuflection, 100);
            getSelf().addTemporaryTrait(Trait.revered, 100);
            
            getSelf().addTemporaryTrait(Trait.ImitatedStrength, 999);
            getSelf().addTemporaryTrait(Trait.succubus, 999);
            getSelf().addTemporaryTrait(Trait.energydrain, 999);
                getSelf().addTemporaryTrait(Trait.spiritphage, 999);
                getSelf().addTemporaryTrait(Trait.lacedjuices, 999);
                getSelf().addTemporaryTrait(Trait.RawSexuality, 999);
                getSelf().addTemporaryTrait(Trait.soulsucker, 999);
                getSelf().addTemporaryTrait(Trait.gluttony, 999);
                getSelf().body.temporaryAddPartMod("ass", DemonicMod.INSTANCE, 999);
                getSelf().body.temporaryAddPartMod("hands", DemonicMod.INSTANCE, 999);
                getSelf().body.temporaryAddPartMod("feet", DemonicMod.INSTANCE, 999);
                getSelf().body.temporaryAddPartMod("mouth", DemonicMod.INSTANCE, 999);
        getSelf().addTemporaryTrait(Trait.succubus, 999);
        getSelf().addTemporaryTrait(Trait.soulsucker, 999);
        getSelf().addTemporaryTrait(Trait.energydrain, 999);
        getSelf().addTemporaryTrait(Trait.spiritphage, 999);
        getSelf().body.temporaryAddOrReplacePartWithType(WingsPart.demonic, 999);
        getSelf().body.temporaryAddOrReplacePartWithType(TailPart.demonic, 999);
        getSelf().body.temporaryAddOrReplacePartWithType(EarPart.pointed, 999);
        
        Pairing pair = Pairing.findPairing(getSelf(), target);
        double base = 10.0 + Math.min(20, Global.random(getSelf().get(Attribute.Slime) / 3 + getSelf().get(Attribute.Seduction) / 5));
        int selfDmg = (int) ((base * pair.modPleasure(true)) / (getSelf().has(Trait.experienced) ? 2.0 : 3.0));
        int targetDmg = (int) (base * pair.modPleasure(false));
        switch (pair) {
            case ASEX_MALE:
                c.write(getSelf(),
                                Global.format("{self:SUBJECT-ACTION:wrap|wraps} {other:name-possessive}"
                                                + " {other:body-part:cock} in a clump of slime molded after {self:possessive} ass"
                                                + " and {self:action:pump|pumps} it furiously.", getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomAss(), target.body.getRandomCock(), targetDmg,
                                c, this);
                getSelf().body.pleasure(target, target.body.getRandomCock(), getSelf().body.getRandomAss(), selfDmg, c, this);
                break;
            case FEMALE_HERM:
                c.write(getSelf(),
                                Global.format("{self:SUBJECT-ACTION:impale|impales} {self:reflective} on"
                                                + " {other:name-possessive} {other:body-part:cock} and {self:action:bounce|bounces} wildly,"
                                                + " filling {other:direct-object} with pleasure. At the same time, {self:pronoun} "
                                                + "{self:action:twiddle|twiddles} {other:possessive} clit with {self:possessive} fingers.",
                                getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomPussy(), target.body.getRandomCock(),
                                targetDmg / 2, c, this);
                target.body.pleasure(getSelf(), getSelf().body.getRandom("hands"), target.body.getRandomPussy(),
                                targetDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomCock(), getSelf().body.getRandomPussy(), selfDmg,
                                c, this);
                break;
            case FEMALE_MALE:
                c.write(getSelf(),
                                Global.format("{self:SUBJECT-ACTION:impale|impales} {self:reflective} on"
                                                + " {other:name-possessive} {other:body-part:cock} and {self:action:bounce|bounces} wildly,"
                                                + " filling {other:direct-object} with pleasure.", getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomPussy(), target.body.getRandomCock(), targetDmg,
                                c, this);
                getSelf().body.pleasure(target, target.body.getRandomCock(), getSelf().body.getRandomPussy(), selfDmg,
                                c, this);
                break;
            case HERM_ASEX:
            case MALE_ASEX:
                c.write(getSelf(),
                                Global.format("Despite not having much to work with, {self:subject} still"
                                                + " {self:action:manage|manages} to make {other:subject} squeal by pounding {other:name-possessive}"
                                                + " {other:body-part:ass} with {self:possessive} {self:body-part:cock}.",
                                getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomCock(), target.body.getRandomAss(), targetDmg,
                                c, this);
                getSelf().body.pleasure(target, target.body.getRandomAss(), getSelf().body.getRandomCock(), selfDmg, c, this);
                break;
            case HERM_FEMALE:
                c.write(getSelf(),
                                Global.format("{self:SUBJECT-ACTION:pound|pounds} {other:name-possessive} "
                                                + "{other:body-part:pussy} with vigor, at the same time fingering {other:possessive}"
                                                + " ass.", getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomCock(), target.body.getRandomPussy(),
                                targetDmg / 2, c, this);
                target.body.pleasure(getSelf(), getSelf().body.getRandom("hands"), target.body.getRandomAss(),
                                targetDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomPussy(), getSelf().body.getRandomCock(), selfDmg,
                                c, this);
                break;
            case HERM_HERM:
                c.write(getSelf(),
                                Global.format("It takes some clever maneuvering, but {self:SUBJECT-ACTION:manage|manages}"
                                                + " to line {other:name-do} and {self:reflective} up perfectly. When"
                                                + " {self:pronoun} {self:action:strike|strikes}, {other:possessive} {other:body-part:cock} ends up"
                                                + " in {self:possessive} {self:body-part:pussy}, and {self:body-part:cock} in {other:possessive}"
                                                + " {other:body-part:pussy}. With every twitch, both of you are filled with unimaginable pleasure,"
                                                + " so when {self:pronoun} {self:action:start|starts} fucking in earnest the sensations are"
                                                + " almost enough to cause you both to pass out.", getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomCock(), target.body.getRandomPussy(),
                                targetDmg / 2, c, this);
                target.body.pleasure(getSelf(), getSelf().body.getRandomPussy(), target.body.getRandomCock(),
                                targetDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomPussy(), getSelf().body.getRandomCock(),
                                selfDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomCock(), getSelf().body.getRandomPussy(),
                                selfDmg / 2, c, this);
                break;
            case HERM_MALE:
                c.write(getSelf(),
                                Global.format("{self:SUBJECT-ACTION:lower|lowers} {self:possessive}"
                                                + " {self:body-part:pussy} down on {other:name-possessive} {other:body-part:cock}."
                                                + " While slowly fucking {other:direct-object}, {self:pronoun} {self:action:move|moves}"
                                                + " {self:possessive} {self:body-part:cock} to the entrance of {other:possessive} ass."
                                                + " Before {other:pronoun} {other:action:have|has} a chance to react, {self:pronoun}"
                                                + " {self:action:shove|shoves} it up there in one thrust, fucking {other:direct-object}"
                                                + " from both sides.", getSelf(), target));
                target.body.pleasure(getSelf(), getSelf().body.getRandomCock(), target.body.getRandomAss(),
                                targetDmg / 2, c, this);
                target.body.pleasure(getSelf(), getSelf().body.getRandomPussy(), target.body.getRandomCock(),
                                targetDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomPussy(), getSelf().body.getRandomCock(),
                                selfDmg / 2, c, this);
                getSelf().body.pleasure(target, target.body.getRandomCock(), getSelf().body.getRandomAss(), selfDmg / 2,
                                c, this);
                break;
            case MALE_FEMALE:
            case MALE_MALE:
            case MALE_HERM:
                BodyPart bpart = pair == Pairing.MALE_MALE ? target.body.getRandomAss() : target.body.getRandomPussy();
                int realTargetDmg = targetDmg;
                String msg = "{self:SUBJECT-ACTION:place|places} {self:possessive}"
                                + " {self:body-part:cock} at {other:name-possessive} " + bpart.describe(target)
                                + " and thrust in all the way"
                                + " in a single movement of {self:possessive} hips. {self:PRONOUN} "
                                + "{self:action:proceed|proceeds} to piston in and out at a furious pace.";
                if (pair == Pairing.MALE_HERM) {
                    msg += " At the same time, {self:pronoun} {self:action:use:uses} both of {self:possessive}"
                                    + " hands to pump {other:possessive} {other:body-part:cock}.";
                }
                c.write(getSelf(), Global.format(msg, getSelf(), target));
                if (pair == Pairing.MALE_HERM) {
                    target.body.pleasure(getSelf(), getSelf().body.getRandom("hands"), target.body.getRandomCock(),
                                    targetDmg / 2, c, this);
                    realTargetDmg /= 2;
                }
                target.body.pleasure(getSelf(), getSelf().body.getRandomCock(), bpart, realTargetDmg, c, this);
                getSelf().body.pleasure(target, bpart, getSelf().body.getRandomCock(), selfDmg, c, this);
                break;
        }
        
        
        
        target.add(c, new WingWrapped(target, getSelf()));
        
        getSelf().emote(Emotion.dominant, 50);
        getSelf().emote(Emotion.horny, 30);
        target.emote(Emotion.desperate, 50);
        target.emote(Emotion.nervous, 75);
        int m = 5 + Global.random(5);
        int otherm = m;
        if (getSelf().has(Trait.insertion)) {
            otherm += Math.min(getSelf().get(Attribute.Seduction) / 4, 40);
        }
        c.setStance(new IncubusEmbrace(getSelf(), target), getSelf(), getSelf().canMakeOwnDecision());
            
        return true;
    }

    @Override
    public int getMojoBuilt(Combat c) {
        return 0;
    }

    @Override
    public Skill copy(Character user) {
        return new FutaFight(user);
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
        return "BreastSmotherSuperSucc";
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
                            + getSelf().body.getRandomBreasts().fullDescribe(getSelf()) + " trying to force " + target.directObject() + " to gasp. ");                           
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

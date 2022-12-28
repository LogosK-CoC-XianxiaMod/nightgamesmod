package nightgames.pet;

import nightgames.characters.Character;
import nightgames.characters.Emotion;
import nightgames.characters.Growth;
import nightgames.characters.Trait;
import nightgames.characters.body.BodyPart;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.grammar.Person;
import nightgames.grammar.SingularFeminineThirdPerson;
import nightgames.grammar.SingularMasculineThirdPerson;
import nightgames.match.Dialog;
import nightgames.match.Intelligence;
import nightgames.skills.Tactics;
import nightgames.status.Slimed;
import nightgames.status.Status;

import java.util.Arrays;
import java.util.List;

public class PetCharacter extends Character {
    
    public static final PetCharacter DUMMY = new PetCharacter();

    private String type;
    private String ownerType;
    public Pet self;

    @Override
    public final int getPetLimit() {
        // NO PETS OF PETS ARGH
        return 0;
    }

    private static final List<Trait> INSPIRABLE_TRAITS = Arrays.asList(
                    Trait.analFanatic,
                    Trait.analTraining1,
                    Trait.analTraining2,
                    Trait.analTraining3,
                    Trait.anatomyknowledge,
                    Trait.asshandler,
                    Trait.assmaster,
                    Trait.autonomousAss,
                    Trait.autonomousPussy,
                    Trait.carnalvirtuoso,
                    Trait.defthands,
                    Trait.desensitized,
                    Trait.desensitized2,
                    Trait.dexterous,
                    Trait.dominatrix,
                    Trait.energydrain,
                    Trait.experienced,
                    Trait.experttongue,
                    Trait.fakeout,
                    Trait.freeSpirit,
                    Trait.graceful,
                    Trait.hawkeye,
                    Trait.holecontrol,
                    Trait.insertion,
                    Trait.limbTraining1,
                    Trait.limbTraining2,
                    Trait.limbTraining3,
                    Trait.mojoMaster,
                    Trait.naturalTop,
                    Trait.nimbletoes,
                    Trait.obsequiousAppeal,
                    Trait.oiledass,
                    Trait.polecontrol,
                    Trait.powerfulhips,
                    Trait.pussyhandler,
                    Trait.responsive,
                    Trait.romantic,
                    Trait.RawSexuality,
                    Trait.sadist,
                    Trait.silvertongue,
                    Trait.sexualmomentum,
                    Trait.shameless,
                    Trait.sexTraining1,
                    Trait.sexTraining2,
                    Trait.sexTraining3,
                    Trait.soulsucker,
                    Trait.spiral,
                    Trait.submissive,
                    Trait.sweetlips,
                    Trait.SexualGroove,
                    Trait.temptingtits,
                    Trait.ticklemonster,
                    Trait.toymaster,
                    Trait.tongueTraining1,
                    Trait.tongueTraining2,
                    Trait.tongueTraining3,
                    Trait.tight);
    public PetCharacter(Pet self, String name, String type, Growth growth, int level) {
        super(name, 1);
        this.ownerType = self.owner().type;
        this.self = self;
        this.type = type;
        this.setGrowth(growth);
        for (int i = 1; i < level; i++) {
            getProgression().setLevel(getProgression().getLevel() + 1);
            getGrowth().levelUp(this);
        }
        distributePoints(Arrays.asList());
        if (self.owner().has(Trait.inspirational)) {
            for (Trait t : INSPIRABLE_TRAITS) {
                if (self.owner().has(t) && !has(t)) {
                    add(t);
                }
            }
        }
        this.getSkills().clear();
        this.mojo.setMax(100);
        stamina.renew();
        arousal.renew();
        getMojo().renew();
    }

    private PetCharacter() {
        super("{{{DUMMY}}}", 1);
    }
    
    public boolean isDummy() {
        return self == null;
    }
    
    public PetCharacter cloneWithOwner(Character owner) throws CloneNotSupportedException {
        PetCharacter clone = (PetCharacter) clone();
        clone.self = getSelf().cloneWithOwner(owner);
        return clone;
    }

    @Override
    public void ding(Combat c) {
        getProgression().setLevel(getProgression().getLevel() + 1);
        getGrowth().levelUp(this);
        distributePoints(Arrays.asList());
    }

    @Override
    public String describe(int per, Character observer) {
        return "";
    }

    @Override
    public boolean resist3p(Combat c, Character target, Character assist) {
        return true;
    }

    @Override
    public void add(Combat c, Status status) {
        super.add(c, status);
        if (stunned()) {
            c.write(this, Global.format("With {self:name-possessive} link to the fight weakened, {self:subject-action:disappears|disappears}..", this, this));
            c.removePet(this);
        }
    }

    @Override
    public boolean human() {
        return false;
    }

    @Override
    public String bbLiner(Combat c, Character target) {
        return "";
    }

    @Override
    public String nakedLiner(Combat c, Character target) {
        return "";
    }

    @Override
    public String stunLiner(Combat c, Character target) {
        return "";
    }

    @Override
    public String taunt(Combat c, Character target) {
        return "";
    }

    @Override
    public String challenge(Character other) {
        return "";
    }

    @Override
    public String getPortrait() {
        return "";
    }

    @Override
    public String getType() {
        return type;
    }
    
    @Override
    protected void resolveOrgasm(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart, int times, int totalTimes) {
        super.resolveOrgasm(c, opponent, selfPart, opponentPart, times, totalTimes);
        if (getSelf().owner().has(Trait.StickyFinale)) {
            c.write(this, Global.format("The force of {self:name-possessive} orgasm causes {self:direct-object} to shudder and explode in a rain of slime, completely covering {other:name-do} with the sticky substance.", this, opponent));
            opponent.add(c, new Slimed(opponent, getSelf().owner(), Global.random(5, 11)));
        } else {
            c.write(this, Global.format("The force of {self:name-possessive} orgasm destroys {self:possessive} anchor to the fight and {self:pronoun} disappears.", this, opponent));
        }
        c.removePet(this);
    }

    @Override
    public void afterParty() {}
    
    @Override
    public void emote(Emotion emo, int amt) {}

    @Override
    public void counterattack(Character target, Tactics type, Combat c) {}

    public boolean isPetOf(Character other) {
        return other != null && !isDummy() && ownerType.equals(other.type);
    }

    public Pet getSelf() {
        return self;
    }
    
    public double percentHealth() {
        return Math.min(stamina.percent(), arousal.percent());
    }

    public boolean isPet() {
        return true;
    }

    @Override
    public Person getGrammar() {
        if (useFemalePronouns()) {
            return new SingularFeminineThirdPerson(this);
        } else {
            return new SingularMasculineThirdPerson(this);
        }
    }

    @Override
    public Intelligence makeIntelligence() {
        throw new UnsupportedOperationException("pets aren't intelligent, stupid.");
    }

    @Override
    public Dialog makeDialog() {
        throw new UnsupportedOperationException("pets don't have match dialog");
    }
}
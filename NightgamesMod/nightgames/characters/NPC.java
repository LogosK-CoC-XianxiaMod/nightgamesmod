package nightgames.characters;

import nightgames.characters.body.BodyPart;
import nightgames.characters.body.StraponPart;
import nightgames.characters.custom.CharacterLine;
import nightgames.characters.custom.CommentSituation;
import nightgames.characters.custom.RecruitmentData;
import nightgames.combat.Combat;
import nightgames.combat.CombatScene;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.grammar.Person;
import nightgames.grammar.SingularFeminineThirdPerson;
import nightgames.grammar.SingularMasculineThirdPerson;
import nightgames.items.clothing.Clothing;
import nightgames.items.clothing.ClothingSlot;
import nightgames.match.ArtificialIntelligence;
import nightgames.match.Dialog;
import nightgames.match.Intelligence;
import nightgames.match.Match;
import nightgames.pet.arms.ArmManager;
import nightgames.pet.arms.ArmType;
import nightgames.skills.Skill;
import nightgames.skills.Stage;
import nightgames.skills.Tactics;
import nightgames.skills.damage.DamageType;
import nightgames.skills.strategy.CombatStrategy;
import nightgames.stance.Behind;
import nightgames.stance.Neutral;
import nightgames.stance.Position;
import nightgames.status.Disguised;
import nightgames.status.Pheromones;
import nightgames.status.Status;
import nightgames.status.Stsflag;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class NPC extends Character {
    public BasePersonality ai;
    public HashMap<Emotion, Integer> emotes;
    public Emotion mood;
    public Plan plan;
    public boolean isStartCharacter = false;
    public List<CombatStrategy> personalStrategies;
    private List<CombatScene> postCombatScenes;

    public NPC(String name, int level, BasePersonality ai) {
        super(name, level);
        this.ai = ai;
        emotes = new HashMap<>();
        for (Emotion e : Emotion.values()) {
            emotes.put(e, 0);
        }
        mood = Emotion.confident;
        initialGender = CharacterSex.female;
        personalStrategies = new ArrayList<>();
        postCombatScenes = new ArrayList<>();
    }

    protected void addPersonalStrategy(CombatStrategy strategy) {
        personalStrategies.add(strategy);
    }

    protected void addCombatScene(CombatScene scene) {
        postCombatScenes.add(scene);
    }
    
    public List<CombatScene> getPostCombatScenes() {
        return Collections.unmodifiableList(postCombatScenes);
    }

    @Override
    public String describe(int per, Character observer) {
        StringBuilder b = new StringBuilder();
        b.append(ai.describe(this));
        b.append("<br/><br/>");
        body.describe(b, observer, " ");
        b.append("<br/>");
        for (Trait t : getTraits()) {
            t.describe(this, b);
            b.append(' ');
        }
        b.append("<br/>");
        for (Status s : status) {
            String statusDesc = s.describe(observer);
            if (!statusDesc.isEmpty()) {
                b.append(statusDesc)
                    .append("<br/>");
            }
        }
        b.append(outfit.describe(this));
        b.append(observe(per));
        if (getArmManager() != null) b.append(getArmManager().describe(this));
        return b.toString();
    }

    private String observeStamina(int perception) {
        if (perception >= 8) {
            return "Her stamina is at " + stamina.percent() + "%<br/>";
        }
        if (perception >= 6) {
            if (stamina.percent() <= 33) {
                return "She looks a bit unsteady on her feet<br/>";
            } else if (stamina.percent() <= 66) {
                return "She's starting to look tired<br/>";
            }
        }
        if (perception >= 4) {
            if (stamina.percent() <= 50) {
                return "She looks pretty tired<br/>";
            }
        }
        return "";
    }

    private String observeArousal(int per) {
        if (per >= 9) {
            return "Her arousal is at " + arousal.percent() + "%<br/>";
        }
        if (per >= 7) {
            if (arousal.percent() >= 75) {
                return "She's dripping with arousal and breathing heavily. She's at least 3/4 of the way to orgasm<br/>";
            } else if (arousal.percent() >= 50) {
                return "She's showing signs of arousal. She's at least halfway to orgasm<br/>";
            } else if (arousal.percent() >= 25) {
                return "She's starting to look noticeably arousal, maybe a quarter of her limit<br/>";
            }
        }
        if (per >= 3) {
            if (arousal.percent() >= 50) {
                return "She's showing clear sign of arousal. You're definitely getting to her.<br/>";
            }
        }
        return "";
    }

    private String observeWillpower(int per) {
        if (per >= 9) {
            return "Her willpower is at " + willpower.percent() + "%<br/>";
        }
        if (per >= 7) {
            if (willpower.percent() <= 75) {
                return "She still seems ready to fight.<br/>";
            } else if (willpower.percent() <= 50) {
                return "She seems a bit unsettled, but she still has some spirit left in her.<br/>";
            } else if (willpower.percent() <= 25) {
                return "Her eyes seem glazed over and ready to give in.<br/>";
            }
        }
        if (per >= 3) {
            if (arousal.percent() >= 50) {
                return "She's showing clear sign of arousal. You're definitely getting to her.<br/>";
            }
            if (willpower.percent() <= 50) {
                return "She seems a bit distracted and unable to look you in the eye.<br/>";
            }
        }
        return "";
    }

    private String observe(int per) {
        String visible = "";
        if (is(Stsflag.unreadable)) {
            return visible;
        }
        visible += observeArousal(per);
        visible += observeStamina(per);
        visible += observeWillpower(per);
        if (per >= 7) {
            visible = visible + "She looks " + mood.name() + "<br/>";
        }
        if (per >= 5) {
            visible += Stage.describe(this);
        }
        if (per >= 6 && status.size() > 0) {
            visible += "List of statuses:<br/><i>";
            visible += status.stream().filter(s -> !s.flags().contains(Stsflag.disguised) || per >= 9).map(Status::toString).collect(Collectors.joining(", "));
            visible += "</i><br/>";
        }
        return visible;
    }

    @Override
    public void sendVictoryMessage(Combat c, Result flag) {
        super.sendVictoryMessage(c, flag);
        c.write(ai.victory(c, flag));
    }

    @Override
    public void sendDefeatMessage(Combat c, Result flag) {
        super.sendDefeatMessage(c, flag);
        c.write(ai.defeat(c, flag));
    }

    @Override
    public boolean resist3p(Combat combat, Character intruder, Character assist) {
        if (has(Trait.cursed)) {
            Global.gui().message(ai.resist3p(combat, intruder, assist));
            return true;
        }
        return false;
    }


    @Override
    public boolean human() {
        return false;
    }

    @Override
    public void sendDrawMessage(Combat c, Result flag) {
        super.sendDrawMessage(c, flag);
        c.write(ai.draw(c, flag));
    }

    @Override
    public Intelligence makeIntelligence() {
        return new ArtificialIntelligence(this);
    }

    @Override
    public Dialog makeDialog() {
        return ai.makeDialog();
    }

    public String getRandomLineFor(String lineType, Combat c, Character other) {
        return ai.getRandomLineFor(lineType, c, other);
    }

    @Override
    public String challenge(Character other) {
        return getRandomLineFor(CharacterLine.CHALLENGE, null, other);
    }

    @Override
    public String orgasmLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.ORGASM_LINER, c, target);
    }

    @Override
    public String makeOrgasmLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.MAKE_ORGASM_LINER, c, target);
    }

    @Override
    public String bbLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.BB_LINER, c, target);
    }

    @Override
    public String nakedLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.NAKED_LINER, c, target);
    }

    @Override
    public String stunLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.STUNNED_LINER, c, target);
    }

    @Override
    public String taunt(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.TAUNT_LINER, c, target);
    }

    @Override
    public String temptLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.TEMPT_LINER, c, target);
    }
    
    @Override 
    public String victoryLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.VICTORY_LINER, c, target);
    }
    
    @Override 
    public String loserLiner(Combat c, Character target) {
        return getRandomLineFor(CharacterLine.LOSER_LINER, c, target);
    }

    @Override
    public void ding(Combat c) {
        getProgression().setLevel(getProgression().getLevel() + 1);
        ai.ding(this);
        if (c != null) {
            String string = Global.format("{self:subject-action:have} leveled up!", this, this);
            c.write(this, string);
            c.updateMessage();
        }
    }


    @Override
    public void afterParty() {
        Global.gui().message(getRandomLineFor(CharacterLine.NIGHT_LINER, null, Global.getPlayer()));
    }

    public void daytime(int time) {
        ai.rest(time);
    }

    @Override
    public Emotion getMood() {
        return mood;
    }

    @Override
    public void counterattack(Character target, Tactics type, Combat c) {
        switch (type) {
            case damage:
                c.write(this, getName() + " avoids your clumsy attack and swings her fist into your nuts.");
                target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
                break;
            case pleasure:
                if (target.hasDick()) {
                    if (target.crotchAvailable()) {
                        c.write(this, getName() + " catches you by the penis and rubs your sensitive glans.");
                        target.body.pleasure(this, body.getRandomHands(), target.body.getRandomCock(),
                                        4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                    } else {
                        c.write(this, getName() + " catches you as you approach and grinds her knee into the tent in your "
                                        + target.outfit.getTopOfSlot(ClothingSlot.bottom) +".");
                        target.body.pleasure(this, body.getRandom("legs"), target.body.getRandomCock(),
                                        4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                    }
                } else {
                    c.write(this, getName()
                                    + " pulls you off balance and licks your sensitive ear. You tremble as she nibbles on your earlobe.");
                    target.body.pleasure(this, body.getRandom("tongue"),
                        target.body.getRandomEars(),
                                    4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                }
                break;
            case fucking:
                if (c.getStance().sub(this)) {
                    Position reverse = c.getStance().reverse(c, true);
                    if (reverse != c.getStance() && !BodyPart.hasOnlyType(reverse.bottomParts(), StraponPart.TYPE)) {
                        c.setStance(reverse, this, false);
                    } else {
                        c.write(this, Global.format(
                                        "{self:NAME-POSSESSIVE} quick wits find a gap in {other:name-possessive} hold and {self:action:slip|slips} away.",
                                        this, target));
                        c.setStance(new Neutral(this, c.getOpponentCharacter(this)), this, true);
                    }
                } else {
                    target.body.pleasure(this, body.getRandomHands(), target.body.getRandomBreasts(),
                                    4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                    c.write(this, Global.format(
                                    "{self:SUBJECT-ACTION:pinch|pinches} {other:possessive} nipples with {self:possessive} hands as {other:subject-action:try|tries} to fuck {self:direct-object}. "
                                                    + "While {other:subject-action:yelp|yelps} with surprise, {self:subject-action:take|takes} the chance to pleasure {other:possessive} body",
                                    this, target));
                }

                break;
            case stripping:
                Clothing clothes = target.stripRandom(c);
                if (clothes != null) {
                    c.write(this, getName()
                                    + " manages to catch you groping her clothing, and in a swift motion strips off your "
                                    + clothes.getName() + ".");
                } else {
                    c.write(this, getName()
                                    + " manages to dodge your groping hands and gives a retaliating slap in return.");
                    target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
                }
                break;
            case positioning:
                if (c.getStance().dom(this)) {
                    c.write(this, getName() + " outmanuevers you and you're exhausted from the struggle.");
                    target.weaken(c, (int) this.modifyDamage(DamageType.stance, target, 15));
                } else {
                    c.write(this, getName() + " outmanuevers you and catches you from behind when you stumble.");
                    c.setStance(new Behind(this, target));
                }
                break;
            default:
                c.write(this, getName() + " manages to dodge your attack and gives a retaliating slap in return.");
                target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
        }
    }

    public Skill prioritize(ArrayList<WeightedSkill> plist) {
        if (plist.isEmpty()) {
            return null;
        }
        double sum = 0;
        ArrayList<WeightedSkill> wlist = new ArrayList<>();
        for (WeightedSkill wskill : plist) {
            sum += wskill.weight;
            wlist.add(new WeightedSkill(sum, wskill.skill));
        }

        if (wlist.isEmpty()) {
            return null;
        }
        double s = Global.randomdouble() * sum;
        for (WeightedSkill wskill : wlist) {
            if (wskill.weight > s) {
                return wskill.skill;
            }
        }
        return plist.get(plist.size() - 1).skill;
    }

    @Override
    public void emote(Emotion emo, int amt) {
        if (emo == mood) {
            // if already this mood, cut gain by half
            amt = Math.max(1, amt / 2);
        }
        emotes.put(emo, emotes.get(emo) + amt);
    }

    public Emotion moodSwing(Combat c) {
        Emotion current = mood;
        for (Emotion e : emotes.keySet()) {
            if (ai.checkMood(c, e, emotes.get(e))) {
                emotes.put(e, 0);
                // cut all the other emotions by half so that the new mood
                // persists for a bit
                for (Emotion e2 : emotes.keySet()) {
                    emotes.put(e2, emotes.get(e2) / 2);
                }
                mood = e;
                return e;
            }
        }
        return current;
    }

    @Override
    public void endOfCombatRound(Combat c, Character opponent) {
        super.endOfCombatRound(c, opponent);
        ai.eot(c, opponent);
        if (opponent.has(Trait.pheromones) && opponent.arousal.percent() >= 20 && opponent.rollPheromones(c)) {
            c.write(opponent, Global.format("<br/>{other:SUBJECT-ACTION:see} {self:subject} swoon slightly "
                            + "as {self:pronoun-action:get} close to {other:direct-object}. "
                            + "Seems like {self:pronoun-action:are} starting to feel "
                            + "the effects of {other:possessive} musk.", this, opponent));
            add(c, Pheromones.getWith(opponent, this, opponent.getPheromonePower(), 10));
        }
        if (has(Trait.RawSexuality)) {
            c.write(this, Global.format("{self:NAME-POSSESSIVE} raw sexuality turns both of you on.", this, opponent));
            temptNoSkillNoSource(c, opponent, arousal.max() / 20);
            opponent.temptNoSkillNoSource(c, this, opponent.arousal.max() / 20);
        }
        if (c.getStance().dom(this)) {
            emote(Emotion.dominant, 20);
            emote(Emotion.confident, 10);
        } else if (c.getStance().sub(this)) {
            emote(Emotion.nervous, 15);
            emote(Emotion.desperate, 10);
        }
        if (opponent.mostlyNude()) {
            emote(Emotion.horny, 15);
            emote(Emotion.confident, 10);
        }
        if (mostlyNude()) {
            emote(Emotion.nervous, 10);
            if (has(Trait.exhibitionist)) {
                emote(Emotion.horny, 20);
            }
        }
        if (opponent.arousal.percent() >= 75) {
            emote(Emotion.confident, 20);
        }

        if (arousal.percent() >= 50) {
            emote(Emotion.horny, arousal.percent() / 4);
        }

        if (arousal.percent() >= 50) {
            emote(Emotion.desperate, 10);
        }
        if (arousal.percent() >= 75) {
            emote(Emotion.desperate, 20);
            emote(Emotion.nervous, 10);
        }
        if (arousal.percent() >= 90) {
            emote(Emotion.desperate, 20);
        }
        if (!canAct()) {
            emote(Emotion.desperate, 10);
        }
        if (!opponent.canAct()) {
            emote(Emotion.dominant, 20);
        }
        moodSwing(c);
    }

    @Override
    public NPC clone() throws CloneNotSupportedException {
        return (NPC) super.clone();
    }

    @Override
    protected void resolveOrgasm(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart, int times,
                    int totalTimes) {
        super.resolveOrgasm(c, opponent, selfPart, opponentPart, times, totalTimes);
        ai.resolveOrgasm(c, this, opponent, selfPart, opponentPart, times, totalTimes);
    }
    @Override
    public String getPortrait() {
        Disguised disguised = (Disguised) getStatus(Stsflag.disguised);
        if (disguised != null) {
            return disguised.getTarget().ai.image();
        }
        return ai.image();
    }

    @Override
    public String getType() {
        if(ai==null) {System.out.println("is broken: name "+this.getTrueName());return "Nobody";}
        return ai.getType();
    }

    public RecruitmentData getRecruitmentData() {
        return ai.getRecruitmentData();
    }

    @Override
    public double dickPreference() {
        return ai instanceof Eve ? 10.0 : super.dickPreference();
    }

    public Optional<String> getComment(Combat c) {
        // can't really talk when they're disabled
        if (!canRespond()) {
            return Optional.empty();
        }
        Set<CommentSituation> applicable = CommentSituation.getApplicableComments(c, this, c.getOpponentCharacter(this));
        Set<CommentSituation> forbidden = EnumSet.allOf(CommentSituation.class);
        forbidden.removeAll(applicable);
        Map<CommentSituation, String> comments = ai.getComments(c);
        forbidden.forEach(comments::remove);
        if (comments.isEmpty() || comments.size() == 1 && comments.containsKey(CommentSituation.NO_COMMENT))
            return Optional.empty();
        return Global.pickRandom(new ArrayList<>(comments.values()));
    }
    
    public Emotion moodSwing() {
        Emotion current = mood;
        int max=emotes.get(current);
        for(Emotion e: emotes.keySet()){
            if(emotes.get(e)>max){
                mood=e;
                max=emotes.get(e);
            }
        }
        return mood;
    }
    
    public void decayMood(){
        for(Emotion e: emotes.keySet()){
            if(mostlyNude()&&!has(Trait.exhibitionist)&&!has(Trait.shameless)&& e==Emotion.nervous){
                emotes.put(e, emotes.get(e)-((emotes.get(e)-50)/2));
            }else if(mostlyNude()&&!has(Trait.exhibitionist)&& e==Emotion.horny){
                emotes.put(e, emotes.get(e)-((emotes.get(e)-50)/2));
            }
            else if(!mostlyNude()&&e==Emotion.confident){
                emotes.put(e, emotes.get(e)-((emotes.get(e)-50)/2));
            }
            else{
                if(emotes.get(e)>=5){
                    emotes.put(e, emotes.get(e)-(emotes.get(e)/2));
                }
            }
        }
    }
    
    @Override
    public void endOfMatchRound() {
        super.endOfMatchRound();
        moodSwing();
        decayMood();
        update();
    }

    public Map<String, List<CharacterLine>> getLines() {
        return ai.lines;
    }

    @Override
    public void matchPrep(Match m) {
        super.matchPrep(m);
        ArmManager manager = ai.getArmManager();
        if (manager != null) {
            ai.initializeArms(manager);
            if (manager.getActiveArms().stream().anyMatch(a -> a.getType() == ArmType.STABILIZER)) {
                add(Trait.stabilized);
            } else {
                remove(Trait.stabilized);
            }
        };
    }

    @Nullable
    @Override
    public ArmManager getArmManager() {
        return ai.getArmManager();
    }

    @Override
    public Person getGrammar() {
        if (useFemalePronouns()) {
            return new SingularFeminineThirdPerson(this);
        } else {
            return new SingularMasculineThirdPerson(this);
        }
    }
}

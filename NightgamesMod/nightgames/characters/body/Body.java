package nightgames.characters.body;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.CharacterSex;
import nightgames.characters.Trait;
import nightgames.characters.body.AssPart.Size;
import nightgames.combat.Combat;
import nightgames.global.Flag;
import nightgames.global.Global;
import nightgames.items.clothing.Clothing;
import nightgames.items.clothing.ClothingSlot;
import nightgames.json.JsonUtils;
import nightgames.nskills.tags.SkillTag;
import nightgames.pet.PetCharacter;
import nightgames.skills.Divide;
import nightgames.skills.Skill;
import nightgames.status.*;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Body implements Cloneable {
    private static class PartReplacement {
        private Optional<GenericBodyPart> added;
        private Optional<GenericBodyPart> removed;
        private int duration;

        PartReplacement(int duration) {
            added = Optional.empty();
            removed = Optional.empty();
            this.duration = duration;
        }

        PartReplacement(PartReplacement original) {
            added = original.added;
            removed = original.removed;
            duration = original.duration;
        }

        private void timePasses() {
            duration -= 1;
        }

        private boolean isExpired() {
            return duration <= 0;
        }
    }

    // yeah i know :(
    public static GenericBodyPart nonePart = new GenericBodyPart("none", 0, 1, 1, "none", "");
    private static Set<String> pluralParts = Set.of(HandsPart.TYPE,
        FeetPart.TYPE,
        WingsPart.TYPE,
        BreastsPart.TYPE,
        BallsPart.TYPE);
    private final static GenericBodyPart[] requiredParts = {
        new HandsPart(),
        new FeetPart(),
        new SkinPart(),
        new AssPart(AssPart.Size.Normal),
        new MouthPart(),
        new BreastsPart(0),
        new EarsPart()};
    private final static String[] fetishParts = {AssPart.TYPE, FeetPart.TYPE, CockPart.TYPE, WingsPart.TYPE, TailPart.TYPE, TentaclePart.TYPE, BreastsPart.TYPE};

    private LinkedHashSet<GenericBodyPart> bodyParts;
    private transient Collection<PartReplacement> replacements;
    private transient Collection<GenericBodyPart> currentParts;
    transient public Character character;
    public double baseFemininity;
    private double height;

    public Body() {
        bodyParts = new LinkedHashSet<>();
        currentParts = new HashSet<>();
        replacements = new ArrayList<>();
        height = 170;
    }

    public Body(Character character) {
        this();
        this.character = character;
    }

    private Collection<GenericBodyPart> getCurrentParts() {
        return currentParts;
    }

    public List<BodyPart> getCurrentPartsThatMatch(Predicate<BodyPart> filterPredicate){
        return getCurrentParts().stream().filter(filterPredicate).collect(Collectors.toList());
    }

    private void updateCurrentParts() {
        currentParts.clear();
        currentParts.addAll(bodyParts);
        for (PartReplacement r : replacements) {
            r.removed.ifPresent(p -> currentParts.remove(p));
            r.added.ifPresent(p -> currentParts.add(p));
        }
    }

    public void temporaryAddPart(GenericBodyPart part, int duration) {
        if (getRandom(part.getType()) != null) {
            setTemporaryPartDuration(part, duration);
        } else {
            PartReplacement replacement = new PartReplacement(duration);
            replacement.added = Optional.of(part);
            replacements.add(replacement);
            updateCurrentParts();
            if (character != null) {
                updateCharacter();
            }
        }
    }

    public void temporaryRemovePart(GenericBodyPart part, int duration) {
        PartReplacement replacement = new PartReplacement(duration);
        replacement.removed = Optional.of(part);
        replacements.add(replacement);
        updateCurrentParts();
        if (character != null) {
            updateCharacter();
        }
    }

    private BodyPart getPartIn(String type, Collection<GenericBodyPart> parts) {
        for (BodyPart p : parts) {
            if (p.isType(type)) {
                return p;
            }
        }
        return null;
    }

    public void setTemporaryPartDuration(GenericBodyPart part, int newDuration) {
        assert part != null;
        for (PartReplacement r : replacements) {
            BodyPart other;
            if (r.added.isPresent() && r.added.get().equals(part)) {
                other = part;
            } else {
                other = getPartIn(part.getType(), r.added.stream().collect(Collectors.toList()));
            }
            assert other != null;
            r.added = Optional.of(part);
            r.duration = Math.max(newDuration, r.duration);
            break;
        }
        updateCurrentParts();
        if (character != null) {
            updateCharacter();
        }
    }

    public void describe(StringBuilder b, Character other, String delimiter) {
        describe(b, other, delimiter, true);
    }

    public void describe(StringBuilder b, Character other, String delimiter, boolean hideInvisible) {
        List<BodyPart> sortedParts = new ArrayList<>(getCurrentParts());
        sortedParts.sort(SORTER);
        for (BodyPart part : sortedParts) {
            if ((!hideInvisible || part.isVisible(character)) && part.isNotable()) {
                int prevLength = b.length();
                part.describeLong(b, character);
                if (prevLength != b.length()) {
                    b.append(delimiter);
                }
            }
        }
        b.append(formatHotnessText(other));
    }

    private String formatHotnessText(Character other) {
        int topLayer = Optional.ofNullable(character.outfit.getTopOfSlot(ClothingSlot.top)).map(Clothing::getLayer).orElse(-1);
        int bottomLayer = Optional.ofNullable(character.outfit.getTopOfSlot(ClothingSlot.bottom)).map(Clothing::getLayer).orElse(-1);
        JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/hotness_text.twig");
        JtwigModel model = JtwigModel.newModel()
            .with("self", character)
            .with("other", other)
            .with("hotness", getHotness(other))
            .with("topLayer", topLayer)
            .with("bottomLayer", bottomLayer);
        return template.render(model).replace(System.lineSeparator(), "");
    }
    private static final BodyPartSorter SORTER = new BodyPartSorter();
    public void describeBodyText(StringBuilder b, Character other, boolean notableOnly) {
        b.append(character.possessiveAdjective() + " body has ");
        BodyPart previous = null;
        List<BodyPart> sortedParts = new ArrayList<>(getCurrentParts());
        sortedParts.sort(SORTER);
        for (BodyPart part : sortedParts) {
            if (!notableOnly || part.isNotable()) {
                if (previous != null) {
                    b.append(Global.prependPrefix(previous.prefix(), previous.fullDescribe(character)));
                    b.append(", ");
                }
                previous = part;
            }
        }
        if (previous == null) {
            b.append("nothing notable.<br/>");
        } else {
            b.append("and ");
            b.append(Global.prependPrefix(previous.prefix(), previous.fullDescribe(character)));
            b.append(".<br/>");
        }
        b.append(formatHotnessText(other));
    }

    public void add(GenericBodyPart part) {
        assert part != null;
        bodyParts.add(part);
        updateCurrentParts();
        updateCharacter();
    }

    private void updateCharacter() {
        if (character != null) {
            character.update();
        }
    }

    public boolean contains(GenericBodyPart part) {
        return getCurrentParts().contains(part);
    }

    public PussyPart getRandomPussy() {
        return (PussyPart) getRandom(PussyPart.TYPE);

    }

    public WingsPart getRandomWings() {
        return (WingsPart) getRandom(WingsPart.TYPE);
    }

    public AssPart getRandomAss() {
        return (AssPart) getRandom(AssPart.TYPE);
    }

    public BreastsPart getRandomBreasts() {
        return (BreastsPart) getRandom(BreastsPart.TYPE);
    }

    public Optional<BodyFetish> getFetish(String part) {
        Optional<Status> fs = character.status.stream().filter(status -> {
            if (status.flags().contains(Stsflag.bodyfetish)) {
                BodyFetish fetish = (BodyFetish) status;
                if (fetish.part.equalsIgnoreCase(part)) {
                    return true;
                }
            }
            return false;
        }).findFirst();
        return fs.map(status -> (BodyFetish) status);
    }

    public double getHotness(Character opponent) {
        // represents tempt damage
        double bodyHotness = 1.0;
        for (BodyPart part : getCurrentParts()) {
            bodyHotness += part.getHotness(character, opponent) * (getFetish(part.getType()).isPresent() ? 2 : 1);
        }
        double clothingHotness = character.outfit.getHotness();
        double totalHotness = bodyHotness * (.5 + character.getExposure()) + clothingHotness;
        if (character.is(Stsflag.glamour)) {
            totalHotness += 2.0;
        }
        if (character.is(Stsflag.alluring)) {
            totalHotness *= 1.5;
        }
        if (character.has(Trait.attractive)) {
            totalHotness *= 1.25;
        }
        if (character.has(Trait.unpleasant)) {
            totalHotness *= .75;
        }
        if (character.has(Trait.PinkHaze) && opponent.is(Stsflag.charmed)) {
            totalHotness = Math.max(totalHotness * 1.5, totalHotness + 3.0);
        }
        return totalHotness;
    }

    public void remove(GenericBodyPart part) {
        bodyParts.remove(part);

        updateCurrentParts();
        if (character != null) {
            updateCharacter();
        }
    }

    // returns how many are removed
    public int removeAll(String type) {
        List<BodyPart> removed = new ArrayList<>();
        for (BodyPart part : bodyParts) {
            assert part != null;
            if (part.isType(type)) {
                removed.add(part);
            }
        }
        for (BodyPart part : removed) {
            bodyParts.remove(part);
        }
        updateCurrentParts();

        if (character != null) {
            updateCharacter();
        }
        return removed.size();
    }

    public CockPart getRandomCock() {
        return (CockPart) getRandom(CockPart.TYPE);
    }
    
    public List<BodyPart> getAllGenitals() {
        List<String> partTypes = Arrays.asList(CockPart.TYPE, "pussy", StraponPart.TYPE, AssPart.TYPE);
        return getCurrentPartsThatMatch(part -> partTypes.contains(part.getType()));
    }

    public BodyPart getRandomInsertable() {
        BodyPart part = getRandomCock();
        if (part == null && character.has(Trait.strapped)) {
            part = new StraponPart();
        }
        return part;
    }

    public boolean has(String type) {
        return getRandom(type) != null;
    }

    public BodyPart getRandom(String type) {
        List<BodyPart> parts = getCurrentParts().stream()
            .filter(p -> p.isType(type))
            .collect(Collectors.toList());
        BodyPart part = null;
        if (parts.size() > 0) {
            part = parts.get(Global.random(parts.size()));
        }
        return part;
    }
    
    public BodyPart getRandomErogenous() {
        return Global.pickRandom(getCurrentPartsThatMatch(BodyPart::isErogenous)).get();
    }

    public int pleasure(Character opponent, BodyPart with, BodyPart target, double magnitude, Combat c) {
        return pleasure(opponent, with, target, magnitude, 0, c, null);
    }

    public int pleasure(Character opponent, BodyPart with, BodyPart target, double magnitude, Combat c, Skill skill) {
        return pleasure(opponent, with, target, magnitude, 0, c, skill);
    }

    public int pleasure(Character opponent, BodyPart with, BodyPart target, double magnitude,
        int bonus, Combat c, Skill skill) {
        if (target == null) {
            target = nonePart;
        }
        if (with == null) {
            with = nonePart;
        }
        if (target.getType().equals(StraponPart.TYPE)) {
            return 0;
        }

        double sensitivity = target.getSensitivity(opponent);
        if (character.has(Trait.desensitized)) {
            sensitivity -= .5;
        }
        if (character.has(Trait.desensitized2)) {
            sensitivity -= .5;
        }
        if (target.isErogenous() && character.has(Trait.hairtrigger)) {
            sensitivity += 1;
        }
        final double moddedSensitivity = sensitivity;
        sensitivity += character.status.stream()
            .mapToDouble(status -> status.sensitivity(moddedSensitivity, skill))
            .sum();
        if (opponent != null) {
            sensitivity += opponent.status.stream()
                .mapToDouble(status -> status.opponentSensitivity(moddedSensitivity, skill))
                .sum();
        }
        double pleasure = 1;
        if (!with.isType("none")) {
            pleasure = with.getPleasure(opponent);
        }
        double perceptionBonus = 1.0;
        if (opponent != null) {
            perceptionBonus *= 1 + (opponent.body.getCharismaBonus(c, character) - 1) / 2;
        }
        double baseBonusDamage = bonus;
        if (opponent != null) {
            baseBonusDamage += with.applyBonuses(opponent, character, target, magnitude, c);
            baseBonusDamage += target.applyReceiveBonuses(character, opponent, with, magnitude, c);
            // double the base damage if the opponent is submissive and in a
            // submissive stance
            if (c.getStance().sub(opponent) && opponent.has(Trait.submissive) && target.isErogenous()) {
                baseBonusDamage += baseBonusDamage + magnitude;
            } else if (c.getStance().dom(opponent) && opponent.has(Trait.submissive) && !opponent.has(Trait.flexibleRole) && target.isErogenous()) {
                baseBonusDamage -= (baseBonusDamage + magnitude) * 1. / 3.;
            }
        }

        if (character.has(Trait.NaturalHeat) && character.is(Stsflag.frenzied)) {
            baseBonusDamage -= (baseBonusDamage + magnitude) / 2;
        }

        Optional<BodyFetish> fetish = getFetish(with.getType());
        if (fetish.isPresent()) {
            double fetishBonus = fetish.get().magnitude * 3 * with.getFetishEffectiveness();
            if ( with.getType().equals(AssPart.TYPE) && character.has(Trait.analFanatic) ) {
                fetishBonus /= 4;
            }
            perceptionBonus += fetishBonus;
            // if a fetish is present, the chance of it intensifying is 4 times the chance of a
            // new fetish occurring of that type with fetishtrainer
            if (Global.random(100) > 4*100*with.getFetishChance()) {
                character.add(c,
                    new BodyFetish(character, opponent, with.getType(), .05));
            }
        }
        double base = baseBonusDamage + magnitude;

        // use the status bonus damage as part of the multiplier instead of adding to the base.
        double statusBonusDamage = 0;
        for (Status s : character.status) {
            statusBonusDamage += s.pleasure(c, with, target, base);
        }

        if (base > 0) {
            double statusMultiplier = (base + statusBonusDamage) / base;
            sensitivity += statusMultiplier - 1;
        }

        boolean unsatisfied = false;
        if (character.has(Trait.Unsatisfied)
            && (character.arousal.percent() >= 50)
            && (skill == null || !skill.getTags(c).contains(SkillTag.fucking))
            && !(with.isGenitalOrToy() && target.isGenitalOrToy() && c.getStance().havingSex(c))) {
            if (c != null && c.getOpponentCharacter(character).human()) {
                pleasure -= 4;
            } else {
                pleasure -= .8;
            }
            unsatisfied = true;
        }

        double dominance = 0.0;
        perceptionBonus += dominance;

        double multiplier = Math.max(0, 1 + ((sensitivity - 1) + (pleasure - 1) + (perceptionBonus - 1)));
        double stageMultiplier = pleasureResolveCalculateStageMultiplier(Optional.ofNullable(skill));
        double staleness = pleasureResolveCalculateStaleness(c, Optional.ofNullable(skill));
        multiplier = Math.max(0, multiplier + stageMultiplier) * staleness;

        double damage = base * multiplier;
        double perceptionlessDamage = base * (multiplier - (perceptionBonus - 1));

        int result = (int) Math.round(damage);
        if (character.is(Stsflag.rewired)) {
            character.pain(c, opponent, result, false, false);
            return 0;
        }

        pleasureResolveWriteSynopsis(c,
            opponent,
            with,
            target,
            skill,
            base,
            baseBonusDamage,
            result,
            dominance,
            staleness,
            magnitude,
            multiplier,
            sensitivity,
            pleasure,
            perceptionBonus,
            perceptionlessDamage);

        pleasureResolveUnsatisfied(c, opponent, unsatisfied);
        if (skill != null) {
            pleasureResolveStaleness(c, opponent, skill, staleness);
        }
        double percentPleasure = 100.0 * result / character.arousal.max();
        pleasureResolveSexualDynamo(c, opponent, percentPleasure);
        pleasureResolveShowmanship(c, opponent, percentPleasure, result, skill);

        character.resolvePleasure(result, c, opponent, target, with);
        pleasureResolveFetishTrainer(c, opponent, with);
        return result;
    }

    private double pleasureResolveCalculateStageMultiplier(Optional<Skill> skill) {
        return skill.map(s -> s.getStage().multiplierFor(character)).orElse(0.0);
    }

    private double pleasureResolveCalculateStaleness(Combat c, Optional<Skill> skill) {
        return skill
            .filter(s -> s.getSelf() != null && c.getCombatantData(s.getSelf()) != null)
            .map(s -> c.getCombatantData(s.getSelf()).getMoveModifier(s))
            .orElse(1.0);
    }

    private void pleasureResolveWriteSynopsis(Combat c,
        Character opponent,
        BodyPart with,
        BodyPart target,
        Skill skill,
        double base,
        double baseBonusDamage,
        int result,
        double dominance,
        double staleness,
        double magnitude,
        double multiplier,
        double sensitivity,
        double pleasure,
        double perceptionBonus,
        double perceptionlessDamage) {
        if (opponent != null) {
            String pleasuredBy = opponent.nameOrPossessivePronoun() + " " + with.describe(opponent);
            if (with == nonePart) {
                pleasuredBy = opponent.subject();
            }
            String firstColor =
                character.human() ? "<font color='rgb(150,150,255)'>" : "<font color='rgb(255,150,150)'>";
            if (character.isPet()) {
                if (((PetCharacter)character).getSelf().owner().human()) {
                    firstColor = "<font color='rgb(130,225,200)'>";
                } else {
                    firstColor = "<font color='rgb(210,130,255)'>";
                }
            }
            String secondColor =
                opponent.human() ? "<font color='rgb(150,150,255)'>" : "<font color='rgb(255,150,150)'>";
            if (opponent.isPet()) {
                if (((PetCharacter)opponent).getSelf().owner().human()) {
                    secondColor = "<font color='rgb(130,225,200)'>";
                } else {
                    secondColor = "<font color='rgb(210,130,255)'>";
                }
            }
            String bonusString = baseBonusDamage > 0
                ? String.format(" + <font color='rgb(255,100,50)'>%.1f</font>", baseBonusDamage)
                : baseBonusDamage < 0 ? String.format(" + <font color='rgb(50,100,255)'>%.1f</font>", baseBonusDamage) : "";
            String stageString = skill == null ? "" : String.format(" + stage:%.2f", skill.multiplierForStage(character));
            String dominanceString = dominance < 0.01 ? "" : String.format(" + dominance:%.2f", dominance);
            String staleString = staleness < .99 ? String.format(" x staleness: %.2f", staleness) : "";
            if (Global.checkFlag(Flag.basicSystemMessages)) {
                String battleString = String.format(
                    "%s%s %s</font> was pleasured by %s%s</font> for"
                        + " <font color='rgb(255,50,200)'>%d</font>.\n",
                    firstColor,
                    Global.capitalizeFirstLetter(character.nameOrPossessivePronoun()),
                    target.describe(character),
                    secondColor,
                    pleasuredBy,
                    result);
                if (c != null) {
                    c.writeSystemMessage(battleString, true);
                }
            } else {
                String battleString = String.format(
                    "%s%s %s</font> was pleasured by %s%s</font> for <font color='rgb(255,50,200)'>%d</font> "
                        + "base:%.1f (%.1f%s) x multiplier: %.2f (1 + sen:%.1f + ple:%.1f + per:%.1f %s %s)%s\n",
                    firstColor,
                    Global.capitalizeFirstLetter(character.nameOrPossessivePronoun()),
                    target.describe(character),
                    secondColor,
                    pleasuredBy,
                    result,
                    base,
                    magnitude,
                    bonusString,
                    multiplier,
                    sensitivity - 1,
                    pleasure - 1,
                    perceptionBonus - 1,
                    stageString,
                    dominanceString,
                    staleString);
                if (c != null) {
                    c.writeSystemMessage(battleString, false);
                }
            }
            Optional<BodyFetish> otherFetish = opponent.body.getFetish(target.getType());
            if (otherFetish.isPresent()
                && otherFetish.get().magnitude > .3
                && perceptionlessDamage > 0
                && skill != null
                && skill.getSelf().equals(character)
                && opponent != character
                && opponent.canRespond()) {
                c.write(character,
                    Global.format("Playing with {other:possessive} {other:body-part:%s} "
                            + "arouses {self:direct-object} almost as much as {other:direct-object}.",
                        opponent, character, target.getType()));
                opponent.temptNoSkill(c, character, target,
                    (int) Math.round(perceptionlessDamage * (otherFetish.get().magnitude - .2)));
            }
        } else {
            String firstColor =
                character.human() ? "<font color='rgb(150,150,255)'>" : "<font color='rgb(255,150,150)'>";
            String bonusString = baseBonusDamage > 0
                ? String.format(" + <font color='rgb(255,100,50)'>%.1f</font>", baseBonusDamage)
                : "";
            String battleString = String.format(
                "%s%s %s</font> was pleasured for <font color='rgb(255,50,200)'>%d</font> "
                    + "base:%.1f (%.2f%s) x multiplier: %.2f (sen:%.1f + ple:%.1f + per:%.1f)\n",
                firstColor,
                Global.capitalizeFirstLetter(character.nameOrPossessivePronoun()),
                target.describe(character),
                result,
                base,
                magnitude,
                bonusString,
                multiplier,
                sensitivity - 1,
                pleasure - 1,
                perceptionBonus - 1);
            if (c != null) {
                c.writeSystemMessage(battleString, false);
            }
        }
    }

    private void pleasureResolveUnsatisfied(Combat c, Character opponent, boolean unsatisfied) {
        if (unsatisfied) {
            c.write(character,
                Global.format("Foreplay doesn't seem to do it for {self:name-do} anymore. "
                        + "{self:PRONOUN-ACTION:clearly need|clearly needs} to fuck!",
                    character, opponent));
        }

    }

    private void pleasureResolveStaleness(Combat c, Character opponent, Skill skill, double staleness) {
        if (staleness > .51 && skill.user().human()) {
            c.write(opponent,
                Global.format("This seems to be a getting bit boring for "
                        + "{other:direct-object}... Maybe it's time to switch it up?",
                    opponent, character));
        }
    }

    private void pleasureResolveSexualDynamo(Combat c, Character opponent, double percentPleasureDealt) {
        if (character.has(Trait.sexualDynamo)
            && percentPleasureDealt >= 5
            && Global.random(4) == 0) {
            c.write(character, Global.format("Sexual pleasure seems only to feed {self:name-possessive} ", character, opponent));
            character.buildMojo(c, (int)Math.floor(percentPleasureDealt));
        }
    }

    private void pleasureResolveShowmanship(Combat c, Character opponent, double percentPleasureDealt, int pleasureDamage, Skill skill) {
        if (character.has(Trait.showmanship)
            && percentPleasureDealt >= 5
            && opponent.isPet()
            && ((PetCharacter)opponent).getSelf().owner().equals(character)) {
            Character voyeur = c.getOpponentCharacter(character);
            c.write(character, Global.format("{self:NAME-POSSESSIVE} moans as {other:subject-action:make|makes} a show of pleasing {other:possessive} {self:master} "
                + "turns %s on immensely.", character, opponent, voyeur.nameDirectObject()));
            voyeur.tempt(c, character, null, Math.max(Global.random(14, 20), pleasureDamage / 3), skill);
        }
    }

    private void pleasureResolveFetishTrainer(Combat c, Character opponent, BodyPart with) {
        if (opponent != null && Arrays.asList(fetishParts).contains(with.getType())) {
            double chance = opponent.has(Trait.fetishTrainer)?4 * Math.min(opponent.get(Attribute.Fetish), 25):0;

            if (Global.random(100) < chance * with.getFetishChance()) {
                c.write(character,
                    character.subjectAction("now have", "now has")
                        + " a new fetish, courtesy of "
                        + opponent.objectPronoun() + ".");
                character.add(c, new BodyFetish(character, opponent, with.getType(), .25));
            }
            if (opponent.has(Trait.fetishCharmer)
                && Global.random(100) < 4 * Math.min(opponent.get(Attribute.Fetish), 25)) {
                c.write(character,
                    character.subjectAction("find yourself", "finds themself")
                        + " hesitant to resist "
                        + opponent.objectPronoun() + " due to the demands of "
                        +character.possessiveAdjective()+" fetish.");
                character.add(c, new Charmed(character));
            }
        }
    }

    private static Map<Integer, Double> SEDUCTION_DIMISHING_RETURNS_CURVE = new HashMap<>();
    static {
        SEDUCTION_DIMISHING_RETURNS_CURVE.put(0, .06); // 0.6
        SEDUCTION_DIMISHING_RETURNS_CURVE.put(1, .05); // 1.1
        SEDUCTION_DIMISHING_RETURNS_CURVE.put(2, .04); // 1.5
        SEDUCTION_DIMISHING_RETURNS_CURVE.put(3, .03); // 1.8
        SEDUCTION_DIMISHING_RETURNS_CURVE.put(4, .02); // 2.1
    }

    /**
     * Gets how much your opponent views this body. 
     */
    public double getCharismaBonus(Combat c, Character opponent) {
        // you don't get turned on by yourself
        if (opponent == character) {
            return 1.0;
        } else {
            double effectiveSeduction = character.get(Attribute.Seduction);
            if (c.getStance().dom(character) && character.has(Trait.brutesCharisma)) {
                effectiveSeduction += (character.get(Attribute.Power) / 5.0 + character.get(Attribute.Ki) / 5.0);
            }

            if (character.has(Trait.PrimalHeat) && character.is(Stsflag.frenzied)) {
                effectiveSeduction += character.get(Attribute.Animism) / 2;
            }

            if (opponent.has(Trait.MindlessDesire) && character.is(Stsflag.frenzied)) {
                effectiveSeduction /= 2;
            }

            int seductionDiff = (int) Math.max(0, effectiveSeduction - opponent.get(Attribute.Seduction));
            double seductionBonus = 0;
            for (int i = 0; i < seductionDiff; i++) {
                seductionBonus += SEDUCTION_DIMISHING_RETURNS_CURVE.getOrDefault((i / 10), 0.01);   
            }
            double hotness = (getHotness(opponent) - 1) / 2 + 1;
            double perception = (1.0 + (opponent.get(Attribute.Perception) - 5) / 10.0);
            double perceptionBonus = (hotness + seductionBonus) * perception;

            if (opponent.is(Stsflag.lovestruck)) {
                perceptionBonus += 1;
            }
            if (character.has(Trait.romantic)) {
                perceptionBonus += Math.max(0, opponent.arousal.percent() - 70) / 100.0;
            }

            if (character.has(Trait.MindlessClone)) {
                perceptionBonus /= 3;
            }
            return perceptionBonus;
        }
    }

    public void finishBody(CharacterSex sex) {
        switch (sex) {
            case female:
                baseFemininity += 2;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, 2));
                }
                if (getRandomBreasts() == null) {
                    add(new BreastsPart(BreastsPart.Size.BCup));
                }
                if (getRandomAss() == null) {
                    add(new AssPart(Size.Flared));
                }
                break;
            case male:
                baseFemininity -= 2;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, -2));
                }
                break;
            case trap:
                baseFemininity += 2;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, 2));
                }
                if (getRandomAss() == null) {
                    add(new AssPart(Size.Normal));
                }
                break;
            case herm:
                baseFemininity += 1;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, 1));
                }
                if (getRandomBreasts() == null) {
                    add(new BreastsPart(BreastsPart.Size.BCup));
                }
                if (getRandomAss() == null) {
                    add(new AssPart(Size.Girlish));
                }
                break;
            case shemale:
                baseFemininity += 1;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, 1));
                }
                if (getRandomBreasts() == null) {
                    add(new BreastsPart(BreastsPart.Size.DCup));
                }
                if (getRandomAss() == null) {
                    add(new AssPart(Size.Girlish));
                }
                break;
            case asexual:
                baseFemininity += 0;
                if (!has(FacePart.TYPE)) {
                    add(new FacePart(0, 0));
                }
                break;
            default:
                break;
        }
        for (GenericBodyPart part : requiredParts) {
            if (!has(part.getType())) {
                add(part);
            }
        }
    }

    private void replacePussyWithCock(GenericBodyPart basicCock) {
        PussyPart pussy = getRandomPussy();
        removeAll(PussyPart.TYPE);
        add(pussy == null ? basicCock : pussy.getEquivalentCock());
    }

    private void replaceCockWithPussy() {
        CockPart cock = getRandomCock();
        removeAll(CockPart.TYPE);
        add(cock == null ? new PussyPart() : cock.getEquivalentPussy());
    }

    private void addEquivalentCockAndPussy(GenericBodyPart basicCock) {
        boolean hasPussy = getRandomPussy() != null;
        boolean hasCock = getRandomCock() != null;
        if (!hasPussy) {
            CockPart cock = getRandomCock();
            add(cock == null ? new PussyPart() : cock.getEquivalentPussy());
        }
        if (!hasCock) {
            PussyPart pussy = getRandomPussy();
            add(pussy == null ? basicCock : pussy.getEquivalentCock());
        }
    }

    private void addBallsIfNeeded() {
        if (getRandomBalls() == null) {
            add(new BallsPart());
        }
    }

    /**
     * Guesses the character sex based on the current attributes.
     * I'm sorry if I whatever you want to be considered, you're free to add it yourself.
     */
    public CharacterSex guessCharacterSex() {
        if (getRandomCock() != null && getRandomPussy() != null) {
            return CharacterSex.herm;
        } else if (getRandomCock() == null && getRandomPussy() == null) {
            return CharacterSex.asexual;
        } else if (getRandomCock() == null && getRandomPussy() != null) {
            return CharacterSex.female;
        } else {
            if (getRandomBreasts().getSize().compareTo(BreastsPart.Size.ACup) > 0 && getFace().getFemininity(character) > 0) {
                return CharacterSex.shemale;
            } else if (getFace().getFemininity(character) >= 1) {
                return CharacterSex.trap;
            }
            return CharacterSex.male;
        }
    }

    public void autoTG() {
        CharacterSex currentSex = guessCharacterSex();
        if (currentSex == CharacterSex.herm || currentSex == CharacterSex.asexual) {
            // no TG for herms or asexuals
            return;
        }
        if (character.useFemalePronouns() && Global.checkFlag(Flag.femaleTGIntoHerm)) {
            changeSex(CharacterSex.herm);
            return;
        }
        if (currentSex == CharacterSex.female) {
            changeSex(CharacterSex.male);
            return;
        }
        if (currentSex == CharacterSex.male || currentSex == CharacterSex.shemale || currentSex == CharacterSex.trap) {
            changeSex(CharacterSex.female);
            return;
        }
    }
    
    private void changeSex(CharacterSex newSex) {
        FacePart face = ((FacePart)getFace());
        double femininity = face.getFemininity(character);
        switch (newSex) {
            case male:
                femininity = Math.min(0, femininity);
                replacePussyWithCock(new CockPart(CockPart.Size.Average));
                addBallsIfNeeded();
                getRandomBreasts().setSize(BreastsPart.Size.FlatChest);
                break;
            case female:
                femininity = Math.max(2, femininity);
                replaceCockWithPussy();
                getRandomBreasts().setSize(BreastsPart.Size.CCup);
                break;
            case herm:
                femininity = Math.max(1, femininity);
                addEquivalentCockAndPussy(new CockPart(CockPart.Size.Big));
                getRandomBreasts().setSize(BreastsPart.Size.BCup);
                break;
            case shemale:
                femininity = Math.max(1, femininity);
                replacePussyWithCock(new CockPart(CockPart.Size.Big));
                getRandomBreasts().setSize(BreastsPart.Size.DCup);
                addBallsIfNeeded();
                break;
            case trap:
                femininity = Math.max(2, femininity);
                replacePussyWithCock(new CockPart(CockPart.Size.Small));
                getRandomBreasts().setSize(BreastsPart.Size.FlatChest);
                addBallsIfNeeded();
                break;
            case asexual:
                femininity = Math.max(0, femininity);
                break;
            default:
                break;
        }
        if (newSex.hasBalls()) {
            addBallsIfNeeded();
        } else {
            removeAll(BallsPart.TYPE);
        }
        face.setFemininity(femininity);
    }

    public void makeGenitalOrgans(CharacterSex sex) {
        if (sex.hasPussy()) {
            if (!has(PussyPart.TYPE)) {
                add(new PussyPart());
            }
        }
        if (sex.hasCock()) {
            if (!has(CockPart.TYPE)) {
                add(new CockPart(CockPart.Size.Average));
            }
        }
        if (sex.hasBalls()) {
            if (!has(BallsPart.TYPE)) {
                add(new BallsPart());
            }
        }
    }

    @Override
    public Body clone() throws CloneNotSupportedException {
        Body newBody = (Body) super.clone();
        newBody.replacements = new ArrayList<>();
        replacements.forEach(rep -> newBody.replacements.add(new PartReplacement(rep)));
        newBody.bodyParts = bodyParts.stream()
            .map(bp -> {
                if (bp != null) {
                    return bp.copy();
                } else {
                    return null;
                }
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));

        newBody.currentParts = new HashSet<>(getCurrentParts());
        return newBody;
    }

     public JsonObject save() {
        JsonObject bodyObj = new JsonObject();
        bodyObj.addProperty("femininity", baseFemininity);
        JsonArray partsArr = new JsonArray();
        for (BodyPart part : bodyParts) {
            JsonObject obj = part.save();
            obj.addProperty("class", part.getClass()
                                 .getCanonicalName());
            partsArr.add(obj);
        }
        bodyObj.add("parts", partsArr);
        return bodyObj;
    }

    public void loadParts(JsonArray partsArr) {
        for (JsonElement element : partsArr) {
            JsonObject partJson = element.getAsJsonObject();
            try {
                this.add(JsonUtils.getGson().fromJson(partJson, GenericBodyPart.class));
            } catch (Exception e) {
                System.out.println(partJson);
                throw e;
            }
        }
    }

    public static Body load(JsonObject bodyObj, Character character) {
        Body body = new Body(character);
        body.loadParts(bodyObj.getAsJsonArray("parts"));
        double defaultFemininity = 0;
        if (body.has(PussyPart.TYPE)) {
            defaultFemininity += 2;
        }
        if (body.has(CockPart.TYPE)) {
            defaultFemininity -= 2;
        }
        body.baseFemininity = JsonUtils.getOptional(bodyObj, "femininity").map(JsonElement::getAsDouble)
                        .orElse(defaultFemininity);
        body.updateCurrentParts();
        return body;
    }

    private void advancedTemporaryParts(Combat c) {
        bodyParts.stream()
            .filter(p -> p instanceof Sizable)
            .forEach(p -> ((Sizable) p).timePasses());

        Queue<PartReplacement> expired = new ArrayDeque<>();
        for (PartReplacement r : replacements) {
            r.timePasses();
            if (r.isExpired()) {
                expired.add(r);
            }
        }
        for (PartReplacement r : expired) {
            replacements.remove(r);
            updateCurrentParts();
            StringBuilder sb = new StringBuilder();
            LinkedList<BodyPart> added = new LinkedList<>();
            r.added.ifPresent(added::add);
            LinkedList<BodyPart> removed = new LinkedList<>();
            r.removed.ifPresent(removed::add);
            if (added.size() > 0 && removed.size() == 0) {
                sb.append(character.nameOrPossessivePronoun())
                    .append(" ");
                for (BodyPart p : added.subList(0, added.size() - 1)) {
                    sb.append(p.fullDescribe(character))
                      .append(", ");
                }
                if (added.size() > 1) {
                    sb.append(" and ");
                }
                sb.append(added.get(added.size() - 1)
                               .fullDescribe(character));
                sb.append(" disappeared.");
            } else if (removed.size() > 0 && added.size() == 0) {
                sb.append(character.nameOrPossessivePronoun())
                    .append(" ");
                for (BodyPart p : removed.subList(0, removed.size() - 1)) {
                    sb.append(p.fullDescribe(character))
                      .append(", ");
                }
                if (removed.size() > 1) {
                    sb.append(" and ");
                }
                sb.append(removed.get(removed.size() - 1)
                                 .fullDescribe(character));
                sb.append(" reappeared.");
            } else if (removed.size() > 0 && added.size() > 0) {
                sb.append(character.nameOrPossessivePronoun());
                for (BodyPart p : added.subList(0, added.size() - 1)) {
                    sb.append(p.fullDescribe(character))
                      .append(", ");
                }
                if (added.size() > 1) {
                    sb.append(" and ");
                }
                sb.append(added.get(added.size() - 1)
                               .fullDescribe(character));
                if (removed.size() == 1 && removed.get(0) == null) {
                    sb.append(" disappeared");
                } else {
                    sb.append(" turned back into ");
                }
                for (BodyPart p : removed.subList(0, removed.size() - 1)) {
                    sb.append(Global.prependPrefix(p.prefix(), p.fullDescribe(character)))
                      .append(", ");
                }
                if (removed.size() > 1) {
                    sb.append(" and ");
                }
                BodyPart last = removed.get(removed.size() - 1);
                if (last != null)
                sb.append(Global.prependPrefix(last.prefix(), last.fullDescribe(character)));
                sb.append('.');
            }
            Global.writeIfCombat(c, character, sb.toString());
        }

        for (var bp : currentParts) {
            if (bp != null) {
                bp.timePasses(c, character);
            }
        }
    }

    public void tick(Combat c) {
        advancedTemporaryParts(c);
        if (character != null) {
            updateCharacter();
        }
    }

    public BodyPart getRandomHole() {
        BodyPart part = getRandomPussy();
        if (part == null) {
            part = getRandomAss();
        }
        return part;
    }

    public int mod(Attribute a, int total) {
        int res = 0;
        for (BodyPart p : getCurrentParts()) {
            total += p.attributeModifier(a);
        }
        return res;
    }

    public void receiveCum(Combat c, Character opponent, BodyPart part) {
        if (part == null) {
            part = character.body.getSkin();
        }
        part.receiveCum(c, character, opponent, part);
        if (character.has(Trait.spiritphage)) {
            c.write(character, "<br/><b>" + Global.capitalizeFirstLetter(character.subjectAction("glow", "glows")
                            + " with power as the cum is absorbed by " + character.possessiveAdjective() + " "
                            + part.describe(character) + ".</b>"));
            character.add(c, new Abuff(character, Attribute.Power, 5, 10));
            character.add(c, new Abuff(character, Attribute.Seduction, 10, 10));
            character.add(c, new Abuff(character, Attribute.Cunning, 5, 10));
            character.buildMojo(c, 100);
        }
        if (opponent.has(Trait.hypnoticsemen)) {
            c.write(character, Global.format(
                "<br/><b>{other:NAME-POSSESSIVE} hypnotic semen takes its toll on "
                    + "{self:name-possessive} willpower, rendering {self:direct-object} doe-eyed "
                    + "and compliant.</b>",
                            character, opponent));
            character.loseWillpower(c, 10 + Global.random(10));
        }
        if (part.getType().equals(AssPart.TYPE) || part.getType().equals(PussyPart.TYPE)) {
            if (character.has(Trait.RapidMeiosis) && character.has(Trait.slime)) {
                c.write(opponent, Global.format("{self:NAME-POSSESSIVE} hungry %s seems to "
                    + "vacuum {other:name-possessive} sperm into itself as "
                    + "{other:pronoun-action:cum|cums}. As {other:pronoun-action:lay|lays} there "
                    + "heaving in exertion, {self:possessive} belly rapidly bloats up as if going "
                    + "through 9 months of pregancy within seconds. With a groan, "
                    + "{self:pronoun-action:expel|expels} a massive quantity of slime onto the "
                    + "floor. The slime seems to quiver for a second before reforming itself into "
                    + "an exact copy of {self:name-do}!",
                    character, opponent, part.describe(character)));
                c.addPet(character, Divide.makeClone(c, character).getSelf());
            }
            if (opponent.has(Trait.RapidMeiosis) && opponent.has(Trait.slime)) {
                c.write(opponent, Global.format("After {other:name-possessive} gooey cum "
                    + "fills {self:name-possessive} %s, {self:pronoun-action:feel|feels} "
                    + "{self:possessive} belly suddenly churn and inflate. The faux-semen seems to "
                    + "be multiplying inside {self:direct-object}! Without warning, the sticky "
                    + "liquid makes a quick exit out of {self:possessive} orifice and reforms "
                    + "itself into a copy of {other:name-do}!",
                    character, opponent, part.describe(character)));
                c.addPet(opponent, Divide.makeClone(c, opponent).getSelf());
            }
        }
    }

    public void tickHolding(Combat c, Character opponent, BodyPart selfOrgan, BodyPart otherOrgan) {
        if (selfOrgan != null && otherOrgan != null) {
            selfOrgan.tickHolding(c, character, opponent, otherOrgan);
        }
    }

    public float penetrationFitnessModifier(Character self, Character other, boolean pitcher, boolean anal) {
        int totalCounterValue = 0;

        if (anal) {
            if (!pitcher) {
                totalCounterValue += getRandomAss()
                    .counterValue(other.body.getRandomCock(), self, other);
            } else {
                totalCounterValue += getRandomCock()
                    .counterValue(other.body.getRandomAss(), self, other);
            }
        } else {
            if (!pitcher) {
                totalCounterValue += getRandomPussy()
                    .counterValue(other.body.getRandomCock(), self, other);
            } else {
                totalCounterValue += getRandomCock()
                    .counterValue(other.body.getRandomPussy(), self, other);
            }
        }
        return 20 * totalCounterValue;
    }

    public Body clone(Character other) throws CloneNotSupportedException {
        Body res = clone();
        res.character = other;
        return res;
    }

    public void purge(Combat c) {
        for (PartReplacement r : replacements) {
            r.duration = 0;
        }
        advancedTemporaryParts(c);
        bodyParts.stream()
            .filter(bp -> bp instanceof GenericBodyPart)
            .forEach(bp -> ((GenericBodyPart) bp).purge());
    }

    public BodyPart getRandomGenital() {
        List<BodyPart> parts = new ArrayList<>();
        BodyPart pussy = getRandomPussy();
        BodyPart cock = getRandomCock();
        if (pussy != null) {
            parts.add(pussy);
        }
        if (cock != null) {
            parts.add(cock);
        }
        Collections.shuffle(parts);
        if (parts.size() >= 1) {
            return parts.get(0);
        } else {
            return getRandomBreasts();
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Body body = (Body) o;

        if (!(Math.abs(body.baseFemininity - baseFemininity) < 1e-6))
            return false;
        return bodyParts.equals(body.bodyParts);
    }

    @Override public int hashCode() {
        int result;
        long temp;
        result = bodyParts.hashCode();
        temp = Double.doubleToLongBits(baseFemininity);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public FacePart getFace() {
        return (FacePart)getRandom(FacePart.TYPE);
    }

    public SkinPart getSkin() { return (SkinPart) getRandom(SkinPart.TYPE); }

    public BallsPart getRandomBalls() { return (BallsPart) getRandom(BallsPart.TYPE); }

    public HandsPart getRandomHands() { return (HandsPart) getRandom(HandsPart.TYPE); }

    public FeetPart getRandomFeet() { return (FeetPart) getRandom(FeetPart.TYPE); }

    public MouthPart getRandomMouth() { return (MouthPart) getRandom(MouthPart.TYPE); }

    public EarsPart getRandomEars() { return (EarsPart) getRandom(EarsPart.TYPE); }

    // yeah i know it's not that simple, but best try right now
    public static String partArticle(String type) {
        if (pluralParts.contains(type)) {
            return "";
        } else if ("aeiouAEIOU".contains(type.substring(0, 1))){
            return "an ";
        } else {
            return "a ";
        }
    }

    public void onOrgasm(Combat c, Character self, Character opponent) {
        getCurrentParts().forEach(part -> part.onOrgasm(c, self, opponent));
    }

    public void mimic(Body other) {
        purge(null);
        Collection<GenericBodyPart> currentParts = new ArrayList<>(getCurrentParts());
        currentParts.forEach(part -> temporaryRemovePart(part, 1000));
        other.getCurrentParts().forEach(part -> temporaryAddPart(part, 1000));
    }
}
package nightgames.characters.body;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.items.clothing.Clothing;
import nightgames.items.clothing.ClothingSlot;
import nightgames.status.Abuff;
import nightgames.status.Charmed;
import nightgames.status.Stsflag;
import nightgames.status.addiction.Addiction;
import nightgames.status.addiction.AddictionType;

public class BreastsPart extends GenericBodyPart implements Sizable<BreastsPart.Size> {
    public enum Size implements _Size<Size> {
        FlatChest(0, "", "flat"),
        ACup(1, "A", "tiny"),
        BCup(2, "B", "smallish"),
        CCup(3, "C", "modest"),
        DCup(4, "D", "round"),
        DDCup(5, "DD", "large"),
        ECup(6, "E", "huge"),
        FCup(7, "F", "glorious"),
        GCup(8, "G", "massive"),
        HCup(9, "H", "colossal"),
        ICup(10, "I", "mammoth"),
        JCup(11, "J", "godly");

        private static HashMap<Integer, Size> map = new HashMap<>();

        static {
            for (Size s : Size.values()) {
                map.put(s.value, s);
            }
        }

        private static Optional<Size> fromValue(int v) {
            return Optional.of(map.get(v));
        }
        private static Size clampToValid(int v) {
            return fromValue(Global.clamp(v, min().value, max().value)).orElseThrow();
        }
        @Deprecated
        public static Size coerceFromInt(int v) {
            return fromValue(v).orElseThrow();
        }
        public static Size max() {
            return JCup;
        }
        public static Size min() {
            return FlatChest;
        }

        private int value;
        private String description;
        private String cupSize;

        Size(int v, String cupSize, String description) {
            value = v;
            this.description = description;
            this.cupSize = cupSize;
        }

        public Size withModifier(int modifier) {
            return clampToValid(value + modifier);
        }

        @Override
        public Size withModifications(
            Collection<TemporarySizeModification> modifications) {
            var v = value;
            v += modifications.stream()
                .mapToInt(TemporarySizeModification::getModifier)
                .sum();
            return clampToValid(v);
        }
    }

    public static final String TYPE = "breasts";

    private SizeTrait<BreastsPart.Size> sizeTrait;

    public BreastsPart() {
        super("breasts", "", 0.0, 1.0, 1.0, true, TYPE, "");
    }

    public BreastsPart(JsonObject js) {
        super(js);
        var size = Size.fromValue(js.get("size").getAsInt()).orElseThrow();
        sizeTrait = new SizeTrait<>(size);
    }

    public BreastsPart(int size) {
        this(Size.fromValue(size).orElseThrow());
    }

    public BreastsPart(Size size) {
        this();
        sizeTrait = new SizeTrait<>(size);
    }

    protected BreastsPart(BreastsPart original) {
        super(original);
        sizeTrait = new SizeTrait<>(original.sizeTrait);
    }

    @Override
    public boolean isVisible(Character c) {
        return c.breastsAvailable() || getSize().value > 0;
    }

    @Override
    public double getFemininity(Character c) {
        return 3 * ((double) getSize().value) / Size.max().value;
    }

    @Override
    public double getHotness(Character self, Character opponent) {
        double hotness = super.getHotness(self, opponent);
        Clothing top = self.outfit.getTopOfSlot(ClothingSlot.top);
        hotness += -.1 + Math.sqrt(getSize().value) * .15 * self.outfit
                                                .getExposure(ClothingSlot.top);
        if (!opponent.hasDick()) {
            hotness /= 2;
        }
        if (top == null) {
            hotness += .1;
        }
        return Math.max(0, hotness);
    }

    @Override
    public double getPleasure(Character self) {
        return (.25 + getSize().value * .35) * super.getPleasure(self);
    }

    @Override
    public double getSensitivity(Character self) {
        return (.75 + getSize().value * .2)* super.getSensitivity(self);
   }

    @Override
    public int attributeModifier(Attribute a) {
        var res = super.attributeModifier(a);
        switch (a) {
            case Speed:
                res -= Math.max(getSize().value - 3, 0) / 2;
                break;
            case Seduction:
                res += Math.max(getSize().value - 3, 0);
                break;
        }
        return res;
    }

    private static String[] synonyms = {"breasts", "tits", "boobs"};

    @Override
    public void describeLong(StringBuilder b, Character c) {
        if (c.hasPussy() || getSize().value > Size.min().value) {
            b.append(Global.capitalizeFirstLetter(fullDescribe(c)));
            b.append(" adorn " + c.nameOrPossessivePronoun() + " chest.");
        }
    }

    @Override
    public String fullDescribe(Character c) {
        var validDescriptions = new ArrayList<>(List.of(sizeTrait.getSize().description));
        if (!sizeTrait.getSize().cupSize.equals("")) {
            validDescriptions.add(sizeTrait.getSize().cupSize + "-cup");
        }
        return Global.pickRandom(validDescriptions).orElseThrow()
            + " "
            + super.fullDescribe(c);
    }

    protected String modlessDescription(Character c) {
        return Global.pickRandom(synonyms).get();
    }

    @Override
    public double applyBonuses(Character self, Character opponent, BodyPart target, double damage, Combat c) {
        double bonus = super.applyBonuses(self, opponent, target, damage, c);
        bonus += Math.max(5, getSize().value) + Global.random(Math.min(0, getSize().value - 4));
        return bonus;
    }

    @Override
    public double applyReceiveBonuses(Character self, Character opponent, BodyPart target, double damage, Combat c) {
        double bonus = super.applyReceiveBonuses(self, opponent, target, damage, c);
        if (self.has(Trait.lactating) && target.isType(MouthPart.TYPE)) {
            if (self.has(Trait.magicmilk)) {
                float addictionLevel;
                Addiction addiction;
                opponent.addict(c, AddictionType.MAGIC_MILK, self, Addiction.LOW_INCREASE);
                addiction = opponent.getAddiction(AddictionType.MAGIC_MILK);
                addictionLevel = addiction.getMagnitude();
                if (addictionLevel < Addiction.LOW_THRESHOLD) {
                    // not addicted
                    c.write(opponent,
                                    Global.format("{self:NAME-POSSESSIVE} milk makes the blood surge from {other:name-possessive} head into {other:possessive} crotch, leaving {other:direct-object} light-headed and horny",
                                                    self, opponent));
                } else if (addictionLevel < .3f) {
                    // starting addiction
                    c.write(opponent,
                                    Global.format("{self:NAME-POSSESSIVE} milk seems sweeter than usual. While {other:subject} know from experience that {self:possessive} saccharine cream is a powerful aphrodisiac, {other:pronoun} can't but help drinking down more.",
                                                    self, opponent));
                } else if (addictionLevel < .45f) {
                    // addicted
                    c.write(opponent,
                                    Global.format("As Cassie's milk dribbles down her breasts, you awake to a powerful need for her cream. Ignoring the potential aphrodisiac effectes, you quickly capture her nipples in your lips and relieve your parched throat with her delicious milk.",
                                                    self, opponent));
                } else if (addictionLevel < Addiction.HIGH_THRESHOLD) {
                    // dependent
                    c.write(opponent,
                                    Global.format("{other:NAME} desperately {other:action:suck|sucks} at {self:name-possessive} milky teats as soon as they're in front of {other:direct-object}. "
                                                    + "{other:POSSESSIVE} burning need to imbibe {self:possessive} sweet milk is overpowering all rational thought. "
                                                    + "{self:SUBJECT} smiles at {other:direct-object} and gently cradles {other:possessive} head, rocking {other:direct-object} back and forth while {other:subject} drink. "
                                                    + "The warm milk settles in {other:possessive} belly, slowly setting {other:possessive} body on fire with arousal.",
                                    self, opponent));
                } else {
                    // enslaved
                    c.write(opponent,
                                    Global.format("{other:SUBJECT} slavishly {other:action:wrap} {other:possessive} lips around {self:name-possessive} immaculate teats and start suckling. "
                                                    + "{other:POSSESSIVE} vision darkens around the edges and {other:possessive} world is completely focused on draining {self:possessive} wonderful breasts. "
                                                    + "{self:SUBJECT} smiles at {other:direct-object} and gently cradles {other:possessive} head, rocking {other:direct-object} back and forth while {other:subject} drink. "
                                                    + "The warm milk settles in {other:possessive} belly, slowly setting {other:possessive} body on fire with arousal.",
                                    self, opponent));
    
                }
                opponent.temptNoSkill(c, self, this, (int) (15 + addiction.getMagnitude() * 35));
    
                if (opponent.is(Stsflag.magicmilkcraving)) {
                    // temporarily relieve craving
                    addiction.alleviateCombat(c, Addiction.LOW_INCREASE);
                }
                if (c.getCombatantData(opponent) != null) {
                    int timesDrank = c.getCombatantData(opponent)
                                      .getIntegerFlag("drank_magicmilk")
                                    + 1;
                    c.getCombatantData(opponent)
                     .setIntegerFlag("drank_magicmilk", timesDrank);
                }
            }
            if (self.has(Trait.sedativecream)) {
                c.write(opponent,
                                Global.format("The power seems to leave {other:name-possessive} body as {other:pronoun-action:sip|sips} {self:possessive} cloying cream.",
                                                self, opponent));
                opponent.weaken(c, opponent.stamina.max() / 10);
                opponent.add(c, new Abuff(opponent, Attribute.Power, -Global.random(1, 3), 20));
            }
            if (self.has(Trait.Pacification)) {
                c.write(opponent,
                                Global.format("With every drop of {self:name-possessive} infernal milk {other:subject-action:swallow},"
                                                + " {self:pronoun} seems more and more impossibly beautiful to {other:possessive} eyes."
                                                + " Why would {other:pronoun} want to mar such perfect beauty?",
                                                self, opponent));
                opponent.add(c, new Charmed(opponent, 2).withFlagRemoved(Stsflag.mindgames));
            }
            if (self.has(Trait.PheromonedMilk) && !opponent.has(Trait.Rut)) {
                c.write(opponent, Global.format("<b>Drinking {self:possessive} breast milk sends {other:direct-object} into a chemically induced rut!</b>",
                                                self, opponent));
                opponent.addTemporaryTrait(Trait.Rut, 10);
            }
        }
        return bonus;
    }

    @Override
    public boolean isReady(Character c) {
        return true;
    }

    public String getFluidsNoMods(Character c) {
        if (c.has(Trait.lactating)) {
            return "milk";
        }
        return "";
    }

    @Override
    public boolean getDefaultErogenous() {
        return true;
    }

    @Override
    public double priority(Character c) {
        double priority = getPleasure(c);
        if (c.has(Trait.temptingtits)) {
            priority += .5;
        }
        if (c.has(Trait.beguilingbreasts)) {
            priority += .5;
        }
        return priority;
    }

    @Override
    public String adjective() {
        return "breast";
    }

    @Override
    public void changeSize(int modifier) {
        sizeTrait.changeSize(modifier);
    }

    @Override
    public void temporarilyChangeSize(int modifier, int duration) {
        sizeTrait.temporarilyChangeSize(modifier, duration);
    }

    @Override
    public void timePasses() {
        sizeTrait.timePasses();
    }

    @Override
    public Size getSize() {
        return sizeTrait.getSize();
    }

    @Override
    public void setSize(Size target) {
        sizeTrait.setSize(target);
    }

    @Override
    public BreastsPart copy() {
        return new BreastsPart(this);
    }

    @Override
    public boolean isMultipleObjects() {
        return true;
    }
}
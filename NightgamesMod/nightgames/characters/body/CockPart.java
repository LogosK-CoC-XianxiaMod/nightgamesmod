package nightgames.characters.body;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.mods.PartMod;
import nightgames.characters.body.mods.pitcher.CockMod;
import nightgames.characters.body.mods.pitcher.IncubusCockMod;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.items.clothing.Clothing;
import nightgames.items.clothing.ClothingSlot;
import nightgames.status.Sensitized;

public class CockPart extends GenericBodyPart implements Sizable<CockPart.Size> {
    public static final String TYPE = "cock";

    public enum Size implements _Size<Size> {
        Tiny(3, "tiny"),
        Small(4,"tiny"),
        Little(5, "small"),
        Average(6, ""),
        Large(7, "big"),
        Big(8, "huge"),
        Huge(9, "gigantic"),
        Massive(10, "massive"),
        Colossal(11, "colossal"),
        Mammoth(12, "mammoth");

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
        public static Size max() {
            return Mammoth;
        }
        public static Size min() {
            return Tiny;
        }
        private int value;
        private String description;

        Size(int v, String description) {
            value = v;
            this.description = description;
        }

        @Override
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
    
    public static String[] synonyms = {"cock", "dick", "shaft", "phallus"};

    private SizeTrait<Size> sizeTrait;

    public CockPart() {
        this(Size.Average);
    }

    public CockPart(JsonObject js) {
        super(js);
        sizeTrait = new SizeTrait<>(Size.fromValue(js.get("size").getAsInt()).orElseThrow());
    }

    @Deprecated
    public CockPart(int size) {
        this(Size.fromValue(size).orElseThrow());
    }

    public CockPart(Size size) {
        super("cock", "", 0, 1.2, 1, false, CockPart.TYPE, "a ");
        sizeTrait = new SizeTrait<>(size);
    }

    protected CockPart(CockPart original) {
        super(original);
        sizeTrait = new SizeTrait<>(original.sizeTrait);
    }

    @Override
    public double getFemininity(Character c) {
        return Size.Small.value - getSize().value;
    }

    @Override
    public int attributeModifier(Attribute a) {
        int bonus = super.attributeModifier(a);
        int size = getSize().value;
        if (size > Size.Average.value & a == Attribute.Seduction) {
            bonus += (size - Size.Average.value) * 2;
        }
        if (size > Size.Big.value & a == Attribute.Speed) {
            bonus += (size - Size.Big.value);
        }
        return bonus;
    }

    @Override
    public double getPleasure(Character self) {
        double pleasureMod = super.getPleasure(self);
        pleasureMod += self.has(Trait.sexTraining1) ? .5 : 0;
        pleasureMod += self.has(Trait.sexTraining2) ? .7 : 0;
        pleasureMod += self.has(Trait.sexTraining3) ? .7 : 0;
        return pleasureMod;
    }

    @Override
    protected String modlessDescription(Character c) {
        String syn = Global.pickRandom(synonyms).get();
        return (c != null && c.hasPussy() && !moddedPartCountsAs(IncubusCockMod.TYPE) ? "girl-" : "") + syn;
    }

    @Override
    public double applyBonuses(Character self, Character opponent, BodyPart target, double damage, Combat c) {
        double bonus = super.applyBonuses(self, opponent, target, damage, c);
        if (self.has(Trait.polecontrol) && self.canRespond()) {
            String desc = "";
            if (self.has(Trait.polecontrol)) {
                desc += "expert ";
            }
            c.write(self, Global.format(
                            "{self:SUBJECT-ACTION:use|uses} {self:possessive} " + desc
                                            + "cock control to grind against {other:name-possessive} inner walls, making {other:possessive} knuckles whiten as {other:pronoun} {other:action:moan|moans} uncontrollably.",
                            self, opponent));
            bonus += self.has(Trait.polecontrol) ? 8 : 0;
        }
        return bonus;
    }

    @Override
    public void tickHolding(Combat c, Character self, Character opponent, BodyPart otherOrgan) {
        super.tickHolding(c, self, opponent, otherOrgan);
    }

    @Override
    public double applyReceiveBonuses(Character self, Character opponent, BodyPart target, double damage, Combat c) {
        double bonus = super.applyReceiveBonuses(self, opponent, target, damage, c);
        if ((opponent.has(Trait.dickhandler) || opponent.has(Trait.anatomyknowledge)) && opponent.canRespond() && c.getStance().mobile(opponent)) {
            c.write(opponent,
                            Global.format("{other:NAME-POSSESSIVE} expert handling of {self:name-possessive} cock causes {self:subject} to shudder uncontrollably.",
                                            self, opponent));
            if (opponent.has(Trait.dickhandler)) {
                bonus += 5;
            }
            if (opponent.has(Trait.anatomyknowledge)) {
                bonus += 5;
            }
        }
        if (self.has(Trait.druglacedprecum) && !opponent.isPartProtected(target)) {
            opponent.add(c, new Sensitized(opponent, target, .2, 2.0, 20));
            c.write(self, Global.format("{self:NAME-POSSESSIVE} drug-laced precum is affecting {other:direct-object}.",
                            self, opponent));
        }
        return bonus;
    }

    @Override
    public boolean isReady(Character c) {
        return c.arousal.percent() >= 15 || c.has(Trait.alwaysready);
    }

    public String getFluidsNoMods(Character c) {
        return "semen";
    }

    @Override
    public boolean getDefaultErogenous() {
        return true;
    }

    @Override
    public double priority(Character c) {
        return 2 + (c.has(Trait.polecontrol)? 2 : 0) + (c.has(Trait.druglacedprecum) ? 1 : 0)+ (c.has(Trait.hypnoticsemen) ? 2 : 0);
    }

    @Override
    public String adjective() {
        return "phallic";
    }

    @Override
    public boolean isVisible(Character c) {
        return c.crotchAvailable() || getSize().compareTo(Size.Average) > 0;
    }

    @Override
    public String fullDescribe(Character c) {
        return sizeTrait.getSize().description
            + " "
            + super.fullDescribe(c);
    }

    @Override
    public void describeLong(StringBuilder b, Character c) {
        b.append("There is a ");
        if (c.crotchAvailable()) {
            b.append(fullDescribe(c));
            if (c.arousal.percent() <= 15) {
                b.append(" hanging between ");
            } else if (c.arousal.percent() <= 50 ) {
                b.append(" erect between ");
            } else {
                b.append(" proudly erect between ");
            }
            b.append(c.nameOrPossessivePronoun());
            b.append(" legs.");
        } else {
            b.append(sizeAdjective());
            b.append(" bulge in ");
            b.append(c.possessiveAdjective());
            b.append(" ");
            b.append(Optional.ofNullable(c.outfit.getTopOfSlot(ClothingSlot.bottom)).map(Clothing::getName).orElse("outfit"));
            b.append(" where ");
            b.append(c.possessiveAdjective());
            b.append(" crotch is.");
        }
    }

    private String sizeAdjective() {
        return sizeTrait.getSize().description + " ";
    }

    public PussyPart getEquivalentPussy() {
        List<PartMod> newMods = getMods().stream()
            .filter(mod -> mod instanceof CockMod)
            .map(mod -> ((CockMod) mod).getCorrespondingCatcherMod())
            .collect(Collectors.toList());
        PussyPart newPart = new PussyPart();

        for (PartMod mod : newMods) {
            newPart.addMod(mod);
        }
        return newPart;
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
    public CockPart copy() {
        return new CockPart(this);
    }
}

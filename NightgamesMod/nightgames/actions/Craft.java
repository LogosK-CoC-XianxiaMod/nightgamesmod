package nightgames.actions;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Participant;
import nightgames.match.defaults.DefaultEncounter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Craft extends Action {
    private static final long serialVersionUID = 3199968029862277675L;

    private static class Aftermath extends Action.Aftermath {
        private Aftermath() {
        }

        @Override
        public String describe(Character c) {
            return " start mixing various liquids. Whatever it is doesn't look healthy.";
        }
    }

    public static class State implements Participant.PState {
        @Override
        public nightgames.characters.State getEnum() {
            return nightgames.characters.State.crafting;
        }

        @Override
        public boolean allowsNormalActions() {
            return false;
        }

        @Override
        public void move(Participant p) {
            Collection<Item> craftedItems;
            int roll = Global.random(15);
            if (p.getCharacter().check(Attribute.Cunning, 25)) {
                if (roll == 9) {
                    craftedItems = List.of(Item.Aphrodisiac, Item.DisSol);
                } else if (roll >= 5) {
                    craftedItems = List.of(Item.Aphrodisiac);
                } else {
                    craftedItems = List.of(Item.Lubricant, Item.Sedative);
                }
            } else if (p.getCharacter().check(Attribute.Cunning, 20)) {
                if (roll == 9) {
                    craftedItems = List.of(Item.Aphrodisiac);
                } else if (roll >= 7) {
                    craftedItems = List.of(Item.DisSol);
                } else if (roll >= 5) {
                    craftedItems = List.of(Item.Lubricant);
                } else if (roll >= 3) {
                    craftedItems = List.of(Item.Sedative);
                } else {
                    craftedItems = List.of(Item.EnergyDrink);
                }
            } else if (p.getCharacter().check(Attribute.Cunning, 15)) {
                if (roll == 9) {
                    craftedItems = List.of(Item.Aphrodisiac);
                } else if (roll >= 8) {
                    craftedItems = List.of(Item.DisSol);
                } else if (roll >= 7) {
                    craftedItems = List.of(Item.Lubricant);
                } else if (roll >= 6) {
                    craftedItems = List.of(Item.EnergyDrink);
                } else {
                    craftedItems = List.of();
                }
            } else if (roll >= 7) {
                craftedItems = List.of(Item.Lubricant);
            } else if (roll >= 5) {
                craftedItems = List.of(Item.Sedative);
            } else {
                craftedItems = List.of();
            }
            p.getCharacter().craft(craftedItems);
            p.state = new Ready();
        }

        @Override
        public boolean isDetectable() {
            return true;
        }

        @Override
        public Optional<Runnable> eligibleCombatReplacement(DefaultEncounter encounter, Participant p, Participant other) {
            return Optional.of(() -> encounter.spy(other, p));
        }

        @Override
        public Optional<Runnable> ineligibleCombatReplacement(Participant p, Participant other) {
            return Optional.empty();
        }

        @Override
        public int spotCheckDifficultyModifier(Participant p) {
            throw new UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.getCharacter().getTrueName()));
        }
    }

    public Craft() {
        super("Craft Potion");
    }

    @Override
    public boolean usable(Participant user) {
        return user.getCharacter().get(Attribute.Cunning) > 15 && !user.getCharacter().bound();
    }

    @Override
    public Action.Aftermath execute(Participant user) {
        user.state = new State();
        return new Aftermath();
    }

}

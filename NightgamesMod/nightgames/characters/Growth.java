package nightgames.characters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nightgames.global.Flag;
import nightgames.global.Global;

public class Growth {
    public float arousal;
    public float stamina;
    public float bonusArousal;
    public float bonusStamina;
    public int attributes[];
    public int bonusAttributes;
    public float willpower;
    public float bonusWillpower;
    public float mojo;
    public float bonusMojo;
    private Map<Integer, List<Trait>> traits;
    public Map<Integer, Runnable> actions;

    public Growth() {
        stamina = 2;
        arousal = 4;
        bonusStamina = 2;
        bonusArousal = 3;
        bonusAttributes = 1;
        willpower = .5f;
        bonusWillpower = .25f;
        attributes = new int[50];
        Arrays.fill(attributes, 4);
        attributes[0] = 3;
        traits = new HashMap<>();
        actions = new HashMap<>();
    }

    public void addTrait(int level, Trait trait) {
        if (!traits.containsKey(level)) {
            traits.put(level, new ArrayList<Trait>());
        }
        traits.get(level).add(trait);
    }

    public List<Trait> traitsAtLevel(int level) {
        return traits.get(level);
    }

    public void levelUp(Character character) {
        character.getStamina().gain(stamina);
        character.getArousal().gain(arousal);
        character.getWillpower().gain(willpower);

        character.availableAttributePoints += attributes[Math.min(character.rank, attributes.length-1)];

        if (Global.checkFlag(Flag.hardmode)) {
            character.getStamina().gain(bonusStamina);
            character.getArousal().gain(bonusArousal);
            character.getWillpower().gain(bonusWillpower);
            character.availableAttributePoints += bonusAttributes;
        }
        traits.keySet().stream().filter(i -> i <= character.level).forEach(i -> {
            traits.get(i).forEach(character::add);
        });
        actions.keySet().stream().filter(i -> i <= character.level).forEach(i -> {
            actions.get(i).run();
        });
    }
    
    @Override public String toString() {
        return "Growth with stamina "+stamina+" arousal "+arousal+" mojo "+mojo+" bonusStamina "+bonusStamina+" bonusArousal "+bonusArousal+" bonusMojo "+bonusMojo+" bonusAttributes "+bonusAttributes+" willpower "+willpower+" bonusWillpower "+bonusWillpower+" attributes "+attributes+" traits "+traits+" actions "+actions;
    }
}

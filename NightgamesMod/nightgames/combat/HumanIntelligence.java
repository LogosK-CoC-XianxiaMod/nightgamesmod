package nightgames.combat;

import nightgames.characters.Character;
import nightgames.characters.Player;
import nightgames.global.Global;
import nightgames.skills.Nothing;
import nightgames.skills.Skill;
import nightgames.skills.SkillGroup;
import nightgames.skills.Tactics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class HumanIntelligence implements Intelligence {
    private final Player character;

    public HumanIntelligence(Player character) {
        this.character = character;
    }

    @Override
    public boolean act(Combat c, Character target) {
        HashSet<Skill> available = new HashSet<>(character.getSkills());
        HashSet<Skill> unavailable = new HashSet<>();
        HashSet<Skill> cds = new HashSet<>();
        System.out.println("Generated usable skills: " + character.getSkills().stream().map(Skill::getName).collect(Collectors.joining()));
        Skill.filterAllowedSkills(c, available, character, target);
        for (Skill a : available) {
            if (Skill.isUsable(c, a)) {
                if (character.cooldownAvailable(a)) {

                } else {
                    cds.add(a);
                    unavailable.add(a);
                }
            } else { unavailable.add(a); }
        }
        HashMap<Tactics, HashSet<Skill>> skillMap = new HashMap<>();
        if (available.size() == 0) {
            available.add(new Nothing(character));
        }
        available.addAll(cds);
        available.removeAll(unavailable);
        available.forEach(skill -> {
            if (!skillMap.containsKey(skill.type(c))) {
                skillMap.put(skill.type(c), new HashSet<>());
            }
            skillMap.get(skill.type(c)).add(skill);
        });
        ArrayList<SkillGroup> skillGroups = new ArrayList<>();
        skillMap.forEach((tactic, skills) -> skillGroups.add(new SkillGroup(tactic, skills.stream()
                .map(skill -> skill.instantiate(c, target)).collect(Collectors.toSet()))));

        character.gui.chooseSkills(c, target, skillGroups);
        Global.getMatch().pause();
        return true;
    }
}

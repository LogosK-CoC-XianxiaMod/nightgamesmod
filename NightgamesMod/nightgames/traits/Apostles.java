package nightgames.traits;

import nightgames.characters.Character;
import nightgames.characters.*;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.pet.CharacterPet;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Apostles {
    private static final String APOSTLES_COUNT_FLAG = "APOSTLES_COUNT";
    private static final List<JtwigTemplate> SUMMON_APOSTLE_QUOTE = List.of(
            JtwigTemplate.inlineTemplate(
                    "The space around {{ self.nameDirectObject() }} starts abruptly shimmering. "
                            + "{{ opponent.subject() }} {{ opponent.action('look') }} up in alarm, but "
                            + "{{ self.subject() }} just chuckles. "
                            + "<i>\"{{ other.getName() }}, a Goddess should have followers don't you agree? Let's "
                            + "see how you fare in a ménage-à-trois, yes?\"</i>"),
            JtwigTemplate.inlineTemplate("A soft light starts growing around "
                    + "{{ self.nameDirectObject() }}, causing {{ other.objectPronoun() }} to pause. "
                    + "{{ self.subject() }} holds up her arms as if to welcome someone. "
                    + "<i>\"Sex with just two is just so <b>lonely</b> don't you think? Let's spice it up a bit!\"</i>"),
            JtwigTemplate.inlineTemplate("Suddenly, several pillars of light descend from the sky "
                    + "and converge in front of {{ self.nameDirectObject() }} in the form of a humanoid figure. "
                    + "Not knowing what's going on, {{ other.subject() }} cautiously "
                    + "{{ other.action('approach', 'approaches') }}. "
                    + "{{ self.subject() }} reaches into the light and holds the figure's hands. "
                    + "<i>\"See {{ other.getName() }}, I'm not a greedy {{ self.getGuyOrGirl }}. I can "
                    + "share with my friends.\"</i>"));

    public static void eachCombatRound(Combat combat, Character character, Character opponent) {
        if (character.canRespond()
            && combat.getCombatantData(character).getIntegerFlag(APOSTLES_COUNT_FLAG) >= 4) {
            List<BasePersonality> possibleApostles = Stream.of(new Mei(), new Caroline(), new Sarah())
                .filter(possible -> combat.getPetCombatants().stream()
                        .noneMatch(existing -> existing.getName().equals(possible.character.getName())))
                .collect(Collectors.toList());
            var targetApostle = Global.pickRandom(possibleApostles);
            if (targetApostle.isPresent()) {
                var quote = Global.pickRandom(SUMMON_APOSTLE_QUOTE);
                CharacterPet pet = new CharacterPet(character,
                        targetApostle.get().character,
                    character.getProgression().getLevel() - 5,
                    character.getProgression().getLevel() /4);
                var model = JtwigModel.newModel()
                    .with("self", character)
                    .with("opponent", opponent);
                combat.write(character, quote.orElseThrow().render(model));
                combat.addPet(character, pet.getSelf());
                combat.getCombatantData(character).setIntegerFlag(APOSTLES_COUNT_FLAG, 0);
            }
        }
        if (combat.assistantsOf(character).size() < character.getPetLimit()) {
            combat.getCombatantData(character).setIntegerFlag(APOSTLES_COUNT_FLAG, combat.getCombatantData(character).getIntegerFlag(APOSTLES_COUNT_FLAG) + 1);
        }
    }
}

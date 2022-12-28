package nightgames.trap;

import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Participant;
import nightgames.match.Status;
import nightgames.stance.Position;
import nightgames.status.Flatfooted;
import nightgames.status.Hypersensitive;
import nightgames.status.Oiled;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.Map;
import java.util.Optional;

// TODO: Text needs to be reworked to meet genitalia variations
public class TentacleTrap extends Trap {

    private static class Instance extends Trap.Instance {
        public Instance(Trap self, Participant owner) {
            super(self, owner);
        }

        private static final String VICTIM_TRIGGER_INEFFECTIVE_MESSAGE = "Holy hell! A dozen large tentacles shoot " +
                "out of the floor in front of you and thrash wildly. You freeze, hoping they won't notice you, but " +
                "it seems futile. the tentacles approach you from all sides, poking at you tentatively. As suddenly " +
                "as they appeared, the tentacles vanish back into the floor. \n...Is that it? You're safe... " +
                "you guess?";
        private static final String VICTIM_TRIGGER_MESSAGE = "An unearthly glow appears from the floor surrounding " +
                "you and at least a dozen tentacles burst from the floor. Before you can react, you're lifted " +
                "helpless into the air. The tentacles assault you front and back, wriggling around you nipples and " +
                "cock, while one persistent tentacle forces its way into your ass. The overwhelming sensations and " +
                "violation keep you from thinking clearly and you can't even begin to mount a reasonable resistance. " +
                "Just as suddenly as they attacked you, the tentacles are gone, dumping you unceremoniously to the " +
                "floor. You're left coated in a slimy liquid that, based on your rock-hard erection, seems to be a " +
                "powerful aphrodisiac. Holy fucking hell...";

        private static final JtwigTemplate OWNER_TRIGGER_INEFFECTIVE_TEMPLATE = JtwigTemplate.inlineTemplate(
                "{{ victim.subject().defaultNoun() stumbles into range of the fetish totem. A cage of phallic " +
                        "tentacles appear around her. They apparently aren't interested in her and they disappear, " +
                        "leaving her slightly bewildered.");

        private static final JtwigTemplate OWNER_TRIGGER_TEMPLATE = JtwigTemplate.inlineTemplate(
                "{{ victim.subject().defaultNoun() }} gets caught in the tentacle trap and penis-shaped tentacles " +
                        "immediately surround {{ victim.object().pronoun() }}. Before {{ victim.subject().pronoun() }} " +
                        "can escape, they bind {{ victim.possessiveAdjective() }} limbs and start probing and " +
                        "caressing {{ victim.possessiveAdjective() }} naked body. The tentacles start to ooze out " +
                        "lubricant and two tentacles penetrate {{ victim.possessiveAdjective() }} vaginally and anally. " +
                        "A third tentacle slips into {{ victim.possessiveAdjective() }} mouth, while the rest frot " +
                        "against {{ victim.possessiveAdjective() }} body. They gang-bang " +
                        "{{ victim.object().defaultNoun() }} briefly, but thoroughly, before squirting liquid over " +
                        "{{ victim.object().pronoun() }} and disappearing back into the floor. " +
                        "{{ victim.subject().pronoun() }}'s left shivering, sticky, and unsatisfied. In effect, " +
                        "{{ victim.subject().pronoun() }}'s already defeated.");
        @Override
        public void trigger(Participant target) {
            if (target.getCharacter().mostlyNude()) {
                if (target.getCharacter().human()) {
                    Global.gui().message(VICTIM_TRIGGER_MESSAGE);
                } else if (target.getLocation().humanPresent()) {
                    var model = JtwigModel.newModel()
                            .with("victim", target.getCharacter().getGrammar());
                    Global.gui().message(OWNER_TRIGGER_TEMPLATE.render(model));
                }
                target.getCharacter().tempt(target.getCharacter().arousal.max());
                target.getCharacter().addNonCombat(new Status(new Oiled(target.getCharacter())));
                target.getCharacter().addNonCombat(new Status(new Hypersensitive(target.getCharacter())));
                target.getLocation().opportunity(target.getCharacter(), this);
            } else {
                if (target.getCharacter().human()) {
                    Global.gui().message(VICTIM_TRIGGER_INEFFECTIVE_MESSAGE);
                } else if (target.getLocation().humanPresent()) {
                    var model = JtwigModel.newModel()
                            .with("victim", target.getCharacter().getGrammar());
                    Global.gui().message(OWNER_TRIGGER_INEFFECTIVE_TEMPLATE.render(model));
                }
            }
        }

        @Override
        public Optional<Position> capitalize(Participant attacker, Participant victim) {
            victim.getCharacter().addNonCombat(new Status(new Flatfooted(victim.getCharacter(), 1)));
            attacker.getLocation().clearTrap();
            return super.capitalize(attacker, victim);
        }
    }

    public TentacleTrap() {
        super("Tentacle Trap");
    }

    private static final Map<Item, Integer> REQUIRED_ITEMS = Map.of(Item.Totem, 1);

    protected Map<Item, Integer> requiredItems() {
        return REQUIRED_ITEMS;
    }

    @Override
    public boolean requirements(Participant user) {
        return user.getCharacter().getProgression().getRank() > 0;
    }

    @Override
    public InstantiateResult instantiate(Participant owner) {
        deductCostsFrom(owner);
        return new InstantiateResult(CREATION_MESSAGE, new Instance(this, owner));
    }

    private static final String CREATION_MESSAGE = "You need to activate this phallic totem before it can be used " +
            "as a trap. You stroke the small totem with your hand, which is... weird, but effective. You " +
            "quickly place the totem someplace out of sight and hurriedly get out of range. You're not sure " +
            "whether this will actually discriminate before attacking.";
}

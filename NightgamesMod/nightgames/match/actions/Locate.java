package nightgames.match.actions;

import nightgames.areas.Area;
import nightgames.characters.Trait;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Action;
import nightgames.match.Participant;
import nightgames.match.Status;
import nightgames.status.Detected;
import nightgames.status.Horny;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.Optional;
import java.util.stream.Collectors;

public class Locate extends Action {
    private static final int MINIMUM_SCRYING_REQUIREMENT = 5;

    public final class Instance extends Action.Instance {

        private Instance(Participant user, Area location) {
            super(user, location);
        }

        @Override
        public void execute() {
            var dialog = new Dialog(user);
            dialog.start();
            messageOthersInLocation(user.getCharacter().getGrammar().subject().defaultNoun() +
                    String.format(" is holding someone's underwear in %s hands and breathing deeply. Strange.",
                            user.getCharacter().possessiveAdjective()));
        }
    }

    private static final class Dialog {
        private Participant scryer;

        private Dialog(Participant scryer) {
            this.scryer = scryer;
        }

        private void start() {
            var msg = "Thinking back to your 'games' with Reyka, you take out a totem to begin a scrying ritual: ";
            scryer.getCharacter().chooseLocateTarget(
                    Global.getMatch().getParticipants().stream()
                            .filter(p -> scryer.getCharacter().getAffection(p.getCharacter()) >= MINIMUM_SCRYING_REQUIREMENT)
                            .collect(Collectors.toMap(Participant::getCharacter, p -> () -> this.chooseTarget(p))),
                    this::end,
                    msg);
        }

        private static final JtwigTemplate COMPLETION_TEMPLATE = JtwigTemplate.inlineTemplate(
                "Drawing on the dark energies inside the talisman, you attempt to scry for " +
                        "{{ target.object().properNoun() }}'s location. " +
                        "{% if (area.isPresent()) %} " +
                        "In your mind, an image of the <b><i>{{ area.name }}</i></b> appears. It falls apart as " +
                        "quickly as it came to be, but you know where {{ target.subject().pronoun() }} currently is. " +
                        "{% else %}" +
                        "However, you draw a blank." +
                        "{% endif %} " +
                        "Your small talisman is already burning up in those creepy purple flames, the smoke flowing " +
                        "from your nose straight to your crotch and setting another fire there.");

        private void chooseTarget(Participant target) {
            var gui = Global.gui();
            var area = Optional.ofNullable(target.getLocation());
            area.ifPresent(a -> target.getCharacter().addNonCombat(new Status(new Detected(target.getCharacter(), 10))));
            gui.clearText();
            var model = JtwigModel.newModel()
                    .with("target", target.getCharacter().getGrammar())
                    .with("area", area);
            gui.message(COMPLETION_TEMPLATE.render(model));
            scryer.getCharacter().addNonCombat(new Status(new Horny(scryer.getCharacter(),
                    scryer.getCharacter().arousal.max() / 10.0f,
                    10,
                    "Scrying Ritual")));
            scryer.getCharacter().leaveAction(this::end);
        }

        private void end() {
            Global.gui().clearText();
            Global.getMatch().resume();
        }
    }

    public Locate() {
        super("Locate");
    }

    @Override
    public boolean usable(Participant self) {
        boolean hasUnderwear = false;
        for (Item i : self.getCharacter().getInventory().keySet()) {
            // i hate myself for having to add this null check... why is inventory even public...
            if (i != null && i.toString().contains("Trophy")) {
                hasUnderwear = true;
            }
        }
        return self.getCharacter().has(Trait.locator) && hasUnderwear && !self.getCharacter().bound();
    }

    @Override
    public Instance newInstance(Participant user, Area location) {
        return new Instance(user, location);
    }

}

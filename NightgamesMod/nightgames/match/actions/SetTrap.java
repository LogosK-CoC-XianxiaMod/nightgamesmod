package nightgames.match.actions;

import nightgames.areas.Area;
import nightgames.match.Action;
import nightgames.match.Participant;
import nightgames.trap.Trap;

public class SetTrap extends Action {

    public final class Instance extends Action.Instance {

        private Instance(Participant user, Area location) {
            super(user, location);
        }

        @Override
        public void execute() {
            var result = trap.instantiate(user);
            user.getLocation().setTrap(result.instance);
            user.getCharacter().message(result.message);
            messageOthersInLocation(user.getCharacter().getGrammar().subject().defaultNoun() +
                    " start rigging up something weird, probably a trap.");
        }
    }

    private Trap trap;

    public SetTrap(Trap trap) {
        super("Set(" + trap.toString() + ")");
        this.trap = trap;
    }

    @Override
    public boolean usable(Participant user) {
        return trap.recipe(user) && !user.getLocation().open()
                && trap.requirements(user)
                && !user.getCharacter().bound()
                && user.getLocation().getTrap() == null;
    }

    @Override
    public Instance newInstance(Participant user, Area location) {
        return new Instance(user, location);
    }

}

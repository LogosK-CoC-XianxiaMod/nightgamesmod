package nightgames.match.actions;

import nightgames.areas.Area;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.match.Action;
import nightgames.match.Participant;
import nightgames.status.Bound;
import nightgames.status.Stsflag;

public class Struggle extends Action {

    private static final class Aftermath extends Action.Aftermath {
        private Aftermath(Participant usedAction) {
            super(usedAction);
        }

        @Override
        public String describe(Character c) {
            return String.format(" is struggling against %s bondage.", c.possessiveAdjective());
        }
    }

    public final class Instance extends Action.Instance {

        private Instance(Participant user, Area location) {
            super(user, location);
        }

        @Override
        public Action.Aftermath execute() {
            Bound status = (Bound) user.getCharacter().getStatus(Stsflag.bound);
            int difficulty = 20 - user.getCharacter().getEscape(null, null);
            if (user.getCharacter().check(Attribute.Power, difficulty)) {
                if (status != null) {
                    user.getCharacter().message("You manage to break free from the " + status.getVariant() + ".");
                } else {
                    user.getCharacter().message("You manage to snap the restraints that are binding your hands.");
                }
                user.getCharacter().free();
            } else {
                if (status != null) {
                    user.getCharacter().message("You struggle against the " + status.getVariant() + ", but can't get free.");
                } else {
                    user.getCharacter().message("You struggle against your restraints, but can't get free.");
                }
                user.getCharacter().struggle();
            }
            return new Aftermath(user);
        }
    }

    public Struggle() {
        super("Struggle");
    }

    @Override
    public boolean usable(Participant user) {
        return user.getCharacter().bound();
    }

    @Override
    public Instance newInstance(Participant user, Area location) {
        return new Instance(user, location);
    }

}

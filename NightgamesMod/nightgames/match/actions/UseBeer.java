package nightgames.match.actions;

import nightgames.characters.Character;
import nightgames.items.Item;
import nightgames.match.Action;
import nightgames.match.Participant;
import nightgames.match.Status;
import nightgames.status.Buzzed;

public class UseBeer extends Action {

    private static final class Aftermath extends Action.Aftermath {
        private Aftermath() {}

        @Override
        public String describe(Character c) {
            return " opening a beer and downing the whole thing.";
        }
    }

    public static final class Instance extends Action.Instance {
        private Instance(Action self, Participant user) {
            super(self, user);
        }

        @Override
        public Action.Aftermath execute() {
            return self.execute(user);
        }
    }

    public UseBeer() {
        super( "Beer");
    }

    @Override
    public boolean usable(Participant user) {
        return !user.getCharacter().bound();
    }

    @Override
    public Instance newInstance(Participant user) {
        return new Instance(this, user);
    }

    @Override
    public Action.Aftermath execute(Participant user) {
        user.getCharacter().message("You pop open a beer and chug it down, feeling buzzed and a bit slugish.");
        user.getCharacter().addNonCombat(new Status(new Buzzed(user.getCharacter())));
        user.getCharacter().consume(Item.Beer, 1);
        return new Aftermath();
    }

}
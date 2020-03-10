package nightgames.actions;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.State;
import nightgames.match.Participant;

public class TreeAmbush extends Action {
    private static final long serialVersionUID = -8503564080765172483L;

    private static class Aftermath extends Action.Aftermath {
        private Aftermath() {}

        @Override
        public String describe(Character c) {
            return " climb up a tree.";
        }
    }

    public TreeAmbush() {
        super("Climb a Tree");
    }

    @Override
    public boolean usable(Participant user) {
        return (user.getCharacter().get(Attribute.Power) >= 20 || user.getCharacter().get(Attribute.Animism) >= 10)
                        && user.state != State.inTree
                        && !user.getCharacter().bound();
    }

    @Override
    public Action.Aftermath execute(Participant user) {
        if (user.getCharacter().get(Attribute.Animism) >= 10) {
            user.getCharacter().message("Following your instincts, you clamber up a tree" + " to await an unwitting passerby.");
        } else {
            user.getCharacter().message("You climb up a tree that has a branch hanging over"
                    + " the trail. It's hidden in the leaves, so you should be"
                    + " able to surprise someone passing underneath.");
        }
        user.state = State.inTree;
        return new Aftermath();
    }

}

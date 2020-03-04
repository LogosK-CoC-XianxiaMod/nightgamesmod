package nightgames.actions;

import nightgames.characters.State;
import nightgames.global.Global;
import nightgames.match.Participant;

public class Bathe extends Action {

    /**
     * 
     */
    private static final long serialVersionUID = 4565550545479306251L;
    private final String message;

    public Bathe(String message) {
        super("Clean Up");
        this.message = message;
    }

    @Override
    public boolean usable(Participant user) {
        return !user.getCharacter().bound();
    }

    @Override
    public IMovement execute(Participant user) {
        if (user.getCharacter().human()) {
            if (message != null) {
                Global.gui().message(message);
            }
        }
        user.getCharacter().state = State.shower;
        user.waitRounds(1);
        return Movement.bathe;
    }

    public static final String SHOWER_MESSAGE = "It's a bit dangerous, but a shower sounds especially inviting right now.";
    public static final String POOL_MESSAGE = "There's a jacuzzi in the pool area and you decide to risk a quick soak.";

}

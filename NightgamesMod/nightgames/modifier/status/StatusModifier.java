package nightgames.modifier.status;

import nightgames.characters.Character;
import nightgames.modifier.ModifierCategory;
import nightgames.status.Status;

public class StatusModifier implements ModifierCategory<StatusModifier> {

    private final Status status;
    private final boolean playerOnly;

    public StatusModifier(Status status, boolean playerOnly) {
        this.status = status;
        this.playerOnly = playerOnly;
    }

    protected StatusModifier() {
        status = null;
        playerOnly = true;
    }

    public void apply(Character c) {
        if ((!playerOnly || c.human()) && status != null) {
            c.addNonCombat(new nightgames.match.Status(status.instance(c, null)));
        }
    }

    public StatusModifier combine(StatusModifier next) {
        StatusModifier first = this;
        return new StatusModifier() {
            @Override
            public void apply(Character c) {
                first.apply(c);
                next.apply(c);
            }

            @Override public String toString() {
                return first.toString() + " and " + next.toString();
            }
        };
    }

    @Override
    public String toString() {
        return status.name;
    }
}

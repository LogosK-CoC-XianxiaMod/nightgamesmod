package nightgames.status;

import nightgames.characters.Character;
import nightgames.characters.NPC;
import nightgames.characters.Trait;
import nightgames.combat.Combat;
import nightgames.requirements.DurationRequirement;
import nightgames.requirements.Requirement;

import java.util.ArrayList;

public abstract class DurationStatus extends Status {
    private DurationRequirement req;

    public DurationStatus(String name, Character affected, int duration) {
        super(name, affected);
        if (affected.has(Trait.PersonalInertia)) {
            duration = Math.round(1.33f * duration);
        }
        req = new DurationRequirement(duration);

        ((ArrayList<Requirement>)requirements).add((Requirement)req);
    }

    public static NPC dummyNPCForInit = new NPC("dummy", -1, null);

    public int getDuration() {
        return req.remaining();
    }

    public void setDuration(int duration) {
        req.reset(duration);
    }

    @Override
    public int regen(Combat c) {
        int i = 1;
        if (affected.has(Trait.QuickRecovery) && flags.contains(Stsflag.disabling)) {
            i *= 2;
        }
        req.tick(i);
        return 0;
    }

    public void tick(int i) {
        if (affected.has(Trait.QuickRecovery) && flags.contains(Stsflag.disabling)) {
            i *= 2;
        }
        req.tick(i);
    }
}

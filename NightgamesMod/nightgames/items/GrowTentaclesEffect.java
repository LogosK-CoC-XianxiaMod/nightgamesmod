package nightgames.items;

import nightgames.characters.Character;
import nightgames.characters.body.TentaclePart;
import nightgames.combat.Combat;

public class GrowTentaclesEffect extends ItemEffect {
    int selfDuration;

    public GrowTentaclesEffect(String verb, String otherverb) {
        this(verb, otherverb, -1);
    }

    public GrowTentaclesEffect(String verb, String otherverb, int duration) {
        super(verb, otherverb, true, true);
        selfDuration = duration;
    }

    @Override
    public boolean use(Combat c, Character user, Character opponent, Item item) {
        int duration = selfDuration >= 0 ? selfDuration : item.duration;
        TentaclePart part = TentaclePart.randomTentacle("tentacles", user.body, "tentacle-semen");
        BodyModEffect effect = new BodyGrowthMultipleEffect(getSelfVerb(), getOtherVerb(), part, duration);
        effect.use(null, user, opponent, item);
        c.write(part.describe(user) + " sprout from " + user.nameOrPossessivePronoun() + " " + part.attachpoint);
        return true;
    }
}

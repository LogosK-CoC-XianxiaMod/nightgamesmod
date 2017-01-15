package nightgames.characters.body;

import nightgames.characters.Character;
import nightgames.characters.body.mods.PartMod;
import nightgames.global.Global;

public interface CockPart extends BodyPart {
    double getSize();

    BodyPart applyMod(PartMod mod);

    @Override
    public default double getFemininity(Character self) {
        return -3;
    }

    default PussyPart getEquivalentPussy() {
        for (PussyPart pussy : PussyPart.values()) {
            CockMod equivalentMod = pussy.getEquivalentCockMod();
            if (equivalentMod != CockMod.error && moddedPartCountsAs(Global.noneCharacter(), equivalentMod)) {
                return pussy;
            }
        }
        return PussyPart.normal;
    }

    default String adjective(Character c) {
        return "phallic";
    }

    BodyPart removeAllMods();
}

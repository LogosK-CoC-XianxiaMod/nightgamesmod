package nightgames.traits;

import nightgames.characters.Character;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.stance.Neutral;

public final class HiveMind {
    private HiveMind() {
    }

    // Returns true if this should end the orgasm
    public static boolean resolveOrgasm(Combat c, Character bearer, Character opponent) {
        if (!c.getPetsFor(bearer).isEmpty()) {
            // don't use opponent, use opponent of the current combat
            c.write(bearer, Global.format("Just as {self:subject-action:seem} about to "
                            + "orgasm, {self:possessive} expression shifts. {self:POSSESSIVE} eyes dulls "
                            + "and {self:possessive} expressions slacken."
                            + "{other:if-human: Shit you've seen this before, she somehow switched "
                            + "bodies with one of her clones!}",
                    bearer, c.getOpponentCharacter(bearer)));
            c.getPetsFor(bearer).forEach(assistant -> {
                if (bearer.checkOrgasm()) {
                    int amount = Math.min(bearer.getArousal().get(), assistant.getArousal().max());
                    bearer.getArousal().calm(amount);
                    assistant.arouse(amount, c, Global.format("({self:master}'s orgasm)", bearer, opponent));
                    assistant.doOrgasm(c, assistant, null, null);
                }
            });
            c.setStance(new Neutral(bearer, c.getOpponentCharacter(bearer)));
            if (!bearer.checkOrgasm()) {
                return true;
            } else {
                c.write(bearer,
                        Global.format("{other:if-human:Luckily }{self:pronoun} didn't seem to "
                                        + "be able to shunt all {self:possessive} arousal nto {self:possessive} "
                                        + "clones, and rapidly reaches the peak anyways.",
                                bearer, c.getOpponentCharacter(bearer)));
            }
        }
        return false;
    }
}

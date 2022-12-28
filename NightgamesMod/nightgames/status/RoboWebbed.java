package nightgames.status;

import com.google.gson.JsonObject;
import nightgames.characters.Character;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.trap.Trap;

import java.util.Optional;

public class RoboWebbed extends Bound {
    public RoboWebbed(Character affected, double dc, Trap.Instance roboWeb) {
        super("RoboWebbed", affected, dc, "robo-web", roboWeb);
    }

    @Override
    public String initialMessage(Combat c, Status replacement) {
        return "";
    }

    @Override
    public String describe(Character opponent) {
        return Global.format("{self:SUBJECT-ACTION:are|is} hopelessly tangled up in"
                        + " synthetic webbing, which is sending pleasurable sensations"
                        + " through {self:possessive} entire body.", affected, Global.noneCharacter());
    }

    @Override
    public void tick(Combat c) {
        // Message handled in describe
        affected.temptNoSkillNoTempter(c, arousalInflicted());
    }

    @Override
    public void afterMatchRound() {
        super.afterMatchRound();
        if (affected.human()) {
            Global.gui().message(Global.format("{self:SUBJECT-ACTION:are|is} hopelessly tangled up in"
                    + " synthetic webbing, which is sending pleasurable sensations"
                    + " through {self:possessive} entire body.", affected, Global.noneCharacter()));
        }
        affected.tempt(arousalInflicted());
        affected.location().opportunity(affected, trap.orElseThrow());
    }

    private int arousalInflicted() {
        return (int) (affected.arousal.max() * .25);
    }

    @Override
    public Status instance(Character newAffected, Character newOther) {
        return new RoboWebbed(newAffected, toughness, trap.orElse(null));
    }

    @Override
    public JsonObject saveToJson() {
        return null;
    }

    @Override
    public Status loadFromJson(JsonObject obj) {
        return null;
    }
}

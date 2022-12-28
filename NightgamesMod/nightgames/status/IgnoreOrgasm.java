package nightgames.status;

import com.google.gson.JsonObject;
import java.util.Optional;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.combat.Combat;

public class IgnoreOrgasm extends DurationStatus {
    public IgnoreOrgasm(Character affected, int duration) {
        super("Orgasm Ignored", affected, duration);
        flag(Stsflag.orgasmseal);
    }

    @Override
    public String initialMessage(Combat c, Status replacement) {
        if (affected.arousal.isAtUnfavorableExtreme()) {
            return affected.subjectAction("are", "is") + " overpowering the urge to cum";
        }
        return "";
    }

    @Override
    public String describe(Character opponent) {
        if (affected.arousal.isAtUnfavorableExtreme()) {
            return affected.subjectAction("are", "is") + " overpowering the urge to cum";
        }
        return "";
    }

    @Override
    public float fitnessModifier() {
        return 0;
    }

    @Override
    public int mod(Attribute a) {
        return 0;
    }

    @Override
    public int damage(Combat c, int x) {
        return 0;
    }

    @Override
    public double pleasure(Combat c, BodyPart withPart, BodyPart targetPart, double x) {
        if (affected.arousal.isAtUnfavorableExtreme()) {
            return -x * 9 / 10;
        }
        return 0;
    }

    @Override
    public int weakened(Combat c, int x) {
        return 0;
    }

    @Override
    public int tempted(Combat c, int x) {
        return 0;
    }

    @Override
    public int evade() {
        return 0;
    }

    @Override
    public int escape() {
        return 0;
    }

    @Override
    public int gainmojo(int x) {
        return 0;
    }

    @Override
    public int spendmojo(int x) {
        return 0;
    }

    @Override
    public int counter() {
        return 0;
    }

    @Override
    public String toString() {
        return "Orgasm Ignored";
    }

    @Override
    public int value() {
        return 0;
    }

    @Override
    public Status instance(Character newAffected, Character newOther) {
        return new IgnoreOrgasm(newAffected, getDuration());
    }

    @Override  public JsonObject saveToJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", getClass().getSimpleName());
        obj.addProperty("duration", getDuration());
        return obj;
    }

    @Override public Status loadFromJson(JsonObject obj) {
        return new IgnoreOrgasm(null, obj.get("duration").getAsInt());
    }
}

package nightgames.status

import com.google.gson.JsonObject
import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.characters.body.BodyPart
import nightgames.characters.body.CockPart
import nightgames.characters.body.PussyPart
import nightgames.combat.Combat
import nightgames.status.addiction.Addiction
import nightgames.status.addiction.AddictionType
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate

class DivineCharge(affected: Character?, magnitude: Double) : Status("Divine Energy", affected!!) {
    @JvmField
    var magnitude: Double
    private fun getPart(c: Combat?): String {
        val penetrated = c!!.getStance().vaginallyPenetrated(c, affected)
        val inserted = c.getStance().inserted(affected)
        var part = "body"
        if (penetrated && !inserted) {
            part = PussyPart.TYPE
        }
        if (!penetrated && inserted) {
            part = CockPart.TYPE
        }
        if (!penetrated && !inserted && affected.has(Trait.zealinspiring)) {
            part = PussyPart.TYPE
        }
        return part
    }

    override fun tick(c: Combat) {
        val opponent = c.getOpponentCharacter(affected)
        if (!c.getStance().havingSex(c, affected) && !(affected.has(Trait.zealinspiring)
                        && opponent.getAddiction(AddictionType.ZEAL)?.isInWithdrawal != true)) {
            magnitude = magnitude / 2
            c.write(affected, "The holy energy seeps out of " + affected.nameDirectObject() + ".")
            if (magnitude < .05f) affected.removelist.add(this)
        }
    }

    override fun initialMessage(c: Combat?, replacement: Status?): String {
        return if (replacement == null) {
            String.format("%s concentrating divine energy in %s %s.\n", affected.subjectAction("are", "is"),
                    affected.possessiveAdjective(), getPart(c))
        } else ""
    }

    override fun onApply(c: Combat?, other: Character?) {
        affected.usedAttribute(Attribute.Divinity, c, .25)
    }

    init {
        flag(Stsflag.divinecharge)
        flag(Stsflag.purgable)
        this.magnitude = magnitude
    }

    override fun describe(opponent: Character): String? {
        val model = JtwigModel()
                .with("self", affected)
                .with("magnitude", magnitude)
        return DESCRIBE_TEMPLATE.render(model)
    }

    override fun fitnessModifier(): Float {
        return (3 * magnitude).toFloat()
    }

    override fun mod(a: Attribute): Int {
        return 0
    }

    override fun overrides(s: Status?): Boolean {
        return false
    }

    override fun replace(s: Status?) {
        assert(s is DivineCharge)
        val other = s as DivineCharge?
        magnitude = magnitude + other!!.magnitude
        // every 10 divinity past 10, you are allowed to add another stack of divine charge.
        // this will get out of hand super quick, but eh, you shouldn't let it get that far.
        val maximum = Math.max(2.0, Math.pow(2.0, affected[Attribute.Divinity] / 5.0) * .25)
        magnitude = Math.min(maximum, magnitude)
    }

    override fun damage(c: Combat, x: Int): Int {
        return 0
    }

    override fun pleasure(c: Combat, withPart: BodyPart, targetPart: BodyPart, x: Double): Double {
        return 0.0
    }

    override fun weakened(c: Combat?, x: Int): Int {
        return 0
    }

    override fun tempted(c: Combat, x: Int): Int {
        return 0
    }

    override fun evade(): Int {
        return 0
    }

    override fun escape(): Int {
        return 0
    }

    override fun gainmojo(x: Int): Int {
        return 0
    }

    override fun spendmojo(x: Int): Int {
        return 0
    }

    override fun counter(): Int {
        return 0
    }

    override fun value(): Int {
        return 0
    }

    override fun instance(newAffected: Character, newOther: Character?): Status {
        return DivineCharge(newAffected, magnitude)
    }

    override fun saveToJson(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("type", javaClass.simpleName)
        obj.addProperty("magnitude", magnitude)
        return obj
    }

    override fun loadFromJson(obj: JsonObject): Status? {
        return DivineCharge(null, obj["magnitude"].asFloat.toDouble())
    }

    override fun regen(c: Combat?): Int {
        return 0
    }

    companion object {
        private val DESCRIBE_TEMPLATE = JtwigTemplate.inlineTemplate("A faint white glow emanates from {{ self.nameOrDirectObject() }} as divine energy courses "
                + "through {{ self.possessivePronoun() }} body. ({{ magnitude }})")
    }
}
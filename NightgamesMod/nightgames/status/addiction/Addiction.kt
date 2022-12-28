package nightgames.status.addiction

import com.google.gson.JsonObject
import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.combat.Combat
import nightgames.global.Global
import nightgames.status.Status
import nightgames.status.Stsflag

abstract class Addiction protected constructor(affected: Character?, name: String?, cause: Character, magnitude: Float = .01f) : Status(name!!, affected!!) {
    @JvmField
    @Transient
    val cause: Character
    @JvmField var magnitude: Float
    fun getMagnitude() = magnitude
    @JvmField
    protected var combatMagnitude: Float

    //TODO: Suggestion: Since all of the current addictions in the game are given by a character having a trait - Addictions should have a governing Trait and Addictions should manage their own effects. - DSM 
    var governingTrait: Trait? = null

    // should be saved
    private var didDaytime: Boolean
    private var overloading: Boolean
    @JvmField internal var isInWithdrawal: Boolean

    init {
        flag(Stsflag.permanent)
        this.name = name!!
        this.cause = cause
        this.magnitude = magnitude
        combatMagnitude = .01f
        didDaytime = false
        isInWithdrawal = false
        overloading = false
    }

    override fun tick(c: Combat) {
        if (c.getOpponentCharacter(affected) == cause) {
            combatMagnitude += (magnitude / 14.0).toFloat()
        }
    }

    fun clearDaytime() {
        didDaytime = false
    }

    fun flagDaytime() {
        didDaytime = true
    }

    val severity: Severity
        get() = if (magnitude < LOW_THRESHOLD) {
            Severity.NONE
        } else if (magnitude < MED_THRESHOLD) {
            Severity.LOW
        } else if (magnitude < HIGH_THRESHOLD) {
            Severity.MED
        } else {
            Severity.HIGH
        }
    val combatSeverity: Severity
        get() {
            if (combatMagnitude > magnitude) {
                // Effectively cap combatMagnitude to the current regular magnitude
                return severity
            }
            return if (combatMagnitude < LOW_THRESHOLD) {
                Severity.NONE
            } else if (combatMagnitude < MED_THRESHOLD) {
                Severity.LOW
            } else if (combatMagnitude < HIGH_THRESHOLD) {
                Severity.MED
            } else {
                Severity.HIGH
            }
        }

    fun atLeast(threshold: Severity): Boolean {
        return severity.ordinal >= threshold.ordinal
    }

    fun combatAtLeast(threshold: Severity): Boolean {
        return combatSeverity.ordinal >= threshold.ordinal
    }

    override fun saveToJson(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("type", type.name)
        obj.addProperty("cause", cause.type)
        obj.addProperty("magnitude", magnitude)
        obj.addProperty("combat", combatMagnitude)
        obj.addProperty("overloading", overloading)
        obj.addProperty("reenforced", didDaytime)
        return obj
    }

    protected abstract fun withdrawalEffects(): Status?
    protected abstract fun addictionEffects(): Status?

    //TODO:Added these for future revamp of Addiction system, where addictions manage their own effects.
    protected abstract fun applyEffects(self: Character?)
    protected abstract fun removeEffects(self: Character?)
    protected abstract fun cleanseAddiction(self: Character?)
    protected abstract fun describeIncrease(): String?
    protected abstract fun describeDecrease(): String?
    protected abstract fun describeWithdrawal(): String?
    protected abstract fun describeCombatIncrease(): String?
    protected abstract fun describeCombatDecrease(): String?
    abstract fun informantsOverview(): String?
    abstract fun describeMorning(): String?
    abstract val type: AddictionType
    fun overload() {
        magnitude = 1.0f
        overloading = true
    }

    open fun startNight(): Status? {
        if (!didDaytime || overloading) {
            if (!overloading) {
                val amount = Global.randomfloat() / 4f
                alleviate(null, amount)
            }
            if (isActive) {
                isInWithdrawal = true
                if (affected.human()) {
                    Global.gui().message(describeWithdrawal())
                }
                return withdrawalEffects()
            }
        }
        return null
    }

    fun refreshWithdrawal() {
        if (isInWithdrawal) {
            val opt = withdrawalEffects()
            if (opt != null && !affected.has(opt)) affected.addNonCombat(nightgames.match.Status(opt.instance(affected, cause)))
        }
    }

    open fun endNight() {
        isInWithdrawal = false
        clearDaytime()
        if (overloading) {
            magnitude = 0f
            overloading = false
            Global.gui()
                    .message("""
    <b>The overload treatment seems to have worked, and you are now rid of all traces of your $name.
    </b>
    """.trimIndent())
            affected.removeStatusImmediately(this)
        }
    }

    open fun startCombat(c: Combat?, opp: Character): Status? {
        combatMagnitude = if (atLeast(Severity.MED)) .2f else .0f
        if (opp.equals(cause) && atLeast(Severity.LOW)) {
            flags.forEach { flag: Stsflag? -> affected.flagStatus(flag!!) }
            return addictionEffects()
        }
        return null
    }

    fun endCombat(c: Combat?, opp: Character?) {
        flags.forEach { flag: Stsflag? -> affected.unflagStatus(flag!!) }
    }

    val isActive: Boolean
        get() = atLeast(Severity.LOW)

    fun shouldRemove(): Boolean {
        return magnitude <= 0.001f
    }

    fun aggravate(c: Combat?, amt: Float) {
        val old = severity
        magnitude = clamp(magnitude + amt)
        if (severity != old) {
            Global.writeIfCombat(c, cause, Global.format(describeIncrease(), affected, cause))
        }
    }

    fun alleviate(c: Combat?, amt: Float) {
        val old = severity
        magnitude = clamp(magnitude - amt)
        if (severity != old) {
            Global.writeIfCombat(c, cause, Global.format(describeDecrease(), affected, cause))
        }
    }

    fun aggravateCombat(c: Combat?, amt: Float) {
        val old = combatSeverity
        combatMagnitude = clamp(combatMagnitude + amt)
        if (severity != old) {
            Global.writeIfCombat(c, cause, Global.format(describeCombatIncrease(), affected, cause))
        }
    }

    fun alleviateCombat(c: Combat?, amt: Float) {
        val old = combatSeverity
        combatMagnitude = clamp(combatMagnitude - amt)
        if (severity != old) {
            Global.writeIfCombat(c, cause, Global.format(describeCombatDecrease(), affected, cause))
        }
    }

    private fun clamp(amt: Float): Float {
        if (amt < 0f) return 0f
        return if (amt > 1f) 1f else amt
    }

    override val isAddiction: Boolean
        get() = true

    enum class Severity {
        NONE, LOW, MED, HIGH
    }

    fun describeInitial() {
        Global.gui().message(describeIncrease())
    }

    fun wasCausedBy(target: Character?): Boolean {
        return target != null && target.type == cause.type
    }

    companion object {
        const val LOW_INCREASE = .03f
        const val MED_INCREASE = .08f
        const val HIGH_INCREASE = .15f
        const val LOW_THRESHOLD = .15f
        const val MED_THRESHOLD = .4f
        const val HIGH_THRESHOLD = .7f
        fun load(self: Character?, `object`: JsonObject): Addiction? {
            val cause = Global.getNPCByType(`object`["cause"].asString)
                    ?: return null
            val type = AddictionType.valueOf(`object`["type"].asString)
            val mag = `object`["magnitude"].asFloat
            val combat = `object`["combat"].asFloat
            val overloading = `object`["overloading"].asBoolean
            val reenforced = `object`["reenforced"].asBoolean
            val a = type.build(self, cause, mag)
            a.magnitude = mag
            a.combatMagnitude = combat
            a.overloading = overloading
            a.didDaytime = reenforced
            return a
        }
    }
}
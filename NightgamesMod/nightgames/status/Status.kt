package nightgames.status

import com.google.gson.JsonObject
import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.characters.body.BodyPart
import nightgames.combat.Combat
import nightgames.match.Action
import nightgames.match.Participant
import nightgames.requirements.DurationRequirement
import nightgames.requirements.Requirement
import nightgames.skills.Skill
import java.util.*
import java.util.function.Predicate

abstract class Status(@JvmField var name: String, @JvmField @field:Transient var daffected: Character?) : Cloneable {
    @JvmField public val affected: Character
    public fun getAffected() = affected
    init {
        if (daffected == null) {
            throw RuntimeException("asdf")
        }
        affected = daffected as Character
    }

    @JvmField
    @Transient
    protected var flags: MutableSet<Stsflag> = EnumSet.noneOf(Stsflag::class.java)

    @JvmField
    @Transient
    protected var requirements: List<Requirement> = mutableListOf(DurationRequirement(5))

    override fun toString(): String {
        return name
    }

    /** The set of skills that this status effect restricts Characters to using. The union of
     * these sets is taken if a Character is afflicted by multiple Statuses using this feature,
     * so it's still possible that the Character will use a skill outside of this set.
     */
    open fun skillWhitelist(c: Combat): Collection<Skill?>? {
        return emptySet<Skill>()
    }

    /** The set of skills that this Status prevents a Character from using. This takes
     * precedence over the whitelist. The sets of blackslists from all Statuses are also
     * unioned.
     */
    fun skillBlacklist(c: Combat): Set<Skill> {
        return emptySet()
    }

    fun meetsRequirements(c: Combat?, self: Character?, other: Character?): Boolean {
        return requirements.stream().allMatch { req: Requirement -> req.meets(c, self, other) }
    }

    abstract fun initialMessage(c: Combat?, replacement: Status?): String?
    abstract fun describe(opponent: Character): String?
    abstract fun mod(a: Attribute): Int
    abstract fun regen(c: Combat?): Int

    // Increases damage received by the returned value
    abstract fun damage(c: Combat, x: Int): Int

    // Increases pleasure received by the returned value
    abstract fun pleasure(c: Combat, withPart: BodyPart, targetPart: BodyPart, x: Double): Double
    abstract fun weakened(c: Combat?, x: Int): Int
    abstract fun tempted(c: Combat, x: Int): Int
    open fun sensitivity(x: Double, skill: Skill?): Double {
        return 0.0
    }

    fun opponentSensitivity(x: Double, skill: Skill?): Double {
        return 0.0
    }

    abstract fun evade(): Int
    abstract fun escape(): Int
    abstract fun gainmojo(x: Int): Int
    abstract fun spendmojo(x: Int): Int
    abstract fun counter(): Int
    abstract fun value(): Int
    open fun drained(c: Combat?, x: Int): Int {
        return 0
    }

    open fun fitnessModifier(): Float {
        return 0f
    }

    open fun lingering(): Boolean {
        return false
    }

    fun flag(status: Stsflag) {
        flags.add(status)
    }

    fun unflag(status: Stsflag) {
        flags.remove(status)
    }

    open fun flags(): Set<Stsflag> {
        return flags
    }

    fun withFlagRemoved(flag: Stsflag): Status {
        flags.remove(flag)
        return this
    }

    open fun overrides(s: Status?): Boolean {
        return s?.javaClass == this.javaClass
    }

    open fun replace(newStatus: Status?) {}
    fun mindgames(): Boolean {
        return flags().contains(Stsflag.mindgames)
    }

    abstract fun instance(newAffected: Character, newOther: Character?): Status
    open val variant: String?
        get() = toString()

    open fun struggle(character: Character?) {}
    open fun onRemove(c: Combat?, other: Character?) {}
    open fun onApply(c: Combat?, other: Character?) {}
    abstract fun saveToJson(): JsonObject
    abstract fun loadFromJson(obj: JsonObject): Status?
    open fun tick(c: Combat) {}
    open val isAddiction: Boolean
        get() = false

    open fun makeAllowedActionsPredicate(bearer: Participant): Predicate<Action.Instance> {
        return Predicate { action: Action.Instance? -> true }
    }

    open fun afterMatchRound() {}
}
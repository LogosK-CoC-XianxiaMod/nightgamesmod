package nightgames.areas

import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.global.Global
import nightgames.match.Action
import nightgames.match.Action.LocationDescription
import nightgames.match.Encounter
import nightgames.match.Encounter.IntrusionOption
import nightgames.match.Participant
import nightgames.match.actions.Move
import nightgames.status.Stsflag
import nightgames.trap.Trap
import java.io.Serializable
import java.util.*

class Area(@JvmField var name: String, private val descriptions: DescriptionModule) : Serializable {
    private val present = ArrayList<Participant>()
    var fight: Encounter? = null
    @JvmField
    var alarm = false
    @JvmField
    var env = ArrayList<Deployable>()

    @JvmField
    @Transient
    var drawHint = MapDrawHint()
    var isPinged = false
    private var attributes = setOf<AreaAttribute>()
    private val possibleActions: MutableSet<Action> = HashSet()
    private var trap: Trap.Instance? = null

    constructor(name: String, descriptions: DescriptionModule, attributes: Set<AreaAttribute>) : this(name, descriptions) {
        this.attributes = attributes
    }

    fun shortcut(sc: Area?) {
        possibleActions.add(Move.shortcut(sc))
    }

    fun jump(adj: Area?) {
        possibleActions.add(Move.ninjaLeap(adj))
    }

    fun open(): Boolean {
        return attributes.contains(AreaAttribute.Open)
    }

    fun ping(perception: Int): Boolean {
        if (fight != null) {
            return true
        }
        for (participant in present) {
            if (!(participant.character.check(Attribute.Cunning, Global.random(20) + perception) || !participant.state.isDetectable) || open()) {
                return true
            }
        }
        return alarm
    }

    fun enter(c: Character) {
        val p = Global.getMatch().findParticipant(c)
        present.add(p)
        System.out.printf("%s enters %s: %s\n", p.character.trueName, name, env)
        val deps: List<Deployable> = ArrayList(env)
        if (trap != null && trap!!.resolve(p)) {
            return
        }
        for (dep in deps) {
            if (dep.resolve(p)) {
                return
            }
        }
    }

    /**
     * Returns true if we took control of the turn
     */
    fun encounter(p: Participant): Boolean {
        // We can't run encounters if a fight is already occurring.
        if (fight != null) {
            val intrusionOptions: Set<IntrusionOption?> = fight!!.getCombatIntrusionOptions(p)
            if (!intrusionOptions.isEmpty()) {
                p.intrudeInCombat(intrusionOptions) { fight!!.watch() }
                return true
            }
        } else if (present.size > 1) {
            for (opponent in present) {          //FIXME: Currently - encounters repeat - Does this check if they are busy?
                if (opponent !== p // && Global.getMatch().canEngage(p, opponent)
                ) {
                    val fight = Encounter(p, opponent, this)
                    return fight.spotCheck()
                }
            }
        }
        return false
    }

    fun opportunity(target: Character, trap: Trap.Instance?): Boolean {
        val targetParticipant = Global.getMatch().findParticipant(target)
        for (opponent in present) {
            if (opponent !== targetParticipant) {
                if (targetParticipant.canStartCombat(opponent) && opponent.canStartCombat(targetParticipant) && fight == null) {
                    opponent.intelligence.promptTrap(
                            targetParticipant,
                            { fight!!.trap(opponent, targetParticipant, trap!!) }
                    ) {}
                    return true
                }
            }
        }
        clearTrap()
        return false
    }

    fun humanPresent(): Boolean {
        for (player in present) {
            if (player.character.human()) {
                return true
            }
        }
        return false
    }

    val isEmpty: Boolean
        get() = present.isEmpty()

    fun exit(c: Character?) {
        present.removeAll { it.character == c }
    }

    fun endEncounter() {
        fight = null
    }

    fun getMovementToAreaDescription(c: Character?): String {
        return descriptions.movedToLocation()
    }

    fun place(thing: Deployable) {
        if (thing is Trap.Instance) {
            trap = thing
        } else {
            env.add(thing)
        }
    }

    fun setTrap(t: Trap.Instance?) {
        trap = t
    }

    fun clearTrap() {
        trap = null
    }

    fun remove(triggered: Deployable) {
        env.remove(triggered)
    }

    fun getTrap(): Trap.Instance? = trap

    fun getTrap(p: Participant): Trap.Instance? = if (getTrap()?.owner == p) getTrap() else null

    val isDetected: Boolean
        get() = present.any { c: Participant -> c.character.`is`(Stsflag.detected) }

    fun possibleActions(p: Participant?): List<Action.Instance> = possibleActions
        .filter { action: Action -> action.usable(p) }
        .map { action: Action -> action.newInstance(p, this) }

    val occupants: Set<Participant>
        get() = this.present.toSet()

    // Stealthily slips a character into a room without triggering anything. Use with caution.
    fun place(p: Participant) {
        present.add(p)
    }

    fun setMapDrawHint(hint: MapDrawHint) {
        drawHint = hint
    }

    fun getPossibleActions(): MutableSet<Action> {
        return possibleActions
    }

    fun describe(): String {
        return descriptions.whereAmI() + possibleActions
                .filter { act: Action? -> act is LocationDescription }
                .map { act: Action -> (act as LocationDescription).describeLocation() }.joinToString("")
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -1372128249588089014L
        @JvmStatic
        fun addDoor(one: Area, other: Area) {
            one.possibleActions.add(Move.normal(other))
            other.possibleActions.add(Move.normal(one))
        }
    }
}
package nightgames.match

import nightgames.areas.Area
import nightgames.areas.Challenge
import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.combat.Combat
import nightgames.global.Global
import nightgames.match.Action.Ready
import nightgames.match.Encounter.IntrusionOption
import nightgames.match.actions.Move
import nightgames.match.actions.Resupply
import nightgames.modifier.action.DescribablePredicate
import java.util.*

open class Participant {
    // Below, the participant 'p' is the one who holds the state
    interface State {
        fun allowsNormalActions(): Boolean
        fun move(p: Participant)
        val isDetectable: Boolean
        fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant): Runnable?
        fun ineligibleCombatReplacement(p: Participant, other: Participant): Runnable?
        fun spotCheckDifficultyModifier(p: Participant): Int
        fun sendAssessmentMessage(p: Participant, observer: Character) {
            if (p.character.mostlyNude()) {
                observer.message("She is completely naked.")
            } else {
                observer.message("She is dressed and ready to fight.")
            }
        }
    }

    var character: Character
        protected set
    var intelligence: Intelligence
        private set
    var dialog: Dialog
        private set
    var score = 0
        private set
    @JvmField
    var state: State = Ready()
    @JvmField
    var invalidAttackers: MutableSet<Participant> = HashSet()
    @JvmField
    var challenges: MutableList<Challenge> = ArrayList()
    private val actionFilter: DescribablePredicate<Action.Instance>

    constructor(c: Character, actionFilter: DescribablePredicate<Action.Instance>) {
        character = c
        intelligence = c.makeIntelligence()
        dialog = c.makeDialog()
        this.actionFilter = actionFilter
    }

    internal constructor(p: Participant) {
        try {
            character = p.character.clone()
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
        intelligence = p.intelligence
        dialog = p.dialog
        score = p.score
        state = p.state
        invalidAttackers = HashSet(p.invalidAttackers)
        challenges = ArrayList(p.challenges)
        actionFilter = p.actionFilter
    }

    fun copy(): Participant {
        return Participant(this)
    }

    fun incrementScore(amt: Int, reason: String?) {
        score += amt
        location.occupants
                .forEach { participant -> participant.character.message(Match.scoreString(character, amt, reason)) }
    }

    fun place(loc: Area) {
        character.location.set(loc)
        loc.place(this)
        if (loc.name.isEmpty()) {
            throw RuntimeException("empty location")
        }
    }

    open fun canStartCombat(p2: Participant): Boolean {
        return !p2.invalidAttackers.contains(this) && p2.state !is Resupply.State
    }

    fun move() {
        character.displayStateMessage(character.location.get().getTrap(this))
        val possibleActions = ArrayList<Action>()
        possibleActions.addAll(character.getItemActions())
        possibleActions.addAll(Global.getMatch().availableActions)
        possibleActions.removeIf { a: Action -> !a.usable(this) }
        val possibleActionInstances = character.location.get().possibleActions(this).toMutableList()
        possibleActionInstances.addAll(
                possibleActions
                    .mapNotNull { action -> action.newInstance(this, character.location()) }
                    .filter(actionFilter::test).toSet()
        )
        character.status
                .map { s: nightgames.status.Status -> s.makeAllowedActionsPredicate(this) }
                .forEach { filter -> possibleActionInstances.removeIf(filter!!) }
        val callback : (Action.Instance) -> Unit = { it.execute() }
        state.move(this)
        if (state.allowsNormalActions()) {
            if (!character.location.get().encounter(this)) {
                intelligence.move(possibleActionInstances, callback)
            }
        }
    }

    fun flee() {
        val options = character.location.get().possibleActions(this)
        val destinations = options.filterIsInstance<Move.Instance>().map { it.destination }
        val destination = destinations[Global.random(destinations.size)]
        travel(destination, "You dash away and escape into the <b>" + destination.name + ".</b>")
    }

    fun endOfMatchRound() {
        character.getTraits().forEach{ trait: Trait ->
            if (trait.status != null) {
                val newStatus = trait.status.instance(character, null)
                if (!character.has(newStatus)) {
                    character.addNonCombat(Status(newStatus))
                }
            }
        }
        character.endOfMatchRound()
    }

    val location: Area
        get() = character.location()!!

    fun travel(dest: Area, message: String?) {
        state = Ready()
        character.location.get().exit(character)
        character.location.set(dest)
        dest.enter(character)
        if (dest.name.isEmpty()) {
            throw RuntimeException("empty location")
        }
        character.notifyTravel(dest, message)
    }

    open fun timePasses() {}
    fun intrudeInCombat(intrusionOptions: Set<IntrusionOption?>?, noneContinuation: Runnable?) {
        intelligence.intrudeInCombat(
            intrusionOptions,
            character.location.get().possibleActions(this).filterIsInstance<Move.Instance>(),
            { it.execute() },
            noneContinuation
        )
    }

    fun invalidateAttacker(victor: Participant) {
        invalidAttackers.add(victor)
    }

    fun finishMatch() {
        character.finishMatch()
        invalidAttackers.clear()
    }

    open fun pointsForVictory(loser: Participant): Int {
        return loser.pointsGivenToVictor()
    }

    protected open fun pointsGivenToVictor(): Int {
        return 1
    }

    fun accept(c: Challenge) {
        challenges.add(c)
    }

    fun evalChallenges(c: Combat?, victor: Character?) {
        for (chal in challenges) {
            chal.check(c, victor)
        }
    }

    fun makeCombatIntelligence(): nightgames.combat.Intelligence {
        return intelligence.makeCombatIntelligence()
    }
}
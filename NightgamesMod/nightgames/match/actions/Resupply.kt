package nightgames.match.actions

import nightgames.areas.Area
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant

abstract class Resupply protected constructor() : Action("Resupply") {
    interface Trigger {
        fun onActionStart(usedAction: Participant?)
    }

    open inner class Instance protected constructor(user: Participant?, location: Area?) : Action.Instance(user!!, location!!) {
        override fun execute() {
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    " heads for one of the safe rooms, probably to get a change of clothes.")
        }
    }

    open class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        override fun move(p: Participant) {
            p.invalidAttackers.clear()
            p.character.change()
            p.state = Ready()
            p.character.willpower.renew()
        }

        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant): Runnable? {
            throw UnsupportedOperationException(String.format("%s can't be attacked while resupplying",
                    p.character.trueName))
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant): Runnable? = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("%s can't be attacked while resupplying",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return !user!!.character.bound()
    }
}
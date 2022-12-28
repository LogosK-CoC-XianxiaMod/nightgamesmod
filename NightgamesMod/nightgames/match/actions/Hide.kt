package nightgames.match.actions

import nightgames.areas.Area
import nightgames.characters.Attribute
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant

class Hide : Action("Hide") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            user.character.message("You find a decent hiding place and wait for unwary opponents.")
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    " disappear into a hiding place.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return true
        }

        override fun move(p: Participant) {
            p.character.message("You have found a hiding spot and are waiting for someone to pounce upon.")
        }

        override val isDetectable: Boolean
            get() = false

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) = null

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            return p.character[Attribute.Cunning] * 2 / 3 + 20
        }
    }

    override fun usable(user: Participant?): Boolean {
        return user!!.state !is State && !user.character.bound()
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
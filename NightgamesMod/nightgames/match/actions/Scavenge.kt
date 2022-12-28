package nightgames.match.actions

import nightgames.areas.Area
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant
import java.util.*
import java.util.List
import java.util.function.Consumer

class Scavenge : Action("Scavenge Items") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    " begin scrounging through some boxes in the corner.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        override fun move(p: Participant) {
            val foundItems: Collection<Item>
            val roll = Global.random(10)
            foundItems = when (roll) {
                9 -> List.of(Item.Tripwire, Item.Tripwire)
                8 -> List.of(Item.ZipTie, Item.ZipTie, Item.ZipTie)
                7 -> List.of(Item.Phone)
                6 -> List.of(Item.Rope)
                5 -> List.of(Item.Spring)
                else -> listOf()
            }
            val character = p.character
            foundItems.forEach(Consumer { item: Item? -> character.gain(item) })
            if (foundItems.isEmpty()) {
                character.message("You don't find anything useful.")
            }
            p.state = Ready()
        }

        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) =
                Runnable { encounter.spy(other, p) }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return !user!!.character.bound()
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
package nightgames.match.actions

import nightgames.areas.Area
import nightgames.characters.Attribute
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant
import nightgames.match.Status
import nightgames.stance.Pin
import nightgames.status.Bound
import nightgames.status.Flatfooted
import java.util.*

class TreeAmbush : Action("Climb a Tree") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            if (user.character[Attribute.Animism] >= 10) {
                user.character.message("Following your instincts, you clamber up a tree to await an " +
                        "unwitting passerby.")
            } else {
                user.character.message("You climb up a tree that has a branch hanging over"
                        + " the trail. It's hidden in the leaves, so you should be"
                        + " able to surprise someone passing underneath.")
            }
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() + " climb up a tree.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return true
        }

        override fun move(p: Participant) {
            p.character.message("You are hiding in a tree, waiting to drop down on an unwitting foe.")
        }

        override val isDetectable: Boolean
            get() = false

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) = Runnable {
            other.character.addNonCombat(Status(Flatfooted(other.character, 3)))
            if (p.character.has(Item.Handcuffs)) other.character.addNonCombat(Status(Bound(other.character, 75.0, "handcuffs"))) else other.character.addNonCombat(Status(Bound(other.character, 50.0, "zip-tie")))
            val fight = encounter.startFight(p, other)
            fight!!.setStance(Pin(p.character, other.character))
            var victimMessage = ("As you walk down the trail, you hear a slight rustling in the"
                    + " leaf canopy above you. You look up, but all you see is a flash of ")
            victimMessage += if (p.character.mostlyNude()) {
                "nude flesh"
            } else {
                "clothes"
            }
            victimMessage += (" before you are pushed to the ground. Before you have a chance to process"
                    + " what's going on, your hands are tied behind your back and your"
                    + " attacker, who now reveals {self:reflective} to be {self:name},"
                    + " whispers in your ear \"Happy to see me, {other:name}?\"")
            other.character.message(Global.format(victimMessage, p.character, other.character))
            var attackerMessage = ("Your patience finally pays off as {other:name} approaches the"
                    + " tree you are hiding in. You wait until the perfect moment,"
                    + " when {other:pronoun} is right beneath you, before you jump"
                    + " down. You land right on {other:possessive} shoulders, pushing"
                    + " {other:direct-object} firmly to the soft soil. Pulling our a ")
            attackerMessage += if (p.character.has(Item.Handcuffs)) {
                "pair of handcuffs, "
            } else {
                "zip-tie, "
            }
            attackerMessage += " you bind {other:possessive} hands together. There are worse" + " ways to start a match."
            p.character.message(Global.format(attackerMessage, p.character, other.character))
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return ((user!!.character[Attribute.Power] >= 20 || user.character[Attribute.Animism] >= 10)
                && user.state !is State
                && !user.character.bound())
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
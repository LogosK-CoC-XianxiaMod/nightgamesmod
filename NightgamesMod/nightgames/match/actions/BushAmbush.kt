package nightgames.match.actions

import nightgames.areas.Area
import nightgames.characters.Attribute
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant
import nightgames.match.Status
import nightgames.stance.Mount
import nightgames.status.Bound
import nightgames.status.Flatfooted

class BushAmbush : Action("Hide in Bushes") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            if (user.character[Attribute.Animism] >= 10) {
                user.character.message("You crouch down in some dense bushes, ready" + " to pounce on passing prey.")
            } else {
                user.character.message("You spot some particularly dense bushes, and figure"
                        + " they'll make for a decent hiding place. You lie down in them,"
                        + " and wait for someone to walk past.")
            }
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    " dive into some bushes.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return true
        }

        override fun move(p: Participant) {
            p.character.message("You are hiding in dense bushes, waiting for someone to pass by.")
        }

        override val isDetectable: Boolean
            get() = false

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) = Runnable {
            other.character.addNonCombat(Status(Flatfooted(other.character, 3)))
            if (p.character.has(Item.Handcuffs)) other.character.addNonCombat(Status(Bound(other.character, 75.0, "handcuffs"))) else other.character.addNonCombat(Status(Bound(other.character, 50.0, "zip-tie")))
            val fight = encounter.startFight(p, other)
            fight!!.setStance(Mount(p.character, other.character))
            val victimMessage = ("You are having a little difficulty wading through the dense"
                    + " bushes. Your foot hits something, causing you to trip and fall flat"
                    + " on your face. A weight settles on your back and your arms are"
                    + " pulled behind your back and tied together with something. You"
                    + " are rolled over, and {self:name} comes into view as {self:pronoun}"
                    + " settles down on your belly. \"Hi, {other:name}. Surprise!\"")
            other.character.message(Global.format(victimMessage, p.character, other.character))
            val attackerMessage = ("Hiding in the bushes, your vision is somewhat obscured. This is"
                    + " not a big problem, though, as the rustling leaves alert you to"
                    + " passing prey. You inch closer to where you suspect they are headed,"
                    + " and slowly {other:name} comes into view. Just as {other:pronoun}"
                    + " passes you, you stick out a leg and trip {other:direct-object}."
                    + " With a satisfying crunch of the leaves, {other:pronoun} falls."
                    + " Immediately you jump on {other:possessive} back and tie "
                    + "{other:possessive} hands together.")
            p.character.message(Global.format(attackerMessage, p.character, other.character))
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return ((user!!.character[Attribute.Cunning] >= 20 || user.character[Attribute.Animism] >= 10)
                && user.state !is State
                && !user.character.bound())
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
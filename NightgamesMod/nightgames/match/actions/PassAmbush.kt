package nightgames.match.actions

import nightgames.areas.Area
import nightgames.characters.Attribute
import nightgames.global.Global
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant
import nightgames.match.Status
import nightgames.status.Flatfooted

class PassAmbush : Action("Try Ambush") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            user.character.message("You try to find a decent hiding place in the irregular rock faces lining the pass.")
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() + " slip into an alcove.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        override fun move(p: Participant) {
            p.character.message("You are hiding in an alcove in the pass.")
        }

        override val isDetectable: Boolean
            get() = false

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) = Runnable {
            val attackerScore = 30 + p.character[Attribute.Speed] * 10 + p.character[Attribute.Perception] * 5 + Global.random(30)
            val victimScore = other.character[Attribute.Speed] * 10 + other.character[Attribute.Perception] * 5 + Global.random(30)
            val attackerMessage: String
            val victimMessage: String
            if (attackerScore > victimScore) {
                attackerMessage = ("You wait in a small alcove, waiting for someone to pass you."
                        + " Eventually, you hear footsteps approaching and you get ready."
                        + " As soon as {other:name} comes into view, you jump out and push"
                        + " {other:direct-object} against the opposite wall. The impact seems to"
                        + " daze {other:direct-object}, giving you an edge in the ensuing fight.")
                victimMessage = ("Of course you know that walking through a narrow pass is a"
                        + " strategic risk, but you do so anyway. Suddenly, {self:name}"
                        + " flies out of an alcove, pushing you against the wall on the"
                        + " other side. The impact knocks the wind out of you, putting you"
                        + " at a disadvantage.")
            } else {
                attackerMessage = ("While you are hiding behind a rock, waiting for someone to"
                        + " walk around the corner up ahead, you hear a soft cruch behind"
                        + " you. You turn around, but not fast enough. {other:name} is"
                        + " already on you, and has grabbed your shoulders. You are unable"
                        + " to prevent {other:direct-object} from throwing you to the ground,"
                        + " and {other:pronoun} saunters over. \"Were you waiting for me,"
                        + " {self:name}? Well, here I am.\"")
                victimMessage = ("You are walking through the pass when you see {self:name}"
                        + " crouched behind a rock. Since {self:pronoun} is very focused"
                        + " in looking the other way, {self:pronoun} does not see you coming."
                        + " Not one to look a gift horse in the mouth, you sneak up behind"
                        + " {self:direct-object} and grab {self:direct-object} in a bear hug."
                        + " Then, you throw {self:direct-object} to the side, causing"
                        + " {self:direct-object} to fall to the ground.")
            }
            p.character.message(Global.format(attackerMessage, p.character, other.character))
            other.character.message(Global.format(victimMessage, p.character, other.character))
            encounter.startFight(p, other)
            if (attackerScore > victimScore) {
                other.character.addNonCombat(Status(Flatfooted(other.character, 3)))
            } else {
                p.character.addNonCombat(Status(Flatfooted(p.character, 3)))
            }
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return (user!!.state !is State
                && !user.character.bound())
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
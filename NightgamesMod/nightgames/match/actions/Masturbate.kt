package nightgames.match.actions

import nightgames.areas.Area
import nightgames.global.Flag
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant

class Masturbate : Action("Masturbate") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            if (user.character.hasDick()) {
                user.character.message("You desperately need to deal with your erection before you run into " +
                        "an opponent. You find an isolated corner and quickly jerk off.")
                if (Global.checkFlag(Flag.masturbationSemen)) {
                    if (user.character.arousal.percent() > 50) {
                        user.character.message("You remember that Reyka asked you to bring back some semen for " +
                                "her transformation rituals, and you catch your semen with one of her magic bottles.")
                        user.character.gain(Item.semen)
                    } else {
                        user.character.message("You remember that Reyka asked you to bring back some semen for " +
                                "her transformation rituals, and you catch your semen with one of her magic bottles. " +
                                "However it seems like you aren't quite aroused enough to provide the thick cum " +
                                "that she needs as the bottles seem to vomit back the cum you put in it.")
                    }
                }
            } else if (user.character.hasPussy()) {
                user.character.message(
                        "You desperately need to deal with your throbbing pussy before you run into an opponent. " +
                                "You find an isolated corner and quickly finger yourself to a quick orgasm.")
            } else {
                user.character.message(
                        "You desperately need to deal with your throbbing body before you run into an opponent. " +
                                "You find an isolated corner and quickly finger your ass to a quick orgasm.")
            }
            user.state = State()
            val c = user.character
            val mast: String
            mast = if (c.hasDick()) {
                String.format(" starts to stroke %s cock ", c.possessiveAdjective())
            } else if (c.hasPussy()) {
                String.format(" starts to stroke %s pussy ", c.possessiveAdjective())
            } else {
                String.format(" starts to finger %s ass ", c.possessiveAdjective())
            }
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    (mast + "while trying not to make much noise. It's quite a show."))
        }
    }

    class State : Busy(1) {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        public override fun moveAfterDelay(p: Participant?) {
            val character = p!!.character
            character.arousal.renew()
            character.update()
            p.character.message("You hurriedly stroke yourself off, eager to finish before someone catches you. " +
                    "After what seems like an eternity, you ejaculate into a tissue and "
                    + "throw it in the trash. Looks like you got away with it.")
            p.state = Ready()
        }

        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant): Runnable {
            return (Runnable { encounter.caught(other, p) })
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant): Runnable {
            return (Runnable { ineligibleMasturbatingMessages(p, other) })
        }

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return user!!.character.arousal.get() >= 15 && !user.character.bound()
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }

    companion object {
        private fun ineligibleMasturbatingMessages(pastLoser: Participant, pastWinner: Participant) {
            pastLoser.character.message(String.format("%s catches you masturbating, but fortunately %s's not yet allowed to attack you, so %s just "
                    + "watches you pleasure yourself with an amused grin.",
                    pastWinner.character.getName(), pastWinner.character.pronoun(), pastWinner.character.pronoun()))
            pastWinner.character.message(String.format("You stumble onto %s with %s hand between %s legs, masturbating. Since you just fought you still can't touch %s, so "
                    + "you just watch the show until %s orgasms.",
                    pastLoser.character.getName(), pastLoser.character.possessiveAdjective(), pastLoser.character.possessiveAdjective(), pastLoser.character.objectPronoun(),
                    pastLoser.character.pronoun()))
        }
    }
}
package nightgames.match.ftc

import nightgames.characters.Character
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Participant
import nightgames.modifier.action.DescribablePredicate
import kotlin.math.min

class Prey(c: Character?, actionFilter: DescribablePredicate<Action.Instance>) : Participant(c!!, actionFilter) {
    var gracePeriod = INITIAL_GRACE_PERIOD_ROUNDS
    var flagCounter = 0
    override fun canStartCombat(p2: Participant): Boolean {
        return gracePeriod <= 0 && super.canStartCombat(p2)
    }

    override fun timePasses() {
        gracePeriod = min(0, gracePeriod - 1)
        if (character.has(Item.Flag) && gracePeriod == 0 && ++flagCounter % 3 == 0) {
            incrementScore(1, "for holding the flag")
        }
    }

    fun grabFlag() {
        gracePeriod = INITIAL_GRACE_PERIOD_ROUNDS
        flagCounter = 0
        character.gain(Item.Flag)
    }

    override fun pointsForVictory(loser: Participant): Int {
        return super.pointsForVictory(loser)
    }

    override fun pointsGivenToVictor(): Int {
        return 0
    }

    companion object {
        var INITIAL_GRACE_PERIOD_ROUNDS = 3
    }
}
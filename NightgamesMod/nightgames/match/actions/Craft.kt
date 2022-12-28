package nightgames.match.actions

import nightgames.areas.Area
import nightgames.characters.Attribute
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Encounter
import nightgames.match.Participant
import java.util.*
import java.util.List
import java.util.function.Consumer

class Craft : Action("Craft Potion") {
    inner class Instance internal constructor(user: Participant, location: Area) : Action.Instance(user, location) {
        override fun execute() {
            user.state = State()
            messageOthersInLocation(user.character.grammar.subject().defaultNoun() +
                    " start mixing various liquids. Whatever it is doesn't look healthy.")
        }
    }

    class State : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        override fun move(p: Participant) {
            val craftedItems: Collection<Item>
            val roll = Global.random(15)
            craftedItems = if (p.character.check(Attribute.Cunning, 25)) {
                if (roll == 9) {
                    List.of(Item.Aphrodisiac, Item.DisSol)
                } else if (roll >= 5) {
                    List.of(Item.Aphrodisiac)
                } else {
                    List.of(Item.Lubricant, Item.Sedative)
                }
            } else if (p.character.check(Attribute.Cunning, 20)) {
                if (roll == 9) {
                    List.of(Item.Aphrodisiac)
                } else if (roll >= 7) {
                    List.of(Item.DisSol)
                } else if (roll >= 5) {
                    List.of(Item.Lubricant)
                } else if (roll >= 3) {
                    List.of(Item.Sedative)
                } else {
                    List.of(Item.EnergyDrink)
                }
            } else if (p.character.check(Attribute.Cunning, 15)) {
                if (roll == 9) {
                    List.of(Item.Aphrodisiac)
                } else if (roll >= 8) {
                    List.of(Item.DisSol)
                } else if (roll >= 7) {
                    List.of(Item.Lubricant)
                } else if (roll >= 6) {
                    List.of(Item.EnergyDrink)
                } else {
                    listOf()
                }
            } else if (roll >= 7) {
                List.of(Item.Lubricant)
            } else if (roll >= 5) {
                List.of(Item.Sedative)
            } else {
                listOf()
            }
            val character = p.character
            character.message("You spend some time crafting some potions with the equipment.")
            craftedItems.forEach(Consumer { item: Item? -> character.gain(item) })
            character.update()
            if (craftedItems.isEmpty()) {
                character.message("Your concoction turns a sickly color and releases a foul smelling smoke. " +
                        "You trash it before you do any more damage.")
            }
            p.state = Ready()
        }

        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant): Runnable {
            return (Runnable { encounter.spy(other, p) })
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }
    }

    override fun usable(user: Participant?): Boolean {
        return user!!.character[Attribute.Cunning] > 15 && !user.character.bound()
    }

    override fun newInstance(user: Participant?, location: Area?): Instance {
        return Instance(user!!, location!!)
    }
}
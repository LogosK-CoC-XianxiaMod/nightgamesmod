package nightgames.trap

import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action.Busy
import nightgames.match.Action.Ready
import nightgames.match.Encounter
import nightgames.match.Participant
import nightgames.stance.Position
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import java.util.*

class Spiderweb : Trap("Spiderweb") {
    private class Instance(self: Trap?, owner: Participant?) : Trap.Instance(self, owner) {
        public override fun trigger(target: Participant) {
            if (target.character.human()) {
                Global.gui().message(victimTriggerMessage(target))
            } else if (target.location.humanPresent()) {
                val model = JtwigModel.newModel()
                        .with("victim", target.character.grammar)
                Global.gui().message(OWNER_TRIGGER_TEMPLATE.render(model))
            }
            target.state = State()
            target.location.opportunity(target.character, this)
        }

        override fun capitalize(attacker: Participant, victim: Participant): Optional<Position> {
            onSpiderwebDefeat(attacker, victim, this)
            return super.capitalize(attacker, victim)
        }

        companion object {
            private fun victimTriggerMessage(victim: Participant): String {
                val msg = StringBuilder()
                msg.append("You feel the tripwire underfoot too late to avoid it. A staggering amount of rope flies up " +
                        "to entangle your limbs and pull you off the ground. ")
                if (!victim.character.mostlyNude()) {
                    msg.append("Something snags your clothes and pulls them off of you with unbelievable precision.")
                }
                msg.append("Oh hell. You're completely immobilized and suspended naked in midair. Surprisingly, it's not " +
                        "that uncomfortable, but if someone finds you before you can get free, you'll be completely " +
                        "defenseless.")
                return msg.toString()
            }

            private val OWNER_TRIGGER_TEMPLATE = JtwigTemplate.inlineTemplate(
                    "You hear a snap as {{ victim.subject().defaultNoun() }} triggers your spiderweb trap and ends up " +
                            "helplessly suspended in midair like a naked present.")
        }
    }

    class State : Busy(1) {
        override fun allowsNormalActions(): Boolean {
            return false
        }

        public override fun moveAfterDelay(p: Participant?) {
            p!!.character.message("You eventually manage to get an arm free, which you then use to extract yourself from the trap.")
            p.state = Ready()
        }

        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) =
                Runnable { encounter.spider(other, p) }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("spot check for %s should have already been replaced",
                    p.character.trueName))
        }

        override fun sendAssessmentMessage(p: Participant, observer: Character) {
            observer.message("She is naked and helpless. There's no way she could fight back.")
        }
    }

    override fun requiredItems(): Map<Item, Int> {
        return REQUIRED_ITEMS
    }

    override fun requirements(user: Participant): Boolean {
        return user.character.has(Trait.spider) && !user.character.has(Trait.roboweb)
    }

    override fun instantiate(owner: Participant): InstantiateResult {
        deductCostsFrom(owner)
        return InstantiateResult(CREATION_MESSAGE, Instance(this, owner))
    }

    companion object {
        private val REQUIRED_ITEMS = java.util.Map.of(Item.Rope, 4,
                Item.Spring, 2,
                Item.Tripwire, 1)

        fun onSpiderwebDefeat(attacker: Participant, victim: Participant, trap: Trap.Instance?) {
            printSpiderwebLines(attacker.character, victim.character)

            // This code is identical to the encounter defeat code. If there are more use
            // cases like this it needs to be encapsulated somewhere more general.
            if (victim.character.mostlyNude()) {
                attacker.character.gain(victim.character.trophy)
            }
            victim.character.nudify()
            victim.invalidateAttacker(attacker)
            victim.character.arousal.renew()
            victim.state = Ready()
            attacker.character.tempt(20)
            attacker.incrementScore(attacker.pointsForVictory(victim), "for a devious victory")
            attacker.state = Ready()
            victim.location.clearTrap()
        }

        private fun printSpiderwebLines(attacker: Character, victim: Character) {
            if (attacker.human()) {
                Global.gui().message(
                        victim.getName() + " is naked and helpless in the giant rope web. You approach slowly, taking in the lovely view of her body. You trail your fingers "
                                + "down her front, settling between her legs to tease her sensitive pussy lips. She moans and squirms, but is completely unable to do anything in her own defense. "
                                + "You are going to make her cum, that's just a given. If you weren't such a nice guy, you would leave her in that trap afterward to be everyone else's prey "
                                + "instead of helping her down. You kiss and lick her neck, turning her on further. Her entrance is wet enough that you can easily work two fingers into her "
                                + "and begin pumping. You gradually lick your way down her body, lingering at her nipples and bellybutton, until you find yourself eye level with her groin. "
                                + "You can see her clitoris, swollen with arousal, practically begging to be touched. You trap the sensitive bud between your lips and attack it with your tongue. "
                                + "The intense stimulation, coupled with your fingers inside her, quickly brings her to orgasm. While she's trying to regain her strength, you untie the ropes "
                                + "binding her hands and feet and ease her out of the web.")
            } else if (victim.human()) {
                Global.gui().message("You're trying to figure out a way to free yourself, when you see " + attacker.getName()
                        + " approach. You groan in resignation. There's no way you're "
                        + "going to get free before she finishes you off. She smiles as she enjoys your vulnerable state. She grabs your dangling penis and puts it in her mouth, licking "
                        + "and sucking it until it's completely hard. Then the teasing starts. She strokes you, rubs you, and licks the head of your dick. She uses every technique to "
                        + "pleasure you, but stops just short of letting you ejaculate. It's maddening. Finally you have to swallow your pride and beg to cum. She pumps you dick in earnest "
                        + "now and fondles your balls. When you cum, you shoot your load onto her face and chest. You hang in the rope web, literally and figuratively drained. "
                        + attacker.getName() + " " + "graciously unties you and helps you down.")
            }
        }

        private const val CREATION_MESSAGE = "With quite a bit of time and effort, you carefully setup a " +
                "complex series of spring loaded snares. Anyone who gets caught in this will be rendered as helpless " +
                "as a fly in a web."
    }
}
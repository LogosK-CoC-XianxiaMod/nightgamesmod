package nightgames.match

import nightgames.areas.Area
import nightgames.characters.*
import nightgames.combat.Combat
import nightgames.global.Global
import nightgames.match.Action.Ready
import nightgames.stance.Position
import nightgames.status.Enthralled
import nightgames.status.Flatfooted
import nightgames.status.Hypersensitive
import nightgames.status.Stsflag
import nightgames.trap.Spiderweb
import nightgames.trap.Trap
import java.util.*
import kotlin.math.max

class Encounter(protected var p1: Participant, protected var p2: Participant, protected var location: Area) {
    private var p1ff = false
    private var p2ff = false

    @Transient
    private var p1Guaranteed: Optional<String>

    @Transient
    private var p2Guaranteed: Optional<String>

    @Transient
    var combat: Combat? = null
        protected set
        get() = field
    private var checkin = 0

    init {
        p1Guaranteed = Optional.empty()
        p2Guaranteed = Optional.empty()
        checkEnthrall(p1, p2)
        checkEnthrall(p2, p1)
    }

    /**
     * Checks for and runs any scenarios that arise from two Characters encountering each other.
     * Returns true if something has come up that prevents them from being presented with the usual
     * campus Actions.
     */
    fun spotCheck(): Boolean {
        return if (p1.canStartCombat(p2) && p2.canStartCombat(p1)) {
            eligibleSpotCheck()
            true
        } else {
            ineligibleSpotCheck()
            false
        }
    }

    fun spy(attacker: Participant, victim: Participant) {
        attacker.intelligence.spy(victim, { ambush(attacker, victim) }) {}
    }

    protected fun eligibleSpotCheck() {
        val p1Replacement = p1.state.eligibleCombatReplacement(this, p1, p2)
        val p2Replacement = p2.state.eligibleCombatReplacement(this, p2, p1)
        assert(p1Replacement == null || p2Replacement == null)
        if (p1Replacement != null) {
            p1Replacement.run()
            return
        }
        if (p2Replacement != null) {
            p2Replacement.run()
            return
        }

        // We need to run both vision checks no matter what, and they have no
        // side effects besides.
        val p2_sees_p1 = spotCheck(p2, p1)
        val p1_sees_p2 = spotCheck(p1, p2)
        if (p2_sees_p1 && p1_sees_p2) {
            p1.intelligence.faceOff(p2,
                    { fightOrFlight(p1, true, Optional.empty()) },
                    { fightOrFlight(p1, false, Optional.empty()) }
            ) { fightOrFlight(p1, false, Optional.of(smokeMessage(p1.character))) }
            p2.intelligence.faceOff(p1,
                    { fightOrFlight(p2, true, Optional.empty()) },
                    { fightOrFlight(p2, false, Optional.empty()) }
            ) { fightOrFlight(p2, false, Optional.of(smokeMessage(p2.character))) }
        } else if (p2_sees_p1) {
            p2.intelligence.spy(p1, { ambush(p2, p1) }) {}
        } else if (p1_sees_p2) {
            p1.intelligence.spy(p2, { ambush(p1, p2) }) {}
        }
    }

    private fun ineligibleSpotCheck() {
        if (!p1.canStartCombat(p2)) {
            p1.state.ineligibleCombatReplacement(p1, p2) ?: Runnable { ineligibleMessages(p1, p2) }.run()
        } else {
            p2.state.ineligibleCombatReplacement(p2, p1) ?: Runnable { ineligibleMessages(p2, p1) }.run()
        }
    }

    /**
     * @param p The Character making the decision.
     * @param fight Whether the Character wishes to fight (true) or flee (false).
     * @param guaranteed Whether the Character's option is guaranteed to work. If so, the provided
     */
    private fun fightOrFlight(p: Participant, fight: Boolean, guaranteed: Optional<String>) {
        if (p === p1) {
            p1ff = fight
            p1Guaranteed = guaranteed
        } else {
            p2ff = fight
            p2Guaranteed = guaranteed
        }
        checkin++
        if (checkin >= 2) {
            doFightOrFlight()
        }
    }

    private fun doFightOrFlight() {
        if (p1ff && p2ff) {
            startFight(p1, p2)
        } else if (p1ff) {
            fightOrFlee(p1, p2)
        } else if (p2ff) {
            fightOrFlee(p2, p1)
        } else {
            bothFlee()
        }
    }

    fun startFight(p1: Participant, p2: Participant): Combat {
        combat = if (p1.character is Player && p2.character is NPC) {
            Combat(p1, p2, p1.character.location()!!) // Not sure if order matters
        } else if (p2.character is Player && p1.character is NPC) {
            Combat(p2, p1, p2.character.location()!!)
        } else {
            Combat(p1, p2, location)
        }
        p1.character.notifyCombatStart(combat, p2.character)
        p2.character.notifyCombatStart(combat, p1.character)
        location.fight = this
        return combat!!
    }

    // One Character wishes to Fight while the other attempts to flee.
    private fun fightOrFlee(fighter: Participant, fleer: Participant) {
        val fighterGuaranteed = if (fighter === p1) p1Guaranteed else p2Guaranteed
        val fleerGuaranteed = if (fleer === p1) p1Guaranteed else p2Guaranteed

        // Fighter wins automatically
        if (fighterGuaranteed.isPresent && fleerGuaranteed.isEmpty) {
            fighter.character.message(fighterGuaranteed.get())
            startFight(fighter, fleer)
            return
        }

        // Fleer wins automatically
        if (fleerGuaranteed.isPresent) {
            fleer.character.message(fleerGuaranteed.get())
            p2.flee()
            return
        }

        // Roll to see who's will triumphs
        if (rollFightVsFlee(fighter.character, fleer.character)) {
            fighter.character.message(fleer.character.getName() + " dashes away before you can move.")
            fleer.flee()
        } else {
            fighter.character.message(String.format(
                    "%s tries to run, but you stay right on %s heels and catch %s.",
                    fleer.character.getName(), fleer.character.possessiveAdjective(), fleer.character.objectPronoun()))
            fleer.character.message(String.format(
                    "You quickly try to escape, but %s is quicker. %s corners you and attacks.",
                    fighter.character.getName(), Global.capitalizeFirstLetter(fighter.character.pronoun())))
            startFight(fighter, fleer)
        }
    }

    /** Weights a roll with the fighter and fleers stats to determine who prevails. Returns
     * true if the fleer escapes, false otherwise.
     */
    private fun rollFightVsFlee(fighter: Character, fleer: Character): Boolean {
        return fleer.check(Attribute.Speed, 10 + fighter[Attribute.Speed] + (if (fighter.has(Trait.sprinter)) 5 else 0)
                + if (fleer.has(Trait.sprinter)) -5 else 0)
    }

    private fun bothFlee() {
        if (p1Guaranteed.isPresent) {
            p1.character.message(p1Guaranteed.get())
            p2.character.message(p1Guaranteed.get())
            p1.flee()
        } else if (p2Guaranteed.isPresent) {
            p1.character.message(p2Guaranteed.get())
            p2.character.message(p2Guaranteed.get())
            p2.flee()
        } else if (p1.character[Attribute.Speed] + Global.random(10) >= p2.character[Attribute.Speed] + Global.random(10)) {
            p2.character.message(p1.character.getName() + " dashes away before you can move.")
            p1.flee()
        } else {
            p1.character.message(p2.character.getName() + " dashes away before you can move.")
            p2.flee()
        }
    }

    fun ambush(attacker: Participant, target: Participant) {
        target.character.addNonCombat(Status(Flatfooted(target.character, 3)))
        val msg = Global.format("{self:SUBJECT-ACTION:catch|catches} {other:name-do} by surprise and {self:action:attack|attacks}!", attacker.character, target.character)
        p1.character.message(msg)
        p2.character.message(msg)
        startFight(attacker, target)
    }

    fun caught(attacker: Participant, target: Participant) {
        target.character.message("You jerk off frantically, trying to finish as fast as possible. Just as you feel the familiar sensation of imminent orgasm, you're grabbed from behind. "
                + "You freeze, cock still in hand. As you turn your head to look at your attacker, "
                + attacker.character.getName()
                + " kisses you on the lips and rubs the head of your penis with her "
                + "palm. You were so close to the edge that just you cum instantly.")
        if (!target.character.mostlyNude()) {
            target.character.message("You groan in resignation and reluctantly strip off your clothes and hand them over.")
        }
        attacker.character.message("You spot " + target.character.getName()
                + " leaning against the wall with her hand working excitedly between her legs. She is mostly, but not completely successful at "
                + "stifling her moans. She hasn't noticed you yet, and as best as you can judge, she's pretty close to the end. It'll be an easy victory for you as long as you work fast. "
                + "You sneak up and hug her from behind while kissing the nape of her neck. She moans and shudders in your arms, but doesn't stop fingering herself. She probably realizes "
                + "she has no chance of winning even if she fights back. You help her along by licking her neck and fondling her breasts as she hits her climax.")
        if (!target.character.mostlyNude()) {
            attacker.character.gain(target.character.trophy)
        }
        attacker.character.gainXP(attacker.character.getVictoryXP(target.character))
        attacker.character.tempt(20)
        attacker.incrementScore(attacker.pointsForVictory(target),
                "for a win, by being in the right place at the wrong time")
        attacker.state = Ready()
        target.character.gainXP(target.character.getDefeatXP(attacker.character))
        target.character.nudify()
        target.invalidateAttacker(attacker)
        target.character.arousal.renew()
        target.state = Ready()
    }

    fun spider(attacker: Participant, target: Participant) {
        attacker.character.gainXP(attacker.character.getVictoryXP(target.character))
        target.character.gainXP(target.character.getDefeatXP(attacker.character))
        Spiderweb.onSpiderwebDefeat(attacker, target, location.getTrap()!!)
    }

    fun trap(opportunist: Participant, target: Participant, trap: Trap.Instance) {
        if (opportunist.character.human()) {
            Global.gui().message("You leap out of cover and catch " + target.character.getName() + " by surprise.")
        } else if (target.character.human()) {
            Global.gui().message("Before you have a chance to recover, " + opportunist.character.getName() + " pounces on you.")
        }
        val startingPosition = trap.capitalize(opportunist, target)
        startFight(opportunist, target)
        startingPosition.ifPresent { sp: Position? -> combat!!.setStanceRaw(sp!!) }
        if (combat!!.p1Character.human() || combat!!.p2Character.human()) {
            Global.gui().watchCombat(combat)
        }
    }

    private fun smokeMessage(c: Character): String {
        return String.format("%s a smoke bomb and %s.",
                Global.capitalizeFirstLetter(c.subjectAction("drop", "drops")), c.action("disappear", "disappears"))
    }

    inner class IntrusionOption(private val intruder: Participant, var target: Participant, val combat: Combat) {
        val targetCharacter: Character
            get() = target.character

        fun callback() {
            combat.intrude(intruder, target)
        }
    }

    fun getCombatIntrusionOptions(intruder: Participant): Set<IntrusionOption> {
        return if (combat == null || intruder.character == p1.character || intruder.character == p2.character) {
            setOf()
        } else setOf(IntrusionOption(intruder, p1, combat!!), IntrusionOption(intruder, p2, combat!!))
    }

    fun watch() {
        Global.gui().watchCombat(combat)
        combat!!.go()
    }

    companion object {
        private fun checkEnthrall(slave: Participant, master: Participant) {
            val enthrall = slave.character.getStatus(Stsflag.enthralled)
            if (enthrall != null) {
                if ((enthrall as Enthralled).master !== master.character) {
                    slave.character.removelist.add(enthrall)
                    slave.character.addNonCombat(Status(Flatfooted(slave.character, 2)))
                    slave.character.addNonCombat(Status(Hypersensitive(slave.character)))
                    slave.character.message("At " + master.character.getName() + "'s interruption, you break free from the"
                            + " succubus' hold on your mind. However, the shock all but"
                            + " short-circuits your brain; you "
                            + " collapse to the floor, feeling helpless and"
                            + " strangely oversensitive")
                    master.character.message(String.format(
                            "%s doesn't appear to notice you at first, but when you wave your hand close to %s face %s "
                                    + "eyes open wide and %s immediately drops to the floor. Although the display leaves you "
                                    + "somewhat worried about %s health, %s is still in a very vulnerable position and you never "
                                    + "were one to let an opportunity pass you by.",
                            slave.character.getName(), slave.character.possessiveAdjective(),
                            slave.character.possessiveAdjective(), slave.character.pronoun(),
                            slave.character.possessiveAdjective(), slave.character.pronoun()))
                }
            }
        }

        fun spotCheck(spotter: Participant, hidden: Participant): Boolean {
            if (spotter.character.bound()) {
                return false
            }
            var dc = hidden.character[Attribute.Cunning] / 3
            dc += hidden.state.spotCheckDifficultyModifier(hidden)
            if (hidden.character.has(Trait.Sneaky)) {
                dc += 20
            }
            dc -= dc * 5 / max(1, spotter.character[Attribute.Perception])
            return spotter.character.check(Attribute.Cunning, dc)
        }

        private fun ineligibleMessages(pastWinner: Participant, pastLoser: Participant) {
            pastLoser.character.message("You encounter " + pastWinner.character.getName() + ", but you still haven't recovered from your last fight.")
            pastWinner.character.message(String.format(
                    "You find %s still naked from your last encounter, but %s's not fair game again until %s replaces %s clothes.",
                    pastLoser.character.getName(), pastLoser.character.pronoun(), pastLoser.character.pronoun(), pastLoser.character.possessiveAdjective()))
        }
    }
}
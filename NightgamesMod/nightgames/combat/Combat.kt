package nightgames.combat

import nightgames.areas.Area
import nightgames.characters.*
import nightgames.characters.body.*
import nightgames.characters.body.mods.catcher.*
import nightgames.global.Flag
import nightgames.global.Global
import nightgames.items.Item
import nightgames.items.clothing.Clothing
import nightgames.items.clothing.ClothingSlot
import nightgames.items.clothing.ClothingTrait
import nightgames.match.Action.Ready
import nightgames.match.Encounter
import nightgames.match.MatchType
import nightgames.match.Participant
import nightgames.modifier.standard.NoRecoveryModifier
import nightgames.nskills.tags.SkillTag
import nightgames.pet.PetCharacter
import nightgames.pet.arms.ArmManager
import nightgames.skills.*
import nightgames.stance.*
import nightgames.status.*
import nightgames.status.Compulsive.Situation
import nightgames.status.Stunned
import nightgames.status.addiction.Addiction
import nightgames.status.addiction.AddictionType
import nightgames.utilities.ProseUtils
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Combat(p1: Participant, p2: Participant, loc: Area) : Cloneable {
    /**Combat phases. */
    private enum class CombatPhase {
        START, PRETURN, SKILL_SELECTION, PET_ACTIONS, DETERMINE_SKILL_ORDER, P1_ACT_FIRST, P2_ACT_FIRST, P1_ACT_SECOND, P2_ACT_SECOND, UPKEEP, RESULTS_SCENE, FINISHED_SCENE, ENDED
    }

    interface Phase {
        fun turn(c: Combat): Boolean
        fun next(c: Combat): Boolean
    }

    private interface FastSkippablePhase : Phase
    private interface SkippablePhase : FastSkippablePhase
    class State(private val combat: Combat) : Participant.State {
        private var delayRounds = 2
        override fun allowsNormalActions(): Boolean {
            return false
        }

        override fun move(p: Participant) {
            if (--delayRounds <= 0 && !combat.isEnded) {
                combat.go()
            } else {
                Global.getMatch().resume()
            }
        }

        override val isDetectable = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant): Runnable {
            throw UnsupportedOperationException(String.format("%s is already in combat!",
                    p.character.trueName))
        }

        override fun ineligibleCombatReplacement(p: Participant, other: Participant): Runnable? = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            throw UnsupportedOperationException(String.format("%s is already in combat!",
                    p.character.trueName))
        }
    }

    private class StartPhase : Phase {
        override fun turn(c: Combat): Boolean {
            c.phase = PreTurnPhase()
            return false
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class PreTurnPhase : Phase {
        override fun turn(c: Combat): Boolean {
            c.timer += 1
            val player: Character
            val other: Character
            if (c.p1.character.human()) {
                player = c.p1.character
                other = c.p2.character
            } else {
                player = c.p2.character
                other = c.p1.character
            }
            c.write(c.describe(player, other))
            c.write("test")
            if (!c.shouldAutoresolve() && !Global.checkFlag(Flag.noimage)) {
                Global.gui()
                        .clearImage()
                if (c.imagePath.isNotEmpty()) {
                    Global.gui().displayImage(c.imagePath)
                }
            }
            c.p1.character.preturnUpkeep()
            c.p2.character.preturnUpkeep()
            c.p1act = null
            c.p2act = null
            if (Global.random(3) == 0 && !c.shouldAutoresolve()) {
                c.checkForCombatComment()
            }
            c.phase = SkillSelectionPhase()
            return false
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class SkillSelectionPhase : Phase {
        override fun turn(c: Combat): Boolean {
            return if (c.p1act == null) {
                c.p1.act(c, c.p2)
            } else if (c.p2act == null) {
                c.p2.act(c, c.p1)
            } else {
                c.phase = PetActionsPhase()
                false
            }
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class PetActionsPhase : SkippablePhase {
        override fun turn(c: Combat): Boolean {
            val alreadyBattled: MutableSet<PetCharacter> = HashSet()
            if (c.petCombatants.size > 0) {
                if (!Global.checkFlag("NoPetBattles")) {
                    for (pet in c.petCombatants) {
                        if (alreadyBattled.contains(pet.character)) {
                            continue
                        }
                        for (otherPet in c.petCombatants) {
                            if (alreadyBattled.contains(otherPet.character)) {
                                continue
                            }
                            if (pet.character.getSelf().owner() != otherPet.character.getSelf().owner()
                                    && Global.random(2) == 0) {
                                c.petbattle(pet, otherPet)
                                alreadyBattled.add(pet.character)
                                alreadyBattled.add(otherPet.character)
                            }
                        }
                    }
                }
                c.petCombatants
                        .filter { pet: Assistant -> !alreadyBattled.contains(pet.character) }
                        .forEach { pet: Assistant ->
                            pet.act(c, pet.pickTarget(c))
                            c.write("<br/>")
                            if (pet.character.getSelf().owner().has(Trait.devoteeFervor) && Global.random(2) == 0) {
                                c.write(pet.character,
                                        Global.format("{self:SUBJECT} seems to have gained a second wind from {self:possessive} religious fervor!",
                                                pet.character,
                                                pet.character.getSelf().owner()))
                                pet.act(c, pet.pickTarget(c))
                            }
                        }
                c.write("<br/>")
            }
            c.phase = DetermineSkillOrderPhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class DetermineSkillOrderPhase : FastSkippablePhase {
        override fun turn(c: Combat): Boolean {
            val result: Phase = if (c.p1.character.init() + c.p1act!!.speed() >= c.p2.character.init() + c.p2act!!.speed()) {
                P1ActFirstPhase()
            } else {
                P2ActFirstPhase()
            }
            c.phase = result
            return false
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class P1ActFirstPhase : SkippablePhase {
        override fun turn(c: Combat): Boolean {
            if (c.doAction(c.p1.character, c.p1act!!.getDefaultTarget(c), c.p1act)) {
                c.phase = UpkeepPhase()
            } else {
                c.phase = P2ActSecondPhase()
            }
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class P2ActFirstPhase : SkippablePhase {
        override fun turn(c: Combat): Boolean {
            if (c.doAction(c.p2.character, c.p2act!!.getDefaultTarget(c), c.p2act)) {
                c.phase = UpkeepPhase()
            } else {
                c.phase = P1ActSecondPhase()
            }
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class P1ActSecondPhase : SkippablePhase {
        override fun turn(c: Combat): Boolean {
            c.doAction(c.p1.character, c.p1act!!.getDefaultTarget(c), c.p1act)
            c.phase = UpkeepPhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class P2ActSecondPhase : SkippablePhase {
        override fun turn(c: Combat): Boolean {
            c.doAction(c.p2.character, c.p2act!!.getDefaultTarget(c), c.p2act)
            c.phase = UpkeepPhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class UpkeepPhase : FastSkippablePhase {
        override fun turn(c: Combat): Boolean {
            c.p1.character.endOfCombatRound(c, c.p2.character)
            c.p2.character.endOfCombatRound(c, c.p1.character)
            // iterate through all the pets here so we don't get concurrent modification issues
            val pets = c.petCombatants
                    .map { obj: Assistant -> obj.character }
            pets.forEach { other: PetCharacter -> other.endOfCombatRound(c, c.getOpponentCharacter(other)) }
            c.checkStamina(c.p1.character)
            c.checkStamina(c.p2.character)
            pets.forEach { p: PetCharacter -> c.checkStamina(p) }
            c.doStanceTick(c.p1.character)
            c.doStanceTick(c.p2.character)
            val team1 = listOf(c.p1.character) + c.assistantsOf(c.p1.character)
                    .map { obj: Assistant -> obj.character }
            val team2 = listOf(c.p2.character) + c.assistantsOf(c.p2.character)
                    .map { obj: Assistant -> obj.character }
            team1.forEach { self -> c.doAuraTick(self, team2) }
            team2.forEach { self -> c.doAuraTick(self, team1) }
            c.combatantData.values.forEach { data -> data.tick(c) }
            c.getStance().decay(c)
            c.getStance().checkOngoing(c)
            c.p1.character.regen(c)
            c.p2.character.regen(c)
            c.phase = PreTurnPhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class ResultsScenePhase : Phase {
        override fun turn(c: Combat): Boolean {
            if (!c.cloned) {
                if (c.p1.character.checkLoss(c) && c.p2.character.checkLoss(c)) {
                    c.draw()
                } else if (c.p1.character.checkLoss(c)) {
                    c.victory(c.p2)
                } else if (c.p2.character.checkLoss(c)) {
                    c.victory(c.p1)
                }
            }
            c.phase = FinishedScenePhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class FinishedScenePhase : Phase {
        override fun turn(c: Combat): Boolean {
            c.phase = EndedPhase()
            return c.phase.next(c)
        }

        override fun next(c: Combat): Boolean {
            return c.next()
        }
    }

    private class EndedPhase : Phase {
        override fun turn(c: Combat): Boolean {
            return next(c)
        }

        override fun next(c: Combat): Boolean {
            c.end()
            return true
        }
    }

    //TODO: Convert as much of this data as possible to CombatData - DSm
    private var p1: Combatant
    private var p2: Combatant
    private var petCombatants: MutableList<Assistant>
    var combatantData: MutableMap<String, CombatantData>
    @JvmField
    var winner: Combatant?
    var phase: Phase
    protected var p1act: Skill? = null
    protected var p2act: Skill? = null
    var location: Area
    private var stance: Position
    var timer: Int
        protected set
    @JvmField
    var state: Result? = null
    var lastFailed = false
    private var log: CombatLog? = null
    private var postCombatScenesSeen: Int
    private var wroteMessage: Boolean
    private var cloned: Boolean
    private val watchers: MutableSet<Character> = HashSet()
    var imagePath = ""
    private fun applyCombatStatuses(self: Character, other: Character) {
        if (other.human()) {
            write(self.challenge(other))
        }
        self.addictions.forEach { a: Addiction ->
            if (a.isActive) {
                val status = a.startCombat(this, other)
                if (status != null) self.add(this, status)
            }
        }
        if (self.has(Trait.zealinspiring) && other.getAddiction(AddictionType.ZEAL)?.isInWithdrawal == true) {
            self.add(this, DivineCharge(self, .3))
        }
        if (self.has(Trait.suave) && !other.hasDick()) {
            self.add(this, SapphicSeduction(self))
        }
        if (self.has(Trait.footfetishist)) {
            applyFetish(self, other, FeetPart.TYPE)
        }
        if (self.has(Trait.breastobsessed) && other.hasBreasts()) {
            applyFetish(self, other, BreastsPart.TYPE)
        }
        if (self.has(Trait.assaddict)) {
            applyFetish(self, other, AssPart.TYPE)
        }
        if (self.has(Trait.pussywhipped) && other.hasPussy()) {
            applyFetish(self, other, PussyPart.TYPE)
        }
        if (self.has(Trait.cockcraver) && other.hasDick()) {
            applyFetish(self, other, CockPart.TYPE)
        }
    }

    fun applyFetish(self: Character, other: Character, FetishType: String) {
        if (other.body.getRandom(FetishType) != null && self.body.getFetish(FetishType).isEmpty) {
            if (self.human()) {
                val part = other.body.getRandom(FetishType) as GenericBodyPart
                write(self,
                        "As your first battle of the night begins, you can't help but think about "
                                + other.nameOrPossessivePronoun() + " " + FetishType
                                + " and how " + ProseUtils.neuterSubjectPronoun(part.isMultipleObjects)
                                + " would feel on your skin.")
            }
            self.add(this, BodyFetish(self, null, FetishType, .25))
        }
    }

    fun go() {
        if (p1.character.mostlyNude() && !p2.character.mostlyNude()) {
            p1.character.emote(Emotion.nervous, 20)
        }
        if (p2.character.mostlyNude() && !p1.character.mostlyNude()) {
            p2.character.emote(Emotion.nervous, 20)
        }
        applyCombatStatuses(p1.character, p2.character)
        applyCombatStatuses(p2.character, p1.character)
        updateMessage()
        if (doExtendedLog()) {
            log!!.logHeader()
        }
        if (shouldAutoresolve()) {
            autoresolve()
        } else {
            phase.next(this)
        }
    }

    private fun resumeNoClearFlag() {
        paused = false
        while (!paused && !turn()) { }
        if (phase is EndedPhase) {
            updateAndClearMessage()
        }
    }

    fun resume() {
        wroteMessage = false
        resumeNoClearFlag()
    }

    fun getCombatantData(character: Character): CombatantData? {
        if (!combatantData.containsKey(character.trueName)) {
            combatantData[character.trueName] = CombatantData()
        }
        return combatantData[character.trueName]
    }

    private fun checkBottleCollection(victor: Character, loser: Character, modType: String): Boolean {
        return (victor.has(Item.EmptyBottle, 1)
                && loser.body.randomPussy.moddedPartCountsAs(modType))
    }

    private fun draw() {
        state = eval()
        p1.participant.evalChallenges(this, null)
        p2.participant.evalChallenges(this, null)
        if (p1.character.has(Trait.slime)) {
            p1.character.purge(this)
        }
        if (p2.character.has(Trait.slime)) {
            p2.character.purge(this)
        }
        val first = p1.character
        val second = p2.character
        first.gainXP(first.getVictoryXP(second))
        first.orgasm()
        first.undress(this)
        first.gainTrophy(this, second)
        p1.participant.invalidateAttacker(p2.participant)
        first.gainAttraction(second, 4)
        second.gainXP(second.getVictoryXP(first))
        second.orgasm()
        second.undress(this)
        second.gainTrophy(this, first)
        p2.participant.invalidateAttacker(p1.participant)
        second.gainAttraction(first, 4)
        if (p1.character.human()) {
            p2.character.sendDrawMessage(this, state)
        } else if (p2.character.human()) {
            p1.character.sendDrawMessage(this, state)
        }
        winner = null
    }

    private fun victory(won: Combatant) {
        state = eval()
        p1.participant.evalChallenges(this, won.character)
        p2.participant.evalChallenges(this, won.character)
        val winner = won.character
        val loser = getOpponentCharacter(won.character)
        if (won.character.has(Trait.slime)) {
            won.character.purge(this)
        }
        winner.gainXP(winner.getDefeatXP(loser))
        if (!winner.human() || Global.getMatch().condition.name() != NoRecoveryModifier.NAME) {
            winner.orgasm()
        }
        winner.dress(this)
        winner.gainAttraction(loser, 1)
        won.participant.incrementScore(
                won.participant.pointsForVictory(getOpponent(won.character).participant),
                "for a win")
        loser.gainXP(loser.getVictoryXP(winner))
        loser.orgasm()
        loser.undress(this)
        getOpponent(winner).participant.invalidateAttacker(won.participant)
        loser.gainAttraction(winner, 2)
        if (won.character.human()) {
            loser.sendDefeatMessage(this, state)
        } else if (loser.human()) {
            won.character.sendVictoryMessage(this, state)
        }
        val victor = won.character

        //Collect Bottle-able substances.
        doBottleCollection(victor, loser)

        //If they lost, Do a willpower gain.
        if (loser.human() && loser.willpower.max() < loser.getMaxWillpowerPossible()) {
            write("<br/>Ashamed at your loss, you resolve to win next time.")
            write("<br/><b>Gained 1 Willpower</b>.")
            loser.willpower.gain(1f)
        }
        victor.willpower.renew()
        loser.willpower.renew()
        if (Global.getMatch().type == MatchType.FTC && loser.has(Item.Flag)) {
            write(victor, Global.format(
                    "<br/><b>{self:SUBJECT-ACTION:take|takes} the " + "Flag from {other:subject}!</b>", victor,
                    loser))
            loser.remove(Item.Flag)
            victor.gain(Item.Flag)
        }
        this.winner = won
    }

    private fun checkLosses(): Boolean {
        if (cloned) {
            return false
        }
        if (p1.character.checkLoss(this) && p2.character.checkLoss(this)) {
            return true
        }
        if (p1.character.checkLoss(this)) {
            winner = p2
            return true
        }
        if (p2.character.checkLoss(this)) {
            winner = p1
            return true
        }
        return false
    }

    private fun checkForCombatComment() {
        val other: Character = if (p1.character.human()) {
            p2.character
        } else if (p2.character.human()) {
            p1.character
        } else {
            (if (Global.random(2) == 0) p1.character else p2.character) as NPC
        }
        if (other is NPC) {
            val comment = other.getComment(this)
            if (comment.isPresent) {
                write(other, "<i>\"" + Global.format(comment.get(), other, Global.getPlayer()) + "\"</i>")
            }
        }
    }

    private fun doAuraTick(character: Character, opponents: List<Character>) {
        if (character.has(Trait.overwhelmingPresence)) {
            write(character, Global.format("{self:NAME-POSSESSIVE} overwhelming presence mentally exhausts {self:possessive} opponents.", character, character))
            opponents.forEach { opponent -> opponent.weaken(this, opponent.stamina.max() / 10) }
        }
        val beguilingbreastCompletedFlag = Trait.beguilingbreasts.name + "Completed"
        //Fix for Beguiling Breasts being seen when it shouldn't.
        if (character.has(Trait.beguilingbreasts) &&
                !getCombatantData(character)!!.getBooleanFlag(beguilingbreastCompletedFlag) &&
                character.outfit.slotOpen(ClothingSlot.top) &&
                getStance().facing(character, getOpponentCharacter(character)) &&
                !getOpponentCharacter(character).`is`(Stsflag.blinded)) {
            val mainOpponent = getOpponentCharacter(character)
            write(character, Global.format("The instant {self:subject-action:lay|lays} {self:possessive} eyes on {other:name-possessive} bare breasts, {self:possessive} consciousness flies out of {self:possessive} mind. " +
                    if (character.canAct()) ("{other:SUBJECT-ACTION:giggle|giggles} a bit and {other:action:cup} {other:possessive} {other:body-part:breasts}"
                            + "  and {other:action:give} them a little squeeze to which {self:subject} can only moan.") else "",
                    mainOpponent, character))
            // TODO: 50 turns seems so long.  This can't be balanced.
            opponents.forEach { opponent -> opponent.add(this, Trance(opponent, 50)) }
            getCombatantData(character)!!.setBooleanFlag(beguilingbreastCompletedFlag, true)
        }
        if (character.has(Trait.footfetishist)) {
            fetishDisadvantageAura(character, opponents, FeetPart.TYPE, ClothingSlot.feet)
        }
        if (character.has(Trait.breastobsessed)) {
            fetishDisadvantageAura(character, opponents, BreastsPart.TYPE, ClothingSlot.top)
        }
        if (character.has(Trait.assaddict)) {
            fetishDisadvantageAura(character, opponents, AssPart.TYPE, ClothingSlot.bottom)
        }
        if (character.has(Trait.pussywhipped)) {
            fetishDisadvantageAura(character, opponents, PussyPart.TYPE, ClothingSlot.bottom)
        }
        if (character.has(Trait.cockcraver)) {
            fetishDisadvantageAura(character, opponents, CockPart.TYPE, ClothingSlot.bottom)
        }
        opponents.forEach { opponent -> checkIndividualAuraEffects(character, opponent) }
    }

    private fun fetishDisadvantageAura(character: Character, opponents: List<Character>, fetishType: String, clothingType: ClothingSlot) {
        val otherWithAura = opponents.firstOrNull { other: Character -> other.body.getRandom(fetishType) != null }
        otherWithAura?.let { other ->
            val clothes = other.outfit.getTopOfSlot(clothingType)
            val seeFetish = clothes == null || clothes.layer <= 1 || other.outfit.exposure >= .5
            val partDescription = other.body.getRandom(fetishType).describe(other)
            if (seeFetish && Global.random(5) == 0) {
                if (character.human()) {
                    write(character, "You can't help thinking about " + otherWithAura.nameOrPossessivePronoun() + " " + partDescription + ".")
                }
                character.add(this, BodyFetish(character, null, fetishType, .05))
            }
        }
    }

    private fun checkIndividualAuraEffects(self: Character, other: Character) {
        if (self.has(Trait.magicEyeEnthrall) && other.arousal.percent() >= 50 && getStance().facing(other, self)
                && !other.`is`(Stsflag.blinded) && Global.random(20) == 0) {
            write(self,
                    Global.format("<br/>{other:NAME-POSSESSIVE} eyes start glowing and captures both {self:name-possessive} gaze and consciousness.",
                            other, self))
            other.add(this, Enthralled(other, self, 2))
        }
        if (self.has(Trait.magicEyeTrance) && other.arousal.percent() >= 50 && getStance().facing(other, self)
                && !other.`is`(Stsflag.blinded) && Global.random(10) == 0) {
            write(self,
                    Global.format("<br/>{other:NAME-POSSESSIVE} eyes start glowing and send {self:subject} straight into a trance.",
                            other, self))
            other.add(this, Trance(other))
        }
        if (self.has(Trait.magicEyeFrenzy) && other.arousal.percent() >= 50 && getStance().facing(other, self)
                && !other.`is`(Stsflag.blinded) && Global.random(10) == 0) {
            write(self,
                    Global.format("<br/>{other:NAME-POSSESSIVE} eyes start glowing and send {self:subject} into a frenzy.",
                            other, self))
            other.add(this, Frenzied(other, 3))
        }
        if (self.has(Trait.magicEyeArousal) && other.arousal.percent() >= 50 && getStance().facing(other, self)
                && !other.`is`(Stsflag.blinded) && Global.random(5) == 0) {
            write(self,
                    Global.format("<br/>{other:NAME-POSSESSIVE} eyes start glowing and {self:subject-action:feel|feels} a strong pleasure wherever {other:possessive} gaze lands. {self:SUBJECT-ACTION:are|is} literally being raped by {other:name-possessive} eyes!",
                            other, self))
            other.temptNoSkillNoSource(this, self, self[Attribute.Seduction] / 2)
        }
        if (getStance().facing(self, other) && other.breastsAvailable() && !self.has(Trait.temptingtits)
                && other.has(Trait.temptingtits) && !other.`is`(Stsflag.blinded)) {
            write(self, Global.format("{self:SUBJECT-ACTION:can't avert|can't avert} {self:possessive} eyes from {other:name-possessive} perfectly shaped tits sitting in front of {self:possessive} eyes.",
                    self, other))
            self.temptNoSkill(this, other, other.body.randomBreasts, 10 + max(0, other[Attribute.Seduction] / 3 - 7))
        } else if (getOpponentCharacter(self).has(Trait.temptingtits) && getStance().behind(other)) {
            write(self, Global.format("{self:SUBJECT-ACTION:feel|feels} a heat in {self:possessive} groin as {other:name-possessive} enticing tits press against {self:possessive} back.",
                    self, other))
            val selfTopExposure = self.outfit.getExposure(ClothingSlot.top)
            val otherTopExposure = other.outfit.getExposure(ClothingSlot.top)
            var temptDamage = (20 + max(0, other[Attribute.Seduction] / 2 - 12)).toDouble()
            temptDamage *= min(1.0, selfTopExposure + .5) * min(1.0, otherTopExposure + .5)
            self.temptNoSkill(this, other, other.body.randomBreasts, temptDamage.toInt())
        }
        if (self.has(Trait.enchantingVoice)) {
            val voiceCount = getCombatantData(self)!!.getIntegerFlag("enchantingvoice-count")
            if (voiceCount >= 1) {
                if (!self.human()) {
                    write(self,
                            Global.format("{other:SUBJECT} winks at you and verbalizes a few choice words that pass straight through your mental barriers.",
                                    other, self))
                } else {
                    write(self,
                            Global.format("Sensing a moment of distraction, you use the power in your voice to force {self:subject} to your will.",
                                    other, self))
                }
                Command(self).resolve(this, other)
                val cooldown = max(1, 6 - (self.progression.level - other.progression.level / 5))
                getCombatantData(self)!!.setIntegerFlag("enchantingvoice-count", -cooldown)
            } else {
                getCombatantData(self)!!.setIntegerFlag("enchantingvoice-count", voiceCount + 1)
            }
        }
        self.getArmManager()?.let { m: ArmManager -> m.act(this, self, other) }
        if (self.has(Trait.mindcontroller)) {
            val infra = self.outfit.getArticlesWithTrait(ClothingTrait.infrasound)
            val magnitude = infra.size * (Addiction.LOW_INCREASE / 6)
            if (magnitude > 0) {
                other.addict(this, AddictionType.MIND_CONTROL, self, magnitude)
                if (Global.random(3) == 0) {
                    val add = other.getAddiction(AddictionType.MIND_CONTROL)
                    val source = infra.toTypedArray()[0] as Clothing
                    val knows = add != null && add.atLeast(Addiction.Severity.MED) || other[Attribute.Cunning] >= 30 || other[Attribute.Science] >= 10
                    var msg: String?
                    if (other.human()) {
                        msg = "<i>You hear a soft buzzing, just at the edge of your hearing. "
                        msg += if (knows) {
                            Global.format("Although you can't understand it, the way it draws your"
                                    + " attention to {self:name-possessive} %s must mean it's"
                                    + " influencing you somehow!", self, other, source.name)
                        } else {
                            "It's probably nothing, though.</i>"
                        }
                    } else {
                        msg = Global.format("You see that {other:subject-action:is} distracted from {self:possessive} %s", self, other, source.name)
                    }
                    write(other, msg)
                }
            }
        }
    }

    private fun turn(): Boolean {
        if (p1.character.human() && p2.character is NPC) {
            Global.gui().loadPortrait(p2.character as NPC)
        } else if (p2.character.human() && p1.character is NPC) {
            Global.gui().loadPortrait(p1.character as NPC)
        }
        if (!(phase is FinishedScenePhase || phase is ResultsScenePhase)
                && checkLosses()) {
            phase = ResultsScenePhase()
            return phase.next(this)
        }
        if ((p1.character.orgasmed || p2.character.orgasmed)
                && phase !is ResultsScenePhase
                && phase is SkippablePhase) {
            phase = UpkeepPhase()
        }
        return phase.turn(this)
    }

    private fun describe(player: Character, other: Character): String {
        return if (isBeingObserved) {
            ("<font color='rgb(255,220,220)'>"
                    + other.describe(Global.getPlayer()[Attribute.Perception], Global.getPlayer())
                    + "</font><br/><br/><font color='rgb(220,220,255)'>"
                    + player.describe(Global.getPlayer()[Attribute.Perception], Global.getPlayer())
                    + "</font><br/><br/><font color='rgb(134,196,49)'><b>"
                    + Global.capitalizeFirstLetter(getStance().describe(this)) + "</b></font>")
        } else if (!player.`is`(Stsflag.blinded)) {
            (other.describe(player[Attribute.Perception], Global.getPlayer()) + "<br/><br/>"
                    + Global.capitalizeFirstLetter(getStance().describe(this)) + "<br/><br/>"
                    + player.describe(other[Attribute.Perception], other) + "<br/><br/>")
        } else {
            ("<b>You are blinded, and cannot see what " + other.trueName + " is doing!</b><br/><br/>"
                    + Global.capitalizeFirstLetter(getStance().describe(this)) + "<br/><br/>"
                    + player.describe(other[Attribute.Perception], other) + "<br/><br/>")
        }
    }

    protected fun eval(): Result {
        return if (getStance().bottom.human() && getStance().inserted(getStance().top) && getStance().en == Stance.anal) {
            Result.anal
        } else if (getStance().inserted()) {
            Result.intercourse
        } else {
            Result.normal
        }
    }

    private var paused: Boolean
    private var processedEnding: Boolean

    init {
        this.p1 = Combatant(p1)
        combatantData = HashMap()
        this.p2 = Combatant(p2)
        location = loc
        stance = Neutral(p1.character, p2.character)
        paused = false
        processedEnding = false
        timer = 0
        this.p1.participant.state = State(this)
        this.p2.participant.state = State(this)
        postCombatScenesSeen = 0
        petCombatants = ArrayList()
        wroteMessage = false
        winner = null
        phase = StartPhase()
        cloned = false
        if (doExtendedLog()) {
            log = CombatLog(this)
        }
    }

    //FIXME: Worship skills may not be properly changing stance - resulting in the worshipped character orgasming and triggering orgasm effectgs as if the player was still inserted. - DSM 
    fun getRandomWorshipSkill(self: Character?, other: Character): Skill? {
        val avail: MutableList<Skill?> = ArrayList(WORSHIP_SKILLS)
        if (other.has(Trait.piety)) {
            avail.add(ConcedePosition(self))
        }
        avail.shuffle()
        while (avail.isNotEmpty()) {
            val skill = avail.removeLast()!!.copy(self)
            if (Skill.isUsableOn(this, skill, other)) {
                write(other, Global.format(
                        "<b>{other:NAME-POSSESSIVE} divine aura forces {self:subject} to forget what {self:pronoun} {self:action:were|was} doing and crawl to {other:direct-object} on {self:possessive} knees.</b>",
                        self, other))
                return skill
            }
        }
        return null
    }

    private fun rollWorship(self: Character, other: Character): Boolean {
        if (!other.isPet() && (other.has(Trait.objectOfWorship) || self.`is`(Stsflag.lovestruck))
                && (other.breastsAvailable() || other.crotchAvailable())) {
            var chance = min(20, max(5, other[Attribute.Divinity] + 10 - self.progression.level)).toDouble()
            if (other.has(Trait.revered)) {
                chance += 10.0
            }
            chance += getCombatantData(self)!!.getDoubleFlag(TEMPT_WORSHIP_BONUS)
            if (Global.random(100) < chance) {
                getCombatantData(self)!!.setDoubleFlag(TEMPT_WORSHIP_BONUS, 0.0)
                return true
            }
        }
        return false
    }

    private fun rollAssWorship(self: Character, opponent: Character): Boolean {
        var chance = 0
        if (opponent.has(Trait.temptingass) && !opponent.isPet()) {
            chance += max(0, min(15, opponent[Attribute.Seduction] - self[Attribute.Seduction]))
            if (self.`is`(Stsflag.feral)) chance += 10
            if (self.`is`(Stsflag.charmed) || opponent.`is`(Stsflag.alluring)) chance += 5
            if (self.has(Trait.assmaster) || self.has(Trait.analFanatic)) chance += 5
            val fetish = self.body.getFetish(AssPart.TYPE)
            if (fetish.isPresent && opponent.has(Trait.bewitchingbottom)) {
                chance += (20 * fetish.get().magnitude).toInt()
            }
        }
        return Global.random(100) < chance
    }

    private fun checkWorship(self: Character, other: Character, def: Skill?): Skill? {
        if (rollWorship(self, other)) {
            return getRandomWorshipSkill(self, other) ?: def
        }
        if (rollAssWorship(self, other)) {
            val fuck = AssFuck(self)
            if (fuck.requirements(this, other) && fuck.usable(this, other) && !self.`is`(Stsflag.frenzied)) {
                write(other, Global.format("<b>The look of {other:name-possessive} ass,"
                        + " so easily within {self:possessive} reach, causes"
                        + " {self:subject} to involuntarily switch to autopilot."
                        + " {self:SUBJECT} simply {self:action:NEED|NEEDS} that ass.</b>",
                        self, other))
                self.add(this, Frenzied(self, 1))
                return fuck
            }
            val anilingus = Anilingus(self)
            if (anilingus.requirements(this, other) && anilingus.usable(this, other)) {
                write(other, Global.format("<b>The look of {other:name-possessive} ass,"
                        + " so easily within {self:possessive} reach, causes"
                        + " {self:subject} to involuntarily switch to autopilot."
                        + " {self:SUBJECT} simply {self:action:NEED|NEEDS} that ass.</b>",
                        self, other))
                return anilingus
            }
        }
        return def
    }

    fun doAction(self: Character, target: Character, action: Skill?): Boolean {
        val skill = checkWorship(self, target, action)
        val results = resolveSkill(skill, target)
        this.write("<br/>")
        updateMessage()
        return results
    }

    fun act(c: Character, action: Skill?) {
        if (c === p1.character) {
            p1act = action
        }
        if (c === p2.character) {
            p2act = action
        }
    }

    private fun doStanceTick(self: Character) {
        val other = getStance().getPartner(this, self)
        val add = other.getAddiction(AddictionType.DOMINANCE) //FIXME: Causes trigger even though addiction has 0 magnitude.
        if (add != null && add.atLeast(Addiction.Severity.MED) && !add.wasCausedBy(self)) {
            write(self, Global.format("{self:name} does {self:possessive} best to be dominant, but with the "
                    + "way " + add.cause.getName() + " has been working {self:direct-object} over {self:pronoun-action:are} completely desensitized.", self, other))
            return
        }
        if (self.has(Trait.smqueen)) {
            write(self,
                    Global.format("{self:NAME-POSSESSIVE} cold gaze in {self:possessive} dominant position"
                            + " makes {other:direct-object} shiver.",
                            self, other))
        } else if (getStance().time % 2 == 0 && getStance().time > 0) {
            if (other.has(Trait.indomitable)) {
                write(self, Global.format("{other:SUBJECT}, typically being the dominant one,"
                        + " {other:action:are|is} simply refusing to acknowledge {self:name-possessive}"
                        + " current dominance.", self, other))
            } else {
                write(self, Global.format("{other:NAME-POSSESSIVE} compromising position takes a toll on {other:possessive} willpower.",
                        self, other))
            }
        }
        if (self.has(Trait.confidentdom) && Global.random(2) == 0) {
            val attr: Attribute
            val desc: String
            if (self[Attribute.Ki] > 0 && Global.random(2) == 0) {
                attr = Attribute.Ki
                desc = "strengthening {self:possessive} focus on martial discipline"
            } else if (Global.random(2) == 0) {
                attr = Attribute.Power
                desc = "further empowering {self:possessive} muscles"
            } else {
                attr = Attribute.Cunning
                desc = "granting {self:direct-object} increased mental clarity"
            }
            write(self, Global.format("{self:SUBJECT-ACTION:feel|feels} right at home atop"
                    + " {other:name-do}, %s.", self, other, desc))
            self.add(this, Abuff(self, attr, Global.random(3) + 1, 10))
        }
        if (self.has(Trait.unquestionable) && Global.random(4) == 0) {
            write(self, Global.format("<b><i>\"Stay still, worm!\"</i> {self:subject-action:speak|speaks}"
                    + " with such force that it casues {other:name-do} to temporarily"
                    + " cease resisting.</b>", self, other))
            other.add(this, Flatfooted(other, 1, false))
        }
        val compulsion = Compulsive.describe(this, self, Situation.STANCE_FLIP)
        if (compulsion.isPresent && Global.random(10) < 3 && Reversal(other).usable(this, self)) {
            self.pain(this, null, Global.random(20, 50))
            val nw = stance.reverse(this, false)
            stance = if (stance != nw) {
                nw
            } else {
                Pin(other, self)
            }
            write(self, compulsion.get())
            Compulsive.doPostCompulsion(this, self, Situation.STANCE_FLIP)
        }
    }

    private fun checkCounter(attacker: Character, target: Character, skill: Skill?): Boolean {
        return !target.has(Trait.submissive) && getStance().mobile(target) && target.counterChance(this, attacker, skill) > Global.random(100)
    }

    private fun resolveCrossCounter(skill: Skill?, target: Character, chance: Int): Boolean {
        if (target.has(Trait.CrossCounter) && Global.random(100) < chance) {
            if (!target.human()) {
                write(target, Global.format("As {other:SUBJECT-ACTION:move|moves} to counter, {self:subject-action:seem|seems} to disappear from {other:possessive} line of sight. "
                        + "A split second later, {other:pronoun-action:are|is} lying on the ground with a grinning {self:name-do} standing over {other:direct-object}. "
                        + "How did {self:pronoun} do that!?", skill!!.user(), target))
            } else {
                write(target, Global.format("As {other:subject} moves to counter your assault, you press {other:possessive} arms down with your weight and leverage {other:possessive} "
                        + "forward motion to trip {other:direct-object}, sending the poor {other:girl} crashing onto the floor.", skill!!.user(), target))
            }
            skill.user().add(this, Falling(skill.user()))
            return true
        }
        return false
    }

    fun resolveSkill(skill: Skill?, target: Character): Boolean {
        var orgasmed = false
        var madeContact = false
        if (Skill.isUsableOn(this, skill, target)) {
            val success: Boolean
            if (!target.human() || !target.`is`(Stsflag.blinded)) {
                write(skill!!.user()
                        .subjectAction("use ", "uses ") + skill.getLabel(this) + ".")
            }
            if (skill!!.makesContact(this) && !getStance().dom(target) && target.canAct()
                    && checkCounter(skill.user(), target, skill)) {
                write("Countered!")
                if (!resolveCrossCounter(skill, target, 25)) {
                    target.counterattack(skill.user(), skill.type(this), this)
                }
                madeContact = true
                success = false
            } else if (target.`is`(Stsflag.counter) && skill.makesContact(this)) {
                write("Countered!")
                if (!resolveCrossCounter(skill, target, 50)) {
                    val s = target.getStatus(Stsflag.counter) as CounterStatus
                    if (skill.user()
                                    .`is`(Stsflag.wary)) {
                        write(target, s.counterSkill
                                .getBlockedString(this, skill.user()))
                    } else {
                        s.resolveSkill(this, skill.user())
                    }
                }
                madeContact = true
                success = false
            } else {
                success = Skill.resolve(skill, this, target)
                madeContact = madeContact or (success && skill.makesContact(this))
            }
            if (success) {
                if (skill.getTags(this).contains(SkillTag.thrusting) && skill.user().has(Trait.Jackhammer) && Global.random(2) == 0) {
                    write(skill.user(), Global.format("{self:NAME-POSSESSIVE} hips don't stop as {self:pronoun-action:continue|continues} to fuck {other:direct-object}.", skill.user(), target))
                    Skill.resolve(WildThrust(skill.user()), this, target)
                }
                if (skill.getTags(this).contains(SkillTag.thrusting) && skill.user().has(Trait.Piledriver) && Global.random(3) == 0) {
                    write(skill.user(), Global.format("{self:SUBJECT-ACTION:fuck|fucks} {other:name-do} <b>hard</b>, so much so that {other:pronoun-action:are|is} momentarily floored by the stimulation.", skill.user(), target))
                    target.add(this, Stunned(target, 1, false))
                }
                if (skill.type(this) == Tactics.damage) {
                    checkAndDoPainCompulsion(skill.user())
                }
            }
            if (skill.type(this) == Tactics.damage) {
                checkAndDoPainCompulsion(skill.user())
            }
            if (madeContact) {
                resolveContactBonuses(skill.user(), target)
                resolveContactBonuses(target, skill.user())
            }
            checkStamina(target)
            checkStamina(skill.user())
            orgasmed = checkOrgasm(skill.user(), target, skill)
            lastFailed = false
        } else {
            write(skill!!.user()
                    .possessiveAdjective() + " " + skill.getLabel(this) + " failed.")
            lastFailed = true
        }
        return orgasmed
    }

    private fun checkAndDoPainCompulsion(self: Character) {
        val compulsion = Compulsive.describe(this, self, Situation.PUNISH_PAIN)
        if (compulsion.isPresent) {
            self.pain(this, null, Global.random(10, 40))
            write(compulsion.get())
            Compulsive.doPostCompulsion(this, self, Situation.PUNISH_PAIN)
        }
    }

    private fun resolveContactBonuses(contacted: Character, contacter: Character) {
        if (contacted.has(Trait.VolatileSubstrate) && contacted.has(Trait.slime)) {
            contacter.add(this, Slimed(contacter, contacted, 1))
        }
    }

    private fun checkOrgasm(user: Character, target: Character, skill: Skill?): Boolean {
        return target.orgasmed || user.orgasmed
    }

    fun write(text: String?) = if (text != null) broadcastMessageRaw("<br/>$text") else null

    fun updateMessage() {
        Global.gui().refresh()
    }

    fun updateAndClearMessage() {
        Global.gui().clearText()
        updateMessage()
    }

    fun write(user: Character?, text: String?) {
        broadcastMessageRaw(Global.colorizeMessage(user, Global.capitalizeFirstLetter(text)))
    }

    private fun broadcastMessageRaw(text: String) {
        if (text.isEmpty()) {
            return
        }
        (setOf(p1.character, p2.character) + watchers).forEach { watcher -> watcher.message(text) }
        wroteMessage = true
    }

    fun checkStamina(p: Character) {
        if (p.stamina.isAtUnfavorableExtreme && !p.`is`(Stsflag.stunned)) {
            p.add(this, Winded(p, 3))
            if (p.isPet()) {
                // pets don't get stance changes
                return
            }
            val other: Character = if (p === p1.character) {
                p2.character
            } else {
                p1.character
            }
            if (!getStance().prone(p)) {
                if (!getStance().mobile(p) && getStance().dom(other)) {
                    if (p.human()) {
                        write(p, "Your legs give out, but " + other.getName() + " holds you up.")
                    } else {
                        write(p, String.format("%s slumps in %s arms, but %s %s %s to keep %s from collapsing.",
                                p.subject(), other.nameOrPossessivePronoun(),
                                other.pronoun(), other.action("support"), p.objectPronoun(),
                                p.objectPronoun()))
                    }
                } else if (getStance().havingSex(this, p) && getStance().dom(p) && getStance().reversable(this)) {
                    write(getOpponentCharacter(p), Global.format("{other:SUBJECT-ACTION:take|takes} the chance to shift into a more dominant position.", p, getOpponentCharacter(p)))
                    setStance(getStance().reverse(this, false))
                } else {
                    if (stance.havingSex(this)) {
                        setStance(stance.reverse(this, true))
                    } else {
                        if (p.human()) {
                            write(p, "You don't have the strength to stay on your feet. You slump to the floor.")
                        } else {
                            write(p, p.getName() + " drops to the floor, exhausted.")
                        }
                        setStance(StandingOver(other, p), null, false)
                    }
                }
                p.loseWillpower(this, min(p.willpower
                        .max()
                        / 8, 15), true)
            }
            if (other.has(Trait.dominatrix)) {
                if (p.hasAddiction(AddictionType.DOMINANCE)) {
                    write(other, String.format("Being dominated by %s again reinforces %s"
                            + " submissiveness towards %s.", other.getName(), p.nameOrPossessivePronoun(),
                            other.objectPronoun()))
                } else {
                    write(other, Global.format("There's something about the way {other:subject-action:know} just"
                            + " how and where to hurt {self:name-do} which some part of {self:possessive}"
                            + " psyche finds strangely appealing. {self:SUBJECT-ACTION:find} {self:reflective}"
                            + " wanting more.", p, other))
                }
                p.addict(this, AddictionType.DOMINANCE, other, Addiction.HIGH_INCREASE)
            }
        }
    }

    private operator fun next(): Boolean {
        assert(phase !is EndedPhase)
        if (shouldAutoresolve()) {
            return true
        }
        return if (!(wroteMessage || phase is StartPhase)
                || !isBeingObserved || Global.checkFlag(Flag.AutoNext) && phase is FastSkippablePhase) {
            false
        } else {
            if (!paused) {
                p1.character.nextCombat(this)
                p2.character.nextCombat(this)
                // This is a horrible hack to catch the case where the player is watching or
                // has intervened in the combat
                if (!(p1.character.human() || p2.character.human()) && isBeingObserved) {
                    Global.getPlayer().nextCombat(this)
                }
            }
            true
        }
    }

    private fun autoresolve() {
        assert(!p1.character.human() && !p2.character.human() && !isBeingObserved)
        assert(timer == 0)
        while (timer < NPC_TURN_LIMIT && winner == null) {
            turn()
        }
        if (timer < NPC_TURN_LIMIT) {
            val fitness1 = p1.character.getFitness(this).toDouble()
            val fitness2 = p2.character.getFitness(this).toDouble()
            val diff = abs(fitness1 / fitness2 - 1.0)
            if (diff > NPC_DRAW_ERROR_MARGIN) {
                victory(if (fitness1 > fitness2) p1 else p2)
            } else {
                draw()
            }
        }
        phase = EndedPhase()
        phase.next(this)
    }

    fun intrude(intruder: Participant, assist: Participant) {
        val target: Combatant = if (p1.participant === assist) {
            p2
        } else {
            p1
        }
        val targetCharacter = target.character
        val assistCharacter = assist.character
        val intruderCharacter = intruder.character
        if (targetCharacter.resist3p(this, intruderCharacter, assistCharacter)) {
            targetCharacter.gainXP(20 + targetCharacter.lvlBonus(intruderCharacter))
            targetCharacter.orgasm()
            targetCharacter.undress(this)
            intruderCharacter.gainXP(10 + intruderCharacter.lvlBonus(targetCharacter))
            target.participant.invalidateAttacker(intruder)
        } else {
            intruderCharacter.gainXP(intruderCharacter.getAssistXP(targetCharacter))
            intruder.dialog.intrudeInCombat(this, targetCharacter, assistCharacter)
            targetCharacter.gainXP(targetCharacter.getDefeatXP(assistCharacter))
            targetCharacter.orgasm()
            targetCharacter.undress(this)
            target.participant.invalidateAttacker(assist)
            target.participant.invalidateAttacker(intruder)
            targetCharacter.gainAttraction(assistCharacter, 1)
            assistCharacter.gainAttraction(intruderCharacter, 1)
            assistCharacter.gainXP(assistCharacter.getVictoryXP(targetCharacter))
            assistCharacter.dress(this)
            assistCharacter.gainTrophy(this, targetCharacter)
            assist.dialog.assistedByIntruder(this, targetCharacter, intruderCharacter)
            assistCharacter.gainAttraction(targetCharacter, 1)
            assist.incrementScore(assist.pointsForVictory(target.participant), "for an unearned win")
        }
        phase = ResultsScenePhase()
        if (!(p1.character.human() || p2.character.human() || intruderCharacter.human())) {
            end()
        } else {
            resumeNoClearFlag()
        }
    }

    private fun end() {
        p1.participant.state = Ready()
        p2.participant.state = Ready()
        if (processedEnding) {
            if (isBeingObserved) {
                Global.gui().endCombat()
            }
            return
        }
        var hasScene = false
        if (p1.character.human() || p2.character.human()) {
            if (postCombatScenesSeen < 3) {
                if (!p2.character.human() && p2.character is NPC) {
                    hasScene = doPostCombatScenes(p2.character as NPC)
                } else if (!p1.character.human() && p1.character is NPC) {
                    hasScene = doPostCombatScenes(p1.character as NPC)
                }
                if (hasScene) {
                    postCombatScenesSeen += 1
                    return
                }
            } else {
                p1.character.nextCombat(this)
                p2.character.nextCombat(this)
                // This is a horrible hack to catch the case where the player is watching or
                // has intervened in the combat
                if (!(p1.character.human() || p2.character.human()) && isBeingObserved) {
                    Global.getPlayer().nextCombat(this)
                }
            }
        }
        processedEnding = true
        p1.character.endofbattle(this)
        p2.character.endofbattle(this)
        getCombatantData(p1.character)!!.removedItems.forEach { item -> p1.character.gain(item) }
        getCombatantData(p2.character)!!.removedItems.forEach { item -> p2.character.gain(item) }
        location.endEncounter()
        // it's a little ugly, but we must be mindful of lazy evaluation
        var ding = p1.character.levelUpIfPossible(this) && p1.character.human()
        ding = p2.character.levelUpIfPossible(this) && p2.character.human() || ding
        if (doExtendedLog()) {
            log!!.logEnd(winner?.character)
        }
        if (!ding && isBeingObserved) {
            Global.gui().endCombat()
        }
    }

    private fun doPostCombatScenes(npc: NPC): Boolean {
        val availableScenes = npc.postCombatScenes
                .filter { scene: CombatScene -> scene.meetsRequirements(this, npc) }
        val possibleScene = Global.pickRandom(availableScenes)
        return if (possibleScene.isPresent) {
            Global.gui().clearText()
            possibleScene.get().visit(this, npc)
            true
        } else {
            false
        }
    }

    fun petbattle(one: Assistant, two: Assistant) {
        var roll1 = Global.random(20) + one.character.getSelf().power()
        var roll2 = Global.random(20) + two.character.getSelf().power()
        if (one.character.hasPussy() && two.character.hasDick()) {
            roll1 += 3
        } else if (one.character.hasDick() && two.character.hasPussy()) {
            roll2 += 3
        }
        if (roll1 > roll2) {
            one.vanquish(this, two)
        } else if (roll2 > roll1) {
            two.vanquish(this, one)
        } else {
            write(one.character.getName() + " and " + two.character.getName()
                    + " engage each other for awhile, but neither can gain the upper hand.")
        }
    }

    public override fun clone(): Combat {
        val c = super.clone() as Combat
        c.p1 = p1.copy()
        c.p2 = p2.copy()
        c.p1.character.finishClone(c.p2.character)
        c.p2.character.finishClone(c.p1.character)
        c.combatantData = HashMap()
        combatantData.forEach { (name: String, data: CombatantData) -> c.combatantData[name] = data.clone() as CombatantData }
        c.stance = getStance().clone()
        c.state = state
        if (c.getStance().top === p1.character) {
            c.getStance().top = c.p1.character
        }
        if (c.getStance().top === p2.character) {
            c.getStance().top = c.p2.character
        }
        if (c.getStance().bottom === p1.character) {
            c.getStance().bottom = c.p1.character
        }
        if (c.getStance().bottom === p2.character) {
            c.getStance().bottom = c.p2.character
        }
        c.petCombatants = petCombatants
                .map { a: Assistant ->
                    when (val oldMaster = a.character.getSelf().owner) {
                        c.p1.character -> {
                            return@map a.copy(c.p1.character)
                        }
                        c.p2.character -> {
                            return@map a.copy(c.p2.character)
                        }
                        else -> {
                            throw RuntimeException(String.format(
                                    "unable to find copy of master: (oldmaster:%s p1:%s p2:%s",
                                    oldMaster.trueName,
                                    p1.character.trueName,
                                    p2.character.trueName))
                        }
                    }
                }.toMutableList()
        c.getStance().setOtherCombatants(c.petCombatants.map { it.character })
        c.postCombatScenesSeen = postCombatScenesSeen
        c.cloned = true
        return c
    }

    fun lastact(user: Character): Skill? {
        return if (user === p1.character) {
            p1act
        } else if (user === p2.character) {
            p2act
        } else {
            null
        }
    }

    fun offerImage(path: String) {
        imagePath = path
        if (imagePath.isNotEmpty() && !cloned) {
            watchers.forEach { watcher -> watcher.notifyStanceImage(imagePath) }
        }
    }

    fun getStance(): Position {
        return stance
    }

    fun checkStanceStatus(c: Character, oldStance: Position, newStance: Position) {
        if (oldStance.sub(c) && !newStance.sub(c)) {
            if ((oldStance.prone(c) || !oldStance.mobile(c)) && !newStance.prone(c) && newStance.mobile(c)) {
                c.add(this, Braced(c))
                c.add(this, Wary(c, 3))
            } else if (!oldStance.mobile(c) && newStance.mobile(c)) {
                c.add(this, Wary(c, 3))
            }
        }
    }

    fun setStance(newStance: Position) {
        setStance(newStance, null, true)
    }

    private fun doEndPenetration(self: Character, partner: Character) {
        val parts1 = stance.getPartsFor(this, self, partner)
        val parts2 = stance.getPartsFor(this, partner, self)
        parts1.forEach { myPart ->
            parts2.forEach { othersPart ->
                myPart.onEndPenetration(this, self, partner, othersPart)
                othersPart.onEndPenetration(this, self, partner, myPart)
            }
        }
    }

    private fun doStartPenetration(stance: Position, self: Character, partner: Character) {
        val parts1 = stance.getPartsFor(this, self, partner)
        val parts2 = stance.getPartsFor(this, partner, self)
        parts1.forEach { myPart ->
            parts2.forEach { othersPart ->
                myPart.onStartPenetration(this, self, partner, othersPart)
                othersPart.onStartPenetration(this, self, partner, myPart)
            }
        }
    }

    fun setStanceRaw(stance: Position) {
        this.stance = stance
    }

    fun setStance(intendedNewStance: Position, initiator: Character?, voluntary: Boolean) {
        var newStance = intendedNewStance
        if (newStance.top.isPet() && newStance.bottom.isPet() || (newStance.top.isPet() || newStance.bottom.isPet()) && getStance().en != Stance.neutral && !newStance.isThreesome) {
            // Pets don't get into stances with each other, and they don't usurp stances.
            // Threesomes are exceptions to this.
            return
        }
        if (newStance.top !== getStance().bottom && newStance.top !== getStance().top || newStance.bottom !== getStance().bottom && newStance.bottom !== getStance().top) {
            if (initiator != null && initiator.isPet() && newStance.top === initiator) {
                val threesomeSkill = PetInitiatedThreesome(initiator)
                if (newStance.havingSex(this)) {
                    threesomeSkill.resolve(this, newStance.bottom)
                } else if (!getStance().sub(newStance.bottom)) {
                    write(initiator, Global.format("{self:SUBJECT-ACTION:take|takes} the chance to send {other:name-do} sprawling to the ground", initiator, newStance.bottom))
                    newStance.bottom.add(this, Falling(newStance.bottom))
                }
            }
            return
        }
        if (initiator != null) {
            val otherCharacter = getOpponentCharacter(initiator)
            if (voluntary && newStance.en == Stance.neutral && getStance().en != Stance.kneeling && otherCharacter.has(Trait.genuflection)
                    && rollWorship(initiator, otherCharacter)) {
                write(initiator, Global.format("While trying to get back up, {self:name-possessive} eyes accidentally met {other:name-possessive} gaze. "
                        + "Like a deer in headlights, {self:possessive} body involuntarily stops moving and kneels down before {other:direct-object}.", initiator, otherCharacter))
                newStance = Kneeling(otherCharacter, initiator)
            }
        }
        checkStanceStatus(p1.character, stance, newStance)
        checkStanceStatus(p2.character, stance, newStance)
        if (stance.inserted() && !newStance.inserted()) {
            doEndPenetration(p1.character, p2.character)
            val threePCharacter = stance.domSexCharacter(this)
            if (threePCharacter !== p1.character && threePCharacter !== p2.character) {
                doEndPenetration(p1.character, threePCharacter)
                doEndPenetration(p2.character, threePCharacter)
                getCombatantData(threePCharacter)!!.setIntegerFlag("ChoseToFuck", 0)
            }
            getCombatantData(p1.character)!!.setIntegerFlag("ChoseToFuck", 0)
            getCombatantData(p2.character)!!.setIntegerFlag("ChoseToFuck", 0)
        } else if (!stance.inserted() && newStance.inserted() && (newStance.penetrated(this, p1.character) || newStance.penetrated(this, p2.character))) {
            doStartPenetration(newStance, p1.character, p2.character)
        } else if (!stance.havingSex(this) && newStance.havingSex(this)) {
            val threePCharacter = stance.domSexCharacter(this)
            if (threePCharacter !== p1.character && threePCharacter !== p2.character) {
                doStartPenetration(newStance, p1.character, threePCharacter)
                doStartPenetration(newStance, p2.character, threePCharacter)
            }
            if (voluntary) {
                if (initiator != null) {
                    getCombatantData(initiator)!!.setIntegerFlag("ChoseToFuck", 1)
                    getCombatantData(getOpponentCharacter(initiator))!!.setIntegerFlag("ChoseToFuck", -1)
                }
            }
            checkBreeder(p1.character, voluntary)
            checkBreeder(p2.character, voluntary)
        }
        if (stance !== newStance && initiator != null && initiator.has(Trait.Catwalk)) {
            write(initiator, Global.format("The way {self:subject-action:move|moves} exudes such feline grace that it demands {other:name-possessive} attention.",
                    initiator, getOpponentCharacter(initiator)))
            initiator.add(this, Alluring(initiator, 1))
        }
        stance = newStance
        offerImage(stance.image())
    }

    /**Checks if the opponent has breeder - currently presumes Kat is the only character with it and outputs text.
     *
     * FIXME: this is currently hardcoded and needs to be moved elsewhere. The text and activation for this trait needs to be sent into the traint itself. */
    private fun checkBreeder(checked: Character, voluntary: Boolean) {
        val opp = getStance().getPartner(this, checked)
        if (checked.checkAddiction(AddictionType.BREEDER, opp) && getStance().inserted(checked)) {
            if (voluntary) {
                write(checked, "As you enter Kat, instinct immediately kicks in. It just"
                        + " feels so right, like this is what you're supposed"
                        + " to be doing all the time.")
                checked.addict(this, AddictionType.BREEDER, opp, Addiction.MED_INCREASE)
            } else {
                write(checked, "Something shifts inside of you as Kat fills herself with"
                        + " you. A haze descends over your mind, clouding all but a desire"
                        + " to fuck her as hard as you can.")
                checked.addict(this, AddictionType.BREEDER, opp, Addiction.LOW_INCREASE)
            }
        }
    }

    fun getOpponent(self: Character): Combatant {
        if (self == p1.character || self.isPetOf(p1.character)) {
            return p2
        }
        if (self == p2.character || self.isPetOf(p2.character)) {
            return p1
        }
        throw RuntimeException(String.format("no opponent found for %s", self))
    }

    fun getOpponentCharacter(self: Character): Character {
        return getOpponent(self).character
    }

    fun getOpponentAssistants(self: Character): Set<Assistant> {
        return petCombatants
                .filter { assistant -> assistant.character.getSelf().owner != self }.toSet()
    }

    fun writeSystemMessage(battleString: String, basic: Boolean) {
        if (Global.checkFlag(Flag.systemMessages) || basic
                && Global.checkFlag(Flag.basicSystemMessages)) {
            write(battleString)
        }
    }

    fun writeSystemMessage(character: Character?, string: String?) {
        if (Global.checkFlag(Flag.systemMessages)) {
            write(character, string)
        }
    }

    private fun doExtendedLog(): Boolean {
        return (p1.character.human() || p2.character.human()) && Global.checkFlag(Flag.extendedLogs)
    }

    val isBeingObserved: Boolean
        get() = watchers.isNotEmpty()

    fun addWatcher(c: Character) {
        watchers.add(c)
    }

    fun shouldPrintReceive(ch: Character?, c: Combat): Boolean {
        return isBeingObserved || c.p1.character.human() || c.p2.character.human()
    }

    fun shouldAutoresolve(): Boolean {
        return !(p1.character.human() || p2.character.human()) && !isBeingObserved
    }

    fun bothDirectObject(target: Character): String {
        return if (target.human()) "you" else "them"
    }

    fun bothPossessive(target: Character): String {
        return if (target.human()) "your" else "their"
    }

    fun bothSubject(target: Character): String {
        return if (target.human()) "you" else "they"
    }

    fun assistantsOf(target: Character): Set<Assistant> {
        return petCombatants.filter { a: Assistant -> a.character.getSelf().owner == target }.toSet()
    }

    fun removePet(self: PetCharacter) {
        if (self.has(Trait.resurrection) && !getCombatantData(self)!!.getBooleanFlag(FLAG_RESURRECTED)) {
            write(self, "Just as " + self.subject() + " was about to disappear, a dazzling light covers "
                    + self.possessiveAdjective() + " body. When the light fades, " + self.pronoun() + " looks completely refreshed!")
            getCombatantData(self)!!.setBooleanFlag(FLAG_RESURRECTED, true)
            self.stamina.renew()
            self.arousal.renew()
            self.mojo.renew()
            self.willpower.renew()
            return
        }
        getCombatantData(self)!!.setBooleanFlag(FLAG_RESURRECTED, false)
        petCombatants.removeIf { a: Assistant -> a.character == self }
    }

    fun addPet(master: Character, self: PetCharacter?) {
        if (self == null) {
            System.err.println("Something fucked up happened")
            Thread.dumpStack()
            return
        }
        if (petCombatants.any { a: Assistant -> a.character == self }) {
            write(String.format("<b>ERROR: Tried to add %s as a pet for %s,"
                    + " but there is already a %s who is a pet for %s."
                    + " Please report this as a bug. The extra pet will not"
                    + " be added, and you can probably continue playing without"
                    + " problems.</b>", self.trueName, master.trueName,
                    self.trueName, self.getSelf().owner().trueName))
            Thread.dumpStack()
            return
        }
        if (master.has(Trait.leadership)) {
            val levelups = max(5, master.progression.level / 4)
            self.getSelf().power = self.getSelf().power + levelups
            for (i in 0 until levelups) {
                self.ding(this)
            }
        }
        if (master.has(Trait.tactician)) {
            self.getSelf().ac = self.getSelf().ac + 3
            self.arousal.setMax((self.arousal.max().toFloat() * 1.5f).toInt())
            self.stamina.setMax((self.stamina.max().toFloat() * 1.5f).toInt())
        }
        self.stamina.renew()
        self.arousal.renew()
        writeSystemMessage(self, Global.format("{self:SUBJECT-ACTION:have|has} summoned {other:name-do} (Level %s)",
                master, self, self.progression.level))
        petCombatants.add(Assistant(self))
        this.write(self, self.challenge(getOpponentCharacter(self)))
    }

    fun getPetCombatants(): List<PetCharacter> = petCombatants.map { it.character }

    val isEnded: Boolean
        get() = phase is FinishedScenePhase || phase is EndedPhase

    fun pause() {
        paused = true
    }

    /**Collects any substances gained in this victory into an empty bottle.
     *
     * TODO: Mark this for combat rebuild - this goes to one of the final phases on combat end.
     */
    fun doBottleCollection(victor: Character, loser: Character) {
        if (loser.hasDick() && victor.has(Trait.succubus)) {
            victor.gain(Item.semen, 3)
            if (loser.human()) {
                write(victor, "<br/><b>As she leaves, you see all your scattered semen ooze out and gather into a orb in "
                        + victor.nameOrPossessivePronoun() + " hands. "
                        + "She casually drops your seed in some empty vials that appeared out of nowhere</b>")
            } else if (victor.human()) {
                write(victor, "<br/><b>" + loser.nameOrPossessivePronoun()
                        + " scattered semen lazily oozes into a few magically conjured flasks. "
                        + "To speed up the process, you milk " + loser.possessiveAdjective()
                        + " out of the last drops " + loser.subject()
                        + " had to offer. Yum, you just got some leftovers.</b>")
            }
        } else if (loser.hasDick() && (victor.human() || victor.has(Trait.madscientist)) && victor.has(Item.EmptyBottle, 1)) {
            write(victor, Global.format("<br/><b>{self:SUBJECT-ACTION:manage|manages} to collect some of {other:name-possessive} scattered semen in an empty bottle</b>", victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.semen, 1)
        }
        if (checkBottleCollection(victor, loser, DivineMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:name-possessive} divine pussy juices in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.HolyWater, 1)
        }
        if (checkBottleCollection(victor, loser, DemonicMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:name-possessive} demonic pussy juices in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.ExtremeAphrodisiac, 1)
        }
        if (checkBottleCollection(victor, loser, PlantMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:possessive} nectar in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.nectar, 3)
        }
        if (checkBottleCollection(victor, loser, CyberneticMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:possessive} artificial lubricant in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.LubricatingOils, 1)
        }
        if (checkBottleCollection(victor, loser, ArcaneMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of the floating mana wisps ejected from {other:possessive} orgasm in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.RawAether, 1)
        }
        if (checkBottleCollection(victor, loser, FeralMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:possessive} musky juices in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.FeralMusk, 1)
        }
        if (checkBottleCollection(victor, loser, GooeyMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:possessive} goo in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.BioGel, 1)
        }
        if (checkBottleCollection(victor, loser, FieryMod.TYPE)) {
            write(victor, Global.format(
                    "<br/><b>{other:SUBJECT-ACTION:shoot|shoots} {self:name-do} a dirty look as {self:subject-action:move|moves} to collect some of {other:possessive} excitement in an empty bottle</b>",
                    victor, loser))
            victor.consume(Item.EmptyBottle, 1, false)
            victor.gain(Item.MoltenDrippings, 1)
        }
    }

    val p1Character: Character
        get() = p1.character
    val p2Character: Character
        get() = p2.character

    companion object {
        private const val NPC_TURN_LIMIT = 75
        private const val NPC_DRAW_ERROR_MARGIN = .15
        const val FLAG_RESURRECTED = "resurrected"
        var WORSHIP_SKILLS = listOf(BreastWorship(null), CockWorship(null), FootWorship(null),
                PussyWorship(null), Anilingus(null))
        const val TEMPT_WORSHIP_BONUS = "TEMPT_WORSHIP_BONUS"
    }
}
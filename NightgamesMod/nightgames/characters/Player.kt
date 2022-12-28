package nightgames.characters

import nightgames.areas.Area
import nightgames.characters.body.*
import nightgames.characters.body.mods.ExternalTentaclesMod
import nightgames.characters.body.mods.GooeySkinMod
import nightgames.characters.body.mods.catcher.GooeyMod
import nightgames.characters.body.mods.pitcher.SlimyCockMod
import nightgames.characters.corestats.ArousalStat
import nightgames.characters.corestats.StaminaStat
import nightgames.characters.corestats.WillpowerStat
import nightgames.combat.Combat
import nightgames.combat.CombatSceneChoice
import nightgames.daytime.*
import nightgames.global.Global
import nightgames.global.Scene
import nightgames.grammar.Person
import nightgames.grammar.SingularSecondPerson
import nightgames.gui.GUI
import nightgames.gui.commandpanel.CommandPanelOption
import nightgames.items.Item
import nightgames.items.Loot
import nightgames.match.*
import nightgames.match.ftc.FTCMatch
import nightgames.skills.Stage
import nightgames.skills.Tactics
import nightgames.skills.damage.DamageType
import nightgames.stance.Behind
import nightgames.stance.Neutral
import nightgames.start.PlayerConfiguration
import nightgames.status.PlayerSlimeDummy
import nightgames.status.Status
import nightgames.status.addiction.Addiction
import nightgames.trap.Trap
import java.util.*
import kotlin.math.min

class Player @JvmOverloads constructor(name: String?, @JvmField var gui: GUI, sex: CharacterSex = CharacterSex.male, config: PlayerConfiguration? = null,
                                       pickedTraits: List<Trait?> = ArrayList(),
                                       selectedAttributes: Map<Attribute, Int?> = mutableMapOf()) : Character(name!!, 1) {
    @JvmField
    var traitPoints = 0
    private var levelsToGain: Int
    private var skippedFeat = false

    // TODO(Ryplinn): This initialization pattern is very close to that of BasePersonality. I think it makes sense to make NPC the primary parent of characters instead of BasePersonality.
    init {
        initialGender = sex
        levelsToGain = 0
        applyBasicStats(this)
        setGrowth()
        body.makeGenitalOrgans(initialGender)
        if (config != null) { applyConfigStats(config) }
        finishCharacter(pickedTraits, selectedAttributes)
    }

    fun applyBasicStats(self: Character) {
        self.stamina.setMax(80)
        self.arousal.setMax(80)
        self.willpower.setMax(self.willpower.max())
        self.availableAttributePoints = 0
        self.trophy = Item.PlayerTrophy
    }

    private fun applyConfigStats(config: PlayerConfiguration) {
        config.apply(this)
    }

    private fun finishCharacter(pickedTraits: List<Trait?>, selectedAttributes: Map<Attribute, Int?>) {
        pickedTraits.forEach { t: Trait? -> addTraitDontSaveData(t) }
        att.putAll(selectedAttributes)
        change()
        body.finishBody(initialGender)
    }

    fun setGrowth() {
        setGrowth(newGrowth())
    }

    fun describeStatus(): String {
        val b = StringBuilder()
        if (gui.combat != null && (gui.combat.p1Character.human() || gui.combat.p2Character.human())) {
            body.describeBodyText(b, gui.combat.getOpponentCharacter(this), false)
        } else {
            body.describeBodyText(b, Global.getCharacterByType("Angel"), false)
        }
        if (getTraits().isNotEmpty()) {
            b.append("<br/>Traits:<br/>")
            val traits: MutableList<Trait> = getTraits().toMutableList()
            traits.removeIf { t: Trait -> t.isOverridden(this) }
            traits.sortBy { obj: Trait -> obj.toString() }
            b.append(traits.joinToString())
        }
        if (status.size > 0) {
            b.append("<br/><br/>Statuses:<br/>")
            val statuses: MutableList<Status> = status.toMutableList()
            statuses.sortBy { status: Status -> status.name }
            b.append(statuses.joinToString { it.name })
        }
        return b.toString()
    }

    override fun describe(per: Int, observer: Character): String {
        var description = "<i>"
        for (s in status) {
            description = description + s.describe(observer) + "<br/>"
        }
        description = "$description</i>"
        description += outfit.describe(this)
        if (per >= 5 && status.size > 0) {
            description += "<br/>List of statuses:<br/><i>"
            description += status.joinToString()
            description += "</i><br/>"
        }
        description += Stage.describe(this)
        return description
    }

    /**Overridden abstract method for determining if this character is human - meaning the player.
     * TODO: Recommend renaming to isHuman(), to make more meaningful name and easier to find. */
    override fun human(): Boolean {
        return true
    }

    override fun bbLiner(c: Combat?, target: Character?): String? {
        return null
    }

    override fun nakedLiner(c: Combat?, target: Character?): String? {
        return null
    }

    override fun stunLiner(c: Combat?, target: Character?): String? {
        return null
    }

    override fun taunt(c: Combat?, target: Character?): String? {
        return null
    }

    override fun displayStateMessage(knownTrap: Trap.Instance?) {
        if (Global.getMatch().type == MatchType.FTC) {
            val holder = (Global.getMatch() as FTCMatch).flagHolder
            if (holder != null && !holder.human()) {
                gui.message("<b>" + holder.subject() + " currently holds the Flag.</b></br>")
            }
        }
        gui.message(location.get().describe() + "<br/><br/>")
        knownTrap?.let { trap -> gui.message("You've set a " + trap.name + " here.") }
    }

    override fun endOfMatchRound() {
        super.endOfMatchRound()
        addictions.forEach { obj: Addiction -> obj.refreshWithdrawal() }
    }

    override fun ding(c: Combat?) {
        levelsToGain += 1
        if (levelsToGain == 1) {
            actuallyDing()
            if (cloned == 0) {
                c?.pause()
                handleLevelUp()
            }
        }
    }

    private fun handleLevelUp() {
        if (availableAttributePoints > 0) {
            gui.message(this, "$availableAttributePoints Attribute Points remain.</br>")
            val options = att.keys
                    .filter { a: Attribute? -> Attribute.isTrainable(this, a) && getPure(a) > 0 }
                    .map { a: Attribute -> optionToSelectAttribute(a) }.toMutableList()
            options.add(optionToSelectAttribute(Attribute.Willpower))
            gui.presentOptions(options)
            if (Global.getMatch() != null) {
                Global.getMatch().pause()
            }
        } else if (traitPoints > 0 && !skippedFeat) {
            gui.message(this, "You've earned a new perk. Select one below.</br>")
            val options = Global.getFeats(this)
                    .filter { t: Trait? -> !has(t!!) }
                    .map { t: Trait? -> optionToSelectTrait(t) }.toMutableList()
            options.add(optionToSelectTrait(null))
            gui.presentOptions(options)
        } else {
            skippedFeat = false
            Global.gainSkills(this)
            levelsToGain -= 1
            if (levelsToGain > 0) {
                actuallyDing()
                handleLevelUp()
            } else {
                if (gui.combat != null) {
                    gui.combat.resume()
                } else if (Global.getMatch() != null) {
                    Global.getMatch().resume()
                } else if (Global.day != null) {
                    Global.getDay().plan()
                } else {
                    MatchType.NORMAL.runPrematch()
                }
            }
        }
    }

    private fun optionToSelectAttribute(a: Attribute): CommandPanelOption {
        return CommandPanelOption(a.name) { increaseAttribute(a) }
    }

    private fun optionToSelectTrait(t: Trait?): CommandPanelOption {
        var o = CommandPanelOption("Skip",
                "Save the trait point for later."
        ) {
            skipFeat()
            handleLevelUp()
        }
        if (t != null) {
            o = CommandPanelOption(t.toString(), t.desc) { grantTrait(t) }
        }
        return o
    }

    private fun skipFeat() {
        skippedFeat = true
    }

    private fun increaseAttribute(attribute: Attribute) {
        if (availableAttributePoints < 1) {
            throw RuntimeException("attempted to increase attributes with no points remaining")
        }
        mod(attribute, 1)
        availableAttributePoints -= 1
        handleLevelUp()
    }

    override fun matchPrep(m: Match?) {
        super.matchPrep(m)
        gui.startMatch()
    }

    private fun grantTrait(trait: Trait) {
        if (traitPoints < 1) {
            throw RuntimeException("attempted to grant trait without trait points")
        }
        gui.message(this, "Gained feat: $trait")
        add(trait)
        Global.gainSkills(this)
        traitPoints -= 1
        handleLevelUp()
    }

    private fun actuallyDing() {
        progression.level = progression.level + 1
        getGrowth().levelUpCoreStatsOnly(this)
        availableAttributePoints += getGrowth().attributePointsForRank(progression.rank)
        gui.message(this, "You've gained a Level!<br/>Select which attributes to increase.")
        if (progression.level % 3 == 0 && progression.level < 10 || (progression.level + 1) % 2 == 0 && progression.level > 10) {
            traitPoints += 1
        }
    }

    override fun getMaxWillpowerPossible(): Int {
        return 50 + progression.level * 5 - get(Attribute.Submissive) * 2
    }

    override fun notifyTravel(dest: Area?, message: String?) {
        super.notifyTravel(dest, message)
        gui.message(message)
    }

    override fun gain(item: Item?) {
        gui.message("<b>You've gained " + item!!.pre() + item.getName() + ".</b>")
        super.gain(item)
    }

    override fun challenge(other: Character) = null

    override fun afterParty() {}
    override fun counterattack(target: Character?, type: Tactics?, c: Combat?) {
        when (type) {
            Tactics.damage -> {
                c!!.write(this, "You dodge " + target!!.getName()
                        + "'s slow attack and hit her sensitive tit to stagger her.")
                target.pain(c, target, 4 + min(Global.random(get(Attribute.Power)), 20))
            }

            Tactics.pleasure -> if (!target!!.crotchAvailable() || !target.hasPussy()) {
                c!!.write(this, "You pull " + target.getName()
                        + " off balance and lick her sensitive ear. She trembles as you nibble on her earlobe.")
                target.body.pleasure(this, body.getRandom("tongue"),
                        target.body.randomEars,
                        (
                                4 + min(Global.random(get(Attribute.Seduction)), 20)).toDouble(), c)
            } else {
                c!!.write(this, "You pull " + target.getName() + " to you and rub your thigh against her girl parts.")
                target.body.pleasure(this, body.randomFeet, target.body.randomPussy,
                        (
                                4 + min(Global.random(get(Attribute.Seduction)), 20)).toDouble(), c)
            }

            Tactics.fucking -> if (c!!.getStance()
                            .sub(this)) {
                val reverse = c.getStance()
                        .reverse(c, true)
                if (reverse !== c.getStance() && !BodyPart.hasOnlyType(reverse.bottomParts(), StraponPart.TYPE)) {
                    c.setStance(reverse, this, false)
                } else {
                    c.write(this, Global.format(
                            "{self:NAME-POSSESSIVE} quick wits find a gap in {other:name-possessive} hold and {self:action:slip|slips} away.",
                            this, target))
                    c.setStance(Neutral(this, c.getOpponentCharacter(this)), this, true)
                }
            } else {
                target!!.body.pleasure(this, body.randomHands, target.body.randomBreasts,
                        (
                                4 + min(Global.random(get(Attribute.Seduction)), 20)).toDouble(), c)
                c.write(this, Global.format("{self:SUBJECT-ACTION:pinch|pinches} {other:possessive} nipples with {self:possessive} hands as {other:subject-action:try|tries} to fuck {self:direct-object}. "
                        + "While {other:subject-action:yelp|yelps} with surprise, {self:subject-action:take|takes} the chance to pleasure {other:possessive} body.",
                        this, target))
            }

            Tactics.stripping -> {
                val clothes = target!!.stripRandom(c!!)
                if (clothes != null) {
                    c.write(this, "You manage to catch " + target.possessiveAdjective()
                            + " hands groping your clothing, and in a swift motion you strip off her "
                            + clothes.name + " instead.")
                } else {
                    c.write(this, "You manage to dodge " + target.possessiveAdjective()
                            + " groping hands and give a retaliating slap in return.")
                    target.pain(c, target, 4 + min(Global.random(get(Attribute.Power)), 20))
                }
            }

            Tactics.positioning -> if (c!!.getStance().dom(this)) {
                c.write(this, "You outmanuever " + target!!.getName() + " and you exhausted her from the struggle.")
                target.weaken(c, modifyDamage(DamageType.stance, target, 15.0).toInt())
            } else {
                c.write(this, target!!.getName()
                        + " loses her balance while grappling with you. Before she can fall to the floor, you catch her from behind and hold her up.")
                c.setStance(Behind(this, target))
            }

            else -> {
                c!!.write(this, "You manage to dodge " + target!!.possessiveAdjective()
                        + " attack and give a retaliating slap in return.")
                target.pain(c, target, 4 + min(Global.random(get(Attribute.Power)), 20))
            }
        }
    }

    override fun endOfCombatRound(c: Combat, opponent: Character) {
        super.endOfCombatRound(c, opponent)
        if (has(Trait.RawSexuality)) {
            c.write(this, Global.format("{self:NAME-POSSESSIVE} raw sexuality turns both of you on.", this, opponent))
            temptNoSkillNoSource(c, opponent, arousal.max() / 25)
            opponent.temptNoSkillNoSource(c, this, opponent.arousal.max() / 25)
        }
        if (has(Trait.slime)) {
            if (hasPussy() && !body.randomPussy.moddedPartCountsAs(GooeyMod.TYPE)) {
                body.randomPussy.addTemporaryMod(GooeyMod(), 999)
                c.write(this,
                        Global.format("{self:NAME-POSSESSIVE} %s turned back into a gooey pussy.",
                                this, opponent, body.randomPussy))
            }
            if (hasDick() && !body.randomCock.moddedPartCountsAs(SlimyCockMod.TYPE)) {
                body.randomCock.addTemporaryMod(SlimyCockMod(), 999)
                c.write(this,
                        Global.format("{self:NAME-POSSESSIVE} %s turned back into a gooey pussy.",
                                this, opponent, body.randomPussy))
            }
        }
    }

    override fun nameDirectObject(): String {
        return "you"
    }

    override fun add(t: Trait?): Boolean {
        if (t == Trait.nymphomania) {
            mod(Attribute.Nymphomania, 1)
        }
        return super.add(t)
    }

    override fun subjectAction(verb: String?, pluralverb: String): String {
        return subject() + " " + verb
    }

    override fun nameOrPossessivePronoun(): String {
        return "your"
    }

    override fun subjectWas(): String {
        return subject() + " were"
    }

    override fun emote(emo: Emotion?, amt: Int) {}
    override val portrait: String?
        get() = null

    override fun action(firstPerson: String?, thirdPerson: String?): String? {
        return firstPerson
    }

    override fun resist3p(c: Combat?, target: Character?, assist: Character?): Boolean {
        return has(Trait.cursed)
    }

    override fun resolveOrgasm(c: Combat, opponent: Character?, selfPart: BodyPart?, opponentPart: BodyPart?, times: Int,
                               totalTimes: Int) {
        super.resolveOrgasm(c, opponent, selfPart, opponentPart, times, totalTimes)
        if (has(Trait.slimification) && times == totalTimes && willpower.percent() < 60 && !has(Trait.slime)) {
            c.write(this, Global.format(
                    "A powerful shiver runs through your entire body. Oh boy, you know where this"
                            + " is headed... Sure enough, you look down to see your skin seemingly <i>melt</i>,"
                            + " turning a translucent blue. You legs fuse together and collapse into a puddle."
                            + " It only takes a few seconds for you to regain some semblance of control over"
                            + " your amorphous body, but you're not going to switch back to your human"
                            + " form before this fight is over...", this, opponent))
            nudify()
            purge(c)
            addTemporaryTrait(Trait.slime, 999)
            add(c, PlayerSlimeDummy(this))
            if (hasPussy() && !body.randomPussy.moddedPartCountsAs(GooeyMod.TYPE)) {
                body.randomPussy.addTemporaryMod(GooeyMod(), 999)
                body.randomPussy.addTemporaryMod(ExternalTentaclesMod(), 999)
            }
            if (hasDick() && !body.randomCock.moddedPartCountsAs(SlimyCockMod.TYPE)) {
                body.randomCock.addTemporaryMod(SlimyCockMod(), 999)
            }
            val part = body.randomBreasts
            if (part != null
                    && body.randomBreasts.size != BreastsPart.Size.min()) {
                part.temporarilyChangeSize(1, 999)
            }
            (body.skin as GenericBodyPart).addTemporaryMod(GooeySkinMod(), 999)
            body.temporaryAddPart(
                    TentaclePart("slime pseudopod", "back", "slime", 0.0, 1.0, 1.0), 999)
            addTemporaryTrait(Trait.Sneaky, 999)
            addTemporaryTrait(Trait.shameless, 999)
            addTemporaryTrait(Trait.lactating, 999)
            addTemporaryTrait(Trait.addictivefluids, 999)
            addTemporaryTrait(Trait.autonomousPussy, 999)
            addTemporaryTrait(Trait.enthrallingjuices, 999)
            addTemporaryTrait(Trait.energydrain, 999)
            addTemporaryTrait(Trait.desensitized, 999)
            addTemporaryTrait(Trait.steady, 999)
            addTemporaryTrait(Trait.strongwilled, 999)
        }
    }

    override fun exercise(source: Exercise?): Int {
        val gain = super.exercise(source)
        gui.clearText()
        val options = ArrayList<CommandPanelOption>()
        val o = CommandPanelOption("Next") { source!!.done(true); gui.clearText() }
        options.add(o)
        gui.presentOptions(options)
        source!!.showScene(source.pickScene(gain))
        if (gain > 0) {
            Global.gui().message("<b>Your maximum stamina has increased by $gain.</b>")
        }
        return gain
    }

    override fun porn(source: Porn?): Int {
        val gain = super.porn(source)
        gui.clearText()
        val options = ArrayList<CommandPanelOption>()
        val o = CommandPanelOption("Next") { source!!.done(true); gui.clearText() }
        options.add(o)
        gui.presentOptions(options)
        source!!.showScene(source.pickScene(gain))
        if (gain > 0) {
            Global.gui().message("<b>Your maximum arousal has increased by $gain.</b>")
        }
        return gain
    }

    override fun chooseLocateTarget(potentialTargets: Map<Character?, Runnable?>?, noneOption: Runnable?, msg: String?) {
        gui.clearText()
        gui.validate()
        gui.message(msg)
        val options = potentialTargets!!.entries.map { (key, value): Map.Entry<Character?, Runnable?> ->
                    CommandPanelOption(key!!.trueName) { value!!.run() }
        }.toMutableList()
        options.add(CommandPanelOption("Leave") { noneOption!!.run() })
        gui.presentOptions(options)
    }

    override fun leaveAction(callback: Runnable?) {
        val options = ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Leave") { callback!!.run() })
        gui.presentOptions(options)
    }

    override fun chooseShopOption(shop: Store, items: Collection<Loot?>?,
                                  additionalChoices: List<String?>?) {
        val options = items!!
                .map { item: Loot? ->
                    CommandPanelOption(Global.capitalizeFirstLetter(item!!.name), item.desc) { shop.visit(item.name) }
                }.toMutableList()
        options.addAll(additionalChoices!!.map { choice -> newActivitySubchoice(shop, choice) })
        gui.presentOptions(options)
    }

    override fun sceneNext(source: Scene?) {
        val options = ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Next") { source!!.respond("Next") })
        gui.presentOptions(options)
    }

    // displayTexts and prices are expected to be 1:1
    override fun chooseBodyShopOption(shop: BodyShop, displayTexts: List<String?>?,
                                      prices: List<Int?>?, additionalChoices: List<String?>?) {
        assert(displayTexts!!.size == prices!!.size)
        val options = mutableListOf<CommandPanelOption>()
        for (i in displayTexts.indices) {
            val displayText = displayTexts[i]
            options.add(CommandPanelOption(displayText, "Price: $" + prices[i]) { shop.visit(displayText) })
        }
        options.addAll(additionalChoices!!.map { choice -> newActivitySubchoice(shop, choice) })
        gui.presentOptions(options)
    }

    override fun nextCombat(c: Combat?) {
        Global.getMatch().pause()
        val options = ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Next") { c!!.resume() })
        gui.presentOptions(options)
    }

    fun chooseActivity(activities: List<Activity>) {
        gui.presentOptions(activities.map { activity: Activity ->
            CommandPanelOption(activity.toString()) { activity.visit("Start") }
        })
    }

    fun chooseCombatScene(c: Combat, npc: Character?, choices: List<CombatSceneChoice>) {
        gui.presentOptions(choices
            .map { choice: CombatSceneChoice ->
                CommandPanelOption(choice.choice) {
                    c.write("<br/>")
                    choice.choose(c, npc)
                    c.updateMessage()
                    nextCombat(c)
                }
            }
        )
    }

    override fun chooseActivitySubchoices(activity: Activity?, choices: List<String?>?) {
        gui.presentOptions(choices!!.map { choice -> newActivitySubchoice(activity, choice) })
    }

    override val grammar: Person
        get() = SingularSecondPerson(this)

    override fun notifyCombatStart(c: Combat?, opponent: Character?) {
        super.notifyCombatStart(c, opponent)
        assert(opponent is NPC) { opponent.toString() }
        c!!.addWatcher(this)
        gui.beginCombat(c, opponent as NPC?)
    }

    override fun message(message: String?) {
        gui.message("A message for the Player" + this.javaClass.toString())
        gui.message(message)
    }

    override fun makeIntelligence(): Intelligence {
        return HumanIntelligence(this)
    }

    override fun makeDialog(): Dialog {
        return object : Dialog {
            override fun intrudeInCombat(c: Combat, target: Character, assist: Character) {
                c.write("You take your time, approaching " + target.getName() + " and " + assist.getName() + " stealthily. "
                        + assist.getName() + " notices you first and before her reaction "
                        + "gives you away, you quickly lunge and grab " + target.getName()
                        + " from behind. She freezes in surprise for just a second, but that's all you need to "
                        + "restrain her arms and leave her completely helpless. Both your hands are occupied holding her, so you focus on kissing and licking the "
                        + "sensitive nape of her neck.<br/><br/>")
            }

            override fun assistedByIntruder(c: Combat, target: Character, assist: Character) {
                if (target.hasDick()) {
                    c.write(String.format(
                            "You position yourself between %s's legs, gently "
                                    + "forcing them open with your knees. %s dick stands erect, fully "
                                    + "exposed and ready for attention. You grip the needy member and "
                                    + "start jerking it with a practiced hand. %s moans softly, but seems"
                                    + " to be able to handle this level of stimulation. You need to turn "
                                    + "up the heat some more. Well, if you weren't prepared to suck a cock"
                                    + " or two, you may have joined the wrong competition. You take just "
                                    + "the glans into your mouth, attacking the most senstitive area with "
                                    + "your tongue. %s lets out a gasp and shudders. That's a more promising "
                                    + "reaction.<br/><br/>You continue your oral assault until you hear a breathy "
                                    + "moan, <i>\"I'm gonna cum!\"</i> You hastily remove %s dick out of "
                                    + "your mouth and pump it rapidly. %s shoots %s load into the air, barely "
                                    + "missing you.", target.getName(),
                            Global.capitalizeFirstLetter(target.possessiveAdjective()), target.getName(),
                            Global.capitalizeFirstLetter(target.pronoun()), target.possessiveAdjective(), target.getName(),
                            target.possessiveAdjective()))
                } else {
                    c.write(target.nameOrPossessivePronoun()
                            + " arms are firmly pinned, so she tries to kick you ineffectually. You catch her ankles and slowly begin kissing and licking your way "
                            + "up her legs while gently, but firmly, forcing them apart. By the time you reach her inner thighs, she's given up trying to resist. Since you no "
                            + "longer need to hold her legs, you can focus on her flooded pussy. You pump two fingers in and out of her while licking and sucking her clit. In no "
                            + "time at all, she's trembling and moaning in orgasm.")
                }
            }
        }
    }

    override fun notifyStanceImage(path: String?) {
        gui.displayImage(path)
    }

    companion object {
        private fun newGrowth(): Growth {
            val stamina = CoreStatGrowth<StaminaStat>(20f, 0f)
            val arousal = CoreStatGrowth<ArousalStat>(40f, 0f)
            val willpower = CoreStatGrowth<WillpowerStat>(0f, 0f)
            return Growth(CoreStatsGrowth(stamina, arousal, willpower))
        }

        private fun newActivitySubchoice(activity: Activity?, choice: String?): CommandPanelOption {
            return CommandPanelOption(choice) { activity!!.visit(choice) }
        }
    }
}
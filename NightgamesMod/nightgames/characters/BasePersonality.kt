package nightgames.characters

import nightgames.areas.Area
import nightgames.characters.body.BodyPart
import nightgames.characters.body.CockPart
import nightgames.characters.body.mods.pitcher.CockMod
import nightgames.characters.custom.AiModifiers
import nightgames.characters.custom.CharacterLine
import nightgames.characters.custom.CommentSituation
import nightgames.characters.custom.RecruitmentData
import nightgames.combat.Combat
import nightgames.combat.Result
import nightgames.global.Flag
import nightgames.global.Global
import nightgames.items.Item
import nightgames.match.Action
import nightgames.match.Dialog
import nightgames.pet.arms.ArmManager
import nightgames.skills.Skill
import nightgames.start.NpcConfiguration
import nightgames.status.Disguised
import nightgames.status.Stsflag
import nightgames.status.addiction.Addiction
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import java.io.Serializable
import java.util.*

abstract class BasePersonality protected constructor(name: String?, isStartCharacter: Boolean) : Serializable {
    private val type: String
    @JvmField
    var character: NPC
    @JvmField var preferredAttributes: java.util.ArrayList<PreferredAttribute>
    fun getPreferredAttributes() = preferredAttributes
    @JvmField
    protected var preferredCockMod: Optional<CockMod>
    protected var mods: AiModifiers? = null
    @JvmField
    var lines: MutableMap<String, MutableList<CharacterLine>>
    @JvmField
    protected var description: JtwigTemplate? = null
    protected var dominance = 0
    @JvmField
    protected var minDominance = 0

    init {
        // Make the built-in character
        type = javaClass.simpleName
        character = NPC(name, 1, this)
        character.isStartCharacter = isStartCharacter
        preferredCockMod = Optional.empty()
        preferredAttributes = ArrayList()
        lines = HashMap()
    }

    abstract fun setGrowth()
    open fun rest(time: Int) {
        if (preferredCockMod.isPresent && character.progression.rank > 0) {
            val optDick = Optional.ofNullable<BodyPart>(character.body.randomCock)
            if (optDick.isPresent) {
                val part = optDick.get() as CockPart
                part.addMod(preferredCockMod.get())
            }
        }
        for (addiction in character.addictions) {
            if (addiction.atLeast(Addiction.Severity.LOW)) {
                val cause = addiction.cause
                val affection = character.getAffection(cause)
                val affectionDelta = affection - character.getAffection(Global.getPlayer())
                // day 10, this would be (10 + sqrt(10) * 5) * .7 = 18 affection lead to max
                // day 60, this would be (10 + sqrt(70) * 5) * .7 = 36 affection lead to max
                val chanceToDoDaytime = .25 + addiction.getMagnitude() / 2 + Global.clamp(affectionDelta / (10 + Math.sqrt(Global.getDate().toDouble()) * 5), -.7, .7)
                if (Global.randomdouble() < chanceToDoDaytime) {
                    addiction.aggravate(null, Addiction.MED_INCREASE)
                    addiction.flagDaytime()
                    character.gainAffection(cause, 1)
                }
            }
        }
    }

    fun buyUpTo(item: Item, number: Int) {
        while (character.money > item.price && character.count(item) < number) {
            character.money -= item.price
            character.gain(item)
        }
    }

    open fun getType(): String? {
        return type
    }

    fun act(available: HashSet<Skill>, c: Combat?): Skill {
        val tactic: HashSet<Skill>
        val chosen: Skill?
        val priority = Decider.parseSkills(available, c, character)
        chosen = if (!Global.checkFlag(Flag.dumbmode)) {
            Decider.prioritizeNew(character, priority, c)
        } else {
            character.prioritize(priority)
        }
        return if (chosen == null) {
            tactic = available
            val actions = tactic.toTypedArray()
            actions[Global.random(actions.size)]
        } else {
            chosen
        }
    }

    open fun move(available: Collection<Action.Instance>, radar: Collection<Area>): Action.Instance? {
        return Decider.parseMoves(available, radar, character)
    }

    open fun image(): String? {
        return (character.trueName.lowercase(Locale.getDefault())
                + "/portraits/" + character.mood.name + ".jpg")
    }

    fun ding(self: Character) {
        self.getGrowth().levelUp(self)
        onLevelUp(self)
        self.distributePoints(preferredAttributes)
    }

    protected open fun onLevelUp(self: Character?) {
        // NOP
    }

    open val recruitmentData: RecruitmentData?
        get() = null
    open val aiModifiers: AiModifiers?
        get() {
            if (mods == null) resetAiModifiers()
            return mods
        }

    fun resetAiModifiers() {
        mods = AiModifiers.getDefaultModifiers(getType())
    }

    open fun resist3p(c: Combat?, target: Character?, assist: Character?): String? {
        return null
    }

    open fun getComments(c: Combat): Map<CommentSituation, String>? {
        val all = CommentSituation.getDefaultComments(getType())
        val applicable: MutableMap<CommentSituation, String> = HashMap()
        all.entries
                .stream()
                .filter { (key): Map.Entry<CommentSituation, String> ->
                    key
                            .isApplicable(c, character, c.getOpponentCharacter(character))
                }
                .forEach { (key, value): Map.Entry<CommentSituation, String> -> applicable[key] = value }
        return applicable
    }

    abstract fun victory(c: Combat, flag: Result): String?
    abstract fun defeat(c: Combat, flag: Result): String?
    abstract fun victory3p(c: Combat, target: Character, assist: Character?): String?
    abstract fun intervene3p(c: Combat, target: Character, assist: Character?): String?
    abstract fun draw(c: Combat, flag: Result): String?
    abstract fun fightFlight(opponent: Character): Boolean
    abstract fun attack(opponent: Character?): Boolean
    abstract fun fit(): Boolean
    abstract fun checkMood(c: Combat?, mood: Emotion?, value: Int): Boolean
    open fun resolveOrgasm(c: Combat?, self: NPC?, opponent: Character?, selfPart: BodyPart?,
                           opponentPart: BodyPart?, times: Int,
                           totalTimes: Int) {
        // no op
    }

    open fun eot(c: Combat?, opponent: Character?) {
        // noop
    }

    abstract fun applyBasicStats(self: Character)
    fun addLine(lineType: String, line: CharacterLine) {
        require(lineType != CharacterLine.DESCRIBE_LINER)
        lines.computeIfAbsent(lineType) { type: String? -> ArrayList() }
        lines[lineType]!!.add(line)
    }

    fun getRandomLineFor(lineType: String, c: Combat?, other: Character?): String {
        var lines: Map<String, MutableList<CharacterLine>> = lines
        val disguised = character.getStatus(Stsflag.disguised) as Disguised?
        if (disguised != null) {
            lines = disguised.target.lines
        }
        require(lineType != CharacterLine.DESCRIBE_LINER)
        return Global.format(Global.pickRandom(lines[lineType]).orElse(CharacterLine { cb: Combat?, sf: Character?, ot: Character? -> "" }).getLine(c, character, other), character, other)
    }

    fun describe(self: Character?): String {
        val model = JtwigModel.newModel()
                .with("self", self)
        return description!!.render(model).replace(System.lineSeparator(), " ")
    }

    open fun initializeArms(m: ArmManager?) {}
    open val armManager: ArmManager?
        get() = null

    fun makeDialog(): Dialog {
        return object : Dialog {
            override fun intrudeInCombat(c: Combat, target: Character, assist: Character) {
                c.write(intervene3p(c, target, assist))
            }

            override fun assistedByIntruder(c: Combat, target: Character, assist: Character) {
                c.updateAndClearMessage()
                c.write(victory3p(c, target, assist))
            }
        }
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 2279220186754458082L
        @JvmStatic
        protected fun setupCharacter(p: BasePersonality, charConfig: NpcConfiguration?,
                                     commonConfig: NpcConfiguration?) {
            p.setGrowth()
            p.applyBasicStats(p.character)

            // Apply config changes
            val mergedConfig = NpcConfiguration.mergeOptionalNpcConfigs(charConfig, commonConfig)
            mergedConfig?.let { cfg: NpcConfiguration -> cfg.apply(p.character) }
            if (Global.checkFlag("FutaTime") && p.character.initialGender == CharacterSex.female) {
                p.character.initialGender = CharacterSex.herm
            }
            p.character.body.makeGenitalOrgans(p.character.initialGender)
            p.character.body.finishBody(p.character.initialGender)
            for (i in 1 until p.character.progression.level) {
                p.character.growth.levelUp(p.character)
            }
            p.character.distributePoints(p.preferredAttributes)
            p.character.growth.addOrRemoveTraits(p.character)
        }
    }
}
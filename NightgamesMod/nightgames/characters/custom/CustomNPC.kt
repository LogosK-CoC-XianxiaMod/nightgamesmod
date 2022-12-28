package nightgames.characters.custom

import nightgames.characters.BasePersonality
import nightgames.characters.Character
import nightgames.characters.Emotion
import nightgames.characters.Trait
import nightgames.combat.Combat
import nightgames.combat.Result
import nightgames.global.Global
import nightgames.start.NpcConfiguration
import java.util.*
import java.util.function.Consumer

class CustomNPC @JvmOverloads constructor(val data: DataBackedNPCData, charConfig: NpcConfiguration? = null, commonConfig: NpcConfiguration? = null) : BasePersonality(data.getName(), data.isStartCharacter()) {

    init {
        character.isStartCharacter = data.isStartCharacter()
        character.plan = data.getPlan()
        character.mood = Emotion.confident
        setupCharacter(this, charConfig, commonConfig)
        for (lineType in CharacterLine.ALL_LINES) {
            if (lineType == CharacterLine.DESCRIBE_LINER) {
                description = data.describe()
                continue
            }
            addLine(lineType) { c: Combat?, self: Character?, other: Character? -> data.getLine(lineType, c, self, other) }
        }
        for (i in 1 until data.getStats().level) {
            character.ding(null)
        }
    }

    override fun applyBasicStats(self: Character) {
        preferredAttributes = ArrayList(data.getPreferredAttributes())
        self.outfitPlan.addAll(data.topOutfit)
        self.outfitPlan.addAll(data.bottomOutfit)
        self.closet.addAll(self.outfitPlan)
        self.change()
        self.att = HashMap(data.getStats().attributes)
        self.clearTraits()
        data.getStats().traits.forEach(Consumer { t: Trait? -> self.addTraitDontSaveData(t) })
        self.arousal.setMax(data.getStats().arousal)
        self.stamina.setMax(data.getStats().stamina)
        self.getMojo().setMax(data.getStats().mojo)
        self.willpower.setMax(data.getStats().willpower)
        self.trophy = data.getTrophy()
        self.isCustomNPC = true
        try {
            self.body = data.getBody().clone(self)
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        self.initialGender = data.getSex()
        for (i in data.getStartingItems()) {
            self.gain(i.item, i.amount)
        }
        Global.gainSkills(self)
    }

    override fun setGrowth() {
        character.growth = data.getGrowth()
    }

    override fun rest(time: Int) {
        for (i in data.getPurchasedItems()) {
            buyUpTo(i.item, i.amount)
        }
    }

    override fun victory(c: Combat, flag: Result): String {
        character.arousal.renew()
        return data.getLine("victory", c, character, c.getOpponentCharacter(character))
    }

    override fun defeat(c: Combat, flag: Result): String {
        return data.getLine("defeat", c, character, c.getOpponentCharacter(character))
    }

    override fun draw(c: Combat, flag: Result): String {
        return data.getLine("draw", c, character, c.getOpponentCharacter(character))
    }

    override fun fightFlight(opponent: Character): Boolean {
        return !character.mostlyNude() || opponent.mostlyNude()
    }

    override fun attack(opponent: Character?): Boolean {
        return true
    }

    override fun victory3p(c: Combat, target: Character, assist: Character?): String {
        return if (target.human()) {
            data.getLine("victory3p", c, character, assist)
        } else {
            data.getLine("victory3pAssist", c, character, target)
        }
    }

    override fun intervene3p(c: Combat, target: Character, assist: Character?): String {
        return if (target.human()) {
            data.getLine("intervene3p", c, character, assist)
        } else {
            data.getLine("intervene3pAssist", c, character, target)
        }
    }

    override fun fit(): Boolean {
        return !character.mostlyNude() && character.stamina.percent() >= 50
    }

    override fun checkMood(c: Combat?, mood: Emotion?, value: Int): Boolean {
        return data.checkMood(character, mood, value)
    }

    override fun getType(): String {
        return TYPE_PREFIX + data.getType()
    }

    override fun image(): String {
        return data.getPortraitName(character)
    }

    fun defaultImage(): String {
        return data.getDefaultPortraitName()
    }

    override val recruitmentData get() = data.getRecruitment()

    override val aiModifiers get() = data.getAiModifiers()

    override fun getComments(c: Combat): Map<CommentSituation, String> {
        val all = data.getComments()
        val applicable: MutableMap<CommentSituation, String> = HashMap()
        all.entries.stream().filter { (key): Map.Entry<CommentSituation, String> -> key.isApplicable(c, character, c.getOpponentCharacter(character)) }
                .forEach { (key, value): Map.Entry<CommentSituation, String> -> applicable[key] = value }
        return applicable
    }

    companion object {
        private const val serialVersionUID = -8169646189131720872L
        const val TYPE_PREFIX = "CUSTOM_"
    }
}
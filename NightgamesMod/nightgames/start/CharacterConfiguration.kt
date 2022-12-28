package nightgames.start

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import nightgames.characters.*
import nightgames.global.Global
import nightgames.items.clothing.Clothing
import nightgames.json.JsonUtils
import nightgames.json.JsonUtils.collectionFromJson
import nightgames.json.JsonUtils.getOptional
import nightgames.json.JsonUtils.getOptionalArray
import nightgames.json.JsonUtils.getOptionalObject
import java.lang.reflect.Field
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

abstract class CharacterConfiguration() {
    protected var name: Optional<String>
    @JvmField
    var gender: Optional<CharacterSex>
    @JvmField
    var attributes: MutableMap<Attribute, Int>
    protected var money: Optional<Int>
    protected var level: Optional<Int>
    protected var xp: Optional<Int>
    protected var traits: Optional<Collection<Trait>>
    protected var clothing: Optional<Collection<String>>
    protected var growth: MutableMap<String, Float>

    /**
     * Merges the fields of two CharacterConfigurations into the a new CharacterConfiguration.
     *
     * @param primaryConfig   The primary configuration.
     * @param secondaryConfig The secondary configuration. Field values will be overridden by values in primaryConfig. return
     */
    protected constructor(primaryConfig: CharacterConfiguration, secondaryConfig: CharacterConfiguration) : this() {
        name = ConfigurationUtils.mergeOptionals(primaryConfig.name, secondaryConfig.name)
        gender = ConfigurationUtils.mergeOptionals(primaryConfig.gender, secondaryConfig.gender)
        attributes.putAll(secondaryConfig.attributes)
        attributes.putAll(primaryConfig.attributes)
        money = ConfigurationUtils.mergeOptionals(primaryConfig.money, secondaryConfig.money)
        level = ConfigurationUtils.mergeOptionals(primaryConfig.level, secondaryConfig.level)
        xp = ConfigurationUtils.mergeOptionals(primaryConfig.xp, secondaryConfig.xp)
        clothing = ConfigurationUtils.mergeOptionals(primaryConfig.clothing, secondaryConfig.clothing)
        traits = ConfigurationUtils.mergeCollections(primaryConfig.traits, secondaryConfig.traits)
        growth.putAll(primaryConfig.growth)
        growth.putAll(secondaryConfig.growth)
    }

    init {
        name = Optional.empty()
        gender = Optional.empty()
        attributes = mutableMapOf()
        money = Optional.empty()
        level = Optional.empty()
        xp = Optional.empty()
        traits = Optional.empty()
        clothing = Optional.empty()
        growth = mutableMapOf()
    }

    private fun calculateAttributeLevelPlan(base: Character, desiredLevel: Int, desiredFinalAttributes: Map<Attribute, Int>): Map<Int, Map<Attribute, Int>> {
        val deltaAtts = desiredFinalAttributes.keys.associateWith { key -> desiredFinalAttributes[key]!! - base.att.getOrDefault(key, 0)!! }.toMutableMap()
        val attributeLevelPlan: MutableMap<Int, Map<Attribute, Int>> = mutableMapOf()
        // k this is some terrible code but what it's doing is trying to simulate level ups for a character based on the number of levels
        // it gets and what final attributes it has
        for (i in base.progression.level + 1..desiredLevel) {
            // calculates how many more attributes it needs to add
            val attsLeftToAdd = deltaAtts.values.stream().mapToInt { obj: Int -> obj }.sum()
            // calculates how many more levels left to distribute points (counting the current level)
            val levelsLeft = desiredLevel - i + 1
            // calculates how many points to add for this particular level
            val attsToAdd = attsLeftToAdd / levelsLeft
            val attsForLevel: MutableMap<Attribute, Int> = mutableMapOf()
            attributeLevelPlan[i] = attsForLevel
            for (j in 0 until attsToAdd) {
                // randomly pick an attribute to train out of the ones that needs to be trained.
                val attsToTrain = deltaAtts.entries.stream()
                        .filter { (_, value): Map.Entry<Attribute, Int> -> value > 0 }
                        .map { (key): Map.Entry<Attribute, Int> -> key }
                        .collect(Collectors.toList())
                val attToTrain = Global.pickRandom(attsToTrain)
                // put it into the level plan.
                attToTrain.ifPresent { att: Attribute ->
                    attsForLevel.compute(att) { key: Attribute?, old: Int? -> if (old == null) 1 else old + 1 }
                    deltaAtts.compute(att) { key: Attribute?, old: Int? -> old!! - 1 }
                }
            }
        }
        return attributeLevelPlan
    }

    protected fun apply(base: Character) {
        name.ifPresent { n: String? -> base.setName(n!!) }
        money.ifPresent { m: Int? -> base.money = m!! }
        traits.ifPresent { traits: Collection<Trait> ->
            base.clearTraits()
            traits.forEach(Consumer { t: Trait? -> base.addTraitDontSaveData(t) })
            traits.forEach(Consumer { trait: Trait? -> base.getGrowth().addTrait(0, trait) })
        }
        val bg = base.getGrowth()
        for (key in growth.keys) {
            if (GROWTH_FIELDS_NAMES.contains(key)) {
                try {
                    GROWTH_FIELDS[GROWTH_FIELDS_NAMES.indexOf(key)][bg] = growth[key]
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            } //This is a terrible way to do this, but the below is the only alternative without rewriting everything using Growth, and that might be even more terrible.
            //It actually works though, heh.

//            if (key=="arousal") bg.arousal=growth.get("arousal");
//            if (key=="stamina") bg.stamina=growth.get("stamina");
//            if (key=="mojo") bg.mojo=growth.get("mojo");
//            if (key=="willpower") bg.willpower=growth.get("willpower");
        }
        level.ifPresent { l: Int ->
            base.progression.level = l
            modMeters(base, l * 2) // multiplication to compensate for missed daytime gains
        }
        xp.ifPresent { x: Int? -> base.progression.gainXP(x!!) }
        val start: Map<Attribute, Int?> = HashMap(base.att)
        val deltaAtts = attributes.keys
                .stream()
                .collect(Collectors.toMap(Function.identity(), Function { key: Attribute -> attributes[key]!! - start.getOrDefault(key, 0)!! }))
        level.ifPresent { desiredLevel: Int ->
            val attributeLevelPlan = calculateAttributeLevelPlan(base, desiredLevel, attributes)
            println(attributeLevelPlan)
            while (base.progression.level < desiredLevel) {
                base.progression.level = base.progression.level + 1
                modMeters(base, 1) // multiplication to compensate for missed daytime gains
                attributeLevelPlan[base.progression.level]!!.forEach { (a: Attribute?, `val`: Int) ->
                    if (`val` > 0) {
                        base.mod(a, `val`, true)
                    }
                }
                base.getGrowth().addOrRemoveTraits(base)
            }
        }
        base.att.putAll(attributes)
        if (clothing.isPresent) {
            val clothes = clothing.get().stream().map { key: String? -> Clothing.getByID(key) }.collect(Collectors.toList())
            base.outfitPlan = ArrayList(clothes)
            base.closet = HashSet(clothes)
            base.change()
        }
        base.levelUpIfPossible(null)
    }

    /**
     * Parses fields common to PlayerConfiguration and NpcConfigurations.
     *
     * @param object The configuration read from the JSON config file.
     */
    protected fun parseCommon(`object`: JsonObject?) {
        name = getOptional(`object`!!, "name").map { obj: JsonElement -> obj.asString }
        gender = getOptional(`object`, "gender").map { obj: JsonElement -> obj.asString }.map { obj: String -> obj.lowercase(Locale.getDefault()) }
                .map { name: String? -> CharacterSex.valueOf(name!!) }
        traits = getOptionalArray(`object`, "traits")
                .map { array: JsonArray? -> collectionFromJson(array, Trait::class.java) }
        clothing = getOptionalArray(`object`, "clothing").map { obj: JsonArray -> JsonUtils.stringsFromJson(obj) }
        money = getOptional(`object`, "money").map { obj: JsonElement -> obj.asInt }
        level = getOptional(`object`, "level").map { obj: JsonElement -> obj.asInt }
        xp = getOptional(`object`, "xp").map { obj: JsonElement -> obj.asInt }
        attributes = getOptionalObject(`object`, "attributes")
                .map { obj: JsonObject -> JsonUtils.fromJson<MutableMap<Attribute, Int>>(obj) }.orElse(mutableMapOf())
        growth = getOptionalObject(`object`, "growth")
                .map { obj: JsonObject -> JsonUtils.fromJson<MutableMap<String, Float>>(obj) }.orElse(mutableMapOf())
    }

    override fun toString(): String {
        return "CharacterConfiguration with name $name gender $gender attributes $attributes money $money level $level traits $traits XP $xp clothing $clothing growth $growth"
    }

    fun nameIsSet(): Boolean {
        return name.isPresent
    }

    companion object {
        private val GROWTH_FIELDS = Growth::class.java.fields
        private val GROWTH_FIELDS_NAMES = Stream.of(*GROWTH_FIELDS).map { obj: Field -> obj.name }.collect(Collectors.toList())
        private fun modMeters(character: Character, levels: Int) {
            val growth = character.getGrowth()
            for (i in 0 until levels) {
                growth.levelUpCoreStatsOnly(character)
            }
        }
    }
}
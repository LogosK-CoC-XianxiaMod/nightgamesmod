package nightgames.characters.custom

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import nightgames.characters.*
import nightgames.characters.body.Body
import nightgames.characters.custom.effect.CustomEffect
import nightgames.characters.custom.effect.MoneyModEffect
import nightgames.items.Item
import nightgames.items.ItemAmount
import nightgames.items.clothing.Clothing
import nightgames.json.JsonUtils
import nightgames.json.JsonUtils.collectionFromJson
import nightgames.json.JsonUtils.getOptional
import nightgames.json.JsonUtils.getOptionalArray
import nightgames.json.JsonUtils.getOptionalObject
import nightgames.json.JsonUtils.gson
import nightgames.json.JsonUtils.rootJson
import nightgames.requirements.JsonRequirementLoader
import nightgames.skills.Skill
import nightgames.stance.Stance
import nightgames.status.Stsflag
import nightgames.utilities.DebugHelper
import org.jtwig.JtwigTemplate
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer

object JsonSourceNPCDataLoader {
    private val requirementLoader = JsonRequirementLoader()
    @JvmStatic
    fun loadResources(resources: JsonObject, stats: Stats) {
        stats.stamina = resources["stamina"].asInt
        stats.arousal = resources["arousal"].asInt
        stats.mojo = resources["mojo"].asInt
        stats.willpower = resources["willpower"].asInt
    }

    @JvmStatic
    @Throws(JsonParseException::class)
    fun load(`in`: InputStream?): DataBackedNPCData {
        val `object` = rootJson(InputStreamReader(`in`)).asJsonObject
        return load(`object`)
    }

    @JvmStatic
    fun load(`object`: JsonObject): DataBackedNPCData {
        val data = DataBackedNPCData()
        data.name = `object`["name"].asString
        data.type = `object`["type"].asString
        data.trophy = Item.valueOf(`object`["trophy"].asString)
        data.plan = Plan.valueOf(`object`["plan"].asString)

        // load outfit
        val outfit = `object`.getAsJsonObject("outfit")
        val top = outfit.getAsJsonArray("top")
        for (clothing in top) {
            data.top.push(Clothing.getByID(clothing.asString))
        }
        val bottom = outfit.getAsJsonArray("bottom")
        for (clothing in bottom) {
            data.bottom.push(Clothing.getByID(clothing.asString))
        }

        // load stats
        val stats = `object`.getAsJsonObject("stats")
        // load base stats
        val baseStats = stats.getAsJsonObject("base")
        data.stats.level = baseStats["level"].asInt
        // load attributes
        data.stats.attributes.putAll(JsonUtils.fromJson<MutableMap<Attribute, Int>>(baseStats.getAsJsonObject("attributes")))
        loadResources(baseStats.getAsJsonObject("resources"), data.stats)
        loadTraits(baseStats.getAsJsonArray("traits"), data.stats.traits)
        data.setGrowth(Growth(stats.getAsJsonObject("growth")))
        loadPreferredAttributes(stats.getAsJsonObject("growth").getAsJsonArray("preferredAttributes"),
                data.preferredAttributes)
        loadItems(`object`.getAsJsonObject("items"), data)
        loadTemplates(`object`.getAsJsonObject("templates"), data.templates)
        loadAllLines(`object`.getAsJsonObject("lines"), data.characterLines)
        loadPortraits(`object`.getAsJsonObject("portraits"), data.portraitMap)
        loadRecruitment(`object`.getAsJsonObject("recruitment"), data.recruitment)
        data.body = Body.load(`object`.getAsJsonObject("body"), null)
        data.sex = CharacterSex.valueOf(`object`["sex"].asString)
        getOptionalArray(`object`, "ai-modifiers").ifPresent { arr: JsonArray -> loadAiModifiers(arr, data.aiModifiers) }
        getOptional(`object`, "start").map { obj: JsonElement -> obj.asBoolean }.ifPresent { b: Boolean? -> data.isStartCharacter = b!! }
        data.aiModifiers.setMalePref(getOptional(`object`, "male-pref").map { obj: JsonElement -> obj.asDouble })
        getOptionalArray(`object`, "comments").ifPresent { arr: JsonArray -> loadComments(arr, data) }
        return data
    }

    @JvmStatic
    fun loadRecruitment(`object`: JsonObject, recruitment: RecruitmentData) {
        recruitment.introduction = `object`["introduction"].asString
        recruitment.action = `object`["action"].asString
        recruitment.confirm = `object`["confirm"].asString
        recruitment.requirement = requirementLoader.loadRequirements(`object`.getAsJsonObject("requirements"))
        loadEffects(`object`.getAsJsonArray("cost"), recruitment.effects)
    }

    @JvmStatic
    fun loadEffects(jsonArray: JsonArray, effects: MutableList<CustomEffect?>) {
        for (element in jsonArray) {
            val obj = element.asJsonObject
            getOptional(obj, "modMoney").ifPresent { e: JsonElement -> effects.add(MoneyModEffect(e.asInt)) }
        }
    }

    private fun loadAllLines(linesObj: JsonObject, characterLines: MutableMap<String, List<CustomStringEntry>>) {
        for ((key) in linesObj.entrySet()) {
            val lines = loadLines(linesObj.getAsJsonArray(key))
            characterLines[key] = lines
        }
    }

    private fun loadLines(linesArr: JsonArray): List<CustomStringEntry> {
        val entries: MutableList<CustomStringEntry> = ArrayList()
        for (element in linesArr) {
            entries.add(readLine(element.asJsonObject))
        }
        return entries
    }

    @JvmStatic
    fun readLine(`object`: JsonObject): CustomStringEntry {
        val entry = CustomStringEntry(`object`["text"].asString)
        entry.requirements = getOptionalObject(`object`, "requirements")
                .map { obj: JsonObject? -> requirementLoader.loadRequirements(obj) }.orElse(ArrayList())
        return entry
    }

    private fun loadItems(obj: JsonObject, data: DataBackedNPCData) {
        loadItemsArray(obj.getAsJsonArray("initial"), data.startingItems)
        loadItemsArray(obj.getAsJsonArray("purchase"), data.purchasedItems)
    }

    private fun loadItemsArray(arr: JsonArray, items: MutableList<ItemAmount>) {
        for (mem in arr) {
            val obj = mem as JsonObject
            items.add(readItem(obj))
        }
    }

    @JvmStatic
    fun readItem(obj: JsonObject?): ItemAmount {
        return gson!!.fromJson(obj, ItemAmount::class.java)
    }

    @JvmStatic
    fun loadPreferredAttributes(arr: JsonArray, preferredAttributes: MutableList<PreferredAttribute?>) {
        for (element in arr) {
            val obj = element.asJsonObject
            val att = gson!!.fromJson(obj["attribute"], Attribute::class.java)
            val max = getOptional(obj, "max").map { obj: JsonElement -> obj.asInt }.orElse(Int.MAX_VALUE)
            preferredAttributes.add(MaxAttribute(att, max))
        }
    }

    @JvmStatic
    fun loadGrowthTraits(arr: JsonArray, growth: Growth) {
        for (element in arr) {
            val obj = element.asJsonObject
            val trait = gson!!.fromJson(obj["trait"], Trait::class.java)
            if (trait != null) {
                growth.addTrait(obj["level"].asInt, trait)
            } else {
                System.err.println("Tried to load a null trait into growth!")
                DebugHelper.printStackFrame(3, 1)
            }
        }
    }

    private fun loadTraits(array: JsonArray, traits: MutableList<Trait>) {
        collectionFromJson(array, Trait::class.java)?.let { traits.addAll(it) }
    }

    @JvmStatic
    fun loadAiModifiers(arr: JsonArray, mods: AiModifiers) {
        for (aiMod in arr) {
            val obj = aiMod as JsonObject
            val value = obj["value"].asString
            val weight = obj["weight"].asFloat.toDouble()
            val type = obj["type"].asString
            when (type) {
                "skill" -> try {
                    mods.attackMods[Class.forName(value) as Class<out Skill?>] = weight
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("Skill not found: $value")
                }

                "position" -> mods.positionMods[Stance.valueOf(value)] = weight
                "self-status" -> mods.selfStatusMods[Stsflag.valueOf(value)] = weight
                "opponent-status" -> mods.oppStatusMods[Stsflag.valueOf(value)] = weight
                else -> throw IllegalArgumentException("Type of AiModifier must be one of \"skill\", "
                        + "\"position\", \"self-status\", or \"opponent-status\", " + "but was \"" + type
                        + "\".")
            }
        }
    }

    private fun loadComments(arr: JsonArray, data: DataBackedNPCData) {
        arr.forEach(Consumer { e: JsonElement -> CommentSituation.parseComment(e.asJsonObject, data.comments) })
    }

    private fun loadTemplates(obj: JsonObject, templates: MutableMap<String, JtwigTemplate>) {
        for ((key, value) in obj.entrySet()) {
            templates[key] = JtwigTemplate.classpathTemplate(value.asString)
        }
    }

    private fun loadPortraits(obj: JsonObject, portraits: MutableMap<Emotion, String>) {
        obj.entrySet().forEach(Consumer { (key, value): Map.Entry<String?, JsonElement> -> portraits[Emotion.valueOf(key!!)] = value.asString })
    }
}
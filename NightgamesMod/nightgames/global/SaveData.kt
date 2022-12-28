package nightgames.global

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import nightgames.characters.Character
import nightgames.json.JsonUtils
import java.util.function.Consumer

/**
 * SaveData specifies a schema for data that will be saved and loaded.
 */
class SaveData() {
    @JvmField
    val players: MutableSet<Character>
    @JvmField
    val flags: MutableSet<String>
    @JvmField
    val counters: MutableMap<String, Float>
    @JvmField
    var time: Time? = null
    @JvmField
    var date = 0
    @JvmField
    var fontsize = 0

    private enum class JSONKey(val key: String) {
        PLAYERS("characters"), FLAGS("flags"), COUNTERS("counters"), TIME("time"), DATE("date"), FONTSIZE("fontsize")
    }

    init {
        players = HashSet()
        flags = HashSet()
        counters = HashMap()
    }

    constructor(rootJSON: JsonObject) : this() {
        if (rootJSON.has("xpRate")) {
            Global.xpRate = rootJSON["xpRate"].asDouble
        }
        val charactersJSON = rootJSON.getAsJsonArray(JSONKey.PLAYERS.key)
        for (element in charactersJSON) {
            val characterJSON = element.asJsonObject
            val type = characterJSON["type"].asString
            val character = Global.getCharacterByType(type)
            character.load(characterJSON)
            players.add(character)
        }
        val flagsJSON = rootJSON.getAsJsonArray(JSONKey.FLAGS.key)
        for (element in flagsJSON) {
            flags.add(element.asString)
        }
        val countersJSON = rootJSON.getAsJsonObject(JSONKey.COUNTERS.key)
        counters.putAll(JsonUtils.fromJson<Map<String, Float>>(countersJSON))
        date = rootJSON[JSONKey.DATE.key].asInt
        fontsize = if (rootJSON.has(JSONKey.FONTSIZE.key)) {
            rootJSON[JSONKey.FONTSIZE.key].asInt
        } else {
            5
        }
        time = Time.fromDesc(rootJSON[JSONKey.TIME.key].asString)
        println("savedata constructed: $this")
    }

    fun toJson(): JsonObject {
        val rootJSON = JsonObject()
        rootJSON.add("xpRate", JsonPrimitive(Global.xpRate))
        val characterJSON = JsonArray()
        players.stream().map { obj: Character -> obj.save() }.forEach { element: JsonObject? -> characterJSON.add(element) }
        rootJSON.add(JSONKey.PLAYERS.key, characterJSON)
        val flagJSON = JsonArray()
        flags.forEach(Consumer { string: String? -> flagJSON.add(string) })
        rootJSON.add(JSONKey.FLAGS.key, flagJSON)
        val counterJSON = JsonObject()
        counters.forEach { (property: String?, value: Float?) -> counterJSON.addProperty(property, value) }
        rootJSON.add(JSONKey.COUNTERS.key, counterJSON)
        rootJSON.addProperty(JSONKey.TIME.key, time!!.desc)
        rootJSON.addProperty(JSONKey.DATE.key, date)
        rootJSON.addProperty(JSONKey.FONTSIZE.key, fontsize)
        println("savedata toJson: $this")
        return rootJSON
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val saveData = o as SaveData
        if (date != saveData.date) return false
        if (players != saveData.players) return false
        if (flags != saveData.flags) return false
        return if (counters != saveData.counters) false else time == saveData.time
    }

    override fun hashCode(): Int {
        var result = players.hashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + counters.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + date
        return result
    }

    override fun toString(): String {
        return ("SaveData{" + "players=" + players + ", flags=" + flags + ", counters=" + counters + ", time=" + time
                + ", date=" + date + '}')
    }
}
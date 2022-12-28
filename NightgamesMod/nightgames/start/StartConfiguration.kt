package nightgames.start

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import nightgames.characters.Attribute
import nightgames.characters.CharacterSex
import nightgames.global.DebugFlags
import nightgames.json.JsonUtils.collectionFromJson
import nightgames.json.JsonUtils.rootJson
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.function.Consumer

class StartConfiguration private constructor() {
    var name: String? = null
        private set
    var summary: String? = null
        private set
    var isEnabled = false
        private set
    @JvmField
    var player: PlayerConfiguration? = null
    private var npcs: MutableCollection<NpcConfiguration>? = null
    @JvmField
    var npcCommon: NpcConfiguration? = null
    var flags: Collection<String>? = null
        private set
    var debug: Collection<DebugFlags>? = null
        private set
        get() {
            return field
        }
    fun playerCanChooseGender(): Boolean {
        return !player!!.gender.isPresent
    }

    fun playerCanChooseTraits(): Boolean {
        return player!!.allowsMoreTraits()
    }

    fun availableAttributePoints(): Int {
        return player!!.attributePoints
    }

    fun playerAttributes(): Map<Attribute, Int> {
        return HashMap(player!!.attributes)
    }

    fun chosenPlayerGender(): CharacterSex {
        return player!!.gender.orElseThrow { RuntimeException("No gender specified in this configuration") }
    }

    override fun toString(): String {
        return name!!
    }

    fun findNpcConfig(type: String): NpcConfiguration? {
        return npcs!!.firstOrNull { npc: NpcConfiguration -> type == npc.type }
    }

    companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        fun parse(root: JsonObject): StartConfiguration {
            val cfg = StartConfiguration()
            cfg.name = root["name"].asString
            cfg.summary = root["summary"].asString
            cfg.isEnabled = root["enabled"].asBoolean
            cfg.player = PlayerConfiguration.parse(root.getAsJsonObject("player"))
            cfg.npcCommon = NpcConfiguration.parseAllNpcs(root.getAsJsonObject("all_npcs"))
            cfg.npcs = HashSet()
            val npcs = root.getAsJsonArray("npcs")
            npcs.forEach(Consumer { element: JsonElement ->
                try {
                    (cfg.npcs as HashSet<NpcConfiguration>).add(NpcConfiguration.parse(element.asJsonObject))
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            })
            val flags = root.getAsJsonArray("flags")
            cfg.flags = collectionFromJson(flags, String::class.java)
            val debugFlags = root.getAsJsonArray("debugFlags")
            cfg.debug = collectionFromJson(debugFlags, DebugFlags::class.java)
            return cfg
        }

        @JvmStatic
        fun loadConfigurations(): Collection<StartConfiguration> {
            val dir = File("starts/").toPath()
            val res: MutableCollection<StartConfiguration> = ArrayList()
            try {
                for (file in Files.newDirectoryStream(dir)) {
                    if (file.toString()
                                    .endsWith(".json")) {
                        try {
                            res.add(parse(rootJson(file).asJsonObject))
                        } catch (e: Exception) {
                            println("Failed to load configuration from $file: ")
                            System.out.flush()
                            e.printStackTrace()
                            System.err.flush()
                        }
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            return res
        }
    }
}
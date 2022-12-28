package nightgames.json

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import nightgames.characters.body.GenericBodyPart
import nightgames.characters.body.mods.PartMod
import nightgames.items.clothing.Clothing
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object JsonUtils {
    @JvmStatic
    var gson: Gson? = null
        get() {
            if (field == null) {
                field = GsonBuilder().setPrettyPrinting()
                        .registerTypeAdapter(Clothing::class.java, ClothingAdaptor())
                        .registerTypeAdapter(GenericBodyPart::class.java, BodyPartAdapter())
                        .registerTypeAdapter(PartMod::class.java, PartModAdapter())
                        .create()
            }
            return field
        }
        private set

    @JvmStatic
    fun <T> collectionFromJson(array: JsonArray?, clazz: Class<T>?): Collection<T>? {
        val type: Type = ParameterizedCollectionType(clazz)
        return gson?.fromJson(array, type)
    }

    inline fun <reified T> fromJson(`object`: JsonObject): T = gson!!.fromJson(`object`, object: TypeToken<T>() {}.type)
    inline fun <reified T> fromJson(`object`: JsonArray): T = gson!!.fromJson(`object`, object: TypeToken<T>() {}.type)

    fun jsonFromCollection(collection: Collection<*>?): JsonArray {
        return gson!!.toJsonTree(collection).asJsonArray
    }

    /**
     * Convenience method for turning JsonObjects into maps
     *
     * @param object A JsonObject with some number of properties and values.
     * @return A map as constructed by Gson
     */
    @JvmStatic
    fun <K : Any, V: Any> oldMapFromJson(`object`: JsonObject, keyClazz: Class<K>, valueClazz: Class<V>): MutableMap<K?, V?> {
        val type: Type = ParameterizedMapType(keyClazz, valueClazz)
        return gson!!.fromJson(`object`, type)
    }

    fun JsonFromMap(map: Map<*, *>): JsonObject {
        return gson!!.toJsonTree(map).asJsonObject
    }

    @JvmStatic
    fun getOptional(`object`: JsonObject, key: String?): Optional<JsonElement> {
        return Optional.ofNullable(`object`[key])
    }

    @JvmStatic
    fun getOptionalArray(`object`: JsonObject, key: String?): Optional<JsonArray> {
        return getOptional(`object`, key).map { obj: JsonElement -> obj.asJsonArray }
    }

    @JvmStatic
    fun getOptionalObject(`object`: JsonObject, key: String?): Optional<JsonObject> {
        return getOptional(`object`, key).map { obj: JsonElement -> obj.asJsonObject }
    }

    @JvmStatic
    fun stringsFromJson(array: JsonArray?): Collection<String>? {
        return collectionFromJson(array, String::class.java)
    }

    @JvmStatic
    @Throws(JsonParseException::class, IOException::class)
    fun rootJson(path: Path?): JsonElement {
        val reader: Reader = Files.newBufferedReader(path)
        return rootJson(reader)
    }

    @JvmStatic
    @Throws(JsonParseException::class)
    fun rootJson(reader: Reader?): JsonElement {
        val parser = JsonParser()
        return parser.parse(reader)
    }
}
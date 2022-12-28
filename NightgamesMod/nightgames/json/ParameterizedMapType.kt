package nightgames.json

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Provides a Type for maps that lets Gson.fromJson() get around type erasure.
 */
internal class ParameterizedMapType<K, V>(private val keyType: Class<K>, private val valueType: Class<V>) : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type?> {
        return arrayOf(keyType, valueType)
    }

    override fun getRawType(): Type {
        return MutableMap::class.java
    }

    override fun getOwnerType(): Type? {
        return null
    }
}
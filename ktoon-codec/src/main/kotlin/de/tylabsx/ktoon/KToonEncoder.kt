package de.tylabsx.ktoon

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Reflection based encoder for converting Kotlin values into ToonValue trees.
 *
 * Supported input types:
 *
 * - null
 * - String
 * - Number
 * - Boolean
 * - Enum
 * - Map
 * - Iterable
 * - Array
 * - ToonValue
 * - Kotlin objects / data classes
 *
 * Example:
 *
 * ```kotlin
 * val value = KToonEncoder().encode(User("Alice", 25))
 * val toon = KToon.stringify(value)
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonEncoder {

    /**
     * Encodes any supported Kotlin value into a ToonValue.
     *
     * @param value value to encode
     * @return encoded ToonValue
     */
    fun encode(value: Any?): ToonValue {
        return encodeAny(value)
    }

    /**
     * Encodes a value recursively.
     *
     * @param value value to encode
     * @return encoded ToonValue
     */
    private fun encodeAny(value: Any?): ToonValue {
        return when (value) {
            null -> ToonNull

            is ToonValue -> value

            is String -> ToonString(value)
            is Char -> ToonString(value.toString())

            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double -> ToonNumber(value.toString())

            is Boolean -> ToonBoolean(value)

            is Enum<*> -> ToonString(value.name)

            is Map<*, *> -> encodeMap(value)

            is Iterable<*> -> ToonArray(value.map { encodeAny(it) })

            is Array<*> -> ToonArray(value.map { encodeAny(it) })

            is BooleanArray -> ToonArray(value.map { ToonBoolean(it) })
            is ByteArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is ShortArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is IntArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is LongArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is FloatArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is DoubleArray -> ToonArray(value.map { ToonNumber(it.toString()) })
            is CharArray -> ToonArray(value.map { ToonString(it.toString()) })

            else -> encodeObject(value)
        }
    }

    /**
     * Encodes a map into a ToonObject.
     *
     * @param map map to encode
     * @return encoded ToonObject
     */
    private fun encodeMap(map: Map<*, *>): ToonObject {
        return ToonObject(
            map.entries.associate { (key, value) ->
                val keyString = key?.toString()
                    ?: throw KToonCodecException("Map keys must not be null")

                keyString to encodeAny(value)
            }
        )
    }

    /**
     * Encodes a Kotlin object using reflection.
     *
     * Only member properties are encoded. Functions are ignored.
     *
     * @param value object to encode
     * @return encoded ToonObject
     */
    private fun encodeObject(value: Any): ToonObject {
        val properties = value::class.memberProperties

        return ToonObject(
            properties.associate { property ->
                @Suppress("UNCHECKED_CAST")
                val typedProperty = property as KProperty1<Any, *>
                typedProperty.isAccessible = true

                property.name to encodeAny(typedProperty.get(value))
            }
        )
    }
}
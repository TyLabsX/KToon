package de.tylabsx.ktoon

import de.tylabsx.ktoon.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * Reflection based decoder for converting ToonValue trees into Kotlin objects.
 *
 * Supported target types:
 *
 * - String
 * - Int
 * - Long
 * - Double
 * - Float
 * - Boolean
 * - Enum
 * - List
 * - Map
 * - data classes / constructor based classes
 *
 * Example:
 *
 * ```kotlin
 * val user = KToonDecoder().decode<User>(toonValue)
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonDecoder {

    /**
     * Decodes a ToonValue into a Kotlin value of type T.
     *
     * @param value ToonValue to decode
     * @return decoded Kotlin value
     */
    inline fun <reified T : Any> decode(value: ToonValue): T {
        return decode(value, T::class.createType()) as T
    }

    /**
     * Decodes a ToonValue into the given Kotlin type.
     *
     * @param value ToonValue to decode
     * @param targetType target Kotlin type
     * @return decoded Kotlin value
     */
    fun decode(value: ToonValue, targetType: KType): Any? {
        if (value is ToonNull) {
            if (targetType.isMarkedNullable) return null
            throw KToonCodecException("Cannot decode null into non-nullable type $targetType")
        }

        val targetClass = targetType.jvmErasure

        return when (targetClass) {
            String::class -> decodeString(value)
            Int::class -> decodeNumber(value).toInt()
            Long::class -> decodeNumber(value).toLong()
            Double::class -> decodeNumber(value).toDouble()
            Float::class -> decodeNumber(value).toFloat()
            Short::class -> decodeNumber(value).toShort()
            Byte::class -> decodeNumber(value).toByte()
            Boolean::class -> decodeBoolean(value)

            List::class,
            MutableList::class -> decodeList(value, targetType)

            Map::class,
            MutableMap::class -> decodeMap(value, targetType)

            else -> {
                if (targetClass.java.isEnum) {
                    decodeEnum(value, targetClass)
                } else {
                    decodeObject(value, targetClass)
                }
            }
        }
    }

    /**
     * Decodes a ToonValue into a String.
     *
     * @param value source value
     * @return decoded string
     */
    private fun decodeString(value: ToonValue): String {
        return when (value) {
            is ToonString -> value.value
            is ToonNumber -> value.raw
            is ToonBoolean -> value.value.toString()
            else -> throw KToonCodecException("Cannot decode $value as String")
        }
    }

    /**
     * Decodes a ToonValue into a numeric string.
     *
     * @param value source value
     * @return numeric raw string
     */
    private fun decodeNumber(value: ToonValue): String {
        return when (value) {
            is ToonNumber -> value.raw
            is ToonString -> value.value
            else -> throw KToonCodecException("Cannot decode $value as Number")
        }
    }

    /**
     * Decodes a ToonValue into a Boolean.
     *
     * @param value source value
     * @return decoded boolean
     */
    private fun decodeBoolean(value: ToonValue): Boolean {
        return when (value) {
            is ToonBoolean -> value.value
            is ToonString -> value.value.toBooleanStrict()
            else -> throw KToonCodecException("Cannot decode $value as Boolean")
        }
    }

    /**
     * Decodes a ToonArray into a Kotlin List.
     *
     * @param value source value
     * @param targetType target list type
     * @return decoded list
     */
    private fun decodeList(value: ToonValue, targetType: KType): List<Any?> {
        val array = value as? ToonArray
            ?: throw KToonCodecException("Cannot decode $value as List")

        val elementType = targetType.arguments.firstOrNull()?.type ?: Any::class.createType()

        return array.values.map { decodeFlexible(it, elementType) }
    }

    /**
     * Decodes a ToonObject into a Kotlin Map.
     *
     * @param value source value
     * @param targetType target map type
     * @return decoded map
     */
    private fun decodeMap(value: ToonValue, targetType: KType): Map<String, Any?> {
        val obj = value as? ToonObject
            ?: throw KToonCodecException("Cannot decode $value as Map")

        val valueType = targetType.arguments.getOrNull(1)?.type ?: Any::class.createType()

        return obj.entries.mapValues { (_, childValue) ->
            decodeFlexible(childValue, valueType)
        }
    }

    /**
     * Decodes an enum value from a ToonString.
     *
     * @param value source value
     * @param enumClass target enum class
     * @return decoded enum constant
     */
    private fun decodeEnum(value: ToonValue, enumClass: KClass<*>): Any {
        val raw = decodeString(value)

        return enumClass.java.enumConstants
            ?.firstOrNull { (it as Enum<*>).name == raw }
            ?: throw KToonCodecException("Unknown enum constant '$raw' for ${enumClass.simpleName}")
    }

    /**
     * Decodes a ToonObject into a Kotlin constructor based object.
     *
     * @param value source value
     * @param targetClass target class
     * @return decoded object
     */
    private fun decodeObject(value: ToonValue, targetClass: KClass<*>): Any {
        val obj = value as? ToonObject
            ?: throw KToonCodecException("Cannot decode $value as ${targetClass.simpleName}")

        val constructor = targetClass.primaryConstructor
            ?: targetClass.constructors.firstOrNull()
            ?: throw KToonCodecException("Class ${targetClass.simpleName} has no usable constructor")

        val args = mutableMapOf<KParameter, Any?>()

        constructor.parameters.forEach { parameter ->
            val name = parameter.name
                ?: throw KToonCodecException("Constructor parameter without name in ${targetClass.simpleName}")

            val childValue = obj.entries[name]

            if (childValue == null) {
                if (parameter.isOptional) return@forEach

                if (parameter.type.isMarkedNullable) {
                    args[parameter] = null
                    return@forEach
                }

                throw KToonCodecException("Missing required property '$name' for ${targetClass.simpleName}")
            }

            args[parameter] = decode(childValue, parameter.type)
        }

        return constructor.callBy(args)
    }

    /**
     * Decodes values for loosely typed targets such as Any.
     *
     * @param value source value
     * @param targetType target type
     * @return decoded value
     */
    private fun decodeFlexible(value: ToonValue, targetType: KType): Any? {
        if (targetType.jvmErasure == Any::class) {
            return toNative(value)
        }

        return decode(value, targetType)
    }

    /**
     * Converts a ToonValue into native Kotlin structures.
     *
     * @param value source value
     * @return native Kotlin representation
     */
    private fun toNative(value: ToonValue): Any? {
        return when (value) {
            is ToonObject -> value.entries.mapValues { (_, child) -> toNative(child) }
            is ToonArray -> value.values.map { toNative(it) }
            is ToonString -> value.value
            is ToonNumber -> value.raw
            is ToonBoolean -> value.value
            is ToonNull -> null
        }
    }
}
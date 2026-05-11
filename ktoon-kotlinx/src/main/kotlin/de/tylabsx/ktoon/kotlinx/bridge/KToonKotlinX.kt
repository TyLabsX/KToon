package de.tylabsx.ktoon.kotlinx.bridge

import de.tylabsx.ktoon.KToonParserEngine
import de.tylabsx.ktoon.KToonWriterEngine
import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonBoolean
import de.tylabsx.ktoon.ToonNull
import de.tylabsx.ktoon.ToonNumber
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonString
import de.tylabsx.ktoon.ToonValue

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * kotlinx.serialization integration for KToon.
 *
 * This format bridges kotlinx.serialization through JsonElement and converts
 * between JsonElement and ToonValue internally.
 *
 * It provides a stable first integration layer for @Serializable classes while
 * keeping the custom TOON parser and writer as the final text format.
 *
 * Example:
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val toon = KToonKotlinX.encodeToString(User(1, "Alice"))
 * val user = KToonKotlinX.decodeFromString<User>(toon)
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
@Deprecated(
    message = "Use KToonNativeFormat instead",
    level = DeprecationLevel.WARNING
)
object KToonKotlinX : StringFormat {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    private val parser = KToonParserEngine()
    private val writer = KToonWriterEngine()

    /**
     * Serializers module used by this format.
     */
    override val serializersModule = json.serializersModule

    /**
     * Encodes a serializable value into a TOON string.
     *
     * @param serializer serializer for the value
     * @param value value to encode
     * @return TOON string
     */
    override fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T
    ): String {
        val jsonElement = json.encodeToJsonElement(serializer, value)
        val toonValue = jsonElementToToonValue(jsonElement)
        return writer.stringify(toonValue).serializedData
    }

    /**
     * Decodes a TOON string into a serializable value.
     *
     * @param deserializer deserializer for the target type
     * @param string TOON input
     * @return decoded value
     */
    override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String
    ): T {
        val toonValue = parser.parse(string).toonValue
        val jsonElement = toonValueToJsonElement(toonValue)
        return json.decodeFromJsonElement(deserializer, jsonElement)
    }

    /**
     * Encodes a serializable value into a TOON string using an inferred serializer.
     *
     * @param value value to encode
     * @return TOON string
     */
    inline fun <reified T> encodeToString(value: T): String {
        return encodeToString(serializer(), value)
    }

    /**
     * Decodes a TOON string using an inferred serializer.
     *
     * @param string TOON input
     * @return decoded value
     */
    inline fun <reified T> decodeFromString(string: String): T {
        return decodeFromString(serializer(), string)
    }

    /**
     * Converts a JsonElement tree into a ToonValue tree.
     *
     * @param element JsonElement source
     * @return ToonValue result
     */
    fun jsonElementToToonValue(element: JsonElement): ToonValue {
        return when (element) {
            is JsonObject -> ToonObject(
                element.entries.associate { (key, value) ->
                    key to jsonElementToToonValue(value)
                }
            )

            is JsonArray -> ToonArray(
                element.map { jsonElementToToonValue(it) }
            )

            is JsonNull -> ToonNull

            is JsonPrimitive -> jsonPrimitiveToToonValue(element)
        }
    }

    /**
     * Converts a primitive JsonElement into a ToonValue.
     *
     * @param primitive JsonPrimitive source
     * @return ToonValue primitive
     */
    private fun jsonPrimitiveToToonValue(primitive: JsonPrimitive): ToonValue {
        if (primitive.isString) {
            return ToonString(primitive.content)
        }

        primitive.booleanOrNull?.let {
            return ToonBoolean(it)
        }

        primitive.contentOrNull?.let { raw ->
            if (raw == "null") return ToonNull
            return ToonNumber(raw)
        }

        return ToonString(primitive.toString())
    }

    /**
     * Converts a ToonValue tree into a JsonElement tree.
     *
     * @param value ToonValue source
     * @return JsonElement result
     */
    fun toonValueToJsonElement(value: ToonValue): JsonElement {
        return when (value) {
            is ToonObject -> JsonObject(
                value.entries.mapValues { (_, child) ->
                    toonValueToJsonElement(child)
                }
            )

            is ToonArray -> JsonArray(
                value.values.map { toonValueToJsonElement(it) }
            )

            is ToonString -> JsonPrimitive(value.value)
            is ToonNumber -> toonNumberToJsonPrimitive(value)
            is ToonBoolean -> JsonPrimitive(value.value)
            ToonNull -> JsonNull
        }
    }

    /**
     * Converts a ToonNumber into a JsonPrimitive.
     *
     * This preserves integer and floating number semantics where possible.
     *
     * @param number ToonNumber source
     * @return JsonPrimitive number
     */
    private fun toonNumberToJsonPrimitive(number: ToonNumber): JsonPrimitive {
        number.raw.toIntOrNull()?.let {
            return JsonPrimitive(it)
        }

        number.raw.toLongOrNull()?.let {
            return JsonPrimitive(it)
        }

        number.raw.toDoubleOrNull()?.let {
            return JsonPrimitive(it)
        }

        return JsonPrimitive(number.raw)
    }
}

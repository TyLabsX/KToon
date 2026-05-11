package de.tylabsx.ktoon.kotlinx.native

import de.tylabsx.ktoon.KToonWriterEngine
import de.tylabsx.ktoon.ToonValue

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import kotlinx.serialization.modules.SerializersModule

/**
 * Native kotlinx.serialization format for TOON.
 *
 * This format encodes serializable Kotlin values directly into the internal
 * [ToonValue] tree with [KToonSerializationEncoder], then serializes that tree
 * with the existing TOON writer. Decoding parses TOON into [ToonValue] with the
 * native fast value parser and reads it with [KToonSerializationDecoder].
 *
 * Unlike the legacy JsonElement bridge, this implementation does not convert
 * through kotlinx.serialization JSON types. It is the primary format layer for
 * Kotlin-first TOON serialization.
 *
 * Example:
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val toon = KToonNativeFormat.encodeToString(User.serializer(), User(1, "Alice"))
 * val user = KToonNativeFormat.decodeFromString(User.serializer(), toon)
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
object KToonNativeFormat : StringFormat {

    private val writer = KToonWriterEngine()

    /**
     * Serializers module used by this format.
     */
    override val serializersModule: SerializersModule = SerializersModule {}

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
        return writer.stringify(encodeToToonValue(serializer, value)).serializedData
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
        return decodeFromToonValue(deserializer, KToonFastValueParser.parse(string))
    }

    /**
     * Encodes a value into a [ToonValue] tree without stringifying it.
     *
     * @param serializer serializer for the value
     * @param value value to encode
     * @return encoded ToonValue tree
     */
    fun <T> encodeToToonValue(
        serializer: SerializationStrategy<T>,
        value: T
    ): ToonValue {
        return KToonSerializationEncoder.encode(serializersModule) {
            encodeSerializableValue(serializer, value)
        }
    }

    /**
     * Decodes a [ToonValue] tree into a serializable Kotlin value.
     *
     * @param deserializer deserializer for the target type
     * @param value ToonValue source
     * @return decoded value
     */
    fun <T> decodeFromToonValue(
        deserializer: DeserializationStrategy<T>,
        value: ToonValue
    ): T {
        return KToonSerializationDecoder(value, serializersModule).decodeSerializableValue(deserializer)
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
     * Encodes a value into a [ToonValue] tree using an inferred serializer.
     *
     * @param value value to encode
     * @return encoded ToonValue tree
     */
    inline fun <reified T> encodeToToonValue(value: T): ToonValue {
        return encodeToToonValue(serializer(), value)
    }

    /**
     * Decodes a [ToonValue] tree using an inferred serializer.
     *
     * @param value ToonValue source
     * @return decoded value
     */
    inline fun <reified T> decodeFromToonValue(value: ToonValue): T {
        return decodeFromToonValue(serializer(), value)
    }
}

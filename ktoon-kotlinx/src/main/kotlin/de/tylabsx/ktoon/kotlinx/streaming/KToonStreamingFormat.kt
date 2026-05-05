package de.tylabsx.ktoon.kotlinx.streaming

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import kotlinx.serialization.modules.SerializersModule

/**
 * TOON string format for serializer-driven streaming encode.
 *
 * [KToonStreamingFormat] writes kotlinx.serialization output directly to a
 * [StringBuilder] through [KToonStreamingEncoder]. It does not create
 * [de.tylabsx.ktoon.ToonValue], does not bridge through
 * [kotlinx.serialization.json.JsonElement], does not invoke the KToon writer,
 * and does not use reflection.
 *
 * The format has two modes. [KToonStreamingMode.FAST] is speed-oriented and
 * avoids tabular object-array analysis. [KToonStreamingMode.COMPACT] is
 * size-oriented and buffers list items enough to render eligible object lists
 * as TOON tabular arrays.
 *
 * Decoding is intentionally not implemented in this streaming layer. Use
 * `KToonNativeFormat` for decoding TOON strings into serializable Kotlin
 * objects.
 *
 * Example:
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: Int, val name: String)
 *
 * val toon = KToonStreamingFormat.encodeToString(User.serializer(), User(1, "Alice"))
 * val inferred = KToonStreamingFormat.encodeToString(User(1, "Alice"))
 * ```
 *
 * @property options streaming formatting and strategy options
 * @property serializersModule serializers module used for contextual
 * serializers
 * @since 1.1.0
 * @author TyLabsX
 */
@ExperimentalKToonStreamingApi
class KToonStreamingFormat(
    private val options: KToonStreamingOptions = KToonStreamingOptions(),
    override val serializersModule: SerializersModule = SerializersModule {}
) : StringFormat {

    /**
     * Encodes a serializable value directly into a TOON string.
     *
     * @param serializer serializer for the value
     * @param value value to encode
     * @return TOON v2.1 string
     * @throws KToonStreamingSerializationException when the serializer emits an
     * unsupported construct such as a structured map key
     * @throws UnsupportedOperationException when polymorphic serialization is
     * requested
     */
    override fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T
    ): String {
        val builder = StringBuilder()
        KToonStreamingEncoder(builder, options, serializersModule).encode(serializer, value)

        return if (options.trailingNewline) {
            if (builder.endsWith('\n')) builder.toString() else builder.append('\n').toString()
        } else {
            builder.toString().trimEnd('\n')
        }
    }

    /**
     * Decoding is not available in the streaming format yet.
     *
     * @param deserializer deserializer for the target type
     * @param string TOON input
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String
    ): T {
        throw UnsupportedOperationException(
            "Streaming decode is not supported yet. Use KToonNativeFormat for decoding."
        )
    }

    /**
     * Encodes a serializable value using an inferred serializer.
     *
     * @param value value to encode
     * @return TOON v2.1 string
     */
    inline fun <reified T> encodeToString(value: T): String {
        return encodeToString(serializer(), value)
    }

    /**
     * Decoding is not available in the streaming format yet.
     *
     * @param string TOON input
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    inline fun <reified T> decodeFromString(string: String): T {
        return decodeFromString(serializer(), string)
    }

    private fun StringBuilder.endsWith(char: Char): Boolean {
        return isNotEmpty() && this[length - 1] == char
    }

    /**
     * Default streaming format instance exposed for concise static-style use.
     *
     * This companion uses [KToonStreamingOptions] defaults. Create an explicit
     * [KToonStreamingFormat] instance when FAST mode, trailing newlines, or
     * custom string quoting behavior is required.
     */
    companion object Default : StringFormat {
        private val defaultFormat = KToonStreamingFormat()

        /**
         * Serializers module used by the default streaming format.
         */
        override val serializersModule: SerializersModule = defaultFormat.serializersModule

        /**
         * Encodes a serializable value directly into a TOON string with default
         * streaming options.
         *
         * @param serializer serializer for the value
         * @param value value to encode
         * @return TOON v2.1 string
         */
        override fun <T> encodeToString(
            serializer: SerializationStrategy<T>,
            value: T
        ): String {
            return defaultFormat.encodeToString(serializer, value)
        }

        /**
         * Decoding is not available in the streaming format yet.
         *
         * @param deserializer deserializer for the target type
         * @param string TOON input
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        override fun <T> decodeFromString(
            deserializer: DeserializationStrategy<T>,
            string: String
        ): T {
            return defaultFormat.decodeFromString(deserializer, string)
        }

        /**
         * Encodes a serializable value using an inferred serializer and default
         * streaming options.
         *
         * @param value value to encode
         * @return TOON v2.1 string
         */
        inline fun <reified T> encodeToString(value: T): String {
            return encodeToString(serializer(), value)
        }

        /**
         * Decoding is not available in the streaming format yet.
         *
         * @param string TOON input
         * @return never returns
         * @throws UnsupportedOperationException always
         */
        inline fun <reified T> decodeFromString(string: String): T {
            return decodeFromString(serializer(), string)
        }
    }
}

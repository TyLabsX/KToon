package de.tylabsx.ktoon.processor

import de.tylabsx.ktoon.KToonWriterEngine
import de.tylabsx.ktoon.ToonValue
import de.tylabsx.ktoon.pipeline.*

/**
 * Converts TOON data structures to different output formats.
 *
 * This converter intentionally delegates serialization to KToonWriterEngine.
 * It does not implement JSON, XML, YAML or TOON serialization itself.
 *
 * This keeps KToon serialization logic centralized in one place and prevents
 * duplicated, inconsistent conversion behavior between modules.
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonConverter(
    private val writerEngine: KToonWriterEngine = KToonWriterEngine()
) {

    /**
     * Converts a ToonValue to the specified target format.
     *
     * @param value The ToonValue to convert
     * @param targetFormat The target format to convert to
     * @param options Conversion options
     * @return Converted data in the target format
     */
    fun convert(
        value: ToonValue,
        targetFormat: TargetFormat,
        options: ConversionOptions
    ): Any {
        return when (targetFormat) {
            TargetFormat.JSON -> toJson(value, options)
            TargetFormat.XML -> toXml(value, options)
            TargetFormat.YAML -> toYaml(value, options)
            TargetFormat.MAP -> toMap(value)
            TargetFormat.CUSTOM -> toCustom(value, options)
        }!!
    }

    /**
     * Converts a ToonValue to official TOON syntax.
     *
     * @param value The ToonValue to convert
     * @param options Conversion options
     * @return TOON string
     */
    fun toToon(value: ToonValue, options: ConversionOptions = ConversionOptions()): String {
        return writerEngine.stringify(
            value = value,
            options = WriterOptions(
                pretty = options.pretty,
                indentSize = options.indentSize
            )
        ).serializedData
    }

    /**
     * Converts a ToonValue to JSON.
     *
     * @param value The ToonValue to convert
     * @param options Conversion options
     * @return JSON string
     */
    fun toJson(value: ToonValue, options: ConversionOptions = ConversionOptions()): String {
        return writerEngine.toJson(
            value = value,
            options = JsonOptions(
                pretty = options.pretty,
                indentSize = options.indentSize
            )
        ).serializedData
    }

    /**
     * Converts a ToonValue to XML.
     *
     * @param value The ToonValue to convert
     * @param options Conversion options
     * @return XML string
     */
    fun toXml(value: ToonValue, options: ConversionOptions = ConversionOptions()): String {
        return writerEngine.toXml(
            value = value,
            options = XmlOptions(
                pretty = options.pretty,
                indentSize = options.indentSize
            )
        ).serializedData
    }

    /**
     * Converts a ToonValue to YAML.
     *
     * @param value The ToonValue to convert
     * @param options Conversion options
     * @return YAML string
     */
    fun toYaml(value: ToonValue, options: ConversionOptions = ConversionOptions()): String {
        return writerEngine.toYaml(
            value = value,
            options = YamlOptions(
                indentSize = options.indentSize
            )
        ).serializedData
    }

    /**
     * Converts a ToonValue to plain Kotlin data structures.
     *
     * Object values become Map<String, Any?>.
     * Array values become List<Any?>.
     * Primitive values become their native Kotlin equivalents.
     *
     * @param value The ToonValue to convert
     * @return Native Kotlin representation
     */
    fun toNative(value: ToonValue): Any? {
        return when (value) {
            is de.tylabsx.ktoon.ToonObject -> {
                value.entries.mapValues { (_, child) -> toNative(child) }
            }

            is de.tylabsx.ktoon.ToonArray -> {
                value.values.map { child -> toNative(child) }
            }

            is de.tylabsx.ktoon.ToonString -> value.value
            is de.tylabsx.ktoon.ToonNumber -> value.raw
            is de.tylabsx.ktoon.ToonBoolean -> value.value
            is de.tylabsx.ktoon.ToonNull -> null
        }
    }

    /**
     * Converts a ToonValue to map format.
     *
     * For root objects this returns their native map representation.
     * For non-object root values this wraps the value into a map under "value".
     *
     * @param value The ToonValue to convert
     * @return Map representation
     */
    fun toMap(value: ToonValue): Map<String, Any?> {
        val native = toNative(value)

        return when (native) {
            is Map<*, *> -> native.entries.associate { (key, mapValue) ->
                key.toString() to mapValue
            }

            else -> mapOf("value" to native)
        }
    }

    /**
     * Converts a ToonValue to a custom representation.
     *
     * The default custom conversion currently returns the native Kotlin
     * representation. This can later be replaced by user-provided converter
     * strategies without duplicating serializer logic.
     *
     * @param value The ToonValue to convert
     * @param options Conversion options
     * @return Custom representation
     */
    fun toCustom(value: ToonValue, options: ConversionOptions = ConversionOptions()): Any? {
        return toNative(value)
    }
}
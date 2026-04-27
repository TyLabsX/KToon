package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.*
import de.tylabsx.ktoon.serializer.JsonSerializer
import de.tylabsx.ktoon.serializer.ToonSerializer
import de.tylabsx.ktoon.serializer.XmlSerializer
import de.tylabsx.ktoon.serializer.YamlSerializer
import de.tylabsx.ktoon.streamer.StreamWriter

/**
 * Central serialization engine for KToon.
 *
 * This engine serializes ToonValue structures into official TOON v2.1 output.
 *
 * Important:
 * TOON output must not be post-processed by generic pretty printers because
 * indentation and array headers are semantic parts of the format.
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonWriterEngine {

    private val toonSerializer = ToonSerializer()
    private val jsonSerializer = JsonSerializer()
    private val xmlSerializer = XmlSerializer()
    private val yamlSerializer = YamlSerializer()
    private val streamWriter = StreamWriter()

    /**
     * Converts a ToonValue into official TOON v2.1 syntax.
     *
     * @param value value to serialize
     * @param options writer options
     * @return writer result containing valid TOON output
     */
    fun stringify(value: ToonValue, options: WriterOptions = WriterOptions()): WriterResult {
        return try {
            validateToonValue(value)

            val serialized = toonSerializer.serialize(value, options)

            WriterResult(
                serializedData = serialized,
                originalValue = value,
                format = OutputFormat.TOON,
                options = options,
                serializationTime = System.currentTimeMillis(),
                dataSize = serialized.length.toLong()
            )
        } catch (e: Exception) {
            throw ToonParseException(
                message = "TOON serialization failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Converts a ToonValue to JSON.
     *
     * @param value value to serialize
     * @param options JSON options
     * @return writer result
     */
    fun toJson(value: ToonValue, options: JsonOptions = JsonOptions()): WriterResult {
        return try {
            validateToonValue(value)
            val serialized = jsonSerializer.serialize(value, options)

            WriterResult(
                serializedData = serialized,
                originalValue = value,
                format = OutputFormat.JSON,
                options = WriterOptions(pretty = options.pretty, indentSize = options.indentSize),
                serializationTime = System.currentTimeMillis(),
                dataSize = serialized.length.toLong()
            )
        } catch (e: Exception) {
            throw ToonParseException(
                message = "JSON serialization failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Converts a ToonValue to XML.
     *
     * @param value value to serialize
     * @param options XML options
     * @return writer result
     */
    fun toXml(value: ToonValue, options: XmlOptions = XmlOptions()): WriterResult {
        return try {
            validateToonValue(value)
            val serialized = xmlSerializer.serialize(value, options)

            WriterResult(
                serializedData = serialized,
                originalValue = value,
                format = OutputFormat.XML,
                options = WriterOptions(pretty = options.pretty, indentSize = options.indentSize),
                serializationTime = System.currentTimeMillis(),
                dataSize = serialized.length.toLong()
            )
        } catch (e: Exception) {
            throw ToonParseException(
                message = "XML serialization failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Converts a ToonValue to YAML.
     *
     * @param value value to serialize
     * @param options YAML options
     * @return writer result
     */
    fun toYaml(value: ToonValue, options: YamlOptions = YamlOptions()): WriterResult {
        return try {
            validateToonValue(value)
            val serialized = yamlSerializer.serialize(value, options)

            WriterResult(
                serializedData = serialized,
                originalValue = value,
                format = OutputFormat.YAML,
                options = WriterOptions(pretty = true, indentSize = options.indentSize),
                serializationTime = System.currentTimeMillis(),
                dataSize = serialized.length.toLong()
            )
        } catch (e: Exception) {
            throw ToonParseException(
                message = "YAML serialization failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Streams a ToonValue to an Appendable.
     *
     * @param value value to stream
     * @param output output destination
     * @param options streaming options
     * @return streaming result
     */
    fun stream(
        value: ToonValue,
        output: Appendable,
        options: StreamingOptions = StreamingOptions()
    ): StreamingResult {
        return try {
            validateToonValue(value)
            val startTime = System.currentTimeMillis()

            streamWriter.stream(value, output, options)

            StreamingResult(
                originalValue = value,
                output = output,
                options = options,
                streamingTime = System.currentTimeMillis() - startTime,
                bytesWritten = options.estimateSize(value)
            )
        } catch (e: Exception) {
            throw ToonParseException(
                message = "Streaming failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Validates a ToonValue before serialization.
     *
     * @param value value to validate
     */
    private fun validateToonValue(value: ToonValue) {
        when (value) {
            is ToonObject -> {
                value.entries.forEach { (key, childValue) ->
                    if (key.isEmpty()) {
                        throw ToonParseException(
                            message = "Empty keys are not allowed in TOON objects",
                            line = 0,
                            column = 0
                        )
                    }

                    validateToonValue(childValue)
                }
            }

            is ToonArray -> value.values.forEach { validateToonValue(it) }

            is ToonNumber -> {
                if (value.raw.isEmpty()) {
                    throw ToonParseException(
                        message = "Empty number values are not allowed",
                        line = 0,
                        column = 0
                    )
                }
            }

            is ToonString,
            is ToonBoolean,
            is ToonNull -> Unit
        }
    }

    /**
     * Returns metadata about writer capabilities.
     *
     * @return writer statistics
     */
    fun getWriterStatistics(): WriterStatistics {
        return WriterStatistics(
            supportedFormats = listOf("TOON", "JSON", "XML", "YAML"),
            features = listOf(
                "Official TOON v2.1 object output",
                "Official TOON v2.1 primitive arrays",
                "Official TOON v2.1 tabular arrays",
                "Mixed array list format",
                "Root object",
                "Root array",
                "Root primitive"
            ),
            maxDataSize = Long.MAX_VALUE
        )
    }
}

/**
 * Result of a writer operation.
 *
 * @property serializedData serialized output
 * @property originalValue original value
 * @property format output format
 * @property options writer options
 * @property serializationTime timestamp
 * @property dataSize serialized size
 */
data class WriterResult(
    val serializedData: String,
    val originalValue: ToonValue,
    val format: OutputFormat,
    val options: WriterOptions,
    val serializationTime: Long,
    val dataSize: Long
)

/**
 * Result of a streaming operation.
 *
 * @property originalValue original value
 * @property output output target
 * @property options streaming options
 * @property streamingTime duration in milliseconds
 * @property bytesWritten estimated bytes written
 */
data class StreamingResult(
    val originalValue: ToonValue,
    val output: Appendable,
    val options: StreamingOptions,
    val streamingTime: Long,
    val bytesWritten: Long
)

/**
 * Writer capability metadata.
 *
 * @property supportedFormats supported formats
 * @property features supported features
 * @property maxDataSize maximum supported data size
 */
data class WriterStatistics(
    val supportedFormats: List<String>,
    val features: List<String>,
    val maxDataSize: Long
)

/**
 * Supported output formats.
 */
enum class OutputFormat {
    TOON,
    JSON,
    XML,
    YAML
}
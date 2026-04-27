package de.tylabsx.ktoon.streamer

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.StreamingOptions

/**
 * Streams ToonValue data to output destinations.
 * 
 * This class handles memory-efficient streaming of TOON data
 * structures, writing data incrementally to avoid loading
 * entire structures into memory.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class StreamWriter {

    /**
     * Streams a ToonValue to an output destination.
     * 
     * @param value The ToonValue to stream
     * @param output The output destination
     * @param options Streaming options
     */
    fun stream(value: ToonValue, output: Appendable, options: StreamingOptions) {
        val buffer = StringBuilder()

        if (options.includeLineNumbers) {
            streamWithLineNumbers(value, output, buffer, options)
        } else {
            streamWithoutLineNumbers(value, output, buffer, options)
        }

        // Flush any remaining content
        output.append(buffer.toString())
    }

    /**
     * Streams a ToonValue with line numbers included.
     * 
     * @param value The ToonValue to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     */
    private fun streamWithLineNumbers(
        value: ToonValue,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions
    ) {
        var lineNumber = 1
        streamValue(value, output, buffer, options) { content ->
            output.append("${lineNumber}: $content")
            lineNumber++
        }
    }

    /**
     * Streams a ToonValue without line numbers.
     * 
     * @param value The ToonValue to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     */
    private fun streamWithoutLineNumbers(
        value: ToonValue,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions
    ) {
        streamValue(value, output, buffer, options) { content ->
            output.append(content)
        }
    }

    /**
     * Streams a ToonValue using the provided content handler.
     * 
     * @param value The ToonValue to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamValue(
        value: ToonValue,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        when (value) {
            is ToonObject -> streamObject(value, output, buffer, options, contentHandler)
            is ToonArray -> streamArray(value, output, buffer, options, contentHandler)
            is ToonString -> streamString(value, output, buffer, options, contentHandler)
            is ToonNumber -> streamNumber(value, output, buffer, options, contentHandler)
            is ToonBoolean -> streamBoolean(value, output, buffer, options, contentHandler)
            is ToonNull -> streamNull(output, buffer, options, contentHandler)
        }
    }

    /**
     * Streams a ToonObject.
     * 
     * @param obj The ToonObject to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamObject(
        obj: ToonObject,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        obj.entries.forEach { (key, value) ->
            buffer.clear()
            buffer.append(key).append(":")

            when (value) {
                is ToonObject -> {
                    contentHandler(buffer.toString())
                    streamValue(value, output, buffer, options, contentHandler)
                }

                is ToonArray -> {
                    buffer.append(" ")
                    contentHandler(buffer.toString())
                    streamValue(value, output, buffer, options, contentHandler)
                }

                else -> {
                    buffer.append(" ")
                    contentHandler(buffer.toString())
                }
            }
        }
    }

    /**
     * Streams a ToonArray.
     * 
     * @param array The ToonArray to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamArray(
        array: ToonArray,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        array.values.forEach { value ->
            buffer.clear()
            streamValue(value, output, buffer, options, contentHandler)
        }
    }

    /**
     * Streams a ToonString.
     * 
     * @param str The ToonString to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamString(
        str: ToonString,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        buffer.clear()
        buffer.append(str.value)
        contentHandler(buffer.toString())
    }

    /**
     * Streams a ToonNumber.
     * 
     * @param num The ToonNumber to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamNumber(
        num: ToonNumber,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        buffer.clear()
        buffer.append(num.raw)
        contentHandler(buffer.toString())
    }

    /**
     * Streams a ToonBoolean.
     * 
     * @param bool The ToonBoolean to stream
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamBoolean(
        bool: ToonBoolean,
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        buffer.clear()
        buffer.append(bool.value)
        contentHandler(buffer.toString())
    }

    /**
     * Streams ToonNull.
     * 
     * @param output The output destination
     * @param buffer Buffer for building content
     * @param options Streaming options
     * @param contentHandler Function to handle generated content
     */
    private fun streamNull(
        output: Appendable,
        buffer: StringBuilder,
        options: StreamingOptions,
        contentHandler: (String) -> Unit
    ) {
        buffer.clear()
        buffer.append("null")
        contentHandler(buffer.toString())
    }
}

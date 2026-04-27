package de.tylabsx.ktoon.serializer

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.JsonOptions

/**
 * Serializes ToonValue structures to JSON format.
 * 
 * This class handles conversion from ToonValue instances to
 * valid JSON strings with proper formatting and escaping.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class JsonSerializer {

    /**
     * Serializes a ToonValue to JSON format string.
     * 
     * @param value The ToonValue to serialize
     * @param options JSON formatting options
     * @return JSON format string
     */
    fun serialize(value: ToonValue, options: JsonOptions): String {
        val builder = StringBuilder()
        serializeValue(value, builder, options, 0)
        return builder.toString()
    }

    /**
     * Serializes a ToonValue to JSON with proper indentation.
     * 
     * @param value The ToonValue to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     * @param depth Current indentation depth
     */
    private fun serializeValue(
        value: ToonValue,
        builder: StringBuilder,
        options: JsonOptions,
        depth: Int
    ) {
        when (value) {
            is ToonObject -> serializeObject(value, builder, options, depth)
            is ToonArray -> serializeArray(value, builder, options, depth)
            is ToonString -> serializeString(value, builder, options)
            is ToonNumber -> serializeNumber(value, builder, options)
            is ToonBoolean -> serializeBoolean(value, builder, options)
            is ToonNull -> serializeNull(builder, options)
        }
    }

    /**
     * Serializes a ToonObject to JSON.
     * 
     * @param obj The ToonObject to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     * @param depth Current indentation depth
     */
    private fun serializeObject(
        obj: ToonObject,
        builder: StringBuilder,
        options: JsonOptions,
        depth: Int
    ) {
        builder.append('{')

        if (options.pretty && obj.entries.isNotEmpty()) {
            builder.append('\n')
        }

        obj.entries.toList().forEachIndexed { index: Int, (key: String, value: ToonValue) ->
            if (options.pretty) {
                builder.append(" ".repeat((depth + 1) * options.indentSize))
            }

            builder.append('"').append(escapeJsonString(key)).append('"')
            builder.append(':')

            if (options.pretty) {
                builder.append(' ')
            }

            serializeValue(value, builder, options, depth + 1)

            if (index < obj.entries.size - 1) {
                builder.append(',')
            }

            if (options.pretty && index < obj.entries.size - 1) {
                builder.append('\n')
            }
        }

        if (options.pretty && obj.entries.isNotEmpty()) {
            builder.append('\n')
            builder.append(" ".repeat(depth * options.indentSize))
        }

        builder.append('}')
    }

    /**
     * Serializes a ToonArray to JSON.
     * 
     * @param array The ToonArray to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     * @param depth Current indentation depth
     */
    private fun serializeArray(
        array: ToonArray,
        builder: StringBuilder,
        options: JsonOptions,
        depth: Int
    ) {
        builder.append('[')

        if (options.pretty && array.values.isNotEmpty()) {
            builder.append('\n')
        }

        array.values.forEachIndexed { index: Int, value: ToonValue ->
            if (options.pretty) {
                builder.append(" ".repeat((depth + 1) * options.indentSize))
            }

            serializeValue(value, builder, options, depth + 1)

            if (index < array.values.size - 1) {
                builder.append(',')
            }

            if (options.pretty && index < array.values.size - 1) {
                builder.append('\n')
            }
        }

        if (options.pretty && array.values.isNotEmpty()) {
            builder.append('\n')
            builder.append(" ".repeat(depth * options.indentSize))
        }

        builder.append(']')
    }

    /**
     * Serializes a ToonString to JSON.
     * 
     * @param str The ToonString to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     */
    private fun serializeString(
        str: ToonString,
        builder: StringBuilder,
        options: JsonOptions
    ) {
        builder.append('"').append(escapeJsonString(str.value)).append('"')
    }

    /**
     * Serializes a ToonNumber to JSON.
     * 
     * @param num The ToonNumber to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     */
    private fun serializeNumber(
        num: ToonNumber,
        builder: StringBuilder,
        options: JsonOptions
    ) {
        builder.append(num.raw)
    }

    /**
     * Serializes a ToonBoolean to JSON.
     * 
     * @param bool The ToonBoolean to serialize
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     */
    private fun serializeBoolean(
        bool: ToonBoolean,
        builder: StringBuilder,
        options: JsonOptions
    ) {
        builder.append(bool.value)
    }

    /**
     * Serializes ToonNull to JSON.
     * 
     * @param builder StringBuilder to write to
     * @param options JSON formatting options
     */
    private fun serializeNull(
        builder: StringBuilder,
        options: JsonOptions
    ) {
        builder.append("null")
    }

    /**
     * Escapes a string for JSON output.
     * 
     * @param str The string to escape
     * @return Escaped JSON string
     */
    private fun escapeJsonString(str: String): String {
        val builder = StringBuilder()

        for (char in str) {
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    val code = char.code
                    if (code < 0x20 || code > 0x7E) {
                        builder.append(String.format("\\u%04x", code))
                    } else {
                        builder.append(char)
                    }
                }
            }
        }

        return builder.toString()
    }
}

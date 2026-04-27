package de.tylabsx.ktoon.serializer

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.YamlOptions

/**
 * Serializes ToonValue structures to YAML format.
 * 
 * This class handles conversion from ToonValue instances to
 * valid YAML strings with proper formatting and structure.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class YamlSerializer {

    /**
     * Serializes a ToonValue to YAML format string.
     * 
     * @param value The ToonValue to serialize
     * @param options YAML formatting options
     * @return YAML format string
     */
    fun serialize(value: ToonValue, options: YamlOptions): String {
        val builder = StringBuilder()
        serializeValue(value, builder, options, 0)
        return builder.toString()
    }

    /**
     * Serializes a ToonValue to YAML with proper indentation.
     * 
     * @param value The ToonValue to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeValue(
        value: ToonValue,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        when (value) {
            is ToonObject -> serializeObject(value, builder, options, depth)
            is ToonArray -> serializeArray(value, builder, options, depth)
            is ToonString -> serializeString(value, builder, options, depth)
            is ToonNumber -> serializeNumber(value, builder, options, depth)
            is ToonBoolean -> serializeBoolean(value, builder, options, depth)
            is ToonNull -> serializeNull(builder, options, depth)
        }
    }

    /**
     * Serializes a ToonObject to YAML.
     * 
     * @param obj The ToonObject to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeObject(
        obj: ToonObject,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        if (obj.entries.isEmpty()) {
            builder.append("{}")
            return
        }

        obj.entries.toList().forEachIndexed { index: Int, (key: String, value: ToonValue) ->
            writeIndent(builder, depth, options)
            builder.append(escapeYamlKey(key)).append(":")

            when (value) {
                is ToonObject, is ToonArray -> {
                    builder.append('\n')
                    serializeValue(value, builder, options, depth + 1)
                }

                else -> {
                    builder.append(' ')
                    serializeValue(value, builder, options, 0)
                }
            }

            if (index < obj.entries.size - 1) {
                builder.append('\n')
            }
        }
    }

    /**
     * Serializes a ToonArray to YAML.
     * 
     * @param array The ToonArray to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeArray(
        array: ToonArray,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        if (array.values.isEmpty()) {
            builder.append("[]")
            return
        }

        builder.append('\n')
        array.values.forEachIndexed { index, value ->
            writeIndent(builder, depth + 1, options)
            builder.append("- ")

            when (value) {
                is ToonObject, is ToonArray -> {
                    builder.append('\n')
                    serializeValue(value, builder, options, depth + 2)
                }

                else -> {
                    serializeValue(value, builder, options, 0)
                }
            }

            if (index < array.values.size - 1) {
                builder.append('\n')
            }
        }
    }

    /**
     * Serializes a ToonString to YAML.
     * 
     * @param str The ToonString to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeString(
        str: ToonString,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        val value = str.value

        when {
            value.isEmpty() -> builder.append("''")
            needsQuoting(value) -> builder.append('"').append(escapeYamlString(value)).append('"')
            else -> builder.append(value)
        }
    }

    /**
     * Serializes a ToonNumber to YAML.
     * 
     * @param num The ToonNumber to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeNumber(
        num: ToonNumber,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        builder.append(num.raw)
    }

    /**
     * Serializes a ToonBoolean to YAML.
     * 
     * @param bool The ToonBoolean to serialize
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeBoolean(
        bool: ToonBoolean,
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        builder.append(bool.value.toString())
    }

    /**
     * Serializes ToonNull to YAML.
     * 
     * @param builder StringBuilder to write to
     * @param options YAML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeNull(
        builder: StringBuilder,
        options: YamlOptions,
        depth: Int
    ) {
        builder.append("null")
    }

    /**
     * Writes indentation to builder.
     * 
     * @param builder StringBuilder to write to
     * @param depth Indentation depth
     * @param options YAML formatting options
     */
    private fun writeIndent(builder: StringBuilder, depth: Int, options: YamlOptions) {
        builder.append(" ".repeat(depth * options.indentSize))
    }

    /**
     * Checks if a string needs to be quoted in YAML.
     * 
     * @param value The string to check
     * @return true if quoting is needed, false otherwise
     */
    private fun needsQuoting(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.count { it == '.' } > 1 && value.any { it.isDigit() }) return true

        return value.any { char ->
            char.isWhitespace() || char == ':' || char == '[' || char == ']' ||
                    char == '{' || char == '}' || char == ',' || char == '#' ||
                    char == '"' || char == '\'' || char.isISOControl()
        }
    }

    /**
     * Escapes a YAML key.
     * 
     * @param key The key to escape
     * @return Escaped YAML key
     */
    private fun escapeYamlKey(key: String): String {
        return if (needsQuoting(key)) {
            '"' + escapeYamlString(key) + '"'
        } else {
            key
        }
    }

    /**
     * Escapes a string for YAML content.
     * 
     * @param str The string to escape
     * @return Escaped YAML string
     */
    private fun escapeYamlString(str: String): String {
        val builder = StringBuilder()

        for (char in str) {
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\n' -> builder.append("\\n")
                '\t' -> builder.append("\\t")
                '\r' -> builder.append("\\r")
                else -> builder.append(char)
            }
        }

        return builder.toString()
    }
}

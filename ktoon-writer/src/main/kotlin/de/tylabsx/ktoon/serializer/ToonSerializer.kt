package de.tylabsx.ktoon.serializer

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.WriterOptions

/**
 * Serializes ToonValue structures into official TOON v2.1 syntax.
 *
 * Supported output:
 *
 * - root objects
 * - root arrays
 * - root primitives
 * - nested objects
 * - primitive arrays using key[N]: a,b,c
 * - tabular object arrays using key[N]{field1,field2}:
 * - mixed arrays using list item format
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonSerializer {

    /**
     * Serializes any ToonValue into TOON v2.1 syntax.
     *
     * @param value value to serialize
     * @param options writer options
     * @return TOON document
     */
    fun serialize(value: ToonValue, options: WriterOptions): String {
        return when (value) {
            is ToonObject -> serializeRootObject(value, options)
            is ToonArray -> serializeRootArray(value, options)
            else -> serializePrimitive(value, delimiter = ',')
        }
    }

    /**
     * Serializes a root object.
     *
     * @param obj object to serialize
     * @param options writer options
     * @return TOON object document
     */
    private fun serializeRootObject(obj: ToonObject, options: WriterOptions): String {
        if (obj.entries.isEmpty()) return ""

        return obj.entries.entries.joinToString("\n") { (key, value) ->
            serializeField(
                key = key,
                value = value,
                depth = 0,
                options = options
            )
        }
    }

    /**
     * Serializes a root array.
     *
     * @param array array to serialize
     * @param options writer options
     * @return TOON array document
     */
    private fun serializeRootArray(array: ToonArray, options: WriterOptions): String {
        return when {
            canUsePrimitiveInlineArray(array) -> {
                "[${array.values.size}]: ${serializeInlineArrayValues(array.values)}"
            }

            canUseTabularArray(array) -> {
                val fields = tabularFields(array)
                buildString {
                    append("[${array.values.size}]{${fields.joinToString(",") { encodeKey(it) }}}:")
                    appendRowsForTabularArray(array, fields, depth = 1, options = options)
                }
            }

            else -> {
                buildString {
                    append("[${array.values.size}]:")
                    appendListItems(array, depth = 1, options = options)
                }
            }
        }
    }

    /**
     * Serializes one object field.
     *
     * @param key field key
     * @param value field value
     * @param depth indentation depth
     * @param options writer options
     * @return serialized field
     */
    private fun serializeField(
        key: String,
        value: ToonValue,
        depth: Int,
        options: WriterOptions
    ): String {
        val indent = indent(depth, options)
        val encodedKey = encodeKey(key)

        return when (value) {
            is ToonObject -> {
                if (value.entries.isEmpty()) {
                    "$indent$encodedKey:"
                } else {
                    buildString {
                        append("$indent$encodedKey:")
                        value.entries.entries.forEach { (childKey, childValue) ->
                            append('\n')
                            append(
                                serializeField(
                                    key = childKey,
                                    value = childValue,
                                    depth = depth + 1,
                                    options = options
                                )
                            )
                        }
                    }
                }
            }

            is ToonArray -> serializeArrayField(
                key = encodedKey,
                array = value,
                depth = depth,
                options = options
            )

            else -> "$indent$encodedKey: ${serializePrimitive(value, delimiter = ',')}"
        }
    }

    /**
     * Serializes an array field.
     *
     * @param key already encoded key
     * @param array array value
     * @param depth indentation depth
     * @param options writer options
     * @return serialized array field
     */
    private fun serializeArrayField(
        key: String,
        array: ToonArray,
        depth: Int,
        options: WriterOptions
    ): String {
        val indent = indent(depth, options)

        return when {
            canUsePrimitiveInlineArray(array) -> {
                "$indent$key[${array.values.size}]: ${serializeInlineArrayValues(array.values)}"
            }

            canUseTabularArray(array) -> {
                val fields = tabularFields(array)

                buildString {
                    append("$indent$key[${array.values.size}]{")
                    append(fields.joinToString(",") { encodeKey(it) })
                    append("}:")
                    appendRowsForTabularArray(array, fields, depth = depth + 1, options = options)
                }
            }

            else -> {
                buildString {
                    append("$indent$key[${array.values.size}]:")
                    appendListItems(array, depth = depth + 1, options = options)
                }
            }
        }
    }

    /**
     * Appends rows for a tabular array.
     *
     * @param array array of objects
     * @param fields tabular fields
     * @param depth row indentation depth
     * @param options writer options
     */
    private fun StringBuilder.appendRowsForTabularArray(
        array: ToonArray,
        fields: List<String>,
        depth: Int,
        options: WriterOptions
    ) {
        array.values.forEach { value ->
            val obj = value as ToonObject
            append('\n')
            append(indent(depth, options))
            append(
                fields.joinToString(",") { field ->
                    serializePrimitive(obj.entries[field] ?: ToonNull, delimiter = ',')
                }
            )
        }
    }

    /**
     * Appends list-format array items.
     *
     * @param array array to serialize
     * @param depth item indentation depth
     * @param options writer options
     */
    private fun StringBuilder.appendListItems(
        array: ToonArray,
        depth: Int,
        options: WriterOptions
    ) {
        array.values.forEach { value ->
            append('\n')
            append(serializeListItem(value, depth, options))
        }
    }

    /**
     * Serializes one list-format array item.
     *
     * @param value item value
     * @param depth indentation depth
     * @param options writer options
     * @return serialized list item
     */
    private fun serializeListItem(
        value: ToonValue,
        depth: Int,
        options: WriterOptions
    ): String {
        val indent = indent(depth, options)

        return when (value) {
            is ToonObject -> serializeObjectListItem(value, depth, options)

            is ToonArray -> {
                if (canUsePrimitiveInlineArray(value)) {
                    "$indent- [${value.values.size}]: ${serializeInlineArrayValues(value.values)}"
                } else {
                    buildString {
                        append("$indent- [${value.values.size}]:")
                        appendListItems(value, depth + 1, options)
                    }
                }
            }

            else -> "$indent- ${serializePrimitive(value, delimiter = ',')}"
        }
    }

    /**
     * Serializes an object inside list-item format.
     *
     * @param obj object item
     * @param depth indentation depth
     * @param options writer options
     * @return serialized object list item
     */
    private fun serializeObjectListItem(
        obj: ToonObject,
        depth: Int,
        options: WriterOptions
    ): String {
        val indent = indent(depth, options)

        if (obj.entries.isEmpty()) {
            return "$indent-"
        }

        val entries = obj.entries.entries.toList()
        val first = entries.first()

        val builder = StringBuilder()
        builder.append(indent)
        builder.append("- ")

        when (val firstValue = first.value) {
            is ToonObject -> {
                builder.append(encodeKey(first.key))
                builder.append(":")
                if (firstValue.entries.isNotEmpty()) {
                    firstValue.entries.entries.forEach { (childKey, childValue) ->
                        builder.append('\n')
                        builder.append(
                            serializeField(
                                key = childKey,
                                value = childValue,
                                depth = depth + 2,
                                options = options
                            )
                        )
                    }
                }
            }

            is ToonArray -> {
                val serialized = serializeArrayField(
                    key = encodeKey(first.key),
                    array = firstValue,
                    depth = 0,
                    options = options
                ).trimStart()

                builder.append(serialized)
            }

            else -> {
                builder.append(encodeKey(first.key))
                builder.append(": ")
                builder.append(serializePrimitive(firstValue, delimiter = ','))
            }
        }

        entries.drop(1).forEach { (key, value) ->
            builder.append('\n')
            builder.append(
                serializeField(
                    key = key,
                    value = value,
                    depth = depth + 1,
                    options = options
                )
            )
        }

        return builder.toString()
    }

    /**
     * Checks if an array can be rendered inline.
     *
     * @param array array to inspect
     * @return true if all values are primitive
     */
    private fun canUsePrimitiveInlineArray(array: ToonArray): Boolean {
        return array.values.all { it.isPrimitive() }
    }

    /**
     * Checks if an array can be rendered as a tabular object array.
     *
     * @param array array to inspect
     * @return true if all objects share the same primitive-valued keys
     */
    private fun canUseTabularArray(array: ToonArray): Boolean {
        if (array.values.isEmpty()) return false
        if (array.values.any { it !is ToonObject }) return false

        val objects = array.values.map { it as ToonObject }
        val firstKeys = objects.first().entries.keys.toList()

        if (firstKeys.isEmpty()) return false

        return objects.all { obj ->
            obj.entries.keys.toSet() == firstKeys.toSet() &&
                    obj.entries.values.all { it.isPrimitive() }
        }
    }

    /**
     * Returns the tabular fields for an array.
     *
     * @param array tabular array
     * @return fields from the first object
     */
    private fun tabularFields(array: ToonArray): List<String> {
        return (array.values.first() as ToonObject).entries.keys.toList()
    }

    /**
     * Serializes inline primitive array values.
     *
     * @param values primitive values
     * @return comma-separated TOON values
     */
    private fun serializeInlineArrayValues(values: List<ToonValue>): String {
        return values.joinToString(",") { serializePrimitive(it, delimiter = ',') }
    }

    /**
     * Serializes a primitive TOON value.
     *
     * @param value primitive value
     * @param delimiter active delimiter
     * @return serialized primitive
     */
    private fun serializePrimitive(value: ToonValue, delimiter: Char): String {
        return when (value) {
            is ToonString -> serializeString(value.value, delimiter)
            is ToonNumber -> value.raw
            is ToonBoolean -> value.value.toString()
            is ToonNull -> "null"
            is ToonObject,
            is ToonArray -> throw ToonParseException(
                message = "Expected primitive value but got ${value::class.simpleName}",
                line = 0,
                column = 0
            )
        }
    }

    /**
     * Serializes a string according to TOON quoting rules.
     *
     * @param value raw string
     * @param delimiter active delimiter
     * @return serialized string
     */
    private fun serializeString(value: String, delimiter: Char): String {
        return if (needsQuoting(value, delimiter)) {
            quote(value)
        } else {
            value
        }
    }

    /**
     * Encodes an object key.
     *
     * @param key raw key
     * @return encoded key
     */
    private fun encodeKey(key: String): String {
        return if (needsQuoting(key, delimiter = ',')) quote(key) else key
    }

    /**
     * Checks whether a string must be quoted.
     *
     * @param value raw value
     * @param delimiter active delimiter
     * @return true if quotes are required
     */
    private fun needsQuoting(value: String, delimiter: Char): Boolean {
        if (value.isEmpty()) return true
        if (value.first().isWhitespace() || value.last().isWhitespace()) return true
        if (value == "true" || value == "false" || value == "null") return true
        if (value == "-" || (value.startsWith("-") && value.length > 1)) return true
        if (looksLikeNumber(value)) return true

        return value.any { char ->
            char == ':' ||
                    char == '"' ||
                    char == '\\' ||
                    char == '[' ||
                    char == ']' ||
                    char == '{' ||
                    char == '}' ||
                    char == delimiter ||
                    char == '\n' ||
                    char == '\r' ||
                    char == '\t' ||
                    char.isISOControl()
        }
    }

    /**
     * Checks if a string looks like a number.
     *
     * @param value raw string
     * @return true if the value should be quoted to remain a string
     */
    private fun looksLikeNumber(value: String): Boolean {
        return NUMBER_REGEX.matches(value)
    }

    /**
     * Quotes and escapes a string.
     *
     * TOON v2.1 only allows these escape sequences:
     * \\, \", \n, \r, \t
     *
     * @param value raw string
     * @return quoted string
     */
    private fun quote(value: String): String {
        val builder = StringBuilder()
        builder.append('"')

        value.forEach { char ->
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }

        builder.append('"')
        return builder.toString()
    }

    /**
     * Checks if a value is primitive in the TOON data model.
     *
     * @return true if primitive
     */
    private fun ToonValue.isPrimitive(): Boolean {
        return this is ToonString ||
                this is ToonNumber ||
                this is ToonBoolean ||
                this is ToonNull
    }

    /**
     * Creates indentation for a depth.
     *
     * @param depth nesting depth
     * @param options writer options
     * @return indentation string
     */
    private fun indent(depth: Int, options: WriterOptions): String {
        return " ".repeat(depth * options.indentSize)
    }

    private companion object {
        private val NUMBER_REGEX = Regex(
            pattern = """-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?"""
        )
    }
}
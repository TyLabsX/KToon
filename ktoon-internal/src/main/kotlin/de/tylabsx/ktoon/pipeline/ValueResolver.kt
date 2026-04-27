package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.*

/**
 * Resolves raw string values into appropriate ToonValue instances.
 * 
 * This class handles the seventh phase of the TOON parsing pipeline,
 * taking raw string values and converting them into the appropriate
 * ToonValue types based on their content and type inference rules.
 * 
 * The resolver handles:
 * - Boolean value detection (true/false)
 * - Null value detection (null)
 * - Number value preservation (as raw strings)
 * - String value processing
 * - Array value parsing
 * - Type inference and validation
 * 
 * Example usage:
 * ```kotlin
 * val resolver = ValueResolver()
 * val toonValue = resolver.resolveValue("123")
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ValueResolver {

    /**
     * Resolves a raw string value into appropriate ToonValue.
     * 
     * This method analyzes the raw value and determines the most
     * appropriate ToonValue type based on content and type inference rules.
     * 
     * @param rawValue The raw string value to resolve
     * @return Resolved ToonValue instance
     * @throws ToonParseException if value resolution fails
     */
    fun resolveValue(rawValue: String): ToonValue {
        if (rawValue.isEmpty()) {
            return ToonString("")
        }

        val trimmed = rawValue.trim()

        return when {
            isNullValue(trimmed) -> ToonNull
            isBooleanValue(trimmed) -> ToonBoolean(trimmed.toBooleanStrict())
            isNumberValue(trimmed) -> ToonNumber(trimmed)
            isInlineArrayValue(trimmed) -> resolveInlineArray(trimmed)
            isQuotedStringValue(trimmed) -> resolveQuotedString(trimmed)
            else -> ToonString(trimmed)
        }
    }

    /**
     * Checks if a value represents null.
     * 
     * @param value The value to check
     * @return true if value represents null, false otherwise
     */
    private fun isNullValue(value: String): Boolean {
        return value.equals("null", ignoreCase = true)
    }

    /**
     * Checks if a value represents a boolean.
     * 
     * @param value The value to check
     * @return true if value represents a boolean, false otherwise
     */
    private fun isBooleanValue(value: String): Boolean {
        return value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)
    }

    /**
     * Checks if a value represents a number.
     * 
     * This method uses a comprehensive check to identify various
     * number formats including integers, decimals, and scientific notation.
     * 
     * @param value The value to check
     * @return true if value represents a number, false otherwise
     */
    private fun isNumberValue(value: String): Boolean {
        if (value.isEmpty()) return false

        // Check for valid number format
        val regex = Regex("""^-?\d+(\.\d+)?([eE][+-]?\d+)?$""")
        return regex.matches(value)
    }

    /**
     * Checks if a value represents an inline array.
     * 
     * @param value The value to check
     * @return true if value represents an inline array, false otherwise
     */
    private fun isInlineArrayValue(value: String): Boolean {
        // Simple heuristic: contains commas and not quoted
        return value.contains(',') && !value.startsWith('"') && !value.startsWith('\'')
    }

    /**
     * Checks if a value is a quoted string.
     * 
     * @param value The value to check
     * @return true if value is quoted, false otherwise
     */
    private fun isQuotedStringValue(value: String): Boolean {
        return (value.startsWith('"') && value.endsWith('"')) ||
                (value.startsWith('\'') && value.endsWith('\''))
    }

    /**
     * Resolves an inline array value into ToonArray.
     * 
     * @param arrayValue The array value to resolve
     * @return ToonArray with resolved elements
     * @throws ToonParseException if array resolution fails
     */
    private fun resolveInlineArray(arrayValue: String): ToonArray {
        val elements = parseArrayElements(arrayValue)
        val resolvedElements = elements.map { element ->
            resolveValue(element.trim())
        }

        return ToonArray(resolvedElements)
    }

    /**
     * Parses elements from an inline array value.
     * 
     * @param arrayValue The array value to parse
     * @return List of element strings
     * @throws ToonParseException if parsing fails
     */
    private fun parseArrayElements(arrayValue: String): List<String> {
        val elements = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = '\u0000'
        var escaped = false

        for (char in arrayValue) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }

                char == '\\' -> {
                    current.append(char)
                    escaped = true
                }

                !inQuotes && (char == '"' || char == '\'') -> {
                    inQuotes = true
                    quoteChar = char
                    current.append(char)
                }

                inQuotes && char == quoteChar -> {
                    inQuotes = false
                    current.append(char)
                }

                !inQuotes && char == ',' -> {
                    elements.add(current.toString())
                    current = StringBuilder()
                }

                else -> {
                    current.append(char)
                }
            }
        }

        // Add the last element
        if (current.isNotEmpty()) {
            elements.add(current.toString())
        }

        return elements
    }

    /**
     * Resolves a quoted string value.
     * 
     * @param quotedValue The quoted string value to resolve
     * @return ToonString with unescaped content
     * @throws ToonParseException if quoted string resolution fails
     */
    private fun resolveQuotedString(quotedValue: String): ToonString {
        if (quotedValue.length < 2) {
            throw ToonParseException(
                message = "Quoted string must have at least opening and closing quotes",
                line = 0,
                column = 1
            )
        }

        val quoteChar = quotedValue[0]
        val content = quotedValue.substring(1, quotedValue.length - 1)
        val unescapedContent = unescapeString(content, quoteChar)

        return ToonString(unescapedContent)
    }

    /**
     * Unescapes a string content.
     * 
     * @param content The content to unescape
     * @param quoteChar The quote character used
     * @return Unescaped string content
     * @throws ToonParseException if unescaping fails
     */
    private fun unescapeString(content: String, quoteChar: Char): String {
        val result = StringBuilder()
        var i = 0

        while (i < content.length) {
            val char = content[i]

            when {
                char == '\\' && i + 1 < content.length -> {
                    val nextChar = content[i + 1]
                    result.append(processEscapeSequence(nextChar))
                    i += 2
                }

                char == quoteChar -> {
                    throw ToonParseException(
                        message = "Unescaped quote character inside string",
                        line = 0,
                        column = 1
                    )
                }

                else -> {
                    result.append(char)
                    i++
                }
            }
        }

        return result.toString()
    }

    /**
     * Processes an escape sequence.
     * 
     * @param escapedChar The escaped character
     * @return The processed character
     * @throws ToonParseException if escape sequence is invalid
     */
    private fun processEscapeSequence(escapedChar: Char): String {
        return when (escapedChar) {
            'n' -> "\n"
            't' -> "\t"
            'r' -> "\r"
            'b' -> "\b"
            'f' -> "\u000C"
            '\\' -> "\\"
            '"' -> "\""
            '\'' -> "'"
            '/' -> "/"
            'u' -> {
                throw ToonParseException(
                    message = "Unicode escape sequences are not yet supported",
                    line = 0,
                    column = 1
                )
            }

            else -> {
                throw ToonParseException(
                    message = "Invalid escape sequence: \\$escapedChar",
                    line = 0,
                    column = 1
                )
            }
        }
    }

    /**
     * Resolves tabular array data into ToonArray of objects.
     * 
     * @param headers List of column headers
     * @param rows List of row data strings
     * @return ToonArray containing ToonObject instances
     * @throws ToonParseException if tabular array resolution fails
     */
    fun resolveTabularArray(headers: List<String>, rows: List<String>): ToonArray {
        val objects = rows.map { row ->
            resolveTabularRow(headers, row)
        }

        return ToonArray(objects)
    }

    /**
     * Resolves a single tabular row into ToonObject.
     * 
     * @param headers List of column headers
     * @param row The row data string
     * @return ToonObject representing the row
     * @throws ToonParseException if row resolution fails
     */
    private fun resolveTabularRow(headers: List<String>, row: String): ToonObject {
        val values = parseTableRowValues(row)

        if (values.size != headers.size) {
            throw ToonParseException(
                message = "Row column count (${values.size}) does not match header count (${headers.size})",
                line = 0,
                column = 1,
                context = "Row: $row"
            )
        }

        val entries = headers.zip(values).associate { (header, value) ->
            header to resolveValue(value.trim())
        }

        return ToonObject(entries)
    }

    /**
     * Parses values from a table row.
     * 
     * @param row The row string to parse
     * @return List of value strings
     */
    private fun parseTableRowValues(row: String): List<String> {
        // Simple CSV parsing for tabular arrays
        val values = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = '\u0000'
        var escaped = false

        for (char in row) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }

                char == '\\' -> {
                    current.append(char)
                    escaped = true
                }

                !inQuotes && (char == '"' || char == '\'') -> {
                    inQuotes = true
                    quoteChar = char
                    current.append(char)
                }

                inQuotes && char == quoteChar -> {
                    inQuotes = false
                    current.append(char)
                }

                !inQuotes && char == ',' -> {
                    values.add(current.toString())
                    current = StringBuilder()
                }

                else -> {
                    current.append(char)
                }
            }
        }

        // Add the last value
        if (current.isNotEmpty()) {
            values.add(current.toString())
        }

        return values
    }
}

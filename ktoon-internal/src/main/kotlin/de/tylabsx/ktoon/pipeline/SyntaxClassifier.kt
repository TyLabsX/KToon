package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.KToonContext
import de.tylabsx.ktoon.ToonParseException

/**
 * Classifies and validates syntax of TOON lines.
 * 
 * This class handles the fourth phase of the TOON parsing pipeline,
 * analyzing each line to determine its exact type and validate its syntax.
 * It extracts key-value pairs, identifies arrays, and validates the
 * structure according to TOON specification.
 * 
 * The classifier handles:
 * - Precise line type identification
 * - Key and value extraction
 * - Syntax validation
 * - Array type detection
 * - Comment processing
 * 
 * Example usage:
 * ```kotlin
 * val classifier = SyntaxClassifier()
 * val classifiedLines = classifier.classifyLines(structure)
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class SyntaxClassifier {

    /**
     * Classifies lines in the indentation structure.
     * 
     * This method processes all lines in the indentation structure
     * and creates ClassifiedLine objects with precise type information
     * and extracted components.
     * 
     * @param structure The indentation structure to classify
     * @return List of ClassifiedLine objects
     * @throws ToonParseException if classification fails
     */
    fun classifyLines(structure: IndentationStructure): List<ClassifiedLine> {
        val context = KToonContext.current
        val allLines = structure.getAllLines()

        return allLines.map { line ->
            classifyLine(line, context)
        }
    }

    /**
     * Classifies a single line and extracts its components.
     * 
     * @param line The indentation line to classify
     * @param context The current KToonContext configuration
     * @return ClassifiedLine with type information and extracted components
     * @throws ToonParseException if classification fails
     */
    private fun classifyLine(
        line: ProcessedIndentationLine,
        context: KToonContext
    ): ClassifiedLine {
        val content = line.lineInfo.content.trimStart()

        return when {
            content.isEmpty() -> createEmptyLine(line)
            content.startsWith('#') -> createCommentLine(line, content)
            else -> classifyContentLine(line, content, context)
        }
    }

    /**
     * Creates a classified line for empty content.
     * 
     * @param line The indentation line
     * @return ClassifiedLine for empty content
     */
    private fun createEmptyLine(line: ProcessedIndentationLine): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.EMPTY,
            key = null,
            value = null,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Creates a classified line for comment content.
     * 
     * @param line The indentation line
     * @param content The trimmed content
     * @return ClassifiedLine for comment content
     */
    private fun createCommentLine(
        line: ProcessedIndentationLine,
        content: String
    ): ClassifiedLine {
        val commentText = content.substring(1).trimStart()

        return ClassifiedLine(
            indentationLine = line,
            type = LineType.COMMENT,
            key = null,
            value = commentText,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Classifies a line with actual content (key-value pairs, arrays, etc.).
     * 
     * @param line The indentation line
     * @param content The trimmed content
     * @param context The current KToonContext configuration
     * @return ClassifiedLine for content line
     * @throws ToonParseException if classification fails
     */
    private fun classifyContentLine(
        line: ProcessedIndentationLine,
        content: String,
        context: KToonContext
    ): ClassifiedLine {
        val colonIndex = findColonPosition(content)

        if (colonIndex == -1) {
            return if (context.enableTabularArrays && isChildOfTabularArrayHeader(line)) {
                createTabularArrayRowLine(
                    line = line,
                    key = "__row_${line.lineInfo.lineNumber}",
                    value = content
                )
            } else {
                createInvalidLine(line, "Missing colon in key-value pair")
            }
        }

        val keyPart = content.substring(0, colonIndex).trim()
        val valuePart = if (colonIndex < content.length - 1) {
            content.substring(colonIndex + 1).trim()
        } else {
            ""
        }

        validateKey(keyPart, line.lineInfo.lineNumber, context)

        return when {
            context.enableTabularArrays && isTabularArrayHeader(keyPart) -> {
                createTabularArrayHeaderLine(line, keyPart, valuePart)
            }

            context.enableInlineArrays && isInlineArray(valuePart) -> {
                createInlineArrayLine(line, keyPart, valuePart)
            }

            valuePart.isEmpty() -> {
                createKeyOnlyLine(line, keyPart)
            }

            else -> {
                createKeyValueLine(line, keyPart, valuePart)
            }
        }
    }

    /**
     * Finds the position of the colon that separates key from value.
     * 
     * @param content The line content to search
     * @return Index of the colon, or -1 if not found
     */
    private fun findColonPosition(content: String): Int {
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false

        for ((index, char) in content.withIndex()) {
            when {
                escaped -> {
                    escaped = false
                }

                char == '\\' -> {
                    escaped = true
                }

                char == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                }

                char == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                }

                char == ':' && !inSingleQuote && !inDoubleQuote -> {
                    return index
                }
            }
        }

        return -1
    }

    /**
     * Validates a key according to TOON specification.
     * 
     * @param key The key to validate
     * @param lineNumber The line number for error reporting
     * @param context The current KToonContext configuration
     * @throws ToonParseException if key is invalid
     */
    private fun validateKey(key: String, lineNumber: Int, context: KToonContext) {
        if (key.isEmpty()) {
            throw ToonParseException(
                message = "Empty key is not allowed",
                line = lineNumber,
                column = 1
            )
        }

        if (context.strictMode) {
            // In strict mode, validate key characters
            key.forEachIndexed { index, char ->
                if (!isValidKeyCharacter(char)) {
                    throw ToonParseException(
                        message = "Invalid character '$char' in key",
                        line = lineNumber,
                        column = index + 1,
                        context = key
                    )
                }
            }
        }
    }

    /**
     * Checks if a character is valid in a key.
     * 
     * @param char The character to check
     * @return true if character is valid, false otherwise
     */
    private fun isValidKeyCharacter(char: Char): Boolean {
        return char.isLetterOrDigit() ||
                char == '_' ||
                char == '-' ||
                char == '.' ||
                char == '[' ||
                char == ']' ||
                char == '{' ||
                char == '}' ||
                char == ',' ||
                char == '|'
    }

    /**
     * Checks if a value represents an inline array.
     * 
     * @param value The value to check
     * @return true if value is an inline array, false otherwise
     */
    private fun isInlineArray(value: String): Boolean {
        return containsUnquotedComma(value)
    }


    /**
     * Checks if a value contains an unquoted comma.
     *
     * @param value The value to check
     * @return true if value contains an unquoted comma, false otherwise
     */
    private fun containsUnquotedComma(value: String): Boolean {
        var inQuotes = false
        var escaped = false

        value.forEach { char ->
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> return true
            }
        }

        return false
    }

    /**
     * Checks if a key represents a tabular array header.
     * 
     * @param key The key to check
     * @return true if key is a tabular array header, false otherwise
     */
    private fun isTabularArrayHeader(key: String): Boolean {
        return key.contains('{') && key.contains('}') && key.endsWith('}')
    }

    /**
     * Checks if a value represents a tabular array row.
     * 
     * @param value The value to check
     * @return true if value is a tabular array row, false otherwise
     */
    private fun isTabularArrayRow(value: String): Boolean {
        return value.contains(',') && !value.contains('"') && !value.contains('\'')
    }

    /**
     * Creates a classified line for key-only (object) lines.
     * 
     * @param line The indentation line
     * @param key The extracted key
     * @return ClassifiedLine for key-only line
     */
    private fun createKeyOnlyLine(line: ProcessedIndentationLine, key: String): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.KEY_ONLY,
            key = key,
            value = null,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Creates a classified line for inline array lines.
     * 
     * @param line The indentation line
     * @param key The extracted key
     * @param value The extracted value (array content)
     * @return ClassifiedLine for inline array line
     */
    private fun createInlineArrayLine(
        line: ProcessedIndentationLine,
        key: String,
        value: String
    ): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.INLINE_ARRAY,
            key = key,
            value = value,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Creates a classified line for tabular array header lines.
     * 
     * @param line The indentation line
     * @param key The extracted key (with headers)
     * @param value The extracted value
     * @return ClassifiedLine for tabular array header line
     */
    private fun createTabularArrayHeaderLine(
        line: ProcessedIndentationLine,
        key: String,
        value: String
    ): ClassifiedLine {
        val headers = extractTabularArrayHeaders(key)

        return ClassifiedLine(
            indentationLine = line,
            type = LineType.TABULAR_ARRAY_HEADER,
            key = key.substringBefore('{').trim(),
            value = value,
            arrayHeaders = headers,
            isValid = true
        )
    }

    /**
     * Creates a classified line for tabular array row lines.
     * 
     * @param line The indentation line
     * @param key The extracted key
     * @param value The extracted value (row data)
     * @return ClassifiedLine for tabular array row line
     */
    private fun createTabularArrayRowLine(
        line: ProcessedIndentationLine,
        key: String,
        value: String
    ): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.TABULAR_ARRAY_ROW,
            key = key,
            value = value,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Creates a classified line for regular key-value pairs.
     * 
     * @param line The indentation line
     * @param key The extracted key
     * @param value The extracted value
     * @return ClassifiedLine for key-value line
     */
    private fun createKeyValueLine(
        line: ProcessedIndentationLine,
        key: String,
        value: String
    ): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.KEY_VALUE,
            key = key,
            value = value,
            arrayHeaders = emptyList(),
            isValid = true
        )
    }

    /**
     * Creates a classified line for invalid content.
     * 
     * @param line The indentation line
     * @param errorMessage The error message
     * @return ClassifiedLine marked as invalid
     */
    private fun createInvalidLine(line: ProcessedIndentationLine, errorMessage: String): ClassifiedLine {
        return ClassifiedLine(
            indentationLine = line,
            type = LineType.UNKNOWN,
            key = null,
            value = null,
            arrayHeaders = emptyList(),
            isValid = false,
            errorMessage = errorMessage
        )
    }

    /**
     * Extracts headers from a tabular array key.
     * 
     * @param key The key with headers (e.g., "users{id,name}")
     * @return List of extracted headers
     */
    private fun extractTabularArrayHeaders(key: String): List<String> {
        val start = key.indexOf('{')
        val end = key.indexOf('}')

        if (start == -1 || end == -1 || end <= start) {
            return emptyList()
        }

        val headersPart = key.substring(start + 1, end)
        return headersPart.split(',').map { it.trim() }
    }

    /**
     * Checks whether a line is a direct child of a tabular array header.
     *
     * Tabular array rows do not contain colons:
     *
     * ```
     * users[2]{id,name}:
     *   1,Ada
     *   2,Bob
     * ```
     *
     * @param line current indentation line
     * @return true if parent line is a tabular array header
     */
    private fun isChildOfTabularArrayHeader(line: ProcessedIndentationLine): Boolean {
        val parentContent = line.parent?.lineInfo?.content?.trim() ?: return false
        val colonIndex = findColonPosition(parentContent)

        if (colonIndex == -1) return false

        val parentKey = parentContent.substring(0, colonIndex).trim()
        val parentValue = parentContent.substring(colonIndex + 1).trim()

        return parentValue.isEmpty() && isTabularArrayHeader(parentKey)
    }
}

/**
 * Represents a classified line with precise type information.
 * 
 * This data class contains the results of syntax classification,
 * including the exact line type, extracted components, and validation status.
 * 
 * @property indentationLine The original indentation line
 * @property type The precise line type
 * @property key The extracted key (if applicable)
 * @property value The extracted value (if applicable)
 * @property arrayHeaders List of array headers (for tabular arrays)
 * @property isValid Whether the line passed syntax validation
 * @property errorMessage Error message if validation failed
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ClassifiedLine(
    /**
     * The original indentation line.
     */
    val indentationLine: ProcessedIndentationLine,

    /**
     * The precise type of this line.
     */
    val type: LineType,

    /**
     * The extracted key (null for lines without keys).
     */
    val key: String?,

    /**
     * The extracted value (null for lines without values).
     */
    val value: String?,

    /**
     * List of array headers (only for tabular array headers).
     */
    val arrayHeaders: List<String>,

    /**
     * Whether this line passed syntax validation.
     */
    val isValid: Boolean,

    /**
     * Error message if validation failed.
     */
    val errorMessage: String? = null
) {
    /**
     * Checks if this line should be included in the final structure.
     * 
     * @return true if this line should be processed, false otherwise
     */
    fun shouldIncludeInStructure(): Boolean {
        return isValid && type != LineType.EMPTY && type != LineType.COMMENT
    }

    /**
     * Checks if this line represents a container (has children).
     * 
     * @return true if this line can have children, false otherwise
     */
    fun isContainer(): Boolean {
        return type == LineType.KEY_ONLY || type == LineType.TABULAR_ARRAY_HEADER
    }

    /**
     * Checks if this line represents a value.
     * 
     * @return true if this line has a value, false otherwise
     */
    fun isValue(): Boolean {
        return type == LineType.KEY_VALUE || type == LineType.INLINE_ARRAY || type == LineType.TABULAR_ARRAY_ROW
    }

    /**
     * Returns a string representation for debugging.
     * 
     * @return Debug string representation
     */
    override fun toString(): String {
        val status = if (isValid) "VALID" else "INVALID"
        val keyPart = key?.let { "key=\"$it\"" } ?: "no key"
        val valuePart = value?.let { "value=\"$it\"" } ?: "no value"
        val headersPart = if (arrayHeaders.isNotEmpty()) {
            "headers=${arrayHeaders.joinToString(",")}"
        } else ""

        return "Line${indentationLine.lineInfo.lineNumber}: $type [$status] $keyPart $valuePart $headersPart"
    }
}

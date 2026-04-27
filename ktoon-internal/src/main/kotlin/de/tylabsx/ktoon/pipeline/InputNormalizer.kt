package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.KToonContext
import de.tylabsx.ktoon.ToonParseException

/**
 * Normalizes raw TOON input for consistent processing.
 * 
 * This class handles the first phase of the TOON parsing pipeline,
 * ensuring that input is in a consistent format for subsequent processing.
 * It handles line ending normalization, whitespace management, and
 * basic validation.
 * 
 * The normalizer follows these rules:
 * - Normalizes line endings to \n
 * - Handles trailing whitespace based on configuration
 * - Preserves or removes comments based on configuration
 * - Validates basic input structure
 * 
 * Example usage:
 * ```kotlin
 * val normalizer = InputNormalizer()
 * val normalized = normalizer.normalize(rawInput)
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class InputNormalizer {

    /**
     * Normalizes raw TOON input string.
     * 
     * This method processes the raw input and returns a normalized
     * version suitable for the parsing pipeline. All normalization
     * rules are applied based on the current KToonContext configuration.
     * 
     * @param input The raw TOON input string
     * @return Normalized input string
     * @throws ToonParseException if input validation fails
     */
    fun normalize(input: String): String {
        if (input.isEmpty()) {
            return ""
        }

        val context = KToonContext.current

        // Step 1: Normalize line endings
        val normalizedLineEndings = normalizeLineEndings(input)

        // Step 2: Split into lines for processing
        val lines = normalizedLineEndings.split('\n')

        // Step 3: Process each line
        val processedLines = lines.mapIndexed { index, line ->
            processLine(line, index + 1, context)
        }

        // Step 4: Reassemble and return
        return processedLines.joinToString("\n")
    }

    /**
     * Normalizes line endings to consistent format.
     * 
     * Converts all line ending variations (\r\n, \r) to \n
     * for consistent processing across different platforms.
     * 
     * @param input The input string to normalize
     * @return String with normalized line endings
     */
    private fun normalizeLineEndings(input: String): String {
        return input
            .replace("\r\n", "\n")  // Windows CRLF -> Unix LF
            .replace("\r", "\n")    // Old Mac CR -> Unix LF
    }

    /**
     * Processes a single line according to normalization rules.
     * 
     * @param line The line to process
     * @param lineNumber The line number (1-based)
     * @param context The current KToonContext configuration
     * @return Processed line
     * @throws ToonParseException if line processing fails
     */
    private fun processLine(line: String, lineNumber: Int, context: KToonContext): String {
        // Handle empty lines
        if (line.isEmpty()) {
            return line
        }

        // Check for invalid characters (in strict mode)
        if (context.strictMode) {
            validateLineCharacters(line, lineNumber)
        }

        // Handle trailing whitespace
        val trimmedLine = handleTrailingWhitespace(line, lineNumber, context)

        // Handle comments
        val processedLine = handleComments(trimmedLine, lineNumber, context)

        return processedLine
    }

    /**
     * Validates line characters in strict mode.
     * 
     * @param line The line to validate
     * @param lineNumber The line number
     * @throws ToonParseException if invalid characters are found
     */
    private fun validateLineCharacters(line: String, lineNumber: Int) {
        // Check for control characters (except tab)
        line.forEachIndexed { index, char ->
            if (char.isISOControl() && char != '\t') {
                throw ToonParseException(
                    message = "Invalid control character found: '${char}' (code: ${char.code})",
                    line = lineNumber,
                    column = index + 1,
                    context = line
                )
            }
        }
    }

    /**
     * Handles trailing whitespace according to configuration.
     * 
     * @param line The line to process
     * @param lineNumber The line number
     * @param context The current KToonContext configuration
     * @return Line with trailing whitespace handled
     * @throws ToonParseException if trailing whitespace is not allowed
     */
    private fun handleTrailingWhitespace(line: String, lineNumber: Int, context: KToonContext): String {
        if (line.isEmpty()) return line

        val trimmedEnd = line.trimEnd()
        val trailingWhitespace = line.substring(trimmedEnd.length)

        if (trailingWhitespace.isNotEmpty() && !context.allowTrailingWhitespace) {
            // Find the first trailing whitespace character
            val firstTrailingIndex = trimmedEnd.length

            throw ToonParseException(
                message = "Trailing whitespace is not allowed",
                line = lineNumber,
                column = firstTrailingIndex + 1,
                context = line
            )
        }

        return trimmedEnd
    }

    /**
     * Handles comment processing according to configuration.
     * 
     * @param line The line to process
     * @param lineNumber The line number
     * @param context The current KToonContext configuration
     * @return Line with comments handled
     */
    private fun handleComments(line: String, lineNumber: Int, context: KToonContext): String {
        if (line.isEmpty()) return line

        // Find comment start (only if not in quotes)
        val commentStart = findCommentStart(line)

        return if (commentStart != -1 && !context.preserveComments) {
            // Remove comment part
            line.substring(0, commentStart).trimEnd()
        } else {
            line
        }
    }

    /**
     * Finds the start of a comment in a line.
     * 
     * Comments start with #, but only if the # is not inside quotes.
     * This method handles both single and double quotes.
     * 
     * @param line The line to search
     * @return Index of comment start, or -1 if no comment found
     */
    private fun findCommentStart(line: String): Int {
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false

        for ((index, char) in line.withIndex()) {
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

                char == '#' && !inSingleQuote && !inDoubleQuote -> {
                    return index
                }
            }
        }

        return -1
    }

    /**
     * Validates the normalized input structure.
     * 
     * @param normalizedInput The normalized input to validate
     * @throws ToonParseException if validation fails
     */
    fun validateNormalizedInput(normalizedInput: String) {
        if (normalizedInput.isEmpty()) {
            return // Empty input is valid
        }

        val lines = normalizedInput.split('\n')
        val context = KToonContext.current

        // Check for maximum nesting depth (rough estimate)
        if (context.maxNestingDepth > 0) {
            val maxIndentation = lines.mapNotNull { line ->
                if (line.isNotEmpty()) {
                    calculateIndentation(line)
                } else null
            }.maxOrNull() ?: 0

            if (maxIndentation > context.maxNestingDepth) {
                throw ToonParseException(
                    message = "Maximum nesting depth (${context.maxNestingDepth}) exceeded",
                    line = lines.indexOfFirst { calculateIndentation(it) == maxIndentation } + 1,
                    column = 1
                )
            }
        }

        // Additional structural validations can be added here
    }

    /**
     * Calculates the indentation level of a line.
     * 
     * @param line The line to analyze
     * @return Indentation level (number of indentation units)
     */
    private fun calculateIndentation(line: String): Int {
        val context = KToonContext.current

        var spaces = 0
        var tabs = 0

        for (char in line) {
            when (char) {
                ' ' -> spaces++
                '\t' -> tabs++
                else -> break
            }
        }

        // In strict mode, don't allow mixing spaces and tabs
        if (context.strictMode && spaces > 0 && tabs > 0) {
            throw ToonParseException(
                message = "Mixing spaces and tabs for indentation is not allowed in strict mode",
                line = 0, // Would need line number from caller
                column = 1
            )
        }

        // Convert to indentation units
        return when {
            tabs > 0 -> tabs // Each tab is one unit
            spaces > 0 -> spaces / context.indentationSize // Each N spaces is one unit
            else -> 0
        }
    }
}

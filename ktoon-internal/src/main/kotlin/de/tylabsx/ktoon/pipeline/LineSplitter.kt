package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.KToonContext
import de.tylabsx.ktoon.ToonParseException

/**
 * Splits normalized TOON input into individual lines for processing.
 * 
 * This class handles the second phase of the TOON parsing pipeline,
 * taking normalized input and splitting it into individual lines while
 * preserving line numbers and basic line metadata.
 * 
 * The splitter handles:
 * - Line splitting with line number tracking
 * - Empty line identification
 * - Basic line classification
 * - Line metadata extraction
 * 
 * Example usage:
 * ```kotlin
 * val splitter = LineSplitter()
 * val lines = splitter.splitIntoLines(normalizedInput)
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class LineSplitter {

    /**
     * Splits normalized input into individual lines.
     * 
     * This method takes normalized TOON input and splits it into
     * individual lines, creating LineInfo objects with metadata
     * for each line.
     * 
     * @param normalizedInput The normalized TOON input string
     * @return List of LineInfo objects representing each line
     * @throws ToonParseException if line splitting encounters issues
     */
    fun splitIntoLines(normalizedInput: String): List<LineInfo> {
        if (normalizedInput.isEmpty()) {
            return emptyList()
        }

        val lines = normalizedInput.split('\n')
        return lines.mapIndexed { index, content ->
            createLineInfo(content, index + 1)
        }
    }

    /**
     * Creates a LineInfo object for a single line.
     * 
     * @param content The line content
     * @param lineNumber The line number (1-based)
     * @return LineInfo object with extracted metadata
     */
    private fun createLineInfo(content: String, lineNumber: Int): LineInfo {
        val context = KToonContext.current

        return LineInfo(
            content = content,
            lineNumber = lineNumber,
            isEmpty = content.isEmpty(),
            isComment = isCommentLine(content),
            indentation = calculateIndentation(content),
            hasKey = hasKey(content),
            hasColon = hasColon(content),
            estimatedType = estimateLineType(content)
        )
    }

    /**
     * Checks if a line is a comment line.
     * 
     * @param line The line to check
     * @return true if the line is a comment, false otherwise
     */
    private fun isCommentLine(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith('#')
    }

    /**
     * Calculates the indentation level of a line.
     * 
     * @param line The line to analyze
     * @return Indentation level as number of indentation units
     * @throws ToonParseException if indentation is invalid
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

        // Validate indentation consistency
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
            spaces > 0 -> {
                val units = spaces / context.indentationSize
                if (spaces % context.indentationSize != 0 && context.strictMode) {
                    throw ToonParseException(
                        message = "Indentation must be multiple of ${context.indentationSize} spaces in strict mode",
                        line = 0,
                        column = 1
                    )
                }
                units
            }

            else -> 0
        }
    }

    /**
     * Checks if a line contains a key.
     * 
     * A key is identified as text before a colon that is not a comment.
     * 
     * @param line The line to check
     * @return true if the line contains a key, false otherwise
     */
    private fun hasKey(line: String): Boolean {
        if (line.isEmpty() || isCommentLine(line)) {
            return false
        }

        val trimmed = line.trimStart()
        return trimmed.contains(':') && !trimmed.startsWith(':')
    }

    /**
     * Checks if a line contains a colon.
     * 
     * @param line The line to check
     * @return true if the line contains a colon, false otherwise
     */
    private fun hasColon(line: String): Boolean {
        return line.contains(':')
    }

    /**
     * Estimates the type of a line based on its content.
     * 
     * This is a preliminary classification that may be refined
     * by later processing stages.
     * 
     * @param line The line to classify
     * @return Estimated LineType
     */
    private fun estimateLineType(line: String): LineType {
        if (line.isEmpty()) {
            return LineType.EMPTY
        }

        if (isCommentLine(line)) {
            return LineType.COMMENT
        }

        if (!hasColon(line)) {
            return LineType.UNKNOWN
        }

        val trimmed = line.trimStart()
        val colonIndex = trimmed.indexOf(':')

        if (colonIndex == -1) {
            return LineType.UNKNOWN
        }

        val keyPart = trimmed.substring(0, colonIndex).trim()
        val valuePart = if (colonIndex < trimmed.length - 1) {
            trimmed.substring(colonIndex + 1).trim()
        } else {
            ""
        }

        return when {
            valuePart.isEmpty() -> LineType.KEY_ONLY
            valuePart.contains(',') -> LineType.INLINE_ARRAY
            keyPart.contains('{') && keyPart.contains('}') -> LineType.TABULAR_ARRAY_HEADER
            else -> LineType.KEY_VALUE
        }
    }

    /**
     * Validates the list of lines for basic consistency.
     * 
     * @param lines The list of lines to validate
     * @throws ToonParseException if validation fails
     */
    fun validateLines(lines: List<LineInfo>) {
        val context = KToonContext.current

        lines.forEach { line ->
            // Validate indentation sequence
            if (line.lineNumber > 1) {
                val previousLine = lines[line.lineNumber - 2]
                validateIndentationSequence(previousLine, line, context)
            }
        }
    }

    /**
     * Validates that indentation follows proper sequence rules.
     * 
     * @param previousLine The previous line
     * @param currentLine The current line
     * @param context The current KToonContext configuration
     * @throws ToonParseException if indentation sequence is invalid
     */
    private fun validateIndentationSequence(
        previousLine: LineInfo,
        currentLine: LineInfo,
        context: KToonContext
    ) {
        // Skip validation for empty lines and comments
        if (currentLine.isEmpty || currentLine.isComment) {
            return
        }

        val indentDiff = currentLine.indentation - previousLine.indentation

        // Indentation can only increase by 1 at most
        if (indentDiff > 1 && context.strictMode) {
            throw ToonParseException(
                message = "Indentation increased by more than one level (${indentDiff} levels)",
                line = currentLine.lineNumber,
                column = 1,
                context = currentLine.content
            )
        }

        // Indentation cannot decrease below 0
        if (currentLine.indentation < 0) {
            throw ToonParseException(
                message = "Negative indentation level",
                line = currentLine.lineNumber,
                column = 1,
                context = currentLine.content
            )
        }
    }
}

/**
 * Represents information about a line in TOON input.
 * 
 * This data class contains metadata about each line that is
 * extracted during the line splitting phase and used by
 * subsequent processing stages.
 * 
 * @property content The original line content
 * @property lineNumber The line number (1-based)
 * @property isEmpty Whether the line is empty
 * @property isComment Whether the line is a comment
 * @property indentation The calculated indentation level
 * @property hasKey Whether the line contains a key
 * @property hasColon Whether the line contains a colon
 * @property estimatedType The estimated line type
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class LineInfo(
    /**
     * The original content of the line.
     */
    val content: String,

    /**
     * The line number in the original input (1-based).
     */
    val lineNumber: Int,

    /**
     * Whether this line is empty (contains only whitespace).
     */
    val isEmpty: Boolean,

    /**
     * Whether this line is a comment (starts with #).
     */
    val isComment: Boolean,

    /**
     * The calculated indentation level.
     */
    val indentation: Int,

    /**
     * Whether this line contains a key (text before colon).
     */
    val hasKey: Boolean,

    /**
     * Whether this line contains a colon character.
     */
    val hasColon: Boolean,

    /**
     * The estimated type of this line.
     */
    val estimatedType: LineType
) {
    /**
     * Gets the effective content (content without leading indentation).
     * 
     * @return Content without leading whitespace
     */
    fun getEffectiveContent(): String = content.trimStart()

    /**
     * Checks if this line should be processed by the parser.
     * 
     * @return true if this line should be processed, false otherwise
     */
    fun shouldProcess(): Boolean = !isEmpty && !isComment

    /**
     * Returns a string representation for debugging.
     * 
     * @return Debug string representation
     */
    override fun toString(): String {
        val prefix = when {
            isEmpty -> "[EMPTY]"
            isComment -> "[COMMENT]"
            else -> "[${estimatedType}]"
        }
        return "Line$lineNumber: $prefix indent=$indentation content=\"$content\""
    }
}

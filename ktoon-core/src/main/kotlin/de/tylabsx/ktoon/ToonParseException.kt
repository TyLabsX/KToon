package de.tylabsx.ktoon

/**
 * Exception thrown when parsing TOON data encounters an error.
 * 
 * This exception provides detailed information about parsing failures,
 * including the exact location (line and column) where the error occurred,
 * the error context, and suggestions for resolution.
 * 
 * The exception is designed to help developers quickly identify and fix
 * TOON syntax errors in their input data.
 * 
 * Example usage in error handling:
 * ```kotlin
 * try {
 *     val value = KToon.parse(invalidToonData)
 * } catch (e: ToonParseException) {
 *     println("Parse error at line ${e.line}, column ${e.column}")
 *     println("Error: ${e.message}")
 *     e.context?.let { println("Context: $it") }
 * }
 * ```
 * 
 * @property message Detailed error message describing what went wrong
 * @property line The line number where the error occurred (1-based)
 * @property column The column number where the error occurred (1-based)
 * @property context Optional context information showing the problematic line or section
 * @property cause Optional underlying cause exception
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonParseException(
    override val message: String,
    /**
     * The line number where the parsing error occurred.
     * 
     * Line numbers are 1-based, meaning the first line of the input is line 1.
     * This helps users locate errors in their TOON data files.
     * 
     * Example: If the error is on the third line, this value will be 3.
     */
    val line: Int,
    /**
     * The column number where the parsing error occurred.
     * 
     * Column numbers are 1-based, meaning the first character of a line is column 1.
     * This provides precise location information within the problematic line.
     * 
     * Example: If the error is at the 10th character of line 5, this will be 10.
     */
    val column: Int,
    /**
     * Optional context information about the error.
     * 
     * This may include the problematic line content, surrounding lines,
     * or additional details about the parsing state when the error occurred.
     * Context is extremely helpful for debugging complex TOON structures.
     * 
     * Example: "user: id: 123" (showing the problematic line)
     */
    val context: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Returns a formatted error message with location information.
     * 
     * This method provides a comprehensive error report including the line,
     * column, message, and context (if available). The format is designed
     * to be easily readable and suitable for logging or user display.
     * 
     * Example output:
     * ```
     * Parse error at line 3, column 5: Invalid indentation
     * Context: "    name: Alice"
     * ```
     * 
     * @return Formatted error message string
     */
    fun getFormattedMessage(): String {
        val sb = StringBuilder()
        sb.append("Parse error at line $line, column $column: $message")
        context?.let { ctx ->
            sb.append("\nContext: \"$ctx\"")
        }
        cause?.let { c ->
            sb.append("\nCaused by: ${c.message}")
        }
        return sb.toString()
    }

    /**
     * Returns a concise error summary suitable for quick debugging.
     * 
     * This method provides a shorter version of the error information,
     * focusing on the essential location and message without context.
     * 
     * Example output: "Error at 3:5: Invalid indentation"
     * 
     * @return Concise error summary
     */
    fun getErrorSummary(): String = "Error at $line:$column: $message"

    /**
     * Creates a ToonParseException for syntax errors.
     * 
     * This factory method creates a standardized exception for common
     * syntax-related parsing errors.
     * 
     * @param expected What was expected in the syntax
     * @param actual What was actually found
     * @param line Line number where the error occurred
     * @param column Column number where the error occurred
     * @param context Optional context information
     * @return ToonParseException configured for syntax error
     */
    companion object {

        /**
         * Creates a syntax error exception.
         * 
         * Use this factory method for common syntax errors where expected
         * tokens don't match actual tokens.
         * 
         * @param expected The expected syntax element
         * @param actual The actual syntax element found
         * @param line Line number (1-based)
         * @param column Column number (1-based)
         * @param context Optional context line
         * @return ToonParseException for syntax error
         */
        fun syntaxError(
            expected: String,
            actual: String,
            line: Int,
            column: Int,
            context: String? = null
        ): ToonParseException {
            return ToonParseException(
                message = "Expected '$expected' but found '$actual'",
                line = line,
                column = column,
                context = context
            )
        }

        /**
         * Creates an indentation error exception.
         * 
         * Use this factory method for indentation-related errors,
         * which are common in TOON parsing.
         * 
         * @param description Description of the indentation problem
         * @param line Line number (1-based)
         * @param column Column number (1-based)
         * @param context Optional context line
         * @return ToonParseException for indentation error
         */
        fun indentationError(
            description: String,
            line: Int,
            column: Int,
            context: String? = null
        ): ToonParseException {
            return ToonParseException(
                message = "Invalid indentation: $description",
                line = line,
                column = column,
                context = context
            )
        }

        /**
         * Creates an unexpected end of input exception.
         * 
         * Use this factory method when the parser reaches the end of input
         * before expecting to finish parsing.
         * 
         * @param line Line number where EOF was encountered (1-based)
         * @param column Column number where EOF was encountered (1-based)
         * @param context Optional context about what was being parsed
         * @return ToonParseException for unexpected EOF
         */
        fun unexpectedEndOfInput(
            line: Int,
            column: Int,
            context: String? = null
        ): ToonParseException {
            return ToonParseException(
                message = "Unexpected end of input while parsing",
                line = line,
                column = column,
                context = context
            )
        }

        /**
         * Creates an invalid value exception.
         * 
         * Use this factory method when a value cannot be parsed into
         * the expected TOON data type.
         * 
         * @param value The invalid value that was found
         * @param expectedType The expected type or format
         * @param line Line number (1-based)
         * @param column Column number (1-based)
         * @param context Optional context line
         * @return ToonParseException for invalid value
         */
        fun invalidValue(
            value: String,
            expectedType: String,
            line: Int,
            column: Int,
            context: String? = null
        ): ToonParseException {
            return ToonParseException(
                message = "Invalid value '$value' for type $expectedType",
                line = line,
                column = column,
                context = context
            )
        }
    }
}

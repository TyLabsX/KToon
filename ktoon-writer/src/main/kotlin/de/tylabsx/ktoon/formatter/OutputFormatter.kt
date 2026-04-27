package de.tylabsx.ktoon.formatter

import de.tylabsx.ktoon.pipeline.WriterOptions

/**
 * Formats serialized TOON output according to specified options.
 * 
 * This class handles output formatting including pretty printing,
 * minification, and custom formatting rules.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class OutputFormatter {

    /**
     * Formats serialized data according to options.
     * 
     * @param data The raw serialized data
     * @param options Formatting options
     * @return Formatted output string
     */
    fun format(data: String, options: WriterOptions): String {
        return when {
            options.minify -> minify(data)
            options.pretty -> prettyPrint(data, options)
            else -> data
        }
    }

    /**
     * Minifies serialized data by removing unnecessary whitespace.
     * 
     * @param data The data to minify
     * @return Minified data string
     */
    fun minify(data: String): String {
        return data
            .replace(Regex("\\s+\\n\\s+"), " ") // Replace newlines with single space
            .replace(Regex("\\s*:\\s*"), ":") // Remove spaces around colons
            .replace(Regex("\\s*,\\s*"), ",") // Remove spaces around commas
            .trim()
    }

    /**
     * Pretty prints serialized data with proper indentation.
     * 
     * @param data The data to pretty print
     * @param options Formatting options
     * @return Pretty printed data string
     */
    fun prettyPrint(data: String, options: WriterOptions): String {
        if (data.isEmpty()) return data

        val lines = data.split('\n')
        val builder = StringBuilder()
        var currentIndent = 0

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                // Empty line - just add newline
                if (index < lines.size - 1) {
                    builder.append('\n')
                }
                return@forEachIndexed
            }

            // Calculate indentation for this line
            val indentLevel = calculateIndentLevel(line)

            // Apply custom formatters if any
            val formattedLine = applyCustomFormatters(trimmed, options)

            // Add indentation
            builder.append(" ".repeat(indentLevel * options.indentSize))
            builder.append(formattedLine)

            // Add newline (except for last line)
            if (index < lines.size - 1) {
                builder.append('\n')
            }
        }

        return builder.toString()
    }

    /**
     * Calculates the indentation level of a line.
     * 
     * @param line The line to analyze
     * @return Indentation level
     */
    private fun calculateIndentLevel(line: String): Int {
        var indent = 0

        for (char in line) {
            when (char) {
                ' ' -> indent++
                '\t' -> indent += 4 // Assume tab = 4 spaces
                else -> break
            }
        }

        return indent / 2 // Assuming 2 spaces per level
    }

    /**
     * Applies custom formatting rules to a line.
     * 
     * @param line The line to format
     * @param options Formatting options
     * @return Formatted line
     */
    private fun applyCustomFormatters(line: String, options: WriterOptions): String {
        var formatted = line

        options.customFormatters.forEach { (key, formatter) ->
            if (line.contains(key)) {
                formatted = formatter(formatted)
            }
        }

        return formatted
    }
}

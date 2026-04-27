package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.KToonContext
import de.tylabsx.ktoon.ToonParseException

/**
 * Analyzes indentation and builds the logical line hierarchy for TOON input.
 *
 * This analyzer is responsible for turning a flat list of physical lines into
 * a tree of ProcessedIndentationLine objects.
 *
 * Important:
 * The IndentationStructure.rootLines list must contain only real root-level
 * lines. It must not contain every processed line. Otherwise nested values such
 * as:
 *
 * ```
 * user:
 *   id: 123
 *   name: Bob
 * ```
 *
 * would incorrectly be processed as:
 *
 * ```
 * user
 * id
 * name
 * ```
 *
 * instead of:
 *
 * ```
 * user
 * ├── id
 * └── name
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class IndentationAnalyzer {

    /**
     * Analyzes the indentation structure of all processable input lines.
     *
     * Empty lines and comments are ignored for hierarchy construction.
     *
     * @param lines physical input lines with metadata
     * @return indentation structure containing only true root lines
     * @throws ToonParseException if indentation is invalid
     */
    fun analyzeIndentation(lines: List<LineInfo>): IndentationStructure {
        if (lines.isEmpty()) {
            return IndentationStructure(emptyList())
        }

        val context = KToonContext.current
        val rootLines = mutableListOf<ProcessedIndentationLine>()
        val stack = mutableListOf<ProcessedIndentationLine>()

        lines.filter { it.shouldProcess() }.forEach { line ->
            val parent = findParent(line, stack, context)

            validateIndentationRules(
                line = line,
                parent = parent,
                stack = stack,
                context = context
            )

            val processedLine = ProcessedIndentationLine(
                lineInfo = line,
                parent = parent,
                children = mutableListOf(),
                depth = parent?.depth?.plus(1) ?: 0
            )

            if (parent == null) {
                rootLines.add(processedLine)
            } else {
                parent.children.add(processedLine)
            }

            updateStack(processedLine, stack)
        }

        return IndentationStructure(rootLines)
    }

    /**
     * Finds the parent line for the current line based on indentation.
     *
     * @param line current line
     * @param stack current hierarchy stack
     * @param context active parser context
     * @return parent line or null for root-level lines
     */
    private fun findParent(
        line: LineInfo,
        stack: List<ProcessedIndentationLine>,
        context: KToonContext
    ): ProcessedIndentationLine? {
        if (line.indentation == 0) {
            return null
        }

        for (index in stack.size - 1 downTo 0) {
            val candidate = stack[index]

            if (candidate.lineInfo.indentation < line.indentation) {
                return candidate
            }
        }

        if (context.strictMode) {
            throw ToonParseException(
                message = "Indented line has no valid parent",
                line = line.lineNumber,
                column = 1,
                context = line.content
            )
        }

        return null
    }

    /**
     * Validates indentation consistency for the current line.
     *
     * @param line current line
     * @param parent resolved parent line
     * @param stack current hierarchy stack
     * @param context active parser context
     * @throws ToonParseException if indentation is invalid
     */
    private fun validateIndentationRules(
        line: LineInfo,
        parent: ProcessedIndentationLine?,
        stack: List<ProcessedIndentationLine>,
        context: KToonContext
    ) {
        if (context.maxNestingDepth > 0) {
            val depth = parent?.depth?.plus(1) ?: 0

            if (depth > context.maxNestingDepth) {
                throw ToonParseException(
                    message = "Maximum nesting depth (${context.maxNestingDepth}) exceeded",
                    line = line.lineNumber,
                    column = 1,
                    context = line.content
                )
            }
        }

        if (line.indentation > 0 && parent == null && context.strictMode) {
            throw ToonParseException(
                message = "Indented line must have a parent",
                line = line.lineNumber,
                column = 1,
                context = line.content
            )
        }

        if (parent != null && line.indentation <= parent.lineInfo.indentation) {
            throw ToonParseException(
                message = "Child indentation must be greater than parent indentation",
                line = line.lineNumber,
                column = 1,
                context = line.content
            )
        }

        validateIndentationConsistency(line)
    }

    /**
     * Validates that a single line does not mix spaces and tabs.
     *
     * @param currentLine line to validate
     * @throws ToonParseException if spaces and tabs are mixed
     */
    private fun validateIndentationConsistency(currentLine: LineInfo) {
        val content = currentLine.content
        var hasSpaces = false
        var hasTabs = false

        for (char in content) {
            when (char) {
                ' ' -> hasSpaces = true
                '\t' -> hasTabs = true
                else -> break
            }
        }

        if (hasSpaces && hasTabs && KToonContext.current.strictMode) {
            throw ToonParseException(
                message = "Mixing spaces and tabs for indentation is not allowed in strict mode",
                line = currentLine.lineNumber,
                column = 1,
                context = currentLine.content
            )
        }
    }

    /**
     * Updates the processing stack after a line has been processed.
     *
     * The stack always contains the current active path from root to the latest
     * processed line.
     *
     * @param processedLine newly processed line
     * @param stack mutable hierarchy stack
     */
    private fun updateStack(
        processedLine: ProcessedIndentationLine,
        stack: MutableList<ProcessedIndentationLine>
    ) {
        while (stack.isNotEmpty() && stack.last().lineInfo.indentation >= processedLine.lineInfo.indentation) {
            stack.removeLast()
        }

        stack.add(processedLine)
    }
}

/**
 * Represents one logical TOON line after indentation processing.
 *
 * @property lineInfo original physical line information
 * @property parent parent line or null for root-level lines
 * @property children nested child lines
 * @property depth logical tree depth
 *
 * @since 1.0.0
 * @author TyLabsX
 */
data class ProcessedIndentationLine(
    val lineInfo: LineInfo,
    var parent: ProcessedIndentationLine?,
    val children: MutableList<ProcessedIndentationLine>,
    val depth: Int
) {
    /**
     * Checks whether this line is a root-level line.
     *
     * @return true if this line has no parent
     */
    fun isRoot(): Boolean = parent == null

    /**
     * Checks whether this line has no children.
     *
     * @return true if this line is a leaf line
     */
    fun isLeaf(): Boolean = children.isEmpty()

    /**
     * Returns all descendant lines recursively.
     *
     * @return all nested lines below this line
     */
    fun getAllDescendants(): List<ProcessedIndentationLine> {
        val result = mutableListOf<ProcessedIndentationLine>()

        children.forEach { child ->
            result.add(child)
            result.addAll(child.getAllDescendants())
        }

        return result
    }

    /**
     * Returns the path from the root line to this line.
     *
     * @return ordered list from root to current line
     */
    fun getPath(): List<ProcessedIndentationLine> {
        val path = mutableListOf<ProcessedIndentationLine>()
        var current: ProcessedIndentationLine? = this

        while (current != null) {
            path.add(0, current)
            current = current.parent
        }

        return path
    }

    /**
     * Returns a debug representation of this processed line.
     *
     * @return debug string
     */
    override fun toString(): String {
        val indent = "  ".repeat(depth)
        val parentInfo = parent?.let { " (parent: line ${it.lineInfo.lineNumber})" } ?: ""
        val childrenInfo = " (${children.size} children)"

        return "${indent}Line${lineInfo.lineNumber}: depth=$depth$parentInfo$childrenInfo"
    }
}

/**
 * Represents the complete indentation hierarchy of a TOON document.
 *
 * Important:
 * rootLines contains only real root-level lines.
 *
 * @property rootLines root-level indentation lines
 *
 * @since 1.0.0
 * @author TyLabsX
 */
data class IndentationStructure(
    val rootLines: List<ProcessedIndentationLine>
) {
    /**
     * Returns all lines in stable document order.
     *
     * @return flattened hierarchy
     */
    fun getAllLines(): List<ProcessedIndentationLine> {
        val result = mutableListOf<ProcessedIndentationLine>()

        fun collect(lines: List<ProcessedIndentationLine>) {
            lines.forEach { line ->
                result.add(line)
                collect(line.children)
            }
        }

        collect(rootLines)
        return result
    }

    /**
     * Finds a processed line by original source line number.
     *
     * @param lineNumber source line number
     * @return matching line or null
     */
    fun findLineByNumber(lineNumber: Int): ProcessedIndentationLine? {
        return getAllLines().find { it.lineInfo.lineNumber == lineNumber }
    }

    /**
     * Validates the indentation tree.
     *
     * @throws ToonParseException if a child has invalid indentation
     */
    fun validate() {
        rootLines.forEach { validateLine(it) }
    }

    /**
     * Recursively validates one processed line.
     *
     * @param line line to validate
     * @throws ToonParseException if the line is invalid
     */
    private fun validateLine(line: ProcessedIndentationLine) {
        line.children.forEach { child ->
            if (child.lineInfo.indentation <= line.lineInfo.indentation) {
                throw ToonParseException(
                    message = "Child indentation must be greater than parent indentation",
                    line = child.lineInfo.lineNumber,
                    column = 1,
                    context = child.lineInfo.content
                )
            }

            if (child.parent != line) {
                throw ToonParseException(
                    message = "Invalid parent-child relationship",
                    line = child.lineInfo.lineNumber,
                    column = 1,
                    context = child.lineInfo.content
                )
            }

            validateLine(child)
        }
    }

    /**
     * Returns a formatted debug representation of the indentation tree.
     *
     * @return debug string
     */
    override fun toString(): String {
        val builder = StringBuilder()

        fun printLines(lines: List<ProcessedIndentationLine>, prefix: String = "") {
            lines.forEach { line ->
                builder.appendLine("$prefix${line.lineInfo.content}")
                printLines(line.children, "$prefix  ")
            }
        }

        printLines(rootLines)
        return builder.toString()
    }
}
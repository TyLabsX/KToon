package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.*
import java.util.IdentityHashMap

/**
 * Central orchestration engine for TOON parsing operations.
 *
 * This class coordinates the complete parsing pipeline:
 *
 * 1. Input normalization
 * 2. Line splitting
 * 3. Indentation analysis
 * 4. Syntax classification
 * 5. Key-value extraction
 * 6. Structure building
 * 7. Value resolution
 *
 * This parser resolves the final public ToonValue from ClassifiedLine data.
 * That is important because official TOON v2.1 features such as tabular arrays
 * and block arrays are syntax-level constructs and should not be reduced too
 * early by the generic node builder.
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonParserEngine {

    private val inputNormalizer = InputNormalizer()
    private val lineSplitter = LineSplitter()
    private val indentationAnalyzer = IndentationAnalyzer()
    private val syntaxClassifier = SyntaxClassifier()
    private val valueResolver = ValueResolver()

    /**
     * Parses raw TOON input into a resolved ToonValue document.
     *
     * @param input raw TOON input
     * @return parse result containing final value and intermediate pipeline artifacts
     * @throws ToonParseException if parsing fails
     */
    fun parse(input: String): ParseResult {
        val context = KToonContext.current

        try {
            val normalizedInput = if (context.enableDebugLogging) {
                debugLog("Phase 1: Input Normalization")
                inputNormalizer.normalize(input).also {
                    debugLog("Normalized input: ${it.take(100)}")
                }
            } else {
                inputNormalizer.normalize(input)
            }

            val lines = if (context.enableDebugLogging) {
                debugLog("Phase 2: Line Splitting")
                lineSplitter.splitIntoLines(normalizedInput).also {
                    debugLog("Split into ${it.size} lines")
                }
            } else {
                lineSplitter.splitIntoLines(normalizedInput)
            }

            lineSplitter.validateLines(lines)

            val indentationStructure = if (context.enableDebugLogging) {
                debugLog("Phase 3: Indentation Analysis")
                indentationAnalyzer.analyzeIndentation(lines).also {
                    debugLog("Root lines: ${it.rootLines.size}")
                }
            } else {
                indentationAnalyzer.analyzeIndentation(lines)
            }

            indentationStructure.validate()

            val classifiedLines = if (context.enableDebugLogging) {
                debugLog("Phase 4: Syntax Classification")
                syntaxClassifier.classifyLines(indentationStructure).also {
                    debugLog("Classified lines: ${it.size}")
                }
            } else {
                syntaxClassifier.classifyLines(indentationStructure)
            }

            val invalidLines = classifiedLines.filter { !it.isValid }
            if (invalidLines.isNotEmpty()) {
                val firstInvalid = invalidLines.first()
                throw ToonParseException(
                    message = firstInvalid.errorMessage ?: "Invalid syntax",
                    line = firstInvalid.indentationLine.lineInfo.lineNumber,
                    column = 1,
                    context = firstInvalid.indentationLine.lineInfo.content
                )
            }

            val toonValue = if (context.enableDebugLogging) {
                debugLog("Phase 7: Value Resolution")
                resolveClassifiedLines(classifiedLines).also {
                    debugLog("Resolved value: $it")
                }
            } else {
                resolveClassifiedLines(classifiedLines)
            }

            return ParseResult(
                toonValue = toonValue,
                classifiedLines = classifiedLines,
                indentationStructure = indentationStructure,
                lines = lines,
                normalizedInput = normalizedInput
            )
        } catch (e: ToonParseException) {
            throw e
        } catch (e: Exception) {
            throw ToonParseException(
                message = "Unexpected parsing error: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Resolves classified lines directly into the public ToonValue model.
     *
     * This resolver supports:
     *
     * - normal objects
     * - scalar values
     * - inline primitive arrays
     * - tabular arrays
     * - block-style arrays with list items
     *
     * Example:
     *
     * ```toon
     * app:
     *   users[2]{id,name}:
     *     1,Ada
     *     2,Bob
     *   environments[2]:
     *     - name: dev
     *       url: localhost
     *     - name: prod
     *       url: api
     * ```
     *
     * @param classifiedLines classified parser lines
     * @return resolved ToonValue document
     */
    private fun resolveClassifiedLines(classifiedLines: List<ClassifiedLine>): ToonValue {
        val included = classifiedLines.filter { it.shouldIncludeInStructure() }
        val context = ResolutionContext(included)
        val rootLines = included.filter { it.indentationLine.parent == null }

        val entries = linkedMapOf<String, ToonValue>()

        rootLines.forEach { line ->
            val rawKey = line.key ?: throw ToonParseException(
                message = "Root line missing key",
                line = line.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = line.indentationLine.lineInfo.content
            )

            val value = resolveClassifiedLine(line, context)

            putEntryWithOptionalKeyFolding(
                target = entries,
                rawKey = rawKey,
                value = value,
                line = line
            )
        }

        return ToonObject(entries)
    }

    /**
     * Resolves one classified line.
     *
     * @param line current line
     * @param allLines all included classified lines
     * @return resolved ToonValue
     */
    private fun resolveClassifiedLine(
        line: ClassifiedLine,
        context: ResolutionContext
    ): ToonValue {
        return when (line.type) {
            LineType.TABULAR_ARRAY_HEADER -> resolveTabularArray(line, context)

            LineType.TABULAR_ARRAY_ROW -> {
                throw ToonParseException(
                    message = "Tabular array row cannot be resolved outside a tabular array header",
                    line = line.indentationLine.lineInfo.lineNumber,
                    column = 1,
                    context = line.indentationLine.lineInfo.content
                )
            }

            LineType.INLINE_ARRAY -> resolveInlineArray(line)

            LineType.KEY_ONLY -> {
                val key = line.key ?: ""
                val children = context.directChildren(line)

                if (isArrayHeader(key)) {
                    resolveBlockArray(line, children, context)
                } else {
                    resolveObjectFromChildren(children, context)
                }
            }

            LineType.KEY_VALUE -> {
                val children = context.directChildren(line)

                if (children.isNotEmpty()) {
                    throw ToonParseException(
                        message = "Scalar value cannot have child elements",
                        line = line.indentationLine.lineInfo.lineNumber,
                        column = 1,
                        context = line.indentationLine.lineInfo.content
                    )
                }

                valueResolver.resolveValue(line.value ?: "")
            }

            else -> {
                throw ToonParseException(
                    message = "Unsupported line type in resolver: ${line.type}",
                    line = line.indentationLine.lineInfo.lineNumber,
                    column = 1,
                    context = line.indentationLine.lineInfo.content
                )
            }
        }
    }

    /**
     * Resolves a normal object from child lines.
     *
     * @param children direct children
     * @param allLines all included classified lines
     * @return resolved ToonObject
     */
    private fun resolveObjectFromChildren(
        children: List<ClassifiedLine>,
        context: ResolutionContext
    ): ToonObject {
        val entries = LinkedHashMap<String, ToonValue>(children.size)

        children.forEach { child ->
            val childKey = child.key ?: throw ToonParseException(
                message = "Object child missing key",
                line = child.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = child.indentationLine.lineInfo.content
            )

            entries[cleanKeyName(childKey)] = resolveClassifiedLine(child, context)
        }

        return ToonObject(entries)
    }

    /**
     * Resolves an inline primitive array.
     *
     * Example:
     *
     * ```toon
     * tags[3]: kotlin,toon,llm
     * ```
     *
     * @param line inline array line
     * @return resolved ToonArray
     */
    private fun resolveInlineArray(line: ClassifiedLine): ToonArray {
        val rawValues = splitDelimitedValues(line.value ?: "", ',')
        val values = ArrayList<ToonValue>(rawValues.size)
        rawValues.forEach { value ->
            values.add(valueResolver.resolveValue(value))
        }

        validateArrayLength(
            key = line.key ?: "",
            actual = values.size,
            line = line.indentationLine.lineInfo.lineNumber,
            context = line.indentationLine.lineInfo.content
        )

        return ToonArray(values)
    }

    /**
     * Resolves a TOON tabular array into an array of objects.
     *
     * Example:
     *
     * ```toon
     * users[2]{id,name}:
     *   1,Ada
     *   2,Bob
     * ```
     *
     * @param headerLine tabular array header line
     * @param allLines all included classified lines
     * @return resolved ToonArray
     */
    private fun resolveTabularArray(
        headerLine: ClassifiedLine,
        context: ResolutionContext
    ): ToonArray {
        val headers = headerLine.arrayHeaders

        if (headers.isEmpty()) {
            throw ToonParseException(
                message = "Tabular array must declare at least one header",
                line = headerLine.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = headerLine.indentationLine.lineInfo.content
            )
        }

        val children = context.directChildren(headerLine)
        val rows = ArrayList<ClassifiedLine>(children.size)
        children.forEach { child ->
            if (child.type == LineType.TABULAR_ARRAY_ROW) {
                rows.add(child)
            }
        }

        validateArrayLength(
            key = headerLine.key ?: "",
            actual = rows.size,
            line = headerLine.indentationLine.lineInfo.lineNumber,
            context = headerLine.indentationLine.lineInfo.content
        )

        val objects = ArrayList<ToonValue>(rows.size)

        rows.forEach { row ->
            val values = splitDelimitedValues(row.value ?: "", ',')

            if (values.size != headers.size) {
                throw ToonParseException(
                    message = "Tabular row column mismatch: expected ${headers.size} but got ${values.size}",
                    line = row.indentationLine.lineInfo.lineNumber,
                    column = 1,
                    context = row.indentationLine.lineInfo.content
                )
            }

            val entries = LinkedHashMap<String, ToonValue>(headers.size)
            for (index in headers.indices) {
                entries[headers[index]] = valueResolver.resolveValue(values[index])
            }
            objects.add(ToonObject(entries))
        }

        return ToonArray(objects)
    }

    /**
     * Resolves a block-style TOON array.
     *
     * Example:
     *
     * ```toon
     * environments[2]:
     *   - name: dev
     *     url: localhost
     *   - name: prod
     *     url: api
     * ```
     *
     * @param headerLine array header
     * @param directChildren direct children below the array header
     * @param allLines all included classified lines
     * @return resolved ToonArray
     */
    private fun resolveBlockArray(
        headerLine: ClassifiedLine,
        directChildren: List<ClassifiedLine>,
        context: ResolutionContext
    ): ToonArray {
        val values = ArrayList<ToonValue>(directChildren.size)

        directChildren.forEach { child ->
            val key = child.key ?: ""

            if (!isListItemKey(key)) {
                throw ToonParseException(
                    message = "Block array item must start with '-'",
                    line = child.indentationLine.lineInfo.lineNumber,
                    column = 1,
                    context = child.indentationLine.lineInfo.content
                )
            }

            values.add(resolveArrayItem(child, context))
        }

        validateArrayLength(
            key = headerLine.key ?: "",
            actual = values.size,
            line = headerLine.indentationLine.lineInfo.lineNumber,
            context = headerLine.indentationLine.lineInfo.content
        )

        return ToonArray(values)
    }

    /**
     * Resolves a single block-array item.
     *
     * Supported item forms:
     *
     * ```toon
     * - name: dev
     *   url: localhost
     * ```
     *
     * and:
     *
     * ```toon
     * - value
     * ```
     *
     * @param itemLine list item line
     * @param allLines all included classified lines
     * @return resolved array item
     */
    private fun resolveArrayItem(
        itemLine: ClassifiedLine,
        context: ResolutionContext
    ): ToonValue {
        val rawKey = itemLine.key ?: ""
        val itemKey = rawKey.removePrefix("-").trim()
        val children = context.directChildren(itemLine)

        if (itemKey.isEmpty()) {
            return if (children.isEmpty()) {
                valueResolver.resolveValue(itemLine.value ?: "")
            } else {
                resolveObjectFromChildren(children, context)
            }
        }

        val entries = linkedMapOf<String, ToonValue>()
        entries[cleanKeyName(itemKey)] = valueResolver.resolveValue(itemLine.value ?: "")

        children.forEach { child ->
            val childKey = child.key ?: throw ToonParseException(
                message = "Array item child missing key",
                line = child.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = child.indentationLine.lineInfo.content
            )

            entries[cleanKeyName(childKey)] = resolveClassifiedLine(child, context)
        }

        return ToonObject(entries)
    }

    /**
     * Validates array length declarations.
     *
     * @param key raw array key
     * @param actual actual item count
     * @param line source line number
     * @param context source context
     * @throws ToonParseException if declared and actual length differ
     */
    private fun validateArrayLength(
        key: String,
        actual: Int,
        line: Int,
        context: String?
    ) {
        val expected = extractArrayLength(key) ?: return

        if (expected != actual) {
            throw ToonParseException(
                message = "Array length mismatch: expected $expected but got $actual",
                line = line,
                column = 1,
                context = context
            )
        }
    }

    /**
     * Splits comma-delimited TOON values while respecting quotes and escapes.
     *
     * @param input raw delimited input
     * @param delimiter active delimiter
     * @return parsed value fragments
     */
    private fun splitDelimitedValues(input: String, delimiter: Char): List<String> {
        if (input.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var escaped = false

        input.forEach { char ->
            when {
                escaped -> {
                    current.append('\\')
                    current.append(char)
                    escaped = false
                }

                char == '\\' -> {
                    escaped = true
                }

                char == '"' -> {
                    inQuotes = !inQuotes
                    current.append(char)
                }

                char == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }

                else -> current.append(char)
            }
        }

        if (escaped) {
            current.append('\\')
        }

        result.add(current.toString().trim())
        return result
    }

    /**
     * Extracts the array length from a TOON array key.
     *
     * Examples:
     * - `tags[3]` -> 3
     * - `users[2]{id,name}` -> 2
     *
     * @param key raw key
     * @return array length or null
     */
    private fun extractArrayLength(key: String): Int? {
        val start = key.indexOf('[')
        val end = key.indexOf(']', startIndex = start + 1)

        if (start == -1 || end == -1 || end <= start) {
            return null
        }

        return key.substring(start + 1, end).toIntOrNull()
    }

    /**
     * Checks whether a key contains TOON array length syntax.
     *
     * @param key raw key
     * @return true if key contains `[N]`
     */
    private fun isArrayHeader(key: String): Boolean {
        return extractArrayLength(key) != null
    }

    /**
     * Checks whether a key represents an inline array.
     *
     * @param key raw key
     * @return true if this is an array key without tabular headers
     */
    private fun isInlineArrayKey(key: String): Boolean {
        return isArrayHeader(key) && !key.contains('{')
    }

    /**
     * Checks whether a key belongs to a block-array list item.
     *
     * @param key raw key
     * @return true if key starts with "-"
     */
    private fun isListItemKey(key: String): Boolean {
        return key.trimStart().startsWith("-")
    }

    /**
     * Removes TOON array metadata and list item prefixes from a key.
     *
     * Examples:
     * - `tags[3]` -> `tags`
     * - `users[2]{id,name}` -> `users`
     * - `- name` -> `name`
     *
     * @param key raw key
     * @return clean object key
     */
    private fun cleanKeyName(key: String): String {
        return key
            .removePrefix("-")
            .trim()
            .substringBefore('[')
            .substringBefore('{')
            .trim()
    }

    /**
     * Parses a single TOON line.
     *
     * This is a convenience method for tests and simple parser usage.
     *
     * @param line single TOON line
     * @return parse result
     * @throws ToonParseException if the line is invalid
     */
    fun parseLine(line: String): ParseResult {
        return parse(line)
    }

    /**
     * Validates TOON input by attempting to parse it.
     *
     * @param input TOON input
     * @return validation result
     */
    fun validate(input: String): ValidationResult {
        return try {
            val result = parse(input)
            ValidationResult(
                isValid = true,
                errors = emptyList(),
                warnings = emptyList(),
                parseResult = result
            )
        } catch (e: ToonParseException) {
            ValidationResult(
                isValid = false,
                errors = listOf(e),
                warnings = emptyList(),
                parseResult = null
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf(
                    ToonParseException(
                        message = "Validation error: ${e.message}",
                        line = 0,
                        column = 0,
                        cause = e
                    )
                ),
                warnings = emptyList(),
                parseResult = null
            )
        }
    }

    /**
     * Returns static metadata about the parser pipeline.
     *
     * @return parser statistics
     */
    fun getParsingStatistics(): ParsingStatistics {
        return ParsingStatistics(
            phases = listOf(
                "Input Normalization",
                "Line Splitting",
                "Indentation Analysis",
                "Syntax Classification",
                "Key-Value Extraction",
                "Structure Building",
                "Value Resolution"
            ),
            totalPhases = 7,
            supportedFeatures = listOf(
                "Key Folding",
                "Inline Arrays",
                "Block Arrays",
                "Tabular Arrays",
                "Comments",
                "Quoted Strings",
                "Escape Sequences"
            )
        )
    }

    /**
     * Writes debug logs when enabled in KToonContext.
     *
     * @param message debug message
     */
    private fun debugLog(message: String) {
        if (KToonContext.current.enableDebugLogging) {
            println("[KToonParserEngine] $message")
        }
    }

    /**
     * Inserts a resolved value into a target object map.
     *
     * If key folding is enabled, dotted keys are expanded:
     *
     * user.id: 456
     *
     * becomes:
     *
     * user:
     *   id: 456
     *
     * If key folding is disabled, the key is inserted literally.
     *
     * @param target target object entries
     * @param rawKey raw key from the parsed line
     * @param value resolved value
     * @param line source line for error reporting
     */
    private fun putEntryWithOptionalKeyFolding(
        target: MutableMap<String, ToonValue>,
        rawKey: String,
        value: ToonValue,
        line: ClassifiedLine
    ) {
        val cleanedKey = cleanKeyName(rawKey)

        if (!KToonContext.current.enableKeyFolding || !cleanedKey.contains('.')) {
            target[cleanedKey] = value
            return
        }

        val parts = cleanedKey.split('.').filter { it.isNotBlank() }

        if (parts.isEmpty()) {
            throw ToonParseException(
                message = "Empty folded key",
                line = line.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = line.indentationLine.lineInfo.content
            )
        }

        putFoldedEntry(
            target = target,
            parts = parts,
            value = value,
            line = line
        )
    }

    /**
     * Recursively inserts a folded key path into a ToonObject map.
     *
     * @param target target entries
     * @param parts folded key parts
     * @param value value to insert
     * @param line source line for errors
     */
    private fun putFoldedEntry(
        target: MutableMap<String, ToonValue>,
        parts: List<String>,
        value: ToonValue,
        line: ClassifiedLine
    ) {
        val head = parts.first()

        if (parts.size == 1) {
            target[head] = value
            return
        }

        val existing = target[head]

        val childEntries = when (existing) {
            null -> linkedMapOf()

            is ToonObject -> existing.entries.toMutableMap()

            else -> throw ToonParseException(
                message = "Cannot fold key through non-object value '$head'",
                line = line.indentationLine.lineInfo.lineNumber,
                column = 1,
                context = line.indentationLine.lineInfo.content
            )
        }

        putFoldedEntry(
            target = childEntries,
            parts = parts.drop(1),
            value = value,
            line = line
        )

        target[head] = ToonObject(childEntries)
    }
}

/**
 * Per-parse lookup table used during value resolution.
 *
 * The indentation phase already builds parent/child links for every processed
 * line. Resolution uses this identity map to move from a processed child line
 * to its classified representation without repeatedly scanning the complete
 * document.
 */
private class ResolutionContext(
    includedLines: List<ClassifiedLine>
) {

    private val classifiedByIndentationLine = IdentityHashMap<ProcessedIndentationLine, ClassifiedLine>(
        includedLines.size
    )

    init {
        includedLines.forEach { line ->
            classifiedByIndentationLine[line.indentationLine] = line
        }
    }

    fun directChildren(line: ClassifiedLine): List<ClassifiedLine> {
        val children = line.indentationLine.children
        if (children.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<ClassifiedLine>(children.size)
        children.forEach { child ->
            classifiedByIndentationLine[child]?.let(result::add)
        }
        return result
    }
}

/**
 * Result of a TOON parsing operation.
 *
 * Contains the final ToonValue as well as all relevant intermediate parser
 * artifacts. This is useful for debugging, tests and future tooling.
 *
 * @property toonValue final parsed value
 * @property nodeStructure hierarchical node structure
 * @property classifiedLines syntax-classified lines
 * @property extractedPairs extracted key-value pairs
 * @property indentationStructure indentation analysis result
 * @property lines split input lines
 * @property normalizedInput normalized input string
 *
 * @since 1.0.0
 * @author TyLabsX
 */
data class ParseResult(
    val toonValue: ToonValue,
    val classifiedLines: List<ClassifiedLine>,
    val indentationStructure: IndentationStructure,
    val lines: List<LineInfo>,
    val normalizedInput: String
) {
    /**
     * Creates a human-readable summary of this parse result.
     *
     * @return summary text
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Parse Result Summary:")
            appendLine("  Final Value: $toonValue")
            appendLine("  Total Lines: ${lines.size}")
            appendLine("  Processed Lines: ${classifiedLines.size}")
            appendLine("  Root Nodes: ${indentationStructure.rootLines.size}")
        }
    }
}

/**
 * Result of a TOON validation operation.
 *
 * @property isValid true if the input is valid
 * @property errors parser errors
 * @property warnings non-fatal warnings
 * @property parseResult parse result when validation succeeds
 *
 * @since 1.0.0
 * @author TyLabsX
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ToonParseException>,
    val warnings: List<String>,
    val parseResult: ParseResult?
)

/**
 * Static parser pipeline metadata.
 *
 * @property phases parser phases
 * @property totalPhases number of parser phases
 * @property supportedFeatures declared parser features
 *
 * @since 1.0.0
 * @author TyLabsX
 */
data class ParsingStatistics(
    val phases: List<String>,
    val totalPhases: Int,
    val supportedFeatures: List<String>
)

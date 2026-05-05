package de.tylabsx.ktoon

/**
 * Internal parser used by [KToonNativeFormat] for serializer-driven decoding.
 *
 * The public [KToonParserEngine] keeps the full parsing pipeline and exposes
 * intermediate artifacts for tooling and diagnostics. Native decoding only
 * needs the final [ToonValue] tree, so this parser performs a single lightweight
 * pass over normalized TOON input and avoids the pipeline allocations.
 */
internal object KToonFastValueParser {

    private const val DEFAULT_INDENT_SIZE = 2

    /**
     * Parses a TOON document into the public [ToonValue] model.
     *
     * This parser is intentionally scoped to native kotlinx.serialization
     * decoding. It preserves the same value semantics required by
     * [KToonNativeFormat] while avoiding the diagnostic artifacts produced by
     * [KToonParserEngine].
     *
     * @param input TOON document
     * @return parsed [ToonValue] tree
     * @throws ToonParseException if the document cannot be parsed
     */
    fun parse(input: String): ToonValue {
        val lines = readLines(input)
        if (lines.isEmpty()) {
            return ToonObject(emptyMap())
        }

        val cursor = Cursor(lines)
        val value = parseObject(cursor, lines.first().indent)

        if (cursor.hasNext()) {
            val line = cursor.peek()
            throw parseError("Unexpected trailing line", line)
        }

        return value
    }

    private fun parseObject(cursor: Cursor, depth: Int): ToonObject {
        val entries = LinkedHashMap<String, ToonValue>()

        while (cursor.hasNext()) {
            val line = cursor.peek()
            when {
                line.indent < depth -> break
                line.indent > depth -> throw parseError("Unexpected indentation", line)
            }

            cursor.next()
            val entry = parseEntry(line, cursor, depth)
            putEntry(entries, entry.first, entry.second, line)
        }

        return ToonObject(entries)
    }

    private fun parseEntry(
        line: FastLine,
        cursor: Cursor,
        depth: Int
    ): Pair<String, ToonValue> {
        val colonIndex = findColon(line.content)

        if (colonIndex == -1) {
            throw parseError("Missing colon in key-value pair", line)
        }

        val key = line.content.substring(0, colonIndex).trim()
        val value = line.content.substring(colonIndex + 1).trim()

        if (key.isEmpty()) {
            throw parseError("Empty key is not allowed", line)
        }

        val resolvedValue = when {
            isTabularArrayHeader(key) -> parseTabularArray(key, cursor, depth + 1, line)
            value.isNotEmpty() && isArrayHeader(key) -> parseInlineArray(key, value, line)
            value.isNotEmpty() -> resolveValue(value, line)
            isArrayHeader(key) -> parseBlockArray(key, cursor, depth + 1, line)
            else -> parseObject(cursor, depth + 1)
        }

        return cleanKeyName(key) to resolvedValue
    }

    private fun parseInlineArray(
        key: String,
        value: String,
        line: FastLine
    ): ToonArray {
        val rawValues = splitDelimitedValues(value)
        validateArrayLength(key, rawValues.size, line)

        val values = ArrayList<ToonValue>(rawValues.size)
        rawValues.forEach { raw ->
            values.add(resolveValue(raw, line))
        }
        return ToonArray(values)
    }

    private fun parseTabularArray(
        key: String,
        cursor: Cursor,
        rowDepth: Int,
        line: FastLine
    ): ToonArray {
        val headers = extractTabularHeaders(key)
        if (headers.isEmpty()) {
            throw parseError("Tabular array must declare at least one header", line)
        }

        val rows = ArrayList<ToonValue>()
        while (cursor.hasNext()) {
            val rowLine = cursor.peek()
            when {
                rowLine.indent < rowDepth -> break
                rowLine.indent > rowDepth -> throw parseError("Unexpected indentation in tabular array", rowLine)
            }

            if (findColon(rowLine.content) != -1) {
                break
            }

            cursor.next()
            val rawValues = splitDelimitedValues(rowLine.content)
            if (rawValues.size != headers.size) {
                throw parseError(
                    "Tabular row column mismatch: expected ${headers.size} but got ${rawValues.size}",
                    rowLine
                )
            }

            val entries = LinkedHashMap<String, ToonValue>(headers.size)
            for (index in headers.indices) {
                entries[headers[index]] = resolveValue(rawValues[index], rowLine)
            }
            rows.add(ToonObject(entries))
        }

        validateArrayLength(key, rows.size, line)
        return ToonArray(rows)
    }

    private fun parseBlockArray(
        key: String,
        cursor: Cursor,
        itemDepth: Int,
        line: FastLine
    ): ToonArray {
        val values = ArrayList<ToonValue>()

        while (cursor.hasNext()) {
            val itemLine = cursor.peek()
            when {
                itemLine.indent < itemDepth -> break
                itemLine.indent > itemDepth -> throw parseError("Unexpected indentation in block array", itemLine)
            }

            if (!itemLine.content.trimStart().startsWith("-")) {
                break
            }

            cursor.next()
            values.add(parseArrayItem(itemLine, cursor, itemDepth + 1))
        }

        validateArrayLength(key, values.size, line)
        return ToonArray(values)
    }

    private fun parseArrayItem(
        itemLine: FastLine,
        cursor: Cursor,
        childDepth: Int
    ): ToonValue {
        val content = itemLine.content.trimStart()
        val itemContent = content.removePrefix("-").trimStart()
        val colonIndex = findColon(itemContent)

        if (colonIndex == -1) {
            if (itemContent.isNotEmpty()) {
                return resolveValue(itemContent, itemLine)
            }

            return if (cursor.hasNext() && cursor.peek().indent >= childDepth) {
                parseObject(cursor, childDepth)
            } else {
                ToonString("")
            }
        }

        val key = itemContent.substring(0, colonIndex).trim()
        val value = itemContent.substring(colonIndex + 1).trim()
        if (key.isEmpty()) {
            throw parseError("Array item child missing key", itemLine)
        }

        val entries = LinkedHashMap<String, ToonValue>()
        entries[cleanKeyName(key)] = if (value.isEmpty() && cursor.hasNext() && cursor.peek().indent >= childDepth) {
            parseObject(cursor, childDepth)
        } else if (value.isNotEmpty() && isArrayHeader(key)) {
            parseInlineArray(key, value, itemLine)
        } else {
            resolveValue(value, itemLine)
        }

        while (cursor.hasNext()) {
            val child = cursor.peek()
            when {
                child.indent < childDepth -> break
                child.indent > childDepth -> throw parseError("Unexpected indentation in array item", child)
            }

            cursor.next()
            val entry = parseEntry(child, cursor, childDepth)
            putEntry(entries, entry.first, entry.second, child)
        }

        return ToonObject(entries)
    }

    private fun resolveValue(rawValue: String, line: FastLine): ToonValue {
        if (rawValue.isEmpty()) {
            return ToonString("")
        }

        val value = rawValue.trim()
        return when {
            value == "null" -> ToonNull
            value == "true" -> ToonBoolean(true)
            value == "false" -> ToonBoolean(false)
            isNumber(value) -> ToonNumber(value)
            isQuotedString(value) -> ToonString(unescapeQuotedString(value, line))
            containsUnquotedComma(value) -> parseAnonymousInlineArray(value, line)
            else -> ToonString(value)
        }
    }

    private fun parseAnonymousInlineArray(value: String, line: FastLine): ToonArray {
        val rawValues = splitDelimitedValues(value)
        val values = ArrayList<ToonValue>(rawValues.size)
        rawValues.forEach { raw ->
            values.add(resolveValue(raw, line))
        }
        return ToonArray(values)
    }

    private fun readLines(input: String): List<FastLine> {
        if (input.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<FastLine>()
        val indentSize = DEFAULT_INDENT_SIZE
        var lineStart = 0
        var lineNumber = 1
        var index = 0

        while (index <= input.length) {
            if (index == input.length || input[index] == '\n') {
                var lineEnd = index
                if (lineEnd > lineStart && input[lineEnd - 1] == '\r') {
                    lineEnd--
                }

                addLine(input, lineStart, lineEnd, lineNumber, indentSize, result)
                lineStart = index + 1
                lineNumber++
            }
            index++
        }

        return result
    }

    private fun addLine(
        input: String,
        start: Int,
        end: Int,
        lineNumber: Int,
        indentSize: Int,
        target: MutableList<FastLine>
    ) {
        var cursor = start
        var spaces = 0
        var tabs = 0

        while (cursor < end) {
            when (input[cursor]) {
                ' ' -> spaces++
                '\t' -> tabs++
                else -> break
            }
            cursor++
        }

        if (cursor >= end) {
            return
        }

        if (input[cursor] == '#') {
            return
        }

        val indent = if (tabs > 0) {
            tabs
        } else {
            spaces / indentSize
        }

        target.add(
            FastLine(
                indent = indent,
                content = input.substring(cursor, end),
                lineNumber = lineNumber
            )
        )
    }

    private fun findColon(content: String): Int {
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false

        for (index in content.indices) {
            val char = content[index]
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                char == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                char == ':' && !inSingleQuote && !inDoubleQuote -> return index
            }
        }

        return -1
    }

    private fun splitDelimitedValues(input: String): List<String> {
        if (input.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<String>()
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

                char == '\\' -> escaped = true

                char == '"' -> {
                    inQuotes = !inQuotes
                    current.append(char)
                }

                char == ',' && !inQuotes -> {
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

    private fun isNumber(value: String): Boolean {
        var index = 0
        if (value.isEmpty()) return false

        if (value[index] == '-') {
            index++
            if (index == value.length) return false
        }

        val integerStart = index
        while (index < value.length && value[index].isDigit()) {
            index++
        }
        if (index == integerStart) return false

        if (index < value.length && value[index] == '.') {
            index++
            val fractionStart = index
            while (index < value.length && value[index].isDigit()) {
                index++
            }
            if (index == fractionStart) return false
        }

        if (index < value.length && (value[index] == 'e' || value[index] == 'E')) {
            index++
            if (index < value.length && (value[index] == '+' || value[index] == '-')) {
                index++
            }

            val exponentStart = index
            while (index < value.length && value[index].isDigit()) {
                index++
            }
            if (index == exponentStart) return false
        }

        return index == value.length
    }

    private fun isQuotedString(value: String): Boolean {
        return value.length >= 2 &&
            ((value.first() == '"' && value.last() == '"') ||
                (value.first() == '\'' && value.last() == '\''))
    }

    private fun unescapeQuotedString(value: String, line: FastLine): String {
        val quote = value.first()
        val result = StringBuilder(value.length - 2)
        var index = 1
        val end = value.length - 1

        while (index < end) {
            val char = value[index]
            if (char == '\\' && index + 1 < end) {
                result.append(
                    when (val escaped = value[index + 1]) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        '\\' -> '\\'
                        '"' -> '"'
                        '\'' -> '\''
                        '/' -> '/'
                        else -> throw parseError("Invalid escape sequence: \\$escaped", line)
                    }
                )
                index += 2
            } else {
                if (char == quote) {
                    throw parseError("Unescaped quote character inside string", line)
                }
                result.append(char)
                index++
            }
        }

        return result.toString()
    }

    private fun isArrayHeader(key: String): Boolean {
        return extractArrayLength(key) != null
    }

    private fun isTabularArrayHeader(key: String): Boolean {
        return key.contains('{') && key.contains('}') && key.endsWith('}')
    }

    private fun extractArrayLength(key: String): Int? {
        val start = key.indexOf('[')
        if (start == -1) return null
        val end = key.indexOf(']', start + 1)
        if (end == -1 || end <= start) return null
        return key.substring(start + 1, end).toIntOrNull()
    }

    private fun extractTabularHeaders(key: String): List<String> {
        val start = key.indexOf('{')
        val end = key.indexOf('}', start + 1)
        if (start == -1 || end == -1 || end <= start) {
            return emptyList()
        }

        return splitDelimitedValues(key.substring(start + 1, end))
    }

    private fun validateArrayLength(key: String, actual: Int, line: FastLine) {
        val expected = extractArrayLength(key) ?: return
        if (expected != actual) {
            throw parseError("Array length mismatch: expected $expected but got $actual", line)
        }
    }

    private fun cleanKeyName(key: String): String {
        return key
            .removePrefix("-")
            .trim()
            .substringBefore('[')
            .substringBefore('{')
            .trim()
    }

    private fun putEntry(
        target: MutableMap<String, ToonValue>,
        key: String,
        value: ToonValue,
        line: FastLine
    ) {
        if (!key.contains('.')) {
            target[key] = value
            return
        }

        val parts = key.split('.').filter { it.isNotBlank() }
        if (parts.isEmpty()) {
            throw parseError("Empty folded key", line)
        }

        putFoldedEntry(target, parts, value, line)
    }

    private fun putFoldedEntry(
        target: MutableMap<String, ToonValue>,
        parts: List<String>,
        value: ToonValue,
        line: FastLine
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
            else -> throw parseError("Cannot fold key through non-object value '$head'", line)
        }

        putFoldedEntry(childEntries, parts.drop(1), value, line)
        target[head] = ToonObject(childEntries)
    }

    private fun parseError(message: String, line: FastLine): ToonParseException {
        return ToonParseException(
            message = message,
            line = line.lineNumber,
            column = 1,
            context = line.content
        )
    }

    private data class FastLine(
        val indent: Int,
        val content: String,
        val lineNumber: Int
    )

    private class Cursor(
        private val lines: List<FastLine>
    ) {

        private var index = 0

        fun hasNext(): Boolean = index < lines.size

        fun peek(): FastLine = lines[index]

        fun next(): FastLine = lines[index++]
    }
}

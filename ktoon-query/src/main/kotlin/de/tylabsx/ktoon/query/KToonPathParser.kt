package de.tylabsx.ktoon.query

internal object KToonPathParser {

    fun parse(path: String): List<PathSegment> {
        if (path.isEmpty()) {
            throw KToonQueryException("Path must not be empty")
        }

        val segments = ArrayList<PathSegment>(4)
        var index = 0

        while (index < path.length) {
            if (path[index] == '.') {
                throw invalidPath(path, index, "empty path segment")
            }

            if (path[index] == '[') {
                index = parseArraySelector(path, index, segments)
            } else {
                index = parseProperty(path, index, segments)
            }

            if (index < path.length) {
                when (path[index]) {
                    '.' -> {
                        index++
                        if (index == path.length) {
                            throw invalidPath(path, index - 1, "trailing dot")
                        }
                    }
                    '[' -> Unit
                    else -> throw invalidPath(path, index, "expected '.' or '['")
                }
            }
        }

        return segments
    }

    private fun parseProperty(
        path: String,
        start: Int,
        segments: MutableList<PathSegment>
    ): Int {
        var index = start
        while (index < path.length && path[index] != '.' && path[index] != '[' && path[index] != ']') {
            index++
        }

        if (index == start) {
            throw invalidPath(path, start, "empty property name")
        }

        segments.add(PropertySegment(path.substring(start, index)))
        return index
    }

    private fun parseArraySelector(
        path: String,
        start: Int,
        segments: MutableList<PathSegment>
    ): Int {
        var index = start + 1
        if (index >= path.length) {
            throw invalidPath(path, start, "unterminated array selector")
        }

        if (path[index] == '*') {
            index++
            if (index >= path.length || path[index] != ']') {
                throw invalidPath(path, index, "wildcard selector must be '[*]'")
            }
            segments.add(WildcardSegment)
            return index + 1
        }

        val numberStart = index
        while (index < path.length && path[index].isDigit()) {
            index++
        }

        if (index == numberStart) {
            throw invalidPath(path, start, "array selector must contain a non-negative index or '*'")
        }

        if (index >= path.length || path[index] != ']') {
            throw invalidPath(path, index, "unterminated array selector")
        }

        val rawIndex = path.substring(numberStart, index)
        val arrayIndex = rawIndex.toIntOrNull()
            ?: throw invalidPath(path, numberStart, "array index is too large")

        segments.add(IndexSegment(arrayIndex))
        return index + 1
    }

    private fun invalidPath(path: String, index: Int, reason: String): KToonQueryException {
        return KToonQueryException("Invalid path '$path' at index $index: $reason")
    }
}

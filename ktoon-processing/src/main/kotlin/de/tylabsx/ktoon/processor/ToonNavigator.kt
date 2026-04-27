package de.tylabsx.ktoon.processor

import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonNull
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonValue
import de.tylabsx.ktoon.pipeline.QueryMatch

/**
 * Navigates and queries TOON data structures.
 *
 * This navigator supports:
 *
 * - dot paths: `user.name`
 * - array indexes: `users[0].name`
 * - wildcard traversal: `users.*.name`
 * - root prefix: `$.users.*.name`
 *
 * Query examples:
 *
 * ```kotlin
 * navigator.executeQuery(value, "users[0].name")
 * navigator.executeQuery(value, "users.*.name")
 * navigator.executeQuery(value, "$.company.employees.*.id")
 * ```
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonNavigator {

    /**
     * Returns all reachable paths in the given TOON structure.
     *
     * Object paths are represented using dot notation.
     * Array paths are represented using bracket notation.
     *
     * Example:
     *
     * ```text
     * users[0].name
     * users[1].name
     * ```
     *
     * @param value root value to inspect
     * @return all reachable paths
     */
    fun getAllPaths(value: ToonValue): List<String> {
        val paths = mutableListOf<String>()
        collectPaths(value, currentPath = "", paths = paths)
        return paths.distinct()
    }

    /**
     * Gets a single value by exact path.
     *
     * Supported:
     *
     * - `user.name`
     * - `users[0]`
     * - `users[0].name`
     *
     * Unsupported wildcards return [ToonNull].
     *
     * @param value root value
     * @param path exact path
     * @return value at path or [ToonNull] when no value exists
     */
    fun getValueByPath(value: ToonValue, path: String): ToonValue {
        if (path.isBlank() || path == "$") {
            return value
        }

        val parts = parseQuery(path.removePrefix("$."))
        var current: ToonValue = value

        for (part in parts) {
            current = resolveSinglePart(current, part) ?: return ToonNull
        }

        return current
    }

    /**
     * Executes a query against a TOON value.
     *
     * Supports exact and wildcard queries:
     *
     * ```text
     * users[0].name
     * users.*.name
     * *.name
     * $.users.*.name
     * ```
     *
     * @param value root value
     * @param query query string
     * @return matching query results
     */
    fun executeQuery(value: ToonValue, query: String): List<QueryMatch> {
        if (query.isBlank()) {
            return emptyList()
        }

        val normalizedQuery = query.removePrefix("$.")
        val parts = parseQuery(normalizedQuery)

        return queryRecursive(
            value = value,
            parts = parts,
            currentPath = ""
        )
    }

    /**
     * Recursively resolves query parts.
     *
     * @param value current value
     * @param parts remaining query parts
     * @param currentPath current resolved path
     * @return matching query results
     */
    private fun queryRecursive(
        value: ToonValue,
        parts: List<String>,
        currentPath: String
    ): List<QueryMatch> {
        if (parts.isEmpty()) {
            return listOf(QueryMatch(currentPath, value))
        }

        val head = parts.first()
        val tail = parts.drop(1)

        return when (value) {
            is ToonObject -> queryObject(value, head, tail, currentPath)
            is ToonArray -> queryArray(value, head, tail, currentPath)
            else -> emptyList()
        }
    }

    /**
     * Queries an object value.
     *
     * The wildcard `*` matches all direct entries of the object.
     *
     * @param obj object to query
     * @param head current query part
     * @param tail remaining query parts
     * @param currentPath current path
     * @return matching query results
     */
    private fun queryObject(
        obj: ToonObject,
        head: String,
        tail: List<String>,
        currentPath: String
    ): List<QueryMatch> {
        if (head == "*") {
            return obj.entries.flatMap { (key, child) ->
                queryRecursive(
                    value = child,
                    parts = tail,
                    currentPath = appendObjectPath(currentPath, key)
                )
            }
        }

        val objectKey = head.substringBefore("[")
        val child = obj.entries[objectKey] ?: return emptyList()
        val afterObjectPath = appendObjectPath(currentPath, objectKey)

        val indexes = extractIndexes(head)

        if (indexes.isEmpty()) {
            return queryRecursive(
                value = child,
                parts = tail,
                currentPath = afterObjectPath
            )
        }

        return resolveIndexesForQuery(
            value = child,
            indexes = indexes,
            remainingParts = tail,
            currentPath = afterObjectPath
        )
    }

    /**
     * Queries an array value.
     *
     * The wildcard `*` matches all direct array items.
     *
     * @param array array to query
     * @param head current query part
     * @param tail remaining query parts
     * @param currentPath current path
     * @return matching query results
     */
    private fun queryArray(
        array: ToonArray,
        head: String,
        tail: List<String>,
        currentPath: String
    ): List<QueryMatch> {
        if (head == "*") {
            return array.values.flatMapIndexed { index, child ->
                queryRecursive(
                    value = child,
                    parts = tail,
                    currentPath = appendArrayPath(currentPath, index)
                )
            }
        }

        val index = parseIndexToken(head) ?: return emptyList()
        val child = array.values.getOrNull(index) ?: return emptyList()

        return queryRecursive(
            value = child,
            parts = tail,
            currentPath = appendArrayPath(currentPath, index)
        )
    }

    /**
     * Resolves one exact path part.
     *
     * @param value current value
     * @param part path part
     * @return resolved value or null
     */
    private fun resolveSinglePart(value: ToonValue, part: String): ToonValue? {
        return when (value) {
            is ToonObject -> {
                val key = part.substringBefore("[")
                val child = value.entries[key] ?: return null
                resolveIndexes(child, extractIndexes(part))
            }

            is ToonArray -> {
                val index = parseIndexToken(part) ?: return null
                value.values.getOrNull(index)
            }

            else -> null
        }
    }

    /**
     * Resolves one or more indexes on a value.
     *
     * @param value value to index into
     * @param indexes indexes to apply
     * @return indexed value or null
     */
    private fun resolveIndexes(value: ToonValue, indexes: List<Int>): ToonValue? {
        var current: ToonValue = value

        indexes.forEach { index ->
            val array = current as? ToonArray ?: return null
            current = array.values.getOrNull(index) ?: return null
        }

        return current
    }

    /**
     * Resolves indexes during query execution.
     *
     * @param value value to index into
     * @param indexes indexes to apply
     * @param remainingParts remaining query parts
     * @param currentPath current path
     * @return matching query results
     */
    private fun resolveIndexesForQuery(
        value: ToonValue,
        indexes: List<Int>,
        remainingParts: List<String>,
        currentPath: String
    ): List<QueryMatch> {
        var currentValue: ToonValue = value
        var path = currentPath

        indexes.forEach { index ->
            val array = currentValue as? ToonArray ?: return emptyList()
            currentValue = array.values.getOrNull(index) ?: return emptyList()
            path = appendArrayPath(path, index)
        }

        return queryRecursive(
            value = currentValue,
            parts = remainingParts,
            currentPath = path
        )
    }

    /**
     * Parses a query string into path parts.
     *
     * This keeps bracket expressions attached to their path part:
     *
     * ```text
     * users[0].name -> ["users[0]", "name"]
     * ```
     *
     * @param query query string
     * @return parsed query parts
     */
    private fun parseQuery(query: String): List<String> {
        return query.split('.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Extracts all bracket indexes from a path part.
     *
     * Example:
     *
     * ```text
     * matrix[0][1] -> [0, 1]
     * ```
     *
     * @param part path part
     * @return extracted indexes
     */
    private fun extractIndexes(part: String): List<Int> {
        val result = mutableListOf<Int>()
        val regex = Regex("""\[(\d+)]""")

        regex.findAll(part).forEach { match ->
            result.add(match.groupValues[1].toInt())
        }

        return result
    }

    /**
     * Parses an array index token.
     *
     * Supported:
     *
     * - `0`
     * - `[0]`
     *
     * @param token token to parse
     * @return index or null
     */
    private fun parseIndexToken(token: String): Int? {
        return token
            .removePrefix("[")
            .removeSuffix("]")
            .toIntOrNull()
    }

    /**
     * Collects all paths recursively.
     *
     * @param value current value
     * @param currentPath current path
     * @param paths output list
     */
    private fun collectPaths(
        value: ToonValue,
        currentPath: String,
        paths: MutableList<String>
    ) {
        when (value) {
            is ToonObject -> {
                value.entries.forEach { (key, child) ->
                    val childPath = appendObjectPath(currentPath, key)
                    paths.add(childPath)
                    collectPaths(child, childPath, paths)
                }
            }

            is ToonArray -> {
                value.values.forEachIndexed { index, child ->
                    val childPath = appendArrayPath(currentPath, index)
                    paths.add(childPath)
                    collectPaths(child, childPath, paths)
                }
            }

            else -> {
                if (currentPath.isNotEmpty()) {
                    paths.add(currentPath)
                }
            }
        }
    }

    /**
     * Appends an object key to a path.
     *
     * @param base base path
     * @param key key to append
     * @return combined path
     */
    private fun appendObjectPath(base: String, key: String): String {
        return if (base.isEmpty()) key else "$base.$key"
    }

    /**
     * Appends an array index to a path.
     *
     * @param base base path
     * @param index array index
     * @return combined path
     */
    private fun appendArrayPath(base: String, index: Int): String {
        return "$base[$index]"
    }
}
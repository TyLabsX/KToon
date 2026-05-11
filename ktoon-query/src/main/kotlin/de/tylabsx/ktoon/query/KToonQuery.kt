package de.tylabsx.ktoon.query

import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonValue

/**
 * Path navigation API for [ToonValue] trees.
 *
 * The supported path syntax is intentionally small and deterministic:
 *
 * - `user`
 * - `user.name`
 * - `users[0]`
 * - `users[0].name`
 * - `users[*]`
 * - `users[*].name`
 *
 * [get] returns a single value and rejects wildcard paths. [select] returns all
 * matching values and is the only API that expands wildcards. Missing
 * properties, out-of-range indexes, and type mismatches return `null` from
 * [get] and an empty list from [select].
 *
 * @since 1.2.0
 */
object KToonQuery {

    /**
     * Returns the value at [path], or `null` when the path cannot be resolved.
     *
     * Wildcards are not allowed in [get] paths because the result would be
     * ambiguous. Use [select] for paths such as `users[*].name`.
     *
     * @param root root value to navigate
     * @param path property/index path
     * @return matched value, or `null` if missing
     * @throws KToonQueryException when [path] is invalid or contains a wildcard
     */
    fun get(root: ToonValue, path: String): ToonValue? {
        val segments = KToonPathParser.parse(path)
        if (containsWildcard(segments)) {
            throw KToonQueryException("Wildcard is not allowed in get path '$path'. Use select instead.")
        }

        return get(root, segments)
    }

    private fun get(root: ToonValue, segments: List<PathSegment>): ToonValue? {
        var current: ToonValue = root
        for (segment in segments) {
            current = when (segment) {
                is PropertySegment -> property(current, segment.name) ?: return null
                is IndexSegment -> index(current, segment.index) ?: return null
                WildcardSegment -> error("Wildcard paths are rejected before traversal")
            }
        }

        return current
    }

    /**
     * Returns all values matched by [path].
     *
     * Wildcards expand array items in-place. For example,
     * `users[*].name` selects the `name` value from every object in `users`
     * that contains such a property. Missing branches are skipped.
     *
     * @param root root value to navigate
     * @param path property/index/wildcard path
     * @return all matched values, or an empty list when no branch matches
     * @throws KToonQueryException when [path] is invalid
     */
    fun select(root: ToonValue, path: String): List<ToonValue> {
        val segments = KToonPathParser.parse(path)
        return select(root, segments)
    }

    private fun select(root: ToonValue, segments: List<PathSegment>): List<ToonValue> {
        var current = listOf(root)

        for (segment in segments) {
            if (current.isEmpty()) {
                return emptyList()
            }

            val next = ArrayList<ToonValue>(current.size)
            for (value in current) {
                when (segment) {
                    is PropertySegment -> property(value, segment.name)?.let(next::add)
                    is IndexSegment -> index(value, segment.index)?.let(next::add)
                    WildcardSegment -> {
                        if (value is ToonArray) {
                            next.addAll(value.values)
                        }
                    }
                }
            }
            current = next
        }

        return current
    }

    /**
     * Returns whether [path] resolves to at least one value.
     *
     * Wildcard paths are evaluated with [select]. Non-wildcard paths are
     * evaluated with [get].
     *
     * @param root root value to navigate
     * @param path property/index/wildcard path
     * @return true when at least one value matches
     * @throws KToonQueryException when [path] is invalid
     */
    fun exists(root: ToonValue, path: String): Boolean {
        val segments = KToonPathParser.parse(path)
        return if (containsWildcard(segments)) {
            select(root, segments).isNotEmpty()
        } else {
            get(root, segments) != null
        }
    }

    private fun containsWildcard(segments: List<PathSegment>): Boolean {
        for (segment in segments) {
            if (segment is WildcardSegment) {
                return true
            }
        }
        return false
    }

    private fun property(value: ToonValue, name: String): ToonValue? {
        return (value as? ToonObject)?.entries?.get(name)
    }

    private fun index(value: ToonValue, index: Int): ToonValue? {
        val array = value as? ToonArray ?: return null
        return array.values.getOrNull(index)
    }
}

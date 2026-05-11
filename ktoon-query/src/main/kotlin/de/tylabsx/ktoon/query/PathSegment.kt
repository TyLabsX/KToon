package de.tylabsx.ktoon.query

/**
 * A single step in a parsed KToon query path.
 *
 * Supported public path syntax is intentionally small:
 *
 * - `user`
 * - `user.name`
 * - `users[0]`
 * - `users[0].name`
 * - `users[*]`
 * - `users[*].name`
 *
 * Wildcards are accepted only by selection APIs. Single-value `get` APIs reject
 * paths containing [WildcardSegment].
 *
 * @since 1.2.0
 */
internal sealed interface PathSegment

/**
 * Selects a property from a [de.tylabsx.ktoon.ToonObject].
 *
 * @property name object property name
 * @since 1.2.0
 */
internal data class PropertySegment(val name: String) : PathSegment

/**
 * Selects an item from a [de.tylabsx.ktoon.ToonArray] by zero-based index.
 *
 * @property index zero-based array index
 * @since 1.2.0
 */
internal data class IndexSegment(val index: Int) : PathSegment

/**
 * Selects all items from a [de.tylabsx.ktoon.ToonArray].
 *
 * Wildcards are valid in [KToonQuery.select] and extension `select` calls.
 * [KToonQuery.get] rejects wildcard paths to keep single-value navigation
 * deterministic.
 *
 * @since 1.2.0
 */
internal data object WildcardSegment : PathSegment

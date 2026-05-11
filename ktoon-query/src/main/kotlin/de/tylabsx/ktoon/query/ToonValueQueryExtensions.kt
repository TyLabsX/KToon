package de.tylabsx.ktoon.query

import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonBoolean
import de.tylabsx.ktoon.ToonNull
import de.tylabsx.ktoon.ToonNumber
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonString
import de.tylabsx.ktoon.ToonValue

/**
 * Returns the value at [path], or `null` when the path cannot be resolved.
 *
 * Wildcards are rejected by this single-value API. Use [select] for wildcard
 * paths such as `users[*].name`.
 *
 * @throws KToonQueryException when [path] is invalid or contains a wildcard
 * @since 1.2.0
 */
fun ToonValue.get(path: String): ToonValue? = KToonQuery.get(this, path)

/**
 * Returns all values matched by [path].
 *
 * Wildcards expand array items and missing branches are skipped.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.select(path: String): List<ToonValue> = KToonQuery.select(this, path)

/**
 * Returns true when [path] resolves to at least one value.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.exists(path: String): Boolean = KToonQuery.exists(this, path)

/**
 * Returns the string value at [path].
 *
 * @throws KToonQueryException when the path is missing or the value is not a
 * [ToonString]
 * @since 1.2.0
 */
fun ToonValue.string(path: String): String {
    val value = get(path)
    return (value as? ToonString)?.value ?: throw missingOrType(path, "ToonString", value)
}

/**
 * Returns the string value at [path], or `null` if missing or not a string.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.stringOrNull(path: String): String? {
    return (get(path) as? ToonString)?.value
}

/**
 * Returns the integer value at [path].
 *
 * @throws KToonQueryException when the path is missing, not a number, or cannot
 * be parsed as [Int]
 * @since 1.2.0
 */
fun ToonValue.int(path: String): Int {
    val value = get(path)
    return (value as? ToonNumber)?.raw?.toIntOrNull()
        ?: throw missingOrType(path, "ToonNumber(Int)", value)
}

/**
 * Returns the integer value at [path], or `null` if missing, not a number, or
 * not parseable as [Int].
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.intOrNull(path: String): Int? {
    return (get(path) as? ToonNumber)?.raw?.toIntOrNull()
}

/**
 * Returns the long value at [path].
 *
 * @throws KToonQueryException when the path is missing, not a number, or cannot
 * be parsed as [Long]
 * @since 1.2.0
 */
fun ToonValue.long(path: String): Long {
    val value = get(path)
    return (value as? ToonNumber)?.raw?.toLongOrNull()
        ?: throw missingOrType(path, "ToonNumber(Long)", value)
}

/**
 * Returns the long value at [path], or `null` if missing, not a number, or not
 * parseable as [Long].
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.longOrNull(path: String): Long? {
    return (get(path) as? ToonNumber)?.raw?.toLongOrNull()
}

/**
 * Returns the double value at [path].
 *
 * @throws KToonQueryException when the path is missing, not a number, or cannot
 * be parsed as [Double]
 * @since 1.2.0
 */
fun ToonValue.double(path: String): Double {
    val value = get(path)
    return (value as? ToonNumber)?.raw?.toDoubleOrNull()
        ?: throw missingOrType(path, "ToonNumber(Double)", value)
}

/**
 * Returns the double value at [path], or `null` if missing, not a number, or not
 * parseable as [Double].
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.doubleOrNull(path: String): Double? {
    return (get(path) as? ToonNumber)?.raw?.toDoubleOrNull()
}

/**
 * Returns the boolean value at [path].
 *
 * @throws KToonQueryException when the path is missing or the value is not a
 * [ToonBoolean]
 * @since 1.2.0
 */
fun ToonValue.boolean(path: String): Boolean {
    val value = get(path)
    return (value as? ToonBoolean)?.value ?: throw missingOrType(path, "ToonBoolean", value)
}

/**
 * Returns the boolean value at [path], or `null` if missing or not a boolean.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.booleanOrNull(path: String): Boolean? {
    return (get(path) as? ToonBoolean)?.value
}

/**
 * Returns the object value at [path].
 *
 * @throws KToonQueryException when the path is missing or the value is not a
 * [ToonObject]
 * @since 1.2.0
 */
fun ToonValue.obj(path: String): ToonObject {
    val value = get(path)
    return value as? ToonObject ?: throw missingOrType(path, "ToonObject", value)
}

/**
 * Returns the object value at [path], or `null` if missing or not an object.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.objOrNull(path: String): ToonObject? {
    return get(path) as? ToonObject
}

/**
 * Returns the array value at [path].
 *
 * @throws KToonQueryException when the path is missing or the value is not a
 * [ToonArray]
 * @since 1.2.0
 */
fun ToonValue.array(path: String): ToonArray {
    val value = get(path)
    return value as? ToonArray ?: throw missingOrType(path, "ToonArray", value)
}

/**
 * Returns the array value at [path], or `null` if missing or not an array.
 *
 * @throws KToonQueryException when [path] is invalid
 * @since 1.2.0
 */
fun ToonValue.arrayOrNull(path: String): ToonArray? {
    return get(path) as? ToonArray
}

private fun missingOrType(path: String, expected: String, value: ToonValue?): KToonQueryException {
    return if (value == null) {
        KToonQueryException("Path '$path' does not exist")
    } else {
        KToonQueryException("Expected $expected at path '$path' but found ${value.typeName()}")
    }
}

private fun ToonValue.typeName(): String {
    return when (this) {
        is ToonArray -> "ToonArray"
        is ToonBoolean -> "ToonBoolean"
        ToonNull -> "ToonNull"
        is ToonNumber -> "ToonNumber"
        is ToonObject -> "ToonObject"
        is ToonString -> "ToonString"
    }
}

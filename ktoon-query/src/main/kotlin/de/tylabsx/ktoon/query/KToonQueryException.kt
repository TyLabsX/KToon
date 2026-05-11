package de.tylabsx.ktoon.query

/**
 * Signals an invalid query path, a missing value, or an unexpected value type
 * during KToon path navigation.
 *
 * Nullable accessor functions return `null` instead of throwing for missing
 * paths or type mismatches. Non-null accessor functions throw this exception
 * with the path and expected type in the message.
 *
 * @since 1.2.0
 */
class KToonQueryException(message: String) : RuntimeException(message)

package de.tylabsx.ktoon

/**
 * Exception thrown when Kotlin object encoding or decoding fails.
 *
 * This exception is used by the reflection based KToon codec layer.
 *
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonCodecException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
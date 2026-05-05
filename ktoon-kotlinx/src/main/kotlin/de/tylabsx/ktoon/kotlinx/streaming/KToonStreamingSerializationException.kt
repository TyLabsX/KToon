package de.tylabsx.ktoon.kotlinx.streaming

/**
 * Signals an invalid or unsupported streaming serialization operation.
 *
 * The streaming encoder is intentionally strict. It rejects structured map
 * keys, unsupported polymorphic serialization, and invalid encoder state
 * instead of applying implicit corrections that could change the serialized
 * data.
 *
 * @since 1.1.0
 * @author TyLabsX
 */
class KToonStreamingSerializationException : RuntimeException {

    /**
     * Creates an exception with a message describing the encoding failure.
     *
     * @param message failure message
     */
    constructor(message: String) : super(message)

    /**
     * Creates an exception with a message and cause.
     *
     * @param message failure message
     * @param cause underlying failure
     */
    constructor(message: String, cause: Throwable) : super(message, cause)
}

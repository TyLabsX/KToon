package de.tylabsx.ktoon.kotlinx.streaming

/**
 * Marks the streaming TOON serialization API as experimental.
 *
 * The API is available for direct use, but its performance characteristics and
 * supported streaming feature set may still change while decode support and
 * additional kotlinx.serialization constructs are added.
 *
 * @since 1.1.0
 * @author TyLabsX
 */
@RequiresOptIn(
    message = "KToon streaming serialization is experimental and not performance-stable yet."
)
annotation class ExperimentalKToonStreamingApi

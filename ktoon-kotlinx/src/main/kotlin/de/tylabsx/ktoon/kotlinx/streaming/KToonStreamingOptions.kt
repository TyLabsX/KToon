package de.tylabsx.ktoon.kotlinx.streaming

/**
 * Controls how [KToonStreamingFormat] renders TOON output.
 *
 * The options are limited to formatting and streaming strategy choices. They do
 * not enable implicit type coercion, lossy conversion, or parser-side recovery.
 *
 * @property indentSize number of spaces per indentation level
 * @property quoteStrings when true, all strings are quoted even when TOON
 * syntax would allow an unquoted string
 * @property trailingNewline when true, encoded output ends with a newline
 * @property mode speed-oriented or size-oriented streaming strategy
 * @since 1.1.0
 * @author TyLabsX
 */
data class KToonStreamingOptions(
    val indentSize: Int = 2,
    val quoteStrings: Boolean = false,
    val trailingNewline: Boolean = false,
    val mode: KToonStreamingMode = KToonStreamingMode.COMPACT
) {
    init {
        if (indentSize < 0) {
            throw KToonStreamingSerializationException("indentSize must be greater than or equal to zero")
        }
    }
}

/**
 * Selects the streaming encoder strategy.
 *
 * [FAST] favors low overhead. Primitive lists are rendered inline, while object
 * lists are rendered as block arrays without tabular analysis.
 *
 * [COMPACT] favors size and token efficiency. Primitive lists are rendered
 * inline, and lists of objects are rendered as tabular arrays when every item
 * has the same keys in the same order and every row value is primitive. Lists
 * that do not satisfy those constraints fall back to block arrays.
 *
 * @since 1.1.0
 * @author TyLabsX
 */
enum class KToonStreamingMode {
    /**
     * Speed-oriented strategy.
     *
     * FAST minimizes buffering and analysis. Primitive lists are rendered
     * inline, and object lists use block syntax unless a specialized fast path
     * can write a known flat shape without extra row analysis.
     */
    FAST,

    /**
     * Size-oriented strategy.
     *
     * COMPACT buffers list items enough to detect tabular object arrays and
     * writes the tabular form when it is lossless.
     */
    COMPACT
}

/**
 * Legacy array rendering mode retained for source compatibility.
 *
 * New code should use [KToonStreamingMode]. [BLOCK] maps to
 * [KToonStreamingMode.FAST], and [INLINE_PRIMITIVES] maps to
 * [KToonStreamingMode.COMPACT].
 *
 * @since 1.1.0
 */
@Deprecated(
    message = "Use KToonStreamingMode through KToonStreamingOptions.mode instead.",
    replaceWith = ReplaceWith("KToonStreamingMode")
)
enum class KToonStreamingArrayMode {
    /**
     * Legacy spelling for block-style list rendering.
     */
    BLOCK,

    /**
     * Legacy spelling for inline primitive list rendering.
     */
    INLINE_PRIMITIVES
}

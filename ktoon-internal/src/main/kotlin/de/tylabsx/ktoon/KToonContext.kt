package de.tylabsx.ktoon

/**
 * Global configuration context for KToon parsing and processing operations.
 * 
 * This class serves as the central configuration hub for all KToon operations,
 * providing a unified way to manage parsing behavior, formatting preferences,
 * and debug settings across the entire library.
 * 
 * The context follows the singleton pattern to ensure consistent configuration
 * throughout the application lifecycle. All engines and components should
 * retrieve their configuration from this central context.
 * 
 * Example usage:
 * ```kotlin
 * // Configure global settings
 * KToonContext.configure {
 *     indentationSize = 4
 *     enableDebugLogging = true
 *     strictMode = true
 * }
 * 
 * // Use configuration in engines
 * val indent = KToonContext.current.indentationSize
 * ```
 * 
 * @property indentationSize Number of spaces for each indentation level (default: 2)
 * @property strictMode Whether to enforce strict TOON syntax rules (default: false)
 * @property enableDebugLogging Whether to enable debug logging (default: false)
 * @property allowTrailingWhitespace Whether to allow trailing whitespace (default: false)
 * @property maxNestingDepth Maximum allowed nesting depth (default: 100)
 * @property preserveComments Whether to preserve comments during parsing (default: false)
 * @property enableKeyFolding Whether to enable key folding syntax (default: true)
 * @property enableInlineArrays Whether to enable inline array syntax (default: true)
 * @property enableTabularArrays Whether to enable tabular array syntax (default: true)
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonContext private constructor(
    /**
     * The number of spaces used for each indentation level.
     * 
     * TOON standard uses 2 spaces, but this can be configured for different
     * formatting preferences. All parsing and writing operations will use this
     * value to determine indentation levels.
     * 
     * Default: 2
     * Range: 1-8
     */
    var indentationSize: Int = 2,

    /**
     * Whether to enforce strict TOON syntax rules.
     * 
     * In strict mode, the parser will reject any syntax that doesn't conform
     * exactly to the TOON specification. In non-strict mode, some common
     * variations may be accepted.
     * 
     * Default: false
     */
    var strictMode: Boolean = false,

    /**
     * Whether to enable debug logging throughout the parsing pipeline.
     * 
     * When enabled, detailed information about parsing steps, tokenization,
     * and processing will be logged. Useful for debugging complex TOON structures.
     * 
     * Default: false
     */
    var enableDebugLogging: Boolean = false,

    /**
     * Whether to allow trailing whitespace in TOON input.
     * 
     * When false, trailing whitespace will cause parsing errors.
     * When true, trailing whitespace is ignored during parsing.
     * 
     * Default: false
     */
    var allowTrailingWhitespace: Boolean = false,

    /**
     * Maximum allowed nesting depth for TOON structures.
     * 
     * This prevents stack overflow attacks and extremely deep nesting
     * that could cause performance issues. Set to 0 for unlimited depth.
     * 
     * Default: 100
     */
    var maxNestingDepth: Int = 100,

    /**
     * Whether to preserve comments during parsing.
     * 
     * When true, comments are included in the parsed structure and can be
     * retrieved during serialization. When false, comments are completely ignored.
     * 
     * Default: false
     */
    var preserveComments: Boolean = false,

    /**
     * Whether to enable key folding syntax support.
     * 
     * Key folding allows dot notation like "user.id: 123" to be expanded
     * into nested objects. Disabling this will treat dots as regular key characters.
     * 
     * Default: true
     */
    var enableKeyFolding: Boolean = true,

    /**
     * Whether to enable inline array syntax support.
     * 
     * Inline arrays allow comma-separated values like "roles: admin, user".
     * Disabling this will require all arrays to use tabular syntax.
     * 
     * Default: true
     */
    var enableInlineArrays: Boolean = true,

    /**
     * Whether to enable tabular array syntax support.
     * 
     * Tabular arrays allow header-based CSV-like syntax for structured data.
     * Disabling this will require all arrays to use inline syntax.
     * 
     * Default: true
     */
    var enableTabularArrays: Boolean = true
) {

    /**
     * Validates the current configuration settings.
     * 
     * This method checks that all configuration values are within acceptable
     * ranges and don't conflict with each other. Throws IllegalArgumentException
     * if validation fails.
     * 
     * @throws IllegalArgumentException if any configuration is invalid
     */
    fun validate() {
        require(indentationSize in 1..8) {
            "indentationSize must be between 1 and 8, but was $indentationSize"
        }
        require(maxNestingDepth >= 0) {
            "maxNestingDepth must be non-negative, but was $maxNestingDepth"
        }

        if (!enableInlineArrays && !enableTabularArrays) {
            throw IllegalArgumentException(
                "At least one of enableInlineArrays or enableTabularArrays must be true"
            )
        }
    }

    /**
     * Creates a copy of this context with modified values.
     * 
     * This method is useful for creating temporary context variations
     * for specific operations without modifying the global context.
     * 
     * @param block Configuration block to modify the copy
     * @return New KToonContext instance with modified values
     */
    fun copy(block: KToonContext.() -> Unit): KToonContext {
        val copy = KToonContext(
            indentationSize = this.indentationSize,
            strictMode = this.strictMode,
            enableDebugLogging = this.enableDebugLogging,
            allowTrailingWhitespace = this.allowTrailingWhitespace,
            maxNestingDepth = this.maxNestingDepth,
            preserveComments = this.preserveComments,
            enableKeyFolding = this.enableKeyFolding,
            enableInlineArrays = this.enableInlineArrays,
            enableTabularArrays = this.enableTabularArrays
        )
        copy.block()
        copy.validate()
        return copy
    }

    /**
     * Returns a summary of the current configuration.
     * 
     * This method provides a human-readable summary of all configuration
     * settings, useful for debugging and logging purposes.
     * 
     * @return Configuration summary string
     */
    fun getConfigurationSummary(): String {
        return buildString {
            appendLine("KToonContext Configuration:")
            appendLine("  indentationSize: $indentationSize")
            appendLine("  strictMode: $strictMode")
            appendLine("  enableDebugLogging: $enableDebugLogging")
            appendLine("  allowTrailingWhitespace: $allowTrailingWhitespace")
            appendLine("  maxNestingDepth: $maxNestingDepth")
            appendLine("  preserveComments: $preserveComments")
            appendLine("  enableKeyFolding: $enableKeyFolding")
            appendLine("  enableInlineArrays: $enableInlineArrays")
            appendLine("  enableTabularArrays: $enableTabularArrays")
        }
    }

    companion object {

        /**
         * The singleton instance of KToonContext.
         * 
         * This is the global context that all components should use for
         * configuration. It can be modified through the configure() method.
         */
        @Volatile
        private var _instance: KToonContext = KToonContext()

        /**
         * Gets the current global KToonContext instance.
         * 
         * @return Current KToonContext instance
         */
        val current: KToonContext
            get() = _instance

        /**
         * Configures the global KToonContext instance.
         * 
         * This method allows modification of the global configuration.
         * All changes are applied atomically and validated before taking effect.
         * 
         * Example:
         * ```kotlin
         * KToonContext.configure {
         *     indentationSize = 4
         *     strictMode = true
         * }
         * ```
         * 
         * @param block Configuration block to modify the context
         * @throws IllegalArgumentException if any configuration is invalid
         */
        fun configure(block: KToonContext.() -> Unit) {
            synchronized(this) {
                _instance.block()
                _instance.validate()
            }
        }

        /**
         * Resets the global context to default values.
         * 
         * This method restores all configuration settings to their
         * default values as defined in the primary constructor.
         */
        fun resetToDefaults() {
            synchronized(this) {
                _instance = KToonContext()
            }
        }

        /**
         * Creates a new context instance with default values.
         * 
         * This factory method creates a fresh context that is not
         * connected to the global singleton. Useful for testing or
         * isolated operations.
         * 
         * @return New KToonContext with default configuration
         */
        fun createDefault(): KToonContext = KToonContext()

        /**
         * Creates a new context instance with custom configuration.
         * 
         * This factory method creates a fresh context with the provided
         * configuration. The context is not connected to the global singleton.
         * 
         * @param block Configuration block for the new context
         * @return New KToonContext with custom configuration
         * @throws IllegalArgumentException if any configuration is invalid
         */
        fun createCustom(block: KToonContext.() -> Unit): KToonContext {
            val context = KToonContext()
            context.block()
            context.validate()
            return context
        }
    }
}

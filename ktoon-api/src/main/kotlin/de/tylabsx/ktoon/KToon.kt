package de.tylabsx.ktoon

/**
 * Main public API for the KToon library.
 * 
 * This object provides the primary entry point for all KToon operations,
 * offering a simple and intuitive interface for parsing and stringifying
 * TOON data. It delegates to the comprehensive KToonEngine
 * while maintaining a clean, minimal public API.
 * 
 * The API supports:
 * - Simple parsing of TOON strings to ToonValue
 * - Simple serialization of ToonValue to TOON strings
 * - Configuration management through KToon.configure()
 * - Error handling with detailed exceptions
 * - Performance optimization and caching
 * 
 * Example usage:
 * ```kotlin
 * // Parse TOON data
 * val toonData = """
 *     user:
 *       id: 123
 *       name: "Alice"
 *       active: true
 * """.trimIndent()
 * 
 * val value = KToon.parse(toonData)
 * 
 * // Serialize back to TOON
 * val output = KToon.stringify(value)
 * 
 * // Configure global settings
 * KToon.configure {
 *     indentationSize = 4
 *     strictMode = true
 * }
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
object KToon {

    /**
     * Internal engine instance for all operations.
     * This is lazily initialized to avoid unnecessary instantiation
     * when the API is not used.
     */
    private val engine by lazy { KToonEngine() }

    private val encoder by lazy { KToonEncoder() }

    /**
     * Parses TOON input into a ToonValue structure.
     * 
     * This method provides the main entry point for parsing TOON data.
     * It accepts any valid TOON string and converts it into the
     * internal ToonValue representation with full type safety.
     * 
     * The parsing process follows these phases:
     * 1. Input normalization (line endings, whitespace)
     * 2. Line splitting with metadata extraction
     * 3. Indentation analysis and hierarchy building
     * 4. Syntax classification and component extraction
     * 5. Key-value pair processing and extraction
     * 6. Structure building with proper relationships
     * 7. Value resolution into appropriate ToonValue types
     * 
     * Supported TOON features:
     * - Nested objects through indentation
     * - Key folding (user.id: 123)
     * - Inline arrays (roles: admin, user)
     * - Tabular arrays (users{id,name}: 1,Alice)
     * - Comments (# This is a comment)
     * - Quoted strings with escape sequences
     * - Boolean values (true/false)
     * - Null values (null)
     * - Numbers (preserved as raw strings)
     * 
     * @param input The raw TOON string to parse
     * @return Parsed ToonValue structure
     * @throws ToonParseException if parsing fails
     * 
     * Example:
     * ```kotlin
     * val input = """
     *     user:
     *       id: 123
     *       name: "Alice"
     *       active: true
     *       roles: admin, user
     * """.trimIndent()
     * 
     * val result = KToon.parse(input)
     * // result is ToonObject with entries for id, name, active, and roles
     * ```
     */
    fun parse(input: String): ToonValue {
        return engine.parse(input).toonValue
    }

    /**
     * Serializes a ToonValue structure to TOON format string.
     * 
     * This method provides the main entry point for converting ToonValue
     * structures back to TOON format strings. It handles all ToonValue
     * types and produces valid TOON output with proper formatting.
     * 
     * The serialization process:
     * 1. Validates the ToonValue structure
     * 2. Applies current configuration settings
     * 3. Formats output with proper indentation
     * 4. Handles special characters and escaping
     * 5. Optimizes output for readability
     * 
     * Output formatting follows current KToonContext settings:
     * - Indentation size (default: 2 spaces)
     * - Strict mode compliance
     * - Comment preservation (if enabled)
     * - Trailing whitespace handling
     * 
     * @param value The ToonValue structure to serialize
     * @return TOON format string
     * @throws ToonParseException if serialization fails
     * 
     * Example:
     * ```kotlin
     * val value = ToonObject(mapOf(
     *     "user" to ToonObject(mapOf(
     *         "id" to ToonNumber("123"),
     *         "name" to ToonString("Alice"),
     *         "active" to ToonBoolean(true),
     *         "roles" to ToonArray(listOf(
     *             ToonString("admin"),
     *             ToonString("user")
     *         ))
     *     ))
     * ))
     * 
     * val output = KToon.stringify(value)
     * // output is properly formatted TOON string
     * ```
     */
    fun stringify(value: ToonValue): String {
        return engine.stringify(value)
    }

    /**
     * Validates TOON input without fully parsing it.
     * 
     * This method provides a lightweight way to check if TOON input
     * is syntactically valid without the overhead of full parsing.
     * Useful for input validation, form validation, and quick checks.
     * 
     * @param input The TOON string to validate
     * @return true if input is valid, false otherwise
     * 
     * Example:
     * ```kotlin
     * val validInput = "user: Alice"
     * val invalidInput = "user Alice" // Missing colon
     * 
     * println(KToon.isValid(validInput))   // true
     * println(KToon.isValid(invalidInput)) // false
     * ```
     */
    fun isValid(input: String): Boolean {
        return try {
            engine.validate(input).isValid
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Configures global KToon settings.
     * 
     * This method provides access to the global configuration system,
     * allowing customization of parsing and serialization behavior.
     * Changes affect all subsequent operations.
     * 
     * Available configuration options:
     * - indentationSize: Number of spaces per indentation level (1-8)
     * - strictMode: Whether to enforce strict TOON syntax rules
     * - enableDebugLogging: Whether to enable debug output
     * - allowTrailingWhitespace: Whether to allow trailing whitespace
     * - maxNestingDepth: Maximum allowed nesting depth (0 = unlimited)
     * - preserveComments: Whether to preserve comments during parsing
     * - enableKeyFolding: Whether to enable key folding syntax
     * - enableInlineArrays: Whether to enable inline array syntax
     * - enableTabularArrays: Whether to enable tabular array syntax
     * 
     * @param configuration Configuration block to apply
     * 
     * Example:
     * ```kotlin
     * KToon.configure {
     *     indentationSize = 4
     *     strictMode = true
     *     enableDebugLogging = false
     *     maxNestingDepth = 50
     * }
     * ```
     */
    fun configure(configuration: KToonContext.() -> Unit) {
        engine.configure(configuration)
    }

    /**
     * Resets global KToon configuration to default values.
     * 
     * This method restores all configuration settings to their
     * default values as defined in KToonContext.
     * Useful for testing or when you want to start fresh.
     * 
     * Example:
     * ```kotlin
     * KToon.resetConfiguration()
     * ```
     */
    fun resetConfiguration() {
        engine.resetConfiguration()
    }

    /**
     * Gets current global KToon configuration.
     * 
     * This method returns the current configuration settings,
     * useful for debugging or displaying current state.
     * 
     * @return Current configuration summary string
     * 
     * Example:
     * ```kotlin
     * println(KToon.getConfiguration())
     * // Displays all current settings
     * ```
     */
    fun getConfiguration(): String {
        return engine.getEngineStatistics().contextConfiguration
    }

    /**
     * Gets comprehensive statistics about KToon library.
     * 
     * This method returns detailed information about all KToon
     * subsystems, their capabilities, and current configuration.
     * Useful for debugging, feature discovery, and library information.
     * 
     * @return Comprehensive engine statistics
     * 
     * Example:
     * ```kotlin
     * val stats = KToon.getStatistics()
     * println(stats.getComprehensiveSummary())
     * ```
     */
    fun getStatistics(): EngineStatistics {
        return engine.getEngineStatistics()
    }

    /**
     * Performs a complete roundtrip: parse -> stringify.
     * 
     * This method provides a convenient way to test roundtrip
     * compatibility, ensuring that parsing and serialization
     * produce consistent results.
     * 
     * @param input The TOON string to roundtrip
     * @return Roundtripped TOON string
     * @throws ToonParseException if roundtrip fails
     * 
     * Example:
     * ```kotlin
     * val input = "user: Alice"
     * val output = KToon.roundtrip(input)
     * // output should be equivalent to input (modulo formatting)
     * ```
     */
    fun roundtrip(input: String): String {
        return engine.roundtrip(input).finalOutput
    }

    /**
     * Creates a new ToonObject with the provided entries.
     * 
     * This is a convenience factory method for creating ToonObject
     * instances with proper type inference and validation.
     * 
     * @param entries Map of string keys to ToonValue values
     * @return New ToonObject instance
     * 
     * Example:
     * ```kotlin
     * val obj = KToon.object(mapOf(
     *     "name" to "Alice",
     *     "age" to 25,
     *     "active" to true
     * ))
     * ```
     */
    fun `object`(entries: Map<String, Any>): ToonObject {
        val convertedEntries = entries.mapValues { (_, value) ->
            when (value) {
                is ToonValue -> value
                is String -> ToonString(value)
                is Int, is Long, is Float, is Double -> ToonNumber(value.toString())
                is Boolean -> ToonBoolean(value)
                null -> ToonNull
                else -> ToonString(value.toString())
            }
        }
        return ToonObject(convertedEntries)
    }

    /**
     * Creates a new ToonArray with the provided values.
     * 
     * This is a convenience factory method for creating ToonArray
     * instances with proper type inference and validation.
     * 
     * @param values List of values to include in array
     * @return New ToonArray instance
     * 
     * Example:
     * ```kotlin
     * val arr = KToon.array(listOf("admin", "user", "guest"))
     * ```
     */
    fun array(values: List<Any>): ToonArray {
        val convertedValues = values.map { value ->
            when (value) {
                is ToonValue -> value
                is String -> ToonString(value)
                is Int, is Long, is Float, is Double -> ToonNumber(value.toString())
                is Boolean -> ToonBoolean(value)
                null -> ToonNull
                else -> ToonString(value.toString())
            }
        }
        return ToonArray(convertedValues)
    }

    /**
     * Creates a new ToonString with the provided value.
     * 
     * This is a convenience factory method for creating ToonString
     * instances with null safety and validation.
     * 
     * @param value The string value
     * @return New ToonString instance
     * 
     * Example:
     * ```kotlin
     * val str = KToon.string("Hello, World!")
     * ```
     */
    fun string(value: String?): ToonString {
        return ToonString(value ?: "")
    }

    /**
     * Creates a new ToonNumber with the provided value.
     * 
     * This is a convenience factory method for creating ToonNumber
     * instances with proper string conversion.
     * 
     * @param value The numeric value
     * @return New ToonNumber instance
     * 
     * Example:
     * ```kotlin
     * val num = KToon.number(123.45)
     * ```
     */
    fun number(value: Number): ToonNumber {
        return ToonNumber(value.toString())
    }

    /**
     * Creates a new ToonBoolean with the provided value.
     * 
     * This is a convenience factory method for creating ToonBoolean
     * instances.
     * 
     * @param value The boolean value
     * @return New ToonBoolean instance
     * 
     * Example:
     * ```kotlin
     * val bool = KToon.boolean(true)
     * ```
     */
    fun boolean(value: Boolean): ToonBoolean {
        return ToonBoolean(value)
    }

    /**
     * Creates a new ToonNull instance.
     * 
     * This is a convenience factory method for creating ToonNull
     * instances.
     * 
     * @return ToonNull instance
     * 
     * Example:
     * ```kotlin
     * val nullValue = KToon.`null`()
     * ```
     */
    fun `null`(): ToonNull {
        return ToonNull
    }

    /**
     * Encodes a Kotlin value into a ToonValue tree.
     *
     * @param value Kotlin value
     * @return encoded ToonValue
     */
    fun encode(value: Any?): ToonValue {
        return encoder.encode(value)
    }

    /**
     * Encodes a Kotlin value directly into a TOON string.
     *
     * @param value Kotlin value
     * @return TOON string
     */
    fun encodeToString(value: Any?): String {
        return stringify(encode(value))
    }

    /**
     * Decodes a ToonValue into a Kotlin object of type T.
     *
     * @param value ToonValue source
     * @return decoded Kotlin object
     */
    inline fun <reified T : Any> decode(value: ToonValue): T {
        return KToonDecoder().decode<T>(value)
    }

    /**
     * Parses a TOON string and decodes it into a Kotlin object of type T.
     *
     * @param input TOON input
     * @return decoded Kotlin object
     */
    inline fun <reified T : Any> decodeFromString(input: String): T {
        return KToonDecoder().decode<T>(parse(input))
    }
}

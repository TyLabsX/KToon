package de.tylabsx.ktoon

/**
 * Sealed interface representing all possible TOON data values.
 * 
 * This interface serves as the root of the TOON data model hierarchy.
 * All TOON data types implement this interface, enabling type-safe
 * pattern matching and comprehensive data handling.
 * 
 * The TOON data model supports:
 * - Objects (key-value mappings)
 * - Arrays (ordered collections)
 * - Strings (textual data)
 * - Numbers (numeric values, preserved as raw strings)
 * - Booleans (true/false values)
 * - Null (absence of value)
 * 
 * Example usage:
 * ```kotlin
 * fun processValue(value: ToonValue) {
 *     when (value) {
 *         is ToonObject -> println("Object with ${value.entries.size} entries")
 *         is ToonArray -> println("Array with ${value.values.size} items")
 *         is ToonString -> println("String: ${value.value}")
 *         is ToonNumber -> println("Number: ${value.raw}")
 *         is ToonBoolean -> println("Boolean: ${value.value}")
 *         is ToonNull -> println("Null value")
 *     }
 * }
 * ```
 * 
 * @see ToonObject
 * @see ToonArray
 * @see ToonString
 * @see ToonNumber
 * @see ToonBoolean
 * @see ToonNull
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
sealed interface ToonValue {

    /**
     * Returns a string representation of this TOON value.
     * 
     * This method provides a human-readable representation suitable
     * for debugging and logging purposes. For TOON serialization,
     * use the dedicated writer engines instead.
     * 
     * @return String representation of this value
     */
    override fun toString(): String
}

/**
 * Represents a TOON object containing key-value mappings.
 * 
 * Objects are the fundamental container type in TOON, similar to
 * JSON objects. They consist of string keys mapped to TOON values.
 * Objects are defined through indentation in TOON syntax.
 * 
 * Example in TOON format:
 * ```
 * user:
 *   id: 123
 *   name: "Alice"
 *   active: true
 * ```
 * 
 * This would create a ToonObject with entries:
 * - "id" -> ToonNumber("123")
 * - "name" -> ToonString("Alice")  
 * - "active" -> ToonBoolean(true)
 * 
 * @property entries Immutable map of string keys to TOON values
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonObject(
    /**
     * The key-value mappings contained in this object.
     * 
     * The map is immutable to ensure data integrity and thread safety.
     * Keys are always strings, while values can be any ToonValue type.
     * 
     * Example:
     * ```kotlin
     * val obj = ToonObject(mapOf(
     *     "name" to ToonString("Bob"),
     *     "age" to ToonNumber("25")
     * ))
     * ```
     */
    val entries: Map<String, ToonValue>
) : ToonValue {

    /**
     * Returns a string representation of this object.
     * 
     * Format: ToonObject(entries={key1=value1, key2=value2, ...})
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonObject(entries=$entries)"
}

/**
 * Represents a TOON array containing an ordered collection of values.
 * 
 * Arrays can be created in two ways in TOON:
 * 1. Inline arrays: `roles: admin, user, guest`
 * 2. Tabular arrays: `users{id,name}:\n 1,Alice\n 2,Bob`
 * 
 * Arrays maintain the order of elements and can contain mixed types.
 * 
 * Example inline array in TOON:
 * ```
 * tags: kotlin, library, parser
 * ```
 * 
 * This would create a ToonArray with ToonString values.
 * 
 * @property values Immutable list of TOON values in order
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonArray(
    /**
     * The ordered collection of values in this array.
     * 
     * The list is immutable to ensure data integrity and thread safety.
     * Elements can be of any ToonValue type, allowing mixed-type arrays.
     * 
     * Example:
     * ```kotlin
     * val arr = ToonArray(listOf(
     *     ToonString("hello"),
     *     ToonNumber("42"),
     *     ToonBoolean(true)
     * ))
     * ```
     */
    val values: List<ToonValue>
) : ToonValue {

    /**
     * Returns a string representation of this array.
     * 
     * Format: ToonArray(values=[value1, value2, ...])
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonArray(values=$values)"
}

/**
 * Represents a string value in TOON.
 * 
 * Strings are used for textual data and can contain any Unicode characters.
 * In TOON syntax, strings with special characters must be quoted.
 * 
 * Examples in TOON:
 * ```
 * name: Alice
 * message: "Hello, world!"
 * path: "C:\\Users\\Admin"
 * ```
 * 
 * @property value The actual string content
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonString(
    /**
     * The string content value.
     * 
     * This contains the actual text without surrounding quotes.
     * Empty strings are allowed and represented as ToonString("").
     * 
     * Example:
     * ```kotlin
     * val str = ToonString("Hello World")
     * println(str.value) // Output: Hello World
     * ```
     */
    val value: String
) : ToonValue {

    /**
     * Returns a string representation of this string value.
     * 
     * Format: ToonString(value="content")
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonString(value=\"$value\")"
}

/**
 * Represents a numeric value in TOON.
 * 
 * Numbers are preserved as raw strings to maintain exact precision
 * and avoid floating-point conversion issues. This allows the library
 * to handle very large numbers and maintain the original formatting.
 * 
 * Examples in TOON:
 * ```
 * count: 42
 * price: 19.99
 * big: 12345678901234567890
 * scientific: 1.23e-4
 * ```
 * 
 * @property raw The raw string representation of the number
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonNumber(
    /**
     * The raw string representation of the numeric value.
     * 
     * This preserves the exact input format, including leading zeros,
     * decimal points, scientific notation, and large numbers that might
     * overflow standard numeric types.
     * 
     * Example:
     * ```kotlin
     * val num = ToonNumber("00123.45")
     * println(num.raw) // Output: 00123.45
     * ```
     */
    val raw: String
) : ToonValue {

    /**
     * Returns a string representation of this number value.
     * 
     * Format: ToonNumber(raw="123.45")
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonNumber(raw=\"$raw\")"
}

/**
 * Represents a boolean value in TOON.
 * 
 * Booleans represent logical true/false values. In TOON syntax,
 * they are written as the literals `true` and `false` (case-sensitive).
 * 
 * Examples in TOON:
 * ```
 * active: true
 * deleted: false
 * ```
 * 
 * @property value The boolean value (true or false)
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonBoolean(
    /**
     * The boolean value.
     * 
     * Can be either true or false. This provides type-safe boolean
     * handling within the TOON data model.
     * 
     * Example:
     * ```kotlin
     * val bool = ToonBoolean(true)
     * println(bool.value) // Output: true
     * ```
     */
    val value: Boolean
) : ToonValue {

    /**
     * Returns a string representation of this boolean value.
     * 
     * Format: ToonBoolean(value=true)
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonBoolean(value=$value)"
}

/**
 * Represents a null value in TOON.
 * 
 * Null represents the absence of a value. In TOON syntax,
 * it is written as the literal `null` (case-sensitive).
 * 
 * Example in TOON:
 * ```
 * middle_name: null
 * ```
 * 
 * This is implemented as an object rather than a data class since
 * there is only one null value - the absence of value itself.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data object ToonNull : ToonValue {

    /**
     * Returns a string representation of this null value.
     * 
     * Format: ToonNull
     * 
     * @return String representation for debugging
     */
    override fun toString(): String = "ToonNull"
}

package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.ToonValue

/**
 * Supporting classes for TOON processing operations.
 * 
 * This file contains data classes and enums used throughout
 * the TOON processing pipeline to provide type safety
 * and comprehensive functionality.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */

/**
 * Rules for transforming TOON data structures.
 * 
 * @property rules List of transformation rules to apply
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class TransformRules(
    val rules: List<TransformRule>
)

/**
 * Individual transformation rule.
 * 
 * @property name Name of the rule
 * @property condition Condition to match
 * @property action Action to apply when condition matches
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class TransformRule(
    val name: String,
    val condition: (ToonValue) -> Boolean,
    val action: (ToonValue) -> ToonValue
) {
    /**
     * Checks if this rule matches the given value.
     * 
     * @param value The value to check
     * @return true if rule matches, false otherwise
     */
    fun matches(value: ToonValue): Boolean = condition(value)
}

/**
 * Criteria for filtering TOON data.
 * 
 * @property conditions List of filter conditions
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class FilterCriteria(
    val conditions: List<FilterCondition>
) {
    /**
     * Checks if this criteria matches the given path and value.
     * 
     * @param path The path to check
     * @param value The value to check
     * @return true if criteria matches, false otherwise
     */
    fun matches(path: String, value: ToonValue): Boolean {
        return conditions.any { it.matches(path, value) }
    }
}

/**
 * Individual filter condition.
 * 
 * @property type Type of filter condition
 * @property path Path pattern to match
 * @property value Expected value (for value-based filters)
 * @property predicate Custom predicate function
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class FilterCondition(
    val type: FilterType,
    val path: String? = null,
    val value: ToonValue? = null,
    val predicate: ((String, ToonValue) -> Boolean)? = null
) {
    /**
     * Checks if this condition matches the given path and value.
     * 
     * @param path The path to check
     * @param value The value to check
     * @return true if condition matches, false otherwise
     */
    fun matches(path: String, value: ToonValue): Boolean {
        return when (type) {
            FilterType.PATH -> this.path?.let { path.matches(it.toRegex()) } ?: false
            FilterType.VALUE -> this.value?.let { it == value } ?: false
            FilterType.PREDICATE -> this.predicate?.invoke(path, value) ?: false
        }
    }
}

/**
 * Types of filter conditions.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
enum class FilterType {
    PATH,
    VALUE,
    PREDICATE
}

/**
 * Constraints for validation operations.
 * 
 * @property requiredPaths List of paths that must be present
 * @property forbiddenPaths List of paths that must not be present
 * @property typeConstraints Map of path to expected type
 * @property valueConstraints Map of path to value constraints
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ValidationConstraints(
    val requiredPaths: List<String> = emptyList(),
    val forbiddenPaths: List<String> = emptyList(),
    val typeConstraints: Map<String, String> = emptyMap(),
    val valueConstraints: Map<String, (ToonValue) -> Boolean> = emptyMap()
)

/**
 * Options for conversion operations.
 * 
 * @property pretty Whether to format output prettily
 * @property indentSize Number of spaces for indentation
 * @property includeMetadata Whether to include metadata
 * @property customOptions Custom conversion options
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ConversionOptions(
    val pretty: Boolean = true,
    val indentSize: Int = 2,
    val includeMetadata: Boolean = false,
    val customOptions: Map<String, Any> = emptyMap()
)

/**
 * Target formats for conversion.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
enum class TargetFormat {
    JSON,
    XML,
    YAML,
    MAP,
    CUSTOM
}

/**
 * Optimization levels for processing.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
enum class OptimizationLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    AGGRESSIVE
}

/**
 * Merge strategies for combining TOON structures.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
enum class MergeStrategy {
    SHALLOW,
    DEEP,
    OVERWRITE,
    MERGE_ARRAYS,
    MERGE_OBJECTS
}

/**
 * Options for writer operations.
 * 
 * @property pretty Whether to format output prettily
 * @property minify Whether to minimize output size
 * @property indentSize Number of spaces for indentation
 * @property preserveComments Whether to preserve comments
 * @property customFormatters Custom formatting rules
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class WriterOptions(
    val pretty: Boolean = true,
    val minify: Boolean = false,
    val indentSize: Int = 2,
    val preserveComments: Boolean = false,
    val customFormatters: Map<String, (String) -> String> = emptyMap()
)

/**
 * Options for JSON serialization.
 * 
 * @property pretty Whether to format JSON prettily
 * @property indentSize Number of spaces for indentation
 * @property escapeUnicode Whether to escape Unicode characters
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class JsonOptions(
    val pretty: Boolean = true,
    val indentSize: Int = 2,
    val escapeUnicode: Boolean = false
)

/**
 * Options for XML serialization.
 * 
 * @property pretty Whether to format XML prettily
 * @property indentSize Number of spaces for indentation
 * @property rootElementName Name for root XML element
 * @property includeAttributes Whether to include object keys as attributes
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class XmlOptions(
    val pretty: Boolean = true,
    val indentSize: Int = 2,
    val rootElementName: String = "root",
    val includeAttributes: Boolean = false
)

/**
 * Options for YAML serialization.
 * 
 * @property indentSize Number of spaces for indentation
 * @property useAnchors Whether to use YAML anchors
 * @property useTags Whether to include YAML tags
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class YamlOptions(
    val indentSize: Int = 2,
    val useAnchors: Boolean = false,
    val useTags: Boolean = false
)

/**
 * Options for streaming operations.
 * 
 * @property bufferSize Size of streaming buffer
 * @property flushAfterWrite Whether to flush after each write
 * @property includeLineNumbers Whether to include line numbers
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class StreamingOptions(
    val bufferSize: Int = 8192,
    val flushAfterWrite: Boolean = false,
    val includeLineNumbers: Boolean = false
) {
    /**
     * Estimates the size needed for streaming the given value.
     * 
     * @param value The value to estimate size for
     * @return Estimated size in bytes
     */
    fun estimateSize(value: ToonValue): Long {
        return when (value) {
            is de.tylabsx.ktoon.ToonString -> value.value.length.toLong()
            is de.tylabsx.ktoon.ToonNumber -> value.raw.length.toLong()
            is de.tylabsx.ktoon.ToonBoolean -> 5L // true/false
            is de.tylabsx.ktoon.ToonNull -> 4L // null
            is de.tylabsx.ktoon.ToonArray -> value.values.sumOf { estimateSize(it) }
            is de.tylabsx.ktoon.ToonObject -> value.entries.values.sumOf { estimateSize(it) }
        }
    }
}

/**
 * Result of a query match.
 * 
 * @property path Path where match was found
 * @property value The matched value
 * @property context Additional context information
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class QueryMatch(
    val path: String,
    val value: ToonValue,
    val context: Map<String, Any> = emptyMap()
)

/**
 * Statistics for optimization operations.
 * 
 * @property originalSize Size of original data
 * @property optimizedSize Size of optimized data
 * @property compressionRatio Percentage of size reduction
 * @property optimizationsApplied List of optimizations applied
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class OptimizationStatistics(
    val originalSize: Long,
    val optimizedSize: Long,
    val compressionRatio: Double,
    val optimizationsApplied: List<String>
)

package de.tylabsx.ktoon.processor

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.OptimizationLevel
import de.tylabsx.ktoon.pipeline.OptimizationStatistics

/**
 * Optimizes TOON data structures for performance and size.
 * 
 * This class handles optimization of ToonValue structures
 * including structure optimization, value deduplication,
 * memory usage optimization, and performance improvements.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonOptimizer {

    /**
     * Optimizes a ToonValue according to specified level.
     * 
     * @param value The ToonValue to optimize
     * @param level The optimization level to apply
     * @return Optimized ToonValue
     */
    fun optimize(value: ToonValue, level: OptimizationLevel): ToonValue {
        return when (level) {
            OptimizationLevel.NONE -> value
            OptimizationLevel.LOW -> lowOptimization(value)
            OptimizationLevel.MEDIUM -> mediumOptimization(value)
            OptimizationLevel.HIGH -> highOptimization(value)
            OptimizationLevel.AGGRESSIVE -> aggressiveOptimization(value)
        }
    }

    /**
     * Gets optimization statistics for an optimization operation.
     * 
     * @param originalValue The original value before optimization
     * @param optimizedValue The optimized value after optimization
     * @return Optimization statistics
     */
    fun getOptimizationStatistics(originalValue: ToonValue, optimizedValue: ToonValue): OptimizationStatistics {
        val originalSize = calculateSize(originalValue)
        val optimizedSize = calculateSize(optimizedValue)
        val compressionRatio = if (originalSize > 0) {
            (originalSize - optimizedSize).toDouble() / originalSize.toDouble()
        } else {
            0.0
        }

        val optimizationsApplied = mutableListOf<String>()

        // Detect what optimizations were applied
        if (originalSize != optimizedSize) {
            optimizationsApplied.add("Size reduction")
        }

        if (hasStructuralOptimizations(originalValue, optimizedValue)) {
            optimizationsApplied.add("Structural optimization")
        }

        if (hasValueDeduplication(originalValue, optimizedValue)) {
            optimizationsApplied.add("Value deduplication")
        }

        return OptimizationStatistics(
            originalSize = originalSize,
            optimizedSize = optimizedSize,
            compressionRatio = compressionRatio,
            optimizationsApplied = optimizationsApplied
        )
    }

    /**
     * Performs low-level optimization.
     * 
     * @param value The ToonValue to optimize
     * @return Low optimized ToonValue
     */
    private fun lowOptimization(value: ToonValue): ToonValue {
        return when (value) {
            is ToonObject -> optimizeObjectLow(value)
            is ToonArray -> optimizeArrayLow(value)
            else -> value
        }
    }

    /**
     * Performs medium-level optimization.
     * 
     * @param value The ToonValue to optimize
     * @return Medium optimized ToonValue
     */
    private fun mediumOptimization(value: ToonValue): ToonValue {
        return when (value) {
            is ToonObject -> optimizeObjectMedium(value)
            is ToonArray -> optimizeArrayMedium(value)
            else -> value
        }
    }

    /**
     * Performs high-level optimization.
     * 
     * @param value The ToonValue to optimize
     * @return High optimized ToonValue
     */
    private fun highOptimization(value: ToonValue): ToonValue {
        return when (value) {
            is ToonObject -> optimizeObjectHigh(value)
            is ToonArray -> optimizeArrayHigh(value)
            else -> value
        }
    }

    /**
     * Performs aggressive optimization.
     * 
     * @param value The ToonValue to optimize
     * @return Aggressively optimized ToonValue
     */
    private fun aggressiveOptimization(value: ToonValue): ToonValue {
        return when (value) {
            is ToonObject -> optimizeObjectAggressive(value)
            is ToonArray -> optimizeArrayAggressive(value)
            else -> value
        }
    }

    /**
     * Low-level object optimization.
     * 
     * @param obj The ToonObject to optimize
     * @return Low optimized ToonObject
     */
    private fun optimizeObjectLow(obj: ToonObject): ToonObject {
        // Basic optimization: remove empty entries
        val optimizedEntries = obj.entries.filter { (_, value) ->
            !isEmptyValue(value)
        }

        return ToonObject(optimizedEntries)
    }

    /**
     * Medium-level object optimization.
     * 
     * @param obj The ToonObject to optimize
     * @return Medium optimized ToonObject
     */
    private fun optimizeObjectMedium(obj: ToonObject): ToonObject {
        // Medium optimization: remove empty entries and deduplicate strings
        val optimizedEntries = mutableMapOf<String, ToonValue>()
        val stringCache = mutableSetOf<String>()

        obj.entries.forEach { (key, value) ->
            if (!isEmptyValue(value)) {
                val optimizedValue = if (value is ToonString) {
                    optimizeStringValue(value, stringCache)
                } else {
                    value
                }
                optimizedEntries[key] = optimizedValue
            }
        }

        return ToonObject(optimizedEntries)
    }

    /**
     * High-level object optimization.
     * 
     * @param obj The ToonObject to optimize
     * @return High optimized ToonObject
     */
    private fun optimizeObjectHigh(obj: ToonObject): ToonObject {
        // High optimization: remove empty entries, deduplicate strings, and optimize structure
        val optimizedEntries = mutableMapOf<String, ToonValue>()
        val stringCache = mutableSetOf<String>()
        val arrayCache = mutableSetOf<String>()

        obj.entries.forEach { (key, value) ->
            if (!isEmptyValue(value)) {
                val optimizedValue = when (value) {
                    is ToonString -> optimizeStringValue(value, stringCache)
                    is ToonArray -> optimizeArrayValue(value, arrayCache)
                    else -> value
                }
                optimizedEntries[key] = optimizedValue
            }
        }

        return ToonObject(optimizedEntries)
    }

    /**
     * Aggressive object optimization.
     * 
     * @param obj The ToonObject to optimize
     * @return Aggressively optimized ToonObject
     */
    private fun optimizeObjectAggressive(obj: ToonObject): ToonObject {
        // Aggressive optimization: all optimizations plus structure reorganization
        val optimizedEntries = mutableMapOf<String, ToonValue>()
        val stringCache = mutableSetOf<String>()
        val arrayCache = mutableSetOf<String>()

        obj.entries.forEach { (key, value) ->
            if (!isEmptyValue(value)) {
                val optimizedValue = when (value) {
                    is ToonString -> optimizeStringValue(value, stringCache)
                    is ToonArray -> optimizeArrayValue(value, arrayCache)
                    is ToonObject -> optimizeObjectAggressive(value) // Recursive optimization
                    else -> value
                }
                optimizedEntries[key] = optimizedValue
            }
        }

        // Sort keys for better performance
        val sortedEntries = optimizedEntries.toSortedMap()

        return ToonObject(sortedEntries)
    }

    /**
     * Low-level array optimization.
     * 
     * @param array The ToonArray to optimize
     * @return Low optimized ToonArray
     */
    private fun optimizeArrayLow(array: ToonArray): ToonArray {
        // Basic optimization: remove empty values
        val optimizedValues = array.values.filter { !isEmptyValue(it) }

        return ToonArray(optimizedValues)
    }

    /**
     * Medium-level array optimization.
     * 
     * @param array The ToonArray to optimize
     * @return Medium optimized ToonArray
     */
    private fun optimizeArrayMedium(array: ToonArray): ToonArray {
        // Medium optimization: remove empty values and deduplicate
        val optimizedValues = mutableListOf<ToonValue>()
        val valueCache = mutableSetOf<String>()

        array.values.forEach { value ->
            if (!isEmptyValue(value)) {
                val optimizedValue = when (value) {
                    is ToonString -> optimizeStringValue(value, valueCache)
                    else -> value
                }
                optimizedValues.add(optimizedValue)
            }
        }

        return ToonArray(optimizedValues)
    }

    /**
     * High-level array optimization.
     * 
     * @param array The ToonArray to optimize
     * @return High optimized ToonArray
     */
    private fun optimizeArrayHigh(array: ToonArray): ToonArray {
        // High optimization: remove empty values, deduplicate, and optimize elements
        val optimizedValues = mutableListOf<ToonValue>()
        val stringCache = mutableSetOf<String>()
        val arrayCache = mutableSetOf<String>()

        array.values.forEach { value ->
            if (!isEmptyValue(value)) {
                val optimizedValue = when (value) {
                    is ToonString -> optimizeStringValue(value, stringCache)
                    is ToonArray -> optimizeArrayValue(value, arrayCache)
                    is ToonObject -> optimizeObjectHigh(value)
                    else -> value
                }
                optimizedValues.add(optimizedValue)
            }
        }

        return ToonArray(optimizedValues)
    }

    /**
     * Aggressive array optimization.
     * 
     * @param array The ToonArray to optimize
     * @return Aggressively optimized ToonArray
     */
    private fun optimizeArrayAggressive(array: ToonArray): ToonArray {
        // Aggressive optimization: all optimizations plus array-specific optimizations
        val optimizedValues = mutableListOf<ToonValue>()
        val stringCache = mutableSetOf<String>()
        val arrayCache = mutableSetOf<String>()

        array.values.forEach { value ->
            if (!isEmptyValue(value)) {
                val optimizedValue = when (value) {
                    is ToonString -> optimizeStringValue(value, stringCache)
                    is ToonArray -> optimizeArrayValue(value, arrayCache)
                    is ToonObject -> optimizeObjectAggressive(value)
                    else -> value
                }
                optimizedValues.add(optimizedValue)
            }
        }

        // Remove duplicates and sort if possible
        val uniqueValues = optimizedValues.distinctBy { it.toString() }

        return ToonArray(uniqueValues)
    }

    /**
     * Optimizes a string value.
     * 
     * @param value The ToonString to optimize
     * @param cache Cache for deduplication
     * @return Optimized ToonString
     */
    private fun optimizeStringValue(value: ToonString, cache: MutableSet<String>): ToonString {
        val stringValue = value.value

        // Check if already cached
        if (cache.contains(stringValue)) {
            return ToonString("[CACHED:$stringValue]")
        }

        cache.add(stringValue)

        // Basic string optimization: trim if not already trimmed
        val trimmedValue = stringValue.trim()

        return if (trimmedValue != stringValue) {
            ToonString(trimmedValue)
        } else {
            value
        }
    }

    /**
     * Optimizes an array value.
     * 
     * @param value The ToonArray to optimize
     * @param cache Cache for deduplication
     * @return Optimized ToonArray
     */
    private fun optimizeArrayValue(value: ToonArray, cache: MutableSet<String>): ToonArray {
        val arrayString = value.values.joinToString(",")

        // Check if already cached
        if (cache.contains(arrayString)) {
            return ToonArray(listOf(ToonString("[CACHED:$arrayString]")))
        }

        cache.add(arrayString)

        // Basic array optimization: remove empty values
        val optimizedValues = value.values.filter { !isEmptyValue(it) }

        return ToonArray(optimizedValues)
    }

    /**
     * Checks if a ToonValue is effectively empty.
     * 
     * @param value The ToonValue to check
     * @return true if value is empty, false otherwise
     */
    private fun isEmptyValue(value: ToonValue): Boolean {
        return when (value) {
            is ToonString -> value.value.isEmpty()
            is ToonArray -> value.values.isEmpty()
            is ToonObject -> value.entries.isEmpty()
            is ToonNull -> true
            else -> false
        }
    }

    /**
     * Calculates the size of a ToonValue.
     * 
     * @param value The ToonValue to calculate size for
     * @return Estimated size in bytes
     */
    private fun calculateSize(value: ToonValue): Long {
        return when (value) {
            is ToonString -> value.value.length.toLong()
            is ToonNumber -> value.raw.length.toLong()
            is ToonBoolean -> 5L // true/false
            is ToonNull -> 4L // null
            is ToonArray -> value.values.sumOf { calculateSize(it) }
            is ToonObject -> value.entries.values.sumOf { calculateSize(it) }
        }
    }

    /**
     * Checks if structural optimizations were applied.
     * 
     * @param originalValue The original value
     * @param optimizedValue The optimized value
     * @return true if structural optimizations were applied
     */
    private fun hasStructuralOptimizations(originalValue: ToonValue, optimizedValue: ToonValue): Boolean {
        return when {
            originalValue is ToonObject && optimizedValue is ToonObject -> {
                originalValue.entries.size != optimizedValue.entries.size
            }

            originalValue is ToonArray && optimizedValue is ToonArray -> {
                originalValue.values.size != optimizedValue.values.size
            }

            else -> false
        }
    }

    /**
     * Checks if value deduplication was applied.
     * 
     * @param originalValue The original value
     * @param optimizedValue The optimized value
     * @return true if value deduplication was applied
     */
    private fun hasValueDeduplication(originalValue: ToonValue, optimizedValue: ToonValue): Boolean {
        // Check for cached values which indicate deduplication
        val originalStrings = extractStrings(originalValue)
        val optimizedStrings = extractStrings(optimizedValue)

        return originalStrings.any { it.startsWith("[CACHED:") } ||
                optimizedStrings.any { it.startsWith("[CACHED:") }
    }

    /**
     * Extracts all string values from a ToonValue.
     * 
     * @param value The ToonValue to extract strings from
     * @return Set of all string values
     */
    private fun extractStrings(value: ToonValue): Set<String> {
        val strings = mutableSetOf<String>()

        fun collectStrings(v: ToonValue) {
            when (v) {
                is ToonString -> strings.add(v.value)
                is ToonArray -> v.values.forEach { collectStrings(it) }
                is ToonObject -> v.entries.values.forEach { collectStrings(it) }
                else -> { /* No strings in other types */
                }
            }
        }

        collectStrings(value)
        return strings
    }
}

package de.tylabsx.ktoon.processor

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.MergeStrategy
import de.tylabsx.ktoon.pipeline.TransformRule

/**
 * Transforms TOON data structures according to specified rules.
 * 
 * This class handles transformation logic for converting ToonValue
 * structures using configurable transformation rules and strategies.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonTransformer {

    /**
     * Applies a transformation rule to a ToonValue.
     * 
     * @param value The value to transform
     * @param rule The transformation rule to apply
     * @return Transformed ToonValue
     */
    fun applyRule(value: ToonValue, rule: TransformRule): ToonValue {
        return rule.action(value)
    }

    /**
     * Merges multiple ToonValue instances using specified strategy.
     * 
     * @param values List of values to merge
     * @param strategy The merge strategy to use
     * @return Merged ToonValue
     */
    fun merge(values: List<ToonValue>, strategy: MergeStrategy): ToonValue {
        if (values.isEmpty()) {
            return ToonObject(emptyMap())
        }

        if (values.size == 1) {
            return values.first()
        }

        return when (strategy) {
            MergeStrategy.SHALLOW -> shallowMerge(values)
            MergeStrategy.DEEP -> deepMerge(values)
            MergeStrategy.OVERWRITE -> overwriteMerge(values)
            MergeStrategy.MERGE_ARRAYS -> mergeArrays(values)
            MergeStrategy.MERGE_OBJECTS -> mergeObjects(values)
        }
    }

    /**
     * Performs shallow merge of values.
     * 
     * @param values List of values to merge
     * @return Shallow merged ToonValue
     */
    private fun shallowMerge(values: List<ToonValue>): ToonValue {
        val result = mutableMapOf<String, ToonValue>()

        values.forEach { value ->
            when (value) {
                is ToonObject -> {
                    result.putAll(value.entries)
                }

                else -> {
                    // For non-objects, use last value
                    result["value"] = value
                }
            }
        }

        return ToonObject(result)
    }

    /**
     * Performs deep merge of values.
     * 
     * @param values List of values to merge
     * @return Deep merged ToonValue
     */
    private fun deepMerge(values: List<ToonValue>): ToonValue {
        val result = mutableMapOf<String, ToonValue>()

        values.forEach { value ->
            when (value) {
                is ToonObject -> {
                    value.entries.forEach { (key, entryValue) ->
                        val existing = result[key]
                        result[key] = if (existing != null && existing is ToonObject && entryValue is ToonObject) {
                            deepMerge(listOf(existing, entryValue))
                        } else {
                            entryValue
                        }
                    }
                }

                else -> {
                    result["value"] = value
                }
            }
        }

        return ToonObject(result)
    }

    /**
     * Performs overwrite merge of values.
     * 
     * @param values List of values to merge
     * @return Overwrite merged ToonValue
     */
    private fun overwriteMerge(values: List<ToonValue>): ToonValue {
        return values.last()
    }

    /**
     * Merges array values.
     * 
     * @param values List of values to merge
     * @return Merged array ToonValue
     */
    private fun mergeArrays(values: List<ToonValue>): ToonArray {
        val allValues = mutableListOf<ToonValue>()

        values.forEach { value ->
            when (value) {
                is ToonArray -> {
                    allValues.addAll(value.values)
                }

                else -> {
                    allValues.add(value)
                }
            }
        }

        return ToonArray(allValues)
    }

    /**
     * Merges object values.
     * 
     * @param values List of values to merge
     * @return Merged object ToonValue
     */
    private fun mergeObjects(values: List<ToonValue>): ToonObject {
        val result = mutableMapOf<String, ToonValue>()

        values.forEach { value ->
            when (value) {
                is ToonObject -> {
                    result.putAll(value.entries)
                }

                is ToonArray -> {
                    // Handle arrays - skip for object merge
                }

                is ToonString -> {
                    // Handle strings - skip for object merge
                }

                is ToonNumber -> {
                    // Handle numbers - skip for object merge
                }

                is ToonBoolean -> {
                    // Handle booleans - skip for object merge
                }

                is ToonNull -> {
                    // Handle null - skip for object merge
                }
            }
        }

        return ToonObject(result)
    }
}

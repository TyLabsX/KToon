package de.tylabsx.ktoon.processor

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.ValidationConstraints

/**
 * Validates TOON data structures and content.
 * 
 * This class handles comprehensive validation of ToonValue
 * structures including type checking, constraint validation,
 * and structural integrity verification.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonValidator {

    /**
     * Validates a ToonValue structure.
     * 
     * @param value The ToonValue to validate
     * @throws ToonParseException if validation fails
     */
    fun validate(value: ToonValue) {
        validateStructure(value, mutableListOf(), mutableListOf())
    }

    /**
     * Validates a ToonValue structure with error collection.
     * 
     * @param value The ToonValue to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    fun validateStructure(
        value: ToonValue,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        when (value) {
            is ToonObject -> validateObject(value, errors, warnings)
            is ToonArray -> validateArray(value, errors, warnings)
            is ToonString -> validateString(value, errors, warnings)
            is ToonNumber -> validateNumber(value, errors, warnings)
            is ToonBoolean -> validateBoolean(value, errors, warnings)
            is ToonNull -> validateNull(value, errors, warnings)
        }
    }

    /**
     * Validates a ToonObject.
     * 
     * @param obj The ToonObject to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateObject(
        obj: ToonObject,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        obj.entries.forEach { (key, value) ->
            if (key.isEmpty()) {
                errors.add(
                    ToonParseException(
                        message = "Empty keys are not allowed in TOON objects",
                        line = 0,
                        column = 0
                    )
                )
            }

            validateStructure(value, errors, warnings)
        }
    }

    /**
     * Validates a ToonArray.
     * 
     * @param array The ToonArray to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateArray(
        array: ToonArray,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        array.values.forEach { value ->
            validateStructure(value, errors, warnings)
        }
    }

    /**
     * Validates a ToonString.
     * 
     * @param str The ToonString to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateString(
        str: ToonString,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        // Strings are always valid
    }

    /**
     * Validates a ToonNumber.
     * 
     * @param num The ToonNumber to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateNumber(
        num: ToonNumber,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        if (num.raw.isEmpty()) {
            errors.add(
                ToonParseException(
                    message = "Empty number values are not allowed",
                    line = 0,
                    column = 0
                )
            )
        }

        // Validate number format
        val numberRegex = Regex("""^-?\d+(\.\d+)?([eE][+-]?\d+)?$""")
        if (!numberRegex.matches(num.raw)) {
            errors.add(
                ToonParseException(
                    message = "Invalid number format: ${num.raw}",
                    line = 0,
                    column = 0
                )
            )
        }
    }

    /**
     * Validates a ToonBoolean.
     * 
     * @param bool The ToonBoolean to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateBoolean(
        bool: ToonBoolean,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        // Booleans are always valid
    }

    /**
     * Validates ToonNull.
     * 
     * @param nullValue The ToonNull to validate
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    private fun validateNull(
        nullValue: ToonNull,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        // Null is always valid
    }

    /**
     * Validates constraints against a ToonValue.
     * 
     * @param value The ToonValue to validate
     * @param constraints The constraints to validate against
     * @param errors List to collect validation errors
     * @param warnings List to collect validation warnings
     */
    fun validateConstraints(
        value: ToonValue,
        constraints: ValidationConstraints,
        errors: MutableList<ToonParseException>,
        warnings: MutableList<String>
    ) {
        // Check required paths
        constraints.requiredPaths.forEach { requiredPath ->
            if (!hasPath(value, requiredPath)) {
                errors.add(
                    ToonParseException(
                        message = "Required path '$requiredPath' is missing",
                        line = 0,
                        column = 0
                    )
                )
            }
        }

        // Check forbidden paths
        constraints.forbiddenPaths.forEach { forbiddenPath ->
            if (hasPath(value, forbiddenPath)) {
                errors.add(
                    ToonParseException(
                        message = "Forbidden path '$forbiddenPath' is present",
                        line = 0,
                        column = 0
                    )
                )
            }
        }

        // Check type constraints
        constraints.typeConstraints.forEach { (path, expectedType) ->
            val pathValue = getValueByPath(value, path)
            if (pathValue != null) {
                val actualType = when (pathValue) {
                    is ToonObject -> "object"
                    is ToonArray -> "array"
                    is ToonString -> "string"
                    is ToonNumber -> "number"
                    is ToonBoolean -> "boolean"
                    is ToonNull -> "null"
                }

                if (actualType != expectedType) {
                    errors.add(
                        ToonParseException(
                            message = "Path '$path' expected type '$expectedType' but found '$actualType'",
                            line = 0,
                            column = 0
                        )
                    )
                }
            }
        }

        // Check value constraints
        constraints.valueConstraints.forEach { (path, constraint) ->
            val pathValue = getValueByPath(value, path)
            if (pathValue != null && !constraint(pathValue)) {
                errors.add(
                    ToonParseException(
                        message = "Path '$path' failed value constraint",
                        line = 0,
                        column = 0
                    )
                )
            }
        }
    }

    /**
     * Checks if a ToonValue contains a specific path.
     * 
     * @param value The ToonValue to search
     * @param path The path to look for
     * @return true if path exists, false otherwise
     */
    private fun hasPath(value: ToonValue, path: String): Boolean {
        return getValueByPath(value, path) != null
    }

    /**
     * Gets a value from a ToonValue by path.
     * 
     * @param value The ToonValue to search
     * @param path The path to extract
     * @return The value at the path, or null if not found
     */
    private fun getValueByPath(value: ToonValue, path: String): ToonValue? {
        val pathParts = path.split('.')
        var current = value

        for (part in pathParts) {
            when (current) {
                is ToonObject -> {
                    current = current.entries[part] ?: return null
                }

                else -> {
                    return null
                }
            }
        }

        return current
    }
}

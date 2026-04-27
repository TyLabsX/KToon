package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.*

/**
 * Central orchestration engine for all KToon operations.
 * 
 * This class serves as the main entry point and coordinator for the entire
 * KToon library, implementing the "God Class" pattern as requested.
 * It provides unified access to parsing, processing, writing, and all
 * other KToon operations through a single, comprehensive interface.
 * 
 * The engine orchestrates these subsystems:
 * - KToonParserEngine: Handles all parsing operations
 * - KToonProcessingEngine: Handles data transformation and manipulation
 * - KToonWriterEngine: Handles serialization and output
 * - Configuration management through KToonContext
 * - Error handling and validation across all operations
 * - Performance monitoring and optimization
 * 
 * Example usage:
 * ```kotlin
 * val engine = KToonEngine()
 * val parsed = engine.parse(toonInput)
 * val processed = engine.transform(parsed.toonValue, rules)
 * val output = engine.stringify(processed.transformedValue)
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonEngine {

    /**
     * Subsystem engines for different operations.
     * These are instantiated once and reused for all operations.
     */
    private val parserEngine = KToonParserEngine()
    private val processingEngine = KToonProcessingEngine()
    private val writerEngine = KToonWriterEngine()

    /**
     * Parses TOON input into ToonValue structure.
     * 
     * This method provides the main entry point for parsing operations,
     * delegating to the parser engine and providing unified error handling.
     * 
     * @param input The raw TOON string to parse
     * @return ParseResult containing parsed ToonValue and metadata
     * @throws ToonParseException if parsing fails
     */
    fun parse(input: String): ParseResult {
        return try {
            debugLog("Starting TOON parsing")
            val result = parserEngine.parse(input)
            debugLog("Parsing completed successfully")
            result
        } catch (e: ToonParseException) {
            debugLog("Parsing failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected parsing error: ${e.message}")
            throw ToonParseException(
                message = "Parsing operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Serializes a ToonValue to TOON format string.
     * 
     * This method provides the main entry point for serialization,
     * delegating to the writer engine with default options.
     * 
     * @param value The ToonValue to serialize
     * @return TOON format string
     * @throws ToonParseException if serialization fails
     */
    fun stringify(value: ToonValue): String {
        return try {
            debugLog("Starting TOON serialization")
            val result = writerEngine.stringify(value)
            debugLog("Serialization completed successfully")
            result.serializedData
        } catch (e: ToonParseException) {
            debugLog("Serialization failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected serialization error: ${e.message}")
            throw ToonParseException(
                message = "Serialization operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Validates TOON input without fully parsing it.
     * 
     * This method provides quick validation capabilities for checking
     * TOON syntax and structure without the overhead of full parsing.
     * 
     * @param input The TOON input to validate
     * @return ValidationResult indicating whether input is valid
     */
    fun validate(input: String): ValidationResult {
        return try {
            debugLog("Starting TOON validation")
            val result = parserEngine.validate(input)
            debugLog("Validation completed: ${if (result.isValid) "VALID" else "INVALID"}")
            result
        } catch (e: Exception) {
            debugLog("Validation error: ${e.message}")
            ValidationResult(
                isValid = false,
                errors = listOf(
                    ToonParseException(
                        message = "Validation operation failed: ${e.message}",
                        line = 0,
                        column = 0,
                        cause = e
                    )
                ),
                warnings = emptyList(),
                parseResult = null
            )
        }
    }

    /**
     * Transforms a ToonValue using specified transformation rules.
     * 
     * This method provides unified access to transformation capabilities,
     * delegating to the processing engine with proper error handling.
     * 
     * @param value The ToonValue to transform
     * @param rules The transformation rules to apply
     * @return TransformationResult containing transformed value and metadata
     * @throws ToonParseException if transformation fails
     */
    fun transform(value: ToonValue, rules: TransformRules): TransformationResult {
        return try {
            debugLog("Starting transformation with ${rules.rules.size} rules")
            val result = processingEngine.transform(value, rules)
            debugLog("Transformation completed successfully")
            result
        } catch (e: ToonParseException) {
            debugLog("Transformation failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected transformation error: ${e.message}")
            throw ToonParseException(
                message = "Transformation operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Filters a ToonValue using specified filter criteria.
     * 
     * This method provides unified access to filtering capabilities,
     * delegating to the processing engine with proper error handling.
     * 
     * @param value The ToonValue to filter
     * @param criteria The filter criteria to apply
     * @return FilterResult containing filtered results and metadata
     * @throws ToonParseException if filtering fails
     */
    fun filter(value: ToonValue, criteria: FilterCriteria): FilterResult {
        return try {
            debugLog("Starting filtering with ${criteria.conditions.size} conditions")
            val result = processingEngine.filter(value, criteria)
            debugLog("Filtering completed: ${result.matchedCount} matches")
            result
        } catch (e: ToonParseException) {
            debugLog("Filtering failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected filtering error: ${e.message}")
            throw ToonParseException(
                message = "Filtering operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Converts a ToonValue to a different format.
     * 
     * This method provides unified access to format conversion capabilities,
     * supporting conversion to JSON, XML, YAML, and other formats.
     * 
     * @param value The ToonValue to convert
     * @param targetFormat The target format to convert to
     * @param options Conversion options
     * @return ConversionResult with converted data and metadata
     * @throws ToonParseException if conversion fails
     */
    fun convert(
        value: ToonValue,
        targetFormat: TargetFormat,
        options: ConversionOptions = ConversionOptions()
    ): ConversionResult {
        return try {
            debugLog("Starting conversion to $targetFormat")
            val result = processingEngine.convert(value, targetFormat, options)
            debugLog("Conversion completed successfully")
            result
        } catch (e: ToonParseException) {
            debugLog("Conversion failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected conversion error: ${e.message}")
            throw ToonParseException(
                message = "Conversion operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Merges multiple ToonValue instances.
     * 
     * This method provides unified access to merging capabilities,
     * delegating to the processing engine with proper error handling.
     * 
     * @param values List of ToonValue instances to merge
     * @param strategy The merge strategy to use
     * @return MergeResult with merged value and metadata
     * @throws ToonParseException if merging fails
     */
    fun merge(values: List<ToonValue>, strategy: MergeStrategy = MergeStrategy.DEEP): MergeResult {
        return try {
            debugLog("Starting merge of ${values.size} values with $strategy strategy")
            val result = processingEngine.merge(values, strategy)
            debugLog("Merge completed successfully")
            result
        } catch (e: ToonParseException) {
            debugLog("Merge failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected merge error: ${e.message}")
            throw ToonParseException(
                message = "Merge operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Queries a ToonValue using a query language.
     * 
     * This method provides unified access to querying capabilities,
     * supporting TOON-specific query language for data extraction.
     * 
     * @param value The ToonValue to query
     * @param query The query string to execute
     * @return QueryResult with query results and metadata
     * @throws ToonParseException if query fails
     */
    fun query(value: ToonValue, query: String): QueryResult {
        return try {
            debugLog("Starting query: $query")
            val result = processingEngine.query(value, query)
            debugLog("Query completed: ${result.results.size} results")
            result
        } catch (e: ToonParseException) {
            debugLog("Query failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected query error: ${e.message}")
            throw ToonParseException(
                message = "Query operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Optimizes a ToonValue for performance and size.
     * 
     * This method provides unified access to optimization capabilities,
     * delegating to the processing engine with proper error handling.
     * 
     * @param value The ToonValue to optimize
     * @param optimizationLevel The level of optimization to apply
     * @return OptimizationResult with optimized value and statistics
     * @throws ToonParseException if optimization fails
     */
    fun optimize(
        value: ToonValue,
        optimizationLevel: OptimizationLevel = OptimizationLevel.MEDIUM
    ): OptimizationResult {
        return try {
            debugLog("Starting optimization with $optimizationLevel level")
            val result = processingEngine.optimize(value, optimizationLevel)
            debugLog("Optimization completed successfully")
            result
        } catch (e: ToonParseException) {
            debugLog("Optimization failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            debugLog("Unexpected optimization error: ${e.message}")
            throw ToonParseException(
                message = "Optimization operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Performs a complete roundtrip: parse -> transform -> stringify.
     * 
     * This method provides a convenient way to perform common operations
     * in sequence, with proper error handling and performance monitoring.
     * 
     * @param input The raw TOON input to process
     * @param transformation Optional transformation rules to apply
     * @param writerOptions Optional writer options for output
     * @return RoundtripResult containing all intermediate results
     * @throws ToonParseException if any operation fails
     */
    fun roundtrip(
        input: String,
        transformation: TransformRules? = null,
        writerOptions: WriterOptions? = null
    ): RoundtripResult {
        return try {
            debugLog("Starting roundtrip operation")
            val startTime = System.currentTimeMillis()

            // Parse phase
            val parseResult = parse(input)

            // Transform phase (if transformation provided)
            val transformResult = transformation?.let { rules ->
                transform(parseResult.toonValue, rules)
            }

            // Stringify phase
            val finalValue = transformResult?.transformedValue ?: parseResult.toonValue
            val writerResult = if (writerOptions != null) {
                writerEngine.stringify(finalValue, writerOptions)
            } else {
                writerEngine.stringify(finalValue)
            }

            val totalTime = System.currentTimeMillis() - startTime

            RoundtripResult(
                originalInput = input,
                parseResult = parseResult,
                transformResult = transformResult,
                writerResult = writerResult,
                finalOutput = writerResult.serializedData,
                totalTime = totalTime
            )

        } catch (e: Exception) {
            debugLog("Roundtrip failed: ${e.message}")
            throw ToonParseException(
                message = "Roundtrip operation failed: ${e.message}",
                line = 0,
                column = 0,
                cause = e
            )
        }
    }

    /**
     * Gets comprehensive statistics about all KToon engines.
     * 
     * This method provides detailed information about capabilities,
     * performance, and configuration of all subsystems.
     * 
     * @return EngineStatistics with comprehensive engine information
     */
    fun getEngineStatistics(): EngineStatistics {
        return EngineStatistics(
            parserStatistics = parserEngine.getParsingStatistics(),
            processingStatistics = processingEngine.getProcessingStatistics(),
            writerStatistics = writerEngine.getWriterStatistics(),
            contextConfiguration = KToonContext.current.getConfigurationSummary(),
            totalPhases = parserEngine.getParsingStatistics().totalPhases,
            supportedOperations = processingEngine.getProcessingStatistics().supportedOperations,
            supportedFormats = writerEngine.getWriterStatistics().supportedFormats
        )
    }

    /**
     * Configures the KToon context with new settings.
     * 
     * This method provides unified access to configuration management,
     * allowing changes to global settings that affect all operations.
     * 
     * @param configuration Configuration block to apply
     */
    fun configure(configuration: KToonContext.() -> Unit) {
        debugLog("Applying new configuration")
        KToonContext.configure(configuration)
        debugLog("Configuration updated successfully")
    }

    /**
     * Resets the KToon context to default settings.
     * 
     * This method provides a way to restore all configuration
     * settings to their default values.
     */
    fun resetConfiguration() {
        debugLog("Resetting configuration to defaults")
        KToonContext.resetToDefaults()
        debugLog("Configuration reset successfully")
    }

    /**
     * Logs debug information if debug logging is enabled.
     * 
     * @param message The debug message to log
     */
    private fun debugLog(message: String) {
        if (KToonContext.current.enableDebugLogging) {
            println("[KToonEngine] $message")
        }
    }
}

/**
 * Result of a complete roundtrip operation.
 * 
 * @property originalInput The original TOON input
 * @property parseResult Result of parsing phase
 * @property transformResult Result of transformation phase (if applicable)
 * @property writerResult Result of writer phase
 * @property finalOutput The final serialized output
 * @property totalTime Total time taken for all operations
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class RoundtripResult(
    val originalInput: String,
    val parseResult: ParseResult,
    val transformResult: TransformationResult?,
    val writerResult: WriterResult,
    val finalOutput: String,
    val totalTime: Long
) {
    /**
     * Gets a summary of the roundtrip operation.
     * 
     * @return Human-readable summary string
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Roundtrip Summary:")
            appendLine("  Original Input Length: ${originalInput.length}")
            appendLine("  Final Output Length: ${finalOutput.length}")
            appendLine("  Total Time: ${totalTime}ms")
            transformResult?.let { transform ->
                appendLine("  Transformation Applied: ${transform.appliedRules.size} rules")
            }
            appendLine("  Parse Success: ${parseResult.toonValue != null}")
        }
    }
}

/**
 * Comprehensive statistics about all KToon engines.
 * 
 * @property parserStatistics Statistics from parser engine
 * @property processingStatistics Statistics from processing engine
 * @property writerStatistics Statistics from writer engine
 * @property contextConfiguration Current context configuration
 * @property totalPhases Total number of parsing phases
 * @property supportedOperations List of all supported operations
 * @property supportedFormats List of all supported formats
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class EngineStatistics(
    val parserStatistics: ParsingStatistics,
    val processingStatistics: ProcessingStatistics,
    val writerStatistics: WriterStatistics,
    val contextConfiguration: String,
    val totalPhases: Int,
    val supportedOperations: List<String>,
    val supportedFormats: List<String>
) {
    /**
     * Gets a comprehensive summary of all engine statistics.
     * 
     * @return Human-readable summary string
     */
    fun getComprehensiveSummary(): String {
        return buildString {
            appendLine("KToon Engine Statistics:")
            appendLine("  Parser Phases: $totalPhases")
            appendLine("  Supported Operations: ${supportedOperations.size}")
            appendLine("  Supported Formats: ${supportedFormats.size}")
            appendLine("  Current Configuration:")
            append(contextConfiguration.lines().joinToString("\n") { "    $it" })
        }
    }
}

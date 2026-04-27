package de.tylabsx.ktoon.pipeline

import de.tylabsx.ktoon.ToonParseException
import de.tylabsx.ktoon.ToonValue

/**
 * Strategy interface for TOON parsing operations.
 * 
 * This interface defines the contract for different parsing strategies
 * that can be used to parse TOON data. Implementations can provide
 * different approaches to parsing, such as strict vs. lenient parsing,
 * streaming vs. full parsing, etc.
 * 
 * The strategy pattern allows for flexible parsing behavior while
 * maintaining a consistent interface across all implementations.
 * 
 * Example usage:
 * ```kotlin
 * val strategy = StrictToonParsingStrategy()
 * val result = strategy.parse(toonInput)
 * ```
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
interface ToonParsingStrategy {

    /**
     * Parses TOON input into a ToonValue structure.
     * 
     * This method takes raw TOON input and converts it into the
     * internal ToonValue representation. The specific parsing behavior
     * depends on the strategy implementation.
     * 
     * @param input The raw TOON string to parse
     * @return Parsed ToonValue structure
     * @throws ToonParseException if parsing fails
     */
    fun parse(input: String): ToonValue

    /**
     * Checks if this strategy can handle the given input.
     * 
     * This method allows strategies to indicate whether they are
     * suitable for parsing the given input based on format, size,
     * or other characteristics.
     * 
     * @param input The input to check
     * @return true if this strategy can handle the input, false otherwise
     */
    fun canHandle(input: String): Boolean

    /**
     * Returns the name of this parsing strategy.
     * 
     * @return Strategy name for identification and debugging
     */
    fun getStrategyName(): String
}

/**
 * Strategy interface for TOON value resolution.
 * 
 * This interface defines the contract for resolving raw values
 * into appropriate ToonValue instances. Different implementations
 * can provide different type inference and conversion logic.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
interface ToonValueResolver {

    /**
     * Resolves a raw string value into a ToonValue.
     * 
     * This method determines the appropriate ToonValue type based on
     * the content of the raw string and converts it accordingly.
     * 
     * @param rawValue The raw string value to resolve
     * @return Resolved ToonValue instance
     * @throws ToonParseException if value resolution fails
     */
    fun resolveValue(rawValue: String): ToonValue

    /**
     * Checks if this resolver can handle the given value.
     * 
     * @param rawValue The raw value to check
     * @return true if this resolver can handle the value, false otherwise
     */
    fun canResolve(rawValue: String): Boolean

    /**
     * Returns the priority of this resolver.
     * 
     * Higher priority resolvers are tried first. This allows for
     * creating specialized resolvers that take precedence over
     * general-purpose ones.
     * 
     * @return Priority value (higher = higher priority)
     */
    fun getPriority(): Int
}

/**
 * Strategy interface for TOON node building.
 * 
 * This interface defines the contract for building TOON nodes
 * from parsed tokens and constructing the hierarchical structure.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
interface ToonNodeBuilder {

    /**
     * Builds a TOON node from the given components.
     * 
     * @param key The node key (or null for root nodes)
     * @param value The node value
     * @param indentation The indentation level
     * @param children Child nodes (if any)
     * @return Constructed TOON node
     * @throws ToonParseException if node building fails
     */
    fun buildNode(
        key: String?,
        value: String?,
        indentation: Int,
        children: List<ToonNode>
    ): ToonNode

    /**
     * Validates the node structure.
     * 
     * @param node The node to validate
     * @throws ToonParseException if the node is invalid
     */
    fun validateNode(node: ToonNode)
}

/**
 * Strategy interface for TOON line processing.
 * 
 * This interface defines the contract for processing individual
 * lines of TOON input and extracting meaningful information.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
interface ToonLineProcessor {

    /**
     * Processes a single line of TOON input.
     * 
     * @param line The line to process
     * @param lineNumber The line number (1-based)
     * @return Processed line information
     * @throws ToonParseException if line processing fails
     */
    fun processLine(line: String, lineNumber: Int): ProcessedLine

    /**
     * Checks if this processor can handle the given line.
     * 
     * @param line The line to check
     * @return true if this processor can handle the line, false otherwise
     */
    fun canProcess(line: String): Boolean
}

/**
 * Strategy interface for TOON structure assembly.
 * 
 * This interface defines the contract for assembling processed lines
 * into the final TOON structure hierarchy.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
interface ToonStructureAssembler {

    /**
     * Assembles processed lines into a TOON structure.
     * 
     * @param processedLines List of processed lines to assemble
     * @return Assembled TOON node structure
     * @throws ToonParseException if assembly fails
     */
    fun assembleStructure(processedLines: List<ProcessedLine>): ToonNode

    /**
     * Validates the assembled structure.
     * 
     * @param root The root node of the assembled structure
     * @throws ToonParseException if the structure is invalid
     */
    fun validateStructure(root: ToonNode)
}

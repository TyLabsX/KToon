package de.tylabsx.ktoon.pipeline

/**
 * Represents a node in the TOON parsing tree structure.
 * 
 * This class is an internal representation used during the parsing process
 * to build the hierarchical structure before converting it to ToonValue instances.
 * Each node represents a line or section of TOON input with its associated
 * metadata and relationships.
 * 
 * @property key The key of this node (null for root nodes)
 * @property rawValue The raw string value (null if this node has children)
 * @property indentation The indentation level of this node
 * @property lineNumber The line number where this node originated (1-based)
 * @property children Child nodes of this node (empty for leaf nodes)
 * @property parent Parent node (null for root node)
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ToonNode(
    /**
     * The key associated with this node.
     * 
     * For object entries, this is the key name. For root nodes or array
     * elements, this may be null. The key is always stored as the raw
     * string from the input without any processing.
     */
    val key: String?,

    /**
     * The raw value string for this node.
     * 
     * This contains the raw text after the colon in key-value pairs.
     * For nodes that have children (objects), this will be null.
     * For leaf nodes, this contains the actual value to be processed.
     */
    val rawValue: String?,

    /**
     * The indentation level of this node.
     * 
     * This represents the depth in the TOON hierarchy, calculated
     * based on the number of indentation characters. Root nodes have
     * indentation level 0.
     */
    val indentation: Int,

    /**
     * The line number where this node was defined.
     * 
     * This is 1-based and helps with error reporting and debugging.
     * The line number corresponds to the original input line.
     */
    val lineNumber: Int
) {
    /**
     * Child nodes of this node.
     * 
     * For object nodes, this contains all nested entries.
     * For leaf nodes, this list is empty. The children are
     * automatically ordered based on their appearance in input.
     */
    val children: MutableList<ToonNode> = mutableListOf()

    /**
     * Parent node reference.
     * 
     * This is set automatically when nodes are added as children
     * to other nodes. Root nodes have null parent.
     */
    var parent: ToonNode? = null

    /**
     * Checks if this node is a leaf node (has no children).
     * 
     * @return true if this node has no children, false otherwise
     */
    fun isLeaf(): Boolean = children.isEmpty()

    /**
     * Checks if this node is a root node (has no parent).
     * 
     * @return true if this node has no parent, false otherwise
     */
    fun isRoot(): Boolean = parent == null

    /**
     * Checks if this node represents an object (has children).
     * 
     * @return true if this node has children, false otherwise
     */
    fun isObject(): Boolean = children.isNotEmpty()

    /**
     * Checks if this node represents a value (has rawValue but no children).
     * 
     * @return true if this node has a raw value and no children, false otherwise
     */
    fun isValue(): Boolean = rawValue != null && children.isEmpty()

    /**
     * Adds a child node to this node.
     * 
     * This method automatically sets the parent reference and maintains
     * the proper hierarchy. The child's indentation should be greater
     * than this node's indentation.
     * 
     * @param child The child node to add
     * @throws IllegalArgumentException if child indentation is invalid
     */
    fun addChild(child: ToonNode) {
        require(child.indentation > this.indentation) {
            "Child indentation (${child.indentation}) must be greater than parent indentation (${this.indentation})"
        }

        // Prevent circular references
        var current: ToonNode? = this
        while (current != null) {
            require(current != child) {
                "Circular reference detected: cannot add node to its own descendants"
            }
            current = current.parent
        }

        child.parent = this
        children.add(child)
    }

    /**
     * Removes a child node from this node.
     * 
     * @param child The child node to remove
     * @return true if the child was removed, false if it wasn't found
     */
    fun removeChild(child: ToonNode): Boolean {
        child.parent = null
        return children.remove(child)
    }

    /**
     * Finds a child node by key.
     * 
     * @param key The key to search for
     * @return The child node with the given key, or null if not found
     */
    fun findChildByKey(key: String): ToonNode? {
        return children.find { it.key == key }
    }

    /**
     * Gets all descendant nodes at the specified depth.
     * 
     * @param depth The target depth relative to this node (0 = direct children)
     * @return List of nodes at the specified depth
     */
    fun getDescendantsAtDepth(depth: Int): List<ToonNode> {
        if (depth < 0) return emptyList()
        if (depth == 0) return children.toList()

        return children.flatMap { it.getDescendantsAtDepth(depth - 1) }
    }

    /**
     * Gets the path from the root to this node.
     * 
     * @return List of keys from root to this node (excluding this node's key)
     */
    fun getPath(): List<String> {
        val path = mutableListOf<String>()
        var current = parent

        while (current != null) {
            current.key?.let { path.add(0, it) }
            current = current.parent
        }

        return path
    }

    /**
     * Gets the full path including this node's key.
     * 
     * @return Full path as dot-separated string
     */
    fun getFullPath(): String {
        val path = getPath().toMutableList()
        key?.let { path.add(it) }
        return path.joinToString(".")
    }

    /**
     * Validates the node structure.
     * 
     * This method checks for common structural issues like
     * invalid indentation, circular references, etc.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        // Check for circular references
        var current = parent
        var visited = mutableSetOf<ToonNode>()

        while (current != null) {
            if (current in visited) {
                throw IllegalArgumentException("Circular reference detected in TOON structure")
            }
            visited.add(current)
            current = current.parent
        }

        // Check indentation consistency
        children.forEach { child ->
            require(child.indentation > this.indentation) {
                "Child indentation (${child.indentation}) must be greater than parent indentation (${this.indentation}) at line ${child.lineNumber}"
            }
        }

        // Validate children recursively
        children.forEach { it.validate() }
    }

    /**
     * Creates a deep copy of this node and its subtree.
     * 
     * @return Deep copy of this node
     */
    fun deepCopy(): ToonNode {
        val copy = ToonNode(
            key = this.key,
            rawValue = this.rawValue,
            indentation = this.indentation,
            lineNumber = this.lineNumber
        )

        this.children.forEach { child ->
            copy.addChild(child.deepCopy())
        }

        return copy
    }

    /**
     * Returns a string representation of this node for debugging.
     * 
     * @return Debug string representation
     */
    override fun toString(): String {
        val indent = "  ".repeat(indentation)
        val keyPart = key ?: "(root)"
        val valuePart = rawValue?.let { ": $it" } ?: ""
        val childrenPart = if (children.isNotEmpty()) " (${children.size} children)" else ""

        return "$indent$keyPart$valuePart$childrenPart (line $lineNumber)"
    }
}

/**
 * Represents a processed line from TOON input.
 * 
 * This data class contains the results of processing a single line
 * of TOON input, including extracted metadata and classification.
 * 
 * @property originalLine The original line content
 * @property lineNumber The line number (1-based)
 * @property indentation The indentation level
 * @property key The extracted key (if any)
 * @property value The extracted value (if any)
 * @property type The type of line (comment, empty, key-value, etc.)
 * @property isValid Whether the line is syntactically valid
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
data class ProcessedLine(
    /**
     * The original line content as it appeared in input.
     */
    val originalLine: String,

    /**
     * The line number in the original input (1-based).
     */
    val lineNumber: Int,

    /**
     * The calculated indentation level.
     */
    val indentation: Int,

    /**
     * The extracted key from the line (if applicable).
     */
    val key: String?,

    /**
     * The extracted value from the line (if applicable).
     */
    val value: String?,

    /**
     * The classification of this line type.
     */
    val type: LineType,

    /**
     * Whether this line passed syntax validation.
     */
    val isValid: Boolean
)

/**
 * Enumeration of possible line types in TOON input.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
enum class LineType {
    /**
     * An empty line containing only whitespace.
     */
    EMPTY,

    /**
     * A comment line starting with #.
     */
    COMMENT,

    /**
     * A key-value pair with a colon separator.
     */
    KEY_VALUE,

    /**
     * A key without a value (indicating a nested object).
     */
    KEY_ONLY,

    /**
     * An inline array definition.
     */
    INLINE_ARRAY,

    /**
     * A tabular array header definition.
     */
    TABULAR_ARRAY_HEADER,

    /**
     * A tabular array data row.
     */
    TABULAR_ARRAY_ROW,

    /**
     * A line that doesn't match any known pattern.
     */
    UNKNOWN
}

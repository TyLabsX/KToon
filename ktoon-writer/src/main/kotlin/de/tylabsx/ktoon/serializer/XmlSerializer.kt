package de.tylabsx.ktoon.serializer

import de.tylabsx.ktoon.*
import de.tylabsx.ktoon.pipeline.XmlOptions

/**
 * Serializes ToonValue structures to XML format.
 * 
 * This class handles conversion from ToonValue instances to
 * valid XML strings with proper formatting and escaping.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class XmlSerializer {

    /**
     * Serializes a ToonValue to XML format string.
     * 
     * @param value The ToonValue to serialize
     * @param options XML formatting options
     * @return XML format string
     */
    fun serialize(value: ToonValue, options: XmlOptions): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

        if (options.pretty) {
            builder.append('\n')
        }

        serializeValue(value, builder, options, options.rootElementName, 0)

        return builder.toString()
    }

    /**
     * Serializes a ToonValue to XML with proper indentation.
     * 
     * @param value The ToonValue to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeValue(
        value: ToonValue,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        when (value) {
            is ToonObject -> serializeObject(value, builder, options, tagName, depth)
            is ToonArray -> serializeArray(value, builder, options, tagName, depth)
            is ToonString -> serializeString(value, builder, options, tagName, depth)
            is ToonNumber -> serializeNumber(value, builder, options, tagName, depth)
            is ToonBoolean -> serializeBoolean(value, builder, options, tagName, depth)
            is ToonNull -> serializeNull(builder, options, tagName, depth)
        }
    }

    /**
     * Serializes a ToonObject to XML.
     * 
     * @param obj The ToonObject to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeObject(
        obj: ToonObject,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        if (obj.entries.isEmpty()) {
            writeIndent(builder, depth, options)
            builder.append("<$tagName/>")
            if (options.pretty) {
                builder.append('\n')
            }
            return
        }

        writeIndent(builder, depth, options)
        builder.append("<$tagName")

        if (options.pretty) {
            builder.append(">\n")
        } else {
            builder.append('>')
        }

        obj.entries.forEach { (key, value) ->
            val childTag = if (options.includeAttributes && isSimpleValue(value)) {
                serializeAsAttribute(key, value, builder, options, depth)
            } else {
                serializeValue(value, builder, options, key, depth + 1)
            }
        }

        writeIndent(builder, depth, options)
        builder.append("</$tagName>")

        if (options.pretty) {
            builder.append('\n')
        }
    }

    /**
     * Serializes a ToonArray to XML.
     * 
     * @param array The ToonArray to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeArray(
        array: ToonArray,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        if (array.values.isEmpty()) {
            writeIndent(builder, depth, options)
            builder.append("<$tagName/>")
            if (options.pretty) {
                builder.append('\n')
            }
            return
        }

        array.values.forEach { value ->
            serializeValue(value, builder, options, tagName, depth + 1)
        }
    }

    /**
     * Serializes a ToonString to XML.
     * 
     * @param str The ToonString to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeString(
        str: ToonString,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        writeIndent(builder, depth, options)
        builder.append("<$tagName>")
        builder.append(escapeXmlContent(str.value))
        builder.append("</$tagName>")

        if (options.pretty) {
            builder.append('\n')
        }
    }

    /**
     * Serializes a ToonNumber to XML.
     * 
     * @param num The ToonNumber to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeNumber(
        num: ToonNumber,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        writeIndent(builder, depth, options)
        builder.append("<$tagName>")
        builder.append(num.raw)
        builder.append("</$tagName>")

        if (options.pretty) {
            builder.append('\n')
        }
    }

    /**
     * Serializes a ToonBoolean to XML.
     * 
     * @param bool The ToonBoolean to serialize
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeBoolean(
        bool: ToonBoolean,
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        writeIndent(builder, depth, options)
        builder.append("<$tagName>")
        builder.append(bool.value)
        builder.append("</$tagName>")

        if (options.pretty) {
            builder.append('\n')
        }
    }

    /**
     * Serializes ToonNull to XML.
     * 
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param tagName Name for the XML element
     * @param depth Current indentation depth
     */
    private fun serializeNull(
        builder: StringBuilder,
        options: XmlOptions,
        tagName: String,
        depth: Int
    ) {
        writeIndent(builder, depth, options)
        builder.append("<$tagName/>")

        if (options.pretty) {
            builder.append('\n')
        }
    }

    /**
     * Serializes a value as XML attribute.
     * 
     * @param key The attribute name
     * @param value The attribute value
     * @param builder StringBuilder to write to
     * @param options XML formatting options
     * @param depth Current indentation depth
     */
    private fun serializeAsAttribute(
        key: String,
        value: ToonValue,
        builder: StringBuilder,
        options: XmlOptions,
        depth: Int
    ) {
        builder.append(" $key=\"")

        when (value) {
            is ToonString -> builder.append(escapeXmlAttribute(value.value))
            is ToonNumber -> builder.append(escapeXmlAttribute(value.raw))
            is ToonBoolean -> builder.append(value.value)
            else -> builder.append("")
        }

        builder.append('"')
    }

    /**
     * Checks if a value is simple enough to be an attribute.
     * 
     * @param value The value to check
     * @return true if value can be an attribute, false otherwise
     */
    private fun isSimpleValue(value: ToonValue): Boolean {
        return value is ToonString || value is ToonNumber || value is ToonBoolean
    }

    /**
     * Writes indentation to the builder.
     * 
     * @param builder StringBuilder to write to
     * @param depth Indentation depth
     * @param options XML formatting options
     */
    private fun writeIndent(builder: StringBuilder, depth: Int, options: XmlOptions) {
        if (options.pretty) {
            builder.append(" ".repeat(depth * options.indentSize))
        }
    }

    /**
     * Escapes content for XML.
     * 
     * @param content The content to escape
     * @return Escaped XML content
     */
    private fun escapeXmlContent(content: String): String {
        return content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Escapes content for XML attribute.
     * 
     * @param content The content to escape
     * @return Escaped XML attribute content
     */
    private fun escapeXmlAttribute(content: String): String {
        return escapeXmlContent(content).replace("\n", "&#10;").replace("\r", "&#13;")
    }
}

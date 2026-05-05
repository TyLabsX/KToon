package de.tylabsx.ktoon.kotlinx.streaming

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Serializer-driven TOON encoder that writes directly to a [StringBuilder].
 *
 * The encoder consumes events emitted by kotlinx.serialization serializers and
 * renders TOON v2.1 text without creating [de.tylabsx.ktoon.ToonValue],
 * [kotlinx.serialization.json.JsonElement], or using the KToon writer engine.
 *
 * [KToonStreamingMode.FAST] is speed-oriented and renders object arrays as
 * block arrays. [KToonStreamingMode.COMPACT] is size-oriented and buffers list
 * items enough to render tabular object arrays when every row is primitive and
 * has the same fields in the same order. The buffering is deliberate: TOON
 * tabular arrays require the complete column header before the first row can be
 * written.
 *
 * @property builder destination builder receiving TOON text
 * @property options streaming formatting options
 * @property serializersModule serializers module used for contextual serializers
 * @since 1.1.0
 * @author TyLabsX
 */
@OptIn(ExperimentalSerializationApi::class)
class KToonStreamingEncoder(
    private val builder: StringBuilder,
    private val options: KToonStreamingOptions = KToonStreamingOptions(),
    override val serializersModule: SerializersModule = SerializersModule {}
) : AbstractEncoder(), CompositeEncoder {

    private val sink = StringBuilderSink(builder, IndentCache(options.indentSize))

    /**
     * Encodes a value with the provided serializer into this encoder's builder.
     *
     * @param serializer serializer for the value
     * @param value value to encode
     */
    fun <T> encode(serializer: SerializationStrategy<T>, value: T) {
        encodeSerializableValue(serializer, value)
    }

    /**
     * Accepts all root elements emitted by serializers.
     *
     * @param descriptor descriptor of the encoded structure
     * @param index element index emitted by the serializer
     * @return always true
     */
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

    /**
     * Begins streaming a structured root value.
     *
     * @param descriptor descriptor of the root structure
     * @return composite encoder for lists, maps, or objects
     * @throws UnsupportedOperationException when the descriptor represents
     * polymorphic serialization
     */
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        return when (descriptor.kind) {
            StructureKind.LIST -> ListEncoder(sink, null, 0, options, serializersModule)
            StructureKind.MAP -> MapEncoder(sink, null, 0, options, serializersModule)
            else -> ObjectEncoder(sink, 0, options, serializersModule)
        }
    }

    /**
     * Begins streaming a root collection with a known size.
     *
     * FAST mode uses the known size to choose lower-overhead list encoders.
     * Other modes delegate to [beginStructure].
     *
     * @param descriptor collection descriptor
     * @param collectionSize number of collection elements reported by the
     * serializer
     * @return composite encoder for the collection
     */
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        rejectPolymorphism(descriptor)
        return if (options.mode == KToonStreamingMode.FAST && descriptor.kind == StructureKind.LIST) {
            FastKnownSizeListEncoder(
                sink = sink,
                key = null,
                depth = 0,
                size = collectionSize,
                options = options,
                serializersModule = serializersModule
            )
        } else {
            beginStructure(descriptor)
        }
    }

    /**
     * Writes a root null literal.
     */
    override fun encodeNull() {
        builder.append("null")
    }

    /**
     * Writes a root Boolean literal.
     *
     * @param value Boolean value
     */
    override fun encodeBoolean(value: Boolean) {
        builder.append(value)
    }

    /**
     * Writes a root Byte literal.
     *
     * @param value Byte value
     */
    override fun encodeByte(value: Byte) {
        builder.append(value)
    }

    /**
     * Writes a root Short literal.
     *
     * @param value Short value
     */
    override fun encodeShort(value: Short) {
        builder.append(value)
    }

    /**
     * Writes a root Int literal.
     *
     * @param value Int value
     */
    override fun encodeInt(value: Int) {
        builder.append(value)
    }

    /**
     * Writes a root Long literal.
     *
     * @param value Long value
     */
    override fun encodeLong(value: Long) {
        builder.append(value)
    }

    /**
     * Writes a root Float literal.
     *
     * @param value Float value
     */
    override fun encodeFloat(value: Float) {
        builder.append(value)
    }

    /**
     * Writes a root Double literal.
     *
     * @param value Double value
     */
    override fun encodeDouble(value: Double) {
        builder.append(value)
    }

    /**
     * Writes a root Char using TOON string escaping rules.
     *
     * @param value Char value
     */
    override fun encodeChar(value: Char) {
        builder.append(formatString(value.toString(), options, delimiter = ','))
    }

    /**
     * Writes a root String using TOON string escaping rules.
     *
     * @param value String value
     */
    override fun encodeString(value: String) {
        builder.append(formatString(value, options, delimiter = ','))
    }

    /**
     * Writes a root enum value using its serialized element name.
     *
     * @param enumDescriptor enum descriptor
     * @param index selected enum element index
     */
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        builder.append(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ObjectEncoder(
    private val sink: OutputSink,
    private val depth: Int,
    private val options: KToonStreamingOptions,
    override val serializersModule: SerializersModule
) : AbstractEncoder(), CompositeEncoder {

    private var currentName: String? = null

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentName = descriptor.getElementName(index)
        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        val key = requireCurrentName()
        return when (descriptor.kind) {
            StructureKind.LIST -> ListEncoder(sink, key, depth, options, serializersModule)
            StructureKind.MAP -> MapEncoder(sink, key, depth, options, serializersModule)
            else -> {
                sink.appendLine(depth, "${formatKey(key, options)}:")
                ObjectEncoder(sink, depth + 1, options, serializersModule)
            }
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        rejectPolymorphism(descriptor)
        val key = requireCurrentName()
        return if (options.mode == KToonStreamingMode.FAST && descriptor.kind == StructureKind.LIST) {
            FastKnownSizeListEncoder(
                sink = sink,
                key = key,
                depth = depth,
                size = collectionSize,
                options = options,
                serializersModule = serializersModule
            )
        } else {
            beginStructure(descriptor)
        }
    }

    override fun encodeNull() = appendProperty("null")
    override fun encodeBoolean(value: Boolean) = appendProperty(value.toString())
    override fun encodeByte(value: Byte) = appendProperty(value.toString())
    override fun encodeShort(value: Short) = appendProperty(value.toString())
    override fun encodeInt(value: Int) = appendProperty(value.toString())
    override fun encodeLong(value: Long) = appendProperty(value.toString())
    override fun encodeFloat(value: Float) = appendProperty(value.toString())
    override fun encodeDouble(value: Double) = appendProperty(value.toString())
    override fun encodeChar(value: Char) = appendProperty(formatString(value.toString(), options, delimiter = ','))
    override fun encodeString(value: String) = appendProperty(formatString(value, options, delimiter = ','))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        appendProperty(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendProperty(value: String) {
        sink.appendLine(depth, "${formatKey(requireCurrentName(), options)}: $value")
    }

    private fun requireCurrentName(): String {
        return currentName ?: throw KToonStreamingSerializationException(
            "Property value was encoded without an element index"
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ListEncoder(
    private val sink: OutputSink,
    private val key: String?,
    private val depth: Int,
    private val options: KToonStreamingOptions,
    override val serializersModule: SerializersModule,
    private val keyIsRendered: Boolean = false
) : AbstractEncoder(), CompositeEncoder {

    private val items = ArrayList<BufferedListItem>()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                val block = LineBufferSink(options.indentSize)
                items += BufferedListItem.RawBlock(block.lines)
                ListEncoder(block, null, 0, options, serializersModule)
            }

            StructureKind.MAP -> {
                val block = LineBufferSink(options.indentSize)
                items += BufferedListItem.RawBlock(block.lines)
                MapEncoder(block, null, 0, options, serializersModule)
            }

            else -> {
                if (options.mode == KToonStreamingMode.FAST) {
                    val block = LineBufferSink(options.indentSize)
                    items += BufferedListItem.RawBlock(block.lines)
                    ObjectEncoder(block, 0, options, serializersModule)
                } else {
                    val row = RowObjectEncoder(serializersModule, options)
                    items += row.item
                    row
                }
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        appendList()
    }

    override fun encodeNull() {
        items += BufferedListItem.Primitive("null")
    }

    override fun encodeBoolean(value: Boolean) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeByte(value: Byte) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeShort(value: Short) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeInt(value: Int) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeLong(value: Long) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeFloat(value: Float) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeDouble(value: Double) {
        items += BufferedListItem.Primitive(value.toString())
    }

    override fun encodeChar(value: Char) {
        items += BufferedListItem.Primitive(formatString(value.toString(), options, delimiter = ','))
    }

    override fun encodeString(value: String) {
        items += BufferedListItem.Primitive(formatString(value, options, delimiter = ','))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        items += BufferedListItem.Primitive(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendList() {
        val header = if (key == null) {
            "[${items.size}]"
        } else {
            "${if (keyIsRendered) key else formatKey(key, options)}[${items.size}]"
        }

        if (items.all { it is BufferedListItem.Primitive }) {
            sink.appendIndent(depth)
            sink.append("$header: ")
            appendPrimitiveListValues()
            sink.appendNewline()
            return
        }

        if (options.mode == KToonStreamingMode.COMPACT && canRenderTabular()) {
            appendTabularList(header)
            return
        }

        sink.appendLine(depth, "$header:")
        items.forEach { item ->
            when (item) {
                is BufferedListItem.Primitive -> sink.appendLine(depth + 1, "-: ${item.rendered}")
                is BufferedListItem.ObjectRow -> appendStructuredListItem(item.rawLines)
                is BufferedListItem.RawBlock -> appendStructuredListItem(item.lines)
            }
        }
    }

    private fun appendPrimitiveListValues() {
        items.forEachIndexed { index, item ->
            if (index > 0) sink.append(',')
            sink.append((item as BufferedListItem.Primitive).rendered)
        }
    }

    private fun canRenderTabular(): Boolean {
        if (items.isEmpty()) return false

        val first = items.first() as? BufferedListItem.ObjectRow ?: return false
        if (!first.compatibleWithTabular || first.fields.isEmpty()) return false

        val keys = first.fields.keys.toList()
        return items.all { item ->
            item is BufferedListItem.ObjectRow &&
                    item.compatibleWithTabular &&
                    item.fields.keys.toList() == keys
        }
    }

    private fun appendTabularList(header: String) {
        val first = items.first() as BufferedListItem.ObjectRow
        val fields = first.fields.keys.toList()

        sink.appendIndent(depth)
        sink.append(header)
        sink.append('{')
        fields.forEachIndexed { index, field ->
            if (index > 0) sink.append(',')
            sink.append(formatKey(field, options))
        }
        sink.appendLineSuffix("}:")

        items.forEach { item ->
            val row = item as BufferedListItem.ObjectRow
            sink.appendIndent(depth + 1)
            fields.forEachIndexed { index, field ->
                if (index > 0) sink.append(',')
                sink.append(
                    row.fields[field]
                        ?: throw KToonStreamingSerializationException("Missing tabular field '$field'")
                )
            }
            sink.appendNewline()
        }
    }

    private fun appendStructuredListItem(lines: List<String>) {
        if (lines.isEmpty()) {
            sink.appendLine(depth + 1, "-:")
            return
        }

        sink.appendIndent(depth + 1)
        sink.append("- ")
        sink.append(lines.first())
        sink.appendNewline()

        for (index in 1 until lines.size) {
            sink.appendIndent(depth + 2)
            sink.append(lines[index])
            sink.appendNewline()
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class FastKnownSizeListEncoder(
    private val sink: OutputSink,
    private val key: String?,
    private val depth: Int,
    private val size: Int,
    private val options: KToonStreamingOptions,
    override val serializersModule: SerializersModule,
    private val keyIsRendered: Boolean = false,
    private var headerWritten: Boolean = false
) : AbstractEncoder(), CompositeEncoder {

    private val primitiveItems = ArrayList<String>()
    private var fastTabularFieldCount = -1

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                appendBlockHeaderIfNeeded()
                val block = LineBufferSink(options.indentSize)
                FastKnownSizeListEncoder(
                    sink = block,
                    key = null,
                    depth = 0,
                    size = 0,
                    options = options,
                    serializersModule = serializersModule
                )
            }

            StructureKind.MAP -> {
                appendBlockHeaderIfNeeded()
                val block = LineBufferSink(options.indentSize)
                MapEncoder(block, null, 0, options, serializersModule)
            }

            else -> {
                if (size >= FAST_TABULAR_MIN_SIZE && canUseFastFlatTabular(descriptor)) {
                    appendFastTabularHeaderIfNeeded(descriptor)
                    FastTabularRowEncoder(sink, depth + 1, options, descriptor.elementsCount)
                } else {
                    appendBlockHeaderIfNeeded()
                    FastListObjectEncoder(sink, depth + 1, options, serializersModule)
                }
            }
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        rejectPolymorphism(descriptor)
        appendBlockHeaderIfNeeded()
        return FastKnownSizeListEncoder(
            sink = sink,
            key = null,
            depth = depth + 1,
            size = collectionSize,
            options = options,
            serializersModule = serializersModule,
            headerWritten = false
        )
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!headerWritten) {
            appendInlinePrimitiveList()
        }
    }

    override fun encodeNull() = appendPrimitive("null")
    override fun encodeBoolean(value: Boolean) = appendPrimitive(value.toString())
    override fun encodeByte(value: Byte) = appendPrimitive(value.toString())
    override fun encodeShort(value: Short) = appendPrimitive(value.toString())
    override fun encodeInt(value: Int) = appendPrimitive(value.toString())
    override fun encodeLong(value: Long) = appendPrimitive(value.toString())
    override fun encodeFloat(value: Float) = appendPrimitive(value.toString())
    override fun encodeDouble(value: Double) = appendPrimitive(value.toString())
    override fun encodeChar(value: Char) = appendPrimitive(formatString(value.toString(), options, delimiter = ','))
    override fun encodeString(value: String) = appendPrimitive(formatString(value, options, delimiter = ','))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        appendPrimitive(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendPrimitive(rendered: String) {
        if (headerWritten) {
            sink.appendLine(depth + 1, "-: $rendered")
        } else {
            primitiveItems += rendered
        }
    }

    private fun appendBlockHeaderIfNeeded() {
        if (headerWritten) return
        appendPendingPrimitivesAsBlock()
        if (headerWritten) return
        sink.appendLine(depth, "${headerPrefix()}:")
        headerWritten = true
    }

    private fun appendPendingPrimitivesAsBlock() {
        if (primitiveItems.isEmpty()) return
        sink.appendLine(depth, "${headerPrefix()}:")
        primitiveItems.forEach { value ->
            sink.appendLine(depth + 1, "-: $value")
        }
        primitiveItems.clear()
        headerWritten = true
    }

    private fun appendInlinePrimitiveList() {
        sink.appendIndent(depth)
        sink.append(headerPrefix())
        sink.append(": ")
        primitiveItems.forEachIndexed { index, value ->
            if (index > 0) sink.append(',')
            sink.append(value)
        }
        sink.appendNewline()
    }

    private fun appendFastTabularHeaderIfNeeded(descriptor: SerialDescriptor) {
        if (headerWritten) return

        sink.appendIndent(depth)
        sink.append(headerPrefix())
        sink.append('{')
        for (index in 0 until descriptor.elementsCount) {
            if (index > 0) sink.append(',')
            sink.append(formatKey(descriptor.getElementName(index), options))
        }
        sink.appendLineSuffix("}:")
        fastTabularFieldCount = descriptor.elementsCount
        headerWritten = true
    }

    private fun headerPrefix(): String {
        return if (key == null) {
            "[$size]"
        } else {
            "${if (keyIsRendered) key else formatKey(key, options)}[$size]"
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class FastTabularRowEncoder(
    private val sink: OutputSink,
    private val depth: Int,
    private val options: KToonStreamingOptions,
    private val fieldCount: Int
) : AbstractEncoder(), CompositeEncoder {

    private var emitted = 0

    override val serializersModule: SerializersModule = SerializersModule {}

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        throw KToonStreamingSerializationException("Nested structures are not supported in FAST tabular rows")
    }

    override fun encodeNull() = appendValue("null")
    override fun encodeBoolean(value: Boolean) = appendValue(value.toString())
    override fun encodeByte(value: Byte) = appendValue(value.toString())
    override fun encodeShort(value: Short) = appendValue(value.toString())
    override fun encodeInt(value: Int) = appendValue(value.toString())
    override fun encodeLong(value: Long) = appendValue(value.toString())
    override fun encodeFloat(value: Float) = appendValue(value.toString())
    override fun encodeDouble(value: Double) = appendValue(value.toString())
    override fun encodeChar(value: Char) = appendValue(formatString(value.toString(), options, delimiter = ','))
    override fun encodeString(value: String) = appendValue(formatString(value, options, delimiter = ','))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        appendValue(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendValue(value: String) {
        if (emitted == 0) {
            sink.appendIndent(depth)
        } else {
            sink.append(',')
        }
        sink.append(value)
        emitted++
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (emitted != fieldCount) {
            throw KToonStreamingSerializationException(
                "FAST tabular row expected $fieldCount fields but encoded $emitted"
            )
        }
        sink.appendNewline()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class FastListObjectEncoder(
    private val sink: OutputSink,
    private val itemDepth: Int,
    private val options: KToonStreamingOptions,
    override val serializersModule: SerializersModule
) : AbstractEncoder(), CompositeEncoder {

    private var currentName: String? = null
    private var fieldCount = 0

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentName = descriptor.getElementName(index)
        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        val key = requireCurrentName()
        appendFieldPrefix(key)
        sink.appendLineSuffix(":")
        return when (descriptor.kind) {
            StructureKind.LIST -> ListEncoder(sink, null, itemDepth + 1, options, serializersModule)
            StructureKind.MAP -> MapEncoder(sink, null, itemDepth + 1, options, serializersModule)
            else -> ObjectEncoder(sink, itemDepth + 2, options, serializersModule)
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        rejectPolymorphism(descriptor)
        val key = requireCurrentName()
        if (fieldCount == 0) {
            appendFieldPrefix("${formatKey(key, options)}[$collectionSize]")
            sink.appendLineSuffix(":")
            return FastKnownSizeListEncoder(
                sink = sink,
                key = null,
                depth = itemDepth + 1,
                size = collectionSize,
                options = options,
                serializersModule = serializersModule,
                headerWritten = true
            )
        }

        fieldCount++
        return FastKnownSizeListEncoder(
            sink = sink,
            key = key,
            depth = itemDepth + 1,
            size = collectionSize,
            options = options,
            serializersModule = serializersModule
        )
    }

    override fun encodeNull() = appendPrimitive("null")
    override fun encodeBoolean(value: Boolean) = appendPrimitive(value.toString())
    override fun encodeByte(value: Byte) = appendPrimitive(value.toString())
    override fun encodeShort(value: Short) = appendPrimitive(value.toString())
    override fun encodeInt(value: Int) = appendPrimitive(value.toString())
    override fun encodeLong(value: Long) = appendPrimitive(value.toString())
    override fun encodeFloat(value: Float) = appendPrimitive(value.toString())
    override fun encodeDouble(value: Double) = appendPrimitive(value.toString())
    override fun encodeChar(value: Char) = appendPrimitive(formatString(value.toString(), options, delimiter = ','))
    override fun encodeString(value: String) = appendPrimitive(formatString(value, options, delimiter = ','))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        appendPrimitive(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendPrimitive(rendered: String) {
        appendFieldPrefix(formatKey(requireCurrentName(), options))
        sink.append(": ")
        sink.append(rendered)
        sink.appendNewline()
    }

    private fun appendFieldPrefix(renderedKey: String) {
        if (fieldCount == 0) {
            sink.appendIndent(itemDepth)
            sink.append("- ")
        } else {
            sink.appendIndent(itemDepth + 1)
        }
        sink.append(renderedKey)
        fieldCount++
    }

    private fun requireCurrentName(): String {
        return currentName ?: throw KToonStreamingSerializationException(
            "Property value was encoded without an element index"
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class RowObjectEncoder(
    override val serializersModule: SerializersModule,
    private val options: KToonStreamingOptions
) : AbstractEncoder(), CompositeEncoder {

    val item = BufferedListItem.ObjectRow()

    private var rawSink: LineBufferSink? = if (options.mode == KToonStreamingMode.FAST) {
        LineBufferSink(options.indentSize)
    } else {
        null
    }
    private var currentName: String? = null

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentName = descriptor.getElementName(index)
        return true
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        item.compatibleWithTabular = false
        val key = requireCurrentName()
        val sink = ensureRawSink()
        return when (descriptor.kind) {
            StructureKind.LIST -> ListEncoder(sink, key, 0, options, serializersModule)
            StructureKind.MAP -> MapEncoder(sink, key, 0, options, serializersModule)
            else -> {
                sink.appendLine(0, "${formatKey(key, options)}:")
                ObjectEncoder(sink, 1, options, serializersModule)
            }
        }
    }

    override fun encodeNull() = appendPrimitive("null")
    override fun encodeBoolean(value: Boolean) = appendPrimitive(value.toString())
    override fun encodeByte(value: Byte) = appendPrimitive(value.toString())
    override fun encodeShort(value: Short) = appendPrimitive(value.toString())
    override fun encodeInt(value: Int) = appendPrimitive(value.toString())
    override fun encodeLong(value: Long) = appendPrimitive(value.toString())
    override fun encodeFloat(value: Float) = appendPrimitive(value.toString())
    override fun encodeDouble(value: Double) = appendPrimitive(value.toString())
    override fun encodeChar(value: Char) = appendPrimitive(formatString(value.toString(), options, delimiter = ','))
    override fun encodeString(value: String) = appendPrimitive(formatString(value, options, delimiter = ','))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        appendPrimitive(formatString(enumDescriptor.getElementName(index), options, delimiter = ','))
    }

    private fun appendPrimitive(rendered: String) {
        val key = requireCurrentName()
        item.fields[key] = rendered
        rawSink?.appendLine(0, "${formatKey(key, options)}: $rendered")
    }

    private fun requireCurrentName(): String {
        return currentName ?: throw KToonStreamingSerializationException(
            "Property value was encoded without an element index"
        )
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        item.rawLines = rawSink?.lines ?: emptyList()
    }

    private fun ensureRawSink(): LineBufferSink {
        rawSink?.let { return it }

        val sink = LineBufferSink(options.indentSize)
        item.fields.forEach { (key, value) ->
            sink.appendLine(0, "${formatKey(key, options)}: $value")
        }
        rawSink = sink
        return sink
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class MapEncoder(
    private val sink: OutputSink,
    private val key: String?,
    private val depth: Int,
    private val options: KToonStreamingOptions,
    override val serializersModule: SerializersModule,
    private val keyIsRendered: Boolean = false
) : AbstractEncoder(), CompositeEncoder {

    private var expectingKey = true
    private var pendingKey: RenderedMapKey? = null
    private val mapDepth = if (key == null) depth else depth + 1
    private var headerWritten = false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        expectingKey = index % 2 == 0
        appendHeaderIfNeeded()
        return true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (pendingKey != null) {
            throw KToonStreamingSerializationException("Map key was encoded without a matching value")
        }
        appendHeaderIfNeeded()
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        rejectPolymorphism(descriptor)
        if (expectingKey) {
            throw KToonStreamingSerializationException("Structured TOON map keys are not supported")
        }

        val entryKey = takePendingKey()
        return when (descriptor.kind) {
            StructureKind.LIST -> ListEncoder(sink, entryKey.rendered, mapDepth, options, serializersModule, keyIsRendered = true)
            StructureKind.MAP -> MapEncoder(sink, entryKey.rendered, mapDepth, options, serializersModule, keyIsRendered = true)
            else -> {
                sink.appendLine(mapDepth, "${entryKey.rendered}:")
                ObjectEncoder(sink, mapDepth + 1, options, serializersModule)
            }
        }
    }

    override fun encodeNull() {
        if (expectingKey) {
            throw KToonStreamingSerializationException("TOON map keys must be primitive and non-null")
        }
        appendMapValue("null")
    }

    override fun encodeBoolean(value: Boolean) = putPrimitive(value.toString())
    override fun encodeByte(value: Byte) = putPrimitive(value.toString())
    override fun encodeShort(value: Short) = putPrimitive(value.toString())
    override fun encodeInt(value: Int) = putPrimitive(value.toString())
    override fun encodeLong(value: Long) = putPrimitive(value.toString())
    override fun encodeFloat(value: Float) = putPrimitive(value.toString())
    override fun encodeDouble(value: Double) = putPrimitive(value.toString())

    override fun encodeChar(value: Char) {
        if (expectingKey) pendingKey = RenderedMapKey(formatKey(value.toString(), options))
        else appendMapValue(formatString(value.toString(), options, delimiter = ','))
    }

    override fun encodeString(value: String) {
        if (expectingKey) pendingKey = RenderedMapKey(formatKey(value, options))
        else appendMapValue(formatString(value, options, delimiter = ','))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val value = enumDescriptor.getElementName(index)
        if (expectingKey) pendingKey = RenderedMapKey(formatKey(value, options))
        else appendMapValue(formatString(value, options, delimiter = ','))
    }

    private fun putPrimitive(raw: String) {
        if (expectingKey) {
            pendingKey = RenderedMapKey(raw)
            return
        }
        appendMapValue(raw)
    }

    private fun appendMapValue(value: String) {
        val entryKey = takePendingKey()
        sink.appendLine(mapDepth, "${entryKey.rendered}: $value")
    }

    private fun takePendingKey(): RenderedMapKey {
        return pendingKey?.also { pendingKey = null }
            ?: throw KToonStreamingSerializationException("Map value was encoded before its key")
    }

    private fun appendHeaderIfNeeded() {
        if (!headerWritten && key != null) {
            val renderedKey = if (keyIsRendered) key else formatKey(key, options)
            sink.appendLine(depth, "$renderedKey:")
            headerWritten = true
        }
    }
}

private sealed interface BufferedListItem {
    data class Primitive(val rendered: String) : BufferedListItem

    data class ObjectRow(
        val fields: LinkedHashMap<String, String> = linkedMapOf(),
        var compatibleWithTabular: Boolean = true,
        var rawLines: List<String> = emptyList()
    ) : BufferedListItem

    data class RawBlock(val lines: List<String>) : BufferedListItem
}

private data class RenderedMapKey(val rendered: String)

/**
 * Caches indentation strings per depth to avoid repeated String.repeat calls
 * while rendering large object arrays.
 */
private class IndentCache(private val indentSize: Int) {
    private val values = ArrayList<String>().apply { add("") }

    fun indent(depth: Int): String {
        while (depth >= values.size) {
            values += " ".repeat(values.size * indentSize)
        }
        return values[depth]
    }
}

private interface OutputSink {
    fun append(value: String)
    fun append(value: Char)
    fun appendIndent(depth: Int)
    fun appendNewline()

    fun appendLine(depth: Int, content: String) {
        appendIndent(depth)
        append(content)
        appendNewline()
    }

    fun appendLineSuffix(content: String) {
        append(content)
        appendNewline()
    }
}

private class StringBuilderSink(
    private val builder: StringBuilder,
    private val indentCache: IndentCache
) : OutputSink {
    override fun append(value: String) {
        builder.append(value)
    }

    override fun append(value: Char) {
        builder.append(value)
    }

    override fun appendIndent(depth: Int) {
        builder.append(indentCache.indent(depth))
    }

    override fun appendNewline() {
        builder.append('\n')
    }
}

/**
 * Lightweight fallback buffer for nested list items. It stores already-rendered
 * relative lines, so block fallback does not need trim, split, or parse passes.
 */
private class LineBufferSink(private val indentSize: Int) : OutputSink {
    val lines = ArrayList<String>()
    private val current = StringBuilder()

    override fun append(value: String) {
        current.append(value)
    }

    override fun append(value: Char) {
        current.append(value)
    }

    override fun appendIndent(depth: Int) {
        repeat(depth) {
            repeat(indentSize) {
                current.append(' ')
            }
        }
    }

    override fun appendNewline() {
        lines += current.toString()
        current.clear()
    }
}

private fun formatKey(value: String, options: KToonStreamingOptions): String {
    if (!options.quoteStrings && isUnquotedSimpleKey(value)) return value
    return formatString(value, options, delimiter = ',')
}

private fun canUseFastFlatTabular(descriptor: SerialDescriptor): Boolean {
    if (descriptor.elementsCount == 0) return false

    for (index in 0 until descriptor.elementsCount) {
        when (descriptor.getElementDescriptor(index).kind) {
            PrimitiveKind.BOOLEAN,
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            PrimitiveKind.CHAR,
            PrimitiveKind.STRING -> Unit

            else -> return false
        }
    }

    return true
}

private fun isUnquotedSimpleKey(value: String): Boolean {
    if (value.isEmpty()) return false

    return value.all { char ->
        char.isLetterOrDigit() || char == '_' || char == '-' || char == '.'
    } && value != "true" &&
            value != "false" &&
            value != "null" &&
            !NUMBER_REGEX.matches(value) &&
            !VERSION_LIKE_REGEX.matches(value)
}

private fun formatString(value: String, options: KToonStreamingOptions, delimiter: Char): String {
    return if (options.quoteStrings || needsQuoting(value, delimiter)) quote(value) else value
}

private fun needsQuoting(value: String, delimiter: Char): Boolean {
    if (value.isEmpty()) return true
    if (value.first().isWhitespace() || value.last().isWhitespace()) return true
    if (value == "true" || value == "false" || value == "null") return true
    if (value == "-" || (value.startsWith("-") && value.length > 1)) return true
    if (NUMBER_REGEX.matches(value)) return true
    if (VERSION_LIKE_REGEX.matches(value)) return true

    return value.any { char ->
        char == ':' ||
                char == '"' ||
                char == '\\' ||
                char == '[' ||
                char == ']' ||
                char == '{' ||
                char == '}' ||
                char == delimiter ||
                char == '\n' ||
                char == '\r' ||
                char == '\t' ||
                char.isISOControl()
    }
}

private fun quote(value: String): String {
    val builder = StringBuilder(value.length + 2)
    builder.append('"')

    value.forEach { char ->
        when (char) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> builder.append(char)
        }
    }

    builder.append('"')
    return builder.toString()
}

@OptIn(ExperimentalSerializationApi::class)
private fun rejectPolymorphism(descriptor: SerialDescriptor) {
    if (descriptor.kind is PolymorphicKind) {
        throw UnsupportedOperationException(
            "Polymorphic streaming encoding is not supported yet. Use KToonNativeFormat for polymorphic values."
        )
    }
}

private val NUMBER_REGEX = Regex(
    pattern = """-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?"""
)

private val VERSION_LIKE_REGEX = Regex(
    pattern = """\d+(?:\.\d+){2,}"""
)

private const val FAST_TABULAR_MIN_SIZE = 8

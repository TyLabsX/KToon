package de.tylabsx.ktoon

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Native kotlinx.serialization decoder that reads directly from the KToon
 * [ToonValue] model.
 *
 * The decoder consumes [ToonObject], [ToonArray], and primitive TOON values
 * without converting the tree to JSON first. It is the decoding counterpart to
 * [KToonSerializationEncoder] and is used by [KToonNativeFormat].
 *
 * @property value source ToonValue tree
 * @property serializersModule serializers module used for contextual and
 * polymorphic serializers
 * @since 1.0.0
 * @author TyLabsX
 */
@OptIn(ExperimentalSerializationApi::class)
class KToonSerializationDecoder(
    private val value: ToonValue,
    override val serializersModule: SerializersModule = SerializersModule {}
) : AbstractDecoder() {

    /**
     * Root primitive decoding does not expose element indexes.
     *
     * @param descriptor descriptor requested by kotlinx.serialization
     * @return never returns
     * @throws SerializationException always, because root indexes require a
     * composite decoder
     */
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        throw SerializationException("Root TOON decoder cannot decode structural element indexes")
    }

    /**
     * Reports whether the root value is not TOON null.
     *
     * @return true when the root value is not [ToonNull]
     */
    override fun decodeNotNullMark(): Boolean = value !is ToonNull

    /**
     * Decodes a root null value.
     *
     * @return null
     */
    override fun decodeNull(): Nothing? = null

    /**
     * Decodes the root value as Boolean.
     *
     * @return Boolean value
     */
    override fun decodeBoolean(): Boolean = value.asBoolean()

    /**
     * Decodes the root value as Byte.
     *
     * @return Byte value
     */
    override fun decodeByte(): Byte = value.asNumber().toByte()

    /**
     * Decodes the root value as Short.
     *
     * @return Short value
     */
    override fun decodeShort(): Short = value.asNumber().toShort()

    /**
     * Decodes the root value as Int.
     *
     * @return Int value
     */
    override fun decodeInt(): Int = value.asNumber().toInt()

    /**
     * Decodes the root value as Long.
     *
     * @return Long value
     */
    override fun decodeLong(): Long = value.asNumber().toLong()

    /**
     * Decodes the root value as Float.
     *
     * @return Float value
     */
    override fun decodeFloat(): Float = value.asNumber().toFloat()

    /**
     * Decodes the root value as Double.
     *
     * @return Double value
     */
    override fun decodeDouble(): Double = value.asNumber().toDouble()

    /**
     * Decodes the root value as Char.
     *
     * @return Char value
     * @throws SerializationException if the source string does not contain
     * exactly one character
     */
    override fun decodeChar(): Char {
        val string = value.asString()
        if (string.length != 1) {
            throw SerializationException("Cannot decode '$string' as Char")
        }

        return string.first()
    }

    /**
     * Decodes the root value as String.
     *
     * @return String value
     */
    override fun decodeString(): String = value.asString()

    /**
     * Decodes the root value as an enum element index.
     *
     * @param enumDescriptor enum descriptor
     * @return matching enum element index
     * @throws SerializationException if the encoded name is not part of the
     * enum descriptor
     */
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = value.asString()
        val index = (0 until enumDescriptor.elementsCount)
            .firstOrNull { enumDescriptor.getElementName(it) == name }

        return index ?: throw SerializationException("Unknown enum constant '$name'")
    }

    /**
     * Begins decoding a structured root value.
     *
     * @param descriptor descriptor of the structured value
     * @return composite decoder for the structure kind
     */
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListDecoder(value, serializersModule)
            StructureKind.MAP -> KToonMapDecoder(value, serializersModule)
            else -> KToonObjectDecoder(value, serializersModule)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonObjectDecoder(
    private val value: ToonValue,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {

    private val entries = (value as? ToonObject)?.entries
        ?: throw SerializationException("Expected TOON object but found $value")

    private var nextIndex = 0
    private var currentIndex = CompositeDecoder.UNKNOWN_NAME
    private var currentDescriptor: SerialDescriptor? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        currentDescriptor = descriptor

        while (nextIndex < descriptor.elementsCount) {
            val candidate = nextIndex++
            val name = descriptor.getElementName(candidate)
            if (entries.containsKey(name)) {
                currentIndex = candidate
                return candidate
            }
        }

        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeNotNullMark(): Boolean = currentValue() !is ToonNull

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = currentValue().asBoolean()

    override fun decodeByte(): Byte = currentValue().asNumber().toByte()

    override fun decodeShort(): Short = currentValue().asNumber().toShort()

    override fun decodeInt(): Int = currentValue().asNumber().toInt()

    override fun decodeLong(): Long = currentValue().asNumber().toLong()

    override fun decodeFloat(): Float = currentValue().asNumber().toFloat()

    override fun decodeDouble(): Double = currentValue().asNumber().toDouble()

    override fun decodeChar(): Char {
        val string = currentValue().asString()
        if (string.length != 1) {
            throw SerializationException("Cannot decode '$string' as Char")
        }

        return string.first()
    }

    override fun decodeString(): String = currentValue().asString()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = currentValue().asString()
        val index = (0 until enumDescriptor.elementsCount)
            .firstOrNull { enumDescriptor.getElementName(it) == name }

        return index ?: throw SerializationException("Unknown enum constant '$name'")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val child = currentValue()
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListDecoder(child, serializersModule)
            StructureKind.MAP -> KToonMapDecoder(child, serializersModule)
            else -> KToonObjectDecoder(child, serializersModule)
        }
    }

    private fun currentValue(): ToonValue {
        val index = currentIndex
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw SerializationException("Decoder does not point at an object element")
        }

        val name = currentDescriptorName(index)
        return entries[name] ?: throw SerializationException("Missing TOON object property '$name'")
    }

    private fun currentDescriptorName(index: Int): String {
        return currentDescriptor?.getElementName(index)
            ?: throw SerializationException("Object decoder is missing an active descriptor")
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonListDecoder(
    value: ToonValue,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {

    private val values = (value as? ToonArray)?.values
        ?: throw SerializationException("Expected TOON array but found $value")

    private var currentIndex = -1

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val next = currentIndex + 1
        if (next >= values.size) {
            return CompositeDecoder.DECODE_DONE
        }

        currentIndex = next
        return next
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = values.size

    override fun decodeNotNullMark(): Boolean = currentValue() !is ToonNull

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = currentValue().asBoolean()

    override fun decodeByte(): Byte = currentValue().asNumber().toByte()

    override fun decodeShort(): Short = currentValue().asNumber().toShort()

    override fun decodeInt(): Int = currentValue().asNumber().toInt()

    override fun decodeLong(): Long = currentValue().asNumber().toLong()

    override fun decodeFloat(): Float = currentValue().asNumber().toFloat()

    override fun decodeDouble(): Double = currentValue().asNumber().toDouble()

    override fun decodeChar(): Char {
        val string = currentValue().asString()
        if (string.length != 1) {
            throw SerializationException("Cannot decode '$string' as Char")
        }

        return string.first()
    }

    override fun decodeString(): String = currentValue().asString()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = currentValue().asString()
        val index = (0 until enumDescriptor.elementsCount)
            .firstOrNull { enumDescriptor.getElementName(it) == name }

        return index ?: throw SerializationException("Unknown enum constant '$name'")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val child = currentValue()
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListDecoder(child, serializersModule)
            StructureKind.MAP -> KToonMapDecoder(child, serializersModule)
            else -> KToonObjectDecoder(child, serializersModule)
        }
    }

    private fun currentValue(): ToonValue {
        if (currentIndex !in values.indices) {
            throw SerializationException("Decoder does not point at a list element")
        }

        return values[currentIndex]
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonMapDecoder(
    value: ToonValue,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {

    private val entries = (value as? ToonObject)?.entries?.entries?.toList()
        ?: throw SerializationException("Expected TOON object for map but found $value")

    private var currentIndex = -1

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val next = currentIndex + 1
        if (next >= entries.size * 2) {
            return CompositeDecoder.DECODE_DONE
        }

        currentIndex = next
        return next
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = entries.size * 2

    override fun decodeNotNullMark(): Boolean = currentValue() !is ToonNull

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = currentValue().asBoolean()

    override fun decodeByte(): Byte = currentValue().asNumber().toByte()

    override fun decodeShort(): Short = currentValue().asNumber().toShort()

    override fun decodeInt(): Int = currentValue().asNumber().toInt()

    override fun decodeLong(): Long = currentValue().asNumber().toLong()

    override fun decodeFloat(): Float = currentValue().asNumber().toFloat()

    override fun decodeDouble(): Double = currentValue().asNumber().toDouble()

    override fun decodeChar(): Char {
        val string = currentValue().asString()
        if (string.length != 1) {
            throw SerializationException("Cannot decode '$string' as Char")
        }

        return string.first()
    }

    override fun decodeString(): String = currentValue().asString()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = currentValue().asString()
        val index = (0 until enumDescriptor.elementsCount)
            .firstOrNull { enumDescriptor.getElementName(it) == name }

        return index ?: throw SerializationException("Unknown enum constant '$name'")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val child = currentValue()
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListDecoder(child, serializersModule)
            StructureKind.MAP -> KToonMapDecoder(child, serializersModule)
            else -> KToonObjectDecoder(child, serializersModule)
        }
    }

    private fun currentValue(): ToonValue {
        if (currentIndex < 0 || currentIndex >= entries.size * 2) {
            throw SerializationException("Decoder does not point at a map element")
        }

        val entry = entries[currentIndex / 2]
        return if (currentIndex % 2 == 0) {
            ToonString(entry.key)
        } else {
            entry.value
        }
    }
}

private fun ToonValue.asString(): String {
    return when (this) {
        is ToonString -> value
        is ToonNumber -> raw
        is ToonBoolean -> value.toString()
        ToonNull -> throw SerializationException("Cannot decode null as String")
        is ToonArray,
        is ToonObject -> throw SerializationException("Cannot decode $this as String")
    }
}

private fun ToonValue.asNumber(): String {
    return when (this) {
        is ToonNumber -> raw
        is ToonString -> value
        else -> throw SerializationException("Cannot decode $this as Number")
    }
}

private fun ToonValue.asBoolean(): Boolean {
    return when (this) {
        is ToonBoolean -> value
        is ToonString -> value.toBooleanStrict()
        else -> throw SerializationException("Cannot decode $this as Boolean")
    }
}

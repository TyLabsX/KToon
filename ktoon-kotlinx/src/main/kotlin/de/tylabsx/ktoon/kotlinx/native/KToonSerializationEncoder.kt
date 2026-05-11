package de.tylabsx.ktoon.kotlinx.native

import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonBoolean
import de.tylabsx.ktoon.ToonNull
import de.tylabsx.ktoon.ToonNumber
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonString
import de.tylabsx.ktoon.ToonValue

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Native kotlinx.serialization encoder that writes directly into the KToon
 * [ToonValue] model.
 *
 * The encoder does not route through JSON. It receives serialization events from
 * kotlinx.serialization and builds the corresponding TOON tree using
 * [ToonObject], [ToonArray], and the primitive [ToonValue] implementations.
 *
 * This class is used by [KToonNativeFormat] and is public for advanced
 * integrations that need direct access to the intermediate [ToonValue] tree.
 *
 * @property serializersModule serializers module used for contextual and
 * polymorphic serializers
 * @since 1.0.0
 * @author TyLabsX
 */
@OptIn(ExperimentalSerializationApi::class)
class KToonSerializationEncoder(
    override val serializersModule: SerializersModule = SerializersModule {},
    private val emit: (ToonValue) -> Unit
) : AbstractEncoder() {

    /**
     * Creates a root encoder and returns the encoded [ToonValue].
     */
    companion object {
        /**
         * Encodes a value by executing a serialization block against a root
         * [KToonSerializationEncoder].
         *
         * The block is expected to call one of the standard kotlinx
         * serialization entry points, such as `encodeSerializableValue`. The
         * emitted root value is returned as a [ToonValue].
         *
         * @param serializersModule serializers module used by the encoder
         * @param block serialization block that writes into the encoder
         * @return encoded ToonValue tree
         * @throws SerializationException if the block does not emit a root value
         */
        fun encode(
            serializersModule: SerializersModule = SerializersModule {},
            block: KToonSerializationEncoder.() -> Unit
        ): ToonValue {
            var result: ToonValue? = null
            val encoder = KToonSerializationEncoder(serializersModule) { value ->
                result = value
            }

            encoder.block()

            return result ?: throw SerializationException("Serializer did not emit a TOON value")
        }
    }

    /**
     * Encodes a null root value.
     */
    override fun encodeNull() {
        emit(ToonNull)
    }

    /**
     * Encodes a Boolean root value.
     *
     * @param value Boolean value
     */
    override fun encodeBoolean(value: Boolean) {
        emit(ToonBoolean(value))
    }

    /**
     * Encodes a Byte root value as a TOON number.
     *
     * @param value Byte value
     */
    override fun encodeByte(value: Byte) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes a Short root value as a TOON number.
     *
     * @param value Short value
     */
    override fun encodeShort(value: Short) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes an Int root value as a TOON number.
     *
     * @param value Int value
     */
    override fun encodeInt(value: Int) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes a Long root value as a TOON number.
     *
     * @param value Long value
     */
    override fun encodeLong(value: Long) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes a Float root value as a TOON number.
     *
     * @param value Float value
     */
    override fun encodeFloat(value: Float) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes a Double root value as a TOON number.
     *
     * @param value Double value
     */
    override fun encodeDouble(value: Double) {
        emit(ToonNumber(value.toString()))
    }

    /**
     * Encodes a Char root value as a TOON string.
     *
     * @param value Char value
     */
    override fun encodeChar(value: Char) {
        emit(ToonString(value.toString()))
    }

    /**
     * Encodes a String root value.
     *
     * @param value String value
     */
    override fun encodeString(value: String) {
        emit(ToonString(value))
    }

    /**
     * Encodes an enum root value using the serialized enum element name.
     *
     * @param enumDescriptor enum descriptor
     * @param index selected enum element index
     */
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        emit(ToonString(enumDescriptor.getElementName(index)))
    }

    /**
     * Begins encoding a structured root value.
     *
     * @param descriptor descriptor of the structured value
     * @return composite encoder for the structure kind
     */
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListEncoder(serializersModule, emit)
            StructureKind.MAP -> KToonMapEncoder(serializersModule, emit)
            else -> KToonObjectEncoder(serializersModule, emit)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonObjectEncoder(
    override val serializersModule: SerializersModule,
    private val emit: (ToonValue) -> Unit
) : AbstractEncoder() {

    private val entries = linkedMapOf<String, ToonValue>()
    private var currentName: String? = null

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentName = descriptor.getElementName(index)
        return true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        emit(ToonObject(entries))
    }

    override fun encodeNull() {
        put(ToonNull)
    }

    override fun encodeBoolean(value: Boolean) {
        put(ToonBoolean(value))
    }

    override fun encodeByte(value: Byte) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeShort(value: Short) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeInt(value: Int) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeLong(value: Long) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeFloat(value: Float) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeDouble(value: Double) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeChar(value: Char) {
        put(ToonString(value.toString()))
    }

    override fun encodeString(value: String) {
        put(ToonString(value))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        put(ToonString(enumDescriptor.getElementName(index)))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val name = requireCurrentName()
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListEncoder(serializersModule) { entries[name] = it }
            StructureKind.MAP -> KToonMapEncoder(serializersModule) { entries[name] = it }
            else -> KToonObjectEncoder(serializersModule) { entries[name] = it }
        }
    }

    private fun put(value: ToonValue) {
        entries[requireCurrentName()] = value
    }

    private fun requireCurrentName(): String {
        return currentName ?: throw SerializationException("Property value was encoded without an element index")
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonListEncoder(
    override val serializersModule: SerializersModule,
    private val emit: (ToonValue) -> Unit
) : AbstractEncoder() {

    private val values = mutableListOf<ToonValue>()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun endStructure(descriptor: SerialDescriptor) {
        emit(ToonArray(values))
    }

    override fun encodeNull() {
        values += ToonNull
    }

    override fun encodeBoolean(value: Boolean) {
        values += ToonBoolean(value)
    }

    override fun encodeByte(value: Byte) {
        values += ToonNumber(value.toString())
    }

    override fun encodeShort(value: Short) {
        values += ToonNumber(value.toString())
    }

    override fun encodeInt(value: Int) {
        values += ToonNumber(value.toString())
    }

    override fun encodeLong(value: Long) {
        values += ToonNumber(value.toString())
    }

    override fun encodeFloat(value: Float) {
        values += ToonNumber(value.toString())
    }

    override fun encodeDouble(value: Double) {
        values += ToonNumber(value.toString())
    }

    override fun encodeChar(value: Char) {
        values += ToonString(value.toString())
    }

    override fun encodeString(value: String) {
        values += ToonString(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        values += ToonString(enumDescriptor.getElementName(index))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListEncoder(serializersModule) { values += it }
            StructureKind.MAP -> KToonMapEncoder(serializersModule) { values += it }
            else -> KToonObjectEncoder(serializersModule) { values += it }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KToonMapEncoder(
    override val serializersModule: SerializersModule,
    private val emit: (ToonValue) -> Unit
) : AbstractEncoder() {

    private val entries = linkedMapOf<String, ToonValue>()
    private var expectingKey = true
    private var pendingKey: String? = null

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        expectingKey = index % 2 == 0
        return true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (pendingKey != null) {
            throw SerializationException("Map key was encoded without a matching value")
        }

        emit(ToonObject(entries))
    }

    override fun encodeNull() {
        put(ToonNull)
    }

    override fun encodeBoolean(value: Boolean) {
        put(ToonBoolean(value))
    }

    override fun encodeByte(value: Byte) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeShort(value: Short) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeInt(value: Int) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeLong(value: Long) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeFloat(value: Float) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeDouble(value: Double) {
        put(ToonNumber(value.toString()))
    }

    override fun encodeChar(value: Char) {
        put(ToonString(value.toString()))
    }

    override fun encodeString(value: String) {
        put(ToonString(value))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        put(ToonString(enumDescriptor.getElementName(index)))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (expectingKey) {
            throw SerializationException("Structured TOON map keys are not supported")
        }

        val key = pendingKey ?: throw SerializationException("Map value was encoded before its key")
        pendingKey = null

        return when (descriptor.kind) {
            StructureKind.LIST -> KToonListEncoder(serializersModule) { entries[key] = it }
            StructureKind.MAP -> KToonMapEncoder(serializersModule) { entries[key] = it }
            else -> KToonObjectEncoder(serializersModule) { entries[key] = it }
        }
    }

    private fun put(value: ToonValue) {
        if (expectingKey) {
            pendingKey = mapKeyToString(value)
            return
        }

        val key = pendingKey ?: throw SerializationException("Map value was encoded before its key")
        pendingKey = null
        entries[key] = value
    }

    private fun mapKeyToString(value: ToonValue): String {
        return when (value) {
            is ToonString -> value.value
            is ToonNumber -> value.raw
            is ToonBoolean -> value.value.toString()
            ToonNull -> throw SerializationException("TOON map keys must not be null")
            is ToonArray,
            is ToonObject -> throw SerializationException("Structured TOON map keys are not supported")
        }
    }
}

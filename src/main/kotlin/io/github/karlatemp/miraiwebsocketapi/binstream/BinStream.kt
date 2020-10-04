/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/04 16:06:36
 *
 * mirai-websocket-api/BinStream.kt
 */

package io.github.karlatemp.miraiwebsocketapi.binstream

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.*
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalSerializationApi
object BinStream : BinaryFormat {
    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
        BinStreamDecoder(DataInputStream(ByteArrayInputStream(bytes))).decodeSerializableValue(deserializer)

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().also { baos ->
            BinStreamEncoder(DataOutputStream(baos)).encodeSerializableValue(serializer, value)
        }.toByteArray()

}

class BinStreamDecoder(val bin: DataInput) : Decoder, CompositeDecoder {
    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    @Suppress("OverridingDeprecatedMember", "DEPRECATION", "DEPRECATION_ERROR")
    override val updateMode: UpdateMode
        get() = UpdateMode.OVERWRITE
    private var index = AtomicInteger()

    @ExperimentalSerializationApi
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return BinStreamDecoder(bin)
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        return deserializer.deserialize(this)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        return if (decodeNotNullMark()) {
            deserializer.deserialize(this)
        } else null
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decodeBoolean()

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decodeByte()

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decodeChar()

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decodeDouble()

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return bin.readUnsignedShort()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val v = index.getAndIncrement()
        if (v < descriptor.elementsCount) return v
        return DECODE_DONE
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decodeFloat()

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeInt()

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decodeLong()

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decodeShort()

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decodeString()

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    override fun decodeBoolean(): Boolean = bin.readBoolean()

    override fun decodeByte(): Byte = bin.readByte()

    override fun decodeChar(): Char = bin.readChar()

    override fun decodeDouble(): Double = bin.readDouble()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = bin.readUnsignedShort()

    override fun decodeFloat(): Float = bin.readFloat()

    override fun decodeInt(): Int = bin.readInt()

    override fun decodeLong(): Long = bin.readLong()

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = bin.readBoolean()

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? = null

    override fun decodeShort(): Short = bin.readShort()

    override fun decodeString(): String = bin.readUTF()

}

class BinStreamEncoder(val bin: DataOutput) : Encoder, CompositeEncoder {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return this
    }

    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        bin.writeShort(collectionSize)
        return this
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) = encodeBoolean(value)

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) = encodeByte(value)

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) = encodeChar(value)

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) = encodeDouble(value)

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) = encodeFloat(value)

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) = encodeInt(value)

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) = encodeLong(value)

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value == null) {
            bin.writeBoolean(false)
        } else {
            bin.writeBoolean(true)
            encodeSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        serializer.serialize(this, value)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) = encodeShort(value)

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) = encodeString(value)

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    override fun encodeBoolean(value: Boolean) = bin.writeBoolean(value)

    override fun encodeByte(value: Byte) = bin.writeByte(value.toInt())

    override fun encodeChar(value: Char) = bin.writeChar(value.toInt())

    override fun encodeDouble(value: Double) = bin.writeDouble(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        bin.writeShort(index)
    }

    override fun encodeFloat(value: Float) = bin.writeFloat(value)

    override fun encodeInt(value: Int) = bin.writeInt(value)

    override fun encodeLong(value: Long) = bin.writeLong(value)

    @ExperimentalSerializationApi
    override fun encodeNull() {
    }

    override fun encodeShort(value: Short) {
        bin.writeShort(value.toInt())
    }

    override fun encodeString(value: String) {
        bin.writeUTF(value)
    }
}
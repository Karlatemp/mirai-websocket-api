/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/10 20:29:04
 *
 * mirai-websocket-api/Request.kt
 */

package io.github.karlatemp.miraiwebsocketapi.actions

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.AbstractPolymorphicSerializer

@Serializable(with = Request.RequestSerializer::class)
data class Request(
    val action: IncomingAction,
    val requestId: String?
) {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    public object RequestSerializer : KSerializer<Request> {
        private val serializerX = IncomingAction.serializer() as AbstractPolymorphicSerializer<IncomingAction>

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
            Request::class.java.name
        ) {
            element("type", String.serializer().descriptor)
            element("requestId", String.serializer().descriptor, isOptional = true)
            element(
                "content",
                buildSerialDescriptor(
                    "kotlinx.serialization.Sealed<${IncomingAction::class.java.name}>",
                    StructureKind.MAP
                ) {
                    this.isNullable = true
                })
        }

        override fun serialize(encoder: Encoder, value: Request) {
            val findPolymorphicSerializer = serializerX.findPolymorphicSerializer(encoder, value.action)
            val desp = findPolymorphicSerializer.descriptor
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, desp.serialName)
                encodeNullableString(descriptor, 1, value.requestId)
                encodeSerializableElement(descriptor, 2, findPolymorphicSerializer, value.action)
            }
        }

        override fun deserialize(decoder: Decoder): Request {
            var rid: String? = null
            var type: String? = null
            var action: IncomingAction? = null
            decoder.decodeStructure(descriptor) {
                rp@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> {
                            break@rp
                        }
                        0 -> {
                            type = decodeStringElement(descriptor, 0)
                        }
                        1 -> {
                            @Suppress("UNCHECKED_CAST")
                            rid = decodeNullableSerializableElement(
                                descriptor, 1,
                                String.serializer() as DeserializationStrategy<String?>,
                                rid
                            )
                        }
                        2 -> {
                            check(type != null) { "Cannot read polymorphic value before its type token" }
                            val serializer = serializerX.findPolymorphicSerializer(
                                this, type
                            )
                            action = decodeSerializableElement(descriptor, 2, serializer)
                        }
                        else ->
                            throw SerializationException("Invalid index in deserialization, got $index")
                    }
                }
            }
            return Request(requireNotNull(action) { "No msg given." }, rid)
        }
    }
}
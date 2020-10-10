/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/10 21:10:44
 *
 * mirai-websocket-api/Response.kt
 */

package io.github.karlatemp.miraiwebsocketapi.actions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object OutgoingSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "io.github.karlatemp.miraiwebsocketapi.OutGoing"
    ) {
        element("type", String.serializer().descriptor, isOptional = false)
        element("event", buildSerialDescriptor(
            "io.github.karlatemp.miraiwebsocketapi.OutGoing.C",
            SerialKind.CONTEXTUAL
        ) {})
    }

    override fun deserialize(decoder: Decoder): Any {
        return decoder.decodeStructure(descriptor) {
            if (decodeElementIndex(descriptor) != 0) {
                throw SerializationException("First key of Outgoing must be key")
            }
            when (decodeStringElement(descriptor, 0)) {
                "ActionResult" -> ActionResult.ActionResultSerializer.deserialize(this)
                "Event" -> {
                    if (decodeElementIndex(descriptor) != 1) {
                        throw SerializationException("Could not deserialize this event. Bad index.")
                    }
                    decodeSerializableElement(descriptor, 1, OutgoingAction.serializer())
                }
                else -> throw AssertionError()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeStructure(descriptor) {
            when (value) {
                is ActionResult -> {
                    encodeStringElement(descriptor, 0, "ActionResult")
                    ActionResult.ActionResultSerializer.serialize(this, value)
                }
                is OutgoingAction -> {
                    encodeStringElement(descriptor, 0, "Event")
                    encodeSerializableElement(descriptor, 1, OutgoingAction.serializer(), value)
                }
                else -> {
                    throw SerializationException("Unknown how to serialize $value")
                }
            }
        }
    }

}



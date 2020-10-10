/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/10 20:18:55
 *
 * mirai-websocket-api/ActionModel.kt
 */

package io.github.karlatemp.miraiwebsocketapi.actions

import io.github.karlatemp.miraiwebsocketapi.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonObject


@Serializable
sealed class IncomingAction {

    @Serializable
    @SerialName("Reply")
    data class ReplyMessage(
        val id: String,
        val message: MessageChainModel
    ) : IncomingAction()

    @SerialName("SendToGroup")
    @Serializable
    data class SendToGroup(
        val group: Long,
        val bot: Long,
        val message: MessageChainModel
    ) : IncomingAction()

    @SerialName("SendToFriend")
    @Serializable
    data class SendToFriend(
        val friend: Long,
        val bot: Long,
        val message: MessageChainModel
    ) : IncomingAction()

    @SerialName("RecallReceipt")
    @Serializable
    data class RecallReceipt(
        val receipt: String
    ) : IncomingAction()

    @SerialName("MuteMember")
    @Serializable
    data class MuteMember(
        val bot: Long,
        val group: Long,
        val member: Long,
        val time: Int
    ) : IncomingAction()

    @SerialName("Recall")
    @Serializable
    data class Recall(
        val messageSource: String
    ) : IncomingAction()

}

@Serializable(with = ActionResult.ActionResultSerializer::class)
data class ActionResult(
    val id: String?,
    val success: Boolean,
    val simpleError: String?,
    val fullError: String?,
    val content: JsonObject?
) {
    @Suppress("UNCHECKED_CAST")
    public object ActionResultSerializer : KSerializer<ActionResult> {
        object ActionResultDeclaredSerializer : SerializationStrategy<ActionResult> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ActionResult#declared") {
                element("success", Boolean.serializer().descriptor, isOptional = false)
                element("simpleError", String.serializer().descriptor, isOptional = true)
                element("fullError", String.serializer().descriptor, isOptional = true)
                element("content", JsonObject.serializer().descriptor, isOptional = true)
            }

            @OptIn(ExperimentalSerializationApi::class)
            override fun serialize(encoder: Encoder, value: ActionResult) {
                encoder.encodeStructure(descriptor) {
                    encodeBooleanElement(descriptor, 0, value.success)
                    encodeNullableString(descriptor, 1, value.simpleError)
                    encodeNullableString(descriptor, 2, value.fullError)
                    val content = value.content
                    if (content != null || shouldEncodeElementDefault(descriptor, 3)) {
                        encodeNullableSerializableElement(descriptor, 3, JsonObject.serializer(), content)
                    }
                }
            }

        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
            "ActionResult"
        ) {
            element("id", String.serializer().descriptor, isOptional = false)
            element("result", ActionResultDeclaredSerializer.descriptor, isOptional = false)
        }

        @ExperimentalSerializationApi
        fun serialize(encoder: CompositeEncoder, value: ActionResult) {
            encoder.run {
                encodeNullableString(descriptor, 0, value.id)
                encodeSerializableElement(
                    descriptor,
                    1,
                    ActionResultDeclaredSerializer,
                    value
                )
            }
        }

        @ExperimentalSerializationApi
        override fun serialize(encoder: Encoder, value: ActionResult) {
            encoder.encodeStructure(descriptor) { serialize(this, value) }
        }

        @ExperimentalSerializationApi
        fun deserialize(decoder: CompositeDecoder): ActionResult = decoder.run {
            var id: String? = null
            var result: ActionResult? = null
            lp1@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    -1 -> break@lp1
                    0 -> {
                        id = decodeNullableString(descriptor, 0, id)
                        if (result != null) {
                            val or = result!!
                            result = ActionResult(
                                id = id,
                                success = or.success,
                                fullError = or.fullError,
                                simpleError = or.simpleError,
                                content = or.content
                            )
                        }
                    }
                    1 -> {
                        result = decodeSerializableElement(
                            descriptor,
                            1,
                            object : DeserializationStrategy<ActionResult> {
                                override val descriptor: SerialDescriptor
                                    get() = ActionResultDeclaredSerializer.descriptor

                                override fun patch(decoder: Decoder, old: ActionResult): ActionResult {
                                    throw UnsupportedOperationException("Not implemented, should not be called")
                                }

                                override fun deserialize(decoder: Decoder): ActionResult {
                                    var success: Boolean? = null
                                    var simpleError: String? = null
                                    var fullError: String? = null
                                    var content: JsonObject? = null
                                    decoder.decodeStructure(descriptor) {
                                        rrp@ while (true) {
                                            when (val index2 = decodeElementIndex(descriptor)) {
                                                -1 -> break@rrp
                                                0 -> success = decodeBooleanElement(descriptor, index2)
                                                1 -> simpleError =
                                                    decodeNullableString(descriptor, index2, simpleError)
                                                2 -> fullError = decodeNullableString(descriptor, index2, fullError)
                                                3 -> content = decodeNullableSerializableElement(
                                                    descriptor,
                                                    index2,
                                                    JsonObject.serializer() as KSerializer<JsonObject?>,
                                                    content
                                                )
                                                else -> {
                                                    throw SerializationException("Invalid index in deserialization, got $index2")
                                                }
                                            }
                                        }
                                    }
                                    return ActionResult(
                                        id,
                                        requireNotNull(success) { "Result damage" },
                                        simpleError,
                                        fullError,
                                        content
                                    )
                                }
                            })
                    }
                    else -> {
                        throw SerializationException("Invalid index in deserialization, got $index")
                    }
                }
            }
            checkNotNull(result) { "Cannot deserialize ActionResult." }
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): ActionResult =
            decoder.decodeStructure(descriptor) { deserialize(this) }
    }
}

@Serializable
sealed class OutgoingAction(
    val isMessageEvent: Boolean = false
) {
    open val replyKey: String? get() = null
    open val message: MessageChainModel? get() = null
    open val sender: SenderModel? get() = null

    @Serializable
    @SerialName("GroupMessage")
    data class GroupMessage(
        val group: GroupModel,
        override val sender: MemberModel,
        override val message: MessageChainModel,
        override val replyKey: String,
        val bot: Long
    ) : OutgoingAction(
        isMessageEvent = true
    )

    @Serializable
    @SerialName("FriendMessage")
    data class FriendMessage(
        override val sender: FriendModel,
        override val message: MessageChainModel,
        override val replyKey: String,
        val bot: Long
    ) : OutgoingAction(
        isMessageEvent = true
    )

    @Serializable
    @SerialName("TempMessage")
    data class TempMessage(
        val group: GroupModel,
        override val sender: MemberModel,
        override val message: MessageChainModel,
        override val replyKey: String,
        val bot: Long
    ) : OutgoingAction(
        isMessageEvent = true
    )
}



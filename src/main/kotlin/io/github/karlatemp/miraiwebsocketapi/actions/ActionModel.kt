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

    @SerialName("ListGroups")
    @Serializable
    data class ListGroups(val bot: Long) : IncomingAction()

    @SerialName("ListFriends")
    @Serializable
    data class ListFriends(val bot: Long) : IncomingAction()

    @SerialName("GroupVerbose")
    @Serializable
    data class GroupVerbose(
        val bot: Long,
        val group: Long,
        val noMembers: Boolean = true
    ) : IncomingAction()

    @SerialName("VerboseMessageSource")
    @Serializable
    data class VerboseMessageSource(
        val messageSource: String
    ) : IncomingAction()
}

@Serializable(with = ActionResult.ActionResultSerializer::class)
data class ActionResult(
    val id: String?,
    val success: Boolean,
    val error: String?,
    val errorDetail: String?,
    val content: JsonObject?
) {
    @Suppress("UNCHECKED_CAST")
    public object ActionResultSerializer : KSerializer<ActionResult> {
        object ActionResultDeclaredSerializer : SerializationStrategy<ActionResult> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ActionResult#declared") {
                element("error", String.serializer().descriptor, isOptional = true)
                element("errorDetail", String.serializer().descriptor, isOptional = true)
                element("content", JsonObject.serializer().descriptor, isOptional = true)
            }

            @OptIn(ExperimentalSerializationApi::class)
            override fun serialize(encoder: Encoder, value: ActionResult) {
                encoder.encodeStructure(descriptor) {
                    encodeNullableString(descriptor, 0, value.error)
                    encodeNullableString(descriptor, 1, value.errorDetail)
                    val content = value.content
                    if (content != null || shouldEncodeElementDefault(descriptor, 2)) {
                        encodeNullableSerializableElement(descriptor, 2, JsonObject.serializer(), content)
                    }
                }
            }

        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
            "ActionResult"
        ) {
            element("id", String.serializer().descriptor, isOptional = false)
            element("success", Boolean.serializer().descriptor, isOptional = false)
            element("result", ActionResultDeclaredSerializer.descriptor, isOptional = false)
        }

        @ExperimentalSerializationApi
        fun serialize(encoder: CompositeEncoder, value: ActionResult) {
            encoder.run {
                encodeNullableString(descriptor, 0, value.id)
                encodeBooleanElement(descriptor, 1, value.success)
                encodeSerializableElement(
                    descriptor,
                    2,
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
            var success: Boolean? = null
            lp1@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    -1 -> break@lp1
                    1 -> {
                        success = decodeBooleanElement(descriptor, 1)
                        if (result != null) {
                            val or = result
                            result = ActionResult(
                                id = or.id,
                                success = success,
                                errorDetail = or.errorDetail,
                                error = or.error,
                                content = or.content
                            )
                        }
                    }
                    0 -> {
                        id = decodeNullableString(descriptor, 0, id)
                        if (result != null) {
                            val or = result
                            result = ActionResult(
                                id = id,
                                success = or.success,
                                errorDetail = or.errorDetail,
                                error = or.error,
                                content = or.content
                            )
                        }
                    }
                    2 -> {
                        result = decodeSerializableElement(
                            descriptor,
                            2,
                            object : DeserializationStrategy<ActionResult> {
                                override val descriptor: SerialDescriptor
                                    get() = ActionResultDeclaredSerializer.descriptor

                                override fun patch(decoder: Decoder, old: ActionResult): ActionResult {
                                    throw UnsupportedOperationException("Not implemented, should not be called")
                                }

                                override fun deserialize(decoder: Decoder): ActionResult {
                                    var simpleError: String? = null
                                    var fullError: String? = null
                                    var content: JsonObject? = null
                                    decoder.decodeStructure(descriptor) {
                                        rrp@ while (true) {
                                            when (val index2 = decodeElementIndex(descriptor)) {
                                                -1 -> break@rrp
                                                0 -> simpleError =
                                                    decodeNullableString(descriptor, index2, simpleError)
                                                1 -> fullError = decodeNullableString(descriptor, index2, fullError)
                                                2 -> content = decodeNullableSerializableElement(
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
                                        success ?: false,
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
            check(result != null && success != null) { "Cannot deserialize ActionResult." }
            result
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun deserialize(decoder: Decoder): ActionResult =
            decoder.decodeStructure(descriptor) { deserialize(this) }
    }
}

@Serializable
sealed class OutgoingAction {
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
    ) : OutgoingAction()

    @Serializable
    @SerialName("FriendMessage")
    data class FriendMessage(
        override val sender: FriendModel,
        override val message: MessageChainModel,
        override val replyKey: String,
        val bot: Long
    ) : OutgoingAction()

    @Serializable
    @SerialName("TempMessage")
    data class TempMessage(
        val group: GroupModel,
        override val sender: MemberModel,
        override val message: MessageChainModel,
        override val replyKey: String,
        val bot: Long
    ) : OutgoingAction()

    @Serializable
    @SerialName("MemberMuteEvent")
    data class MemberMuteEvent(
        val group: GroupModel,
        val member: MemberModel,
        val bot: Long,
        val time: Int,
        val operator: MemberModel? = null
    ) : OutgoingAction()

    @Serializable
    @SerialName("MemberUnmuteEvent")
    data class MemberUnmuteEvent(
        val group: GroupModel,
        val member: MemberModel,
        val bot: Long,
        val operator: MemberModel? = null
    ) : OutgoingAction()

    @Serializable
    @SerialName("BotUnmuteEvent")
    data class BotUnmuteEvent(
        val group: GroupModel,
        val bot: Long,
        val operator: MemberModel? = null
    ) : OutgoingAction()

    @Serializable
    @SerialName("BotMuteEvent")
    data class BotMuteEvent(
        val group: GroupModel,
        val bot: Long,
        val time: Int,
        val operator: MemberModel? = null
    ) : OutgoingAction()

    @Serializable
    @SerialName("BotGroupPermissionChangeEvent")
    data class BotGroupPermissionChangeEvent(
        val group: GroupModel,
        val bot: Long,
        val origin: String,
        val new: String
    ) : OutgoingAction()

    @Serializable
    @SerialName("MemberPermissionChangeEvent")
    data class MemberPermissionChangeEvent(
        val group: GroupModel,
        val member: MemberModel,
        val bot: Long,
        val origin: String,
        val new: String
    ) : OutgoingAction()

    @Serializable
    @SerialName("MemberSpecialTitleChangeEvent")
    data class MemberSpecialTitleChangeEvent(
        val group: GroupModel,
        val member: MemberModel,
        val origin: String,
        val new: String,
        val operator: MemberModel? = null,
        val bot: Long
    ) : OutgoingAction()
}



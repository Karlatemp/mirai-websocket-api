/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ActionModel.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
sealed class IncomingAction {
    abstract val metadata: String?

    @Serializable
    @SerialName("Reply")
    data class ReplyMessage(
        val id: String,
        val message: MessageChainModel,
        override val metadata: String? = null
    ) : IncomingAction()

    @SerialName("SendToGroup")
    @Serializable
    data class SendToGroup(
        val group: Long,
        val bot: Long,
        val message: MessageChainModel,
        override val metadata: String? = null
    ) : IncomingAction()

    @SerialName("SendToFriend")
    @Serializable
    data class SendToFriend(
        val friend: Long,
        val bot: Long,
        val message: MessageChainModel,
        override val metadata: String? = null
    ) : IncomingAction()

    @SerialName("RecallReceipt")
    @Serializable
    data class RecallReceipt(
        val receipt: String,
        override val metadata: String? = null
    ) : IncomingAction()

}

@Serializable
sealed class OutgoingAction(
    val isMessageEvent: Boolean = false
) {
    open val replyKey: String? get() = null
    open val message: MessageChainModel? get() = null
    open val sender: SenderModel? get() = null

    @Serializable
    sealed class ActionResult : OutgoingAction() {
        abstract val metadata: String?

        @Serializable
        @SerialName("ResultSuccess")
        data class Success(
            override val metadata: String?,
            val extendData: JsonElement? = null
        ) : ActionResult()

        @Serializable
        @SerialName("ResultFailed")
        data class Failed(
            override val metadata: String?,
            val error: String,
            val fullError: String
        ) : ActionResult()
    }

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



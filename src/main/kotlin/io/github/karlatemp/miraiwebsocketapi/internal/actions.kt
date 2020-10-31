/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/28 22:53:06
 *
 * mirai-websocket-api/actions.kt
 */

package io.github.karlatemp.miraiwebsocketapi.internal

import io.github.karlatemp.miraiwebsocketapi.MessageChainModel
import io.github.karlatemp.miraiwebsocketapi.MsgModel
import io.github.karlatemp.miraiwebsocketapi.toModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.OfflineMessageSource
import net.mamoe.mirai.message.data.OnlineMessageSource
import io.github.karlatemp.miraiwebsocketapi.json as jsonEncoder

internal fun Group.verbose(noMembers: Boolean): JsonObject = buildJsonObject {
    val group = this@verbose
    put("name", group.name)
    put("id", group.id)
    put("botPermission", group.botPermission.name.toLowerCase())
    if (!noMembers) {
        putJsonArray("members") {
            this@verbose.members.forEach { member ->
                add(member.verbose())
            }
        }
    }
    put("owner", group.owner.verbose())
    put("typeName", "Group")
}

internal fun Member.verbose() = buildJsonObject {
    val member = this@verbose
    put("id", member.id)
    put("permission", member.permission.name.toLowerCase())
    put("nameCard", member.nameCard)
    put("nick", member.nick)
    put("nameCardOrNick", member.nameCardOrNick)
    put("specialTitle", member.specialTitle)
    put("muteTimeRemaining", member.muteTimeRemaining)
    put("isMuted", member.isMuted)
    put("typeName", "Member")
}

internal fun Friend.verbose() = buildJsonObject {
    val friend = this@verbose
    put("id", friend.id)
    put("nick", friend.nick)
    put("typeName", "Friend")
}

internal fun Bot.listFriends() = buildJsonObject {
    putJsonArray("friends") { friends.forEach { add(it.verbose()) } }
}

internal fun Bot.listGroups() = buildJsonObject {
    putJsonArray("groups") { groups.forEach { add(it.verbose(true)) } }
}

internal suspend fun MessageSource.verbose() = buildJsonObject {
    val source = this@verbose
    put("fromBot", source.bot.id)
    put("id", source.id)
    put("internalId", source.internalId)
    put("fromId", source.fromId)
    put("targetId", source.targetId)
    put("time", source.time)
    put("message", source.originalMessage.toModel(true).json())
    when (source) {
        is OnlineMessageSource -> {
            put("isOnline", true)
            when (source) {
                is OnlineMessageSource.Incoming -> {
                    put("isIncoming", true)
                    put("isOutgoing", false)
                    when (source) {
                        is OnlineMessageSource.Incoming.FromFriend -> {
                            put("typeName", "OnlineMessageSource.Incoming.FromFriend")
                            put("sender", source.sender.verbose())
                        }
                        is OnlineMessageSource.Incoming.FromGroup -> {
                            put("typeName", "OnlineMessageSource.Incoming.FromGroup")
                            put("sender", source.sender.verbose())
                            put("group", source.group.verbose(true))
                        }
                        is OnlineMessageSource.Incoming.FromTemp -> {
                            put("typeName", "OnlineMessageSource.Incoming.FromTemp")
                            put("sender", source.sender.verbose())
                            put("sender", source.group.verbose(true))
                        }
                        is OnlineMessageSource.Outgoing,
                        is OfflineMessageSource -> throw AssertionError()
                    }
                }
                is OnlineMessageSource.Outgoing -> {
                    put("isIncoming", false)
                    put("isOutgoing", true)
                    when (source) {
                        is OnlineMessageSource.Outgoing.ToFriend -> {
                            put("typeName", "OnlineMessageSource.Outgoing.ToFriend")
                            put("target", source.target.verbose())
                        }
                        is OnlineMessageSource.Outgoing.ToTemp -> {
                            put("typeName", "OnlineMessageSource.Outgoing.ToTemp")
                            put("target", source.target.verbose())
                            put("group", source.group.verbose(true))
                        }
                        is OnlineMessageSource.Outgoing.ToGroup -> {
                            put("typeName", "OnlineMessageSource.Outgoing.ToGroup")
                            put("target", source.target.verbose(true))
                        }
                        is OfflineMessageSource,
                        is OnlineMessageSource.Incoming -> throw AssertionError()
                    }
                }
                else -> throw AssertionError()
            }
        }
        is OfflineMessageSource -> {
            put("isOnline", false)
            put("sourceKind", source.kind.name.toLowerCase())
            put("typeName", "OfflineMessageSource")
        }
    }
}

private val MessageChainModelSerializer = ListSerializer(MsgModel.serializer())
private fun MessageChainModel.json(): JsonElement {
    return jsonEncoder.encodeToJsonElement(MessageChainModelSerializer, this)
}

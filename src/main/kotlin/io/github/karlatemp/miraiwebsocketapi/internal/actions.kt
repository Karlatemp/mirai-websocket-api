/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/28 22:53:06
 *
 * mirai-websocket-api/actions.kt
 */

package io.github.karlatemp.miraiwebsocketapi.internal

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.*

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
}

internal fun Member.verbose() = buildJsonObject {
    val member = this@verbose
    put("id", member.id)
    put("permission", member.permission.name.toLowerCase())
    put("nameCard", member.nameCard)
    put("nameCardOrNick", member.nameCardOrNick)
    put("specialTitle", member.specialTitle)
    put("muteTimeRemaining", member.muteTimeRemaining)
    put("isMuted", member.isMuted)
}

internal fun Friend.verbose() = buildJsonObject {
    val friend = this@verbose
    put("id", friend.id)
    put("nick", friend.nick)
}

internal fun Bot.listFriends() = buildJsonObject {
    putJsonArray("friends") { friends.forEach { add(it.verbose()) } }
}

internal fun Bot.listGroups() = buildJsonObject {
    putJsonArray("groups") { groups.forEach { add(it.verbose(true)) } }
}

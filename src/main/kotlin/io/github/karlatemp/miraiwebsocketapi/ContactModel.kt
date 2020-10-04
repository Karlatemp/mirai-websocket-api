/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ContactModel.kt
 */

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.karlatemp.miraiwebsocketapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick

@Serializable
data class FriendModel(
    override val id: Long,
    override val nick: String
) : SenderModel

fun Friend.toModel(): FriendModel = FriendModel(
    this.id, this.nick
)

fun Member.toModel(): MemberModel = MemberModel(
    id = this.id,
    nick = this.nick,
    nameCard = this.nameCard,
    nameCardOrNick = this.nameCardOrNick,
    permission = permission.name.toLowerCase(),
    specialTitle = this.specialTitle
)

fun Group.toModel(): GroupModel = GroupModel(
    this.id, this.name, this.botPermission.name.toLowerCase()
)

interface SenderModel {
    val id: Long
    val nick: String
}

@Serializable
data class MemberModel(
    override val id: Long,
    override val nick: String,
    val nameCard: String,
    val nameCardOrNick: String,
    val permission: String,
    val specialTitle: String
) : SenderModel

@Serializable
data class GroupModel(
    val id: Long,
    val name: String,
    val botPermission: String
)


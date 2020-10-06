/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/06 20:42:20
 *
 * mirai-websocket-api/events.kt
 */

@file:Suppress("unused")

package io.github.karlatemp.miraiwebsocketapi.event

import io.github.karlatemp.miraiwebsocketapi.account.Account
import io.ktor.websocket.*
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.CancellableEvent

open class MiraiWSEvent : AbstractEvent()

interface AccountEvent {
    val account: Account
}

class AccountTryLoginEvent(
    override val account: Account,
    /** websocket session, WARNING: 不要使用此 session 进行信息收发 */
    val session: WebSocketServerSession
) : MiraiWSEvent(), AccountEvent, CancellableEvent


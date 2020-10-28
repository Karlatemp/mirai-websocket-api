/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/06 20:17:56
 *
 * mirai-websocket-api/Account.kt
 */

package io.github.karlatemp.miraiwebsocketapi.account

import io.github.karlatemp.miraiwebsocketapi.actions.IncomingAction
import io.github.karlatemp.miraiwebsocketapi.actions.OutgoingAction
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

open class Account internal constructor(
    val username: String,
    val session: WebSocketServerSession,
    val metadata: MutableMap<String, Any?>,
) {
    constructor(username: String, session: WebSocketServerSession) : this(username, session, ConcurrentHashMap())

    override fun toString(): String {
        return "Account($username)"
    }

    open suspend fun shouldBroadcast(action: OutgoingAction): Boolean {
        // TODO
        return true
    }

    open suspend fun isAllowed(action: IncomingAction): Boolean {
        // TODO
        return true
    }

    class Root(session: WebSocketServerSession) : Account("root", session) {
        override suspend fun shouldBroadcast(action: OutgoingAction): Boolean = true
        override suspend fun isAllowed(action: IncomingAction): Boolean = true
        override fun toString(): String {
            return "Account[/trusted]"
        }
    }
}

inline val Account.isRoot: Boolean get() = this is Account.Root

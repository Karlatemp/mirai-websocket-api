/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/06 20:17:56
 *
 * mirai-websocket-api/Account.kt
 */

package io.github.karlatemp.miraiwebsocketapi.account

import io.github.karlatemp.miraiwebsocketapi.IncomingAction
import io.github.karlatemp.miraiwebsocketapi.OutgoingAction
import java.util.concurrent.ConcurrentHashMap

open class Account internal constructor(
    val username: String,
    val metadata: MutableMap<String, Any?>,
) {
    constructor(username: String) : this(username, ConcurrentHashMap())

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

    class Root : Account("root") {
        override suspend fun shouldBroadcast(action: OutgoingAction): Boolean = true
        override suspend fun isAllowed(action: IncomingAction): Boolean = true
        override fun toString(): String {
            return "Account[/trusted]"
        }
    }
}

inline val Account.isRoot: Boolean get() = this is Account.Root

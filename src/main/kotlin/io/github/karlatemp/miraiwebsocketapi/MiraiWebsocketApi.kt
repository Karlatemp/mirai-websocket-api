/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MiraiWebsocketApi.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import com.google.auto.service.AutoService
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.github.karlatemp.miraiwebsocketapi.account.Account
import io.github.karlatemp.miraiwebsocketapi.actions.*
import io.github.karlatemp.miraiwebsocketapi.event.AccountTryLoginEvent
import io.github.karlatemp.miraiwebsocketapi.http.logonSessions
import io.github.karlatemp.miraiwebsocketapi.http.setupHttp
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.getFriendOrNull
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.recall
import org.slf4j.LoggerFactory
import java.util.*

internal val metadata by lazy {
    val properties = Properties()
    MiraiWebsocketApi::class.java
        .getResourceAsStream("/miraiwsapi.properties")
        ?.let { stream ->
            stream.reader().buffered().use { properties.load(it) }
        }
    properties
}
internal val version by lazy {
    metadata.getProperty("version", "0.0.0-ERROR+ERROR-Metadata-NOT-FOUND")
}

@AutoService(JvmPlugin::class)
object MiraiWebsocketApi : KotlinPlugin(
    JvmPluginDescriptionBuilder(
        id = "io.github.karlatemp.mirai.mirai-http-api",
        version = version
    )
        .author("Karlatemp")
        .name("MiraiWSApi")
        .build()
) {
    @OptIn(KtorExperimentalAPI::class)
    override fun onEnable() {
        logger.info("Enabling MiraiWebsocketApi[$version]")
        logger.info("Reloading configs...")
        MiraiWebsocketApiSettings.reload()
        messageSourceCache
        replayCache
        logger.info("User   = " + MiraiWebsocketApiSettings.user)
        logger.info("Passwd = " + MiraiWebsocketApiSettings.passwd)
        val server = embeddedServer(CIO, environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("Mirai WebSocket API")
            this.module(Application::web)

            connector {
                this.host = MiraiWebsocketApiSettings.host
                this.port = MiraiWebsocketApiSettings.port
            }
        })
        logger.info("Server running on ${MiraiWebsocketApiSettings.host}:${MiraiWebsocketApiSettings.port}")
        server.start(false)
        suspend fun OutgoingAction.post() {
            messageBus.post(this, json.encodeToString(OutgoingSerializer, this))
        }
        subscribeAlways<Event>(priority = Listener.EventPriority.MONITOR) {
            when (this) {
                is FriendMessageEvent -> {
                    OutgoingAction.FriendMessage(
                        sender.toModel(),
                        message.toModel(),
                        saveReplyCache(subject),
                        bot.id
                    ).post()
                }
                is GroupMessageEvent -> {
                    OutgoingAction.GroupMessage(
                        sender = sender.toModel(),
                        group = group.toModel(),
                        message = message.toModel(),
                        replyKey = saveReplyCache(subject),
                        bot = bot.id
                    ).post()
                }
                is TempMessageEvent -> {
                    OutgoingAction.TempMessage(
                        sender = sender.toModel(),
                        group = group.toModel(),
                        message = message.toModel(),
                        replyKey = saveReplyCache(sender),
                        bot = bot.id
                    )
                }
                is MessageRecallEvent -> {

                }
            }
        }
        logger.info("Mirai Websocket Api enabled.")
    }
}

val messageBus = ConcurrentLinkedList<suspend (OutgoingAction, String) -> Unit>()

val replayCache: Cache<String, Contact> by lazy {
    CacheBuilder.newBuilder()
        .run { MiraiWebsocketApiSettings.replyCache.run { buildCache("Reply") } }
        .build()
}

fun saveReplyCache(contact: Contact): String {
    val id = "RP." + System.currentTimeMillis() + "/" + UUID.randomUUID()
    replayCache[id] = contact
    return id
}

val receiptCache: Cache<String, MessageReceipt<*>> by lazy {
    CacheBuilder.newBuilder()
        .run { MiraiWebsocketApiSettings.receiptCache.run { buildCache("Receipt") } }
        .build()
}

fun saveReceiptCache(receipt: MessageReceipt<*>): String {
    val id = "REC." + System.currentTimeMillis() + "/" + UUID.randomUUID()
    receiptCache[id] = receipt
    return id
}


suspend fun <T1, T2> ConcurrentLinkedList<suspend (T1, T2) -> Unit>.post(m1: T1, m2: T2) {
    forEach { kotlin.runCatching { it(m1, m2) } }
}

suspend fun selectUser(user: String, passwd: String, session: WebSocketServerSession): Account? {
    // TODO: 多用户
    if (user == MiraiWebsocketApiSettings.user && passwd == MiraiWebsocketApiSettings.passwd) {
        return Account.Root(session)
    }
    return null
}


fun Application.web() {
    install(WebSockets)
    routing {
        setupHttp()
        webSocket("/") {
            // region helpers
            suspend fun <T> rep(serializer: SerializationStrategy<T>, value: T) =
                outgoing.send(Frame.Text(json.encodeToString(serializer, value)))

            suspend fun Request?.repOk(ext: JsonObject?) =
                rep(OutgoingSerializer, ActionResult(this?.requestId, true, null, null, ext))

            suspend fun Request?.repOk() = repOk(null)

            suspend fun Request?.repErr(
                error: String, full: String
            ) = rep(
                OutgoingSerializer,
                ActionResult(this?.requestId, false, error, full, null)
            )

            suspend fun Request?.repErr(error: String) = repErr(error, error)
            suspend fun Request?.repErr(error: Throwable) = repErr(error.toString(), error.stackTraceToString())
            // endregion
            val logonSessionId = "WS.ST" + UUID.randomUUID() + "/SNOW" + System.currentTimeMillis()
            // region login
            val account = kotlin.runCatching {
                val user = (incoming.receive() as Frame.Text).readText()
                val passwd = (incoming.receive() as Frame.Text).readText()
                val selectedAccount = selectUser(user, passwd, this)
                if (selectedAccount != null) {
                    if (!AccountTryLoginEvent(selectedAccount, this).also { it.broadcast() }.isCancelled) {
                        null.repOk(buildJsonObject {
                            logonSessions[logonSessionId] = selectedAccount
                            put("session", logonSessionId)
                        })
                        return@runCatching selectedAccount
                    }
                }
                null.repErr("Login failed.")
                return@webSocket
            }.getOrNull() ?: return@webSocket
            // endregion

            val hook = messageBus.insertLast { action, message ->
                if (account.shouldBroadcast(action))
                    outgoing.send(Frame.Text(message))
            }
            try {

                fun MessageReceipt<*>.json() = buildJsonObject {
                    put("receiptId", saveReceiptCache(this@json))
                    put("sourceId", this@json.source.toModel().id)
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val request = kotlin.runCatching {
                            json.decodeFromString(Request.RequestSerializer, frame.readText())
                        }.getOrElse { exception ->
                            null.repErr(exception)
                            return@webSocket
                        }
                        val action = request.action

                        try {
                            if (!account.isAllowed(action)) {
                                request.repErr("Error: This session don't have the permission to perform the action.")
                                continue
                            }
                            when (action) {
                                is IncomingAction.RecallReceipt -> {
                                    val receipt = receiptCache[action.receipt]
                                    if (receipt == null) {
                                        request.repErr("Receipt ${action.receipt} not found.")
                                    } else {
                                        receipt.recall()
                                        request.repOk()
                                    }
                                }
                                is IncomingAction.Recall -> {
                                    val messageSource = messageSourceCache[action.messageSource]
                                    if (messageSource == null) {
                                        request.repErr("Message source ${action.messageSource} not found.")
                                    } else {
                                        messageSource.recall()
                                    }
                                }
                                is IncomingAction.ReplyMessage -> {
                                    val reply = replayCache[action.id]
                                    if (reply == null) {
                                        request.repErr("Reply id ${action.id} not found.")
                                    } else {
                                        request.repOk(reply.sendMessage(action.message.toChain(reply)).json())
                                    }
                                }
                                is IncomingAction.MuteMember -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        request.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val group = bot.getGroupOrNull(action.group)
                                        if (group == null) {
                                            request.repErr("Group ${action.group} not found in bot ${action.bot}")
                                        } else {
                                            val member = group.getOrNull(action.member)
                                            if (member == null) {
                                                request.repErr("Member ${action.member} not found in group ${action.group} with bot ${action.bot}")
                                            } else {
                                                member.mute(action.time)
                                                request.repOk()
                                            }
                                        }
                                    }
                                }
                                is IncomingAction.SendToGroup -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        request.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val group = bot.getGroupOrNull(action.group)
                                        if (group == null) {
                                            request.repErr("Group ${action.group} not found in bot ${action.bot}")
                                        } else {
                                            request.repOk(group.sendMessage(action.message.toChain(group)).json())
                                        }
                                    }
                                }
                                is IncomingAction.SendToFriend -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        request.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val friend = bot.getFriendOrNull(action.friend)
                                        if (friend == null) {
                                            request.repErr("Friend ${action.friend} not found in bot ${action.bot}")
                                        } else {
                                            request.repOk(friend.sendMessage(action.message.toChain(friend)).json())
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            request.repErr(e)
                        }
                    }
                }
            } finally {
                hook.remove()
                logonSessions.remove(logonSessionId)
            }
        }
    }
}
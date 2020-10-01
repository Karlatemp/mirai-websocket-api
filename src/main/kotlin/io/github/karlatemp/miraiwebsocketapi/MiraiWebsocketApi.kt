/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MiraiWebsocketApi.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import com.google.auto.service.AutoService
import com.google.common.cache.CacheBuilder
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.getFriendOrNull
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.recall
import java.util.*
import java.util.concurrent.TimeUnit


@AutoService(JvmPlugin::class)
object MiraiWebsocketApi : KotlinPlugin(
    JvmPluginDescriptionBuilder(
        id = "io.github.karlatemp.mirai.mirai-http-api",
        version = "1.0.0"
    )
        .author("Karlatemp")
        .name("Mirai WS Api")
        .build()
) {
    @OptIn(KtorExperimentalAPI::class)
    override fun onEnable() {
        logger.info("Enabling MiraiWebsocketApi...")
        logger.info("Reloading configs...")
        MiraiWebsocketApiSettings.reload()
        logger.info("User   = " + MiraiWebsocketApiSettings.user)
        logger.info("Passwd = " + MiraiWebsocketApiSettings.passwd)
        val server = embeddedServer(CIO, environment = applicationEngineEnvironment {
            this.module(Application::web)

            connector {
                this.host = MiraiWebsocketApiSettings.host
                this.port = MiraiWebsocketApiSettings.port
            }
        })
        logger.info("Server running on ${MiraiWebsocketApiSettings.host}:${MiraiWebsocketApiSettings.port}")
        server.start(false)
        suspend fun OutgoingAction.post() {
            messageBus.post(json.encodeToString(OutgoingAction.serializer(), this))
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

val messageBus = ConcurrentLinkedList<suspend (String) -> Unit>()

val replayCache = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build<String, Contact>()

fun saveReplyCache(contact: Contact): String {
    val id = "RP." + System.currentTimeMillis() + "/" + UUID.randomUUID()
    replayCache[id] = contact
    return id
}

val receiptCache = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build<String, MessageReceipt<*>>()

fun saveReceiptCache(receipt: MessageReceipt<*>): String {
    val id = "REC." + System.currentTimeMillis() + "/" + UUID.randomUUID()
    receiptCache[id] = receipt
    return id
}


suspend fun <T> ConcurrentLinkedList<suspend (T) -> Unit>.post(msg: T) {
    forEach { kotlin.runCatching { it(msg) } }
}

fun Application.web() {
    install(WebSockets)
    routing {
        webSocket("/") {
            // region helpers
            suspend fun <T> rep(serializer: SerializationStrategy<T>, value: T) =
                outgoing.send(Frame.Text(json.encodeToString(serializer, value)))

            suspend fun IncomingAction.repOk(ext: JsonElement?) =
                rep(OutgoingAction.serializer(), OutgoingAction.ActionResult.Success(metadata, ext))

            suspend fun IncomingAction.repOk() = repOk(null)

            suspend fun IncomingAction.repErr(
                error: String, full: String
            ) = rep(
                OutgoingAction.ActionResult.serializer(),
                OutgoingAction.ActionResult.Failed(metadata, error, full)
            )

            suspend fun IncomingAction.repErr(error: String) = repErr(error, error)
            // endregion
            // region login
            kotlin.runCatching {
                val user = (incoming.receive() as Frame.Text).readText()
                val passwd = (incoming.receive() as Frame.Text).readText()
                // TODO: 多用户, 等pr
                val success = (
                        user == MiraiWebsocketApiSettings.user && passwd == MiraiWebsocketApiSettings.passwd
                        )
                if (success) {
                    rep(
                        OutgoingAction.ActionResult.serializer(),
                        OutgoingAction.ActionResult.Success(null)
                    )
                } else {
                    rep(
                        OutgoingAction.ActionResult.serializer(),
                        OutgoingAction.ActionResult.Failed(null, "Login failed", "Login failed")
                    )
                    return@webSocket
                }
            }
            // endregion

            val hook = messageBus.insertLast { outgoing.send(Frame.Text(it)) }
            try {

                fun MessageReceipt<*>.json() = buildJsonObject {
                    put("receiptId", saveReceiptCache(this@json))
                    put("sourceId", this@json.source.toModel().id)
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val action = json.decodeFromString(IncomingAction.serializer(), frame.readText())

                        try {
                            when (action) {
                                is IncomingAction.RecallReceipt -> {
                                    val receipt = receiptCache[action.receipt]
                                    if (receipt == null) {
                                        action.repErr("Receipt ${action.receipt} not found.")
                                    } else {
                                        receipt.recall()
                                        action.repOk()
                                    }
                                }
                                is IncomingAction.Recall -> {
                                    val messageSource = messageSourceCache[action.messageSource]
                                    if (messageSource == null) {
                                        action.repErr("Message source ${action.messageSource} not found.")
                                    } else {
                                        messageSource.recall()
                                    }
                                }
                                is IncomingAction.ReplyMessage -> {
                                    val reply = replayCache[action.id]
                                    if (reply == null) {
                                        action.repErr("Reply id ${action.id} not found.")
                                    } else {
                                        action.repOk(reply.sendMessage(action.message.toChain(reply)).json())
                                    }
                                }
                                is IncomingAction.MuteMember -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        action.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val group = bot.getGroupOrNull(action.group)
                                        if (group == null) {
                                            action.repErr("Group ${action.group} not found in bot ${action.bot}")
                                        } else {
                                            val member = group.getOrNull(action.member)
                                            if (member == null) {
                                                action.repErr("Member ${action.member} not found in group ${action.group} with bot ${action.bot}")
                                            } else {
                                                member.mute(action.time)
                                                action.repOk()
                                            }
                                        }
                                    }
                                }
                                is IncomingAction.SendToGroup -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        action.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val group = bot.getGroupOrNull(action.group)
                                        if (group == null) {
                                            action.repErr("Group ${action.group} not found in bot ${action.bot}")
                                        } else {
                                            action.repOk(group.sendMessage(action.message.toChain(group)).json())
                                        }
                                    }
                                }
                                is IncomingAction.SendToFriend -> {
                                    val bot = Bot.getInstanceOrNull(action.bot)
                                    if (bot == null) {
                                        action.repErr("Bot ${action.bot} not found.")
                                    } else {
                                        val friend = bot.getFriendOrNull(action.friend)
                                        if (friend == null) {
                                            action.repErr("Friend ${action.friend} not found in bot ${action.bot}")
                                        } else {
                                            action.repOk(friend.sendMessage(action.message.toChain(friend)).json())
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            action.repErr(e.toString(), e.stackTraceToString())
                        }
                    }
                }
            } finally {
                hook.remove()
            }
        }
    }
}
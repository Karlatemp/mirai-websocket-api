/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ShitListing.kt
 */

import io.github.karlatemp.miraiwebsocketapi.*
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

inline fun <reified T> Iterable<*>.firstInstance(): T = first { it is T } as T

@OptIn(KtorExperimentalAPI::class)
suspend fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    val c = ConcurrentLinkedList<suspend (OutgoingAction) -> Unit>()
    client.webSocket("ws://localhost:7247/") {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val msg = json.decodeFromString(OutgoingAction.serializer(), frame.readText())
                println(msg)
                suspend fun post(action: IncomingAction) {
                    outgoing.send(Frame.Text(json.encodeToString(IncomingAction.serializer(), action)))
                }
                c.forEach { it(msg) }

                suspend fun got(
                    pre: suspend () -> Unit = {},
                    selector: suspend (OutgoingAction) -> Boolean
                ): OutgoingAction {
                    val atomic = AtomicReference<ConcurrentLinkedList.LinkNode<*>>()
                    val cor = AtomicReference<Continuation<OutgoingAction>?>()
                    val node = c.insertLast {
                        var cw = cor.get()
                        while (cw == null) {
                            delay(100)
                            cw = cor.get()
                        }
                        if (selector.invoke(it)) {
                            atomic.get().remove()
                            cw.resume(it)
                        }
                    }
                    atomic.set(node)
                    pre()
                    return suspendCoroutine { corx ->
                        cor.set(corx)
                    }
                }
                launch {
                    if (msg.isMessageEvent) {
                        val chain = msg.message!!
                        val rwmsg = chain.joinToString("") {
                            if (it is PlainModel) it.msg else ""
                        }
                        when (rwmsg) {
                            "image" -> {
                                post(
                                    IncomingAction.ReplyMessage(
                                        msg.replyKey!!, listOf(
                                            ImageModel(url = "file:///C:/Users/32798/Pictures/awx.png")
                                        )
                                    )
                                )
                            }
                            "rec" -> {
                                val id = "Xw" + UUID.randomUUID() + "/" + System.currentTimeMillis()
                                val wkk = got(pre = {
                                    post(
                                        IncomingAction.ReplyMessage(
                                            msg.replyKey!!, listOf(
                                                PlainModel("IVK")
                                            ),
                                            metadata = id
                                        )
                                    )
                                }) { it is OutgoingAction.ActionResult.Success && it.metadata == id } as OutgoingAction.ActionResult.Success
                                val idWX = wkk.extendData!!.jsonObject["receiptId"]!!.jsonPrimitive.content
                                println(idWX)
                                delay(5000)
                                post(IncomingAction.RecallReceipt(idWX))
                            }
                            "reply me" -> {
                                post(
                                    IncomingAction.ReplyMessage(
                                        msg.replyKey!!, listOf(
                                            QuoteModel(chain.firstInstance<MessageSourceModel>().id),
                                            PlainModel("Quote Test")
                                        )
                                    )
                                )
                            }
                            "at" -> {
                                post(
                                    IncomingAction.ReplyMessage(
                                        msg.replyKey!!, listOf(
                                            AtModel(msg.sender!!.id),
                                            PlainModel("At test")
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
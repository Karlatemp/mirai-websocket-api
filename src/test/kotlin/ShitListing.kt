/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ShitListing.kt
 */

import io.github.karlatemp.miraiwebsocketapi.*
import io.github.karlatemp.miraiwebsocketapi.actions.*
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

inline fun <reified T> Iterable<*>.firstInstance(): T = first { it is T } as T
val pj = Json(json) { prettyPrint = true }

@OptIn(KtorExperimentalAPI::class)
suspend fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    val c = ConcurrentLinkedList<suspend (Any) -> Unit>()
    client.webSocket("ws://localhost:7247/") {
        outgoing.send(Frame.Text("root")) // user
        outgoing.send(Frame.Text("ROOT")) // passwd
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val txt = frame.readText()
                println(pj.encodeToString(pj.decodeFromString(JsonElement.serializer(), txt)))
                val msg = json.decodeFromString(OutgoingSerializer, txt)
                println(msg)
                suspend fun post(action: IncomingAction, id: String?) {
                    outgoing.send(
                        Frame.Text(
                            json.encodeToString(
                                Request.RequestSerializer, Request(action, id)
                            )
                        )
                    )
                }
                suspend fun post(action: IncomingAction) = post(action, null)
                c.forEach { it(msg) }

                suspend fun got(
                    pre: suspend () -> Unit = {},
                    selector: suspend (Any) -> Boolean
                ): Any {
                    val atomic = AtomicReference<ConcurrentLinkedList.LinkNode<*>>()
                    val cor = AtomicReference<Continuation<Any>?>()
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

                suspend fun sendAndReceive(msg: suspend (String) -> IncomingAction): ActionResult {
                    val id = "Xw" + UUID.randomUUID() + "/" + System.currentTimeMillis()
                    return got(pre = {
                        post(msg(id), id)
                    }) { it is ActionResult && it.id == id } as ActionResult
                }
                launch {
                    if (msg is OutgoingAction) {
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
                                "rev" -> {
                                    val mid = chain.firstInstance<MessageSourceModel>().id
                                    post(IncomingAction.Recall(mid))
                                    post(
                                        IncomingAction.ReplyMessage(
                                            msg.replyKey!!, listOf(
                                                QuoteModel(mid),
                                                PlainModel("REC TW")
                                            )
                                        )
                                    )
                                }
                                "rec" -> {
                                    val wkk = sendAndReceive {
                                        IncomingAction.ReplyMessage(
                                            msg.replyKey!!,
                                            listOf(
                                                PlainModel("IVK")
                                            )
                                        )
                                    }
                                    val idWX = wkk.content!!.getValue("receiptId").jsonPrimitive.content
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
}

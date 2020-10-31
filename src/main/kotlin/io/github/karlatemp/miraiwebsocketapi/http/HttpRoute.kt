/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/11 09:58:46
 *
 * mirai-websocket-api/HttpRoute.kt
 */

package io.github.karlatemp.miraiwebsocketapi.http

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.github.karlatemp.miraiwebsocketapi.MiraiWebsocketApiSettings
import io.github.karlatemp.miraiwebsocketapi.account.Account
import io.github.karlatemp.miraiwebsocketapi.get
import io.github.karlatemp.miraiwebsocketapi.internal.listFriends
import io.github.karlatemp.miraiwebsocketapi.internal.listGroups
import io.github.karlatemp.miraiwebsocketapi.internal.verbose
import io.github.karlatemp.miraiwebsocketapi.messageSourceCache
import io.github.karlatemp.miraiwebsocketapi.set
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.getGroupOrNull
import java.util.*
import java.util.concurrent.ConcurrentHashMap

val logonSessions = ConcurrentHashMap<String, Account>()
val CONTENT_JSON = ContentType.parse("application/json")

private val prettyJson = Json {
    prettyPrint = true
}

private fun error(
    code: Int,
    error: String? = null,
    errorDetail: String? = null,
    content: (JsonObjectBuilder.() -> Unit)? = null
): String = buildJsonObject {
    put("code", code)
    error?.let {
        put("error", it)
        put("errorDetail", errorDetail ?: it)
    }
    content?.let { putJsonObject("content", it) }
}.let { prettyJson.encodeToString(JsonObject.serializer(), it) }


private fun error0(
    code: Int,
    error: String? = null,
    errorDetail: String? = null,
    content: JsonElement? = null
): String = buildJsonObject {
    put("code", code)
    error?.let {
        put("error", it)
        put("errorDetail", errorDetail ?: it)
    }
    content?.let { put("content", it) }
}.toString()

private fun interrupt(response: String): Nothing = throw Interrupt(response)

private val ERROR_INVALID_SESSION = error(5, "Invalid Session")
private val ERROR_INVALID_PARAM = error(7, "Invalid Parameter")
private val OK = error(0)

val imageCache: Cache<String, ByteArray> by lazy {
    CacheBuilder.newBuilder()
        .run { MiraiWebsocketApiSettings.imageCache.run { buildCache("Image") } }
        .build()
}

internal class Interrupt(val json: String) : RuntimeException(json, null, false, false)

internal suspend inline fun PipelineContext<Unit, ApplicationCall>.interrup(code: () -> Unit) {
    try {
        code()
    } catch (interrupt: Interrupt) {
        call.respondText(interrupt.json, CONTENT_JSON)
    } catch (throwable: Throwable) {
        call.respondText(error(1457, throwable.toString(), throwable.stackTraceToString()), CONTENT_JSON)
    }
}

fun PipelineContext<Unit, ApplicationCall>.checkSession(): Account {
    val request = call.request
    val session = request.headers["WS-API-SESSION"]
        ?: request.queryParameters["session"]
        ?: throw Interrupt(ERROR_INVALID_SESSION)
    return logonSessions[session] ?: throw Interrupt(ERROR_INVALID_SESSION)
}

fun Routing.setupHttp() {
    post("/uploadImage") {
        interrup {
            checkSession()
            // TODO: Check perms
            val allParts = call.receiveMultipart().readAllParts()
            val bytes = allParts.file("image").streamProvider().readBytes()
            val iid = "IMG.K" + UUID.randomUUID().toString().replace("-", "")
            imageCache[iid] = bytes
            call.respondText(error(0) {
                put("id", iid)
                put("ws-image", "wsapi:$iid")
                put("path", "/image?image=$iid")
            }, CONTENT_JSON)
        }
    }
    get("/image") {
        val target = call.request.queryParameters["image"]
        if (target == null) {
            call.respondText(error(404, "Parameter `image` missing."), CONTENT_JSON, status = HttpStatusCode.NotFound)
            return@get
        }
        val image = imageCache[target]
        if (image == null) {
            call.respondText(error(404, "Image $target not found"), CONTENT_JSON, status = HttpStatusCode.NotFound)
            return@get
        }
        call.respondBytes(image, ContentType.parse("image/png"))
    }


    get("/listGroups") {
        interrup {
            checkSession()
            // TODO: Check perms
            call.respondText(error0(0, content = bot().listGroups()), CONTENT_JSON)
        }
    }
    get("/listFriends") {
        interrup {
            checkSession()
            // TODO: Check perms
            call.respondText(error0(0, content = bot().listFriends()), CONTENT_JSON)
        }
    }
    get("/verboseGroup") {
        interrup {
            checkSession()
            // TODO: Check perms
            val noMembers = call.parameters["noMembers"]?.toBoolean() ?: false
            call.respondText(error0(0, content = group().verbose(noMembers)), CONTENT_JSON)
        }
    }
    get("/verboseMessageSource") {
        interrup {
            checkSession()
            // TODO: Check perms
            val messageSource = parameter("source")
            val msg = messageSourceCache[messageSource] ?: interrupt(
                error0(
                    5,
                    "Message source `${messageSource}` invalidated"
                )
            )
            call.respondText(error0(0, content = msg.verbose()), CONTENT_JSON)
        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.parameter(key: String): String {
    return call.parameters[key] ?: interrupt(error(1, "parameter \$$key not found"))
}

internal fun PipelineContext<Unit, ApplicationCall>.bot(): Bot {
    val botParameter = call.parameters["bot"] ?: interrupt(
        error(1, "parameter \$bot not found")
    )
    return Bot.getInstanceOrNull(
        botParameter.toLongOrNull() ?: interrupt(error(1, "$botParameter is not a number"))
    ) ?: interrupt(error(1, "Bot $botParameter not found"))
}

internal fun PipelineContext<Unit, ApplicationCall>.group(): Group {
    val bot = bot()
    val group = call.parameters["group"] ?: interrupt(
        error(2, "parameter \$group not found")
    )
    return bot.getGroupOrNull(
        group.toLongOrNull() ?: interrupt(error(2, "$group is not a number"))
    ) ?: interrupt(error(1, "Group $group not found in bot ${bot.id}"))
}

/**
 * multi part
 */
internal fun List<PartData>.value(name: String) = try {
    (first { it.name == name } as PartData.FormItem).value
} catch (e: Exception) {
    throw Interrupt(ERROR_INVALID_PARAM)
}

internal fun List<PartData>.valueOrNull(name: String) = try {
    (firstOrNull { it.name == name } as? PartData.FormItem)?.value
} catch (e: Exception) {
    throw Interrupt(ERROR_INVALID_PARAM)
}

internal fun List<PartData>.file(name: String) = try {
    first { it.name == name } as PartData.FileItem
} catch (e: Exception) {
    throw Interrupt(ERROR_INVALID_PARAM)
}
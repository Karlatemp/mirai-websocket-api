/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MsgModel.kt
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.github.karlatemp.miraiwebsocketapi

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadAsImage
import net.mamoe.mirai.utils.ExternalImage
import net.mamoe.mirai.utils.UnstableExternalImage
import net.mamoe.mirai.utils.internal.DeferredReusableInput
import java.net.URI
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

val json = Json { }

val messageSourceCache = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.HOURS)
    .build<String, MessageSource>()

internal operator fun <T, V> Cache<T, V>.get(key: T): V? = getIfPresent(key)
internal operator fun <T, V> Cache<T, V>.set(key: T, value: V) = put(key, value)

internal fun MessageSource.toModel(): MessageSourceModel {
    val id = "MS.T" + System.currentTimeMillis() + ".UID" + UUID.randomUUID()
    messageSourceCache[id] = this
    return MessageSourceModel(id)
}

@OptIn(ExperimentalStdlibApi::class)
private val pokeTypeMappings: Map<Int, PokeMessage> = buildMap {
    PokeMessage.values.forEach { msg ->
        put(msg.type, msg)
    }
}

private fun String.getSource(): MessageSource =
    messageSourceCache[this] ?: error("No MessageSource found with $this")

val downClient = HttpClient {
    BrowserUserAgent()
}

private suspend fun ByteArray.uploadAsImage(source: Contact): Image {
    @OptIn(UnstableExternalImage::class)
    return source.uploadImage(ExternalImage(DeferredReusableInput(this, null)))
}

suspend fun ImageModel.upload(source: Contact?): Image {
    id?.let { return Image(it) }
    if (source == null) error("No source found.")
    val url = this.url ?: error("No imageId or url found.")
    return when (url.substringBefore(':', "")) {
        "file" -> {
            Paths.get(URI(url)).toFile().uploadAsImage(source)
        }
        "http", "https" -> {
            downClient.get<ByteArray>(url).uploadAsImage(source)
        }
        "base64", "b64" -> {
            Base64.getMimeDecoder().decode(url.substringAfter(':')).uploadAsImage(source)
        }
        else -> error("Unknown how to upload $url")
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun MessageChainModel.toChain(source: Contact? = null): MessageChain = buildList<SingleMessage> {
    this@toChain.forEach { elm ->
        val m: SingleMessage = when (elm) {
            AtAllModel -> AtAll
            is PlainModel -> PlainText(elm.msg)
            is AtModel -> {
                val group = source as? Group
                if (group == null) {
                    PlainText("@" + (elm.display ?: elm.target))
                } else {
                    val target = group.getOrNull(elm.target)
                    if (target == null) {
                        PlainText("@" + (elm.display ?: elm.target))
                    } else At(target)
                }
            }
            is FaceModel -> Face(elm.id)
            is ImageModel -> elm.upload(source)
            is FlashImageModel -> FlashImage(elm.image.upload(source))
            is MessageSourceModel -> return@forEach // dropped
            is PokeModel -> pokeTypeMappings[elm.type] ?: error("No poke type found with " + elm.type)
            is VoiceModel -> error("Unsupported send voice.")
            is QuoteModel -> QuoteReply(elm.id.getSource())
            is ServiceModel -> ServiceMessage(elm.id, elm.content)
            is LightAppModel -> LightApp(elm.content)
        }
        add(m)
    }
}.asMessageChain()


@OptIn(ExperimentalStdlibApi::class)
suspend fun MessageChain.toModel(): MessageChainModel = buildList {
    this@toModel.forEach { elm ->
        val model = when (elm) {
            is PlainText -> PlainModel(elm.content)
            is AtAll -> AtAllModel
            is At -> AtModel(elm.target, elm.display)
            is Face -> FaceModel(elm.id)
            is FlashImage -> FlashImageModel(
                ImageModel(
                    id = elm.image.imageId,
                    url = elm.image.queryUrl()
                )
            )
            is Image -> ImageModel(elm.imageId, elm.queryUrl())
            is LightApp -> LightAppModel(elm.content)
            is MessageSource -> elm.toModel()
            is PokeMessage -> PokeModel(elm.name, elm.type)
            is QuoteReply -> QuoteModel(elm.source.toModel().id)
            is ServiceMessage -> ServiceModel(elm.serviceId, elm.content)
            is Voice -> VoiceModel(elm.url, elm.fileName)
            else -> return@forEach
        }
        add(model)
    }
}

typealias MessageChainModel = List<MsgModel>

@Serializable
sealed class MsgModel

@Serializable
@SerialName("AtAll")
object AtAllModel : MsgModel()

@Serializable
@SerialName("PlainText")
data class PlainModel(val msg: String) : MsgModel()

@Serializable
@SerialName("At")
data class AtModel(val target: Long, val display: String? = null) : MsgModel()

@Serializable
@SerialName("Face")
data class FaceModel(val id: Int) : MsgModel()

@Serializable
@SerialName("Image")
data class ImageModel(
    val id: String? = null,
    val url: String? = null
) : MsgModel()

@Serializable
@SerialName("FlashImage")
data class FlashImageModel(val image: ImageModel) : MsgModel()

@Serializable
@SerialName("MessageSource")
data class MessageSourceModel(
    val id: String // 由 mirai-websocket-api 维护
) : MsgModel()

@Serializable
@SerialName("Poke")
data class PokeModel(
    val name: String? = null,
    val type: Int
) : MsgModel()

@Serializable
@SerialName("Voice")
data class VoiceModel(
    val url: String?,
    val filename: String
) : MsgModel()

@Serializable
@SerialName("Quote")
data class QuoteModel(val id: String) : MsgModel()

@Serializable
@SerialName("Service")
data class ServiceModel(val id: Int, val content: String) : MsgModel()

@Serializable
@SerialName("LightApp")
data class LightAppModel(val content: String) : MsgModel()

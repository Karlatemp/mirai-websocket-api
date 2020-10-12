/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MiraiWebsocketApiSettings.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import com.google.common.cache.CacheBuilder
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.util.concurrent.TimeUnit

@OptIn(ConsoleExperimentalApi::class)
object MiraiWebsocketApiSettings : AbstractPluginData(), PluginConfig {
    override val saveName: String
        get() = "config"
    val host by value("0.0.0.0")
    val port by value(7247)
    val user by value("root")
    val passwd by value(
        "ROOT"
        //"INITKEY" + UUID.randomUUID().toString().replace("-", "")
    )
    val messageSourceCache by value(CacheSetting())
    val replyCache by value(CacheSetting())
    val imageCache by value(CacheSetting())
    val receiptCache by value(CacheSetting())
    val prettyPrint by value(false)

    @Serializable
    data class CacheSetting(
        val maximumSize: Long = 2000L,
        val expireTime: Long = 2,
        val expireTimeUnit: String = "HOURS"
    ) {
        fun <K, V> CacheBuilder<K, V>.buildCache(
            path: String
        ): CacheBuilder<K, V> {
            val logger = MiraiWebsocketApi.logger
            logger.info("Cache[$path] setting:")
            return this.let { builder ->
                if (expireTime == 0L) {
                    logger.info("    Expire time: NO")
                    builder
                } else {
                    logger.info("    Expire time: $expireTime $expireTimeUnit")
                    builder.expireAfterWrite(expireTime, TimeUnit.valueOf(expireTimeUnit))
                }
            }.let { builder ->
                logger.info("    Maximum size: $maximumSize")
                if (maximumSize == 0L)
                    builder
                else
                    builder.maximumSize(2000)
            }
        }
    }

    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
    }

    override fun onValueChanged(value: Value<*>) {
    }
}

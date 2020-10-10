/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MiraiWebsocketApiSettings.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

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
    val cache by value(CacheSetting())

    @Serializable
    data class CacheSetting(
        val maximumSize: Long = 2000L,
        val expireTime: Long = 2,
        val expireTimeUnit: String = "HOUR"
    )

    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
    }

    override fun onValueChanged(value: Value<*>) {
    }
}

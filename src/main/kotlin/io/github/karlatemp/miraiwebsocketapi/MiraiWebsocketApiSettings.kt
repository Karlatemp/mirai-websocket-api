/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/MiraiWebsocketApiSettings.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group

@OptIn(ConsoleExperimentalApi::class)
object MiraiWebsocketApiSettings : AbstractPluginData(), PluginConfig {
    override val saveName: String
        get() = "config.yml"
    val host by value("0.0.0.0")
    val port by value(7247)

    @Serializable
    data class ChannelSetting(
        val name: String,
        val passcode: String,
        val rules: List<String>
    )

    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
    }

    override fun onValueChanged(value: Value<*>) {
    }
}

sealed class ActionType {
    interface WithBot {
        val bot: Bot
    }

    interface WithGroup {
        val group: Group
    }

    sealed class SendMessage : ActionType(), WithBot {
        class SendMessageToGroup(
            override val bot: Bot,
            override val group: Group
        ) : SendMessage(), WithGroup

        class SendMessageToFriend(
            override val bot: Bot,
            val friend: Friend
        ) : SendMessage()

        class SendMessageToTemp(
            override val bot: Bot
        ) : SendMessage()
    }
}
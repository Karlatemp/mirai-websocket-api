/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ovk.kt
 */

import io.github.karlatemp.miraiwebsocketapi.json
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class W

@Serializable
@SerialName("I")
object Q : W() {
    @Serializable
    @Required
    val k: String
        get() = "SHIT"
}

@Serializable
class T {
    @Serializable
    val w: String get() = ""
}

fun main() {
    println(json.encodeToJsonElement(T.serializer(), T()))
}

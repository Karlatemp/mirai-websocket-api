/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/10 21:28:37
 *
 * mirai-websocket-api/SerializerUtil.kt
 */

package io.github.karlatemp.miraiwebsocketapi.actions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder

@ExperimentalSerializationApi
fun CompositeEncoder.encodeNullableString(
    descriptor: SerialDescriptor,
    index: Int,
    value: String?
) {
    if (value != null || shouldEncodeElementDefault(descriptor, index))
        encodeNullableSerializableElement(descriptor, index, String.serializer(), value)
}

@Suppress("UNCHECKED_CAST")
internal inline val <T> KSerializer<T>.nullable: KSerializer<T?>
    get() = this as KSerializer<T?>

@ExperimentalSerializationApi
fun CompositeDecoder.decodeNullableString(
    descriptor: SerialDescriptor,
    index: Int,
    prev: String?
): String? {
    return decodeNullableSerializableElement(descriptor, index, String.serializer().nullable, prev)
}
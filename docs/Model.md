
## 数据结构
### FriendModel
```json5
{
  "id": 123456789, // [LONG],
  "nick": "鸽子", // [STRING], 用户昵称
}
```

### MemberModel
```json5
{
  "id": 123456789, // [LONG],
  "nick": "鸽子", // [STRING], 用户昵称
  "nameCard": "不要加我好友，不要发私聊", // [STRING], 群名片, 可能为空
  "nameCardOrNick": "不要加我好友，不要发私聊",
  "permission": "administrator", // [STRING ENUM] 权限, administrator = 管理员, owner = 群主, member = 群员
  "specialTitle": "specialTitle" // [String] 群头衔
}
```

### GroupModel
```json5
{
  "id": 123456789, // [LONG], 群号
  "name": "Mamoe Tech", // [STRING] 群名
  "botPermission": "member" // [STRING ENUM] Bot权限, administrator = 管理员, owner = 群主, member = 群员
}
```
## 信息结构体
### MessageChain
```json5
// 一个 Message Chain
[
{"type": "MessageSource", "id": "MESSAGE_SOURCE_ID"},
{"type": "Plain", "msg": "我永远喜欢him188moe"}
]
```
### MessageSource
MessageSource由 WS API 进行维护
```json5
{"type": "MessageSource", "id": "MsgId"} // id由 WS API维护
```
MessageSource id 可用于回复和撤回消息


### PlainText
```json5
{"type": "PlainText", "msg": "纯文本!"}
```

### At
```json5
{
  "type": "At", 
  "target": 123456789,
  "display": ""
}
```

### AtAll
```json5
{"type": "AtAll"} // @全体成员
```

### Face
```json5
{"type": "Face", "id": 123}
```
Also See: [Face.kt](https://github.com/mamoe/mirai/blob/master/mirai-core/src/commonMain/kotlin/net.mamoe.mirai/message/data/Face.kt)

### Image
```json5
{
  "type": "Image",
  "id": "", // 图片id
  "url": "http://404notfound.com"
}
```
url说明

- url在从 WS API 发送到对接程序的时候, 一定是一个有效的url
- url在从程序发送回 WS API的时候, 支持以下协议
    - file://.....
    - http://....
    - https://....
    - base64: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

### FlashImage
*闪照*
```json5
{"type": "FlashImage", "image": {
  "type": "Image",
  //....
}}
```

### Poke
```json5
{
  "type":"Poke",
  "pokeType": 1, // 对应 PokeMessage.type, 由 mirai-core 维护
  "name": "戳一戳"
}
```
Also see: [PokeMessage](https://github.com/mamoe/mirai/blob/a774b8a7062fbd26742b7eb73896294fa6ab01b0/mirai-core/src/commonMain/kotlin/net.mamoe.mirai/message/data/HummerMessage.kt#L64)

### Voice
仅支持接受， 不支持发送
```json5
{
  "type": "Voice",
  "url:": "http://404notfound.com",
  "filename": ""
}
```

### Quote
回复信息
```json5
{
"type": "Quote",
"id": "MESSAGE_SOURCE_ID"
}
```

### LightApp
```json5
{
"type": "LightApp",
"content": "....."
}
```
Also see: [LightApp](https://github.com/mamoe/mirai/blob/a774b8a7062fbd26742b7eb73896294fa6ab01b0/mirai-core/src/commonMain/kotlin/net.mamoe.mirai/message/data/RichMessage.kt#L96)

### Service
```json5
{
"type": "Service",
"id": 123,
"content": "...."
}
```
Also see: [ServiceMessage](https://github.com/mamoe/mirai/blob/a774b8a7062fbd26742b7eb73896294fa6ab01b0/mirai-core/src/commonMain/kotlin/net.mamoe.mirai/message/data/RichMessage.kt#L114)

## 事件列表

### GroupMessage
```json5
{
  "type": "GroupMessage",
  "group" : {}, // [GroupModel]
  "sender": {}, // [MemberModel]
  "replyKey": "RANDOM REPLY KEY", // [STRING], 用于回复的 key
  "message": [ // [MessageChain]
    {"type": "MessageSource", "id": "ABCDEF"},
    {"type": "Plain", "msg": "我永远喜欢hso188moe"}
  ],
  "bot": 123456789 // [LONG]
}
```
### FriendMessage
```json5
{
  "type": "FriendMessage",
  "sender": {}, // [FriendModel]
  "message": [], // [MessageChain]
  "replyKey": "RANDOM REPLY KEY", // [STRING], 用于回复的 key
  "bot": 123456789 // [LONG]
}
```
### TempMessage
```json5
{
  "type": "TempMessage",
  "group": {}, // [GroupModel]
  "sender": {}, // [FriendModel]
  "message": [], // [MessageChain]
  "replyKey": "RANDOM REPLY KEY", // [STRING], 用于回复的 key
  "bot": 123456789 // [LONG]
}
```
注: 要回复 TempMessage 只能通过 ReplyMessage

## 操作列表

### ReplyMessage
回复一条信息
```json5
{
  "type": "Reply",
  "content": {  
    "id": "REPLY ID", // 通过 GroupMessage/FriendMessage/TempMessage获取到的 replyKey
    "message": [], // [MessageChain]
  }
}
```
操作返回:

| key       | desc |
| -----     | ----- |
| receiptId | 可用于 [RecallReceipt](#RecallReceipt) |
| sourceId  | MESSAGE SOURCE ID |

### SendToGroup
主动发送信息到一个群组
```json5
{
  "type": "SendToGroup",
  "content": {
    "bot": 123456789, // [LONG] BOT id, 必须
    "group": 123456789, // [LONG] 群号, 必须
    "message": [], // [MessageChain]
  }
}
```
操作返回:

| key       | desc |
| -----     | ----- |
| receiptId | 可用于 [RecallReceipt](#RecallReceipt) |
| sourceId  | MESSAGE SOURCE ID |

### SendToFriend
主动发送信息到一个群组
```json5
{
  "type": "SendToGroup",
  "content": {
    "bot": 123456789, // [LONG] BOT id, 必须
    "friend": 123456789, // [LONG] 好友QQ号, 必须
    "message": [] // [MessageChain]
  }
}
```
操作返回:

| key       | desc |
| -----     | ----- |
| receiptId | 可用于 [RecallReceipt](#RecallReceipt) |
| sourceId  | MESSAGE SOURCE ID |

### RecallReceipt
撤回机器人发送的消息
```json5
{
"type": "RecallReceipt",
"content":{ "receipt": "RECEIPT ID" }
}
```
### Recall
撤回任意消息
```json5
{
"type": "Recall",
"content":{"messageSource": "MESSAGE SOURCE ID"} // 可以通过 SendToXXX 或者 XXXMessage的message中的MessageSource 获取
}
```
### MuteMember
禁言群成员
```json5
{
"type": "MuteMember",
  "content": {
    "group": 123456789, // [LONG] 群号
    "member": 987654321, // 群成员QQ号
    "time": 60 // 单位: 秒
  }
}
```

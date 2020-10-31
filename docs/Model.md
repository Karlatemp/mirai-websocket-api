
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
{"type": "Plain", "text": "我永远喜欢him188moe"}
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
{"type": "Plain", "text": "纯文本!"}
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
    - base64:\[图片数据的Base64]
        - base64:iVBORw0KGgoAA......

### FlashImage
*闪照*
```json5
{"type": "FlashImage", "image": {
  //....
}}
```
其中 Image 为 [Image](#Image), 且无 `type` 字段

```json5
{"type": "FlashImage", "image": {
  "url": "https://404notfound.com/notfound.jpg"
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
    {"type": "Plain", "text": "我永远喜欢hso188moe"}
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
注: 要回复 TempMessage 只能通过 [ReplyMessage](#ReplyMessage)

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
### ListGroups
获取 bot 的所在群列表
```json5
{
"type": "ListGroups",
"content": {
  "bot": 123456789 // [LONG] Bot id
}
}
```
操作返回:
*自己试试不就有了*

### ListFriends
```json5
{
"type": "ListFriends",
"content": {
  "bot": 123456789 // [LONG] Bot id
}
}
```
操作返回:
*自己试试不就有了*

### GroupVerbose
```json5
{
"type": "GroupVerbose",
"content": {
  "bot": 123456789, // [LONG] Bot id
  "group": 987654321, // [LONG] group id
  "noMembers": true // [Optional] [BOOLEAN] 是否不获取 members, 默认 true
}
}
```
操作返回:
*自己试试不就有了*

### VerboseMessageSource
```json5
{
"type": "VerboseMessageSource",
"content": {
  "messageSource": "[MESSAGE SOURCE]" // Message Source
}
}
```
操作返回:
*自己试试不就有了*


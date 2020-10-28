**Mirai WebSocket API 不提供 http api, 仅提供WS API**

以下使用 WS API 简称 Mirai WebSocket API

# 建立连接

启动 mirai-console 时, WS API 会打印出服务器运行地址, 以及连接账号信息.

通过与 `ws://localhost:7247/` 建立连接, 就能够开始使用 WS API

## 鉴权

使用 WS API, 需要完成鉴权, 鉴权不复杂, 只需要在建立 ws 连接后直接把账户和密码发过去即可
```js
var socket = new WebSocket("ws//localhost:7247/");
socket.send("USER");
socket.send("PASSWD");
// ....
```
注: 发送密码后 WS API 会发送一个 [ActionResult](#ActionResult), 无 id
```json5
{
    "type": "ActionResult",
    "success": true,
    "result": {
        "content": {
            "session": "NEW.WEBSOCKET.SESSION" // 建立的 session id, 可用于 http api 鉴权
        }
    }
}
```
Also see [Http API](http.md)

## 发送请求
发送给WS API的数据, 均满足以下格式
```json5
{
  "type": "REQUEST TYPE", // 发送的请求类型
  "requestId": "", // 请求ID, 由客户端自行指定
  "content": {
    // ...
  }
}
```
Also see: [Request Actions](Model.md#操作列表)

**session** 可用于 WS API 提供的 [http api](http.md) 的鉴权

## 解析数据

### ActionResult
```json5
{
  "type": "ActionResult",
  "id": "", // 客户端发送的识别 ID
  "success": false,
  "result": {
    "error": "",
    "errorDetail": "",
    "content": {} // 注: 对应操作没有写明操作返回时不存在 content
  }
}
```

### Event
```json5
{
  "type": "Event",
  "event": {} // [EventModel]
}
```
Also see: [EventModel](Model.md#事件列表)


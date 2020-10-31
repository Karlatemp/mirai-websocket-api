# 鉴权

**本章节的 Http API 均为 WS API提供的http接口, 并非 maira-api-http 插件**

要使用 Http API, 必须先先行建立一个 [Web Session](API.md#鉴权), 得到 `session`

## 请求参数鉴权
```text
/example/api?session=SESSION.SESSION
```

## HTTP Header 鉴权
```text
GET /example/api
WS-API-SESSION: SESSION.SESSION

```

# APIs

## `/listGroups`
`GET /listGroups?bot={bot}`


## `/listFriends`
`GET /listFriends?bot={bot}`


## `/verboseGroup`
`GET /listFriends?bot={bot}&group={group}`

## `/verboseMessageSource`
`GET /verboseMessageSource?source={source}`

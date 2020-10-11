<div align="center">
   <img width="160" src="http://img.mamoe.net/2020/02/16/a759783b42f72.png" alt="logo"></br>

   <img width="95" src="http://img.mamoe.net/2020/02/16/c4aece361224d.png" alt="title">

----

[![Gitter](https://badges.gitter.im/mamoe/mirai.svg)](https://gitter.im/mamoe/mirai?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Actions Status](https://github.com/mamoe/mirai-api-http/workflows/Gradle%20CI/badge.svg)](https://github.com/mamoe/mirai-api-http/actions)

Mirai 是一个在全平台下运行，提供 QQ Android 和 TIM PC 协议支持的高效率机器人框架

这个项目的名字来源于
     <p><a href = "http://www.kyotoanimation.co.jp/">京都动画</a>作品<a href = "https://zh.moegirl.org/zh-hans/%E5%A2%83%E7%95%8C%E7%9A%84%E5%BD%BC%E6%96%B9">《境界的彼方》</a>的<a href = "https://zh.moegirl.org/zh-hans/%E6%A0%97%E5%B1%B1%E6%9C%AA%E6%9D%A5">栗山未来(Kuriyama <b>Mirai</b>)</a></p>
     <p><a href = "https://www.crypton.co.jp/">CRYPTON</a>以<a href = "https://www.crypton.co.jp/miku_eng">初音未来</a>为代表的创作与活动<a href = "https://magicalmirai.com/2019/index_en.html">(Magical <b>Mirai</b>)</a></p>
图标以及形象由画师<a href = "">DazeCake</a>绘制
</div>

# mirai-websocket-api
Mirai WebSocket API (console) plugin

<b>Mirai-WebSocket-Api 插件 提供WebSocket API供所有语言使用mirai</b>



## 开始使用
0. 请首先运行[Mirai-console](https://github.com/mamoe/mirai-console)相关客户端生成plugins文件夹
1. 将`mirai-websocket-api`生成的`jar包文件`放入`plugins`文件夹中
2. 编辑`config/MiraiWSApi/config.yml`配置文件 (没有则自行创建)
3. 再次启动[Mirai-console](https://github.com/mamoe/mirai-console)相关客户端
4. 记录日志中出现的 `user` 和 `passwd`


#### config.yml模板

```yaml
## 该配置为全局配置，对所有Session有效

# 可选，默认值为0.0.0.0
host: '0.0.0.0'

# 可选，默认值为7247
port: 7247

# 可选, 鉴权系统使用的用户名
user: root

# 必须, 默认密码为 ROOT, 必须修改
passwd: ROOT

# 是否在广播信息时使用 pettyPrint
pettyPrint: false

replyCache:
  maximumSize: 2000
  expireTime: 2
  expireTimeUnit: HOURS

messageSourceCache:
  maximumSize: 2000
  expireTime: 2
  expireTimeUnit: HOURS

receiptCache:
  maximumSize: 2000
  expireTime: 2
  expireTimeUnit: HOURS

```

## 更新日志
[点我查看](CHANGELOG.md)

## 文档

* **[API文档参考](docs/API.md)**
* [数据结构参考](docs/Model.md)
* [示例使用](src/test/kotlin/ShitListing.kt)
* [Proto](proto/proto.proto)

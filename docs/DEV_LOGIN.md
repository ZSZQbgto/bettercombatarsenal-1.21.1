# 开发环境：用正版账号登录（IDEA / Gradle）

普通 `Client` 运行配置使用的是离线开发账号（`Dev`，UUID `380df991-f603-344c-a090-369bad2a924a`），
不能加入正版验证的服务器，皮肤/账号信息也不对。

项目另外提供了一个 `clientAuth` 运行配置：启动时先用
[DevLogin](https://github.com/covers1624/DevLogin) 登录微软账号，再把真实的
`--username / --uuid / --accessToken / --userType msa` 传给游戏。它由 ModDevGradle 内置的
`devLogin` 支持实现，只在开发环境生效，打包发布时不会带上。

## 使用步骤

1. 在 IDEA 里重新加载 Gradle 项目（Gradle 工具窗口的刷新按钮，或构建脚本变更后提示里的 Reload）。
2. 运行配置下拉框里选择 **`Client (正版登录)`**，正常运行即可。
   - 不想走 IDEA 时，命令行等价：`gradlew runClientAuth`
3. 首次启动终端会打印：

   ```
   [DevLogin]: To sign in, use a web browser to open the page https://www.microsoft.com/link and enter the code XXXXXXXXX to authenticate.
   ```

   打开该网页、输入代码，并用**拥有 Minecraft 的微软账号**完成登录；随后游戏自动启动。
4. 之后每次启动都会用缓存的令牌自动登录，过期时自动刷新，通常不需要再操作。

## 说明

- 令牌保存在 `%USERPROFILE%\.devlogin\`（明文）。如需换位置，加 JVM 参数
  `-Ddevlogin.storage=<目录>`。
- 多账号：加程序参数 `--launch_profile <名字>`（不写时为 `default`），或用系统属性
  `-Ddevlogin.launch_profile=<名字>`。
- 依赖：`net.covers1624:DevLogin:0.1.0.5`，发布在 covers1624 的 Nexus 上，
  已在 `build.gradle` 的 `repositories` 里按 `net.covers1624` 分组加入。
- 用真实账号登录后 UUID 与离线 `Dev` 账号不同，`run/saves` 里原来离线存档的玩家数据不会继承，
  联机测试请新开存档或直接进服务器。
- 微软登录接口访问不畅时，可以打开 `build.gradle` 里 `clientAuth` 下的
  `-Dhttps.proxyHost` / `-Dhttps.proxyPort` 两行注释（端口与本项目 `gradle.properties` 的代理一致）。
- 普通 `Client` 运行配置保持离线模式不变，可随时切回。

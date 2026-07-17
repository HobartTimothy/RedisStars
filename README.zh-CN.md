# RedisStars

[English](./README.md) | **简体中文**

基于 **Kotlin** 与 **Compose Desktop** 的跨平台 Redis 桌面客户端。直连 Redis，浏览键、查看元数据，并读写字符串、哈希、列表、集合与有序集合等数据类型。

RedisStars 现为**纯桌面应用**。原有的 Server、Web、HTTP API 与远程客户端已移除。

## 功能特性

- **连接配置** — 支持 Standalone、Sentinel、Cluster 三种部署模式
- **SSH 隧道** — Standalone 连接可选本地端口转发
- **键浏览** — 模式扫描、TTL、重命名、删除
- **值编辑** — 查看与编辑常见 Redis 数据类型
- **TLS** — 启用 TLS 时默认开启对等证书校验
- **本地持久化** — 连接配置与应用设置保存在仓库外的本地目录
- **安装包** — 通过 jpackage 生成 Windows、Linux、macOS 原生安装程序

## 环境要求

| 用途 | 要求 |
|------|------|
| 源码构建与运行 | JDK **17+**、Gradle Wrapper |
| Windows EXE / MSI | [WiX Toolset](https://wixtoolset.org/)（jpackage 依赖） |
| Linux DEB | `fakeroot`、`dpkg` |
| Linux RPM | `rpm-build` / `rpmbuild` |
| 集成测试 | Docker（Testcontainers；不可用时自动跳过） |

Compose Desktop **不支持交叉编译** — 请在目标操作系统上构建对应安装包。

## 快速开始

**Windows（PowerShell）**

```powershell
.\gradlew.bat :app:desktopApp:run
```

**Linux / macOS**

```bash
./gradlew :app:desktopApp:run
```

UI 开发时可使用热重载：

```bash
./gradlew :app:desktopApp:hotRun --auto
```

本地 Redis 环境配置见 [Redis 测试环境](./docs/redis-test-environments.md)。

## 安装包构建

打包产物输出目录：`app/desktopApp/build/compose/binaries/`。

| 操作系统 | 格式 | Gradle 任务 |
|----------|------|-------------|
| Windows | `.exe`、`.msi` | `:app:desktopApp:packageExe`、`:app:desktopApp:packageMsi` |
| Linux | `.deb`、`.rpm` | `:app:desktopApp:packageDeb`、`:app:desktopApp:packageRpm` |
| macOS | `.dmg`、`.pkg` | `:app:desktopApp:packageDmg`、`:app:desktopApp:packagePkg` |

**常用任务**

```bash
# 当前操作系统对应的安装包
./gradlew :app:desktopApp:packageDistributionForCurrentOS

# 解压式应用目录（无安装程序）
./gradlew :app:desktopApp:createDistributable
./gradlew :app:desktopApp:runDistributable

# 当前操作系统的 Fat JAR
./gradlew :app:desktopApp:packageUberJarForCurrentOS
```

**Windows**

```powershell
.\gradlew.bat :app:desktopApp:packageExe
.\gradlew.bat :app:desktopApp:packageMsi
```

**Linux**

```bash
./gradlew :app:desktopApp:packageDeb
./gradlew :app:desktopApp:packageRpm
```

若打包后运行时出现 `ClassNotFoundException`，请执行 `:app:desktopApp:suggestModules`，并在 `app/desktopApp/build.gradle.kts` 的 `nativeDistributions { modules(...) }` 中添加建议的 JDK 模块。

## JVM 运行参数

打包后的 RedisStars 通过 jpackage 原生启动器配置文件 **`RedisStars.cfg`** 读取 JVM 启动参数。原生启动器在 JVM 创建**之前**应用这些选项 — 堆大小、GC 等 `-X`/`-XX` 参数无法在应用代码中修改。

默认参数（定义于 `app/desktopApp/build.gradle.kts`）：

| 参数 | 说明 |
|------|------|
| `-Xms256m` | 初始堆大小 |
| `-Xmx2g` | 最大堆大小 |
| `-XX:+UseG1GC` | G1 垃圾回收器 |
| `-Dfile.encoding=UTF-8` | 文件编码 |

**配置文件路径**

| 分发方式 | 路径 |
|----------|------|
| 解压式目录（`createDistributable`） | `app/desktopApp/build/compose/binaries/main/app/RedisStars/app/RedisStars.cfg` |
| Windows EXE / MSI（安装后） | `<安装目录>\app\RedisStars.cfg` |
| Linux RPM / DEB（安装后） | `/opt/redis-stars/lib/app/RedisStars.cfg` |

Windows 下 `<安装目录>` 为安装时选择的目录。应用数据单独存放在 `%APPDATA%\RedisStars`，**不是** JVM 配置文件。

调整最大堆内存示例 — 编辑 `[JavaOptions]` 段：

```ini
java-options=-Xmx4g
```

保存后完全退出 RedisStars 并重新启动。仅修改 `[JavaOptions]`，勿改动 `[Application]` 与 classpath 条目。修改前请备份。**安装程序升级可能会覆盖**自定义配置。

完整说明：[docs/jvm-options.md](./docs/jvm-options.md)

**验证打包默认参数**

```powershell
.\gradlew.bat :app:desktopApp:createDistributable
.\gradlew.bat :app:desktopApp:verifyPackagedJvmOptions
```

## 项目结构

```
RedisStars/
├── core/              领域模型、校验、用例、端口定义
├── redis-jvm/         Lettuce 适配器（Standalone、Sentinel、Cluster）
├── app/shared/        Compose UI、ViewModel、国际化
└── app/desktopApp/    入口、组合根、打包配置
```

| 模块 | 职责 |
|------|------|
| [`core`](./core/src) | 领域逻辑与 Redis/持久化抽象 |
| [`redis-jvm`](./redis-jvm/src) | 基于 Lettuce 的 Redis 客户端 |
| [`app/shared`](./app/shared/src) | 共享 Compose UI 与展示层 |
| [`app/desktopApp`](./app/desktopApp/src) | 桌面启动与平台适配 |

## 配置与安全

**应用数据**（连接配置、设置）保存在仓库外：

| 操作系统 | 路径 |
|----------|------|
| Windows | `%APPDATA%\RedisStars` |
| Linux / macOS | `~/.config/redis-stars` |

**密码** 仅在启用「记住密码」时保存。此时 JSON 存储中包含**明文凭证** — 请保护操作系统账户，勿同步或提交该文件。

启用 TLS 时，**对等证书校验** 默认开启。

## 开发

**运行测试**

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :redis-jvm:test
.\gradlew.bat :app:shared:jvmTest
.\gradlew.bat :app:desktopApp:compileKotlin
```

**打包验证**

```powershell
.\gradlew.bat :app:desktopApp:createDistributable
.\gradlew.bat :app:desktopApp:verifyPackagedJvmOptions
```

`redis-jvm` 模块通过 Testcontainers 使用临时 `redis:7-alpine` 容器。Docker 不可用时测试会自动跳过。

## 文档

| 文档 | 内容 |
|------|------|
| [docs/jvm-options.md](./docs/jvm-options.md) | JVM 启动器配置、编辑规则、升级行为 |
| [docs/redis-test-environments.md](./docs/redis-test-environments.md) | 本地 Redis、Sentinel、Cluster 环境 |

## 许可证

[MIT](./LICENSE) — Copyright (c) 2026 RobertHU

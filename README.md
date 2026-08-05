# 想法捕捉助手（ThoughtCapture）

一个让你随时记录想法、制定计划、跟踪复习、管理健身的 Android App——手机记录，GitHub 同步，AI Agent 自动处理并回复。

## 能做什么

把手机当成大脑的延伸：冒出什么念头立刻记下来，Agent 帮你分类整理；早上说一句今天的安排，自动生成结构化计划；每天推送错题复习、申论范文、常识速记；健身训练打卡+热量追踪，做完一组勾一组。

### 五个模块

| 模块 | 干什么 |
|------|--------|
| **捕捉** | 文字/语音/拍照记录想法，三档模式切换——记想法、做计划、交练习 |
| **收件箱** | 所有想法的汇总，Agent 处理完会显示"已整理"并附上回复 |
| **复习** | 每日推送的错题、范文、常识、笔记，可打勾，按日期查看 |
| **计划** | 每日学习+生活安排，早上/下午/晚上分时段，带复选框 |
| **运动** | 健身训练计划+热量日志+饮食记录，有"问"按钮直接和 Agent 沟通调整方案 |

## 怎么工作的

```
手机记录 → GitHub 同步 → Agent 自动处理 → 回复推回手机
```

1. 在手机上写/说一条想法，保存
2. App 自动 push 到你的 GitHub 私有仓库
3. PC 端 Claude Code Agent 每隔几分钟拉取新条目
4. 自动分类（考公/技术/健身/其他），生成回复
5. 手机刷新收件箱，看到处理结果和回复
6. 复习内容、计划、健身数据同步到 Obsidian 知识库

## 技术栈

- **前端**：Kotlin + Jetpack Compose + Material 3
- **数据**：Room (SQLite) + 文件系统双源
- **同步**：JGit 操作 GitHub 仓库
- **Agent**：Python 脚本 + Claude Code 协作
- **通知栏**：TileService 快捷开关
- **桌面小组件**：Glance App Widget
- **语音**：Android SpeechRecognizer
- **图片**：CameraX + Coil

## 构建安装

用 Android Studio 打开项目，Gradle Sync 后：

```bash
# 生成签名
keytool -genkey -v -keystore thought-capture.keystore -alias thoughtcapture -keyalg RSA -keysize 2048 -validity 10000

# 构建
./gradlew assembleRelease

# 安装
adb install app/build/outputs/apk/release/app-release.apk
```

首次打开需要配置：
1. 在 GitHub 创建私有仓库（用于数据同步，不是代码仓库）
2. 生成 Personal Access Token（勾选 repo 权限）
3. 在 App 设置页填入仓库地址和 Token

## 项目结构

```
thought-capture-app/
├── app/src/main/java/com/thoughtcapture/app/
│   ├── data/          # Room 数据库层
│   │   ├── entity/    # ThoughtEntry 实体
│   │   ├── dao/       # DAO 接口
│   │   ├── database/  # AppDatabase
│   │   └── repository/# ThoughtRepository
│   ├── sync/          # GitSyncManager (JGit)
│   ├── service/       # TileService + VoiceRecognition
│   ├── ui/            # Compose UI
│   │   ├── capture/   # 捕捉页
│   │   ├── inbox/     # 收件箱
│   │   ├── review/    # 复习
│   │   ├── plan/      # 计划
│   │   ├── fitness/   # 运动
│   │   ├── detail/    # 想法详情
│   │   ├── setup/     # 首次配置
│   │   ├── navigation/# 底部导航
│   │   └── theme/     # Material3 主题
│   ├── widget/        # 桌面小组件
│   └── util/          # 工具类
├── gradle/            # Gradle 配置
└── BUILD.md           # 构建详细说明
```

## 配套的 Agent 端

Agent 端 `.claude/skills/idea-processor/` 负责：

- `check_ideas.py` — 定时拉取+扫描收件箱
- `process_ideas.py` — 分类处理条目、生成回复
- `sync_to_obsidian.py` — 同步到 Obsidian 知识库

需要配合 `~/.claude/idea-mappings.json` 配置分类关键词。

## 开发背景

我是一名计算机专业学生，正在备考 2026 年国考。这个 App 最初的动机很简单——在手机上随时记下学习中的问题和想法，不用打开电脑。后来慢慢加上了计划管理、复习推送、健身跟踪，变成了一个综合性的个人管理系统。

整个开发过程是和 Claude Code 协作完成的：我描述需求，Claude 写代码、编译、安装到手机。从想法到可用的 App，全程没有手写一行代码。

## License

MIT

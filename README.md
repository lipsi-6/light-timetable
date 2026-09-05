# 轻量课程表

> Android 12+ (minSdk 31) · Jetpack Compose · Room · Glance Widget · 纯离线

详见 `docs/课程表软件需求与设计文档.md`

## 快速开始

1. 用 Android Studio Hedgehog+ 打开项目根目录 `D:\Me\My Project\课程表`
2. 修改 `local.properties` 中 `sdk.dir` 为本机 SDK 路径
3. 点击 Run 或执行 `./gradlew assembleDebug`，安装 `app/build/outputs/apk/debug/app-debug.apk`

## 核心特性
- 纵向时间、横向7天周视图，时间轴范围用户可设（默认 08:00-22:00），60分钟一格但课程按分钟比例渲染
- 课程按 `courseName` 归组为同一门课，支持多阶段（不同星期/时间/重复规则），同色、同名即同课
- 重复规则：每周 / 单双周 / 间隔N周 + 截止周 / 重复次数 二选一
- 非本周灰显（ alpha 0.35 + “非本周” 前缀），可切换隐藏
- 冲突叠放：重叠课程错位露出边缘，点击切换置顶；长按编辑
- 学期总览：按课程名列表，点击查看该课程所有阶段
- 小部件：显示今日（过滤已结束）+ 明日课程，支持拉伸
- 纯离线 Room 存储，JSON 完整备份/恢复（SAF 文件选择器），预留 CSV

## 构建要求
- JDK 17
- Android Gradle Plugin 8.3.2, Kotlin 1.9.22
- compileSdk 34, targetSdk 34

## 目录
```
app/src/main/java/com/example/timetable
 ├─ data/db  Room 实体/DAO
 ├─ data/repo Repository
 ├─ ui/week  周视图
 ├─ ui/edit  编辑页
 ├─ ui/overview 总览
 ├─ ui/settings 设置/学期/备份
 ├─ widget Glance 小部件
 └─ util WeekCalculator, RepeatExpander
```

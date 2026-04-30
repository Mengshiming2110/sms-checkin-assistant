# 短信打卡助手

监听短信/钉钉通知并自动打开钉钉完成极速打卡的 Android 应用。

## 功能特性

- **双触发机制**：短信关键字匹配 + 钉钉通知关键字匹配（备用）
- **多关键字支持**：可配置多个触发关键字，满足任一即触发
- **发送者白名单**：限定仅特定号码的短信可触发
- **可配置延迟**：收到触发消息后延迟 0-30 秒再打开钉钉
- **冷却机制**：30 秒内多次触发仅执行一次，防止重复打卡
- **前台常驻服务**：持久通知保证应用不被系统杀死
- **通知栏快捷开关**：常驻通知内置「暂停服务」按钮
- **控制中心磁贴**：下拉面板添加磁贴一键开关服务
- **开机自启动**：重启后自动恢复前台服务 + 重新调度每日报告
- **每日报告**：每天 21:00 推送通知汇总当日打卡次数
- **服务保活**：WorkManager 每 15 分钟检查并自动拉活被杀服务
- **打卡时间段**：可设置上班/下班打卡时间段，其余时间自动休眠省电
- **触发失败告警**：设定截止时间，未打卡时推送提醒
- **桌面小组件**：显示今日打卡次数，支持一键开关服务
- **首次使用引导**：每次启动弹出使用说明，涵盖权限、配置步骤

## 使用方法

1. 安装 APK
2. 授予短信权限
3. 确保钉钉已开启极速打卡功能（设置 → 考勤打卡 → 极速打卡）
4. 进入设置（Toolbar 齿轮图标 或 点击「服务状态」卡片）
5. 按需配置关键字、白名单、通知监听、打卡时间段、延迟、打卡提醒等
6. 在主界面打开服务开关
7. 也可通过通知栏按钮或控制中心磁贴控制服务

## 触发逻辑

```
收到短信/钉钉通知
  → 检查服务是否启用
  → 检查是否在打卡时间段内
  → 检查发送者是否在白名单（仅短信）
  → 检查内容是否匹配关键字
  → 冷却检查（30s 内不重复触发）
  → 记录触发日志（内容截断至 120 字符）
  → 延迟后打开钉钉
```

## 项目结构

```
SmsCheckInApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/pengxh/smscheckin/
│   │   │   ├── MainActivity.kt                  # 主 Activity，Fragment 容器 + Toolbar
│   │   │   ├── MainFragment.kt                  # 主界面：权限状态、服务开关、统计数据、触发日志
│   │   │   ├── SettingsFragment.kt              # 设置：关键字、白名单、通知监听、延迟、打卡时间段、打卡提醒、电池优化、悬浮窗
│   │   │   ├── SmsReceiver.kt                   # 短信广播接收器，核心触发逻辑
│   │   │   ├── NotificationMonitorService.kt    # 通知监听服务，备用触发方式
│   │   │   ├── CheckInForegroundService.kt      # 前台常驻服务，防止被杀
│   │   │   ├── KeepAliveWorker.kt               # WorkManager 保活任务
│   │   │   ├── TimeWindowReceiver.kt            # 打卡时间段定时开关
│   │   │   ├── MissedCheckInReceiver.kt         # 触发失败告警
│   │   │   ├── BootReceiver.kt                  # 开机自启动接收器
│   │   │   ├── DailyReportReceiver.kt           # 每日报告（AlarmManager 定时）
│   │   │   ├── CheckInWidget.kt                 # 桌面小组件
│   │   │   └── CheckInTileService.kt            # 控制中心快捷磁贴
│   │   ├── res/
│   │   │   ├── layout/          # activity_main, fragment_main, fragment_settings, item_log, item_keyword, dialog_keyword, widget_checkin
│   │   │   ├── values/          # strings, colors, themes, styles
│   │   │   ├── drawable/        # 图标、背景资源
│   │   │   ├── menu/            # Toolbar 菜单
│   │   │   ├── xml/             # widget_info（小组件配置）
│   │   │   └── mipmap/          # 应用图标
│   │   ├── AndroidManifest.xml
│   │   └── proguard-rules.pro                 # R8 混淆规则
│   ├── src/test/java/com/pengxh/smscheckin/
│   │   └── SmsReceiverTest.kt                 # 核心逻辑单元测试（11 用例）
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── smscheckin.jks                 # 签名密钥（密码勿提交，已迁至 local.properties）
├── .gitignore
├── README.md
├── ISSUES.md                      # 问题跟踪与修复记录
└── SUGGESTIONS.md                 # 优化建议
```

## 技术栈

| 项 | 值 |
|---|---|
| 语言 | Kotlin |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 34 (Android 14) |
| 构建工具 | 37.0.0 |
| AGP | 9.1.1 |
| Kotlin | 2.2.10 |
| UI | ViewBinding + Material3 |
| 混淆 | R8 (release) |
| 数据持久化 | SharedPreferences (sms_checkin_prefs) |
| 保活 | WorkManager |
| 定时 | AlarmManager |

## 编译

1. 用 Android Studio 打开此项目
2. Sync Gradle
3. Build → Assemble Debug
4. APK 输出在 `app/build/outputs/apk/debug/`

## 签名

Debug 使用默认 debug 签名。Release 使用 `smscheckin.jks`，密码配置在 `local.properties`。

## 运行测试

Android Studio 中右键 `SmsReceiverTest` → Run，或命令行：

```bash
./gradlew test
```

## 详细问题记录

参见 [ISSUES.md](./ISSUES.md)

# 短信打卡助手 — 交接文档

## 项目概览

Android 短信监听工具。收到含指定关键词的短信时，自动打开钉钉应用，用于每日打卡场景。

- 包名：`com.pengxh.smscheckin`
- 最低 SDK：26 (Android 8.0)
- 目标 SDK：34
- 语言：Kotlin
- 构建：Gradle + ViewBinding

## 核心文件

| 文件 | 职责 |
|------|------|
| `MainActivity.kt` | 轻量容器，管理 Fragment 切换和 Toolbar |
| `MainFragment.kt` | 主页：权限状态、服务开关、今日统计、最近活动 |
| `SettingsFragment.kt` | 设置页：关键词、白名单、冷却时间、通知权限、电池优化 |
| `SmsReceiver.kt` | 短信广播接收器，拦截短信并触发打卡逻辑 |
| `CheckInForegroundService.kt` | 前台服务，保持后台存活并在通知栏显示状态 |
| `SettingsStore.kt` | SharedPreferences 封装，管理关键词/白名单/冷却时间 |

## 页面架构

```
MainActivity (容器)
  ├── Toolbar（齿轮图标 ⇄ 返回箭头）
  ├── MainFragment（主页）
  │   ├── 短信权限卡片
  │   ├── 打卡监听开关
  │   ├── 今日统计（次数/上次触发/最新发件人）
  │   └── 最近活动列表
  └── SettingsFragment（设置页）
      ├── 触发关键词 EditText
      ├── 发送者白名单 EditText
      ├── 冷却时间 Slider（0-120 秒）
      ├── 通知权限状态 + 申请按钮
      └── 电池优化跳转按钮
```

- Fragment 切换用 `FragmentTransaction` + `addToBackStack`
- Toolbar 菜单用 `app:menu` + `setOnMenuItemClickListener`
- 两个 Fragment 之间不直接通信，数据通过 SharedPreferences 流转

## 数据流

```
SettingsFragment (写入) ─→ SharedPreferences ─→ SettingsStore (读取)
                                                        ↑
                                                SmsReceiver.onReceive()
```

- **关键词**：默认 "打卡"，SmsReceiver 用 `body.contains(keyword)` 匹配
- **白名单**：逗号分隔的号码列表，空=不限制，非空时只响应白名单内发件人
- **冷却时间**：控制两次打开钉钉的最小间隔，同时控制切回 APP 的延迟

## 关键逻辑

1. 用户开启服务开关 → 检查通知权限（Android 13+）→ 启动前台服务
2. SmsReceiver 收到短信 → 匹配关键词 → 检查白名单 → 冷却判断 → 打开钉钉 → 延迟后切回 APP
3. 今日触发数据存在 `SmsReceiver.Companion` 中（静态变量），进程被杀会清空
4. MainFragment 每 5 秒自动刷新统计和日志列表

## 已知限制

- 触发记录只在内存中（Companion 静态变量），进程被杀后丢失
- 关键词是简单的 `contains` 匹配，不支持正则
- 白名单匹配是精确字符串比较（发件人号码必须完全一致）
- 无多时段配置、无工作日判断

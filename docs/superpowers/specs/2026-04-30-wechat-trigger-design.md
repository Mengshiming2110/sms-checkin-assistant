# 微信触发来源与锁屏保障 — 设计文档

**日期：** 2026-04-30  
**版本：** v1.4

## 目标

新增微信通知作为自动打卡触发来源，仅白名单内微信用户的消息可触发；同时补充 WakeLock 机制确保锁屏/息屏下触发流程不被 CPU 休眠打断。

## 触发流程

```
收到通知
  → 检查服务是否启用
  → 检查是否在打卡时间段内
  → 判断包名：
      钉钉(com.alibaba.android.rimet) → 现有关键字匹配 → WakeLock → 打开钉钉
      微信(com.tencent.mm)            → 发送者在微信白名单？ → 微信关键字匹配？ → WakeLock → 打开钉钉
  → 冷却检查（30s，全局共用）
  → 记录触发日志（微信来源标记为"微信-{昵称}"）
  → 延迟后打开钉钉
```

## WakeLock 机制

在 `SmsReceiver.openDingTalk()` 和 `NotificationMonitorService` 中，打开钉钉前统一获取短时 WakeLock：

```kotlin
val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
val wakeLock = pm.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
    "smscheckin:trigger"
)
wakeLock.acquire(10_000L) // 10 秒自动释放
```

AndroidManifest 新增 `WAKE_LOCK` 权限。

## 微信通知字段提取

- `EXTRA_TITLE` → 发送者昵称/备注名
- `EXTRA_TEXT` → 消息内容

发件人匹配使用模糊匹配（`contains` 双向），与现有短信白名单逻辑一致。

## 配置存储

所有配置存入 `sms_checkin_prefs`：

| Key | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `wechat_whitelist` | JSON Array | `[]` | 微信用户白名单，空=不监听微信 |
| `wechat_keywords` | JSON Array | `[]` | 微信触发关键字，空=不匹配任何内容 |

## 现有配置默认值变更

| Key | 旧默认值 | 新默认值 | 说明 |
|-----|---------|---------|------|
| `keywords` | `["钉钉打卡"]` | `[]` | 短信/钉钉共享关键字，改为空由用户输入 |

关键字输入框 hint 统一为："请输入触发打卡关键字，检测到对应后会自动打卡"

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AndroidManifest.xml` | 修改 | 新增 `WAKE_LOCK` 权限 |
| `strings.xml` | 修改 | 新增微信相关字符串，修改默认关键字 hint |
| `fragment_settings.xml` | 修改 | 核心配置分区新增微信关键字和白名单两张卡片 |
| `NotificationMonitorService.kt` | 修改 | 新增微信包名分支 + WakeLock |
| `SmsReceiver.kt` | 修改 | `openDingTalk()` 前置 WakeLock；Companion 新增微信配置变量和加载逻辑 |
| `SettingsFragment.kt` | 修改 | 新增 `setupWechatKeywordEditor()`、`setupWechatWhitelistEditor()` |

## UI 布局

设置页「核心配置」分区末尾，延迟卡片之后：

```
核心配置 ▼
  ├── 触发关键字        [钉钉/短信共用，默认空]
  ├── 发送者白名单      [短信白名单]
  ├── 触发延迟          [0-30s]
  ├── 微信触发关键字    [独立，默认空]   ← 新增
  └── 微信用户白名单    [昵称匹配]       ← 新增
```

两张新增卡片复用现有关键字/白名单的 icon + 标题 + 副标题 + `>` 样式，点击弹出 BottomSheetDialog 编辑。

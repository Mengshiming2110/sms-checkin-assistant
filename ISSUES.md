# 已知问题清单

### 17. ✅ 设置页闪退（已修复）- 2026-04-27
- **症状**：点击 Toolbar 齿轮图标或「服务状态」卡片，APP 直接闪退
- **根因**：`SettingsFragment.onCreateView` 使用了裸 `inflate` 而非 `FragmentSettingsBinding.inflate`，且 `onViewCreated` 中 `prefs` 未初始化即被 setup 方法使用
- **修复**：
  - `onCreateView` 改回 `FragmentSettingsBinding.inflate`
  - `onViewCreated` 恢复 `prefs` 初始化 + 8 个 setup 方法调用
  - `setupDelaySlider` 增加 `coerceIn(0,30)` 防 Slider 值越界

## 新功能

### 18. ✅ 定时主动触发（已完成）- 2026-04-27
- **位置**：`ProactiveTriggerReceiver.kt`（新增）、`SmsReceiver.kt`、`SettingsFragment.kt`、`fragment_settings.xml`、`strings.xml`、`MainFragment.kt`、`TimeWindowReceiver.kt`、`CheckInWidget.kt`、`CheckInTileService.kt`、`BootReceiver.kt`、`AndroidManifest.xml`
- **内容**：用户在打卡窗口关闭前可设定提前时间，若该窗口内尚无打卡记录则自动打开钉钉
- **细节**：
  - 设置页新增「主动触发」卡片，含总开关 + 上班/下班窗口独立开关 + 提前时间滑块（0-300 秒，步长 30 秒）
  - 通过 AlarmManager 在每个打卡窗口结束前 N 秒触发，检查该窗口内今日是否已有触发记录
  - 若无记录则自动打开钉钉，触发来源标记为「主动触发」
  - 复用现有打卡时间段设置，仅对已启用的窗口生效
  - 按窗口独立判断：上班窗口未打卡不影响下班窗口的主动触发
  - 全程遵循 30 秒冷却机制
  - 服务启用/开机/窗口开关变化时自动更新 AlarmManager 调度
  - SmsReceiver 新增 `hasTriggeredInWindow()` 方法支持窗口级触发检测

### 16. ✅ 触发失败告警（已完成）
- **位置**：`MissedCheckInReceiver.kt`（新增）、`SettingsFragment.kt`、`MainFragment.kt`、`CheckInWidget.kt`、`BootReceiver.kt`
- **内容**：用户设定截止时间，若到时今日仍未触发打卡，推送高优先级通知提醒
- **细节**：
  - 设置页新增「打卡提醒」卡片，可开关 + 设置截止时间（默认 09:30）
  - 通过 AlarmManager 每日定时检查当天打卡次数
  - 次数为 0 则推送「今日尚未打卡」通知，点击跳转 APP
  - 服务启用时自动调度，禁用时取消，开机时恢复

### 15. ✅ 通知栏快捷开关 + 控制中心磁贴（已完成）
- **位置**：`CheckInForegroundService.kt`、`CheckInTileService.kt`（新增）、`AndroidManifest.xml`
- **内容**：两种方式快速开关打卡服务，无需打开 APP
- **细节**：
  - 常驻通知增加「暂停服务」按钮，点击即停止服务
  - 控制中心下拉快捷开关面板可添加「短信打卡助手」磁贴，点击切换服务状态
  - 磁贴显示当前状态（活跃/未活跃）
  - 全厂商通用，兼容 Android 7.0+

### 14. ✅ 打卡时间段设置（已完成）
- **位置**：`TimeWindowReceiver.kt`（新增）、`SettingsFragment.kt`、`SmsReceiver.kt`、`NotificationMonitorService.kt`、`BootReceiver.kt`
- **内容**：用户可设置上班/下班打卡时间段，仅在该时间段内运行服务，其余时间自动休眠，兼顾省电
- **细节**：
  - 上班/下班两个时间段独立开关，默认 07:00-09:00 / 17:00-19:00
  - 点击时间文字设置开始时间，长按设置结束时间
  - 时间段起止通过 AlarmManager 每日自动开关服务
  - 开机时检查当前是否在时间段内，不在则不启动服务
  - SmsReceiver / NotificationMonitorService 增加时间段判断
  - 延迟单位从毫秒改为秒（0-30 秒），增加功能说明文字

### 13. ✅ 服务保活（已完成）
- **位置**：`KeepAliveWorker.kt`（新增）、`MainFragment.kt`、`CheckInWidget.kt`、`BootReceiver.kt`
- **内容**：引入 WorkManager 每 15 分钟定期检查前台服务是否存活，若被系统杀死则自动重启
- **细节**：
  - 服务启用时自动注册保活任务（`KeepAliveWorker.schedule()`）
  - 服务禁用时取消保活任务（`KeepAliveWorker.cancel()`）
  - 开机重启时恢复保活任务调度

## UI / 交互

### 12. ✅ 新增首次启动使用说明弹窗（已完成）
- **位置**：`MainFragment.kt`、`strings.xml`
- **内容**：每次冷启动弹出使用说明，分前置条件/必须权限/使用方法三板块
- **实现**：companion object `guideShown` 标记控制，每进程生命周期弹一次

### 11. ✅ 设置页关键字/白名单改为卡片点击交互（已修复）
- **位置**：`fragment_settings.xml`、`SettingsFragment.kt`
- **问题**：关键字和白名单通过右侧小齿轮按钮进入编辑，交互不直观
- **修复**：移除编辑按钮，改为点击整张卡片触发，右侧添加 `>` 箭头提示

### 10. ✅ 设置页入口不显眼（已修复）
- **位置**：`ic_settings.xml`、`toolbar_menu.xml`、`MainFragment.kt`、`fragment_main.xml`
- **问题**：设置页仅通过 Toolbar 齿轮图标进入，图标颜色 `#FFFFFF`（白）与浅色背景 `#FAF8F5` 对比度极低
- **修复**：齿轮图标 tint 改为 `@color/gray_dark`，菜单标题改为"设置"，服务状态卡片可点击进入

---

## 安全风险

### 6. ✅ NotificationListenerService 读取钉钉全部通知字段（已修复）
- **位置**：`NotificationMonitorService.kt:31`
- **修复**：仅读取 `EXTRA_TITLE` 和 `EXTRA_TEXT`，不再读取 `EXTRA_BIG_TEXT` 和 `EXTRA_SUB_TEXT`

### 5. ✅ 短信/通知内容明文全量存储（已修复）
- **位置**：`SmsReceiver.kt:73-93`、`AndroidManifest.xml:29`
- **修复**：存储内容截断至前 120 字符（`MAX_CONTENT_LENGTH`），`allowBackup` 设为 `false`

### 4. ✅ 签名密码硬编码（已修复）
- **位置**：`app/build.gradle.kts:22-24`
- **修复**：密码改为从 `local.properties` 动态读取（`project.findProperty`）

---

## 代码质量

### 9. ✅ 无单元测试（已修复）
- **位置**：`app/src/test/java/com/pengxh/smscheckin/SmsReceiverTest.kt`
- **内容**：11 个测试用例覆盖关键字解析/序列化、白名单匹配、getter/setter 不可变性、configVersion 等

### 8. ✅ 未启用 R8 代码混淆（已修复）
- **位置**：`app/build.gradle.kts:30`
- **修复**：Release 构建开启 `isMinifyEnabled = true`，使用 `proguard-android-optimize.txt` + 自定义 `proguard-rules.pro`

### 7. ✅ SmsReceiver 每次收到短信都重新解析配置（已修复）
- **位置**：`SmsReceiver.kt:209-218`
- **修复**：引入 `configVersion` 版本号机制，配置未变化时跳过 JSON 解析

---

## 功能 Bug

### 3. ✅ 多小组件实例 PendingIntent 冲突（已修复）
- **位置**：`CheckInWidget.kt:32`
- **修复**：`requestCode` 改为 `widgetId`，每个小组件实例独立

### 2. ✅ 桌面小组件切换服务不处理每日报告调度（已修复）
- **位置**：`CheckInWidget.kt:66-77`
- **修复**：开启时追加 `DailyReportReceiver.scheduleDailyReport()`，关闭时追加 `cancelDailyReport()`

### 1. ✅ 开机重启后每日报告不自动恢复（已修复）
- **位置**：`BootReceiver.kt:14-33`
- **修复**：在启动前台服务后追加 `DailyReportReceiver.scheduleDailyReport(context)`

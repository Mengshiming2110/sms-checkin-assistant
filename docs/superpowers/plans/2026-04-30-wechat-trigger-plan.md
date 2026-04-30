# 微信触发来源与锁屏保障 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增微信通知触发来源（白名单+关键字过滤）+ WakeLock 锁屏保障 + 关键字默认值改为空。

**Architecture:** 扩展 NotificationMonitorService 增加微信包名分支，SmsReceiver.Companion 新增微信配置变量，SettingsFragment 新增两张微信编辑卡片。WakeLock 在 openDingTalk 调用前统一获取。

**Tech Stack:** Kotlin, Android SDK, SharedPreferences, NotificationListenerService, ViewBinding, Material3

---

### Task 1: AndroidManifest 新增 WAKE_LOCK 权限

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 添加 WAKE_LOCK 权限**

在 `AndroidManifest.xml` 第 17 行（`SYSTEM_ALERT_WINDOW` 权限之后）插入：

```xml
    <!-- 锁屏/息屏时保持 CPU 唤醒完成打卡 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add WAKE_LOCK permission for lock-screen trigger reliability"
```

---

### Task 2: strings.xml 新增/修改字符串

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 修改关键字默认 hint**

将第 22 行 `keyword_hint` 的值改为统一文案：

```xml
    <string name="keyword_hint">请输入触发打卡关键字，检测到对应后会自动打卡</string>
```

- [ ] **Step 2: 删除 `keyword_default` 字符串（不再使用）**

删除第 21 行：
```xml
    <string name="keyword_default">钉钉打卡</string>
```

- [ ] **Step 3: 新增微信相关字符串**

在 `</resources>` 前插入：

```xml
    <!-- 微信触发 -->
    <string name="wechat_keyword_title">微信触发关键字</string>
    <string name="wechat_keyword_empty">未设置关键字，不会触发</string>
    <string name="wechat_keyword_multiple">已设置 %d 个关键字</string>
    <string name="wechat_whitelist_title">微信用户白名单</string>
    <string name="wechat_whitelist_desc">仅白名单内微信用户的消息可触发打卡</string>
    <string name="wechat_whitelist_empty">未设置白名单，不监听微信消息</string>
    <string name="wechat_whitelist_hint">输入微信昵称或备注名</string>
    <string name="wechat_whitelist_multiple">已设置 %d 个用户</string>
    <string name="wechat_whitelist_exists">该用户已存在</string>
    <string name="wechat_whitelist_added">用户已添加</string>
    <string name="wechat_whitelist_saved">微信白名单已保存</string>
    <string name="wechat_keyword_saved">微信关键字已保存</string>
    <string name="wechat_keyword_added">微信关键字已添加</string>
    <string name="wechat_keyword_exists">该微信关键字已存在</string>
    <string name="wechat_keyword_empty_error">至少需要一个微信关键字</string>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add WeChat trigger strings and unify keyword hint"
```

---

### Task 3: fragment_settings.xml 新增微信卡片

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`

- [ ] **Step 1: 在延迟卡片之后插入微信关键字卡片**

在 `</LinearLayout>` (id=`sectionCoreContent` 的闭合标签) 之前，即延迟卡片 `</com.google.android.material.card.MaterialCardView>` 之后，插入：

```xml
            <Space
                android:layout_width="match_parent"
                android:layout_height="16dp" />

            <!-- 微信触发关键字 -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/wechatKeywordCard"
                style="@style/Widget.Material3.CardView.Elevated"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardBackgroundColor="@color/surface_container_high"
                app:cardCornerRadius="16dp"
                app:cardElevation="1dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="24dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <ImageView
                            android:layout_width="40dp"
                            android:layout_height="40dp"
                            android:background="@drawable/icon_bg_success"
                            android:padding="10dp"
                            android:src="@drawable/ic_label"
                            android:contentDescription="@string/wechat_keyword_title" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginStart="16dp"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/wechat_keyword_title"
                                android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                                android:textColor="@color/gray_dark"
                                android:textStyle="bold" />

                            <TextView
                                android:id="@+id/wechatKeywordText"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="2dp"
                                android:text="@string/wechat_keyword_empty"
                                android:textAppearance="@style/TextAppearance.Material3.BodyMedium"
                                android:textColor="@color/gray_text" />

                        </LinearLayout>

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_chevron_right"
                            android:contentDescription="@string/wechat_keyword_title"
                            app:tint="@color/gray_text" />

                    </LinearLayout>

                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <Space
                android:layout_width="match_parent"
                android:layout_height="16dp" />

            <!-- 微信用户白名单 -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/wechatWhitelistCard"
                style="@style/Widget.Material3.CardView.Elevated"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardBackgroundColor="@color/surface_container_high"
                app:cardCornerRadius="16dp"
                app:cardElevation="1dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="24dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal">

                        <ImageView
                            android:layout_width="40dp"
                            android:layout_height="40dp"
                            android:background="@drawable/icon_bg_success"
                            android:padding="10dp"
                            android:src="@drawable/ic_sender"
                            android:contentDescription="@string/wechat_whitelist_title" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginStart="16dp"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/wechat_whitelist_title"
                                android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                                android:textColor="@color/gray_dark"
                                android:textStyle="bold" />

                            <TextView
                                android:id="@+id/wechatWhitelistText"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="2dp"
                                android:text="@string/wechat_whitelist_empty"
                                android:textAppearance="@style/TextAppearance.Material3.BodyMedium"
                                android:textColor="@color/gray_text" />

                        </LinearLayout>

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_chevron_right"
                            android:contentDescription="@string/wechat_whitelist_title"
                            app:tint="@color/gray_text" />

                    </LinearLayout>

                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
```

> **定位技巧：** 在文件中找到 `<!-- 延迟设置 -->` 对应的 `MaterialCardView`，在其闭合标签 `</com.google.android.material.card.MaterialCardView>` 之后、`</LinearLayout>`（`sectionCoreContent` 闭合）之前插入。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/fragment_settings.xml
git commit -m "feat: add WeChat keyword and whitelist cards to settings layout"
```

---

### Task 4: SmsReceiver.kt — WakeLock + 微信配置变量

**Files:**
- Modify: `app/src/main/java/com/pengxh/smscheckin/SmsReceiver.kt`

- [ ] **Step 1: 新增 import**

在文件头部 import 区（第 7 行 `android.os.Build` 之后）插入：

```kotlin
import android.os.PowerManager
```

- [ ] **Step 2: Companion 中新增微信配置变量**

在 `companion object` 块内，`configVersion` 变量（第 47 行）之后插入：

```kotlin
        var wechatWhitelist = mutableListOf<String>()
        var wechatKeywords = mutableListOf<String>()
        private var loadedWechatConfigVersion = -1
```

- [ ] **Step 3: 修改关键字默认值为空**

将第 28 行：
```kotlin
        private var keywordsList = mutableListOf("钉钉打卡")
```
改为：
```kotlin
        private var keywordsList = mutableListOf<String>()
```

- [ ] **Step 4: openDingTalk 方法开头添加 WakeLock**

在 `openDingTalk` 方法体（第 176 行，`// Android 12+ 后台启动 Activity...` 注释之前）的开头插入：

```kotlin
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "smscheckin:trigger"
            )
            wakeLock.acquire(10_000L)
```

- [ ] **Step 5: onReceive 中微信配置加载逻辑**

在 `onReceive` 方法中，现有的 `loadedConfigVersion != configVersion` 代码块（第 244-255 行）内，在现有 `delay = ...` 行之后追加：

```kotlin
            val wechatWhitelistJson = prefs.getString("wechat_whitelist", "[]") ?: "[]"
            wechatWhitelist.clear()
            wechatWhitelist.addAll(parseKeywords(wechatWhitelistJson))

            val wechatKeywordsJson = prefs.getString("wechat_keywords", "[]") ?: "[]"
            wechatKeywords.clear()
            wechatKeywords.addAll(parseKeywords(wechatKeywordsJson))

            loadedWechatConfigVersion = loadedConfigVersion
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/pengxh/smscheckin/SmsReceiver.kt
git commit -m "feat: add WakeLock to openDingTalk, WeChat config vars, default keywords empty"
```

---

### Task 5: NotificationMonitorService.kt — 微信分支 + WakeLock

**Files:**
- Modify: `app/src/main/java/com/pengxh/smscheckin/NotificationMonitorService.kt`

- [ ] **Step 1: 新增 import**

在 import 区 `android.util.Log` 之后插入：

```kotlin
import android.os.PowerManager
```

- [ ] **Step 2: Companion 中新增微信包名常量**

在 `companion object` 块内，`DING_DING_PACKAGE` 之后新增：

```kotlin
        private const val WECHAT_PACKAGE = "com.tencent.mm"
```

- [ ] **Step 3: 替换整个 `onNotificationPosted` 方法**

将 `onNotificationPosted` 方法（第 31-91 行）替换为：

```kotlin
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif_monitor_enabled", false)) return
        if (!TimeWindowReceiver.isInTimeWindow(this)) return

        when (sbn.packageName) {
            DING_DING_PACKAGE -> handleDingTalkNotification(sbn, prefs)
            WECHAT_PACKAGE -> handleWechatNotification(sbn, prefs)
        }
    }

    private fun handleDingTalkNotification(sbn: StatusBarNotification, prefs: android.content.SharedPreferences) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")

        val content = listOfNotNull(title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (content.isBlank()) return

        Log.d(TAG, "收到钉钉通知: $content")

        val keywordsJson = prefs.getString("keywords", null) ?: "[]"
        val keywords = SmsReceiver.parseKeywords(keywordsJson)

        if (keywords.isEmpty()) {
            Log.d(TAG, "未配置关键字，跳过")
            return
        }

        val matchedKeyword = keywords.find { content.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "通知内容未匹配关键字，跳过")
            return
        }

        performTrigger(this, prefs, "钉钉通知", content)
    }

    private fun handleWechatNotification(sbn: StatusBarNotification, prefs: android.content.SharedPreferences) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")

        if (title.isBlank() && text.isBlank()) return

        Log.d(TAG, "收到微信通知 - 发件人: $title, 内容: $text")

        val wechatWhitelistJson = prefs.getString("wechat_whitelist", "[]") ?: "[]"
        val wechatWhitelist = SmsReceiver.parseKeywords(wechatWhitelistJson)

        if (wechatWhitelist.isEmpty()) {
            Log.d(TAG, "未配置微信白名单，跳过")
            return
        }

        if (!isSenderAllowedInWechat(title, wechatWhitelist)) {
            Log.d(TAG, "微信用户 '$title' 不在白名单中，跳过")
            return
        }

        val wechatKeywordsJson = prefs.getString("wechat_keywords", "[]") ?: "[]"
        val wechatKeywords = SmsReceiver.parseKeywords(wechatKeywordsJson)

        if (wechatKeywords.isEmpty()) {
            Log.d(TAG, "未配置微信关键字，跳过")
            return
        }

        val content = listOfNotNull(title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val matchedKeyword = wechatKeywords.find { content.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "微信消息未匹配关键字，跳过")
            return
        }

        performTrigger(this, prefs, "微信-$title", text)
    }

    private fun performTrigger(context: Context, prefs: android.content.SharedPreferences, senderLabel: String, content: String) {
        val now = System.currentTimeMillis()
        if (now - SmsReceiver.lastTriggerMs < 30_000L) {
            Log.d(TAG, "冷却中，跳过")
            return
        }
        SmsReceiver.lastTriggerMs = now

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = timeFormat.format(Date(now))
        val date = dateFormat.format(Date(now))
        SmsReceiver.saveRecord(context, time, senderLabel, content, date)

        val delay = prefs.getLong("delay", 0L)
        val delayMs = delay * 1000L

        Handler(Looper.getMainLooper()).post {
            SmsReceiver.todayTriggerCount = SmsReceiver.getTodayCount(context) + 1
            SmsReceiver.lastTriggerTime = time
            SmsReceiver.lastSender = senderLabel

            if (delayMs > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    SmsReceiver.openDingTalk(context, "通知")
                }, delayMs)
                Log.d(TAG, "延迟 ${delay}s 后打开钉钉")
            } else {
                SmsReceiver.openDingTalk(context, "通知")
            }
        }
    }

    private fun isSenderAllowedInWechat(sender: String, whitelist: List<String>): Boolean {
        return whitelist.any { sender.contains(it) || it.contains(sender) }
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/pengxh/smscheckin/NotificationMonitorService.kt
git commit -m "feat: add WeChat notification handling with whitelist and keyword matching"
```

---

### Task 6: SettingsFragment.kt — 微信关键字和白名单编辑器

**Files:**
- Modify: `app/src/main/java/com/pengxh/smscheckin/SettingsFragment.kt`

- [ ] **Step 1: 在 `onViewCreated` 中新增 setup 调用**

在第 44 行 `setupBatteryOptimization()` 之后插入两行：

```kotlin
        setupWechatKeywordEditor()
        setupWechatWhitelistEditor()
```

- [ ] **Step 2: 修改现有关键字默认值逻辑**

在 `setupKeywordEditor()` 方法（第 84 行）中，将第 89 行的默认值 `listOf("钉钉打卡")` 改为空列表：

将：
```kotlin
            listOf("钉钉打卡")
```
改为：
```kotlin
            emptyList()
```

同样在 `showKeywordDialog()` 方法（第 111 行）中，将 `mutableListOf("钉钉打卡")` 改为 `mutableListOf()`：

将：
```kotlin
            mutableListOf("钉钉打卡")
```
改为：
```kotlin
            mutableListOf()
```

- [ ] **Step 3: 修改 `keyword_hint` 使用更新后的字符串（第一步已完成 strings.xml 变更）**

在 `showWhitelistDialog()` 和 `showKeywordDialog()` 的 `keywordInput.hint` 设置中无需改动——微信对话框会使用 `wechat_whitelist_hint`。

- [ ] **Step 4: 在类末尾（`showUpdateDialog` 闭合大括号后）添加两个新方法**

在文件的最后一个 `}` 之前（即 `showUpdateDialog` 方法结束后）插入：

```kotlin
    /* ========== 微信关键字 ========== */

    private fun setupWechatKeywordEditor() {
        val savedJson = prefs.getString("wechat_keywords", null)
        val keywords = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson)
        } else {
            emptyList()
        }
        SmsReceiver.wechatKeywords = keywords.toMutableList()
        updateWechatKeywordDisplay(keywords)

        binding.wechatKeywordCard.setOnClickListener {
            showWechatKeywordDialog()
        }
    }

    private fun updateWechatKeywordDisplay(keywords: List<String>) {
        if (keywords.isEmpty()) {
            binding.wechatKeywordText.text = getString(R.string.wechat_keyword_empty)
        } else if (keywords.size == 1) {
            binding.wechatKeywordText.text = keywords[0]
        } else {
            binding.wechatKeywordText.text = getString(R.string.wechat_keyword_multiple, keywords.size)
        }
    }

    private fun showWechatKeywordDialog() {
        val savedJson = prefs.getString("wechat_keywords", null)
        val currentKeywords = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        keywordInput.hint = getString(R.string.keyword_hint)

        fun refreshKeywordList() {
            keywordListContainer.removeAllViews()
            if (currentKeywords.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentKeywords.forEach { keyword ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = keyword
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentKeywords.remove(keyword)
                        refreshKeywordList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshKeywordList()

        inputLayout.setEndIconOnClickListener {
            val newKeyword = keywordInput.text.toString().trim()
            if (newKeyword.isNotEmpty()) {
                if (currentKeywords.contains(newKeyword)) {
                    Toast.makeText(requireContext(), R.string.wechat_keyword_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentKeywords.add(newKeyword)
                    keywordInput.text?.clear()
                    refreshKeywordList()
                    Toast.makeText(requireContext(), R.string.wechat_keyword_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            if (currentKeywords.isEmpty()) {
                Toast.makeText(requireContext(), R.string.wechat_keyword_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val json = SmsReceiver.keywordsToJson(currentKeywords)
            prefs.edit().putString("wechat_keywords", json).apply()
            SmsReceiver.wechatKeywords = currentKeywords
            SmsReceiver.notifyConfigChanged()
            updateWechatKeywordDisplay(currentKeywords)
            Toast.makeText(requireContext(), R.string.wechat_keyword_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    /* ========== 微信白名单 ========== */

    private fun setupWechatWhitelistEditor() {
        val savedJson = prefs.getString("wechat_whitelist", null)
        val whitelist = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson)
        } else {
            emptyList()
        }
        SmsReceiver.wechatWhitelist = whitelist.toMutableList()
        updateWechatWhitelistDisplay(whitelist)

        binding.wechatWhitelistCard.setOnClickListener {
            showWechatWhitelistDialog()
        }
    }

    private fun updateWechatWhitelistDisplay(whitelist: List<String>) {
        if (whitelist.isEmpty()) {
            binding.wechatWhitelistText.text = getString(R.string.wechat_whitelist_empty)
        } else {
            binding.wechatWhitelistText.text = getString(R.string.wechat_whitelist_multiple, whitelist.size)
        }
    }

    private fun showWechatWhitelistDialog() {
        val savedJson = prefs.getString("wechat_whitelist", null)
        val currentWhitelist = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        keywordInput.hint = getString(R.string.wechat_whitelist_hint)
        emptyHint.text = getString(R.string.wechat_whitelist_empty)

        fun refreshWhitelistList() {
            keywordListContainer.removeAllViews()
            if (currentWhitelist.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentWhitelist.forEach { user ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = user
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentWhitelist.remove(user)
                        refreshWhitelistList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshWhitelistList()

        inputLayout.setEndIconOnClickListener {
            val newUser = keywordInput.text.toString().trim()
            if (newUser.isNotEmpty()) {
                if (currentWhitelist.contains(newUser)) {
                    Toast.makeText(requireContext(), R.string.wechat_whitelist_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentWhitelist.add(newUser)
                    keywordInput.text?.clear()
                    refreshWhitelistList()
                    Toast.makeText(requireContext(), R.string.wechat_whitelist_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val json = SmsReceiver.keywordsToJson(currentWhitelist)
            prefs.edit().putString("wechat_whitelist", json).apply()
            SmsReceiver.wechatWhitelist = currentWhitelist
            SmsReceiver.notifyConfigChanged()
            updateWechatWhitelistDisplay(currentWhitelist)
            Toast.makeText(requireContext(), R.string.wechat_whitelist_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/pengxh/smscheckin/SettingsFragment.kt
git commit -m "feat: add WeChat keyword and whitelist editors in settings"
```

---

### Task 7: 构建验证

- [ ] **Step 1: 编译 debug APK**

```bash
cd "C:\Users\70715\Desktop\短信打卡助手"
./gradlew assembleDebug
```

预期输出: `BUILD SUCCESSFUL`

- [ ] **Step 2: 验证 APK 生成**

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

预期: 文件存在且大小正常

- [ ] **Step 3: Commit（如有微小调整）**

```bash
git add -A
git commit -m "chore: build verification"
```

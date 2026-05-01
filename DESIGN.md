---
name: 短信打卡助手
description: 自动监听短信/通知并完成钉钉极速打卡的 Android 工具
colors:
  terracotta: "#E2825B"
  terracotta-deep: "#C96E4A"
  terracotta-container: "#FBE9DD"
  terracotta-light: "#FDF2EC"
  warm-cream: "#FAF8F5"
  warm-stone: "#F5F0EB"
  pure-white: "#FFFFFF"
  near-black: "#1A1A1A"
  warm-ash: "#5C5C5C"
  soft-stone: "#8B8B8B"
  warm-divider: "#E8E2DC"
  sage: "#5B8A72"
  sage-container: "#E8F3ED"
  error-red: "#B85A42"
  error-container: "#FDEEE8"
typography:
  display:
    fontFamily: "Roboto, sans-serif"
    fontSize: "36sp"
    fontWeight: 700
  headline:
    fontFamily: "Roboto, sans-serif"
    fontSize: "24sp"
    fontWeight: 400
  title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
rounded:
  sm: "8dp"
  md: "16dp"
  full: "9999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
components:
  button-tonal:
    backgroundColor: "{colors.terracotta-container}"
    textColor: "{colors.terracotta-deep}"
    rounded: "{rounded.full}"
    padding: "12dp 24dp"
  card-elevated:
    backgroundColor: "{colors.pure-white}"
    rounded: "{rounded.md}"
    padding: "{spacing.lg}"
  icon-circle:
    size: "40dp"
    backgroundColor: "{colors.terracotta}"
  switch-row:
    padding: "0 {spacing.lg}"
    height: "56dp"
---

# Design System: 短信打卡助手

## 1. Overview

**Creative North Star: "安静的同事"**

短信打卡助手是一个静默运行的后台工具。它的视觉系统像一位可靠的同事：话不多、不刷存在感、但每个关键时刻都准时出现。没有装饰，没有炫技——只保留需要的，然后把它们做到位。

整体气质偏暖但不甜腻，明亮但不刺眼。主色是一抹温润的赤陶橙，在暖奶油色的底上安静地标记重点——开关状态、统计数字、选中项。其他颜色退后，让结构和信息承担主角。

这个系统明确拒绝：设计语言前后不统一、交互状态表达模糊、为装饰而装饰的无意义元素、层层嵌套的复杂菜单。它追求的是「看一遍就懂，用一次就会」。

**Key Characteristics:**
- 暖色基调，但不温情泛滥——赤陶橙只是路标，不是氛围灯
- 色调分层代替阴影分层——卡片白、表面奶油、背景微暖
- 信息层次由颜色对比和间距决定，不由装饰元素决定
- 每个组件都有唯一确定的角色，不混用、不临时凑合

## 2. Colors

这套配色围绕一个暖色系的赤陶橙展开，中性色系偏向暖灰奶油感。整体色彩策略是 **Restrained**——单一主色占据不超过 10% 的屏幕面积，其余由中性色承载。主色的稀缺性本身就是信息：它出现的地方值得注意。

### Primary

- **赤陶橙 Terracotta** (`#E2825B`): 唯一的功能色。标记开关开启态、统计数字、选中项、主要操作。少量使用，出现即强调。
- **深赤陶 Terracotta Deep** (`#C96E4A`): 按下态、hover 加深。主色的下沉版本。
- **赤陶容器 Terracotta Container** (`#FBE9DD`): TonalButton 背景、选中卡片底色。大面积浅色容器，承载主色相关但不需强调的内容。
- **赤陶浅底 Terracotta Light** (`#FDF2EC`): 极淡的功能底色，用于需要微区分但不需强对比的区域。

### Neutral

- **暖奶油 Warm Cream** (`#FAF8F5`): 页面底色。不是纯白，有一丝肉眼可感的暖意。
- **暖石 Warm Stone** (`#F5F0EB`): 表面变体，用于区分底色与卡片之间的中间层。
- **纯白 Pure White** (`#FFFFFF`): 卡片背景。在暖奶油底上浮起，形成自然的层次区分。
- **近黑 Near Black** (`#1A1A1A`): 标题、主要文字。不是纯黑 `#000`。
- **暖灰 Warm Ash** (`#5C5C5C`): 正文、描述文字。足够对比但不刺眼。
- **软石 Soft Stone** (`#8B8B8B`): 辅助文字、占位符。退到背景中。
- **暖分割 Warm Divider** (`#E8E2DC`): 分割线、卡片边框。浅到刚好可见。

### Semantic

- **鼠尾草 Sage** (`#5B8A72`): 成功态、已授权、运行中。冷绿调与暖赤陶形成功能语义的清晰对位——暖色是「注意/操作」，冷色是「好了/完成」。
- **鼠尾草容器 Sage Container** (`#E8F3ED`): 成功态背景。
- **错误红 Error Red** (`#B85A42`): 错误态、未授权。与赤陶同色系但更暗沉，暗示问题的严重性但不跳出整体色调。
- **错误容器 Error Container** (`#FDEEE8`): 错误态背景。

### Named Rules

**The One-Accent Rule.** 主色赤陶橙占据不超过 10% 的屏幕面积。它出现的地方就是用户应该看的地方。滥用主色等于没有主色。

**The Tonal-Layer Rule.** 不用阴影区分层级。用颜色深浅：底色（暖奶油）→ 卡片（纯白）→ 选中区（赤陶容器）。颜色本身的对比度就是深度信息。

## 3. Typography

**Display Font:** Roboto (system default)
**Body Font:** Roboto (system default)
**Label/Mono Font:** Roboto (system default)

**Character:** Roboto 是 Android 的系统字体，不引入外部字体是为了保持加载速度和应用体积。它的中性气质恰好契合「安静的同事」——没有性格就是最好的性格。重点通过字重和大小建立层次，而非依赖字体本身的个性。

### Hierarchy

- **Display** (700, 36sp, 1.0): 统计大数字。仅用于今日触发次数。大而有力，但不占过多空间。
- **Headline** (400, 24sp, 1.25): 最后触发时间。与统计数字形成粗细对比。
- **Title** (500, 16sp, 1.25): 卡片标题、设置行标题、开关标签。主要的信息承载层级。
- **Body** (400, 14sp, 1.5): 描述文字、状态说明、辅助信息。最大行长限制 65-75 字符以保证可读性。
- **Label** (500, 12sp, 1.25): 统计标签、分类标题、微注释。小而有力，不喧哗。

### Named Rules

**The Two-Weight Rule.** 标题用 Medium (500)，正文用 Regular (400)，大数字用 Bold (700)。不用 Light、Thin、Black——三个字重足以覆盖所有层次。

**The No-Caps Rule.** 所有按钮、标签、标题保持常规大小写。不用全大写——中文和数字混合的界面中全大写英文显得突兀。

## 4. Elevation

克制内敛的层次策略：**用颜色深浅代替阴影**。当前使用 1dp 的极轻微卡片阴影，这在 Material3 中几乎不可见——实际效果是卡片通过纯白色在暖奶油底色上自然「浮起」，不依赖阴影产生深度感。

### Shadow Vocabulary

阴影仅用于状态反馈，不作为层级手段：

- **Card-rest** (`elevation: 1dp`): 卡片默认态。极轻微，主要是纯白底色 vs 暖奶油的色差在起作用。
- **Toggle-active** (`elevation: 8dp`): 大圆形服务开关——唯一的例外。启用时轻微上浮，配合颜色变化暗示「被按下/激活」。

### Named Rules

**The Flat-By-Default Rule.** 所有表面默认平坦。阴影只在状态变化时出现（开关激活），且一经变化即消失。不通过持续阴影暗示「这个元素更重要」——颜色和排版承担这个职责。

## 5. Components

### Buttons

- **Shape:** 全圆角（`9999dp`），胶囊形
- **Primary (TonalButton):** 赤陶容器底色 (`#FBE9DD`) + 深赤陶文字 (`#C96E4A`)，内边距 12dp 垂直、24dp 水平。用于「检查更新」「去设置」等次要操作——主要操作靠开关，不靠按钮。
- **Hover / Pressed:** 容器色加深一阶。无动画移动，仅颜色过渡 200ms ease-out。
- **No outlined or text variants in use.** 单一按钮风格保持语言一致。

### Switches

- **Style:** MaterialSwitch，默认 Material3 样式。滑块颜色跟随主色赤陶橙。
- **Layout:** 始终与标签同行——标签在左，开关在右。行高固定 56dp，垂直居中对齐。
- **State:** 开启态轨道颜色为赤陶橙降低透明度，关闭态为暖石色。

### Cards

- **Corner Style:** 16dp 圆角
- **Background:** 纯白 (`#FFFFFF`)，在暖奶油底色上自然区分
- **Border:** 无可见边框。分割线用于卡片内部区域分隔
- **Internal Padding:** 统一 24dp 四面内边距
- **Shadow Strategy:** 1dp elevation，实际感知靠色差而非阴影

### Icon Circles

- **Shape:** 40dp 圆形，内嵌 24dp 图标
- **Background:** 主色赤陶橙 (`#E2825B`) 最常用；鼠尾草 (`#5B8A72`) 用于成功/完成态；错误红 (`#B85A42`) 用于警告
- **Icon Tint:** 白色 (`#FFFFFF`)
- **Usage:** 每个卡片标题区左侧——快速识别卡片功能类别

### Navigation

- **Style:** BottomNavigationView，2 个标签项（仪表盘 + 设置）
- **Typography:** LabelMedium (12sp, 500)
- **Default:** 软石色图标 + 软石色文字
- **Active:** 赤陶橙色图标 + 赤陶橙色文字
- **Background:** 纯白卡片色，与页面内容区形成顶底呼应

### Section Headers (Settings)

- **Style:** 可折叠的分类标题行——文字在左，箭头在右
- **Typography:** LabelLarge (14sp, 500, bold)，赤陶橙色
- **Interaction:** 点击展开/折叠，箭头旋转 90 度过渡 200ms
- **Purpose:** 将设置页的长列表组织为三个分组（核心配置/时间策略/系统权限），降低滚动疲劳

### Stats Row

- **Layout:** 三列等高，列间 1dp 暖分割竖线
- **Typo:** 数字用 Display (36sp, 700, 赤陶橙)；标签用 Label (12sp, 500, 暖灰)
- **Spacing:** 数字到标签间距 4dp；列内垂直居中

## 6. Do's and Don'ts

### Do:

- **Do** 用赤陶橙标记唯一需要用户注意的信息——开关开启态、统计数字、选中项
- **Do** 用颜色深浅区分层级——卡片白 > 表面奶油 > 背景奶油
- **Do** 保持所有卡片内边距统一 24dp，所有卡片间距统一 16dp
- **Do** 每个交互元素在视觉上自解释——开关有文字标签、状态有副标题、不可用时置灰
- **Do** 信息层次用字重区分——Bold 数字 > Medium 标题 > Regular 正文

### Don't:

- **Don't** 设计语言前后不一致——同一组件在不同页面必须同色、同尺寸、同行为。PRODUCT.md 明确禁止「设计语言不统一」
- **Don't** 交互状态模糊——开关必须有明确的视觉开/关差异，权限状态必须有文字说明当前态
- **Don't** 为装饰添加元素——没有渐变背景、没有装饰性图标、没有无意义的动画过渡。PRODUCT.md 明确禁止「过度装饰」
- **Don't** 嵌套过深——从首页到任何设置项的路径不超过 2 步。PRODUCT.md 明确禁止「层 層嵌套菜单」
- **Don't** 使用全大写英文按钮或标签——中文界面中全大写英文显得突兀
- **Don't** 在卡片上使用大于 1dp 的单侧彩色边框作为强调——要么完整边框，要么不用

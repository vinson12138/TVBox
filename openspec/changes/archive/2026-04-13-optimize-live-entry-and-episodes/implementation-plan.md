# 实施清单与验证说明

> **2026-04-12 核查更新**
>
> 经对比当前代码与 tasks.md，以下所有实施块均**尚未实现**。
> 唯一相关的已有代码变更：上游提交 `baddbd15b Fix episode compare` 将
> `Episode.equals()` 改为同时对比 name + url，并新增 `Episode.matchesName()` 方法，
> 该变更属于上游 bugfix，不计入本变更任务。

## 待实施的模块（按依赖顺序）

**先决条件（无依赖，可优先实现）：**

1. 主页直播入口开关（Setting + HomeActivity + SettingFragment/SettingActivity + 布局 XML）

**核心数据层（直播开关无需此模块，其余所有 UI 均依赖此模块）：**

2. `Episode` 结构化识别字段（contentType / versionKey / getDisplayText）
3. `EpisodeGroup` bean 及 `Flag.getGroups()` 分组逻辑（含排序、去重、分页尺寸）

**UI 层（依赖数据层）：**

4. 倒序逻辑修正（收窄作用范围 + 移除箭头按钮 + 文案同步）
5. 电视端分组条（EpisodeGroupPresenter + VideoActivity 布局变更）
6. 手机端选集网格（GridLayoutManager + EpisodeGroupAdapter + 布局变更）

**兼容层（依赖数据层）：**

7. History 续播兼容（URL 优先匹配 + 分组内定位）

## 重点验证样本

建议在所有实施块完成后，重点回归以下样本：

1. 短剧：少量集数、单一正片分组。
2. 长剧：50 集以上，验证分页与倒序。
3. 动漫：包含 `第X话`、`OVA`、`SP`、剧场版。
4. 多清晰度源：同一集存在 `1080P` / `4K` / `HDR`。
5. 多语言源：同一集存在 `国语` / `粤语` / `原声`。
6. 动态追加线路：播放失败后自动切站或补全线路。

## 验收标准

1. 用户可在设置中关闭首页直播入口，且两套首页同步生效。
2. 手机端详情页主选集区不再是单行横向滚动。
3. 电视端可在分组内切换并使用分页辅助浏览。
4. 明显重复的同名同版本选集减少。
5. 同一集的不同版本不会被简单吞并成单个无差别入口。
6. 续播命中优先按 URL，其次按标题与序号回退。
7. 主风味编译任务通过。

# 志愿服务系统修复与功能增强 — 设计文档

## 概述

修复9个Bug并增强4项功能：报名申请表流程、签到签退统一页面+自动定位+三种方式、48h自动审批+双端审批入口、动态折线图、报名名单Tab+导出。

---

## 一、报名申请表流程

### 现状
- `detail.vue` 点击"立即报名"弹窗确认后直接调 `/register`，成功后仅toast，无申请表收集
- `apply.vue` 存在但表单数据未提交到后端，最终只调了 `/register` 后跳转

### 改造
1. `detail.vue`：点击"立即报名"不再弹窗，直接 `uni.navigateTo({ url: '/pages/activities/apply?id=xxx' })`
2. `apply.vue`：填写表单后调用新接口 `POST /api/activities/{id}/apply`，请求体包含：
   ```json
   { "name": "...", "phone": "...", "email": "...", "emergencyContact": "...", "emergencyPhone": "...", "remarks": "..." }
   ```
3. 后端新增 `ActivityController.applyActivity()` 方法：先执行现有报名逻辑（创建VolunteerRecord + SignRecord），再保存表单数据到 `volunteer_record` 表的扩展字段或新建 `application_form` 表
4. 提交成功后返回 `detail.vue`，按钮变为"已报名"

### 文件
- 前端：`detail.vue`、`apply.vue`
- 后端：`ActivityController.java`、`ActivityServiceImpl.java`、`VolunteerRecord.java`（加字段或新建ApplicationForm实体）

---

## 二、发布者不能报名自己的活动

### 现状
- 后端 `ActivityServiceImpl.registerForActivity:115` 已校验 `creatorId.equals(userId)`
- 前端 `canApply` 计算未排除

### 改造
- `detail.vue` 的 `canApply` computed 加 `&& !isActivityCreator.value`

---

## 三、留言板标签修正

### 现状
- `comment-section.vue` 的 userTag computed 逻辑和 `tagLabel` 映射正确
- 标签："发布方"/"已参与"/"未参与"

### 改造
- 确认 `detail.vue` 传给 `CommentSection` 的 `isActivityCreator` 和 `hasParticipated` props 值准确
- `isActivityCreator` 已有 base64 JWT 解析逻辑，保持不变
- `hasRegistered` 基于 sign-status 返回值 > 0

---

## 四、已报名活动仍显示"报名参加"

### 根因
`detail.vue` 中 `hasRegistered` 依赖 sign-status API。报名后 `createSignRecord` 创建 status=0 记录，API 返回 `pending`。但可能存在时序问题或 token 异常导致 API 返回 error。

### 改造
1. `loadSignStatus` 增加重试逻辑（延迟200ms重试一次）
2. `applyButtonText` 在 signStatus 加载完成前显示"加载中..."
3. `canApply` 增加 `!loading` 条件

---

## 五、签到签退显示"未报名"

### 根因
`sign.vue` 的 `checkSignStatus` 方法在 API 返回非200时设 `signError = '请先报名该活动后再签到'`。

### 改造
1. `sign.vue`：分别处理"未找到记录"和"网络错误"
2. 增加 loading 状态，加载完成后才展示错误

---

## 六、管理员活动管理无法查看详情

### 根因
`admin/activities.vue:19` 的 `<view class="list-item">` 缺少 `@tap` 事件。

### 改造
- 添加 `@tap="viewDetail(a.activityId)"`，导航到 `/pages/activities/detail?id=xxx`

---

## 七、报名条件一字一行

### 根因
`detail.vue:38` 中 `v-for="(cond, idx) in activity.conditions"` — conditions 是 JSON 字符串（如 `"[\"条件1\",\"条件2\"]"`），v-for 遍历字符串逐字符输出。

### 改造
- `loadActivity` 中解析：`activity.value.conditions = JSON.parse(activity.value.conditions || '[]')`

---

## 八、签到签退统一页面 + 三种方式 + 自动定位

### 改造
1. **Activity 实体**：新增 `signMethod` 字段（VARCHAR），默认值 `"gps,scan,photo"`，发布方可指定允许的方式
2. **发布活动页**：增加签到方式选择（多选：GPS定位/扫码/拍照）
3. **sign.vue 重构**：
   - 统一签到/签退在一个页面
   - 根据活动 `signMethod` 展示可选方式
   - GPS定位：`navigator.geolocation`(H5) / `uni.getLocation`(小程序) 自动获取
   - 扫码：`uni.scanCode` 扫描二维码
   - 拍照：`uni.chooseImage` + 自动附加位置和时间水印
   - 签退同样三种方式可选
   - 不再要求用户手动输入地址
4. **删除多余的签到入口**：`my-activities.vue` 中的签到按钮跳转到统一的 sign.vue

---

## 九、48h自动审批 + 审批界面 + 动态折线图

### 9a. 48h自动审批

- `SignRecord` 表已有 `approval_status` 字段（0待审批/1已通过/2已拒绝）和 `checkout_time`
- 新增 Spring Boot 定时任务 `SignApprovalTask`：
  - `@Scheduled(cron = "0 0 * * * *")` 每小时执行
  - 查询 `status=2 AND approval_status=0 AND checkout_time < NOW() - INTERVAL 48 HOUR`
  - 批量更新 `approval_status=1`，同时更新用户 `volunteer_hours`
- 发布方可主动审批（通过/拒绝），拒绝会重置状态

### 9b. 审批界面

**发布方入口**：`my-activities.vue` → "我发布的"Tab → 每个活动卡片显示待审批数 → 点击进入 `sign-approval.vue` 审批列表 → 可通过/拒绝
**管理员入口**：`admin/dashboard.vue` → 新增"签到审批"入口 → `admin/sign-approvals.vue` → 列表+筛选+审批

### 9c. 动态折线图

- `service-chart.vue` 改为 ECharts 折线图
- `type: 'line'` 替代 `type: 'bar'`
- X轴：`YYYY-MM` 年月格式
- `dataZoom` 组件：底部滑块可拖动缩放范围
- `tooltip`：点击数据点显示详细数值
- 月度数据查询修复：统计 `sign_record` 表中 `approval_status IN (0,1)` 的记录（含待审批+已审批），让数据实时反映
- admin dashboard 的 `totalVolunteerHours` 改为从 `sign_record` 表 SUM `hours_earned` WHERE `approval_status=1`

---

## 十、报名名单Tab + 导出

### 改造
1. `detail.vue`：活动详情页增加Tab切换（"活动详情"/"报名名单"），报名名单Tab仅在 `isActivityCreator=true` 时显示
2. 报名名单调用 `GET /api/activities/{id}/applicants`，返回报名者列表（姓名、手机号、邮箱、紧急联系人、紧急联系电话、备注、报名时间）
3. 导出按钮：调用 `GET /api/activities/{id}/applicants/export?format=xlsx`，后端使用 Apache POI 生成 Excel 并返回文件流下载

---

## 文件变更清单

| 层 | 文件 | 变更 |
|----|------|------|
| 前端 | `pages/activities/detail.vue` | 报名流程改跳转、canApply加条件、conditions解析、Tab切换、报名名单 |
| 前端 | `pages/activities/apply.vue` | 表单提交改为调apply接口 |
| 前端 | `pages/activities/sign.vue` | 重构为统一页面+三种方式+自动定位 |
| 前端 | `pages/activities/publish.vue` | 增加签到方式选择 |
| 前端 | `pages/my/my-activities.vue` | 审批入口+待审批数 |
| 前端 | `pages/admin/dashboard.vue` | 签到审批入口+折线图 |
| 前端 | `pages/admin/activities.vue` | 添加点击查看详情 |
| 前端 | `pages/admin/sign-approvals.vue` | 新增：管理员签到审批页 |
| 前端 | `pages/activities/sign-approval.vue` | 新增：发布方审批页 |
| 前端 | `components/charts/service-chart.vue` | 柱状图改折线图+交互 |
| 前端 | `api/index.js` | 新增接口 |
| 后端 | `entity/Activity.java` | 加signMethod字段 |
| 后端 | `entity/VolunteerRecord.java` | 加表单字段或新建ApplicationForm |
| 后端 | `controller/ActivityController.java` | 新增apply、applicants、export接口 |
| 后端 | `controller/AdminController.java` | 新增审批列表接口 |
| 后端 | `service/ActivityService.java` | 新增方法签名 |
| 后端 | `service/impl/ActivityServiceImpl.java` | 实现新方法 |
| 后端 | `task/SignApprovalTask.java` | 新增：48h自动审批定时任务 |
| 后端 | `mapper/AdminMapper.java` | 新增审批相关SQL |
| DB | 迁移脚本 | Activity加signMethod；VolunteerRecord扩展字段 |

---

## 自审清单

- [x] 无TBD/TODO占位符
- [x] 各部分无矛盾
- [x] 范围聚焦，无无关重构
- [x] 无歧义：每个需求有明确的前后端变更路径

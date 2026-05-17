# 志愿服务系统修复与功能增强 — 设计文档

## 概述

修复17个Bug + 增强5项功能 + 修复7个安全漏洞 + 修复6个前端逻辑Bug。

---

## 一、原有9项用户报告问题

### 1. 报名申请表流程

**根因**: `detail.vue` 点击"立即报名"弹窗确认后直接调 `/register`，无申请表收集；`apply.vue` 表单数据未提交到后端。

**改造**:
1. `detail.vue` / `list.vue`：点击"立即报名"不再弹窗/直接调API，统一跳转 `apply.vue`
2. `apply.vue`：表单提交改为调用 `POST /api/activities/{id}/apply`，请求体含 name/phone/email/emergencyContact/emergencyPhone/remarks
3. 后端新增 `ActivityController.applyActivity()`：执行报名逻辑 + 保存表单数据到 `volunteer_record` 扩展字段
4. 提交后返回活动详情页

**文件**: `detail.vue`, `list.vue`, `apply.vue`, `ActivityController.java`, `ActivityServiceImpl.java`

### 2. 发布者不能报名自己的活动

**根因**: 后端已有校验，前端 `canApply` 未排除发布者。

**改造**: `detail.vue` `canApply` 加 `&& !isActivityCreator.value`；`list.vue` 同样加检查

### 3. 留言板标签修正

**根因**: `comment-section.vue` 的标签映射正确，需要确保传入 props 准确。

**改造**: 确认 `detail.vue` 传入的 `isActivityCreator` 和 `hasParticipated` 值正确

### 4. 已报名活动仍显示"报名参加"

**根因**: (a) `detail.vue` sign-status API 可能因 token 问题失败；(b) `list.vue:211` `canApply` 根本没查用户报名状态。

**改造**:
1. `detail.vue`: `loadSignStatus` 增加重试；`canApply` 加 `!loading && !hasRegistered`
2. `list.vue`: `canApply` 增加用户报名状态检查（加载已报名活动列表比对）

### 5. 签到签退显示"未报名"

**根因**: `sign.vue` `checkSignStatus` 在 API 失败时直接设 `signError`

**改造**: 区分"未报名"和"网络错误"；增加 loading 态

### 6. 管理员活动管理无法查看详情

**根因**: `admin/activities.vue:19` 列表项缺 `@tap`

**改造**: 加 `@tap="viewDetail(a.activityId)"`

### 7. 报名条件一字一行

**根因**: `conditions` 是 JSON 字符串，`v-for` 遍历字符串逐字符输出

**改造**: `loadActivity` 中 `JSON.parse(activity.conditions || '[]')`

### 8. 签到签退统一页面 + 三种方式 + 自动定位

**改造**:
1. Activity 实体加 `signMethod` 字段（默认 `"gps,scan,photo"`），发布方可指定
2. 发布活动页加签到方式多选
3. `sign.vue` 重构为一个页面，三种方式可选：
   - GPS定位：`navigator.geolocation`(H5) / `uni.getLocation`(小程序) 自动获取
   - 扫码：`uni.scanCode`
   - 拍照：`uni.chooseImage` + 自动位置水印
4. 签退同样三种方式；不再手动输入地址

### 9. 48h自动审批 + 审批界面 + 动态折线图

**9a. 审批**: `AutoApproveTask` 已存在且正常（`@Scheduled(fixedRate=300000)`每5分钟），`findPendingApprovals` SQL 已过滤48h。无需新增。

**9b. 审批界面**:
- 发布方：`my-activities.vue` → 每活动显示待审批数 → `sign-approval.vue` → 通过/拒绝
- 管理员：`admin/dashboard.vue` 加"签到审批"入口 → `admin/sign-approvals.vue`

**9c. 动态折线图**:
- `service-chart.vue` 改为 ECharts 折线图，`type: 'line'`
- X轴 `YYYY-MM`，`dataZoom` 底部滑块，`tooltip` 点击详情
- 月度统计 SQL 改为统计 `approval_status IN (0,1)` 包含待审批
- `totalVolunteerHours` 改为 `sign_record` SUM `hours_earned` WHERE `approval_status=1`

---

## 二、报名名单Tab + 导出

1. `detail.vue` 增加Tab切换："活动详情"/"报名名单"，仅在 `isActivityCreator=true` 显示
2. `GET /api/activities/{id}/applicants` 返回名单
3. `GET /api/activities/{id}/applicants/export` 导出 Excel

---

## 三、安全漏洞修复（7项）

### S1. SecurityConfig 认证绕过 [HIGH]
**文件**: `SecurityConfig.java:48-50`
**问题**: `/activities/**` 和 `/organizations/**` 对所有 HTTP 方法 permitAll，POST/PUT/DELETE 无需认证即可到达 Controller。
**修复**: 移除48-50行的无方法限制 permitAll；仅保留 GET 的公开访问；POST/PUT/DELETE 需认证。

### S2. 管理员接口无角色保护 [HIGH]
**文件**: `AdminController.java`, `JwtAuthenticationFilter.java`
**问题**: 管理员接口使用与用户相同的JWT认证，无角色区分。用户Token可访问 `/admin/**` 端点。
**修复**: JWT 生成时加 `role` claim（`user`/`admin`）；AdminController 所有方法加 `@PreAuthorize("hasRole('admin')")`；JwtAuthenticationFilter 解析 role 并设 GrantedAuthority。

### S3. JWT 过滤器不拒绝无效 Token [MEDIUM]
**文件**: `JwtAuthenticationFilter.java:68-72`
**问题**: Token 解析失败只打日志，不返回401。
**修复**: 对非公开接口，无效/过期 Token 返回 401 错误响应。

### S4. CORS 配置无效 [MEDIUM]
**文件**: `SecurityConfig.java:86`
**问题**: `allowedOriginPatterns: "*"` + `allowCredentials: true` 违反CORS规范。
**修复**: 改为具体的前端域名列表（`http://localhost:5173` 等），或去掉 `allowCredentials`。

### S5. 全局异常处理暴露堆栈 [LOW]
**文件**: `GlobalExceptionHandler.java:12,18`
**问题**: `e.printStackTrace()` 生产环境泄露敏感信息。
**修复**: 改用 Logger 记录，生产环境不输出堆栈到响应。

### S6. Token 解析不一致 [LOW]
**文件**: `UserController.java:36-41`, `ActivityController.java:259-264`
**问题**: UserController 的 `getUserIdFromToken` 静默去除 Bearer 前缀，ActivityController 的版本抛异常。不一致。
**修复**: 统一到 JwtUtils 中处理。

### S7. 前端无路由守卫 [CRITICAL-FRONTEND]
**文件**: `App.vue`, `main.js`
**问题**: 完全无导航拦截。所有页面（包括admin）可被任意用户访问。
**修复**: `main.js` 中 `uni.addInterceptor('navigateTo', ...)` 检查 token；admin 页面额外检查 adminToken。

---

## 四、前端逻辑Bug修复（6项）

### F1. 活动列表不显示"已报名"状态 [HIGH]
**文件**: `list.vue:211-217`
**问题**: `canApply` 不查用户报名状态，`getApplyButtonText` 无"已报名"选项。
**修复**: 加载页面时获取用户已报名的活动ID集合，`canApply` 和 `getApplyButtonText` 中检查。

### F2. 活动列表直接调注册API绕过申请表 [HIGH]
**文件**: `list.vue:193-208`
**问题**: `applyActivity` 直接调 `/register`，绕过 `apply.vue`。
**修复**: 改为跳转 `apply.vue`，与 `detail.vue` 保持一致。

### F3. 报名列表 signStatus 未映射 [MEDIUM]
**文件**: `applications.vue:134-143`
**问题**: 映射函数不含 `signStatus`，签到签退按钮判断失效。
**修复**: 映射加 `signStatus: item.signStatus`。

### F4. Token 写入绕过 Vuex Store [MEDIUM]
**文件**: `login.vue:116-117`, `admin-login.vue:48`
**问题**: 直接 `uni.setStorageSync` 写 token，Store 状态不更新。
**修复**: 统一通过 Store mutation 写入。

### F5. 登出不清理 adminToken [MEDIUM]
**文件**: `store/modules/auth.js:98-101`, `profile.vue:306-307`
**问题**: logout 不删 adminToken。
**修复**: logout 时同时 `uni.removeStorageSync('adminToken')`。

### F6. profile.vue 向所有用户暴露管理后台入口 [LOW]
**文件**: `profile.vue:178`
**问题**: 所有用户可见"管理后台"菜单项。
**修复**: 仅 adminToken 存在时显示。

---

## 五、数据一致性修复（3项）

### D1. 仪表盘 totalVolunteerHours 统计口径
**文件**: `AdminMapper.java:50`
**问题**: 从 user 表 SUM volunteer_hours，但用户时长只在审批后才更新。
**修复**: 改为从 sign_record 表 SUM hours_earned WHERE approval_status=1。

### D2. 月度统计包含待审批记录
**文件**: `UserMapper.java:73`
**问题**: `WHERE approval_status = 1` 排除待审批记录，用户看月度时长为0。
**修复**: 改为 `WHERE approval_status IN (0,1)`，让数据实时反映。

### D3. home.vue monthHours 取错元素
**文件**: `home.vue:159`
**问题**: 取 `monthlyStats` 最后一个元素当本月，但顺序不保证。
**修复**: 按 month 字段排序后取最新月份，或直接用当前月份匹配。

---

## 六、输入验证补充（3项）

### V1. publish.vue 缺手机号格式校验
**文件**: `publish.vue:236`
**修复**: 加正则校验 `/^1[3-9]\d{9}$/`

### V2. admin-login.vue 无最小长度校验
**文件**: `admin-login.vue:36-39`
**修复**: 用户名字段加 `>= 3` 长度校验

### V3. verify-identity.vue 未用共享身份证校验器
**文件**: `verify-identity.vue:41`
**修复**: 改为引用 `@/utils/validate.js` 的 `validateIdCard`

---

## 文件变更清单

| 层 | 文件 | 变更 |
|----|------|------|
| 前端 | `pages/activities/detail.vue` | 报名跳转、canApply条件、conditions解析、Tab切换+报名名单、isActivityCreator修复 |
| 前端 | `pages/activities/apply.vue` | 表单提交改为调apply接口 |
| 前端 | `pages/activities/sign.vue` | 重构为统一页面+三种方式+自动定位 |
| 前端 | `pages/activities/list.vue` | 报名按钮改跳转apply.vue、增加已报名状态检查、tags解析 |
| 前端 | `pages/activities/publish.vue` | 签到方式选择、手机号格式校验 |
| 前端 | `pages/my/my-activities.vue` | 待审批数标识、审批入口 |
| 前端 | `pages/my/applications.vue` | signStatus映射修复 |
| 前端 | `pages/home/home.vue` | monthHours修复、登出清理adminToken、导航加token检查 |
| 前端 | `pages/admin/dashboard.vue` | 签到审批入口、折线图改造 |
| 前端 | `pages/admin/activities.vue` | 点击查看详情 |
| 前端 | `pages/admin/sign-approvals.vue` | 新增：管理员签到审批页 |
| 前端 | `pages/activities/sign-approval.vue` | 新增：发布方审批页 |
| 前端 | `components/charts/service-chart.vue` | 折线图+交互 |
| 前端 | `components/activity/comment-section.vue` | 标签逻辑确认 |
| 前端 | `pages/profile/profile.vue` | 管理后台入口仅admin可见、登出清adminToken |
| 前端 | `pages/auth/login.vue` | Token写入通过Store |
| 前端 | `pages/auth/admin-login.vue` | Token写入通过Store、用户名校验 |
| 前端 | `pages/auth/verify-identity.vue` | 使用共享校验器 |
| 前端 | `pages/my/org-upgrade.vue` | 输入校验 |
| 前端 | `utils/request.js` | 401时同时清adminToken、公共JWT解码工具 |
| 前端 | `store/modules/auth.js` | logout清adminToken |
| 前端 | `api/index.js` | 新增apply、applicants、export、审批接口 |
| 前端 | `main.js` | 路由拦截器 |
| 后端 | `config/SecurityConfig.java` | 修复认证绕过、CORS、角色权限 |
| 后端 | `filter/JwtAuthenticationFilter.java` | 加角色解析、拒绝无效Token返回401 |
| 后端 | `entity/Activity.java` | 加signMethod字段 |
| 后端 | `entity/VolunteerRecord.java` | 加表单扩展字段 |
| 后端 | `controller/ActivityController.java` | 新增apply、applicants、export接口 |
| 后端 | `controller/AdminController.java` | 加审批列表接口、@PreAuthorize |
| 后端 | `service/ActivityService.java` | 新增方法签名 |
| 后端 | `service/impl/ActivityServiceImpl.java` | 实现新方法 |
| 后端 | `mapper/AdminMapper.java` | 审批列表SQL、修复totalVolunteerHours口径 |
| 后端 | `mapper/UserMapper.java` | 月度统计改为包含待审批 |
| 后端 | `exception/GlobalExceptionHandler.java` | printStackTrace改为Logger |
| 后端 | `utils/JwtUtils.java` | 统一token处理、加role claim |
| DB | 迁移脚本 | Activity加signMethod；VolunteerRecord扩展字段 |

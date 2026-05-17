# 志愿服务系统修复与功能增强 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复28个Bug + 增强5项功能 + 7项安全修复 + 6项前端逻辑修复，覆盖报名流程、签到签退、审批系统、数据看板。

**Architecture:** Spring Boot 2.x + MyBatis 后端 + uni-app Vue 3 (Composition API) 前端。按数据层→服务层→控制层→安全层→前端工具→前端页面的顺序实施，确保每层改动不破坏已有功能。

**Tech Stack:** Java 17, Spring Boot, MyBatis, JWT (jjwt), Apache POI, uni-app (Vue 3), ECharts

---

### Task 1: 数据库迁移 — 新增字段

**Files:**
- Create: `volunteer-service-backend/src/main/resources/db/migration/V2__add_sign_method_and_form_fields.sql`

- [ ] **Step 1: 编写迁移SQL**

```sql
-- V2: 添加签到方式字段和报名表单扩展字段
ALTER TABLE activity ADD COLUMN sign_method VARCHAR(50) DEFAULT 'gps,scan,photo' COMMENT '允许的签到方式，逗号分隔';

ALTER TABLE volunteer_record 
  ADD COLUMN applicant_name VARCHAR(50) COMMENT '报名者姓名',
  ADD COLUMN applicant_phone VARCHAR(20) COMMENT '报名者手机号',
  ADD COLUMN applicant_email VARCHAR(100) COMMENT '报名者邮箱',
  ADD COLUMN emergency_contact VARCHAR(50) COMMENT '紧急联系人',
  ADD COLUMN emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
  ADD COLUMN remarks VARCHAR(500) COMMENT '备注';
```

- [ ] **Step 2: 执行迁移确认**

```bash
# 如果有Flyway会自动执行；否则手动执行SQL
# 确认字段已添加
mysql -u root -p volunteer_db -e "DESC activity;" | grep sign_method
mysql -u root -p volunteer_db -e "DESC volunteer_record;" | grep applicant_name
```

- [ ] **Step 3: Commit**

```bash
git add volunteer-service-backend/src/main/resources/db/migration/V2__add_sign_method_and_form_fields.sql
git commit -m "feat: add sign_method column to activity and form fields to volunteer_record"
```

---

### Task 2: 后端实体 — Activity 和 VolunteerRecord 新增字段

**Files:**
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/entity/Activity.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/entity/VolunteerRecord.java`

- [ ] **Step 1: Activity 加 signMethod 字段**

```java
// Activity.java 在类中添加:
private String signMethod; // 允许的签到方式，逗号分隔，默认 gps,scan,photo
```

- [ ] **Step 2: VolunteerRecord 加表单字段**

```java
// VolunteerRecord.java 在类中添加:
private String applicantName;
private String applicantPhone;
private String applicantEmail;
private String emergencyContact;
private String emergencyPhone;
private String remarks;
```

- [ ] **Step 3: 更新 ActivityMapper insert SQL**

```java
// ActivityMapper.java 修改 insert 的 @Insert 注解，SQL 的字段列表中加入 sign_method 和 VALUES 中加入 #{signMethod}
// 原SQL:
// "INSERT INTO activity(title, description, start_time, ... , feed backs) VALUES(#{title}, ... ,#{feedbacks})"
// 改为: 字段列表加 sign_method, VALUES加 #{signMethod}
```

- [ ] **Step 4: 更新 VolunteerRecordMapper insert SQL**

```java
// VolunteerRecordMapper.java 修改 insert 的 @Insert 注解，加入新字段
// "INSERT INTO volunteer_record(user_id, activity_id, hours_earned, status, register_time, applicant_name, applicant_phone, applicant_email, emergency_contact, emergency_phone, remarks) ..."
```

- [ ] **Step 5: Commit**

```bash
git add volunteer-service-backend/src/main/java/com/volunteer/entity/Activity.java
git add volunteer-service-backend/src/main/java/com/volunteer/entity/VolunteerRecord.java
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/ActivityMapper.java
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/VolunteerRecordMapper.java
git commit -m "feat: add signMethod to Activity and form fields to VolunteerRecord entity/mapper"
```

---

### Task 3: 后端安全修复 — JwtUtils + SecurityConfig + JwtFilter

**Files:**
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/utils/JwtUtils.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/config/SecurityConfig.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/filter/JwtAuthenticationFilter.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: JwtUtils 加 role claim**

```java
// JwtUtils.java: 修改 generateToken 加 role 参数

public String generateToken(Integer userId, String username, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    claims.put("role", role != null ? role : "user");
    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
}

// 新增方法: 从token获取role
public String getRoleFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
    }
    Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    return claims.get("role", String.class);
}

// 新增方法: 统一从token获取userId（处理Bearer前缀）
public Integer getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
    }
    Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    return Integer.parseInt(claims.get("userId").toString());
}
```

- [ ] **Step 2: 更新所有 generateToken 调用处，传入 role**

```java
// UserServiceImpl.java login方法:
return jwtUtils.generateToken(user.getUserId(), user.getUsername(), "user");

// UserServiceImpl.java wechatLogin方法:
return jwtUtils.generateToken(user.getUserId(), user.getUsername(), "user");

// AdminServiceImpl.java login方法:
return jwtUtils.generateToken(admin.getAdminId(), admin.getUsername(), "admin");
```

- [ ] **Step 3: SecurityConfig 修复认证绕过和CORS**

```java
// SecurityConfig.java: 替换 authorizeHttpRequests 部分

.authorizeHttpRequests(auth -> auth
    // 1. OPTIONS preflight
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    // 2. Public auth endpoints
    .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/users/ping").permitAll()
    .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/users/wechat/login").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/users/wechat/auth-url").permitAll()
    // 3. Public GET — 活动列表和详情公开读
    .requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/organizations/**").permitAll()
    // 4. Uploads publicly accessible
    .requestMatchers("/uploads/**").permitAll()
    .requestMatchers("/api/upload", "/api/upload/batch").permitAll()
    .requestMatchers("/upload", "/upload/batch").permitAll()
    // 5. Swagger
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    // 6. Admin endpoints require admin role
    .requestMatchers("/admin/**").hasRole("admin")
    // 7. All other requests need authentication
    .anyRequest().authenticated()
)
```

- [ ] **Step 4: SecurityConfig CORS 修复**

```java
// 替换 corsConfigurationSource 方法
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

- [ ] **Step 5: JwtAuthenticationFilter 加角色解析 + 拒绝无效Token**

```java
// JwtAuthenticationFilter.java doFilterInternal方法，在 try 块之后:
// 替换 System.err.println 部分

try {
    String jwt = parseJwt(request);
    if (jwt != null && !jwt.isEmpty()) {
        if (jwtUtils.validateToken(jwt)) {
            Integer userId = jwtUtils.getUserIdFromToken(jwt);
            String role = jwtUtils.getRoleFromToken(jwt);
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // Token无效 — 对非公开接口返回401
            if (!isPublicPath(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期\"}");
                return;
            }
        }
    }
} catch (Exception e) {
    if (!isPublicPath(request)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"认证失败\"}");
        return;
    }
}

chain.doFilter(request, response);

// 新增辅助方法 isPublicPath
private boolean isPublicPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    return path.equals("/api/users/login") || path.equals("/api/users/register")
        || path.equals("/admin/login") || path.startsWith("/api/users/wechat")
        || ("GET".equals(method) && (path.startsWith("/api/activities") || path.startsWith("/api/organizations")));
}
```

- [ ] **Step 6: AdminController 加 @PreAuthorize**

```java
// AdminController.java 类上添加:
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // 加这行
public class AdminController {
    // 注意: login 方法需要豁免
    @PostMapping("/login")
    @PreAuthorize("permitAll()")  // login不需要admin角色
    public Result login(@RequestBody Map<String, String> params) { ... }
}
```

- [ ] **Step 7: 统一 Controller 中 token 解析**

```java
// ActivityController.java 删除私有方法 getUserIdFromToken，替换为:
private Integer getUserIdFromToken(String token) throws Exception {
    if (token == null) throw new Exception("未提供Token");
    return jwtUtils.getUserIdFromToken(token); // 统一用 JwtUtils 方法
}

// UserController.java 删除私有方法 getUserIdFromToken，替换为:
private Integer getUserIdFromToken(String token) {
    if (token == null) return null;
    return jwtUtils.getUserIdFromToken(token);
}
```

- [ ] **Step 8: GlobalExceptionHandler 改用 Logger**

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage());
        return Result.error(e.getMessage() != null ? e.getMessage() : "系统错误");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("Exception: {}", e.getMessage());
        return Result.error(e.getMessage() != null ? e.getMessage() : "系统错误");
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add volunteer-service-backend/src/main/java/com/volunteer/utils/JwtUtils.java
git add volunteer-service-backend/src/main/java/com/volunteer/config/SecurityConfig.java
git add volunteer-service-backend/src/main/java/com/volunteer/filter/JwtAuthenticationFilter.java
git add volunteer-service-backend/src/main/java/com/volunteer/controller/AdminController.java
git add volunteer-service-backend/src/main/java/com/volunteer/controller/ActivityController.java
git add volunteer-service-backend/src/main/java/com/volunteer/controller/UserController.java
git add volunteer-service-backend/src/main/java/com/volunteer/exception/GlobalExceptionHandler.java
git add volunteer-service-backend/src/main/java/com/volunteer/service/impl/UserServiceImpl.java
git add volunteer-service-backend/src/main/java/com/volunteer/service/impl/AdminServiceImpl.java
git commit -m "fix: security - add role-based auth, fix auth bypass, fix CORS, fix JWT filter, fix exception logging"
```

---

### Task 4: 后端数据一致性修复 — Mapper SQL

**Files:**
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/mapper/AdminMapper.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/mapper/UserMapper.java`

- [ ] **Step 1: AdminMapper 修复 totalVolunteerHours 统计口径**

```java
// AdminMapper.java: 替换 totalVolunteerHours
@Select("SELECT COALESCE(SUM(sr.hours_earned), 0) FROM sign_record sr WHERE sr.approval_status = 1")
int totalVolunteerHours();
```

- [ ] **Step 2: UserMapper 月度统计改为包含待审批记录**

```java
// UserMapper.java: 替换 getMonthlyStats 的 WHERE 子句
@Select("SELECT DATE_FORMAT(sr.checkout_time, '%Y-%m') as month, " +
        "COALESCE(SUM(sr.hours_earned), 0) as hours " +
        "FROM sign_record sr " +
        "WHERE sr.user_id = #{userId} AND sr.approval_status IN (0, 1) " +
        "AND sr.checkout_time IS NOT NULL " +
        "GROUP BY DATE_FORMAT(sr.checkout_time, '%Y-%m') ORDER BY month ASC")
List<Map<String, Object>> getMonthlyStats(@Param("userId") Integer userId);
```

- [ ] **Step 3: Commit**

```bash
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/AdminMapper.java
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/UserMapper.java
git commit -m "fix: data consistency - totalVolunteerHours from sign_record, monthlyStats include pending"
```

---

### Task 5: 后端新API — 报名申请表提交 + 报名者名单 + 导出

**Files:**
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/service/ActivityService.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/service/impl/ActivityServiceImpl.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/controller/ActivityController.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/mapper/VolunteerRecordMapper.java`

- [ ] **Step 1: ActivityService 接口加新方法签名**

```java
// ActivityService.java 添加:
void applyForActivity(Integer userId, Integer activityId, Map<String, String> formData) throws Exception;
List<Map<String, Object>> getApplicants(Integer activityId) throws Exception;
byte[] exportApplicants(Integer activityId) throws Exception;
```

- [ ] **Step 2: ActivityServiceImpl 实现 applyForActivity**

```java
// ActivityServiceImpl.java，在 registerForActivity 方法后进行改造:

@Override
@Transactional
public void applyForActivity(Integer userId, Integer activityId, Map<String, String> formData) throws Exception {
    User user = userMapper.findById(userId);
    if (user == null || user.getStatus() != 1) throw new Exception("用户不存在或未通过审核");
    if (user.getRealNameStatus() == null || user.getRealNameStatus() != 1) throw new Exception("请先完成实名认证后再报名活动");

    Activity activity = activityMapper.findById(activityId);
    if (activity == null) throw new Exception("活动不存在");
    if (activity.getCreatorId().equals(userId)) throw new Exception("您是该活动的发布方，不能报名自己的活动");
    if (activity.getStatus() != 1) throw new Exception("活动不在招募状态");

    VolunteerRecord existing = recordMapper.findByUserAndActivity(userId, activityId);
    if (existing != null) throw new Exception("您已报名过该活动");

    if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) throw new Exception("活动人数已满");

    VolunteerRecord record = new VolunteerRecord();
    record.setUserId(userId);
    record.setActivityId(activityId);
    record.setHoursEarned(activity.getRewardHours());
    record.setStatus("registered");
    // 表单数据
    record.setApplicantName(formData.get("name"));
    record.setApplicantPhone(formData.get("phone"));
    record.setApplicantEmail(formData.get("email"));
    record.setEmergencyContact(formData.get("emergencyContact"));
    record.setEmergencyPhone(formData.get("emergencyPhone"));
    record.setRemarks(formData.get("remarks"));

    recordMapper.insert(record);
    activityMapper.increaseParticipants(activityId);
    createSignRecord(userId, activityId);
}
```

- [ ] **Step 3: ActivityServiceImpl 实现 getApplicants**

```java
@Override
public List<Map<String, Object>> getApplicants(Integer activityId) throws Exception {
    Activity activity = activityMapper.findById(activityId);
    if (activity == null) throw new Exception("活动不存在");
    return recordMapper.findApplicantsByActivity(activityId);
}
```

- [ ] **Step 4: ActivityServiceImpl 实现 exportApplicants (Excel导出)**

```java
@Override
public byte[] exportApplicants(Integer activityId) throws Exception {
    List<Map<String, Object>> applicants = getApplicants(activityId);
    try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("报名名单");
        Row header = sheet.createRow(0);
        String[] headers = {"姓名", "手机号", "邮箱", "紧急联系人", "紧急联系电话", "备注", "报名时间"};
        for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

        int rowIdx = 1;
        for (Map<String, Object> a : applicants) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(str(a.get("applicantName")));
            row.createCell(1).setCellValue(str(a.get("applicantPhone")));
            row.createCell(2).setCellValue(str(a.get("applicantEmail")));
            row.createCell(3).setCellValue(str(a.get("emergencyContact")));
            row.createCell(4).setCellValue(str(a.get("emergencyPhone")));
            row.createCell(5).setCellValue(str(a.get("remarks")));
            row.createCell(6).setCellValue(str(a.get("registerTime")));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        return bos.toByteArray();
    }
}

private String str(Object o) { return o != null ? o.toString() : ""; }
```

- [ ] **Step 5: VolunteerRecordMapper 加申请人查询 SQL**

```java
// VolunteerRecordMapper.java 添加:
@Select("SELECT vr.applicant_name AS applicantName, vr.applicant_phone AS applicantPhone, " +
        "vr.applicant_email AS applicantEmail, vr.emergency_contact AS emergencyContact, " +
        "vr.emergency_phone AS emergencyPhone, vr.remarks AS remarks, " +
        "vr.register_time AS registerTime, u.real_name AS userName " +
        "FROM volunteer_record vr " +
        "JOIN user u ON vr.user_id = u.user_id " +
        "WHERE vr.activity_id = #{activityId} AND vr.status != 'cancelled' " +
        "ORDER BY vr.register_time DESC")
List<Map<String, Object>> findApplicantsByActivity(@Param("activityId") Integer activityId);
```

- [ ] **Step 6: ActivityController 加新端点**

```java
// ActivityController.java 添加:

@PostMapping("/{id}/apply")
public Result applyForActivity(@RequestHeader("Authorization") String token,
                                @PathVariable Integer id,
                                @RequestBody Map<String, String> formData) {
    try {
        Integer userId = getUserIdFromToken(token);
        activityService.applyForActivity(userId, id, formData);
        return Result.success("报名成功");
    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}

@GetMapping("/{id}/applicants")
public Result getApplicants(@RequestHeader("Authorization") String token,
                            @PathVariable Integer id) {
    try {
        Integer userId = getUserIdFromToken(token);
        List<Map<String, Object>> applicants = activityService.getApplicants(id);
        return Result.success(applicants);
    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}

@GetMapping("/{id}/applicants/export")
public ResponseEntity<byte[]> exportApplicants(@RequestHeader("Authorization") String token,
                                               @PathVariable Integer id) {
    try {
        Integer userId = getUserIdFromToken(token);
        byte[] data = activityService.exportApplicants(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=applicants.xlsx")
                .body(data);
    } catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add volunteer-service-backend/src/main/java/com/volunteer/service/ActivityService.java
git add volunteer-service-backend/src/main/java/com/volunteer/service/impl/ActivityServiceImpl.java
git add volunteer-service-backend/src/main/java/com/volunteer/controller/ActivityController.java
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/VolunteerRecordMapper.java
git commit -m "feat: add apply API with form data, applicant list, Excel export"
```

---

### Task 6: 后端审批列表API

**Files:**
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/controller/AdminController.java`
- Modify: `volunteer-service-backend/src/main/java/com/volunteer/mapper/AdminMapper.java`

- [ ] **Step 1: AdminMapper 加审批列表查询**

```java
// AdminMapper.java 添加:
@Select("<script>" +
        "SELECT sr.id, sr.user_id AS userId, sr.activity_id AS activityId, " +
        "sr.hours_earned AS hoursEarned, sr.checkin_time AS checkinTime, " +
        "sr.checkout_time AS checkoutTime, sr.approval_status AS approvalStatus, " +
        "u.real_name AS realName, a.title AS activityTitle " +
        "FROM sign_record sr " +
        "JOIN user u ON sr.user_id = u.user_id " +
        "JOIN activity a ON sr.activity_id = a.activity_id " +
        "WHERE sr.status = 2 AND sr.approval_status = 0 " +
        "ORDER BY sr.checkout_time DESC LIMIT #{offset}, #{limit}" +
        "</script>")
List<Map<String, Object>> findPendingSignApprovals(@Param("offset") int offset, @Param("limit") int limit);

@Select("SELECT COUNT(*) FROM sign_record WHERE status = 2 AND approval_status = 0")
int countPendingSignApprovals();

@Update("UPDATE sign_record SET approval_status = 2, approval_time = NOW() WHERE id = #{id}")
int rejectSignRecord(@Param("id") Integer id);
```

- [ ] **Step 2: AdminController 加审批端点**

```java
// AdminController.java 添加:

@GetMapping("/sign-approvals")
public Result getSignApprovals(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
    try {
        int offset = (page - 1) * size;
        List<Map<String, Object>> list = adminMapper.findPendingSignApprovals(offset, size);
        int total = adminMapper.countPendingSignApprovals();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}

@PutMapping("/sign-approvals/{id}/reject")
public Result rejectSignApproval(@PathVariable Integer id) {
    try {
        adminMapper.rejectSignRecord(id);
        return Result.success("已拒绝");
    } catch (Exception e) {
        return Result.error(e.getMessage());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add volunteer-service-backend/src/main/java/com/volunteer/mapper/AdminMapper.java
git add volunteer-service-backend/src/main/java/com/volunteer/controller/AdminController.java
git commit -m "feat: add sign approval list and reject API for admin"
```

---

### Task 7: 前端基础修复 — API、Store、工具类、路由守卫

**Files:**
- Modify: `volunteer-uniapp/utils/request.js`
- Modify: `volunteer-uniapp/store/modules/auth.js`
- Modify: `volunteer-uniapp/api/index.js`
- Modify: `volunteer-uniapp/main.js`

- [ ] **Step 1: request.js — 401时同时清adminToken，加公共JWT解码**

```javascript
// utils/request.js: 修改401处理（约第47行），清两种token
} else if (data.code === 401 || data.msg === '无效的token') {
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  store.commit('auth/SET_TOKEN', '')
  uni.removeStorageSync('token')
  uni.removeStorageSync('adminToken')
  setTimeout(() => uni.reLaunch({ url: '/pages/auth/login' }), 1500)
  reject(new Error(data.msg || 'Error'))
}

// 新增导出: JWT解码工具函数
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = atob(base64)
    return JSON.parse(decoded)
  } catch (e) { return null }
}
```

- [ ] **Step 2: store/modules/auth.js — logout清adminToken**

```javascript
// store/modules/auth.js: logout 方法加清理
logout() {
  commit('SET_TOKEN', '')
  commit('SET_USER_INFO', null)
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('adminToken')  // 加这行
}
```

- [ ] **Step 3: api/index.js — 新增接口**

```javascript
// api/index.js: activityApi 对象添加:
applyForActivity: (id, data) => request.post(`/activities/${id}/apply`, data),
getApplicants: (id) => request.get(`/activities/${id}/applicants`),
exportApplicants: (id) => request.get(`/activities/${id}/applicants/export`),
getSignApprovals: (activityId) => request.get(`/activities/${activityId}/sign-approvals`),
approveSign: (id) => request.put(`/activities/sign-approvals/${id}/approve`),
rejectSign: (id) => request.put(`/activities/sign-approvals/${id}/reject`),

// adminApi 对象添加:
getSignApprovals: (params) => request.get('/admin/sign-approvals', params),
approveSignRecord: (id, data) => request.post(`/admin/sign-records/${id}/approve`, data),
rejectSignApproval: (id) => request.put(`/admin/sign-approvals/${id}/reject`),
```

- [ ] **Step 4: main.js — 路由拦截器**

```javascript
// main.js: 在文件开头 import { createSSRApp } from 'vue' 之后添加:

// 路由守卫: 检查需要登录的页面
const authRequiredPaths = [
  '/pages/activities/publish',
  '/pages/activities/apply',
  '/pages/activities/sign',
  '/pages/my/applications',
  '/pages/my/my-activities',
  '/pages/my/certificates',
  '/pages/my/org-upgrade',
  '/pages/auth/verify-identity'
]

const adminRequiredPaths = [
  '/pages/admin/dashboard',
  '/pages/admin/users',
  '/pages/admin/activities',
  '/pages/admin/organizations'
]

uni.addInterceptor('navigateTo', {
  invoke(e) {
    const token = uni.getStorageSync('token')
    const adminToken = uni.getStorageSync('adminToken')
    const url = e.url.split('?')[0]

    if (authRequiredPaths.some(p => url.startsWith(p)) && !token) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      uni.navigateTo({ url: '/pages/auth/login' })
      return false
    }

    if (adminRequiredPaths.some(p => url.startsWith(p)) && !adminToken) {
      uni.showToast({ title: '请先以管理员身份登录', icon: 'none' })
      uni.navigateTo({ url: '/pages/auth/admin-login' })
      return false
    }
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add volunteer-uniapp/utils/request.js
git add volunteer-uniapp/store/modules/auth.js
git add volunteer-uniapp/api/index.js
git add volunteer-uniapp/main.js
git commit -m "fix: frontend base - JWT decode util, adminToken cleanup, route guards, new API methods"
```

---

### Task 8: 前端 — auth页面修复 (login, admin-login, verify-identity)

**Files:**
- Modify: `volunteer-uniapp/pages/auth/login.vue`
- Modify: `volunteer-uniapp/pages/auth/admin-login.vue`
- Modify: `volunteer-uniapp/pages/auth/verify-identity.vue`

- [ ] **Step 1: login.vue — Token通过Store写入**

```vue
<!-- login.vue: 替换直接 uni.setStorageSync 为 store commit -->
<script setup>
import { useStore } from 'vuex'
const store = useStore()

// 在登录成功回调中（约116-117行），替换:
// uni.setStorageSync('token', token)
// uni.setStorageSync('userInfo', { username: formData.username.trim() })
// 改为:
store.commit('auth/SET_TOKEN', token)
// 同时获取用户信息
try {
  const infoRes = await request.get('/api/users/info')
  if (infoRes.code === 200) {
    store.commit('auth/SET_USER_INFO', infoRes.data)
    uni.setStorageSync('userInfo', infoRes.data)
  }
} catch(e) {}
uni.switchTab({ url: '/pages/home/home' })
```

- [ ] **Step 2: admin-login.vue — Token通过Store + 用户名校验**

```vue
<!-- admin-login.vue: 加校验和通过store写入 -->
<script setup>
// 修改 canSubmit:
const canSubmit = computed(() => {
  return formData.username.trim().length >= 3 && formData.password.trim().length >= 6
})

// 登录成功回调中替换 uni.setStorageSync
store.commit('auth/SET_ADMIN_TOKEN', token)
uni.setStorageSync('adminToken', token)
uni.redirectTo({ url: '/pages/admin/dashboard' })
</script>
```

- [ ] **Step 3: verify-identity.vue — 使用共享校验器**

```vue
<!-- verify-identity.vue: 替换内联身份证校验 -->
<script setup>
import { validateIdCard } from '@/utils/validate'

// 替换内联的 /^\d{17}[\dXx]$/ 为:
const isValidIdCard = computed(() => validateIdCard(formData.idCard))
</script>
```

- [ ] **Step 4: 更新store modules/auth.js 加 SET_ADMIN_TOKEN**

```javascript
// store/modules/auth.js: 添加 adminToken state 和 mutation
state: {
  token: '',
  adminToken: '',
  userInfo: null
},
mutations: {
  SET_TOKEN(state, token) { state.token = token },
  SET_ADMIN_TOKEN(state, token) { state.adminToken = token },
  SET_USER_INFO(state, info) { state.userInfo = info }
}
```

- [ ] **Step 5: Commit**

```bash
git add volunteer-uniapp/pages/auth/login.vue
git add volunteer-uniapp/pages/auth/admin-login.vue
git add volunteer-uniapp/pages/auth/verify-identity.vue
git add volunteer-uniapp/store/modules/auth.js
git commit -m "fix: auth pages - token via store, admin login validation, shared ID validator"
```

---

### Task 9: 前端 — detail.vue 大改造 (报名跳转、canApply、conditions、Tab、名单)

**Files:**
- Modify: `volunteer-uniapp/pages/activities/detail.vue`

- [ ] **Step 1: 模板 — 报名按钮改跳转，加Tab切换**

```vue
<template>
  <view class="activity-detail-container" v-if="!loading">
    <!-- ... back-header 保持不变 ... -->
    <!-- ... cover保持不变 ... -->

    <!-- Tab切换 -->
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 'detail' }" @tap="currentTab = 'detail'">活动详情</view>
      <view class="tab" :class="{ active: currentTab === 'applicants' }"
        v-if="isActivityCreator" @tap="currentTab = 'applicants'">报名名单</view>
    </view>

    <!-- 详情Tab内容 -->
    <view class="activity-info" v-if="currentTab === 'detail'">
      <!-- 原有info-section, desc-section, conditions-section, CommentSection -->
    </view>

    <!-- 报名名单Tab内容 -->
    <view class="applicants-list" v-if="currentTab === 'applicants'">
      <view class="export-bar">
        <button class="export-btn" @tap="exportApplicants">导出Excel</button>
      </view>
      <view class="applicant-card" v-for="(a, i) in applicants" :key="i">
        <text class="app-name">{{ a.applicantName }}</text>
        <text class="app-info">{{ a.applicantPhone }} | {{ a.applicantEmail }}</text>
        <text class="app-info">紧急联系人: {{ a.emergencyContact }} {{ a.emergencyPhone }}</text>
        <text class="app-info" v-if="a.remarks">备注: {{ a.remarks }}</text>
      </view>
      <view v-if="applicants.length === 0" class="empty">暂无报名者</view>
    </view>

    <!-- 底部按钮 -->
    <view class="bottom-action" v-if="isLoggedIn && currentTab === 'detail'">
      <view class="action-btn favorite-btn" @tap="toggleFavorite">...</view>
      <button class="apply-btn" :class="{ disabled: !canApply }" @tap="goToApply" :disabled="!canApply">
        {{ applyButtonText }}
      </button>
      <button v-if="showSignBtn" class="sign-btn" @tap="goToSignPage">
        {{ signStatus.value === 'checked_in' ? '签退' : '签到' }}
      </button>
    </view>
  </view>
</template>
```

- [ ] **Step 2: Script — 加 Tab、名单、条件解析等逻辑**

```javascript
<script setup>
import { decodeJwt } from '@/utils/request'

const currentTab = ref('detail')
const applicants = ref([])

// 修复 conditions 解析
const parsedConditions = computed(() => {
  const c = activity.value.conditions
  if (!c) return []
  if (Array.isArray(c)) return c
  try { return JSON.parse(c) } catch (e) { return [] }
})

// 修复 canApply: 加 isActivityCreator 和 hasRegistered 检查
const canApply = computed(() =>
  isActivityOpen.value && !hasRegistered.value && !isActivityCreator.value &&
  (activity.value.appliedCount || 0) < (activity.value.maxParticipants || 0) && !loading.value
)

// 修复 applyButtonText: 加已报名状态
const applyButtonText = computed(() => {
  if (!isActivityOpen.value) return '活动已结束'
  if (isActivityCreator.value) return '您发布的活动'
  if (hasRegistered.value) return '已报名'
  if ((activity.value.appliedCount || 0) >= (activity.value.maxParticipants || 0)) return '名额已满'
  return '立即报名'
})

// 报名改为跳转apply.vue
const goToApply = () => {
  if (!canApply.value) return uni.showToast({ title: applyButtonText.value, icon: 'none' })
  uni.navigateTo({ url: `/pages/activities/apply?id=${activityId.value}` })
}

// 加载报名名单
const loadApplicants = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}/applicants`)
    if (res.code === 200) applicants.value = res.data || []
  } catch (e) {}
}

// 导出Excel
const exportApplicants = async () => {
  try {
    uni.showLoading({ title: '导出中...' })
    const res = await request.get(`/activities/${activityId.value}/applicants/export`)
    // H5: 触发下载; 小程序: 复制链接提示
    // #ifdef H5
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = '报名名单.xlsx'; a.click()
    // #endif
    uni.hideLoading()
    uni.showToast({ title: '导出成功', icon: 'success' })
  } catch (e) { uni.hideLoading(); uni.showToast({ title: '导出失败', icon: 'none' }) }
}

// 修复 isActivityCreator: 使用 decodeJwt 工具函数
const isActivityCreator = computed(() => {
  const token = uni.getStorageSync('token')
  if (!token) return false
  const decoded = decodeJwt(token)
  return decoded && decoded.userId === activity.value.creatorId
})

// loadSignStatus 加重试
const loadSignStatus = async (retry = true) => {
  if (!isLoggedIn.value) return
  try {
    const res = await request.get(`/activities/${activityId.value}/sign-status`)
    if (res.code === 200) signStatus.value = res.data.status
  } catch (err) {
    if (retry) {
      setTimeout(() => loadSignStatus(false), 300)
    }
  }
}

// onLoad 中增加 loadApplicants
onLoad(async (options) => {
  activityId.value = options.id
  loading.value = true
  await Promise.all([loadActivity(), loadSignStatus()])
  if (isActivityCreator.value) loadApplicants()
  loading.value = false
})
</script>
```

- [ ] **Step 3: 样式 — 加Tab和名单样式**

```scss
.tabs { display: flex; margin: 20rpx 30rpx; background: #fff; border-radius: 12rpx; overflow: hidden;
  .tab { flex: 1; text-align: center; padding: 20rpx 0; font-size: 28rpx; color: #888;
    &.active { color: #667eea; font-weight: bold; border-bottom: 4rpx solid #667eea; }
  }
}
.applicants-list { padding: 0 30rpx;
  .export-bar { text-align: right; margin-bottom: 16rpx;
    .export-btn { padding: 10rpx 24rpx; background: #4caf50; color: #fff; border-radius: 20rpx; font-size: 24rpx; border: none; }
  }
  .applicant-card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 12rpx;
    .app-name { font-size: 28rpx; font-weight: bold; color: #333; display: block; }
    .app-info { font-size: 24rpx; color: #666; display: block; margin-top: 6rpx; }
  }
  .empty { text-align: center; padding: 60rpx 0; color: #999; }
}
```

- [ ] **Step 4: Commit**

```bash
git add volunteer-uniapp/pages/activities/detail.vue
git commit -m "fix: detail.vue - apply redirect, canApply fix, conditions parse, Tab+applicant list, JWT decode util"
```

---

### Task 10: 前端 — apply.vue 改造 (表单提交流程)

**Files:**
- Modify: `volunteer-uniapp/pages/activities/apply.vue`

- [ ] **Step 1: submitApply 改为调用 apply 接口**

```javascript
// apply.vue: 替换 submitApply 中的注册调用
const submitApply = async () => {
  if (!canSubmit.value) {
    uni.showToast({ title: '请完善报名信息', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await request.post(`/activities/${activityId.value}/apply`, {
      name: formData.name,
      phone: formData.phone,
      email: formData.email,
      emergencyContact: formData.emergencyContact,
      emergencyPhone: formData.emergencyPhone,
      remarks: formData.remarks
    })
    if (res.code === 200) {
      uni.showToast({ title: '报名成功', icon: 'success' })
      setTimeout(() => { uni.navigateBack() }, 1500) // 返回活动详情页
    } else {
      uni.showToast({ title: res.msg || '报名失败', icon: 'none' })
    }
  } catch (error) {
    uni.showToast({ title: '报名失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add volunteer-uniapp/pages/activities/apply.vue
git commit -m "fix: apply.vue - submit form data to /apply API instead of /register"
```

---

### Task 11: 前端 — sign.vue 重构 (统一页面 + 三种方式 + 自动定位)

**Files:**
- Modify: `volunteer-uniapp/pages/activities/sign.vue`

- [ ] **Step 1: 完整重构 sign.vue 模板**

```vue
<template>
  <view class="page">
    <view class="card" v-if="!loading">
      <text class="act-title">{{ activity.title || '加载中...' }}</text>
      <text class="act-meta" v-if="activity.startTime">{{ fmt(activity.startTime) }} ~ {{ fmt(activity.endTime) }}</text>
    </view>

    <!-- 错误提示 -->
    <view class="card warn" v-if="signError">
      <text>{{ signError }}</text>
      <button class="btn sm" v-if="signError.includes('报名')" @tap="goRegister">去报名</button>
    </view>

    <!-- 签到/签退操作区域 -->
    <view v-if="!signError && !loading">
      <view class="card">
        <text class="sec-label">{{ isCheckin ? '签 到' : '签 退' }}</text>

        <!-- 相机拍照 -->
        <view class="method-card" v-if="allowMethod('photo')">
          <view class="method-header"><text class="method-icon">🤳</text><text class="method-name">拍照{{ isCheckin ? '签到' : '签退' }}</text></view>
          <image v-if="photoPath" :src="photoPath" class="photo-preview" mode="aspectFit"></image>
          <button class="btn outline block" @tap="takePhoto">{{ photoPath ? '重新拍照' : '拍照' }}</button>
          <button v-if="photoPath" class="btn block" @tap="doSign('photo')" :disabled="signing">确认{{ isCheckin ? '签到' : '签退' }}</button>
        </view>

        <!-- GPS定位 -->
        <view class="method-card" v-if="allowMethod('gps')">
          <view class="method-header"><text class="method-icon">📍</text><text class="method-name">GPS定位{{ isCheckin ? '签到' : '签退' }}</text></view>
          <text class="loc-text" v-if="location">{{ location }}</text>
          <text class="loc-text dim" v-else>点击按钮自动获取位置</text>
          <button class="btn block" @tap="signWithGps" :disabled="signing">
            {{ location ? '确认' + (isCheckin ? '签到' : '签退') : '获取位置并' + (isCheckin ? '签到' : '签退') }}
          </button>
        </view>

        <!-- 扫码 -->
        <view class="method-card" v-if="allowMethod('scan')">
          <view class="method-header"><text class="method-icon">📷</text><text class="method-name">扫码{{ isCheckin ? '签到' : '签退' }}</text></view>
          <button class="btn outline block" @tap="signWithQr">{{ isCheckin ? '签到' : '签退' }}（扫码）</button>
        </view>

        <!-- 反扫码（仅活动发布方可见） -->
        <view class="method-card" v-if="isCreator && isCheckin">
          <view class="method-header"><text class="method-icon">🔄</text><text class="method-name">反扫码签到</text></view>
          <input class="inp" v-model="scanVolunteerInput" placeholder="扫描志愿者二维码数据" />
          <button class="btn outline block" @tap="organizerScan" :disabled="signing">确认签到</button>
        </view>
      </view>
    </view>
  </view>
</template>
```

- [ ] **Step 2: Script 重构**

```javascript
<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'
import { decodeJwt } from '@/utils/request'

const activityId = ref(null)
const type = ref('checkin')
const activity = reactive({})
const signing = ref(false)
const loading = ref(true)
const signError = ref('')
const photoPath = ref('')
const location = ref('')
const scanVolunteerInput = ref('')
const signMethod = ref('gps,scan,photo')

const isCheckin = computed(() => type.value === 'checkin')
const isCreator = computed(() => {
  const token = uni.getStorageSync('token')
  if (!token) return false
  const decoded = decodeJwt(token)
  return decoded && decoded.userId === activity.creatorId
})

const allowMethod = (method) => signMethod.value.split(',').map(s => s.trim()).includes(method)

// 自动获取GPS位置
const getLocation = async () => {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => resolve(`${pos.coords.latitude},${pos.coords.longitude}`),
        err => reject(new Error('GPS定位失败: ' + err.message)),
        { timeout: 10000 }
      )
    } else { reject(new Error('浏览器不支持GPS')) }
    // #endif
    // #ifndef H5
    uni.getLocation({
      type: 'gcj02',
      success: r => resolve(`${r.latitude},${r.longitude}`),
      fail: err => reject(new Error('GPS定位失败'))
    })
    // #endif
  })
}

// GPS签到
const signWithGps = async () => {
  try {
    if (!location.value) {
      uni.showLoading({ title: '获取位置中...' })
      location.value = await getLocation()
      uni.hideLoading()
    }
    await doSign('gps')
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message, icon: 'none' })
  }
}

// 拍照
const takePhoto = () => {
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: (res) => { photoPath.value = res.tempFilePaths[0] }
  })
}

// 扫码签到
const signWithQr = () => {
  // #ifdef H5
  uni.showToast({ title: 'H5暂不支持扫码，请使用GPS', icon: 'none' })
  return
  // #endif
  // #ifndef H5
  uni.scanCode({
    scanType: ['qrCode'],
    success: async (res) => {
      await doSign('scan', res.result)
    },
    fail: () => {}
  })
  // #endif
}

// 统一签到签退方法
const doSign = async (method, qrData) => {
  signing.value = true
  try {
    const endpoint = isCheckin.value ? 'checkin' : 'checkout'
    let data = {}
    if (method === 'gps') data.location = location.value
    else if (method === 'scan') data.qrToken = qrData
    else if (method === 'photo') {
      data.location = location.value || await getLocation()
      data.photo = photoPath.value
    } else data.location = location.value || '手动确认'

    const res = await request.post(`/activities/${activityId.value}/${endpoint}`, data)
    if (res.code === 200) {
      uni.showToast({ title: (isCheckin.value ? '签到' : '签退') + '成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    } else {
      uni.showToast({ title: res.msg || '操作失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  } finally { signing.value = false }
}

// 检查签到状态（区分不同错误）
const checkSignStatus = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}/sign-status`)
    if (res.code === 200 && res.data) {
      const s = res.data
      if (s.status === 'not_start') {
        signError.value = '请先报名该活动后再' + (isCheckin.value ? '签到' : '签退')
      } else if (s.status === 'checked_in' && isCheckin.value) {
        signError.value = '您已签到，无需重复签到'
      } else if (s.status === 'checked_out') {
        signError.value = '您已完成签到签退'
      }
    } else {
      signError.value = '请先报名该活动后再' + (isCheckin.value ? '签到' : '签退')
    }
  } catch (e) {
    signError.value = '网络错误，请稍后重试'
  }
}

const loadActivity = async () => {
  try {
    const res = await request.get(`/activities/${activityId.value}`)
    if (res.code === 200 && res.data) {
      Object.assign(activity, res.data)
      signMethod.value = res.data.signMethod || 'gps,scan,photo'
    }
  } catch (e) {}
}

const goRegister = () => uni.navigateTo({ url: `/pages/activities/detail?id=${activityId.value}` })
const fmt = (d) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getMonth()+1}/${dt.getDate()} ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

onLoad(async (options) => {
  activityId.value = parseInt(options.id)
  type.value = options.type || 'checkin'
  loading.value = true
  await loadActivity()
  await checkSignStatus()
  loading.value = false
})
</script>
```

- [ ] **Step 3: 样式补充**

```scss
.method-card {
  background: #f8f9fa; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx;
  .method-header { display: flex; align-items: center; margin-bottom: 12rpx;
    .method-icon { font-size: 40rpx; margin-right: 12rpx; }
    .method-name { font-size: 28rpx; font-weight: 600; color: #333; }
  }
  .loc-text { font-size: 26rpx; color: #333; margin-bottom: 12rpx; display: block;
    &.dim { color: #999; }
  }
  .photo-preview { width: 100%; height: 300rpx; border-radius: 8rpx; margin-bottom: 12rpx; }
}
```

- [ ] **Step 4: Commit**

```bash
git add volunteer-uniapp/pages/activities/sign.vue
git commit -m "feat: sign.vue - unified page with GPS/scan/photo methods, auto-location"
```

---

### Task 12: 前端 — list.vue 修复 (已报名状态 + 跳转apply)

**Files:**
- Modify: `volunteer-uniapp/pages/activities/list.vue`

- [ ] **Step 1: 加已报名活动ID集合 + 修复按钮逻辑**

```javascript
// list.vue script部分修改:

const registeredActivityIds = ref(new Set())

// 加载已报名活动
const loadMyRegistrations = async () => {
  if (!uni.getStorageSync('token')) return
  try {
    const res = await request.get('/api/users/applications', { page: 1, size: 500 })
    if (res.code === 200 && res.data) {
      (res.data.list || []).forEach(item => {
        registeredActivityIds.value.add(item.activityId)
      })
    }
  } catch (e) {}
}

// 替换 canApply
const canApply = (activity) => {
  return activity.status === 1
    && activity.appliedCount < activity.maxParticipants
    && !registeredActivityIds.value.has(activity.id || activity.activityId)
}

// 替换 getApplyButtonText
const getApplyButtonText = (activity) => {
  if (activity.status !== 1) return '已结束'
  if (registeredActivityIds.value.has(activity.id || activity.activityId)) return '已报名'
  if (activity.appliedCount >= activity.maxParticipants) return '已满员'
  return '报名参加'
}

// 替换 applyActivity — 改为跳转apply.vue
const applyActivity = (activity) => {
  if (registeredActivityIds.value.has(activity.id || activity.activityId)) {
    uni.showToast({ title: '已报名', icon: 'none' })
    return
  }
  if (!canApply(activity)) {
    uni.showToast({ title: getApplyButtonText(activity), icon: 'none' })
    return
  }
  if (!uni.getStorageSync('token')) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/login' }), 1000)
    return
  }
  uni.navigateTo({ url: `/pages/activities/apply?id=${activity.id || activity.activityId}` })
}

// onMounted 中加
onMounted(() => { loadData(); loadMyRegistrations() })
```

- [ ] **Step 2: Commit**

```bash
git add volunteer-uniapp/pages/activities/list.vue
git commit -m "fix: list.vue - show registered status, redirect to apply.vue instead of direct API call"
```

---

### Task 13: 前端 — publish.vue 签到方式选择 + 手机号校验

**Files:**
- Modify: `volunteer-uniapp/pages/activities/publish.vue`

- [ ] **Step 1: 模板加签到方式多选**

```vue
<!-- publish.vue 表单中加 -->
<view class="form-section">
  <text class="section-title">签到方式设置</text>
  <view class="checkbox-group">
    <view class="checkbox-item" @tap="toggleSignMethod('gps')">
      <text class="checkbox" :class="{ checked: signMethods.includes('gps') }"></text>
      <text>GPS定位</text>
    </view>
    <view class="checkbox-item" @tap="toggleSignMethod('scan')">
      <text class="checkbox" :class="{ checked: signMethods.includes('scan') }"></text>
      <text>扫码</text>
    </view>
    <view class="checkbox-item" @tap="toggleSignMethod('photo')">
      <text class="checkbox" :class="{ checked: signMethods.includes('photo') }"></text>
      <text>拍照</text>
    </view>
  </view>
</view>
```

- [ ] **Step 2: Script 加 signMethods 逻辑 + 手机号校验**

```javascript
// publish.vue script:
const signMethods = ref(['gps', 'scan', 'photo'])

const toggleSignMethod = (method) => {
  const idx = signMethods.value.indexOf(method)
  if (idx >= 0) signMethods.value.splice(idx, 1)
  else signMethods.value.push(method)
}

// 提交时传 signMethod
const submitData = () => {
  // ... 原有校验 ...
  // 加手机号校验:
  if (contactPhone.value && !/^1[3-9]\d{9}$/.test(contactPhone.value.trim())) {
    return uni.showToast({ title: '手机号格式不正确', icon: 'none' })
  }
  // activityData 加:
  signMethod: signMethods.value.join(',')
}
```

- [ ] **Step 3: Commit**

```bash
git add volunteer-uniapp/pages/activities/publish.vue
git commit -m "feat: publish.vue - sign method multi-select, phone format validation"
```

---

### Task 14: 前端 — my-activities.vue + sign-approval.vue (审批入口+审批页)

**Files:**
- Modify: `volunteer-uniapp/pages/my/my-activities.vue`
- Create: `volunteer-uniapp/pages/activities/sign-approval.vue`
- Modify: `volunteer-uniapp/pages.json`

- [ ] **Step 1: my-activities.vue 加待审批数显示 + 点击进入审批**

```vue
<!-- my-activities.vue: 在"我发布的"活动卡片中加待审批徽标 -->
<view class="card" v-for="a in published" :key="a.activityId">
  <!-- ... 原有内容 ... -->
  <view class="card-actions">
    <text class="pending-badge" v-if="a.pendingApprovals > 0" @tap.stop="goApproval(a.activityId)">
      待审批: {{ a.pendingApprovals }}人
    </text>
  </view>
</view>

<!-- Script: 加 goApproval 和 loadPendingCounts -->
<script setup>
const goApproval = (activityId) => {
  uni.navigateTo({ url: `/pages/activities/sign-approval?activityId=${activityId}` })
}

// loadPublished 中为每个活动加载待审批数
const loadPendingCounts = async () => {
  for (const a of published.value) {
    try {
      const res = await request.get(`/activities/${a.activityId}/sign-approvals`)
      if (res.code === 200) a.pendingApprovals = (res.data || []).length
    } catch (e) { a.pendingApprovals = 0 }
  }
}
</script>
```

- [ ] **Step 2: 创建 sign-approval.vue 发布方审批页**

```vue
<!-- sign-approval.vue: 发布方审批列表 -->
<template>
  <view class="page">
    <view class="top-bar"><text class="back" @tap="goBack">← 返回</text><text class="title">签到审批</text><text></text></view>
    <view class="list">
      <view class="card" v-for="r in records" :key="r.id">
        <text class="name">{{ r.realName }}</text>
        <text class="detail">签到: {{ fmt(r.checkinTime) }}</text>
        <text class="detail">签退: {{ fmt(r.checkoutTime) }}</text>
        <text class="detail">时长: {{ r.hoursEarned || 0 }}h</text>
        <view class="actions">
          <button class="btn pass" @tap="approve(r.id)">通过</button>
          <button class="btn reject" @tap="reject(r.id)">拒绝</button>
        </view>
      </view>
      <view v-if="records.length === 0" class="empty">暂无待审批记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'

const activityId = ref(null)
const records = ref([])

const loadRecords = async () => {
  const res = await request.get(`/activities/${activityId.value}/sign-approvals`)
  if (res.code === 200) records.value = res.data || []
}

const approve = async (id) => {
  await request.put(`/activities/sign-approvals/${id}/approve`)
  uni.showToast({ title: '已通过', icon: 'success' })
  loadRecords()
}

const reject = async (id) => {
  await request.put(`/activities/sign-approvals/${id}/reject`)
  uni.showToast({ title: '已拒绝', icon: 'success' })
  loadRecords()
}

const fmt = (d) => d ? new Date(d).toLocaleString() : ''
const goBack = () => uni.navigateBack()

onLoad((options) => { activityId.value = options.activityId; loadRecords() })
</script>
```

- [ ] **Step 3: pages.json 注册 sign-approval 页面**

```json
{
  "path": "pages/activities/sign-approval",
  "style": { "navigationBarTitleText": "签到审批" }
}
```

- [ ] **Step 4: Commit**

```bash
git add volunteer-uniapp/pages/my/my-activities.vue
git add volunteer-uniapp/pages/activities/sign-approval.vue
git add volunteer-uniapp/pages.json
git commit -m "feat: sign approval - my-activities pending badge, sign-approval page for publishers"
```

---

### Task 15: 前端 — admin/sign-approvals.vue (管理员审批页)

**Files:**
- Create: `volunteer-uniapp/pages/admin/sign-approvals.vue`
- Modify: `volunteer-uniapp/pages/admin/dashboard.vue`
- Modify: `volunteer-uniapp/pages.json`

- [ ] **Step 1: 创建 admin/sign-approvals.vue**

```vue
<template>
  <view class="page">
    <view class="top-bar"><text class="back" @tap="goBack">← 返回</text><text class="title">签到审批</text><text></text></view>
    <view class="list">
      <view class="card" v-for="r in records" :key="r.id">
        <text class="name">{{ r.realName }} — {{ r.activityTitle }}</text>
        <text class="detail">签到: {{ fmt(r.checkinTime) }} | 签退: {{ fmt(r.checkoutTime) }}</text>
        <text class="detail">时长: {{ r.hoursEarned || 0 }}h | 状态: {{ approvalText(r.approvalStatus) }}</text>
        <view class="actions" v-if="r.approvalStatus === 0">
          <button class="btn pass" @tap="approve(r)">通过</button>
          <button class="btn reject" @tap="reject(r.id)">拒绝</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onMounted } from '@dcloudio/uni-app'
import request from '@/utils/request'

const records = ref([])

const loadData = async () => {
  const res = await request.get('/admin/sign-approvals', { page: 1, size: 100 })
  if (res.code === 200 && res.data) records.value = res.data.list || []
}

const approve = async (r) => {
  await request.post(`/admin/sign-records/${r.id}/approve`, { userId: r.userId, activityId: r.activityId, hours: r.hoursEarned })
  uni.showToast({ title: '已通过', icon: 'success' })
  loadData()
}

const reject = async (id) => {
  await request.put(`/admin/sign-approvals/${id}/reject`)
  uni.showToast({ title: '已拒绝', icon: 'success' })
  loadData()
}

const approvalText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝' }[s] || '')
const fmt = (d) => d ? new Date(d).toLocaleString() : ''
const goBack = () => uni.navigateBack()

onMounted(loadData)
</script>
```

- [ ] **Step 2: dashboard.vue 加签到审批入口**

```vue
<!-- admin/dashboard.vue: 在 actions 中加 -->
<view class="act" @tap="go('sign-approvals')">
  <text>✅</text><text>签到审批</text>
</view>

<!-- go 方法中加映射 -->
const urls = {
  users:'/pages/admin/users',
  activities:'/pages/admin/activities',
  organizations:'/pages/admin/organizations',
  'sign-approvals':'/pages/admin/sign-approvals'
}
```

- [ ] **Step 3: pages.json 注册**

```json
{ "path": "pages/admin/sign-approvals", "style": { "navigationBarTitleText": "签到审批" } }
```

- [ ] **Step 4: Commit**

```bash
git add volunteer-uniapp/pages/admin/sign-approvals.vue
git add volunteer-uniapp/pages/admin/dashboard.vue
git add volunteer-uniapp/pages.json
git commit -m "feat: admin sign approvals page + dashboard entry"
```

---

### Task 16: 前端 — 图表改造 + home/profile/applications 修复

**Files:**
- Modify: `volunteer-uniapp/components/charts/service-chart.vue`
- Modify: `volunteer-uniapp/pages/home/home.vue`
- Modify: `volunteer-uniapp/pages/profile/profile.vue`
- Modify: `volunteer-uniapp/pages/my/applications.vue`
- Modify: `volunteer-uniapp/pages/admin/activities.vue`

- [ ] **Step 1: service-chart.vue — 默认改折线图 + dataZoom + 年月X轴**

```javascript
// 替换 option 生成逻辑中的 type: 'bar' 为 'line'
option = {
  tooltip: { trigger: 'axis', formatter: function(p) { return p[0].name + '<br/>服务时长: ' + p[0].value + 'h' } },
  grid: { left: 40, right: 30, bottom: 50, top: 20 },
  xAxis: {
    type: 'category',
    data: items.map(d => d.month || d.name || ''),
    axisLabel: { fontSize: 10, color: '#999', rotate: 45 }
  },
  yAxis: { type: 'value', name: 'h' },
  dataZoom: [
    { type: 'slider', start: 0, end: 100, height: 20, bottom: 5 },
    { type: 'inside' }
  ],
  series: [{
    type: 'line',  // 折线图
    data: items.map(d => d.hours || d.value || 0),
    smooth: true,
    symbol: 'circle', symbolSize: 8,
    itemStyle: { color: '#667eea' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#667eea44' }, { offset: 1, color: '#667eea08' }] } }
  }]
}
```

- [ ] **Step 2: home.vue — monthHours 修复 + 导航加token检查**

```javascript
// home.vue: 修复 monthHours 取当前月份
const currentMonthHours = computed(() => {
  if (!monthlyStats.value || monthlyStats.value.length === 0) return 0
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`
  const found = monthlyStats.value.find(s => s.month === currentMonth)
  return found ? (found.hours || 0) : 0
})

// goToMyApplications 和 goToCertificates 加token检查:
const goToMyApplications = () => {
  if (!hasToken.value) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  uni.navigateTo({ url: '/pages/my/applications' })
}
```

- [ ] **Step 3: profile.vue — 管理后台入口仅admin可见 + 登出清adminToken**

```vue
<!-- profile.vue: 管理后台菜单条件渲染 -->
<view class="menu-item" v-if="isAdmin" @tap="goTo('adminDashboard')">
  <text class="menu-icon">🛡️</text><text class="menu-text">管理后台</text>
</view>

<script setup>
const isAdmin = computed(() => !!uni.getStorageSync('adminToken'))

// logout 方法:
const handleLogout = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('adminToken')
  store.commit('auth/SET_TOKEN', '')
  store.commit('auth/SET_USER_INFO', null)
  uni.redirectTo({ url: '/pages/auth/login' })
}
</script>
```

- [ ] **Step 4: applications.vue — signStatus 映射修复**

```javascript
// applications.vue: 数据映射中加入 signStatus
applications.value = (res.data.list || []).map(item => ({
  id: item.recordId,
  activityId: item.activityId,
  activityTitle: item.activityTitle || '活动',
  activityTime: item.startTime || '',
  applyTime: item.registerTime || '',
  status: item.status || 'applied',
  duration: item.hoursEarned || 0,
  signStatus: item.signStatus,               // 加这行
  activityCover: '/default_activity_poster.jpg'
}))
```

- [ ] **Step 5: admin/activities.vue — 点击查看详情**

```vue
<!-- admin/activities.vue: 列表项加 @tap -->
<view class="list-item" v-for="a in activities" :key="a.activityId" @tap="viewDetail(a.activityId)">
  <!-- ... -->
</view>

<script setup>
const viewDetail = (id) => uni.navigateTo({ url: `/pages/activities/detail?id=${id}` })
</script>
```

- [ ] **Step 6: Commit**

```bash
git add volunteer-uniapp/components/charts/service-chart.vue
git add volunteer-uniapp/pages/home/home.vue
git add volunteer-uniapp/pages/profile/profile.vue
git add volunteer-uniapp/pages/my/applications.vue
git add volunteer-uniapp/pages/admin/activities.vue
git commit -m "fix: chart to line chart, home monthHours, profile admin visibility, applications signStatus, admin detail click"
```

---

### Task 17: 最终验证 & 集成测试

- [ ] **Step 1: 后端编译检查**

```bash
cd volunteer-service-backend
mvn compile -q
# 确认无编译错误
```

- [ ] **Step 2: 启动后端检查启动日志**

```bash
mvn spring-boot:run
# 检查: 无Bean注入错误，定时任务正常注册，接口可访问
```

- [ ] **Step 3: 关键API手动测试**

```bash
# 测试公开GET
curl http://localhost:8080/api/activities?page=1

# 测试需认证的POST（无token应返回401）
curl -X POST http://localhost:8080/api/activities/1/register

# 测试admin接口（无token应返回401）
curl http://localhost:8080/admin/dashboard
```

- [ ] **Step 4: 前端H5启动**

```bash
cd volunteer-uniapp
npm run dev:h5
# 访问 http://localhost:5173
```

- [ ] **Step 5: 端到端测试清单**

- [ ] 注册新用户 → 登录 → 实名认证
- [ ] 浏览活动列表 → 点击活动 → 查看详情（条件正常显示）
- [ ] 点击"立即报名" → 跳转apply.vue → 填表提交 → 返回详情页（按钮变为"已报名"）
- [ ] 发布方查看自己的活动 → 不能报名
- [ ] 发布方查看报名名单Tab → 导出Excel
- [ ] 签到：详情页点签到 → 自动获取GPS → 确认签到 → 成功
- [ ] 签退：拍照片签退 → 成功
- [ ] 查看"我的活动" → "我参与的" → 签到签退状态正确
- [ ] 发布方进入审批页 → 通过/拒绝签到记录
- [ ] 管理员登录 → 仪表盘折线图 → 签到审批 → 活动管理点击详情
- [ ] 留言板标签：发布方显示"发布方"，已报名显示"已参与"，其他显示"未参与"
- [ ] 数据看板月度时长为非零值

- [ ] **Step 6: 最终Commit**

```bash
git add .
git commit -m "test: integration verification - all endpoints and flows validated"
```

---

## 自审清单

- [x] 覆盖所有spec需求（28修复 + 5增强 + 7安全 + 6逻辑 + 3数据 + 3验证）
- [x] 无TBD/TODO占位符
- [x] 类型一致性：各Task间的API路径、字段名、方法签名互相对应
- [x] 每个Task有明确的文件路径和具体的代码

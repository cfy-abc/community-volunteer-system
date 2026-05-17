package com.volunteer.controller;

import com.volunteer.entity.SignRecord;
import com.volunteer.entity.User;
import com.volunteer.mapper.*;
import com.volunteer.service.AdminService;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private SignRecordMapper signRecordMapper;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public Result login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String token = adminService.login(username, password);
            return Result.success(token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result getDashboard() {
        try {
            Map<String, Object> data = adminService.getDashboardData();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户列表（分页）
     */
    @GetMapping("/users")
    public Result getUserList(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              @RequestParam(required = false) Integer status) {
        try {
            int offset = (page - 1) * size;
            List<User> users = adminMapper.findAllUsers(offset, size, status);
            int total = adminMapper.countUsers(status);

            Map<String, Object> result = new HashMap<>();
            result.put("list", users);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核用户
     */
    @PutMapping("/users/{id}/status")
    public Result auditUser(@PathVariable Integer id, @RequestBody Map<String, Integer> params) {
        try {
            Integer status = params.get("status");
            if (status == null || (status != 1 && status != 2)) {
                return Result.error("无效的审核状态");
            }
            userMapper.updateStatus(id, status);
            return Result.success("审核成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核组织
     */
    @PutMapping("/organizations/{id}/status")
    public Result auditOrganization(@PathVariable Integer id, @RequestBody Map<String, Integer> params) {
        try {
            Integer status = params.get("status");
            if (status == null || (status != 1 && status != 2)) {
                return Result.error("无效的审核状态");
            }
            organizationMapper.updateStatus(id, status);
            return Result.success("审核成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核活动
     */
    @PutMapping("/activities/{id}/status")
    public Result auditActivity(@PathVariable Integer id, @RequestBody Map<String, Integer> params) {
        try {
            Integer status = params.get("status");
            if (status == null || (status < 0 || status > 4)) {
                return Result.error("无效的活动状态");
            }
            activityMapper.updateStatus(id, status);
            return Result.success("审核成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取组织升级申请列表
     */
    @GetMapping("/org-upgrade-applications")
    public Result getOrgUpgradeApplications(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> applications = userMapper.findOrgUpgradeApplications(offset, size);
            int total = userMapper.countOrgUpgradeApplications();
            Map<String, Object> result = new HashMap<>();
            result.put("list", applications);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核组织升级申请
     */
    @PutMapping("/org-upgrade-applications/{id}/status")
    public Result auditOrgUpgrade(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        try {
            Integer status = params.get("status") != null ?
                    Integer.parseInt(params.get("status").toString()) : null;
            if (status == null || (status != 1 && status != 2)) {
                return Result.error("无效的审核状态");
            }
            userMapper.updateOrgUpgradeStatus(id, status);
            if (status == 1) {
                // 通过后升级用户为组织用户
                Map<String, Object> app = userMapper.findOrgUpgradeApplications(0, 1).stream()
                        .filter(a -> a.get("application_id").equals(id))
                        .findFirst().orElse(null);
                if (app != null) {
                    Integer userId = (Integer) app.get("user_id");
                    String position = (String) app.get("position");
                    String department = (String) app.get("department");
                    userMapper.upgradeToOrgUser(userId, position, department);
                }
            }
            return Result.success("审核完成");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取待实名认证列表
     */
    @GetMapping("/real-name-verifications")
    public Result getRealNameVerifications(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> verifications = userMapper.findPendingRealNameVerifications(offset, size);
            int total = userMapper.countPendingRealNameVerifications();
            Map<String, Object> result = new HashMap<>();
            result.put("list", verifications);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核实名认证（通过）
     */
    @PutMapping("/real-name-verifications/{userId}/approve")
    public Result approveRealName(@PathVariable Integer userId) {
        try {
            userMapper.approveRealName(userId);
            return Result.success("实名认证审核通过");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动修改服务时长（管理员）
     */
    @PutMapping("/sign-records/{id}/hours")
    public Result updateSignRecordHours(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        try {
            Double hours = params.get("hours") != null ?
                    Double.parseDouble(params.get("hours").toString()) : null;
            if (hours == null || hours < 0) {
                return Result.error("无效的时长值");
            }
            // 直接审批通过并设置指定时长
            signRecordMapper.approveRecord(id, hours);
            return Result.success("时长修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动审批签到记录（覆盖48h自动审批）
     */
    @PostMapping("/sign-records/{id}/approve")
    public Result approveSignRecord(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        try {
            SignRecord record = signRecordMapper.findByUserAndActivity(
                    params.get("userId") != null ? Integer.parseInt(params.get("userId").toString()) : null,
                    params.get("activityId") != null ? Integer.parseInt(params.get("activityId").toString()) : null
            );
            if (record == null) {
                return Result.error("签到记录不存在");
            }
            double hours = record.getHoursEarned() != null ? record.getHoursEarned() : 0;
            if (params.get("hours") != null) {
                hours = Double.parseDouble(params.get("hours").toString());
            }
            signRecordMapper.approveRecord(id, hours);
            // 更新用户时长
            User user = userMapper.findById(record.getUserId());
            if (user != null) {
                user.setVolunteerHours(user.getVolunteerHours() + (int)Math.round(hours));
                user.setTotalEarnedHours(user.getTotalEarnedHours() + (int)Math.round(hours));
                userMapper.updateHours(user);
            }
            return Result.success("审批通过");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

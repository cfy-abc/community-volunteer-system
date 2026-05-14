package com.volunteer.controller;

import com.volunteer.entity.User;
import com.volunteer.mapper.AdminMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.mapper.OrganizationMapper;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.service.AdminService;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
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

    /**
     * 管理员登录
     */
    @PostMapping("/login")
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
}

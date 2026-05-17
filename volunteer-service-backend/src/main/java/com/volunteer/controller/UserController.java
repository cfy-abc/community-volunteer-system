package com.volunteer.controller;

import com.volunteer.dto.OrganizationDTO;
import com.volunteer.dto.UserDTO;
import com.volunteer.service.UserService;
import com.volunteer.service.VolunteerRecordService;
import com.volunteer.utils.JwtUtils;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600) // 允许前端跨域请求
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private VolunteerRecordService volunteerRecordService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 健康检查/测试端点
     */
    @GetMapping("/ping")
    public Result ping() {
        return Result.success("pong - 后端服务正常运行");
    }

    private Integer getUserIdFromToken(String token) {
        if (token == null) return null;
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        System.out.println("接收到的用户对象: " + userDTO);
        System.out.println("realName = " + userDTO.getRealName());
        try {
            userService.register(userDTO);
            return Result.success("注册成功，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserDTO loginDTO) {
        try {
            System.out.println("登录尝试: " + loginDTO.getUsername());
            String token = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            System.out.println("登录成功，生成token: " + token);
            return Result.success(token);
        } catch (Exception e) {
            System.err.println("登录失败: " + e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            UserDTO userDTO = userService.getCurrentUser(token);
            return Result.success(userDTO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/info/{userId}")
    public Result getUserInfo(@PathVariable Integer userId) {
        try {
            UserDTO userDTO = userService.getUserById(userId);
            return Result.success(userDTO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户个人信息（手机号等）
     */
    @PutMapping("/update")
    public Result updateUserInfo(@RequestHeader("Authorization") String token,
                                 @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            UserDTO userDTO = userService.updateUserInfo(userId, params);
            return Result.success(userDTO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息（不需要传userId）
     */
    @GetMapping("/info")
    public Result getUserInfoByToken(@RequestHeader("Authorization") String token) {
        try {
            UserDTO userDTO = userService.getCurrentUser(token);
            return Result.success(userDTO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建组织
     */
    @PostMapping("/organizations")
    public Result createOrganization(@RequestHeader("Authorization") String token,
                                     @RequestBody OrganizationDTO organizationDTO) {
        try {
            userService.createOrganization(token, organizationDTO);
            return Result.success("组织创建成功，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户的报名记录
     */
    @GetMapping("/applications")
    public Result getUserApplications(@RequestHeader("Authorization") String token,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        try {
            Integer userId = getUserIdFromToken(token);
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("status", status);
            params.put("offset", (page - 1) * size);
            params.put("limit", size);
            List<?> records = volunteerRecordService.getUserRecords(params);
            int total = volunteerRecordService.countUserRecords(params);
            Map<String, Object> result = new HashMap<>();
            result.put("list", records);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实名认证
     */
    @PostMapping("/verify-identity")
    public Result verifyIdentity(@RequestHeader("Authorization") String token,
                                 @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String realName = params.get("realName");
            String idCard = params.get("idCard");
            if (realName == null || idCard == null) {
                return Result.error("姓名和身份证号不能为空");
            }
            if (!idCard.matches("^\\d{17}[\\dXx]$")) {
                return Result.error("身份证号格式不正确");
            }
            // 校验姓名是否与注册时一致
            userService.verifyIdentity(userId, realName, idCard);
            return Result.success("实名认证提交成功，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取实名认证状态
     */
    @GetMapping("/verify-status")
    public Result getVerifyStatus(@RequestHeader("Authorization") String token) {
        try {
            Integer userId = getUserIdFromToken(token);
            Map<String, Object> status = userService.getVerifyStatus(userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 申请组织用户升级
     */
    @PostMapping("/apply-org-upgrade")
    public Result applyOrgUpgrade(@RequestHeader("Authorization") String token,
                                   @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String position = params.get("position");
            String department = params.get("department");
            String reason = params.get("reason");
            if (position == null || department == null) {
                return Result.error("职位和所属组织不能为空");
            }
            userService.applyOrgUpgrade(userId, position, department, reason);
            return Result.success("申请已提交，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取组织升级申请状态
     */
    @GetMapping("/org-upgrade-status")
    public Result getOrgUpgradeStatus(@RequestHeader("Authorization") String token) {
        try {
            Integer userId = getUserIdFromToken(token);
            Map<String, Object> status = userService.getOrgUpgradeStatus(userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户服务统计数据
     */
    @GetMapping("/stats")
    public Result getUserStats(@RequestHeader("Authorization") String token) {
        try {
            Integer userId = getUserIdFromToken(token);
            Map<String, Object> stats = userService.getUserStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消报名申请
     */
    @PostMapping("/applications/{id}/cancel")
    public Result cancelApplication(@RequestHeader("Authorization") String token,
                                    @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromToken(token);
            userService.cancelApplication(userId, id);
            return Result.success("取消成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取微信OAuth授权URL
     */
    @GetMapping("/wechat/auth-url")
    public Result getWechatAuthUrl() {
        try {
            String authUrl = userService.getWechatAuthUrl();
            return Result.success(Map.of("url", authUrl));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 微信登录
     */
    @PostMapping("/wechat/login")
    public Result wechatLogin(@RequestBody Map<String, String> params) {
        try {
            String code = params.get("code");
            if (code == null || code.isEmpty()) {
                return Result.error("授权码不能为空");
            }
            String token = userService.wechatLogin(code);
            return Result.success(token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
package com.volunteer.controller;

import com.volunteer.dto.OrganizationDTO;
import com.volunteer.dto.UserDTO;
import com.volunteer.service.UserService;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600) // 允许前端跨域请求
public class UserController {

    @Autowired
    private UserService userService;

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
}
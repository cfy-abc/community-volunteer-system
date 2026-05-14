package com.volunteer.service.impl;

import com.volunteer.entity.Admin;
import com.volunteer.mapper.AdminMapper;
import com.volunteer.service.AdminService;
import com.volunteer.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public String login(String username, String password) throws Exception {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new Exception("管理员账号不存在");
        }
        if (admin.getStatus() != 1) {
            throw new Exception("管理员账号已被禁用");
        }
        if (!jwtUtils.checkPassword(password, admin.getPassword())) {
            throw new Exception("密码错误");
        }
        adminMapper.updateLoginTime(admin.getAdminId());
        return jwtUtils.generateToken(admin.getAdminId(), admin.getUsername());
    }

    @Override
    public Map<String, Object> getDashboardData() throws Exception {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", adminMapper.totalUsers());
        data.put("totalActivities", adminMapper.totalActivities());
        data.put("totalOrganizations", adminMapper.totalOrganizations());
        data.put("totalVolunteerHours", adminMapper.totalVolunteerHours());

        List<com.volunteer.entity.User> topVolunteers = adminMapper.topVolunteers(10);
        data.put("topVolunteers", topVolunteers);

        List<Map<String, Object>> topOrgs = adminMapper.topOrganizations(10);
        data.put("topOrganizations", topOrgs);

        List<Map<String, Object>> typeDistribution = adminMapper.activityTypeDistribution();
        data.put("activityTypeDistribution", typeDistribution);

        return data;
    }
}

package com.volunteer.service.impl;

import com.volunteer.entity.Admin;
import com.volunteer.mapper.AdminMapper;
import com.volunteer.service.AdminService;
import com.volunteer.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

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
        return jwtUtils.generateToken(admin.getAdminId(), admin.getUsername(), "admin");
    }

    @Override
    public Map<String, Object> getDashboardData() throws Exception {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", adminMapper.totalUsers());
        data.put("totalActivities", adminMapper.totalActivities());
        data.put("totalOrganizations", adminMapper.totalOrganizations());

        List<com.volunteer.entity.User> topVolunteers = adminMapper.topVolunteers(10);
        data.put("topVolunteers", topVolunteers);

        List<Map<String, Object>> topOrgs = adminMapper.topOrganizations(10);
        data.put("topOrganizations", topOrgs);

        // Compute stats from raw data — one query, consistent results in Java
        List<Map<String, Object>> rows = adminMapper.getDashboardStatsRaw();

        double totalHours = 0;
        Map<String, Double> monthlyMap = new LinkedHashMap<>();
        Map<String, Double> typeMap = new LinkedHashMap<>();
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");

        for (Map<String, Object> row : rows) {
            Number srHours = (Number) row.get("srHours");
            Number vrHours = (Number) row.get("vrHours");
            double hours = (srHours != null && srHours.doubleValue() > 0)
                    ? srHours.doubleValue()
                    : (vrHours != null ? vrHours.doubleValue() : 0);
            totalHours += hours;

            // Monthly grouping
            Object checkoutObj = row.get("checkoutTime");
            Object checkinObj = row.get("checkinTime");
            java.util.Date date = null;
            if (checkoutObj instanceof java.util.Date) date = (java.util.Date) checkoutObj;
            else if (checkoutObj instanceof java.sql.Timestamp) date = (java.util.Date) checkoutObj;
            if (date == null && checkinObj instanceof java.util.Date) date = (java.util.Date) checkinObj;
            if (date == null && checkinObj instanceof java.sql.Timestamp) date = (java.util.Date) checkinObj;

            if (date != null) {
                String monthKey = monthFmt.format(date);
                monthlyMap.merge(monthKey, hours, Double::sum);
            }

            // Type grouping
            String activityType = (String) row.get("activityType");
            if (activityType != null && !activityType.isEmpty()) {
                typeMap.merge(activityType, hours, Double::sum);
            }
        }

        data.put("totalVolunteerHours", (int) Math.round(totalHours));

        // Build monthly list (sorted)
        List<Map<String, Object>> monthlyHours = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(monthlyMap.keySet());
        Collections.sort(sortedMonths);
        for (String m : sortedMonths) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("month", m);
            entry.put("hours", Math.round(monthlyMap.get(m) * 10) / 10.0);
            monthlyHours.add(entry);
        }
        data.put("monthlyVolunteerHours", monthlyHours);

        // Build type distribution list
        List<Map<String, Object>> typeDistribution = new ArrayList<>();
        for (Map.Entry<String, Double> e : typeMap.entrySet()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", e.getKey());
            entry.put("value", Math.round(e.getValue() * 10) / 10.0);
            typeDistribution.add(entry);
        }
        data.put("activityTypeDistribution", typeDistribution);

        return data;
    }
}

package com.volunteer.service.impl;

import com.volunteer.dto.UserDTO;
import com.volunteer.dto.OrganizationDTO;
import com.volunteer.entity.Organization;
import com.volunteer.entity.User;
import com.volunteer.entity.VolunteerRecord;
import com.volunteer.mapper.OrganizationMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.mapper.VolunteerRecordMapper;
import com.volunteer.service.UserService;
import com.volunteer.utils.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private VolunteerRecordMapper volunteerRecordMapper;

    @Value("${wechat.app-id:YOUR_WECHAT_APPID}")
    private String wechatAppId;

    @Value("${wechat.app-secret:YOUR_WECHAT_APPSECRET}")
    private String wechatAppSecret;

    @Value("${wechat.oauth2-redirect-uri:http://localhost:5173}")
    private String wechatRedirectUri;

    @Override
    @Transactional
    public void register(UserDTO userDTO) throws Exception {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(userDTO.getUsername()) != null) {
            throw new Exception("用户名已存在");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(jwtUtils.encodePassword(userDTO.getPassword())); // 密码加密
        user.setRealName(userDTO.getRealName());
        user.setPhone(userDTO.getPhone());
        user.setVolunteerHours(0);
        user.setTotalEarnedHours(0);
        user.setTotalSpentHours(0);
        user.setStatus(1); // 普通用户注册后直接通过，无需审核
        user.setAvatar("/default_avatar.png");

        userMapper.insert(user);
    }

    @Override
    public String login(String username, String password) throws Exception {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        if (!jwtUtils.checkPassword(password, user.getPassword())) {
            throw new Exception("密码错误");
        }

        if (user.getStatus() != 1) {
            throw new Exception("账户未通过审核");
        }

        // 生成JWT token
        return jwtUtils.generateToken(user.getUserId(), user.getUsername(), "user");
    }

    @Override
    public UserDTO updateUserInfo(Integer userId, Map<String, String> params) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null) throw new Exception("用户不存在");
        if (params.containsKey("username")) {
            // 检查新用户名是否已被占用
            String newUsername = params.get("username");
            User existing = userMapper.findByUsername(newUsername);
            if (existing != null && !existing.getUserId().equals(userId)) {
                throw new Exception("该用户名已被占用");
            }
            user.setUsername(newUsername);
        }
        if (params.containsKey("phone")) user.setPhone(params.get("phone"));
        if (params.containsKey("avatar")) user.setAvatar(params.get("avatar"));
        userMapper.updateInfo(user);
        return getUserById(userId);
    }

    @Override
    public UserDTO getCurrentUser(String token) throws Exception {
        Integer userId = jwtUtils.getUserIdFromToken(token);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhone(user.getPhone());
        userDTO.setVolunteerHours(user.getVolunteerHours());
        userDTO.setTotalEarnedHours(user.getTotalEarnedHours());
        userDTO.setTotalSpentHours(user.getTotalSpentHours());
        userDTO.setStatus(user.getStatus());
        userDTO.setAvatar(user.getAvatar());
        userDTO.setIdCard(user.getIdCard());
        userDTO.setRealNameStatus(user.getRealNameStatus() != null ? user.getRealNameStatus() : 0);
        userDTO.setIsOrgUser(user.getIsOrgUser() != null ? user.getIsOrgUser() : 0);
        userDTO.setOrgPosition(user.getOrgPosition());
        userDTO.setOrgDepartment(user.getOrgDepartment());

        return userDTO;
    }

    @Override
    public UserDTO getUserById(Integer userId) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhone(user.getPhone());
        userDTO.setVolunteerHours(user.getVolunteerHours());
        userDTO.setTotalEarnedHours(user.getTotalEarnedHours());
        userDTO.setTotalSpentHours(user.getTotalSpentHours());
        userDTO.setStatus(user.getStatus());
        userDTO.setAvatar(user.getAvatar());
        userDTO.setIdCard(user.getIdCard());
        userDTO.setRealNameStatus(user.getRealNameStatus() != null ? user.getRealNameStatus() : 0);
        userDTO.setIsOrgUser(user.getIsOrgUser() != null ? user.getIsOrgUser() : 0);
        userDTO.setOrgPosition(user.getOrgPosition());
        userDTO.setOrgDepartment(user.getOrgDepartment());

        return userDTO;
    }

    @Override
    @Transactional
    public void createOrganization(String token, OrganizationDTO organizationDTO) throws Exception {
        Integer userId = jwtUtils.getUserIdFromToken(token);
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new Exception("用户不存在或未通过审核");
        }

        if (organizationMapper.findByCreatorId(userId) != null) {
            throw new Exception("您已创建过组织，不能重复创建");
        }

        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDTO, organization);
        organization.setCreatorId(userId);
        organization.setStatus(0);
        organization.setLogo(organizationDTO.getLogo() != null ? organizationDTO.getLogo() : "/default_org_logo.png");

        organizationMapper.insert(organization);
    }

    @Override
    public void verifyIdentity(Integer userId, String realName, String idCard) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null) throw new Exception("用户不存在");
        // 校验姓名是否与注册时一致
        if (user.getRealName() != null && !user.getRealName().equals(realName)) {
            throw new Exception("姓名与注册时不一致，认证失败");
        }
        // 系统自动审核通过
        userMapper.updateRealNameStatus(userId, realName, idCard, 1);
    }

    @Override
    public Map<String, Object> getVerifyStatus(Integer userId) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null) throw new Exception("用户不存在");
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("realNameStatus", user.getRealNameStatus() != null ? user.getRealNameStatus() : 0);
        status.put("idCard", user.getIdCard());
        return status;
    }

    @Override
    public void applyOrgUpgrade(Integer userId, String position, String department, String reason) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1) throw new Exception("用户不存在或未通过审核");
        userMapper.insertOrgUpgradeApplication(userId, position, department, reason);
    }

    @Override
    public Map<String, Object> getOrgUpgradeStatus(Integer userId) throws Exception {
        User user = userMapper.findById(userId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("isOrgUser", user.getIsOrgUser() != null && user.getIsOrgUser() == 1);
        result.put("orgPosition", user.getOrgPosition());
        result.put("orgDepartment", user.getOrgDepartment());
        result.put("status", 0);
        return result;
    }

    @Override
    public Map<String, Object> getUserStats(Integer userId) throws Exception {
        Map<String, Object> stats = new java.util.HashMap<>();
        User user = userMapper.findById(userId);
        if (user == null) throw new Exception("用户不存在");

        stats.put("volunteerHours", user.getVolunteerHours());
        stats.put("totalEarnedHours", user.getTotalEarnedHours() != null ? user.getTotalEarnedHours() : 0);
        stats.put("totalSpentHours", user.getTotalSpentHours() != null ? user.getTotalSpentHours() : 0);

        // Load raw data: one query, compute everything in Java for guaranteed consistency
        java.util.List<Map<String, Object>> rows = userMapper.getUserStatsRaw(userId);

        // Compute total hours from raw data (sr hours preferred, fallback to vr hours)
        double computedTotal = 0;
        java.util.Map<String, Double> monthlyMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Double> typeMap = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Double> yearlyMap = new java.util.LinkedHashMap<>();

        java.text.SimpleDateFormat monthFmt = new java.text.SimpleDateFormat("yyyy-MM");
        java.util.Calendar cal = java.util.Calendar.getInstance();

        for (Map<String, Object> row : rows) {
            Number srHours = (Number) row.get("srHours");
            Number vrHours = (Number) row.get("vrHours");
            double hours = (srHours != null && srHours.doubleValue() > 0)
                    ? srHours.doubleValue()
                    : (vrHours != null ? vrHours.doubleValue() : 0);
            computedTotal += hours;

            // Determine date for monthly grouping (checkout time preferred, fallback to register time)
            Object checkoutObj = row.get("checkoutTime");
            Object registerObj = row.get("registerTime");
            java.util.Date date = null;
            if (checkoutObj instanceof java.util.Date) date = (java.util.Date) checkoutObj;
            else if (checkoutObj instanceof java.sql.Timestamp) date = (java.util.Date) checkoutObj;
            if (date == null && registerObj instanceof java.util.Date) date = (java.util.Date) registerObj;
            if (date == null && registerObj instanceof java.sql.Timestamp) date = (java.util.Date) registerObj;

            if (date != null) {
                String monthKey = monthFmt.format(date);
                monthlyMap.merge(monthKey, hours, Double::sum);

                cal.setTime(date);
                int year = cal.get(java.util.Calendar.YEAR);
                yearlyMap.merge(year, hours, Double::sum);
            }

            // Activity type
            String activityType = (String) row.get("activityType");
            if (activityType != null && !activityType.isEmpty()) {
                typeMap.merge(activityType, hours, Double::sum);
            }
        }

        // Use computed total as authoritative allTimeHours
        double allTimeHours;
        if (computedTotal > 0) {
            allTimeHours = Math.round(computedTotal * 10) / 10.0;
        } else {
            allTimeHours = ((Number) stats.get("totalEarnedHours")).doubleValue();
        }
        stats.put("allTimeHours", allTimeHours);

        // Self-heal: if DB totalEarnedHours differs from computed, update the DB
        int dbEarned = user.getTotalEarnedHours() != null ? user.getTotalEarnedHours() : 0;
        int computedEarned = (int) Math.round(computedTotal);
        if (computedTotal > 0 && Math.abs(dbEarned - computedEarned) > 0) {
            user.setTotalEarnedHours(computedEarned);
            userMapper.updateHours(user);
            stats.put("totalEarnedHours", computedEarned);
        }

        // Build monthly stats list (sorted)
        java.util.List<Map<String, Object>> monthlyStats = new java.util.ArrayList<>();
        java.util.List<String> sortedMonths = new java.util.ArrayList<>(monthlyMap.keySet());
        java.util.Collections.sort(sortedMonths);
        for (String m : sortedMonths) {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("month", m);
            entry.put("hours", Math.round(monthlyMap.get(m) * 10) / 10.0);
            monthlyStats.add(entry);
        }
        stats.put("monthlyStats", monthlyStats);

        // Build type distribution list
        java.util.List<Map<String, Object>> typeDistribution = new java.util.ArrayList<>();
        for (Map.Entry<String, Double> e : typeMap.entrySet()) {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("name", e.getKey());
            entry.put("value", Math.round(e.getValue() * 10) / 10.0);
            typeDistribution.add(entry);
        }
        stats.put("typeDistribution", typeDistribution);

        // Build yearly stats list
        java.util.List<Map<String, Object>> yearlyStats = new java.util.ArrayList<>();
        java.util.List<Integer> sortedYears = new java.util.ArrayList<>(yearlyMap.keySet());
        java.util.Collections.sort(sortedYears);
        for (Integer y : sortedYears) {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("year", y);
            entry.put("hours", Math.round(yearlyMap.get(y) * 10) / 10.0);
            yearlyStats.add(entry);
        }
        stats.put("yearlyStats", yearlyStats);

        return stats;
    }

    @Override
    public void cancelApplication(Integer userId, Integer recordId) throws Exception {
        VolunteerRecord record = volunteerRecordMapper.findById(recordId);
        if (record == null) throw new Exception("记录不存在");
        if (!record.getUserId().equals(userId)) throw new Exception("只能取消自己的报名");
        if (!"registered".equals(record.getStatus())) throw new Exception("只能取消待参加状态的活动");
        volunteerRecordMapper.cancel(recordId);
    }

    @Override
    public String getWechatAuthUrl() throws Exception {
        String redirectUri = java.net.URLEncoder.encode(wechatRedirectUri, "UTF-8");
        return String.format(
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=volunteer#wechat_redirect",
            wechatAppId, redirectUri
        );
    }

    @Override
    public String wechatLogin(String code) throws Exception {
        // 1. 通过code换取access_token和openid
        String accessTokenUrl = String.format(
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            wechatAppId, wechatAppSecret, code
        );

        String response = httpGet(accessTokenUrl);
        Map<String, Object> tokenData = com.alibaba.fastjson.JSON.parseObject(response);

        if (tokenData.containsKey("errcode")) {
            throw new Exception("微信登录失败: " + tokenData.get("errmsg"));
        }

        String openid = (String) tokenData.get("openid");
        String accessToken = (String) tokenData.get("access_token");

        // 2. 获取微信用户信息
        String userInfoUrl = String.format(
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
            accessToken, openid
        );
        String userInfoResp = httpGet(userInfoUrl);
        Map<String, Object> wxUserInfo = com.alibaba.fastjson.JSON.parseObject(userInfoResp);

        if (wxUserInfo.containsKey("errcode")) {
            throw new Exception("获取微信用户信息失败: " + wxUserInfo.get("errmsg"));
        }

        String nickname = (String) wxUserInfo.get("nickname");
        String headimgurl = (String) wxUserInfo.get("headimgurl");

        // 3. 查询是否已有微信绑定用户（通过username=openid）
        User user = userMapper.findByUsername("wx_" + openid);
        if (user == null) {
            // 4. 自动注册新用户
            user = new User();
            user.setUsername("wx_" + openid);
            user.setPassword(jwtUtils.encodePassword(openid)); // 用openid作为初始密码
            user.setRealName(nickname != null ? nickname : "微信用户");
            user.setPhone("");
            user.setVolunteerHours(0);
            user.setTotalEarnedHours(0);
            user.setTotalSpentHours(0);
            user.setStatus(1); // 微信用户自动通过审核
            user.setAvatar(headimgurl != null ? headimgurl : "/default_avatar.png");
            userMapper.insert(user);
            // 重新查询获取userId
            user = userMapper.findByUsername("wx_" + openid);
        }

        if (user.getStatus() != 1) {
            throw new Exception("账户未通过审核，请联系管理员");
        }

        return jwtUtils.generateToken(user.getUserId(), user.getUsername(), "user");
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (InputStream is = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()) {
            if (is == null) throw new Exception("HTTP请求失败: " + conn.getResponseCode());
            Scanner scanner = new Scanner(is, "UTF-8");
            String text = scanner.useDelimiter("\\A").next();
            scanner.close();
            return text;
        }
    }
}
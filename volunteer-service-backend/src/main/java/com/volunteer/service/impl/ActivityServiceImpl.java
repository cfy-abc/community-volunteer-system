package com.volunteer.service.impl;

import com.volunteer.dto.ActivityDTO;
import com.volunteer.entity.Activity;
import com.volunteer.entity.Organization;
import com.volunteer.entity.SignRecord;
import com.volunteer.entity.User;
import com.volunteer.entity.VolunteerRecord;
import com.volunteer.mapper.*;
import com.volunteer.service.ActivityService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VolunteerRecordMapper recordMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private SignRecordMapper signRecordMapper;

    @Override
    @Transactional
    public void publishActivity(Integer userId, ActivityDTO activityDTO) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new Exception("用户不存在或未通过审核");
        }

        // 检查实名认证
        if (user.getRealNameStatus() == null || user.getRealNameStatus() != 1) {
            throw new Exception("请先完成实名认证后再发布活动");
        }

        // 检查是否组织用户
        boolean isOrgUser = user.getIsOrgUser() != null && user.getIsOrgUser() == 1;
        if (activityDTO.getOrgId() != null && activityDTO.getOrgId() > 0) {
            Organization org = organizationMapper.findById(activityDTO.getOrgId());
            if (org == null || !org.getCreatorId().equals(userId)) {
                throw new Exception("您没有权限使用该组织发布活动");
            }
        }

        // 普通用户奖励时长不能超过可用余额
        if (!isOrgUser && activityDTO.getRewardHours() != null && activityDTO.getRewardHours() > user.getVolunteerHours()) {
            throw new Exception("奖励时长不能超过您的可用志愿时长（当前余额：" + user.getVolunteerHours() + "小时）");
        }

        Activity activity = new Activity();
        BeanUtils.copyProperties(activityDTO, activity);
        activity.setCreatorId(userId);
        activity.setCurrentParticipants(0);
        // 发布活动直接进入招募状态，无需审核
        activity.setStatus(1);
        activity.setPoster(activityDTO.getPoster() != null ? activityDTO.getPoster() : "/default_activity_poster.jpg");

        activityMapper.insert(activity);
    }

    @Override
    public Activity getActivityDetail(Integer activityId) throws Exception {
        Activity activity = activityMapper.findById(activityId);
        if (activity == null) {
            throw new Exception("活动不存在");
        }
        return activity;
    }

    @Override
    public List<Activity> getActivityList(Map<String, Object> params) {
        return activityMapper.findPage(params);
    }

    @Override
    public int getActivityCount(Map<String, Object> params) {
        return activityMapper.count(params);
    }

    @Override
    @Transactional
    public void registerForActivity(Integer userId, Integer activityId) throws Exception {
        // 检查用户状态
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new Exception("用户不存在或未通过审核");
        }
        // 检查实名认证状态
        if (user.getRealNameStatus() == null || user.getRealNameStatus() != 1) {
            throw new Exception("请先完成实名认证后再报名活动");
        }

        Activity activity = activityMapper.findById(activityId);
        if (activity == null) {
            throw new Exception("活动不存在");
        }
        // 发布方不能报名自己发布的活动
        if (activity.getCreatorId().equals(userId)) {
            throw new Exception("您是该活动的发布方，不能报名自己的活动");
        }

        if (activity.getStatus() != 1) {
            throw new Exception("活动不在招募状态");
        }

        VolunteerRecord existing = recordMapper.findByUserAndActivity(userId, activityId);
        if (existing != null) {
            throw new Exception("您已报名过该活动");
        }

        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new Exception("活动人数已满");
        }

        VolunteerRecord record = new VolunteerRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setHoursEarned(activity.getRewardHours());
        record.setStatus("registered");

        recordMapper.insert(record);

        activityMapper.increaseParticipants(activityId);

        createSignRecord(userId, activityId);
    }

    /**
     * 签到活动（旧版本，基于recordId）
     */
    @Transactional
    public void checkInActivity(Integer recordId, String location, String photo) throws Exception {
        // 检查记录是否存在
        VolunteerRecord record = recordMapper.findById(recordId);
        if (record == null) {
            throw new Exception("记录不存在");
        }

        // 检查活动状态
        Activity activity = activityMapper.findById(record.getActivityId());
        if (activity == null || activity.getStatus() != 2) { // 2表示进行中
            throw new Exception("活动不在进行中状态");
        }

        // 检查是否已签到
        if ("attended".equals(record.getStatus()) || "completed".equals(record.getStatus())) {
            throw new Exception("您已签到");
        }

        // 签到
        recordMapper.checkIn(recordId, "attended", location, photo);
    }

    /**
     * 完成活动（旧版本，基于recordId）
     */
    @Transactional
    public void completeActivity(Integer recordId, Integer hoursEarned) throws Exception {
        // 检查记录是否存在
        VolunteerRecord record = recordMapper.findById(recordId);
        if (record == null) {
            throw new Exception("记录不存在");
        }

        // 检查是否已签到
        if (!"attended".equals(record.getStatus())) {
            throw new Exception("请先签到");
        }

        // 完成活动
        recordMapper.complete(recordId, hoursEarned);

        // 更新用户时长
        User user = userMapper.findById(record.getUserId());
        if (user != null) {
            user.setVolunteerHours(user.getVolunteerHours() + hoursEarned);
            user.setTotalEarnedHours(user.getTotalEarnedHours() + hoursEarned);
            userMapper.updateHours(user);
        }
    }

    /**
     * 生成签到二维码token（包含活动ID+用户ID+时间戳+校验码）
     */
    private String generateQrToken(Integer activityId, Integer userId) {
        String raw = activityId + ":" + userId + ":" + System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }

    /**
     * 创建签到记录（报名成功后调用，生成二维码）
     */
    private void createSignRecord(Integer userId, Integer activityId) {
        SignRecord record = signRecordMapper.findByUserAndActivity(userId, activityId);
        if (record == null) {
            record = new SignRecord();
            record.setUserId(userId);
            record.setActivityId(activityId);
            record.setQrToken(generateQrToken(activityId, userId));
            signRecordMapper.insert(record);
        }
    }

    @Override
    @Transactional
    public void checkIn(Integer userId, Integer activityId, String location, String photo, String qrToken) throws Exception {
        // 查找签到记录
        SignRecord record = signRecordMapper.findByUserAndActivity(userId, activityId);
        if (record == null) {
            throw new Exception("未找到签到记录，请先报名");
        }
        if (record.getStatus() != 0) {
            throw new Exception("您已经签到过或已完成签退");
        }

        // 校验二维码（如果提供了二维码）
        if (qrToken != null && !qrToken.equals(record.getQrToken())) {
            throw new Exception("二维码无效");
        }

        // 校验位置距离（如果活动设置了坐标和范围）
        Activity activity = activityMapper.findById(activityId);
        if (activity != null && activity.getLatitude() != null && activity.getLongitude() != null && location != null) {
            try {
                String[] locParts = location.split(",");
                double distance = calculateDistance(
                    Double.parseDouble(locParts[0]),
                    Double.parseDouble(locParts[1]),
                    activity.getLatitude(),
                    activity.getLongitude()
                );
                // 默认签到范围为100米，如果Activity有signRange字段可以使用
                if (distance > 100) {
                    throw new Exception("您不在活动签到范围内（距离：" + String.format("%.2f", distance) + "米）");
                }
            } catch (NumberFormatException e) {
                throw new Exception("位置格式错误");
            }
        }

        // 更新签到信息
        record.setCheckinTime(new Date());
        record.setCheckinLocation(location);
        record.setCheckinPhoto(photo);
        record.setStatus(1);
        signRecordMapper.updateCheckin(record);
    }

    @Override
    @Transactional
    public void checkOut(Integer userId, Integer activityId, String location) throws Exception {
        // 查找签到记录
        SignRecord record = signRecordMapper.findByUserAndActivity(userId, activityId);
        if (record == null) {
            throw new Exception("未找到签到记录");
        }
        if (record.getStatus() != 1) {
            throw new Exception("尚未签到或已经签退");
        }

        Date now = new Date();
        // 计算服务时长（小时）：签退时间 - 签到时间
        double hoursEarned = 0;
        if (record.getCheckinTime() != null) {
            long diffMs = now.getTime() - record.getCheckinTime().getTime();
            hoursEarned = Math.round(diffMs / (1000.0 * 60 * 60) * 10.0) / 10.0; // 精确到0.1小时
            if (hoursEarned < 0) hoursEarned = 0;
        }

        // 更新签退信息（含服务时长计算，待审批状态）
        record.setCheckoutTime(now);
        record.setCheckoutLocation(location);
        record.setStatus(2);
        record.setHoursEarned(hoursEarned);
        record.setApprovalStatus(0); // 待审批
        signRecordMapper.updateCheckout(record);
    }

    @Override
    public Map<String, Object> getSignStatus(Integer userId, Integer activityId) throws Exception {
        SignRecord record = signRecordMapper.findByUserAndActivity(userId, activityId);
        Map<String, Object> map = new HashMap<>();

        if (record == null) {
            map.put("status", "not_start");
            map.put("checkinTime", null);
            map.put("checkoutTime", null);
            map.put("checkinLocation", null);
            map.put("checkoutLocation", null);
        } else {
            String statusStr;
            switch (record.getStatus()) {
                case 0:
                    statusStr = "pending";
                    break;
                case 1:
                    statusStr = "checked_in";
                    break;
                case 2:
                    statusStr = "checked_out";
                    break;
                default:
                    statusStr = "unknown";
            }
            map.put("status", statusStr);
            map.put("checkinTime", record.getCheckinTime());
            map.put("checkoutTime", record.getCheckoutTime());
            map.put("checkinLocation", record.getCheckinLocation());
            map.put("checkoutLocation", record.getCheckoutLocation());
            map.put("userId", record.getUserId());
            map.put("qrToken", record.getQrToken());
        }

        return map;
    }

    @Override
    @Transactional
    public void organizerCheckIn(Integer organizerUserId, Integer volunteerUserId, Integer activityId, String qrToken) throws Exception {
        // 验证组织者是活动发布方
        Activity activity = activityMapper.findById(activityId);
        if (activity == null) throw new Exception("活动不存在");
        if (!activity.getCreatorId().equals(organizerUserId)) {
            // 检查是否是组织管理员
            if (activity.getOrgId() != null && activity.getOrgId() > 0) {
                Organization org = organizationMapper.findById(activity.getOrgId());
                if (org == null || !org.getCreatorId().equals(organizerUserId)) {
                    throw new Exception("只有活动发布方或组织管理员可以进行反扫码签到");
                }
            } else {
                throw new Exception("只有活动发布方可以进行反扫码签到");
            }
        }

        // 验证志愿者的二维码
        SignRecord record = signRecordMapper.findByUserAndActivity(volunteerUserId, activityId);
        if (record == null) throw new Exception("未找到该志愿者的签到记录");
        if (!qrToken.equals(record.getQrToken())) throw new Exception("二维码无效或已过期");
        if (record.getStatus() != 0) throw new Exception("该志愿者已签到或已完成签退");

        // 执行签到（不需要位置，由组织者确认）
        record.setCheckinTime(new Date());
        record.setCheckinLocation("组织者扫码签到");
        record.setStatus(1);
        signRecordMapper.updateCheckin(record);
    }

    /**
     * 计算两点之间的距离（单位：米）
     * 使用Haversine公式
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

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
        // Set form data
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

    @Override
    public List<Map<String, Object>> getApplicants(Integer activityId) throws Exception {
        Activity activity = activityMapper.findById(activityId);
        if (activity == null) throw new Exception("活动不存在");
        return recordMapper.findApplicantsByActivity(activityId);
    }

    @Override
    public byte[] exportApplicants(Integer activityId) throws Exception {
        List<Map<String, Object>> applicants = getApplicants(activityId);
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("报名名单");
            org.apache.poi.xssf.usermodel.XSSFRow header = sheet.createRow(0);
            String[] headers = {"姓名", "手机号", "邮箱", "紧急联系人", "紧急联系电话", "备注", "报名时间"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Map<String, Object> a : applicants) {
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.get("applicantName") != null ? a.get("applicantName").toString() : "");
                row.createCell(1).setCellValue(a.get("applicantPhone") != null ? a.get("applicantPhone").toString() : "");
                row.createCell(2).setCellValue(a.get("applicantEmail") != null ? a.get("applicantEmail").toString() : "");
                row.createCell(3).setCellValue(a.get("emergencyContact") != null ? a.get("emergencyContact").toString() : "");
                row.createCell(4).setCellValue(a.get("emergencyPhone") != null ? a.get("emergencyPhone").toString() : "");
                row.createCell(5).setCellValue(a.get("remarks") != null ? a.get("remarks").toString() : "");
                row.createCell(6).setCellValue(a.get("registerTime") != null ? a.get("registerTime").toString() : "");
            }
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
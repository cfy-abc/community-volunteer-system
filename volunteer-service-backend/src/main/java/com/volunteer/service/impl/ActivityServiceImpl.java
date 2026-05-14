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

        if (activityDTO.getOrgId() != null && activityDTO.getOrgId() > 0) {
            Organization org = organizationMapper.findById(activityDTO.getOrgId());
            if (org == null || !org.getCreatorId().equals(userId)) {
                throw new Exception("您没有权限使用该组织发布活动");
            }
        }

        Activity activity = new Activity();
        BeanUtils.copyProperties(activityDTO, activity);
        activity.setCreatorId(userId);
        activity.setCurrentParticipants(0);
        activity.setStatus(4);
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
        Activity activity = activityMapper.findById(activityId);
        if (activity == null) {
            throw new Exception("活动不存在");
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

        // 更新签退信息
        record.setCheckoutTime(new Date());
        record.setCheckoutLocation(location);
        record.setStatus(2);
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
        }

        return map;
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
}
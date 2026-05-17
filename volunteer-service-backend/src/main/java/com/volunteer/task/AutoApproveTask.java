package com.volunteer.task;

import com.volunteer.entity.SignRecord;
import com.volunteer.entity.User;
import com.volunteer.mapper.SignRecordMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class AutoApproveTask {

    @Autowired
    private SignRecordMapper signRecordMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 每5分钟检查一次，自动审核48小时未处理的签退记录
     */
    @Scheduled(fixedRate = 300000)
    public void autoApproveCheckouts() {
        try {
            // 查询签退48小时后仍未审核的记录（已在SQL层过滤）
            List<SignRecord> pendingRecords = signRecordMapper.findPendingApprovals();

            for (SignRecord record : pendingRecords) {
                // 使用签退时计算的服务时长（若为空则计算）
                double hours = record.getHoursEarned() != null && record.getHoursEarned() > 0
                        ? record.getHoursEarned()
                        : calculateHours(record.getCheckinTime(), record.getCheckoutTime());

                // 自动通过审核
                signRecordMapper.approveRecord(record.getId(), hours);

                // 增加用户志愿时长
                User user = userMapper.findById(record.getUserId());
                if (user != null) {
                    int earnedHours = (int) Math.round(hours);
                    if (earnedHours < 1) earnedHours = 1; // 最少1小时
                    user.setVolunteerHours(user.getVolunteerHours() + earnedHours);
                    user.setTotalEarnedHours(user.getTotalEarnedHours() + earnedHours);
                    userMapper.updateHours(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateHours(Date checkinTime, Date checkoutTime) {
        if (checkinTime == null || checkoutTime == null) return 0.5;
        long diffMs = checkoutTime.getTime() - checkinTime.getTime();
        double hours = Math.round(diffMs / (1000.0 * 60 * 60) * 10.0) / 10.0;
        return Math.max(hours, 0.1);
    }
}

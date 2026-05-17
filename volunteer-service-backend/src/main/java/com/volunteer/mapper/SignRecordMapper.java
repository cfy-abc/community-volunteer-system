package com.volunteer.mapper;

import com.volunteer.entity.SignRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SignRecordMapper {

    @Insert("INSERT INTO sign_record(user_id, activity_id, status, qr_token) VALUES(#{userId}, #{activityId}, 0, #{qrToken})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SignRecord record);

    @Select("SELECT * FROM sign_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    SignRecord findByUserAndActivity(@Param("userId") Integer userId, @Param("activityId") Integer activityId);

    @Update("UPDATE sign_record SET checkin_time = #{checkinTime}, checkin_location = #{checkinLocation}, " +
            "checkin_photo = #{checkinPhoto}, status = 1 WHERE id = #{id}")
    int updateCheckin(SignRecord record);

    @Update("UPDATE sign_record SET checkout_time = #{checkoutTime}, checkout_location = #{checkoutLocation}, " +
            "status = 2, hours_earned = #{hoursEarned}, approval_status = 0 WHERE id = #{id}")
    int updateCheckout(SignRecord record);

    @Select("SELECT * FROM sign_record WHERE status = 2 AND approval_status = 0 " +
            "AND checkout_time IS NOT NULL AND checkout_time <= DATE_SUB(NOW(), INTERVAL 48 HOUR)")
    List<SignRecord> findPendingApprovals();

    @Update("UPDATE sign_record SET approval_status = 1, approval_time = NOW(), hours_earned = #{hours} WHERE id = #{id}")
    int approveRecord(@Param("id") Integer id, @Param("hours") Double hours);

    @Select("SELECT sr.id, sr.user_id AS userId, sr.activity_id AS activityId, " +
            "sr.hours_earned AS hoursEarned, sr.checkin_time AS checkinTime, " +
            "sr.checkout_time AS checkoutTime, sr.approval_status AS approvalStatus, " +
            "sr.checkin_location AS checkinLocation, sr.checkout_location AS checkoutLocation, " +
            "u.real_name AS realName " +
            "FROM sign_record sr " +
            "JOIN user u ON sr.user_id = u.user_id " +
            "WHERE sr.activity_id = #{activityId} AND sr.status = 2 AND sr.approval_status = 0 " +
            "ORDER BY sr.checkout_time DESC")
    List<Map<String, Object>> findPendingByActivity(@Param("activityId") Integer activityId);

    @Select("SELECT * FROM sign_record WHERE id = #{id}")
    SignRecord findById(@Param("id") Integer id);

    @Update("UPDATE sign_record SET approval_status = #{approvalStatus}, approval_time = NOW() WHERE id = #{id}")
    int updateApprovalStatus(SignRecord record);
}
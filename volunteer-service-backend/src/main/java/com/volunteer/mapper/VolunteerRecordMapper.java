package com.volunteer.mapper;

import com.volunteer.entity.VolunteerRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface VolunteerRecordMapper {

    @Insert("INSERT INTO volunteer_record(user_id, activity_id, hours_earned, status, register_time, applicant_name, applicant_phone, applicant_email, emergency_contact, emergency_phone, remarks) " +
            "VALUES(#{userId}, #{activityId}, #{hoursEarned}, #{status}, NOW(), #{applicantName}, #{applicantPhone}, #{applicantEmail}, #{emergencyContact}, #{emergencyPhone}, #{remarks})")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(VolunteerRecord record);

    @Select("SELECT * FROM volunteer_record WHERE record_id = #{recordId}")
    VolunteerRecord findById(Integer recordId);

    @Select("SELECT * FROM volunteer_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    VolunteerRecord findByUserAndActivity(@Param("userId") Integer userId, @Param("activityId") Integer activityId);

    @Select("<script>" +
            "SELECT r.record_id AS recordId, r.user_id AS userId, r.activity_id AS activityId, " +
            "r.hours_earned AS hoursEarned, r.status AS status, r.register_time AS registerTime, " +
            "r.check_in_time AS checkInTime, r.check_out_time AS checkOutTime, " +
            "a.title AS activityTitle, a.start_time AS startTime, a.end_time AS endTime, " +
            "a.location AS location, a.type AS type, " +
            "COALESCE(s.status, 0) AS signStatus, s.qr_token AS qrToken, " +
            "s.approval_status AS approvalStatus, s.hours_earned AS signHoursEarned " +
            "FROM volunteer_record r " +
            "JOIN activity a ON r.activity_id = a.activity_id " +
            "LEFT JOIN sign_record s ON r.user_id = s.user_id AND r.activity_id = s.activity_id " +
            "WHERE r.user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND r.status = #{status}" +
            "</if>" +
            "ORDER BY r.register_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> findPage(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) FROM volunteer_record WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status}" +
            "</if>" +
            "</script>")
    int countByUser(Map<String, Object> params);

    @Update("UPDATE volunteer_record SET status = 'cancelled' WHERE record_id = #{recordId}")
    int cancel(Integer recordId);

    @Select("SELECT vr.applicant_name AS applicantName, vr.applicant_phone AS applicantPhone, " +
            "vr.applicant_email AS applicantEmail, vr.emergency_contact AS emergencyContact, " +
            "vr.emergency_phone AS emergencyPhone, vr.remarks AS remarks, " +
            "vr.register_time AS registerTime, u.real_name AS userName " +
            "FROM volunteer_record vr " +
            "JOIN user u ON vr.user_id = u.user_id " +
            "WHERE vr.activity_id = #{activityId} AND vr.status != 'cancelled' " +
            "ORDER BY vr.register_time DESC")
    List<Map<String, Object>> findApplicantsByActivity(@Param("activityId") Integer activityId);
}
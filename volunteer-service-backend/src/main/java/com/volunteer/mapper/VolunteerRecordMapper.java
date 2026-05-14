package com.volunteer.mapper;

import com.volunteer.entity.VolunteerRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface VolunteerRecordMapper {

    @Insert("INSERT INTO volunteer_record(user_id, activity_id, hours_earned, status, register_time) " +
            "VALUES(#{userId}, #{activityId}, #{hoursEarned}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(VolunteerRecord record);

    @Select("SELECT * FROM volunteer_record WHERE record_id = #{recordId}")
    VolunteerRecord findById(Integer recordId);

    @Select("SELECT * FROM volunteer_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    VolunteerRecord findByUserAndActivity(@Param("userId") Integer userId, @Param("activityId") Integer activityId);

    @Update("UPDATE volunteer_record SET status = #{status}, check_in_time = NOW(), check_in_location = #{location}, check_in_photo = #{photo} WHERE record_id = #{recordId}")
    int checkIn(@Param("recordId") Integer recordId, @Param("status") String status, @Param("location") String location, @Param("photo") String photo);

    @Update("UPDATE volunteer_record SET status = 'completed', check_out_time = NOW(), hours_earned = #{hoursEarned} WHERE record_id = #{recordId}")
    int complete(@Param("recordId") Integer recordId, @Param("hoursEarned") Integer hoursEarned);

    @Select("<script>" +
            "SELECT r.*, a.title as activity_title, a.start_time, a.end_time " +
            "FROM volunteer_record r " +
            "JOIN activity a ON r.activity_id = a.activity_id " +
            "WHERE r.user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND r.status = #{status}" +
            "</if>" +
            "ORDER BY r.register_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<VolunteerRecord> findPage(Map<String, Object> params);

    @Select("SELECT COUNT(*) FROM volunteer_record WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status}" +
            "</if>")
    int countByUser(Map<String, Object> params);
}
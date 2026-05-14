package com.volunteer.mapper;

import com.volunteer.entity.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    @Insert("INSERT INTO activity(title, description, start_time, end_time, location, longitude, latitude, max_participants, current_participants, reward_hours, creator_id, org_id, status, create_time, poster, contact_phone, type, tags, conditions, feedbacks) " +
            "VALUES(#{title}, #{description}, #{startTime}, #{endTime}, #{location}, #{longitude}, #{latitude}, #{maxParticipants}, 0, #{rewardHours}, #{creatorId}, #{orgId}, #{status}, NOW(), #{poster}, #{contactPhone}, #{type}, #{tags}, #{conditions}, #{feedbacks})")
    @Options(useGeneratedKeys = true, keyProperty = "activityId")
    int insert(Activity activity);

    @Select("SELECT * FROM activity WHERE activity_id = #{activityId}")
    Activity findById(Integer activityId);

    @Select("<script>" +
            "SELECT a.*, u.real_name as creator_name, o.org_name " +
            "FROM activity a " +
            "LEFT JOIN user u ON a.creator_id = u.user_id " +
            "LEFT JOIN organization o ON a.org_id = o.org_id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.location LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='status != null'>" +
            "AND a.status = #{status}" +
            "</if>" +
            "<if test='orgId != null and orgId > 0'>" +
            "AND a.org_id = #{orgId}" +
            "</if>" +
            "ORDER BY a.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Activity> findPage(Map<String, Object> params);

    @Select("SELECT COUNT(*) FROM activity WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR location LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status}" +
            "</if>" +
            "<if test='orgId != null and orgId > 0'>" +
            "AND org_id = #{orgId}" +
            "</if>")
    int count(Map<String, Object> params);

    @Update("UPDATE activity SET current_participants = current_participants + 1 WHERE activity_id = #{activityId} AND current_participants < max_participants")
    int increaseParticipants(Integer activityId);

    @Update("UPDATE activity SET status = #{status} WHERE activity_id = #{activityId}")
    int updateStatus(@Param("activityId") Integer activityId, @Param("status") Integer status);
}

package com.volunteer.mapper;

import com.volunteer.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO user(username, password, real_name, phone, volunteer_hours, total_earned_hours, total_spent_hours, status, register_time, avatar) " +
            "VALUES(#{username}, #{password}, #{realName}, #{phone}, #{volunteerHours}, #{totalEarnedHours}, #{totalSpentHours}, #{status}, NOW(), #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    User findById(Integer userId);

    @Update("UPDATE user SET volunteer_hours = #{volunteerHours}, total_earned_hours = #{totalEarnedHours}, total_spent_hours = #{totalSpentHours} WHERE user_id = #{userId}")
    int updateHours(User user);

    @Update("UPDATE user SET password = #{password} WHERE user_id = #{userId}")
    int updatePassword(User user);

    @Update("UPDATE user SET username = #{username}, phone = #{phone}, avatar = #{avatar} WHERE user_id = #{userId}")
    int updateInfo(User user);

    @Select("SELECT EXISTS(SELECT 1 FROM organization WHERE creator_id = #{userId})")
    boolean hasOrganization(Integer userId);

    @Update("UPDATE user SET status = #{status} WHERE user_id = #{userId}")
    int updateStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    // @formatter:off
    @Select("<script>" +
            "SELECT * FROM user WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "ORDER BY register_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    // @formatter:on
    List<User> findAll(@Param("offset") int offset, @Param("limit") int limit, @Param("status") Integer status);

    // @formatter:off
    @Select("<script>" +
            "SELECT COUNT(*) FROM user WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</script>")
    // @formatter:on
    int countAll(@Param("status") Integer status);

    @Update("UPDATE user SET real_name = #{realName}, id_card = #{idCard}, real_name_status = #{status} WHERE user_id = #{userId}")
    int updateRealNameStatus(@Param("userId") Integer userId, @Param("realName") String realName,
                             @Param("idCard") String idCard, @Param("status") Integer status);

    @Insert("INSERT INTO org_upgrade_application(user_id, position, department, reason, status, apply_time) " +
            "VALUES(#{userId}, #{position}, #{department}, #{reason}, 0, NOW())")
    int insertOrgUpgradeApplication(@Param("userId") Integer userId, @Param("position") String position,
                                    @Param("department") String department, @Param("reason") String reason);

    @Select("SELECT a.`type` as name, " +
            "COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE vr.hours_earned END), 0) as value " +
            "FROM volunteer_record vr " +
            "JOIN activity a ON vr.activity_id = a.activity_id " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.user_id = #{userId} AND vr.status != 'cancelled' " +
            "GROUP BY a.`type`")
    List<Map<String, Object>> getActivityTypeDistribution(@Param("userId") Integer userId);

    @Select("SELECT DATE_FORMAT(COALESCE(sr.checkout_time, vr.register_time), '%Y-%m') as month, " +
            "COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE 0 END), 0) as hours " +
            "FROM volunteer_record vr " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.user_id = #{userId} AND vr.status != 'cancelled' " +
            "GROUP BY DATE_FORMAT(COALESCE(sr.checkout_time, vr.register_time), '%Y-%m') ORDER BY month ASC")
    List<Map<String, Object>> getMonthlyStats(@Param("userId") Integer userId);

    @Select("SELECT YEAR(COALESCE(sr.checkout_time, vr.register_time)) as year, " +
            "COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE 0 END), 0) as hours " +
            "FROM volunteer_record vr " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.user_id = #{userId} AND vr.status != 'cancelled' " +
            "AND COALESCE(sr.checkout_time, vr.register_time) >= DATE_SUB(NOW(), INTERVAL 3 YEAR) " +
            "GROUP BY YEAR(COALESCE(sr.checkout_time, vr.register_time)) ORDER BY year ASC")
    List<Map<String, Object>> getYearlyStats(@Param("userId") Integer userId);

    // Load raw stats data for a user — one query, all stats computed in Java for consistency
    @Select("SELECT vr.record_id, vr.activity_id, vr.hours_earned AS vrHours, vr.status, vr.register_time AS registerTime, " +
            "sr.id AS srId, sr.hours_earned AS srHours, sr.checkin_time AS checkinTime, sr.checkout_time AS checkoutTime, sr.approval_status AS approvalStatus, " +
            "a.type AS activityType, a.title AS activityTitle " +
            "FROM volunteer_record vr " +
            "JOIN activity a ON vr.activity_id = a.activity_id " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.user_id = #{userId} AND vr.status != 'cancelled' " +
            "ORDER BY vr.register_time DESC")
    List<Map<String, Object>> getUserStatsRaw(@Param("userId") Integer userId);

    // ===== 组织升级申请管理 =====
    @Select("SELECT a.*, u.username, u.real_name FROM org_upgrade_application a " +
            "JOIN user u ON a.user_id = u.user_id ORDER BY a.apply_time DESC LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findOrgUpgradeApplications(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM org_upgrade_application")
    int countOrgUpgradeApplications();

    @Update("UPDATE org_upgrade_application SET status = #{status}, audit_time = NOW() WHERE application_id = #{id}")
    int updateOrgUpgradeStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("UPDATE user SET is_org_user = 1, org_position = #{position}, org_department = #{department} WHERE user_id = #{userId}")
    int upgradeToOrgUser(@Param("userId") Integer userId, @Param("position") String position,
                         @Param("department") String department);

    // ===== 实名认证管理 =====
    @Select("SELECT user_id, username, real_name, phone, id_card, real_name_status, register_time " +
            "FROM user WHERE real_name_status = 2 ORDER BY register_time DESC LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> findPendingRealNameVerifications(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user WHERE real_name_status = 2")
    int countPendingRealNameVerifications();

    @Update("UPDATE user SET real_name_status = 1 WHERE user_id = #{userId}")
    int approveRealName(@Param("userId") Integer userId);
}

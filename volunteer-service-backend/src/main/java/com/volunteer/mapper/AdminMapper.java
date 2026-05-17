package com.volunteer.mapper;

import com.volunteer.entity.Admin;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin findByUsername(@Param("username") String username);

    @Select("SELECT * FROM admin WHERE admin_id = #{adminId}")
    Admin findById(@Param("adminId") Integer adminId);

    @Update("UPDATE admin SET last_login_time = NOW() WHERE admin_id = #{adminId}")
    void updateLoginTime(@Param("adminId") Integer adminId);

    @Insert("INSERT INTO admin(username, password, real_name, phone, role, status, create_time) " +
            "VALUES(#{username}, #{password}, #{realName}, #{phone}, #{role}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "adminId")
    int insert(Admin admin);

    @Update("UPDATE admin SET password = #{password} WHERE admin_id = #{adminId}")
    int updatePassword(@Param("adminId") Integer adminId, @Param("password") String password);

    @Select("<script>" +
            "SELECT * FROM user WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "ORDER BY register_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<com.volunteer.entity.User> findAllUsers(@Param("offset") int offset, @Param("limit") int limit, @Param("status") Integer status);

    @Select("<script>" +
            "SELECT COUNT(*) FROM user WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</script>")
    int countUsers(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM user")
    int totalUsers();

    @Select("SELECT COUNT(*) FROM activity")
    int totalActivities();

    @Select("SELECT COUNT(*) FROM organization")
    int totalOrganizations();

    @Select("SELECT COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE vr.hours_earned END), 0) " +
            "FROM volunteer_record vr " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.status != 'cancelled'")
    int totalVolunteerHours();

    @Select("SELECT * FROM user ORDER BY volunteer_hours DESC LIMIT #{limit}")
    List<com.volunteer.entity.User> topVolunteers(@Param("limit") int limit);

    @Select("SELECT o.org_name as name, COALESCE(SUM(gm.contribution_hours), 0) as total FROM organization o " +
            "LEFT JOIN group_member gm ON o.org_id = gm.org_id " +
            "WHERE o.status = 1 GROUP BY o.org_id ORDER BY total DESC LIMIT #{limit}")
    List<java.util.Map<String, Object>> topOrganizations(@Param("limit") int limit);

    @Select("SELECT a.`type` as name, " +
            "COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE vr.hours_earned END), 0) as value " +
            "FROM volunteer_record vr " +
            "JOIN activity a ON vr.activity_id = a.activity_id " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.status != 'cancelled' " +
            "GROUP BY a.`type` ORDER BY value DESC")
    List<java.util.Map<String, Object>> activityTypeDistribution();

    @Select("SELECT DATE_FORMAT(COALESCE(sr.checkout_time, vr.register_time), '%Y-%m') as month, " +
            "COALESCE(SUM(CASE WHEN sr.id IS NOT NULL AND sr.hours_earned > 0 THEN sr.hours_earned ELSE 0 END), 0) as hours " +
            "FROM volunteer_record vr " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.status != 'cancelled' " +
            "GROUP BY DATE_FORMAT(COALESCE(sr.checkout_time, vr.register_time), '%Y-%m') ORDER BY month ASC")
    List<java.util.Map<String, Object>> monthlyVolunteerHours();

    @Select("<script>" +
            "SELECT sr.id, sr.user_id AS userId, sr.activity_id AS activityId, " +
            "sr.hours_earned AS hoursEarned, sr.checkin_time AS checkinTime, " +
            "sr.checkout_time AS checkoutTime, sr.approval_status AS approvalStatus, " +
            "u.real_name AS realName, a.title AS activityTitle " +
            "FROM sign_record sr " +
            "JOIN user u ON sr.user_id = u.user_id " +
            "JOIN activity a ON sr.activity_id = a.activity_id " +
            "WHERE sr.status = 2 AND sr.approval_status = 0 " +
            "ORDER BY sr.checkout_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> findPendingSignApprovals(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sign_record WHERE status = 2 AND approval_status = 0")
    int countPendingSignApprovals();

    @Update("UPDATE sign_record SET approval_status = 2, approval_time = NOW() WHERE id = #{id}")
    int rejectSignRecord(@Param("id") Integer id);

    // Load raw stats data for dashboard — one query, compute in Java for consistency
    @Select("SELECT vr.user_id AS userId, vr.hours_earned AS vrHours, " +
            "sr.id AS srId, sr.hours_earned AS srHours, sr.checkin_time AS checkinTime, sr.checkout_time AS checkoutTime, " +
            "a.type AS activityType " +
            "FROM volunteer_record vr " +
            "JOIN activity a ON vr.activity_id = a.activity_id " +
            "LEFT JOIN sign_record sr ON vr.user_id = sr.user_id AND vr.activity_id = sr.activity_id " +
            "WHERE vr.status != 'cancelled'")
    List<Map<String, Object>> getDashboardStatsRaw();
}

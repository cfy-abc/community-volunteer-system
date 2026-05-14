package com.volunteer.mapper;

import com.volunteer.entity.Admin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin findByUsername(@Param("username") String username);

    @Select("SELECT * FROM admin WHERE admin_id = #{adminId}")
    Admin findById(@Param("adminId") Integer adminId);

    @Update("UPDATE admin SET last_login_time = NOW() WHERE admin_id = #{adminId}")
    void updateLoginTime(@Param("adminId") Integer adminId);

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

    @Select("SELECT COALESCE(SUM(volunteer_hours), 0) FROM user")
    int totalVolunteerHours();

    @Select("SELECT * FROM user ORDER BY volunteer_hours DESC LIMIT #{limit}")
    List<com.volunteer.entity.User> topVolunteers(@Param("limit") int limit);

    @Select("SELECT o.org_name as name, COALESCE(SUM(gm.contribution_hours), 0) as total FROM organization o " +
            "LEFT JOIN group_member gm ON o.org_id = gm.org_id " +
            "WHERE o.status = 1 GROUP BY o.org_id ORDER BY total DESC LIMIT #{limit}")
    List<java.util.Map<String, Object>> topOrganizations(@Param("limit") int limit);

    @Select("SELECT a.type as name, COUNT(*) as value FROM activity a GROUP BY a.type ORDER BY value DESC")
    List<java.util.Map<String, Object>> activityTypeDistribution();
}

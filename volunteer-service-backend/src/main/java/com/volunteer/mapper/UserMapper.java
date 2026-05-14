package com.volunteer.mapper;

import com.volunteer.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
}

package com.volunteer.mapper;

import com.volunteer.entity.GroupMember;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GroupMemberMapper {

    @Insert("INSERT INTO group_member(org_id, user_id, role) VALUES(#{orgId}, #{userId}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupMember member);

    @Select("SELECT gm.*, u.real_name as userName, u.avatar FROM group_member gm " +
            "JOIN user u ON gm.user_id = u.user_id WHERE gm.org_id = #{orgId} ORDER BY gm.role DESC, gm.join_time ASC")
    List<GroupMember> findByOrgId(@Param("orgId") Integer orgId);

    @Select("SELECT * FROM group_member WHERE org_id = #{orgId} AND user_id = #{userId}")
    GroupMember findByOrgAndUser(@Param("orgId") Integer orgId, @Param("userId") Integer userId);

    @Delete("DELETE FROM group_member WHERE org_id = #{orgId} AND user_id = #{userId}")
    int delete(@Param("orgId") Integer orgId, @Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM group_member WHERE org_id = #{orgId}")
    int countByOrgId(@Param("orgId") Integer orgId);
}

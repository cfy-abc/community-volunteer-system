package com.volunteer.mapper;

import com.volunteer.entity.ActivityComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityCommentMapper {

    @Insert("INSERT INTO activity_comment(activity_id, user_id, parent_id, content, user_tag, create_time) " +
            "VALUES(#{activityId}, #{userId}, #{parentId}, #{content}, #{userTag}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    int insert(ActivityComment comment);

    @Select("SELECT c.*, u.username, u.real_name, u.avatar " +
            "FROM activity_comment c " +
            "JOIN user u ON c.user_id = u.user_id " +
            "WHERE c.activity_id = #{activityId} AND c.parent_id IS NULL " +
            "ORDER BY c.create_time DESC")
    List<ActivityComment> findByActivityId(Integer activityId);

    @Select("SELECT c.*, u.username, u.real_name, u.avatar " +
            "FROM activity_comment c " +
            "JOIN user u ON c.user_id = u.user_id " +
            "WHERE c.parent_id = #{parentId} " +
            "ORDER BY c.create_time ASC")
    List<ActivityComment> findReplies(Integer parentId);

    @Select("SELECT * FROM activity_comment WHERE comment_id = #{commentId}")
    ActivityComment findById(Integer commentId);

    @Delete("DELETE FROM activity_comment WHERE comment_id = #{commentId}")
    int deleteById(Integer commentId);

    @Select("SELECT COUNT(*) FROM activity_comment WHERE activity_id = #{activityId}")
    int countByActivityId(Integer activityId);

    // Clean up legacy invalid userTag values (e.g. 'admin') — set to 'unparticipated'
    @Update("UPDATE activity_comment SET user_tag = 'unparticipated' WHERE user_tag NOT IN ('organizer', 'participated', 'unparticipated')")
    int fixInvalidUserTags();
}

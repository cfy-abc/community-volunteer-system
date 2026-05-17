package com.volunteer.service;

import com.volunteer.entity.ActivityComment;

import java.util.List;
import java.util.Map;

public interface ActivityCommentService {
    ActivityComment addComment(Integer userId, Integer activityId, String content, String userTag);
    ActivityComment replyToComment(Integer userId, Integer commentId, String content, String userTag);
    List<ActivityComment> getComments(Integer activityId);
    List<ActivityComment> getReplies(Integer parentId);
    void deleteComment(Integer userId, Integer commentId);
    int getCommentCount(Integer activityId);
}

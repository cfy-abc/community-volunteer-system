package com.volunteer.service.impl;

import com.volunteer.entity.*;
import com.volunteer.mapper.*;
import com.volunteer.service.ActivityCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ActivityCommentServiceImpl implements ActivityCommentService {

    @Autowired
    private ActivityCommentMapper commentMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VolunteerRecordMapper volunteerRecordMapper;

    @Autowired
    private AdminMapper adminMapper;

    /**
     * Compute the authoritative user tag based on actual data, ignoring client input.
     */
    private String computeUserTag(Integer userId, Integer activityId) {
        // Check if user is the activity creator
        Activity activity = activityMapper.findById(activityId);
        if (activity != null && activity.getCreatorId().equals(userId)) {
            return "organizer";
        }
        // Check if user has participated (has a non-cancelled volunteer_record)
        VolunteerRecord vr = volunteerRecordMapper.findByUserAndActivity(userId, activityId);
        if (vr != null && !"cancelled".equals(vr.getStatus())) {
            return "participated";
        }
        return "unparticipated";
    }

    @Override
    public ActivityComment addComment(Integer userId, Integer activityId, String content, String userTag) {
        ActivityComment comment = new ActivityComment();
        comment.setActivityId(activityId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setUserTag(computeUserTag(userId, activityId));
        commentMapper.insert(comment);
        return commentMapper.findById(comment.getCommentId());
    }

    @Override
    public ActivityComment replyToComment(Integer userId, Integer commentId, String content, String userTag) {
        ActivityComment parent = commentMapper.findById(commentId);
        if (parent == null) throw new RuntimeException("评论不存在");

        ActivityComment reply = new ActivityComment();
        reply.setActivityId(parent.getActivityId());
        reply.setUserId(userId);
        reply.setParentId(commentId);
        reply.setContent(content);
        reply.setUserTag(computeUserTag(userId, parent.getActivityId()));
        commentMapper.insert(reply);
        return commentMapper.findById(reply.getCommentId());
    }

    @Override
    public List<ActivityComment> getComments(Integer activityId) {
        // Self-heal legacy invalid userTag values (e.g. 'admin')
        commentMapper.fixInvalidUserTags();
        return commentMapper.findByActivityId(activityId);
    }

    @Override
    public List<ActivityComment> getReplies(Integer parentId) {
        return commentMapper.findReplies(parentId);
    }

    @Override
    public void deleteComment(Integer userId, Integer commentId) {
        ActivityComment comment = commentMapper.findById(commentId);
        if (comment == null) throw new RuntimeException("评论不存在");
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("只能删除自己的评论");
        commentMapper.deleteById(commentId);
    }

    @Override
    public int getCommentCount(Integer activityId) {
        return commentMapper.countByActivityId(activityId);
    }
}

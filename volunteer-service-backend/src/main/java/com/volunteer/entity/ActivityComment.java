package com.volunteer.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityComment {
    private Integer commentId;
    private Integer activityId;
    private Integer userId;
    private Integer parentId;
    private String content;
    private String userTag;
    private LocalDateTime createTime;

    // Join fields
    private String username;
    private String realName;
    private String avatar;
}

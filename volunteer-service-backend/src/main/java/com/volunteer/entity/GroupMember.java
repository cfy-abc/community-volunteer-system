package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class GroupMember {
    private Integer id;
    private Integer orgId;
    private Integer userId;
    private Integer role; // 0普通成员, 1管理员, 2创建者
    private Date joinTime;
    private Integer contributionHours;
}

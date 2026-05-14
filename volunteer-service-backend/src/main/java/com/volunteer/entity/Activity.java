package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Activity {
    private Integer activityId;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private Double longitude; // 经度
    private Double latitude;  // 纬度
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer rewardHours; // 奖励时长
    private Integer creatorId; // 创建者ID
    private Integer orgId; // 所属组织ID，0表示个人活动
    private Integer status; // 0已取消，1招募中，2进行中，3已结束，4审核中
    private Date createTime;
    private String poster;
    private String contactPhone; // 联系电话
    private String type; // 活动类型
    private String tags; // 标签列表（JSON数组格式）
    private String conditions; // 报名条件列表（JSON数组格式）
    private String feedbacks; // 志愿者反馈（JSON数组格式，可后续收集）
}

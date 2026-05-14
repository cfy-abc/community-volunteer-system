package com.volunteer.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ActivityDTO {
    private Integer activityId;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private Double longitude;
    private Double latitude;
    private Integer maxParticipants;
    private Integer rewardHours;
    private Integer orgId;
    private String poster;
    private String contactPhone; // 联系电话
    private String type; // 活动类型
    private String tags; // 标签列表（JSON数组格式）
    private String conditions; // 报名条件列表（JSON数组格式）
    private String feedbacks; // 志愿者反馈（JSON数组格式，可后续收集）
}

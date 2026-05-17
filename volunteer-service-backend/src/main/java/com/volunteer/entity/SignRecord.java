package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SignRecord {
    private Integer id;
    private Integer userId;
    private Integer activityId;
    private Date checkinTime;
    private Date checkoutTime;
    private String checkinLocation;
    private String checkoutLocation;
    private String checkinPhoto;
    private Integer status; // 0-未签到 1-已签到 2-已签退
    private String qrToken;
    private Date createTime;
    private Date updateTime;
    private Integer approvalStatus; // 0待审批, 1已通过, 2已拒绝
    private Date approvalTime;
    private Double hoursEarned; // 实际服务时长
}
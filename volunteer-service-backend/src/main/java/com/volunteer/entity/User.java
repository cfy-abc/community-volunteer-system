package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Integer userId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer volunteerHours; // 当前可用志愿时长
    private Integer totalEarnedHours; // 累计赚取时长
    private Integer totalSpentHours; // 累计支出时长
    private Integer status; // 0待审核，1通过，2拒绝
    private Date registerTime;
    private String avatar;
}
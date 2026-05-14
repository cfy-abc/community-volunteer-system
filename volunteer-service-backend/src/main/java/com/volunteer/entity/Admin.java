package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Admin {
    private Integer adminId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer role; // 0超级管理员, 1活动组织者, 2普通管理员
    private Integer status; // 0禁用, 1启用
    private Date lastLoginTime;
    private Date createTime;
}

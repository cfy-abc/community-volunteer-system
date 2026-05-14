package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Organization {
    private Integer orgId;
    private String orgName;
    private String description;
    private String contactPhone;
    private String contactEmail;
    private Integer creatorId; // 创建者用户ID
    private Integer status; // 0待审核，1通过，2拒绝
    private Date createTime;
    private Date auditTime;
    private String logo;
}

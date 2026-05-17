package com.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class UserDTO {
    private Integer userId;
    private String username;
    private String password;
    @JsonProperty("realName")
    private String realName;
    private String phone;
    private Integer volunteerHours;
    private Integer totalEarnedHours;
    private Integer totalSpentHours;
    private Integer status;
    private String avatar;
    private String idCard;
    private Integer realNameStatus;
    private Integer isOrgUser;
    private String orgPosition;
    private String orgDepartment;
}

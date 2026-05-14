package com.volunteer.dto;

import lombok.Data;

@Data
public class OrganizationDTO {
    private Integer orgId;
    private String orgName;
    private String description;
    private String contactPhone;
    private String contactEmail;
    private String logo;
}

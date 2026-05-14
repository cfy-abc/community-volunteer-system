package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class VolunteerRecord {
    private Integer recordId;
    private Integer userId;
    private Integer activityId;
    private Integer hoursEarned; // 获得的时长
    private String status; // 'registered', 'attended', 'completed', 'cancelled'
    private Date registerTime;
    private Date checkInTime;
    private Date checkOutTime;
    private String checkInLocation;
    private String checkInPhoto;
}

package com.volunteer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class HourTransfer {
    private Integer transferId;
    private Integer fromUserId;
    private Integer toUserId;
    private Integer hours;
    private String reason;
    private Date transferTime;
    private Integer status; // 0待处理，1已同意，2已拒绝
}
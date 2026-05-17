package com.volunteer.service;

import java.util.List;
import java.util.Map;

public interface VolunteerRecordService {
    List<Map<String, Object>> getUserRecords(Map<String, Object> params);
    int countUserRecords(Map<String, Object> params);
}
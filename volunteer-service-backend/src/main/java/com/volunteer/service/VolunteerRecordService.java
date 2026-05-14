package com.volunteer.service;

import com.volunteer.entity.VolunteerRecord;
import java.util.List;
import java.util.Map;

public interface VolunteerRecordService {
    List<VolunteerRecord> getUserRecords(Map<String, Object> params);
    int countUserRecords(Map<String, Object> params);
}
package com.volunteer.service.impl;

import com.volunteer.entity.VolunteerRecord;
import com.volunteer.mapper.VolunteerRecordMapper;
import com.volunteer.service.VolunteerRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VolunteerRecordServiceImpl implements VolunteerRecordService {

    @Autowired
    private VolunteerRecordMapper recordMapper;

    @Override
    public List<VolunteerRecord> getUserRecords(Map<String, Object> params) {
        return recordMapper.findPage(params);
    }

    @Override
    public int countUserRecords(Map<String, Object> params) {
        return recordMapper.countByUser(params);
    }
}
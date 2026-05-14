package com.volunteer.service;

import com.volunteer.dto.ActivityDTO;
import com.volunteer.entity.Activity;

import java.util.List;
import java.util.Map;

public interface ActivityService {
    void publishActivity(Integer userId, ActivityDTO activityDTO) throws Exception;
    Activity getActivityDetail(Integer activityId) throws Exception;
    List<Activity> getActivityList(Map<String, Object> params);
    int getActivityCount(Map<String, Object> params);
    void registerForActivity(Integer userId, Integer activityId) throws Exception;
    Map<String, Object> getSignStatus(Integer userId, Integer activityId) throws Exception;
    void checkIn(Integer userId, Integer activityId, String location, String photo, String qrToken) throws Exception;
    void checkOut(Integer userId, Integer activityId, String location) throws Exception;
}
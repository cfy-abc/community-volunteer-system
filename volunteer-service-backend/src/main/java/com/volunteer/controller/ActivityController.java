package com.volunteer.controller;

import com.volunteer.dto.ActivityDTO;
import com.volunteer.entity.Activity;
import com.volunteer.service.ActivityService;
import com.volunteer.utils.JwtUtils;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 发布活动
     */
    @PostMapping
    public Result publishActivity(@RequestHeader("Authorization") String token,
                                  @RequestBody ActivityDTO activityDTO) {
        try {
            Integer userId = getUserIdFromToken(token);
            activityService.publishActivity(userId, activityDTO);
            return Result.success("活动发布成功，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Result getActivityDetail(@PathVariable Integer id) {
        try {
            Activity activity = activityService.getActivityDetail(id);
            return Result.success(activity);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取活动列表
     */
    @GetMapping
    public Result getActivityList(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) Integer orgId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("status", status);
            params.put("orgId", orgId);
            params.put("offset", (page - 1) * size);
            params.put("limit", size);

            List<Activity> activities = activityService.getActivityList(params);
            int total = activityService.getActivityCount(params);

            Map<String, Object> result = new HashMap<>();
            result.put("list", activities);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 报名活动
     */
    @PostMapping("/{id}/register")
    public Result registerForActivity(@RequestHeader("Authorization") String token,
                                      @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromToken(token);
            activityService.registerForActivity(userId, id);
            return Result.success("报名成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取签到状态
     */
    @GetMapping("/{id}/sign-status")
    public Result getSignStatus(@RequestHeader("Authorization") String token,
                                @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromToken(token);
            Map<String, Object> status = activityService.getSignStatus(userId, id);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 签到（支持位置+二维码）
     */
    @PostMapping("/{id}/checkin")
    public Result checkIn(@RequestHeader("Authorization") String token,
                          @PathVariable Integer id,
                          @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String location = params.get("location");
            String photo = params.get("photo");
            String qrToken = params.get("qrToken");
            activityService.checkIn(userId, id, location, photo, qrToken);
            return Result.success("签到成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 签退
     */
    @PostMapping("/{id}/checkout")
    public Result checkOut(@RequestHeader("Authorization") String token,
                           @PathVariable Integer id,
                           @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String location = params.get("location");
            activityService.checkOut(userId, id, location);
            return Result.success("签退成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer getUserIdFromToken(String token) throws Exception {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new Exception("无效的Token");
        }
        String actualToken = token.substring(7);
        return jwtUtils.getUserIdFromToken(actualToken);
    }
}
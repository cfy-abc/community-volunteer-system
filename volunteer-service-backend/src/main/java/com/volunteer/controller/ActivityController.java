package com.volunteer.controller;

import com.volunteer.dto.ActivityDTO;
import com.volunteer.entity.Activity;
import com.volunteer.entity.ActivityComment;
import com.volunteer.service.ActivityCommentService;
import com.volunteer.service.ActivityService;
import com.volunteer.utils.JwtUtils;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityCommentService commentService;

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
            return Result.success("活动发布成功");
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

    /**
     * 获取活动评论列表
     */
    @GetMapping("/{id}/comments")
    public Result getComments(@PathVariable Integer id) {
        try {
            List<ActivityComment> comments = commentService.getComments(id);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取评论的回复列表
     */
    @GetMapping("/comments/{commentId}/replies")
    public Result getReplies(@PathVariable Integer commentId) {
        try {
            List<ActivityComment> replies = commentService.getReplies(commentId);
            return Result.success(replies);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加评论
     */
    @PostMapping("/{id}/comments")
    public Result addComment(@RequestHeader("Authorization") String token,
                             @PathVariable Integer id,
                             @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String content = params.get("content");
            String userTag = params.get("userTag");
            if (content == null || content.trim().isEmpty()) {
                return Result.error("评论内容不能为空");
            }
            ActivityComment comment = commentService.addComment(userId, id, content, userTag);
            return Result.success(comment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 回复评论
     */
    @PostMapping("/comments/{commentId}/reply")
    public Result replyToComment(@RequestHeader("Authorization") String token,
                                 @PathVariable Integer commentId,
                                 @RequestBody Map<String, String> params) {
        try {
            Integer userId = getUserIdFromToken(token);
            String content = params.get("content");
            String userTag = params.get("userTag");
            if (content == null || content.trim().isEmpty()) {
                return Result.error("回复内容不能为空");
            }
            ActivityComment reply = commentService.replyToComment(userId, commentId, content, userTag);
            return Result.success(reply);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 组织者扫描志愿者二维码签到（反扫码）
     */
    @PostMapping("/{id}/organizer-checkin")
    public Result organizerCheckIn(@RequestHeader("Authorization") String token,
                                   @PathVariable Integer id,
                                   @RequestBody Map<String, Object> params) {
        try {
            Integer organizerUserId = getUserIdFromToken(token);
            Integer volunteerUserId = params.get("volunteerUserId") != null ?
                    Integer.parseInt(params.get("volunteerUserId").toString()) : null;
            String qrToken = params.get("qrToken") != null ? params.get("qrToken").toString() : null;
            activityService.organizerCheckIn(organizerUserId, volunteerUserId, id, qrToken);
            return Result.success("签到成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public Result deleteComment(@RequestHeader("Authorization") String token,
                                @PathVariable Integer commentId) {
        try {
            Integer userId = getUserIdFromToken(token);
            commentService.deleteComment(userId, commentId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 报名活动（含表单数据）
     */
    @PostMapping("/{id}/apply")
    public Result applyForActivity(@RequestHeader("Authorization") String token,
                                    @PathVariable Integer id,
                                    @RequestBody Map<String, String> formData) {
        try {
            Integer userId = getUserIdFromToken(token);
            activityService.applyForActivity(userId, id, formData);
            return Result.success("报名成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取报名者名单
     */
    @GetMapping("/{id}/applicants")
    public Result getApplicants(@RequestHeader("Authorization") String token,
                                @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromToken(token);
            List<Map<String, Object>> applicants = activityService.getApplicants(id);
            return Result.success(applicants);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出报名名单 Excel
     */
    @GetMapping("/{id}/applicants/export")
    public org.springframework.http.ResponseEntity<byte[]> exportApplicants(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromToken(token);
            byte[] data = activityService.exportApplicants(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition
                    .attachment().filename("applicants.xlsx").build());
            return new org.springframework.http.ResponseEntity<>(data, headers,
                    org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }

    private Integer getUserIdFromToken(String token) throws Exception {
        if (token == null) throw new Exception("未提供Token");
        return jwtUtils.getUserIdFromToken(token);
    }
}
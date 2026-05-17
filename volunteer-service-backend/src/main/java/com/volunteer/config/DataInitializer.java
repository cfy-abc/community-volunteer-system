package com.volunteer.config;

import com.volunteer.entity.Admin;
import com.volunteer.entity.Activity;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.AdminMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private ActivityMapper activityMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        ensureAdminUser();
        ensureTestUsers();
        ensureSampleActivities();
    }

    private void ensureAdminUser() {
        try {
            Admin admin = adminMapper.findByUsername("admin");
            String javaHash = passwordEncoder.encode("admin123");
            if (admin == null) {
                admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword(javaHash);
                admin.setRealName("系统管理员");
                admin.setPhone("13800138000");
                admin.setRole(0);
                admin.setStatus(1);
                adminMapper.insert(admin);
                System.out.println("已创建管理员: admin / admin123");
            } else if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                adminMapper.updatePassword(admin.getAdminId(), javaHash);
                System.out.println("已更新管理员密码为Java BCrypt兼容哈希");
            }
        } catch (Exception e) {
            System.err.println("初始化管理员失败: " + e.getMessage());
        }
    }

    private void ensureTestUsers() {
        ensureTestUser("zhangsan", "张三", "13800000001", 35, 80, 45, 1);
        ensureTestUser("lisi", "李四", "13800000002", 20, 60, 40, 1);
        ensureTestUser("wangwu", "王五", "13800000003", 50, 120, 70, 1);
        ensureTestUser("zhaoliu", "赵六", "13800000004", 15, 40, 25, 1);
        ensureTestUser("sunqi", "孙七", "13800000005", 5, 20, 15, 1);
        ensureTestUser("zhouba", "周八", "13800000006", 0, 10, 10, 1);
        System.out.println("测试用户初始化完成（密码均为: 123456）");
    }

    private void ensureTestUser(String username, String realName, String phone,
                                int volunteerHours, int totalEarned, int totalSpent, int status) {
        try {
            User existing = userMapper.findByUsername(username);
            if (existing != null) {
                String newHash = passwordEncoder.encode("123456");
                if (!passwordEncoder.matches("123456", existing.getPassword())) {
                    existing.setPassword(newHash);
                    userMapper.updatePassword(existing);
                    System.out.println("已更新用户 " + username + " 的密码");
                }
            } else {
                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setRealName(realName);
                user.setPhone(phone);
                user.setVolunteerHours(volunteerHours);
                user.setTotalEarnedHours(totalEarned);
                user.setTotalSpentHours(totalSpent);
                user.setStatus(status);
                user.setAvatar("/default_avatar.png");
                userMapper.insert(user);
                System.out.println("已创建测试用户: " + username);
            }
        } catch (Exception e) {
            System.err.println("初始化用户 " + username + " 失败: " + e.getMessage());
        }
    }

    private void ensureSampleActivities() {
        try {
            // 修复所有状态为4（审核中）的旧活动，改为1（招募中）
            int fixed = activityMapper.fixPendingActivities();
            if (fixed > 0) {
                System.out.println("已自动通过 " + fixed + " 条审核中的活动");
            }

            int count = activityMapper.count(null);
            if (count > 0) {
                System.out.println("活动数据已存在，共 " + count + " 条");
                return;
            }
            insertActivity("社区环保清洁活动", "参与社区环境清洁，维护美好家园。需要志愿者协助清理街道垃圾、分类回收物品、美化公共区域。",
                    "2026-06-10 09:00:00", "2026-06-10 17:00:00", "市中心公园南门", 116.407400, 39.904200,
                    50, 3, 1, 1, "环保公益", "[\"环保\",\"社区\",\"户外\"]", "[\"年满16周岁\",\"身体健康\"]");
            insertActivity("敬老院慰问活动", "陪伴老人聊天，帮助老人整理房间，表演文艺节目，为他们带去温暖和关怀。",
                    "2026-06-15 14:00:00", "2026-06-15 18:00:00", "阳光敬老院", 116.397400, 39.914200,
                    30, 4, 1, 0, "敬老助残", "[\"敬老\",\"关爱\",\"社会\"]", "[\"有耐心和爱心\",\"会表演节目者优先\"]");
            insertActivity("儿童图书馆助教活动", "协助图书馆工作人员管理儿童阅读区域，组织读书会、讲故事等活动。",
                    "2026-06-17 10:00:00", "2026-06-17 16:00:00", "市图书馆少儿区", 116.417400, 39.924200,
                    20, 3, 2, 2, "教育助学", "[\"教育\",\"儿童\",\"阅读\"]", "[\"年满18周岁\",\"有教育经验者优先\"]");
            insertActivity("医院导诊志愿服务", "为患者提供导诊服务，协助就医流程，帮助老年人使用自助设备。",
                    "2026-06-18 08:00:00", "2026-06-18 17:00:00", "市人民医院门诊大厅", 116.427400, 39.934200,
                    15, 5, 3, 0, "医疗健康", "[\"医疗\",\"服务\",\"导诊\"]", "[\"有基本医疗常识\",\"服务意识强\"]");
            insertActivity("文化广场文艺演出", "参与社区文艺演出活动，丰富居民文化生活。招募节目表演者、场务志愿者。",
                    "2026-06-19 18:00:00", "2026-06-19 21:00:00", "文化广场主舞台", 116.437400, 39.944200,
                    40, 4, 1, 1, "文化文艺", "[\"文化\",\"演出\",\"娱乐\"]", "[\"有才艺者优先\",\"守时有责任心\"]");
            insertActivity("春季植树造林活动", "在森林公园开展春季植树活动，种植树苗、浇水施肥，为城市增添绿色。",
                    "2026-06-20 08:00:00", "2026-06-20 16:00:00", "森林公园北门", 116.447400, 39.954200,
                    100, 5, 2, 2, "环保公益", "[\"环保\",\"植树\",\"户外\"]", "[\"自带工具者优先\",\"体力充沛\"]");
            System.out.println("已创建 6 条示例活动数据");
        } catch (Exception e) {
            System.err.println("初始化活动数据失败: " + e.getMessage());
        }
    }

    private void insertActivity(String title, String description, String startTime, String endTime,
                                String location, Double longitude, Double latitude,
                                int maxParticipants, int rewardHours, int creatorId, int orgId,
                                String type, String tags, String conditions) {
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setStartTime(java.sql.Timestamp.valueOf(startTime));
        activity.setEndTime(java.sql.Timestamp.valueOf(endTime));
        activity.setLocation(location);
        activity.setLongitude(longitude);
        activity.setLatitude(latitude);
        activity.setMaxParticipants(maxParticipants);
        activity.setCurrentParticipants(0);
        activity.setRewardHours(rewardHours);
        activity.setCreatorId(creatorId);
        activity.setOrgId(orgId);
        activity.setStatus(1); // 直接招募中，无需审核
        activity.setPoster("/default_activity_poster.jpg");
        activity.setContactPhone("1380000000" + creatorId);
        activity.setType(type);
        activity.setTags(tags);
        activity.setConditions(conditions);
        activityMapper.insert(activity);
    }
}

-- 创建数据库
CREATE DATABASE IF NOT EXISTS volunteer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE volunteer_db;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `phone` VARCHAR(11) NOT NULL COMMENT '手机号',
    `volunteer_hours` INT DEFAULT 0 COMMENT '当前可用志愿时长(余额)',
    `total_earned_hours` INT DEFAULT 0 COMMENT '累计赚取时长',
    `total_spent_hours` INT DEFAULT 0 COMMENT '累计支出时长',
    `status` TINYINT DEFAULT 0 COMMENT '审核状态: 0待审核, 1通过, 2拒绝',
    `register_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `avatar` VARCHAR(255) DEFAULT '/default_avatar.png' COMMENT '头像URL'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
    `admin_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `phone` VARCHAR(11) COMMENT '联系电话',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 0超级管理员, 1活动组织者, 2普通管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用, 1启用',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================
-- 组织(团体)表
-- ============================================
CREATE TABLE IF NOT EXISTS `organization` (
    `org_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '组织ID',
    `org_name` VARCHAR(100) NOT NULL COMMENT '组织名称',
    `description` TEXT COMMENT '组织描述',
    `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `contact_email` VARCHAR(100) COMMENT '联系邮箱',
    `creator_id` INT NOT NULL COMMENT '创建者用户ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待审核, 1通过, 2拒绝',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `audit_time` DATETIME COMMENT '审核时间',
    `logo` VARCHAR(255) DEFAULT '/default_org_logo.png' COMMENT '组织logo',
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

-- ============================================
-- 组织成员表
-- ============================================
CREATE TABLE IF NOT EXISTS `group_member` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    `org_id` INT NOT NULL COMMENT '组织ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role` TINYINT DEFAULT 0 COMMENT '角色: 0普通成员, 1管理员, 2创建者',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `contribution_hours` INT DEFAULT 0 COMMENT '在该组织的贡献时长',
    UNIQUE KEY `uk_org_user` (`org_id`, `user_id`),
    FOREIGN KEY (`org_id`) REFERENCES `organization`(`org_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织成员表';

-- ============================================
-- 活动表
-- ============================================
CREATE TABLE IF NOT EXISTS `activity` (
    `activity_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
    `description` TEXT NOT NULL COMMENT '活动描述',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `location` VARCHAR(255) NOT NULL COMMENT '活动地点',
    `longitude` DECIMAL(10,6) COMMENT '经度',
    `latitude` DECIMAL(10,6) COMMENT '纬度',
    `max_participants` INT NOT NULL COMMENT '最大参与人数',
    `current_participants` INT DEFAULT 0 COMMENT '当前参与人数',
    `reward_hours` INT NOT NULL COMMENT '奖励时长(悬赏时长)',
    `creator_id` INT NOT NULL COMMENT '创建者ID',
    `org_id` INT DEFAULT 0 COMMENT '所属组织ID, 0表示个人活动',
    `status` TINYINT DEFAULT 4 COMMENT '状态: 0已取消, 1招募中, 2进行中, 3已结束, 4审核中',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `poster` VARCHAR(255) DEFAULT '/default_activity_poster.jpg' COMMENT '活动海报',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `type` VARCHAR(50) COMMENT '活动类型: 环保公益,敬老助残,教育助学,医疗健康,社区服务,文化文艺,其他',
    `tags` TEXT COMMENT '标签列表(JSON数组)',
    `conditions` TEXT COMMENT '报名条件(JSON数组)',
    `feedbacks` TEXT COMMENT '志愿者反馈(JSON数组)',
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- ============================================
-- 志愿记录表(报名记录)
-- ============================================
CREATE TABLE IF NOT EXISTS `volunteer_record` (
    `record_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `activity_id` INT NOT NULL COMMENT '活动ID',
    `hours_earned` INT DEFAULT 0 COMMENT '获得时长',
    `status` VARCHAR(20) DEFAULT 'registered' COMMENT '状态: registered(已报名), attended(已签到), completed(已完成), cancelled(已取消)',
    `register_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `check_in_time` DATETIME COMMENT '签到时间',
    `check_out_time` DATETIME COMMENT '签退时间',
    `check_in_location` VARCHAR(255) COMMENT '签到位置',
    `check_in_photo` VARCHAR(255) COMMENT '签到照片',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`activity_id`) REFERENCES `activity`(`activity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿记录表';

-- ============================================
-- 时长转账表
-- ============================================
CREATE TABLE IF NOT EXISTS `hour_transfer` (
    `transfer_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '转账ID',
    `from_user_id` INT NOT NULL COMMENT '转出用户ID',
    `to_user_id` INT NOT NULL COMMENT '转入用户ID',
    `hours` INT NOT NULL COMMENT '转账时长',
    `reason` VARCHAR(255) COMMENT '转账原因',
    `transfer_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '转账时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待处理, 1已同意, 2已拒绝',
    FOREIGN KEY (`from_user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`to_user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时长转账表';

-- ============================================
-- 签到记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `sign_record` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL COMMENT '用户ID',
    `activity_id` INT NOT NULL COMMENT '活动ID',
    `checkin_time` DATETIME DEFAULT NULL COMMENT '签到时间',
    `checkout_time` DATETIME DEFAULT NULL COMMENT '签退时间',
    `checkin_location` VARCHAR(255) DEFAULT NULL COMMENT '签到位置(坐标)',
    `checkout_location` VARCHAR(255) DEFAULT NULL COMMENT '签退位置',
    `checkin_photo` VARCHAR(255) DEFAULT NULL COMMENT '签到照片URL',
    `status` TINYINT DEFAULT 0 COMMENT '0-未签到 1-已签到 2-已签退',
    `qr_token` VARCHAR(64) DEFAULT NULL COMMENT '签到二维码凭证',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ============================================
-- 种子数据
-- ============================================

-- 系统管理员 (密码: admin123)
INSERT INTO `admin` (`username`, `password`, `real_name`, `phone`, `role`, `status`)
VALUES ('admin', '$2a$10$E8b8dhBpb2iLmLzo1uCaf.R1BTpr7YpV2MKY3j37yPfeMG3vvp3GW', '系统管理员', '13800138000', 0, 1);

-- 测试用户 (密码: 123456)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `volunteer_hours`, `total_earned_hours`, `total_spent_hours`, `status`, `avatar`) VALUES
('zhangsan', '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '张三', '13800000001', 35, 80, 45, 1, '/default_avatar.png'),
('lisi',     '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '李四', '13800000002', 20, 60, 40, 1, '/default_avatar.png'),
('wangwu',   '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '王五', '13800000003', 50, 120, 70, 1, '/default_avatar.png'),
('zhaoliu',  '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '赵六', '13800000004', 15, 40, 25, 1, '/default_avatar.png'),
('sunqi',    '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '孙七', '13800000005', 5, 20, 15, 1, '/default_avatar.png'),
('zhouba',   '$2a$10$1qfwBFFrvZmlhbR6HbC4S.cdDctPIHeF/6Vrjj/mOCRHSMZByKtfS', '周八', '13800000006', 0, 10, 10, 0, '/default_avatar.png');

-- 测试组织
INSERT INTO `organization` (`org_name`, `description`, `contact_phone`, `contact_email`, `creator_id`, `status`, `logo`) VALUES
('爱心志愿者协会', '致力于社区服务的志愿者组织，定期开展敬老、环保、助学等公益活动', '13800000001', 'love@example.com', 1, 1, '/default_org_logo.png'),
('环保先锋队', '专注环境保护的志愿团队，倡导绿色生活理念', '13800000002', 'green@example.com', 2, 1, '/default_org_logo.png'),
('阳光助学社', '帮扶困难学生，组织课外辅导和兴趣培养活动', '13800000003', 'edu@example.com', 3, 1, '/default_org_logo.png'),
('社区医疗服务队', '为社区居民提供基础医疗咨询和健康讲座服务', '13800000004', 'health@example.com', 4, 0, '/default_org_logo.png');

-- 组织成员
INSERT INTO `group_member` (`org_id`, `user_id`, `role`) VALUES
(1, 1, 2), (1, 2, 1), (1, 3, 0),
(2, 2, 2), (2, 4, 1), (2, 5, 0),
(3, 3, 2), (3, 1, 0), (3, 5, 0);

-- 测试活动 (各种状态和类型)
INSERT INTO `activity` (`title`, `description`, `start_time`, `end_time`, `location`, `longitude`, `latitude`, `max_participants`, `current_participants`, `reward_hours`, `creator_id`, `org_id`, `status`, `poster`, `contact_phone`, `type`, `tags`, `conditions`) VALUES
('社区环保清洁活动', '参与社区环境清洁，维护美好家园。需要志愿者协助清理街道垃圾、分类回收物品、美化公共区域。', '2026-05-10 09:00:00', '2026-05-10 17:00:00', '市中心公园南门', 116.407400, 39.904200, 50, 23, 3, 1, 1, 1, '/default_activity_poster.jpg', '13800000001', '环保公益', '["环保","社区","户外"]', '["年满16周岁","身体健康"]'),
('敬老院慰问活动', '陪伴老人聊天，帮助老人整理房间，表演文艺节目，为他们带去温暖和关怀。', '2026-05-15 14:00:00', '2026-05-15 18:00:00', '阳光敬老院', 116.397400, 39.914200, 30, 18, 4, 1, 0, 1, '/default_activity_poster.jpg', '13800000001', '敬老助残', '["敬老","关爱","社会"]', '["有耐心和爱心","会表演节目者优先"]'),
('儿童图书馆助教活动', '协助图书馆工作人员管理儿童阅读区域，组织读书会、讲故事等活动，培养孩子们的阅读兴趣。', '2026-05-17 10:00:00', '2026-05-17 16:00:00', '市图书馆少儿区', 116.417400, 39.924200, 20, 20, 3, 2, 2, 2, '/default_activity_poster.jpg', '13800000002', '教育助学', '["教育","儿童","阅读"]', '["年满18周岁","有教育经验者优先"]'),
('医院导诊志愿服务', '为患者提供导诊服务，协助就医流程，帮助老年人使用自助设备，缓解医院门诊压力。', '2026-05-18 08:00:00', '2026-05-18 17:00:00', '市人民医院门诊大厅', 116.427400, 39.934200, 15, 8, 5, 3, 0, 1, '/default_activity_poster.jpg', '13800000003', '医疗健康', '["医疗","服务","导诊"]', '["有基本医疗常识","服务意识强"]'),
('文化广场文艺演出', '参与社区文艺演出活动，丰富居民文化生活。招募节目表演者、场务志愿者、观众引导员。', '2026-05-19 18:00:00', '2026-05-19 21:00:00', '文化广场主舞台', 116.437400, 39.944200, 40, 12, 4, 1, 1, 1, '/default_activity_poster.jpg', '13800000001', '文化文艺', '["文化","演出","娱乐"]', '["有才艺者优先","守时有责任心"]'),
('春季植树造林活动', '在森林公园开展春季植树活动，种植树苗、浇水施肥，为城市增添绿色。', '2026-05-20 08:00:00', '2026-05-20 16:00:00', '森林公园北门', 116.447400, 39.954200, 100, 56, 5, 2, 2, 1, '/default_activity_poster.jpg', '13800000002', '环保公益', '["环保","植树","户外"]', '["自带工具者优先","体力充沛"]'),
('社区安全巡逻', '配合社区警务室进行安全巡逻，排查安全隐患，宣传防火防盗知识。', '2026-05-22 19:00:00', '2026-05-22 22:00:00', '阳光社区居委会', 116.457400, 39.964200, 20, 5, 2, 5, 0, 1, '/default_activity_poster.jpg', '13800000005', '社区服务', '["安全","巡逻","社区"]', '["年满18周岁","责任心强"]');

-- 志愿记录
INSERT INTO `volunteer_record` (`user_id`, `activity_id`, `hours_earned`, `status`, `register_time`) VALUES
(1, 1, 3, 'registered', NOW()),
(2, 1, 3, 'registered', NOW()),
(3, 1, 3, 'registered', NOW()),
(4, 2, 4, 'registered', NOW()),
(5, 2, 4, 'registered', NOW()),
(1, 3, 3, 'registered', NOW()),
(2, 3, 3, 'registered', NOW()),
(3, 4, 5, 'registered', NOW()),
(4, 5, 4, 'registered', NOW()),
(5, 6, 5, 'registered', NOW());

-- 时长转账记录
INSERT INTO `hour_transfer` (`from_user_id`, `to_user_id`, `hours`, `reason`, `status`) VALUES
(1, 2, 5, '感谢帮忙搬家', 1),
(2, 3, 3, '志愿活动协作奖励', 0),
(3, 5, 8, '感谢协助敬老院活动', 1);

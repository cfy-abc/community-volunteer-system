-- ============================================
-- 志愿者管理系统 - 数据库初始化脚本
-- 包含所有建表语句（不含种子数据）
-- ============================================

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
    `avatar` VARCHAR(255) DEFAULT '/default_avatar.png' COMMENT '头像URL',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    `real_name_status` TINYINT DEFAULT 0 COMMENT '实名状态: 0未认证, 1已认证, 2认证中',
    `is_org_user` TINYINT DEFAULT 0 COMMENT '是否组织用户: 0否, 1是',
    `org_position` VARCHAR(100) DEFAULT NULL COMMENT '组织内职务',
    `org_department` VARCHAR(100) DEFAULT NULL COMMENT '所属社区/部门'
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
    `sign_method` VARCHAR(50) DEFAULT 'gps,scan,photo' COMMENT '签到方式,逗号分隔: gps,scan,photo',
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
    `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '报名者姓名',
    `applicant_phone` VARCHAR(20) DEFAULT NULL COMMENT '报名者电话',
    `applicant_email` VARCHAR(100) DEFAULT NULL COMMENT '报名者邮箱',
    `emergency_contact` VARCHAR(50) DEFAULT NULL COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系电话',
    `remarks` TEXT DEFAULT NULL COMMENT '备注信息',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`activity_id`) REFERENCES `activity`(`activity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿记录表';

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
    `approval_status` TINYINT DEFAULT 0 COMMENT '0待审批, 1已通过, 2已拒绝',
    `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `hours_earned` DECIMAL(5,1) DEFAULT 0 COMMENT '实际服务时长(小时)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity` (`activity_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`activity_id`) REFERENCES `activity`(`activity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ============================================
-- 活动留言板表
-- ============================================
CREATE TABLE IF NOT EXISTS `activity_comment` (
    `comment_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    `activity_id` INT NOT NULL COMMENT '活动ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `parent_id` INT DEFAULT NULL COMMENT '父评论ID(NULL=顶级评论)',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `user_tag` VARCHAR(20) NOT NULL COMMENT '用户标签: organizer/participated/unparticipated',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`activity_id`) REFERENCES `activity`(`activity_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_id`) REFERENCES `activity_comment`(`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动留言板';

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
-- 组织用户升级申请表
-- ============================================
CREATE TABLE IF NOT EXISTS `org_upgrade_application` (
    `application_id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `org_id` INT DEFAULT NULL COMMENT '所属组织ID',
    `position` VARCHAR(100) NOT NULL COMMENT '职位',
    `department` VARCHAR(100) NOT NULL COMMENT '所属社区/部门',
    `reason` TEXT COMMENT '申请理由',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待审核, 1通过, 2拒绝',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`org_id`) REFERENCES `organization`(`org_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织用户升级申请表';

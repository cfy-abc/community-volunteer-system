package com.volunteer.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码验证和生成工具
 * 运行这个类生成并验证 BCrypt 密码
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        
        // 生成新的加密密码
        String encoded = encoder.encode(password);
        
        System.out.println("=== BCrypt 密码生成器 ===");
        System.out.println("原始密码: " + password);
        System.out.println("新生成的BCrypt: " + encoded);
        System.out.println();
        
        // 验证我们之前提供的密码
        String oldEncoded = "$2a$10$e0MYzXyjpJS7Pd0RVvHwHe8FVfVpXz4GJl2K9N3mP5qR7sT8uW6xY";
        boolean matches = encoder.matches(password, oldEncoded);
        System.out.println("旧密码是否匹配: " + matches);
        System.out.println();
        
        // 验证新生成的密码
        boolean newMatches = encoder.matches(password, encoded);
        System.out.println("新密码是否匹配: " + newMatches);
        System.out.println();
        
        if (!matches) {
            System.out.println("⚠️  旧密码不匹配！请使用以下SQL更新数据库:");
            System.out.println("UPDATE `volunteer_db`.`user` SET `password` = '" + encoded + "' WHERE `username` = 'admin';");
        } else {
            System.out.println("✅ 旧密码正确，无需更新");
        }
    }
}

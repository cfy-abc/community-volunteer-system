package com.volunteer.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] users = {"zhangsan","lisi","wangwu","zhaoliu","sunqi","zhouba"};

        System.out.println("-- 测试用户 (密码: 123456)");
        for (String u : users) {
            String hash = encoder.encode("123456");
            System.out.printf("('%s', '%s', '%s', '1380000000%d'),%n", u, hash, getRealName(u), getIndex(u));
        }

        System.out.println();
        System.out.println("-- 管理员 (密码: admin123)");
        String adminHash = encoder.encode("admin123");
        System.out.printf("('admin', '%s', '系统管理员', '13800138000'),%n", adminHash);

        System.out.println();
        System.out.println("验证: 新生成hash是否匹配123456？");
        System.out.println(encoder.matches("123456", encoder.encode("123456")));
    }

    static String getRealName(String u) {
        switch(u) {
            case "zhangsan": return "张三"; case "lisi": return "李四";
            case "wangwu": return "王五"; case "zhaoliu": return "赵六";
            case "sunqi": return "孙七"; case "zhouba": return "周八";
            default: return "";
        }
    }
    static int getIndex(String u) {
        switch(u) {
            case "zhangsan": return 1; case "lisi": return 2;
            case "wangwu": return 3; case "zhaoliu": return 4;
            case "sunqi": return 5; case "zhouba": return 6;
            default: return 0;
        }
    }
}

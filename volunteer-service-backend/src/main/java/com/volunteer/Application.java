package com.volunteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("启动成功");
        System.out.println("http://localhost:5173/#/pages/activities/publish");
        System.out.println("http://localhost:5173/#/pages/auth/admin-login");
        System.out.println("http://localhost:5173");
    }
}

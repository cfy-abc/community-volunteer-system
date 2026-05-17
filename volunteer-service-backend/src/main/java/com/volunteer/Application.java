package com.volunteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("启动成功");
        System.out.println("http://localhost:8080/api/users/ping");
        System.out.println("http://localhost:5173/#/pages/auth/admin-login");
        System.out.println("http://localhost:5173");
    }
}

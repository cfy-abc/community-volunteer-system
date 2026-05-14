package com.volunteer.service;

import java.util.Map;

public interface AdminService {
    String login(String username, String password) throws Exception;
    Map<String, Object> getDashboardData() throws Exception;
}

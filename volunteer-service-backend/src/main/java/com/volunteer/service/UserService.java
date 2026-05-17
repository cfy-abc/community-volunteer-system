package com.volunteer.service;

import com.volunteer.dto.UserDTO;
import com.volunteer.dto.OrganizationDTO;

import java.util.Map;

public interface UserService {
    void register(UserDTO userDTO) throws Exception;
    String login(String username, String password) throws Exception;
    UserDTO getCurrentUser(String token) throws Exception;
    UserDTO getUserById(Integer userId) throws Exception;
    UserDTO updateUserInfo(Integer userId, Map<String, String> params) throws Exception;
    void createOrganization(String token, OrganizationDTO organizationDTO) throws Exception;
    void verifyIdentity(Integer userId, String realName, String idCard) throws Exception;
    Map<String, Object> getVerifyStatus(Integer userId) throws Exception;
    void applyOrgUpgrade(Integer userId, String position, String department, String reason) throws Exception;
    Map<String, Object> getOrgUpgradeStatus(Integer userId) throws Exception;
    Map<String, Object> getUserStats(Integer userId) throws Exception;
    void cancelApplication(Integer userId, Integer recordId) throws Exception;
    String getWechatAuthUrl() throws Exception;
    String wechatLogin(String code) throws Exception;
}

package com.volunteer.service;

import com.volunteer.dto.UserDTO;
import com.volunteer.dto.OrganizationDTO;

public interface UserService {
    void register(UserDTO userDTO) throws Exception;
    String login(String username, String password) throws Exception;
    UserDTO getCurrentUser(String token) throws Exception;
    UserDTO getUserById(Integer userId) throws Exception;
    void createOrganization(String token, OrganizationDTO organizationDTO) throws Exception;
}

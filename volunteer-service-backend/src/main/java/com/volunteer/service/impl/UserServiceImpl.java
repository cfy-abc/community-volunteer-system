package com.volunteer.service.impl;

import com.volunteer.dto.UserDTO;
import com.volunteer.dto.OrganizationDTO;
import com.volunteer.entity.Organization;
import com.volunteer.entity.User;
import com.volunteer.mapper.OrganizationMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.service.UserService;
import com.volunteer.utils.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Override
    @Transactional
    public void register(UserDTO userDTO) throws Exception {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(userDTO.getUsername()) != null) {
            throw new Exception("用户名已存在");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(jwtUtils.encodePassword(userDTO.getPassword())); // 密码加密
        user.setRealName(userDTO.getRealName());
        user.setPhone(userDTO.getPhone());
        user.setVolunteerHours(0);
        user.setTotalEarnedHours(0);
        user.setTotalSpentHours(0);
        user.setStatus(0); // 0待审核，管理员审核后变为1
        user.setAvatar("/default_avatar.png");

        userMapper.insert(user);
    }

    @Override
    public String login(String username, String password) throws Exception {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        if (!jwtUtils.checkPassword(password, user.getPassword())) {
            throw new Exception("密码错误");
        }

        if (user.getStatus() != 1) {
            throw new Exception("账户未通过审核");
        }

        // 生成JWT token
        return jwtUtils.generateToken(user.getUserId(), user.getUsername());
    }

    @Override
    public UserDTO getCurrentUser(String token) throws Exception {
        Integer userId = jwtUtils.getUserIdFromToken(token);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhone(user.getPhone());
        userDTO.setVolunteerHours(user.getVolunteerHours());
        userDTO.setTotalEarnedHours(user.getTotalEarnedHours());
        userDTO.setTotalSpentHours(user.getTotalSpentHours());
        userDTO.setStatus(user.getStatus());
        userDTO.setAvatar(user.getAvatar());

        return userDTO;
    }

    @Override
    public UserDTO getUserById(Integer userId) throws Exception {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhone(user.getPhone());
        userDTO.setVolunteerHours(user.getVolunteerHours());
        userDTO.setTotalEarnedHours(user.getTotalEarnedHours());
        userDTO.setTotalSpentHours(user.getTotalSpentHours());
        userDTO.setStatus(user.getStatus());
        userDTO.setAvatar(user.getAvatar());

        return userDTO;
    }

    @Override
    @Transactional
    public void createOrganization(String token, OrganizationDTO organizationDTO) throws Exception {
        Integer userId = jwtUtils.getUserIdFromToken(token);
        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new Exception("用户不存在或未通过审核");
        }

        if (organizationMapper.findByCreatorId(userId) != null) {
            throw new Exception("您已创建过组织，不能重复创建");
        }

        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDTO, organization);
        organization.setCreatorId(userId);
        organization.setStatus(0);
        organization.setLogo(organizationDTO.getLogo() != null ? organizationDTO.getLogo() : "/default_org_logo.png");

        organizationMapper.insert(organization);
    }
}
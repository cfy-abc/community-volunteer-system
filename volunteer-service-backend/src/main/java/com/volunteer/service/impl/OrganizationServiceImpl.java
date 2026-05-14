package com.volunteer.service.impl;

import com.volunteer.dto.OrganizationDTO;
import com.volunteer.entity.Organization;
import com.volunteer.entity.User;
import com.volunteer.mapper.OrganizationMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.service.OrganizationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void createOrganization(Integer userId, OrganizationDTO organizationDTO) throws Exception {
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }

        // 检查用户是否已有组织
        if (organizationMapper.findByCreatorId(userId) != null) {
            throw new Exception("您已创建过组织，不能重复创建");
        }

        // 创建组织
        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDTO, organization);
        organization.setCreatorId(userId);
        organization.setStatus(0); // 0表示待审核
        organization.setLogo(organizationDTO.getLogo() != null ? organizationDTO.getLogo() : "/default_org_logo.png");

        organizationMapper.insert(organization);
    }

    @Override
    public Organization getOrganizationByCreatorId(Integer userId) {
        return organizationMapper.findByCreatorId(userId);
    }

    @Override
    public boolean hasActiveOrganization(Integer userId) {
        Organization org = organizationMapper.findByCreatorId(userId);
        return org != null && org.getStatus() == 1;
    }

    @Override
    public List<Organization> getAllActiveOrganizations() {
        return organizationMapper.findAllActive();
    }
}

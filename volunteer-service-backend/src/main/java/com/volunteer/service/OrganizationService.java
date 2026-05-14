package com.volunteer.service;

import com.volunteer.dto.OrganizationDTO;
import com.volunteer.entity.Organization;

import java.util.List;

public interface OrganizationService {
    void createOrganization(Integer userId, OrganizationDTO organizationDTO) throws Exception;
    Organization getOrganizationByCreatorId(Integer userId);
    boolean hasActiveOrganization(Integer userId);
    List<Organization> getAllActiveOrganizations();
}

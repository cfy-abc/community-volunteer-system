package com.volunteer.mapper;

import com.volunteer.entity.Organization;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrganizationMapper {

    @Insert("INSERT INTO organization(org_name, description, contact_phone, contact_email, creator_id, status, create_time, logo) " +
            "VALUES(#{orgName}, #{description}, #{contactPhone}, #{contactEmail}, #{creatorId}, #{status}, NOW(), #{logo})")
    @Options(useGeneratedKeys = true, keyProperty = "orgId")
    int insert(Organization organization);

    @Select("SELECT * FROM organization WHERE org_id = #{orgId}")
    Organization findById(Integer orgId);

    @Select("SELECT * FROM organization WHERE creator_id = #{creatorId}")
    Organization findByCreatorId(Integer creatorId);

    @Select("SELECT * FROM organization WHERE status = 1")
    List<Organization> findAllActive();

    @Update("UPDATE organization SET status = #{status}, audit_time = NOW() WHERE org_id = #{orgId}")
    int updateStatus(@Param("orgId") Integer orgId, @Param("status") Integer status);
}
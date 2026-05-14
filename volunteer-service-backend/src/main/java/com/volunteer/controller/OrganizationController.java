package com.volunteer.controller;

import com.volunteer.dto.OrganizationDTO;
import com.volunteer.entity.GroupMember;
import com.volunteer.entity.Organization;
import com.volunteer.mapper.GroupMemberMapper;
import com.volunteer.service.OrganizationService;
import com.volunteer.utils.JwtUtils;
import com.volunteer.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    /**
     * 创建组织
     */
    @PostMapping
    public Result createOrganization(@RequestHeader("Authorization") String token,
                                     @RequestBody OrganizationDTO organizationDTO) {
        try {
            Integer userId = getUserIdFromToken(token);
            organizationService.createOrganization(userId, organizationDTO);
            return Result.success("组织创建成功，请等待审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户的组织
     */
    @GetMapping("/current")
    public Result getCurrentOrganization(@RequestHeader("Authorization") String token) {
        try {
            Integer userId = getUserIdFromToken(token);
            Organization organization = organizationService.getOrganizationByCreatorId(userId);
            return Result.success(organization);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 加入组织
     */
    @PostMapping("/{orgId}/join")
    public Result joinOrganization(@RequestHeader("Authorization") String token,
                                   @PathVariable Integer orgId) {
        try {
            Integer userId = getUserIdFromToken(token);
            GroupMember existing = groupMemberMapper.findByOrgAndUser(orgId, userId);
            if (existing != null) {
                return Result.error("您已经是该组织成员");
            }
            GroupMember member = new GroupMember();
            member.setOrgId(orgId);
            member.setUserId(userId);
            member.setRole(0);
            groupMemberMapper.insert(member);
            return Result.success("加入成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 退出组织
     */
    @DeleteMapping("/{orgId}/leave")
    public Result leaveOrganization(@RequestHeader("Authorization") String token,
                                    @PathVariable Integer orgId) {
        try {
            Integer userId = getUserIdFromToken(token);
            GroupMember member = groupMemberMapper.findByOrgAndUser(orgId, userId);
            if (member == null) {
                return Result.error("您不是该组织成员");
            }
            if (member.getRole() == 2) {
                return Result.error("创建者不能退出组织，请先转让或解散组织");
            }
            groupMemberMapper.delete(orgId, userId);
            return Result.success("已退出组织");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取组织成员列表
     */
    @GetMapping("/{orgId}/members")
    public Result getMembers(@PathVariable Integer orgId) {
        try {
            List<GroupMember> members = groupMemberMapper.findByOrgId(orgId);
            return Result.success(members);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有活跃组织列表
     */
    @GetMapping
    public Result getAllOrganizations() {
        try {
            return Result.success(organizationService.getAllActiveOrganizations());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer getUserIdFromToken(String token) throws Exception {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new Exception("无效的Token");
        }
        String actualToken = token.substring(7);
        return jwtUtils.getUserIdFromToken(actualToken);
    }
}

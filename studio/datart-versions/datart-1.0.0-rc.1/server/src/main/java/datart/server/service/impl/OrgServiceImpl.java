/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.service.impl;

import datart.core.base.consts.Const;
import datart.core.base.consts.FileOwner;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.NotAllowedException;
import datart.core.common.UUIDGenerator;
import datart.core.entity.*;
import datart.core.entity.ext.RoleBaseInfo;
import datart.core.entity.ext.UserBaseInfo;
import datart.core.mappers.ext.*;
import datart.security.base.InviteToken;
import datart.security.base.RoleType;
import datart.security.util.JwtUtils;
import datart.security.util.PermissionHelper;
import datart.server.base.dto.InviteMemberResponse;
import datart.server.base.dto.OrganizationBaseInfo;
import datart.server.base.params.OrgCreateParam;
import datart.server.base.params.OrgUpdateParam;
import datart.server.service.BaseService;
import datart.server.service.FileService;
import datart.server.service.MailService;
import datart.server.service.OrgService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrgServiceImpl extends BaseService implements OrgService {

    private final OrganizationMapperExt organizationMapper;

    private final RoleMapperExt roleMapper;

    private final UserMapperExt userMapper;

    private final FileService fileService;

    private final RelRoleUserMapperExt rruMapper;

    private final RelUserOrganizationMapperExt ruoMapper;

    private final MailService mailService;

    public OrgServiceImpl(OrganizationMapperExt organizationMapper, RoleMapperExt roleMapper, UserMapperExt userMapper, FileService fileService, RelRoleUserMapperExt rruMapper, RelUserOrganizationMapperExt ruoMapper, MailService mailService) {
        this.organizationMapper = organizationMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.fileService = fileService;
        this.rruMapper = rruMapper;
        this.ruoMapper = ruoMapper;
        this.mailService = mailService;
    }

    @Override
    public List<OrganizationBaseInfo> listOrganizations() {
        List<Organization> organizations = organizationMapper.listOrganizationsByUserId(getCurrentUser().getId());
        return organizations.stream().map(OrganizationBaseInfo::new).collect(Collectors.toList());
    }

    @Override
    public OrganizationBaseInfo getOrgDetail(String orgId) {
        securityManager.requireOrgOwner(orgId);
        Organization organization = retrieve(orgId);
        return new OrganizationBaseInfo(organization);
    }

    @Override
    @Transactional
    public boolean updateOrganization(OrgUpdateParam updateParam) {
        securityManager.requireOrgOwner(updateParam.getId());
        boolean success = update(updateParam);
        log.info("The organization({}) is updated by user {}", updateParam.getId(), getCurrentUser().getUsername());
        return success;
    }

    @Override
    @Transactional
    public Organization createOrganization(OrgCreateParam createParam) {
        Organization organization = create(createParam);
        initOrganization(organization, getCurrentUser());
        return organization;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void initOrganization(Organization organization, User creator) {
        //插入创建者和组织关联关系
        RelUserOrganization ruo = new RelUserOrganization();
        ruo.setId(UUIDGenerator.generate());
        ruo.setCreateBy(creator.getId());
        ruo.setCreateTime(new Date());
        ruo.setUserId(creator.getId());
        ruo.setOrgId(organization.getId());
        ruoMapper.insert(ruo);
        //为组织创建默认角色：Owner
        createDefaultRole(RoleType.ORG_OWNER, creator, organization);
    }

    /**
     * 创建角色Owner
     * Owner角色无需分配权限，默认拥有最高权限
     *
     * @param creator 组织的创建用户
     * @param org     创建的组织
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createDefaultRole(RoleType roleType, User creator, Organization org) {
        //创建默认角色
        Role role = getDefaultRoleEntity(roleType, creator, org);
        roleMapper.insert(role);
        //关联角色至user
        RelRoleUser relRoleUser = getRelRoleUserEntity(creator, role);
        rruMapper.insert(relRoleUser);
    }

    private RelRoleUser getRelRoleUserEntity(User user, Role role) {
        RelRoleUser relRoleUser = new RelRoleUser();
        relRoleUser.setId(UUIDGenerator.generate());
        relRoleUser.setRoleId(role.getId());
        relRoleUser.setUserId(user.getId());
        relRoleUser.setCreateBy(user.getId());
        relRoleUser.setCreateTime(new Date());
        return relRoleUser;
    }

    private Role getDefaultRoleEntity(RoleType roleType, User creator, Organization org) {
        Role role = new Role();
        role.setId(UUIDGenerator.generate());
        role.setOrgId(org.getId());
        role.setName(roleType.name());
        role.setType(roleType.name());
        role.setCreateBy(creator.getId());
        role.setCreateTime(new Date());
        return role;
    }

    @Override
    @Transactional
    public boolean deleteOrganization(String orgId) {
        securityManager.requireOrgOwner(orgId);
        organizationMapper.deleteOrg(orgId);
        log.info("Organization deleted By User {}", getCurrentUser().getUsername());
        return true;
    }

    @Override
    public boolean updateAvatar(String orgId, String path) {
        Organization update = new Organization();
        update.setId(orgId);
        update.setAvatar(path);
        return organizationMapper.updateByPrimaryKeySelective(update) == 1;
    }


    @Override
    public List<UserBaseInfo> listOrgMembers(String orgId) {
        securityManager.requireAllPermissions(PermissionHelper.userPermission(orgId, Const.READ));
        List<User> users = organizationMapper.listOrgMembers(orgId);
        if (users == null || users.size() == 0) {
            return Collections.emptyList();
        }
        List<User> owners = organizationMapper.selectOrgOwners(orgId);
        final List<String> owernIds = owners.stream().map(User::getId).collect(Collectors.toList());
        return users.stream().map(user -> {
            UserBaseInfo userBaseInfo = new UserBaseInfo(user);
            userBaseInfo.setOrgOwner(owernIds.contains(user.getId()));
            return userBaseInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoleBaseInfo> listOrgRoles(String orgId) {
        List<Role> roles = roleMapper.listByOrgId(orgId);
        if (roles == null || roles.size() == 0) {
            return Collections.emptyList();
        }
        return roles.stream().map(RoleBaseInfo::new).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InviteMemberResponse addMembers(String orgId, Set<String> emails, boolean sendMail) {
        securityManager.requireOrgOwner(orgId);
        InviteMemberResponse response = new InviteMemberResponse();
        response.setFail(new HashSet<>());
        response.setSuccess(new HashSet<>());
        //过滤掉无效用户
        Set<User> validUsers = emails.stream().map(userId -> {
            User user = userMapper.selectByEmail(userId);
            if (user == null || !user.getActive()) {
                response.getFail().add(userId);
                return null;
            }
            return user;
        }).filter(Objects::nonNull).collect(Collectors.toSet());

        //邀请用户
        validUsers.forEach(user -> {
            try {
                if (sendMail) {
                    sendInviteMail(user, orgId);
                } else {
                    addUserToOrg(user.getId(), orgId);
                }
                response.getSuccess().add(user.getUsername());
            } catch (Exception e) {
                log.error("Invite user " + user.getUsername() + " error", e);
                response.getFail().add(user.getUsername());
            }
        });
        return response;
    }

    @Override
    public boolean confirmInvite(String token) {
        InviteToken inviteToken = null;
        try {
            inviteToken = JwtUtils.toInviteToken(token);
        } catch (ExpiredJwtException e) {
            Exceptions.msg("message.user.confirm.mail.timeout");
        }
        addUserToOrg(inviteToken.getUserId(), inviteToken.getOrgId());
        return true;
    }

    /**
     * 将用户加入组织并赋予普通成员角色
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addUserToOrg(String userId, String orgId) {
        //建立组织和用户的关系
        if (ruoMapper.selectByUserAndOrg(userId, orgId) != null) {
            return;
        }
        RelUserOrganization relUserOrganization = new RelUserOrganization();
        relUserOrganization.setId(UUIDGenerator.generate());
        relUserOrganization.setCreateBy(getCurrentUser()==null?userId:getCurrentUser().getId());
        relUserOrganization.setCreateTime(new Date());
        relUserOrganization.setUserId(userId);
        relUserOrganization.setOrgId(orgId);
        ruoMapper.insert(relUserOrganization);
    }

    @Override
    public Organization checkTeamOrg() {
        List<Organization> organizations = organizationMapper.list();
        if (CollectionUtils.isEmpty(organizations)) {
            return null;
        } else if (organizations.size() == 1) {
            return organizations.get(0);
        } else {
            Exceptions.base("There is more than one organization in team tenant-management-mode, please initialize database or switch to platform tenant-management-mode.");
        }
        return null;
    }

    private void sendInviteMail(User user, String orgId) throws UnsupportedEncodingException, MessagingException {
        mailService.sendInviteMail(user, organizationMapper.selectByPrimaryKey(orgId));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean removeUser(String orgId, String userId) {
        Organization organization = organizationMapper.selectForUpdate(orgId);
        securityManager.requireOrgOwner(orgId);

        if (organization.getCreateBy().equals(userId)) {
            Exceptions.tr(NotAllowedException.class,"message.org.member.delete-creator");
        }
        //
        if (getCurrentUser().getId().equals(userId)) {
            Exceptions.tr(NotAllowedException.class,"message.org.member.delete-self");
        }
        organizationMapper.deleteOrgMember(orgId, userId);
        log.info("User{} deleted by {}", userId, getCurrentUser().getUsername());
        return true;
    }

    @Override
    public List<RoleBaseInfo> listUserRoles(String orgId, String userId) {
        securityManager.requireAllPermissions(PermissionHelper.rolePermission(orgId, Const.READ));
        List<Role> roles = roleMapper.listUserGeneralRoles(orgId, userId);
        if (roles == null || roles.size() == 0) {
            return Collections.emptyList();
        }
        return roles.stream().map(RoleBaseInfo::new).collect(Collectors.toList());
    }

    @Override
    public void requirePermission(Organization entity, int permission) {
        if ((Const.CREATE | permission) == Const.CREATE) {
            return;
        }
        securityManager.requireAllPermissions(PermissionHelper.rolePermission(entity.getId(), permission));
    }

    @Override
    public void deleteStaticFiles(Organization org) {
        fileService.deleteFiles(FileOwner.ORG_AVATAR, org.getId());
    }
}
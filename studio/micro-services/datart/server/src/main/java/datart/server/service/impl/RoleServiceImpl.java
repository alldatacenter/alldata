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
import datart.core.base.exception.Exceptions;
import datart.core.common.UUIDGenerator;
import datart.core.entity.*;
import datart.core.entity.ext.UserBaseInfo;
import datart.core.mappers.ext.*;
import datart.security.base.PermissionInfo;
import datart.security.base.ResourceType;
import datart.security.base.RoleType;
import datart.security.base.SubjectType;
import datart.security.util.PermissionHelper;
import datart.server.base.dto.ResourcePermissions;
import datart.server.base.dto.SubjectPermissions;
import datart.server.base.dto.ViewPermission;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.params.GrantPermissionParam;
import datart.server.base.params.ViewPermissionParam;
import datart.server.service.BaseService;
import datart.server.service.OrgService;
import datart.server.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class RoleServiceImpl extends BaseService implements RoleService {

    private final RoleMapperExt roleMapper;

    private final RelRoleUserMapperExt rruMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    private final RelSubjectColumnsMapperExt rscMapper;

    private final OrgService orgService;

    public RoleServiceImpl(RoleMapperExt roleMapper,
                           RelRoleUserMapperExt rruMapper,
                           RelRoleResourceMapperExt rrrMapper,
                           RelSubjectColumnsMapperExt rscMapper,
                           OrgService orgService,
                           UserMapperExt userMapper) {
        this.roleMapper = roleMapper;
        this.rruMapper = rruMapper;
        this.rrrMapper = rrrMapper;
        this.rscMapper = rscMapper;
        this.orgService = orgService;
    }


    @Override
    @Transactional
    public Role create(BaseCreateParam createParam) {
        Role role = new Role();
        BeanUtils.copyProperties(createParam, role);
        role.setId(UUIDGenerator.generate());
        role.setCreateBy(getCurrentUser().getId());
        role.setCreateTime(new Date());
        role.setType(RoleType.NORMAL.name());
        roleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return roleMapper.deleteRole(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateUsersForRole(String roleId, Set<String> userIds) {
        Role role = retrieve(roleId);
        securityManager.requireAllPermissions(PermissionHelper.rolePermission(role.getOrgId(), Const.READ | Const.MANAGE),
                PermissionHelper.userPermission(role.getOrgId(), Const.READ | Const.MANAGE));
        List<User> users = roleMapper.listRoleUsers(roleId);
        List<String> userToDelete = new ArrayList<>();
        for (User user : users) {
            if (!userIds.contains(user.getId())) {
                userToDelete.add(user.getId());
            }
        }
        rruMapper.deleteByRoleIdAndUserIds(roleId, userToDelete);
        List<String> userToAdd = new ArrayList<>();
        List<String> collect = users.stream().map(User::getId).collect(Collectors.toList());
        for (String userId : userIds) {
            if (!collect.contains(userId)) {
                userToAdd.add(userId);
            }
        }
        addUsersForRole(roleId, userToAdd);
        return true;
    }

    @Override
    public boolean updateRolesForUser(String userId, String orgId, Set<String> roleIds) {

        requireExists(userId, User.class);

        securityManager.requireAllPermissions(
                PermissionHelper.userPermission(orgId, Const.READ | Const.MANAGE));

        List<Role> roles = roleMapper.selectUserAllRoles(userId);

        LinkedList<String> roleToDelete = new LinkedList<>();

        for (Role role : roles) {
            if (!RoleType.NORMAL.name().equals(role.getType())) {
                continue;
            }
            if (!roleIds.contains(role.getId())) {
                roleToDelete.add(role.getId());
            }
        }
        if (!CollectionUtils.isEmpty(roleToDelete)) {
            rruMapper.deleteByUserAndRoles(userId, roleToDelete);
        }

        List<String> currentRoleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
        LinkedList<String> roleToAdd = new LinkedList<>();
        for (String roleId : roleIds) {
            if (!currentRoleIds.contains(roleId)) {
                requireExists(roleId, Role.class);
                roleToAdd.add(roleId);
            }
        }
        addRolesForUser(userId, roleToAdd);
        return true;
    }

    private void addUsersForRole(String roleId, List<String> userIds) {
        List<RelRoleUser> records = userIds
                .stream()
                .map(userId -> createRelRoleUser(userId, roleId)).collect(Collectors.toList());
        if (records.size() > 0) {
            rruMapper.insertBatch(records);
        }
    }

    private void addRolesForUser(String userId, List<String> roleIds) {
        List<RelRoleUser> records = roleIds
                .stream()
                .map(roleId -> createRelRoleUser(userId, roleId)).collect(Collectors.toList());
        if (records.size() > 0) {
            rruMapper.insertBatch(records);
        }
    }

    private RelRoleUser createRelRoleUser(String userId, String roleId) {
        RelRoleUser relRoleUser = new RelRoleUser();
        relRoleUser.setId(UUIDGenerator.generate());
        relRoleUser.setCreateBy(getCurrentUser().getId());
        relRoleUser.setCreateTime(new Date());
        relRoleUser.setUserId(userId);
        relRoleUser.setRoleId(roleId);
        return relRoleUser;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Role getPerUserRole(String orgId, String userId) {
        Role role = roleMapper.selectPerUserRole(orgId, userId);
        if (role == null) {
            role = new Role();
            role.setId(UUIDGenerator.generate());
            role.setOrgId(orgId);
            role.setName("per-user-" + userId);
            role.setType(RoleType.PER_USER.name());
            role.setCreateBy(userId);
            role.setCreateTime(new Date());
            roleMapper.insert(role);
            // add relation
            RelRoleUser relRoleUser = createRelRoleUser(userId, role.getId());
            rruMapper.insert(relRoleUser);
        }
        return role;
    }

    @Override
    public User getPerUserRoleUser(String roleId) {
        return roleMapper.selectPerUserRoleUser(roleId);
    }

    @Override
    public List<Role> listUserRoles(String orgId, String userId) {
        return roleMapper.selectUserRoles(orgId, userId);
    }

    @Override
    public List<UserBaseInfo> listRoleUsers(String roleId) {
        Role role = retrieve(roleId);
        securityManager.requireAllPermissions(PermissionHelper.rolePermission(role.getOrgId(), Const.READ)
                , PermissionHelper.userPermission(role.getOrgId(), Const.READ));
        List<User> users = roleMapper.listRoleUsers(roleId);
        return users.stream().map(UserBaseInfo::new).collect(Collectors.toList());
    }

    public RelRoleResource upsertPermission(RelRoleResource relRoleResource) {
        // disable permission, delete the permissions
        if ("*".equals(relRoleResource.getResourceId()) && relRoleResource.getPermission() == Const.DISABLE) {
            rrrMapper.deleteByResource(relRoleResource.getResourceType(), relRoleResource.getResourceId());
            return null;
        }
        if (StringUtils.isBlank(relRoleResource.getId())) {
            relRoleResource.setId(UUIDGenerator.generate());
            relRoleResource.setCreateBy(getCurrentUser().getId());
            relRoleResource.setCreateTime(new Date());
            rrrMapper.insert(relRoleResource);
        } else {
            relRoleResource.setUpdateTime(new Date());
            relRoleResource.setUpdateBy(getCurrentUser().getId());
            rrrMapper.updateByPrimaryKeySelective(relRoleResource);
        }
        return relRoleResource;
    }

    public PermissionInfo upsertPermission(PermissionInfo permissionInfo) {
        RelRoleResource relRoleResource = new RelRoleResource();
        relRoleResource.setId(permissionInfo.getId());
        relRoleResource.setResourceType(permissionInfo.getResourceType().name());
        relRoleResource.setOrgId(permissionInfo.getOrgId());
        relRoleResource.setPermission(permissionInfo.getPermission());
        if (permissionInfo.getSubjectType() == SubjectType.USER) {
            Role role = roleMapper.selectPerUserRole(permissionInfo.getOrgId(), permissionInfo.getSubjectId());
            relRoleResource.setRoleId(role.getId());
        } else if (permissionInfo.getSubjectType() == SubjectType.USER_ROLE) {
            Role role = getPerUserRole(permissionInfo.getOrgId(), permissionInfo.getSubjectId());
            relRoleResource.setRoleId(role.getId());
        } else {
            relRoleResource.setRoleId(permissionInfo.getSubjectId());
        }
        relRoleResource.setResourceId(permissionInfo.getResourceId());
        RelRoleResource rrr = upsertPermission(relRoleResource);
        if (rrr != null) {
            permissionInfo.setId(rrr.getId());
        }
        return permissionInfo;

    }

    @Override
    public boolean grantPermission(List<PermissionInfo> permissionInfo) {
        if (CollectionUtils.isEmpty(permissionInfo)) {
            return true;
        }
        permissionInfo.stream().filter(permission -> {
            Role role;
            if (SubjectType.USER.equals(permission.getSubjectType())) {
                role = getPerUserRole(permission.getOrgId(), permission.getSubjectId());
            } else {
                role = roleMapper.selectByPrimaryKey(permission.getSubjectId());
            }
            if (role == null) {
                return false;
            }
            return !RoleType.ORG_OWNER.name().equals(role.getType());
        }).forEach(this::upsertPermission);
        return true;
    }

    @Override
    @Transactional
    public boolean grantOrgOwner(String orgId, String userId, boolean checkPermission) {
        if (checkPermission) {
            securityManager.requireOrgOwner(orgId);
        }
        requireExists(orgId, Organization.class);
        requireExists(userId, User.class);
        Role role = roleMapper.selectOrgOwnerRole(orgId);
        RelRoleUser rru = rruMapper.selectByUserAndRole(userId, role.getId());
        if (rru != null) {
            return true;
        }
        RelRoleUser relRoleUser = createRelRoleUser(userId, role.getId());
        rruMapper.insert(relRoleUser);
        // add user to org
        orgService.addUserToOrg(userId, orgId);
        return true;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean revokeOrgOwner(String orgId, String userId) {
        Organization organization = orgService.getDefaultMapper().selectForUpdate(orgId);
        List<User> users = roleMapper.selectOrgOwners(orgId);
        if (users.size() < 2) {
            Exceptions.msg("resource.organization.owner");
        }
        securityManager.requireOrgOwner(orgId);
        Role role = roleMapper.selectOrgOwnerRole(orgId);
        rruMapper.deleteByUserAndRole(userId, role.getId());
        return true;
    }

    @Override
    @Transactional
    public List<PermissionInfo> grantPermission(GrantPermissionParam grantPermissionParam) {

        Set<String> orgId = new HashSet<>();

        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToUpdate())) {
            for (PermissionInfo permissionInfo : grantPermissionParam.getPermissionToUpdate()) {
                orgId.add(permissionInfo.getOrgId());
            }
        }
        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToCreate())) {
            for (PermissionInfo permissionInfo : grantPermissionParam.getPermissionToCreate()) {
                orgId.add(permissionInfo.getOrgId());
            }
        }
        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToDelete())) {
            for (PermissionInfo permissionInfo : grantPermissionParam.getPermissionToDelete()) {
                orgId.add(permissionInfo.getOrgId());
            }
        }
        if (orgId.size() > 1) {
            Exceptions.base("The org id has to be unique");
        }
        securityManager.requireOrgOwner(orgId.stream().findFirst().get());
        // delete permission
        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToDelete())) {
            List<String> ids = grantPermissionParam.getPermissionToDelete()
                    .stream()
                    .map(PermissionInfo::getId).collect(Collectors.toList());
            rrrMapper.deleteByPrimaryKeys(RelRoleResource.class, ids);
        }
        // update permissions
        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToUpdate())) {
            grantPermissionParam
                    .getPermissionToUpdate()
                    .forEach(this::upsertPermission);
        }
        // create permissions
        List<PermissionInfo> createdPermission = null;
        if (!CollectionUtils.isEmpty(grantPermissionParam.getPermissionToCreate())) {
            createdPermission = grantPermissionParam
                    .getPermissionToCreate()
                    .stream()
                    .map(this::upsertPermission)
                    .collect(Collectors.toList());
        }
        return createdPermission == null ? Collections.emptyList() : createdPermission;
    }

    @Override
    public SubjectPermissions getSubjectPermissions(String orgId, SubjectType subjectType, String subjectId) {

        SubjectPermissions permissions = new SubjectPermissions();
        permissions.setOrgId(orgId);
        permissions.setSubjectId(subjectId);
        permissions.setSubjectType(subjectType);
        permissions.setPermissionInfos(new LinkedList<>());
        Role orgOwnerRole = roleMapper.selectOrgOwnerRole(orgId);
        Set<String> roleIds = new HashSet<>();
        if (subjectType == SubjectType.USER_ROLE) {
            Role role = roleMapper.selectPerUserRole(orgId, subjectId);
            if (role != null) {
                roleIds.add(role.getId());
                permissions.setOrgOwner(role.getId().equals(orgOwnerRole.getId()));
            }
        } else if (subjectType == SubjectType.USER) {
            List<Role> roles = roleMapper.selectUserRoles(orgId, subjectId);
            for (Role r : roles) {
                roleIds.add(r.getId());
                if (r.getId().equals(orgOwnerRole.getId())) {
                    permissions.setOrgOwner(true);
                }
            }
        } else {
            roleIds.add(subjectId);
            permissions.setOrgOwner(orgOwnerRole.getId().equals(subjectId));
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            return permissions;
        }
        List<RelRoleResource> resourceList = rrrMapper.selectByRoleIds(roleIds);

        if (CollectionUtils.isEmpty(resourceList)) {
            return permissions;
        }

        Map<String, List<RelRoleResource>> groupedResource = resourceList
                .stream()
                .collect(Collectors.groupingBy(RelRoleResource::getResourceType));
        for (String key : groupedResource.keySet()) {
            permissions.getPermissionInfos().addAll(convertPermissionInfo(groupedResource.get(key), subjectType, subjectId));
        }
        return permissions;
    }

    @Override
    public ResourcePermissions getResourcePermission(String orgId, ResourceType resourceType, String resourceId) {
        ResourcePermissions permissions = new ResourcePermissions();
        permissions.setUserPermissions(new LinkedList<>());
        permissions.setRolePermissions(new LinkedList<>());
        permissions.setResourceId(resourceId);
        permissions.setResourceType(resourceType);
        permissions.setOrgId(orgId);
        List<RelRoleResource> relRoleResources = rrrMapper.selectByResource(orgId, resourceType.name(), resourceId);
        if (CollectionUtils.isEmpty(relRoleResources)) {
            return permissions;
        }
        List<String> roleIds = relRoleResources
                .stream()
                .map(RelRoleResource::getRoleId)
                .collect(Collectors.toList());
        List<Role> roles = roleMapper.listByIds(roleIds);
        HashMap<String, Role> roleMap = new HashMap<>();
        roles.forEach(role -> roleMap.put(role.getId(), role));
        relRoleResources.forEach(rrr -> {
            PermissionInfo info = new PermissionInfo();
            info.setId(rrr.getId());
            info.setOrgId(orgId);
            info.setResourceType(resourceType);
            info.setResourceId(resourceId);
            info.setPermission(rrr.getPermission());
            Role role = roleMap.get(rrr.getRoleId());
            if (role == null) {
                return;
            }
            if (RoleType.PER_USER.name().equals(role.getType())) {
                User user = roleMapper.selectPerUserRoleUser(role.getId());
                info.setSubjectType(SubjectType.USER_ROLE);
                info.setSubjectId(user.getId());
                permissions.getUserPermissions().add(info);
            } else {
                info.setSubjectType(SubjectType.ROLE);
                info.setSubjectId(rrr.getRoleId());
                permissions.getRolePermissions().add(info);
            }
        });
        return permissions;
    }

    private List<PermissionInfo> convertPermissionInfo(List<RelRoleResource> relRoleResources, SubjectType subjectType, String subjectId) {
        return relRoleResources.stream()
                .map(rrr -> {
                    PermissionInfo info = new PermissionInfo();
                    info.setSubjectType(subjectType);
                    info.setSubjectId(subjectId);
                    info.setId(rrr.getId());
                    info.setOrgId(rrr.getOrgId());
                    info.setResourceType(ResourceType.valueOf(rrr.getResourceType()));
                    info.setResourceId(rrr.getResourceId());
                    info.setPermission(rrr.getPermission());
                    return info;
                }).collect(Collectors.toList());
    }

    @Override
    public List<ViewPermission> listRoleViewPermission(String orgId, SubjectType subjectType, String subjectId) {
        if (subjectType == SubjectType.USER) {
            Role role = roleMapper.selectPerUserRole(orgId, subjectId);
            if (role == null) {

                return Collections.emptyList();
            }
            subjectId = role.getId();
        }
        List<RelSubjectColumns> relRoleViews = rscMapper.listByRole(subjectId);
        if (CollectionUtils.isEmpty(relRoleViews)) {
            return Collections.emptyList();
        }
        return convert2ViewPermission(relRoleViews);
    }

    @Override
    public List<ViewPermission> listViewPermission(String viewId) {
        List<RelSubjectColumns> relRoleViews = rscMapper.listByView(viewId);
        if (CollectionUtils.isEmpty(relRoleViews)) {
            return Collections.emptyList();
        }
        return convert2ViewPermission(relRoleViews);
    }

    @Override
    @Transactional
    public boolean grantViewPermission(ViewPermissionParam viewPermissionParam) {

        Set<String> orgId = new HashSet<>();
        if (!CollectionUtils.isEmpty(viewPermissionParam.getPermissionToCreate())) {
            for (ViewPermission viewPermission : viewPermissionParam.getPermissionToCreate()) {
                orgId.add(viewPermission.getOrgId());
            }
        }
        if (!CollectionUtils.isEmpty(viewPermissionParam.getPermissionToUpdate())) {
            for (ViewPermission viewPermission : viewPermissionParam.getPermissionToUpdate()) {
                orgId.add(viewPermission.getOrgId());
            }
        }
        if (orgId.size() != 1) {
            Exceptions.base("The org id has to be unique");
        }
        securityManager.requireOrgOwner(orgId.stream().findAny().get());
        if (!CollectionUtils.isEmpty(viewPermissionParam.getPermissionToCreate())) {
            List<RelSubjectColumns> creates = viewPermissionParam.getPermissionToCreate()
                    .stream()
                    .map(param -> {
                        RelSubjectColumns relSubjectColumns = convert2RelRoleView(param);
                        relSubjectColumns.setCreateBy(getCurrentUser().getId());
                        relSubjectColumns.setCreateTime(new Date());
                        return relSubjectColumns;
                    }).collect(Collectors.toList());

            rscMapper.batchInsert(creates);
        }

        if (!CollectionUtils.isEmpty(viewPermissionParam.getPermissionToUpdate())) {
            List<RelSubjectColumns> updates = viewPermissionParam.getPermissionToUpdate()
                    .stream()
                    .map(param -> {
                        RelSubjectColumns relSubjectColumns = convert2RelRoleView(param);
                        relSubjectColumns.setUpdateBy(getCurrentUser().getId());
                        relSubjectColumns.setUpdateTime(new Date());
                        return relSubjectColumns;
                    }).collect(Collectors.toList());
            rscMapper.batchUpdate(updates);
        }
        if (!CollectionUtils.isEmpty(viewPermissionParam.getPermissionToDelete())) {
            rscMapper.batchDelete(viewPermissionParam.getPermissionToDelete());
        }
        return true;

    }

    private List<ViewPermission> convert2ViewPermission(List<RelSubjectColumns> relRoleViews) {

        if (CollectionUtils.isEmpty(relRoleViews)) {
            return Collections.emptyList();
        }
        return relRoleViews.stream().map(rel -> {
            ViewPermission viewPermission = new ViewPermission();
            BeanUtils.copyProperties(rel, viewPermission);
            Role role = retrieve(rel.getSubjectId());
            if (RoleType.PER_USER.name().equals(role.getType())) {
                viewPermission.setSubjectType(SubjectType.USER);
                User user = roleMapper.selectPerUserRoleUser(role.getId());
                viewPermission.setSubjectName(user.getUsername());
                viewPermission.setSubjectId(user.getId());
            } else {
                viewPermission.setSubjectType(SubjectType.ROLE);
                viewPermission.setSubjectName(role.getName());
                viewPermission.setSubjectId(role.getId());
            }
            return viewPermission;
        }).collect(Collectors.toList());
    }

    private RelSubjectColumns convert2RelRoleView(ViewPermission permission) {
        View view = retrieve(permission.getViewId(), View.class);
        RelSubjectColumns relSubjectColumns = new RelSubjectColumns();
        BeanUtils.copyProperties(permission, relSubjectColumns);
        if (relSubjectColumns.getId() == null) {
            relSubjectColumns.setId(UUIDGenerator.generate());
        }
        if (permission.getSubjectType() == SubjectType.USER) {
            Role role = roleMapper.selectPerUserRole(view.getOrgId(), permission.getSubjectId());
            relSubjectColumns.setSubjectId(role.getId());
        } else {
            relSubjectColumns.setSubjectId(permission.getSubjectId());
        }
        requireExists(relSubjectColumns.getSubjectId());
        return relSubjectColumns;
    }

    @Override
    public void requirePermission(Role entity, int permission) {
        securityManager.requireAllPermissions(PermissionHelper.rolePermission(entity.getOrgId(), permission));
    }
}

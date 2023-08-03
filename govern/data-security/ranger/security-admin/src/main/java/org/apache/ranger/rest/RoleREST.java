/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.RoleDBStore;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.ServiceUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPluginInfo;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.validation.RangerRoleValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.GrantRevokeRoleRequest;
import org.apache.ranger.plugin.util.RangerRESTUtils;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerRoleService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.view.RangerRoleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Path("roles")
@Component
@Scope("request")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class RoleREST {
    private static final Logger LOG = LoggerFactory.getLogger(RoleREST.class);

    private static List<String> INVALID_USERS = new ArrayList<>();

    public static final String POLICY_DOWNLOAD_USERS = "policy.download.auth.users";

    @Autowired
    RESTErrorUtil restErrorUtil;

    @Autowired
    AssetMgr assetMgr;

    @Autowired
    RangerDaoManager daoManager;

    @Autowired
    RoleDBStore roleStore;

    @Autowired
    RangerRoleService roleService;

    @Autowired
    XUserService xUserService;

    @Autowired
    ServiceDBStore svcStore;

    @Autowired
    RangerSearchUtil searchUtil;

    @Autowired
    ServiceUtil serviceUtil;

    @Autowired
    RangerValidatorFactory validatorFactory;

    @Autowired
    RangerBizUtil bizUtil;

    @Autowired
    XUserMgr userMgr;

    static {
        INVALID_USERS.add(RangerPolicyEngine.USER_CURRENT);
        INVALID_USERS.add(RangerPolicyEngine.RESOURCE_OWNER);
    }

    /* This operation is allowed only when effective User has ranger admin privilege
     * if execUser is not same as logged-in user then effective user is execUser
     * else  effective user is logged-in user.
     * This logic is implemented as part of ensureAdminAccess(String serviceName, String userName);
     */

    @POST
    @Path("/roles")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerRole createRole(@QueryParam("serviceName") String serviceName,  RangerRole role
           , @DefaultValue("false") @QueryParam("createNonExistUserGroup") Boolean createNonExistUserGroup
           ) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> createRole("+ role + ")");
        }

        RangerRole ret;
        try {
            RangerRoleValidator validator = validatorFactory.getRangerRoleValidator(roleStore);
            validator.validate(role, RangerValidator.Action.CREATE);

            String userName = role.getCreatedByUser();
            ensureAdminAccess(serviceName, userName);
            if (containsInvalidMember(role.getUsers())) {
                throw new Exception("Invalid role user(s)");
            }
            ret = roleStore.createRole(role, createNonExistUserGroup);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("createRole(" + role + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== createRole("+ role + "):" +  ret);
        }
        return ret;
    }

    /* This operation is allowed only when -
     * Logged in user has ranger admin role
     */

    @PUT
    @Path("/roles/{id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerRole updateRole(@PathParam("id") Long roleId
                                , RangerRole role
                                , @DefaultValue("false") @QueryParam("createNonExistUserGroup") Boolean createNonExistUserGroup
                                ) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> updateRole(id=" + roleId +", " + role + ")");
        }

        if (role.getId() != null && !roleId.equals(role.getId())) {
            throw restErrorUtil.createRESTException("roleId mismatch!!");
        } else {
            role.setId(roleId);
        }
        RangerRole ret;
        try {
            UserSessionBase usb          = ContextUtil.getCurrentUserSession();
            String          loggedInUser = usb != null ? usb.getLoginId() : null;
            RangerRole      existingRole = getRole(roleId);

            if (!bizUtil.isUserRangerAdmin(loggedInUser) && !ensureRoleAccess(loggedInUser, userMgr.getGroupsForUser(loggedInUser), existingRole)) {
                LOG.error("User " + loggedInUser + " does not have permission for this operation");

                throw new Exception("User does not have permission for this operation");
            }

            RangerRoleValidator validator = validatorFactory.getRangerRoleValidator(roleStore);
            validator.validate(role, RangerValidator.Action.UPDATE);

            if (containsInvalidMember(role.getUsers())) {
                throw new Exception("Invalid role user(s)");
            }
            ret = roleStore.updateRole(role, createNonExistUserGroup);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("updateRole(" + role + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== updateRole(id=" + roleId +", " + role + "):" + ret);
        }
        return ret;
    }

    /* This operation is allowed only when effective User has ranger admin privilege
     * if execUser is not same as logged-in user then effective user is execUser
     * else  effective user is logged-in user.
     * This logic is implemented as part of ensureAdminAccess(String serviceName, String userName);
     */

    @DELETE
    @Path("/roles/name/{name}")
    public void deleteRole(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String execUser, @PathParam("name") String roleName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> deleteRole(user=" + execUser + " name=" + roleName + ")");
        }
        try {
            RangerRoleValidator validator = validatorFactory.getRangerRoleValidator(roleStore);
            validator.validate(roleName, RangerRoleValidator.Action.DELETE);

            ensureAdminAccess(serviceName, execUser);
            roleStore.deleteRole(roleName);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("deleteRole(" + roleName + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== deleteRole(name=" + roleName + ")");
        }
    }

    /* This operation is allowed only when -
     * Logged in user has ranger admin role
     */

    @DELETE
    @Path("/roles/{id}")
    public void deleteRole(@PathParam("id") Long roleId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> deleteRole(id=" + roleId + ")");
        }
        try {
            RangerRoleValidator validator = validatorFactory.getRangerRoleValidator(roleStore);
            validator.validate(roleId, RangerRoleValidator.Action.DELETE);

            ensureAdminAccess(null, null);
            roleStore.deleteRole(roleId);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("deleteRole(" + roleId + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== deleteRole(id=" + roleId + ")");
        }
    }

    /*
     * Minimum required privilege is the effective user has admin option for this role.
     * This is used to list all the roles, groups, and users who belong to this role.
     */

    @GET
    @Path("/roles/name/{name}")
    @Produces({ "application/json" })
    public RangerRole getRole(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String execUser, @PathParam("name") String roleName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getRole(name=" + roleName + ")");
        }
        RangerRole ret;
        try {
            ret = getRoleIfAccessible(roleName, serviceName, execUser, userMgr.getGroupsForUser(execUser));
            if (ret == null) {
                throw restErrorUtil.createRESTException("User doesn't have permissions to get details for " + roleName);
            }

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getRole(" + roleName + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getRole(name=" + roleName + "):" + ret);
        }
        return ret;
    }

    @GET
    @Path("/roles/{id}")
    @Produces({ "application/json" })
    public RangerRole getRole(@PathParam("id") Long id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getRole(id=" + id + ")");
        }
        RangerRole ret;
        try {
            ret = roleStore.getRole(id);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getRole(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getRole(id=" + id + "):" + ret);
        }
        return ret;
    }

    /* This operation is allowed only when effective User has ranger admin or auditor privilege
     * if execUser is not same as logged-in user then effective user is execUser
     * else  effective user is logged-in user.
     * This logic is implemented as part of ensureAdminAccess(String serviceName, String userName);
     */

    @GET
    @Path("/roles")
    @Produces({ "application/json" })
    public RangerRoleList getAllRoles(@Context HttpServletRequest request) {
        RangerRoleList ret = new RangerRoleList();
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getAllRoles()");
        }
        SearchFilter filter = searchUtil.getSearchFilter(request, roleService.sortFields);
        try {
            roleStore.getRoles(filter,ret);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getAllRoles():" + ret);
        }
        return ret;
    }

    @GET
    @Path("/lookup/roles")
    @Produces({ "application/json" })
    public RangerRoleList getAllRolesForUser(@Context HttpServletRequest request) {
        RangerRoleList ret = new RangerRoleList();
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getAllRolesForUser()");
        }
        SearchFilter filter = searchUtil.getSearchFilter(request, roleService.sortFields);
        try {
            roleStore.getRolesForUser(filter,ret);
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getAllRoles():" + ret);
        }
        return ret;
    }

    /* This operation is allowed only when effective User has ranger admin privilege
     * if execUser is not same as logged-in user then effective user is execUser
     * else  effective user is logged-in user.
     * This logic is implemented as part of ensureAdminAccess(String serviceName, String userName);
     */

    @GET
    @Path("/roles/names")
    @Produces({ "application/json" })
    public List<String> getAllRoleNames(@QueryParam("serviceName") String serviceName, @QueryParam("execUser") String userName, @Context HttpServletRequest request) {
        final List<String> ret;
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getAllRoleNames()");
        }
        SearchFilter filter = searchUtil.getSearchFilter(request, roleService.sortFields);
        try {
            ensureAdminAccess(serviceName, userName);
            ret = roleStore.getRoleNames(filter);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getAllRoleNames() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getAllRoleNames():" + ret);
        }
        return ret;
    }

    /*
        This API is used to add users and groups with/without GRANT privileges to this Role. It follows add-or-update semantics
     */
    @PUT
    @Path("/roles/{id}/addUsersAndGroups")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerRole addUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups, Boolean isAdmin) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ", isAdmin=" + isAdmin + ")");
        }

        RangerRole role;

        try {
            // Real processing
            ensureAdminAccess(null, null);
            if (containsInvalidUser(users)) {
                throw new Exception("Invalid role user(s)");
            }

            role = getRole(roleId);

            Set<RangerRole.RoleMember> roleUsers = new HashSet<>();
            Set<RangerRole.RoleMember> roleGroups = new HashSet<>();

            for (RangerRole.RoleMember user : role.getUsers()) {
                if (users.contains(user.getName()) && isAdmin == Boolean.TRUE) {
                    user.setIsAdmin(isAdmin);
                    roleUsers.add(user);
                }
            }
            Set<String> existingUsernames = getUserNames(role);
            for (String user : users) {
                if (!existingUsernames.contains(user)) {
                    roleUsers.add(new RangerRole.RoleMember(user, isAdmin));
                }
            }

            for (RangerRole.RoleMember group : role.getGroups()) {
                if (group.getIsAdmin() == isAdmin) {
                    roleGroups.add(group);
                }
            }
            for (String group : groups) {
                roleGroups.add(new RangerRole.RoleMember(group, isAdmin));
            }
            role.setUsers(new ArrayList<>(roleUsers));
            role.setGroups(new ArrayList<>(roleGroups));

            role = roleStore.updateRole(role,false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("addUsersAndGroups() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ", isAdmin=" + isAdmin + ")");
        }

        return role;
    }

    /*
        This API is used to remove users and groups, without regard to their GRANT privilege, from this Role.
     */
    @PUT
    @Path("/roles/{id}/removeUsersAndGroups")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerRole removeUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ")");
        }
        RangerRole role;

        try {
            // Real processing
            ensureAdminAccess(null, null);
            role = getRole(roleId);

            for (String user : users) {
                Iterator<RangerRole.RoleMember> iter = role.getUsers().iterator();
                while (iter.hasNext()) {
                    RangerRole.RoleMember member = iter.next();
                    if (StringUtils.equals(member.getName(), user)) {
                        iter.remove();
                        break;
                    }
                }
            }
            for (String group : groups) {
                Iterator<RangerRole.RoleMember> iter = role.getGroups().iterator();
                while (iter.hasNext()) {
                    RangerRole.RoleMember member = iter.next();
                    if (StringUtils.equals(member.getName(), group)) {
                        iter.remove();
                        break;
                    }
                }
            }

            role = roleStore.updateRole(role, false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("removeUsersAndGroups() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== removeUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ")");
        }

        return role;
    }

    /*
        This API is used to remove GRANT privilege from listed users and groups.
     */
    @PUT
    @Path("/roles/{id}/removeAdminFromUsersAndGroups")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RangerRole removeAdminFromUsersAndGroups(@PathParam("id") Long roleId, List<String> users, List<String> groups) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeAdminFromUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ")");
        }
        RangerRole role;
        try {
            // Real processing
            ensureAdminAccess(null, null);
            role = getRole(roleId);

            for (String user : users) {
                for (RangerRole.RoleMember member : role.getUsers()) {
                    if (StringUtils.equals(member.getName(), user) && member.getIsAdmin()) {
                        member.setIsAdmin(false);
                    }
                }
            }
            for (String group : groups) {
                for (RangerRole.RoleMember member : role.getGroups()) {
                    if (StringUtils.equals(member.getName(), group) && member.getIsAdmin()) {
                        member.setIsAdmin(false);
                    }
                }
            }

            role = roleStore.updateRole(role, false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("removeAdminFromUsersAndGroups() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeAdminFromUsersAndGroups(id=" + roleId + ", users=" + Arrays.toString(users.toArray()) + ", groups=" + Arrays.toString(groups.toArray()) + ")");
        }

        return role;
    }

    /*
     * This API is used to GRANT role to users and roles with/without ADMIN option. It follows add-or-update semantics
     * Minimum required privilege is the effective user has admin option for the target roles
     */

    @PUT
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/roles/grant/{serviceName}")
    public RESTResponse grantRole(@PathParam("serviceName") String serviceName, GrantRevokeRoleRequest grantRoleRequest, @Context HttpServletRequest request) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RoleREST.grantRole(" + serviceName + ", " + grantRoleRequest + ")");
        }
        RESTResponse     ret  = new RESTResponse();

        try {
            validateUsersGroupsAndRoles(grantRoleRequest);
            String userName = grantRoleRequest.getGrantor();
            for (String roleName : grantRoleRequest.getTargetRoles()) {
                /* For each target Role, check following to allow access
                 * If userName (execUser) is not same as logged in user then check
                    * If logged-in user is not ranger admin/service admin/service user, then deny the operation
                    * effective User is execUser
                 * else
                    * effective user is logged-in user
                 * If effective user is ranger admin/has role admin privilege, then allow the operation
                 * else deny the operation
                 * This logic is implemented as part of getRoleIfAccessible(roleName, serviceName, userName, userGroups)
                */
                Set<String>          userGroups = CollectionUtils.isNotEmpty(grantRoleRequest.getGrantorGroups()) ? grantRoleRequest.getGrantorGroups() : userMgr.getGroupsForUser(userName);
                RangerRole existingRole = getRoleIfAccessible(roleName, serviceName, userName, userGroups);
                if (existingRole == null) {
                    throw restErrorUtil.createRESTException("User doesn't have permissions to grant role " + roleName);
                }

                existingRole.setUpdatedBy(userName);
                addUsersGroupsAndRoles(existingRole, grantRoleRequest.getUsers(), grantRoleRequest.getGroups(), grantRoleRequest.getRoles(), grantRoleRequest.getGrantOption());
            }
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("grantRole() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> grantRole(serviceName=" + serviceName + ", users=" + Arrays.toString(grantRoleRequest.getUsers().toArray()) + ", groups=" + Arrays.toString(grantRoleRequest.getRoles().toArray()) + ", isAdmin=" + grantRoleRequest.getGrantOption() + ")");
        }
        ret.setStatusCode(RESTResponse.STATUS_SUCCESS);

        return ret;
    }

    /*
     * This API is used to remove users and roles, with regard to their REVOKE role from users and roles.
     * Minimum required privilege is the execUser (or doAsUser) has admin option for the target roles
     */

    @PUT
    @Path("/roles/revoke/{serviceName}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public RESTResponse revokeRole(@PathParam("serviceName") String serviceName, GrantRevokeRoleRequest revokeRoleRequest, @Context HttpServletRequest request) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RoleREST.revokeRole(" + serviceName + ", " + revokeRoleRequest + ")");
        }
        RESTResponse     ret  = new RESTResponse();

        try {
            validateUsersGroupsAndRoles(revokeRoleRequest);
            String userName = revokeRoleRequest.getGrantor();
            for (String roleName : revokeRoleRequest.getTargetRoles()) {
                /* For each target Role, check following to allow access
                 * If userName (execUser) is not same as logged in user then check
                    * If logged-in user is not ranger admin/service admin/service user, then deny the operation
                    * effective User is execUser
                 * else
                    * effective user is logged-in user
                 * If effective user is ranger admin/has role admin privilege, then allow the operation
                 * else deny the operation
                 * This logic is implemented as part of getRoleIfAccessible(roleName, serviceName, userName, userGroups)
                 */
                Set<String>          userGroups = CollectionUtils.isNotEmpty(revokeRoleRequest.getGrantorGroups()) ? revokeRoleRequest.getGrantorGroups() : userMgr.getGroupsForUser(userName);
                RangerRole existingRole = getRoleIfAccessible(roleName, serviceName, userName, userGroups);
                if (existingRole == null) {
                    throw restErrorUtil.createRESTException("User doesn't have permissions to revoke role " + roleName);
                }
                existingRole.setUpdatedBy(userName);

                if (revokeRoleRequest.getGrantOption()) {
                    removeAdminFromUsersGroupsAndRoles(existingRole, revokeRoleRequest.getUsers(), revokeRoleRequest.getGroups(), revokeRoleRequest.getRoles());
                } else {
                    removeUsersGroupsAndRoles(existingRole, revokeRoleRequest.getUsers(), revokeRoleRequest.getGroups(), revokeRoleRequest.getRoles());
                }
            }
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("revokeRole() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> revokeRole(serviceName=" + serviceName + ", users=" + Arrays.toString(revokeRoleRequest.getUsers().toArray()) + ", roles=" + Arrays.toString(revokeRoleRequest.getRoles().toArray()) + ", isAdmin=" + revokeRoleRequest.getGrantOption() + ")");
        }
        ret.setStatusCode(RESTResponse.STATUS_SUCCESS);

        return ret;
    }

    /* Get all the roles that this user or user's groups belong to
     */

    @GET
    @Path("/roles/user/{user}")
    @Produces({ "application/json" })
    public List<String> getUserRoles(@PathParam("user") String userName, @Context HttpServletRequest request) {
        Set<String> ret = new HashSet<>();
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getUserRoles()");
        }
        try {
            if (xUserService.getXUserByUserName(userName) == null) {
                throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "User:" + userName + " not found", false);
            }
            Set<RangerRole> roleList = roleStore.getRoleNames(userName, userMgr.getGroupsForUser(userName));
            for (RangerRole role : roleList) {
                ret.add(role.getName());
                Set<String> roleMembers = new HashSet<>();
                getRoleMemberNames(roleMembers, role);
                ret.addAll(roleMembers);
            }
        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("getUserRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getUserRoles():" + ret);
        }
        return new ArrayList<>(ret);
    }

    @GET
    @Path("/download/{serviceName}")
    @Produces({ "application/json" })
    public RangerRoles getRangerRolesIfUpdated(
            @PathParam("serviceName") String serviceName,
            @DefaultValue("-1") @QueryParam("lastKnownRoleVersion") Long lastKnownRoleVersion,
            @DefaultValue("0") @QueryParam("lastActivationTime") Long lastActivationTime,
            @QueryParam("pluginId") String pluginId,
            @DefaultValue("") @QueryParam("clusterName") String clusterName,
            @DefaultValue("") @QueryParam(RangerRESTUtils.REST_PARAM_CAPABILITIES) String pluginCapabilities,
            @Context HttpServletRequest request) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RoleREST.getRangerRolesIfUpdated("
                    + serviceName + ", " + lastKnownRoleVersion + ", " + lastActivationTime + ")");
        }
        RangerRoles ret = null;

        boolean isValid           = false;
        int     httpCode          = HttpServletResponse.SC_OK;
        Long    downloadedVersion = null;
        String  logMsg            = null;

        try {
            bizUtil.failUnauthenticatedDownloadIfNotAllowed();
            isValid = serviceUtil.isValidService(serviceName, request);
        } catch (WebApplicationException webException) {
            httpCode = webException.getResponse().getStatus();
            logMsg = webException.getResponse().getEntity().toString();
        } catch (Exception e) {
            httpCode = HttpServletResponse.SC_BAD_REQUEST;
            logMsg = e.getMessage();
        }
        if (isValid) {
            try {
                RangerRoles roles = roleStore.getRoles(serviceName, lastKnownRoleVersion);
                if (roles == null) {
                    downloadedVersion = lastKnownRoleVersion;
                    httpCode = HttpServletResponse.SC_NOT_MODIFIED;
                    logMsg = "No change since last update";
                } else {
                    downloadedVersion = roles.getRoleVersion();
                    roles.setServiceName(serviceName);
                    ret = roles;
                    httpCode = HttpServletResponse.SC_OK;
                    logMsg = "Returning RangerRoles =>" + (ret.toString());
                }

            } catch (Throwable excp) {
                LOG.error("getRangerRolesIfUpdated(" + serviceName + ", " + lastKnownRoleVersion + ", " + lastActivationTime + ") failed", excp);
                httpCode = HttpServletResponse.SC_BAD_REQUEST;
                logMsg = excp.getMessage();
            }
        }

        assetMgr.createPluginInfo(serviceName, pluginId, request, RangerPluginInfo.ENTITY_TYPE_ROLES, downloadedVersion, lastKnownRoleVersion, lastActivationTime, httpCode, clusterName, pluginCapabilities);

        if (httpCode != HttpServletResponse.SC_OK) {
            boolean logError = httpCode != HttpServletResponse.SC_NOT_MODIFIED;
            throw restErrorUtil.createRESTException(httpCode, logMsg, logError);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RoleREST.getRangerRolesIfUpdated(" + serviceName + ", " + lastKnownRoleVersion + ", " + lastActivationTime + ")" + ret);
        }
        return ret;
    }

    @GET
    @Path("/secure/download/{serviceName}")
    @Produces({ "application/json" })
    public RangerRoles getSecureRangerRolesIfUpdated(
            @PathParam("serviceName") String serviceName,
            @DefaultValue("-1") @QueryParam("lastKnownRoleVersion") Long lastKnownRoleVersion,
            @DefaultValue("0") @QueryParam("lastActivationTime") Long lastActivationTime,
            @QueryParam("pluginId") String pluginId,
            @DefaultValue("") @QueryParam("clusterName") String clusterName,
            @DefaultValue("") @QueryParam(RangerRESTUtils.REST_PARAM_CAPABILITIES) String pluginCapabilities,
            @Context HttpServletRequest request) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RoleREST.getSecureRangerRolesIfUpdated("
                    + serviceName + ", " + lastKnownRoleVersion + ", " + lastKnownRoleVersion + ")");
        }
        RangerRoles ret  = null;
        int     httpCode          = HttpServletResponse.SC_OK;
        String  logMsg            = null;
        boolean isAllowed         = false;
        boolean isAdmin           = bizUtil.isAdmin();
        boolean isKeyAdmin        = bizUtil.isKeyAdmin();
        Long    downloadedVersion = null;

        request.setAttribute("downloadPolicy", "secure");

        boolean isValid = false;
        try {
            isValid = serviceUtil.isValidService(serviceName, request);
        } catch (WebApplicationException webException) {
            httpCode = webException.getResponse().getStatus();
            logMsg = webException.getResponse().getEntity().toString();
        } catch (Exception e) {
            httpCode = HttpServletResponse.SC_BAD_REQUEST;
            logMsg = e.getMessage();
        }
        if (isValid) {
            try {
                XXService xService = daoManager.getXXService().findByName(serviceName);
                if (xService == null) {
                    LOG.error("Requested Service not found. serviceName=" + serviceName);
                    throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Service:" + serviceName + " not found",
                            false);
                }
                XXServiceDef xServiceDef = daoManager.getXXServiceDef().getById(xService.getType());
                RangerService rangerService = svcStore.getServiceByName(serviceName);

                if (StringUtils.equals(xServiceDef.getImplclassname(), EmbeddedServiceDefsUtil.KMS_IMPL_CLASS_NAME)) {
                    if (isKeyAdmin) {
                        isAllowed = true;
                    }else {
                        isAllowed = bizUtil.isUserAllowed(rangerService, POLICY_DOWNLOAD_USERS);
                    }
                }else{
                    if (isAdmin) {
                        isAllowed = true;
                    }else{
                        isAllowed = bizUtil.isUserAllowed(rangerService, POLICY_DOWNLOAD_USERS);
                    }
                }

                if (isAllowed) {
                    RangerRoles roles = roleStore.getRoles(serviceName, lastKnownRoleVersion);
                    if (roles == null) {
                        downloadedVersion = lastKnownRoleVersion;
                        httpCode = HttpServletResponse.SC_NOT_MODIFIED;
                        logMsg = "No change since last update";
                    } else {
                        downloadedVersion = roles.getRoleVersion();
                        roles.setServiceName(serviceName);
                        ret = roles;
                        httpCode = HttpServletResponse.SC_OK;
                        logMsg = "Returning RangerRoles =>" + (ret.toString());
                    }
                } else {
                    LOG.error("getSecureRangerRolesIfUpdated(" + serviceName + ", " + lastKnownRoleVersion + ") failed as User doesn't have permission to UserGroupRoles");
                    httpCode = HttpServletResponse.SC_FORBIDDEN; // assert user is authenticated.
                    logMsg = "User doesn't have permission to download UserGroupRoles";
                }

            } catch (Throwable excp) {
                LOG.error("getSecureRangerRolesIfUpdated(" + serviceName + ", " + lastKnownRoleVersion + ", " + lastActivationTime + ") failed", excp);
                httpCode = HttpServletResponse.SC_BAD_REQUEST;
                logMsg = excp.getMessage();
            }
        }

        assetMgr.createPluginInfo(serviceName, pluginId, request, RangerPluginInfo.ENTITY_TYPE_ROLES, downloadedVersion, lastKnownRoleVersion, lastActivationTime, httpCode, clusterName, pluginCapabilities);

        if (httpCode != HttpServletResponse.SC_OK) {
            boolean logError = httpCode != HttpServletResponse.SC_NOT_MODIFIED;
            throw restErrorUtil.createRESTException(httpCode, logMsg, logError);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RoleREST.getSecureRangerRolesIfUpdated(" + serviceName + ", " + lastKnownRoleVersion + ", " + lastActivationTime + ")" + ret);
        }
        return ret;
    }

    private void ensureAdminAccess(String serviceName, String userName) throws Exception {

        /* If userName (execUser) is not same as logged in user then check
            * If logged-in user is not ranger admin/service admin/service user, then deny the operation
            * effective User is execUser
         * else
            * effective user is logged-in user
         * If effective user is ranger admin, then allow the operation
         * else deny the operation
         */
        String effectiveUser;
        UserSessionBase usb = ContextUtil.getCurrentUserSession();
        String loggedInUser = usb != null ? usb.getLoginId() : null;
        if (!StringUtil.equals(userName, loggedInUser)) {
            if (!bizUtil.isUserRangerAdmin(loggedInUser) && !userIsSrvAdmOrSrvUser(serviceName, loggedInUser)) {
                throw new Exception("User does not have permission for this operation");
            }
            effectiveUser = userName != null ? userName : loggedInUser;
        } else {
            effectiveUser = loggedInUser;
        }

        if (!bizUtil.isUserRangerAdmin(effectiveUser) && !svcStore.isServiceAdminUser(serviceName, effectiveUser)) {
            throw new Exception("User " + effectiveUser + " does not have permission for this operation");
        }
    }

    private RangerRole getRoleIfAccessible(String roleName, String serviceName, String userName, Set<String> userGroups) {
        /* If userName (execUser) is not same as logged in user then check
            * If logged-in user is not ranger admin/service admin/service user, then deny the operation
                * effective User is execUser
         * else
            * effective user is logged-in user
         * If effective user is ranger admin/has role admin privilege, then allow the operation
         * else deny the operation
         */
        RangerRole existingRole;
        String effectiveUser;
        UserSessionBase usb = ContextUtil.getCurrentUserSession();
        String loggedInUser = usb != null ? usb.getLoginId() : null;
        if (!StringUtil.equals(userName, loggedInUser)) {
            if (!bizUtil.isUserRangerAdmin(loggedInUser) && !userIsSrvAdmOrSrvUser(serviceName, loggedInUser)) {
                LOG.error("User does not have permission for this operation");
                return null;
            }
            effectiveUser = userName != null ? userName : loggedInUser;
        } else {
            effectiveUser = loggedInUser;
        }
        try {
            if (!bizUtil.isUserRangerAdmin(effectiveUser) && !svcStore.isServiceAdminUser(serviceName, effectiveUser)) {
                existingRole = roleStore.getRole(roleName);
                ensureRoleAccess(effectiveUser, userGroups, existingRole);

            } else {
                existingRole = roleStore.getRole(roleName);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return null;
        }

        return existingRole;
    }

    private boolean userIsSrvAdmOrSrvUser(String serviceName, String username) {
        boolean isServiceAdmin = false;

        if (!StringUtil.isEmpty(serviceName)) {
            try {
                isServiceAdmin = svcStore.isServiceAdminUser(serviceName, username);
                if (!isServiceAdmin) {
                    RangerService rangerService = svcStore.getServiceByName(serviceName);
                    if (rangerService != null) {
                        String serviceUser = PropertiesUtil.getProperty("ranger.plugins." + rangerService.getType() + ".serviceuser");

                        isServiceAdmin = StringUtil.equals(serviceUser, username);
                    }
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
            }
        }
        return isServiceAdmin;
    }

    private boolean containsInvalidMember(List<RangerRole.RoleMember> users) {
        boolean ret = false;
        for (RangerRole.RoleMember user : users) {
            for (String invalidUser : INVALID_USERS) {
                if (StringUtils.equals(user.getName(), invalidUser)) {
                    ret = true;
                    break;
                }
            }
            if (ret) {
                break;
            }
        }
        return ret;
    }

    private boolean containsInvalidUser(List<String> users) {
        return CollectionUtils.isNotEmpty(users) && CollectionUtils.containsAny(users, INVALID_USERS);
    }

    private boolean ensureRoleAccess(String username, Set<String> userGroups, RangerRole role) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> ensureRoleAccess("+ username + ", " + role + ")");
        }
        boolean isAccessible = false;
        List<RangerRole.RoleMember> userList = role.getUsers();
        RangerRole.RoleMember userMember = new RangerRole.RoleMember(username, true);

        if (!CollectionUtils.isEmpty(userList) && userList.contains(userMember))  {
            isAccessible = true;
            if (LOG.isDebugEnabled()) {
                LOG.debug("==> ensureRoleAccess(): user "+ username + " has permission for role " + role.getName());
            }
            return isAccessible;
        }

        if (!CollectionUtils.isEmpty(userGroups)) {
            List<RangerRole.RoleMember> groupList = role.getGroups();
            for (RangerRole.RoleMember groupMember : groupList) {
                if (!groupMember.getIsAdmin()) {
                    continue;
                }
                if (userGroups.contains(groupMember.getName())) {
                    isAccessible = true;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("==> ensureRoleAccess(): group " + groupMember.getName() + " has permission for role " + role.getName());
                    }
                    return isAccessible;
                }
            }
        }

        Set<RangerRole.RoleMember> roleMemberList = new HashSet<>();
        getRoleMembers(roleMemberList, role);
        for (RangerRole.RoleMember roleMember : roleMemberList) {
            if (!roleMember.getIsAdmin()) {
                continue;
            }

            RangerRole roleMemberObj = roleStore.getRole(roleMember.getName());
            if (getUserNames(roleMemberObj).contains(username)) {
                isAccessible = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("==> ensureRoleAccess(): role "+ roleMember.getName() + " has permission for role " + role.getName());
                }
                return isAccessible;
            }

            if (!CollectionUtils.isEmpty(userGroups) && !CollectionUtils.intersection(userGroups, getGroupNames(roleMemberObj)).isEmpty()) {
                isAccessible = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("==> ensureRoleAccess(): role " + roleMember.getName() + " has permission for role " + role.getName());
                }
                return isAccessible;
            }
        }
        if (!isAccessible) {
            throw restErrorUtil.createRESTException("User " + username + " does not have privilege to role " + role.getName());
        }
        return isAccessible;
    }

    private RangerRole addUsersGroupsAndRoles(RangerRole role, Set<String> users, Set<String> groups, Set<String> roles, Boolean isAdmin) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addUsersGroupsAndRoles(name=" + role.getName() + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ", isAdmin=" + isAdmin + ")");
        }

        try {
            // Check for any cycling relationship between roles
            for (String newRole : roles) {
                //get members recursively and check if the grantor role is already a member
                Set<String> roleMembers = new HashSet<>();
                getRoleMemberNames(roleMembers, roleStore.getRole(newRole));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Role members for " + newRole + " = " + roleMembers);
                }
                if (roleMembers.contains(role.getName())) {
                    throw new Exception("Invalid role grant");
                }

            }

            Set<RangerRole.RoleMember> roleUsers = new HashSet<>();
            Set<RangerRole.RoleMember> roleGroups = new HashSet<>();
            Set<RangerRole.RoleMember> roleRoles = new HashSet<>();

			for (RangerRole.RoleMember user : role.getUsers()) {
				String userName = user.getName();
				if (users.contains(userName)) {
					user.setIsAdmin(isAdmin);
				}
				roleUsers.add(user);
			}

            Set<String> existingUsernames = getUserNames(role);
            for (String user : users) {
                if (!existingUsernames.contains(user)) {
                    roleUsers.add(new RangerRole.RoleMember(user, isAdmin));
                }
            }

			for (RangerRole.RoleMember group : role.getGroups()) {
				String groupName = group.getName();
				if (groups.contains(groupName)) {
					group.setIsAdmin(isAdmin);
				}
				roleGroups.add(group);
			}

            Set<String> existingGroupnames = getGroupNames(role);
            for (String group : groups) {
                if (!existingGroupnames.contains(group)) {
                    roleGroups.add(new RangerRole.RoleMember(group, isAdmin));
                }
            }

			for (RangerRole.RoleMember roleMember : role.getRoles()) {
				String roleName = roleMember.getName();
				if (roles.contains(roleName)) {
					roleMember.setIsAdmin(isAdmin);
				}
				roleRoles.add(roleMember);
			}

            Set<String> existingRolenames = getRoleNames(role);
            for (String newRole : roles) {
                if (!existingRolenames.contains(newRole)) {
                    roleRoles.add(new RangerRole.RoleMember(newRole, isAdmin));
                }
            }
            role.setUsers(new ArrayList<>(roleUsers));
            role.setGroups(new ArrayList<>(roleGroups));
            role.setRoles(new ArrayList<>(roleRoles));

            role = roleStore.updateRole(role, false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("addUsersGroupsAndRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addUsersGroupsAndRoles(name=" + role.getName() + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ", isAdmin=" + isAdmin + ")");
        }

        return role;
    }

    private RangerRole removeUsersGroupsAndRoles(RangerRole role, Set<String> users, Set<String> groups, Set<String> roles) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeUsersGroupsAndRoles(name=" + role.getName() + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ")");
        }

        try {
            // Real processing
            for (String user : users) {
                Iterator<RangerRole.RoleMember> iter = role.getUsers().iterator();
                while (iter.hasNext()) {
                    RangerRole.RoleMember member = iter.next();
                    if (StringUtils.equals(member.getName(), user)) {
                        iter.remove();
                        break;
                    }
                }
            }

            for (String group : groups) {
                Iterator<RangerRole.RoleMember> iter = role.getGroups().iterator();
                while (iter.hasNext()) {
                    RangerRole.RoleMember member = iter.next();
                    if (StringUtils.equals(member.getName(), group)) {
                        iter.remove();
                        break;
                    }
                }
            }

            for (String newRole : roles) {
                Iterator<RangerRole.RoleMember> iter = role.getRoles().iterator();
                while (iter.hasNext()) {
                    RangerRole.RoleMember member = iter.next();
                    if (StringUtils.equals(member.getName(), newRole)) {
                        iter.remove();
                        break;
                    }
                }
            }

            role = roleStore.updateRole(role, false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("removeUsersGroupsAndRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== removeUsersGroupsAndRoles(name=" + role.getName() + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ")");
        }

        return role;
    }

    private RangerRole removeAdminFromUsersGroupsAndRoles(RangerRole role, Set<String> users, Set<String> groups, Set<String> roles) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeAdminFromUsersGroupsAndRoles(name=" + role + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ")");
        }
        try {
            // Real processing
            for (String user : users) {
                for (RangerRole.RoleMember member : role.getUsers()) {
                    if (StringUtils.equals(member.getName(), user) && member.getIsAdmin()) {
                        member.setIsAdmin(false);
                    }
                }
            }
            for (String group : groups) {
                for (RangerRole.RoleMember member : role.getGroups()) {
                    if (StringUtils.equals(member.getName(), group) && member.getIsAdmin()) {
                        member.setIsAdmin(false);
                    }
                }
            }

            for (String newRole : roles) {
                for (RangerRole.RoleMember member : role.getRoles()) {
                    if (StringUtils.equals(member.getName(), newRole) && member.getIsAdmin()) {
                        member.setIsAdmin(false);
                    }
                }
            }

            role = roleStore.updateRole(role, false);

        } catch(WebApplicationException excp) {
            throw excp;
        } catch(Throwable excp) {
            LOG.error("removeAdminFromUsersGroupsAndRoles() failed", excp);

            throw restErrorUtil.createRESTException(excp.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== removeAdminFromUsersGroupsAndRoles(name=" + role.getName() + ", users=" + Arrays.toString(users.toArray()) + ", roles=" + Arrays.toString(roles.toArray()) + ")");
        }

        return role;
    }

    private Set<String> getUserNames(RangerRole role) {

        Set<String> usernames = new HashSet<>();
        for (RangerRole.RoleMember user : role.getUsers()) {
            usernames.add(user.getName());
        }
        return usernames;
    }

    private Set<String> getGroupNames(RangerRole role) {

        Set<String> groupnames = new HashSet<>();
        for (RangerRole.RoleMember group : role.getGroups()) {
            groupnames.add(group.getName());
        }
        return groupnames;
    }

    private Set<String> getRoleNames(RangerRole role) {
        Set<String> rolenames = new HashSet<>();
        for (RangerRole.RoleMember roleMember : role.getRoles()) {
            rolenames.add(roleMember.getName());
        }
        return rolenames;
    }

    private void getRoleMemberNames(Set<String> roleMembers, RangerRole role) throws Exception {
       for (RangerRole.RoleMember roleMember : role.getRoles()) {
           roleMembers.add(roleMember.getName());
           getRoleMemberNames(roleMembers, roleStore.getRole(roleMember.getName()));
        }
    }

    private void getRoleMembers(Set<RangerRole.RoleMember> roleMembers, RangerRole role) throws Exception {
        for (RangerRole.RoleMember roleMember : role.getRoles()) {
            roleMembers.add(roleMember);
            getRoleMembers(roleMembers, roleStore.getRole(roleMember.getName()));
        }
    }

    private void validateUsersGroupsAndRoles(GrantRevokeRoleRequest request){
        if (request == null) {
            throw restErrorUtil.createRESTException("Invalid grant/revoke role request");
        }

        if(CollectionUtils.isEmpty(request.getUsers()) && CollectionUtils.isEmpty(request.getGroups()) && CollectionUtils.isEmpty(request.getRoles())) {
            throw restErrorUtil.createRESTException("Grantee users/groups/roles list is empty");
        }
        if (request.getUsers() == null) {
            request.setUsers(new HashSet<>());
        }

        if (request.getGroups() == null ) {
            request.setGroups(new HashSet<>());
        }

        if (request.getRoles() == null) {
            request.setRoles(new HashSet<>());
        }
    }
}

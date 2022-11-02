package com.alibaba.tesla.authproxy.outbound.oam;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.api.model.CheckPermissionRequest;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.outbound.acs.AcsClientFactory;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * OAM 客户端
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class OamClient {

    @Autowired
    private AcsClientFactory acsClientFactory;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 获取当前的所有角色名称
     *
     * @param roleNameHas 模糊搜索名字
     * @return 角色列表
     */
    //public List<ListRolesResponse.OamRole> listRoles(String ownerAliyunPk, String ownerBid, String roleNameHas)
    //    throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListRolesRequest request = new ListRolesRequest();
    //    request.setPageIndex(1);
    //    request.setPageSize(1000);
    //    request.setRoleNameHas(roleNameHas);
    //    log.info("Call OAM ListRoles, request={}", gson.toJson(request));
    //    ListRolesResponse response;
    //    try {
    //        response = acsClient.getAcsResponse(request);
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "ListRoles failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //    log.info("Call OAM ListRoles, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 根据 OAM Username 来获取对应用户（副作用：让 OAM 认识该用户，用于新建用户过程中）
    // *
    // * @param username 用户名
    // */
    //public GetOamUserByUserNameResponse.Data getOamUserByUsername(String username) throws ClientException {
    //    IAcsClient acsClient = acsClientFactory.getSuperClient();
    //    GetOamUserByUserNameRequest request = new GetOamUserByUserNameRequest();
    //    request.setUserName(username);
    //    log.info("Call OAM GetOamUserByUserName, request={}", gson.toJson(request));
    //    GetOamUserByUserNameResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM GetOamUserByUserName, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 检查权限
    // *
    // * @param ownerAliyunPk  所有者 Aliyun PK
    // * @param ownerBid       所有者 Bid
    // * @param aliyunPk       需要更新的用户 Aliyun PK
    // * @param permissionName 新密码
    // * @throws AuthProxyThirdPartyError OAM 系统错误时抛出
    // */
    //public Boolean checkPermission(String ownerAliyunPk, String ownerBid, String aliyunPk, String permissionName)
    //    throws ClientException, AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    CheckPermissionRequest request = new CheckPermissionRequest();
    //    request.setResource(permissionName);
    //    request.setActionField("read");
    //    request.setAliUid(Long.valueOf(aliyunPk));
    //    log.info("Call OAM CheckPermission, request={}", gson.toJson(request));
    //    CheckPermissionResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM CheckPermission, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个 Role 的详细信息
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名称
    // */
    //public GetRoleResponse.Data getRole(String ownerAliyunPk, String ownerBid, String roleName)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    GetRoleRequest request = new GetRoleRequest();
    //    request.setRoleName(roleName);
    //    log.info("Call OAM GetRole, request={}", gson.toJson(request));
    //    GetRoleResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM GetRole, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个用户的所有被授予的权限
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param aliyunPk      需要查询用户的 Aliyun ID
    // */
    //public List<OamRole> listRoleByOperator(String ownerAliyunPk, String ownerBid, String aliyunPk)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListRoleByOperatorRequest request = new ListRoleByOperatorRequest();
    //    request.setOperatorName(aliyunPk);
    //    request.setUserType("User");
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    log.info("Call OAM ListRoleByOperator, request={}", gson.toJson(request));
    //    ListRoleByOperatorResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListRoleByOperator, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个角色当前被授权的所有用户
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      需要查询的角色名
    // */
    //public List<OamUser> listOperatorByRole(String ownerAliyunPk, String ownerBid, String roleName)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListOperatorByRoleRequest request = new ListOperatorByRoleRequest();
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    request.setRoleName(roleName);
    //    log.info("Call OAM ListOperatorByRole, request={}", gson.toJson(request));
    //    ListOperatorByRoleResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListOperatorByRole, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个角色当前被授权的所有组
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      需要查询的角色名
    // */
    //public List<ListGroupsByRoleResponse.OamGroup> listGroupsByRole(
    //    String ownerAliyunPk, String ownerBid, String roleName)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListGroupsByRoleRequest request = new ListGroupsByRoleRequest();
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    request.setRoleName(roleName);
    //    log.info("Call OAM ListGroupsByRole, request={}", gson.toJson(request));
    //    ListGroupsByRoleResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListGroupsByRole, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个用户的被授予的 Groups
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param aliyunPk      需要查询的用户 PK
    // */
    //public List<ListGroupsForUserResponse.OamGroup> listGroupsForUser(
    //    String ownerAliyunPk, String ownerBid, String aliyunPk)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListGroupsForUserRequest request = new ListGroupsForUserRequest();
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    request.setUserName(aliyunPk);
    //    log.info("Call OAM ListGroupsForUser, request={}", gson.toJson(request));
    //    ListGroupsForUserResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListGroupsForUser, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个用户的被授予的 Groups
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param groupName     需要查询的 Group name
    // */
    //public List<ListUsersForGroupResponse.OamUser> listUsersForGroup(
    //    String ownerAliyunPk, String ownerBid, String groupName)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListUsersForGroupRequest request = new ListUsersForGroupRequest();
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    request.setGroupName(groupName);
    //    log.info("Call OAM ListUsersForGroup, request={}", gson.toJson(request));
    //    ListUsersForGroupResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListUsersForGroup, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 获取某个 Role 的继承关系中所有的 Roles
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      需要查询的角色名称
    // */
    //public List<ListBaseRolesByRoleResponse.OamRole> listBaseRolesByRole(
    //    String ownerAliyunPk, String ownerBid, String roleName)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListBaseRolesByRoleRequest request = new ListBaseRolesByRoleRequest();
    //    request.setPageSize(10000);
    //    request.setPageIndex(1);
    //    request.setRoleName(roleName);
    //    log.info("Call OAM ListBaseRolesByRole, request={}", gson.toJson(request));
    //    ListBaseRolesByRoleResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM ListBaseRolesByRole, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 授权某个角色对某个用户
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名
    // * @param aliyunPk      需要授权对应的 Aliyun PK
    // */
    //public void grantRoleToOperator(String ownerAliyunPk, String ownerBid, String roleName, String aliyunPk)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    GrantRoleToOperatorRequest request = new GrantRoleToOperatorRequest();
    //    request.setUserType("User");
    //    request.setRoleName(roleName);
    //    request.setToOperatorName(aliyunPk);
    //    request.setGmtExpired("2099-12-31 00:00:00 UTC");
    //    log.info("Call OAM GrantRoleToOperator, request={}", gson.toJson(request));
    //    GrantRoleToOperatorResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM GrantRoleToOperator, response={}", gson.toJson(response));
    //}
    //
    ///**
    // * 取消某个角色对某个用户的授权
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名
    // * @param aliyunPk      需要撤销对应的 Aliyun PK
    // */
    //public void revokeRoleFromOperator(String ownerAliyunPk, String ownerBid, String roleName, String aliyunPk)
    //    throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    RevokeRoleFromOperatorRequest request = new RevokeRoleFromOperatorRequest();
    //    request.setUserType("User");
    //    request.setRoleName(roleName);
    //    request.setOperatorName(aliyunPk);
    //    log.info("Call OAM RevokeRoleFromOperator, request={}", gson.toJson(request));
    //    RevokeRoleFromOperatorResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call OAM RevokeRoleFromOperator, response={}", gson.toJson(response));
    //}
    //
    ///**
    // * 在角色 roleName 中添加资源点 resource
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名
    // * @param resource      需要添加的资源
    // */
    //public void addRoleCellToRole(String ownerAliyunPk, String ownerBid, String roleName, String resource)
    //    throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    AddRoleCellToRoleRequest request = new AddRoleCellToRoleRequest();
    //    request.setRoleName(roleName);
    //    request.setResource(resource);
    //    request.setActionLists(Collections.singletonList("*"));
    //    request.setGrantOption(0);
    //    log.info("Call OAM AddRoleCellToRole, request={}", TeslaGsonUtil.toJson(request));
    //    try {
    //        AddRoleCellToRoleResponse response = acsClient.getAcsResponse(request);
    //        log.info("Call OAM AddRoleCellToRole, response={}", gson.toJson(response));
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "AddRoleCellToRole failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //}
    //
    ///**
    // * Add role to role
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      roleName
    // * @param baseRoleName  baseRoleName
    // */
    //public void addRoleToRole(String ownerAliyunPk, String ownerBid, String roleName, String baseRoleName)
    //    throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    AddRoleToRoleRequest request = new AddRoleToRoleRequest();
    //    request.setRoleName(roleName);
    //    request.setBaseRoleName(baseRoleName);
    //    log.info("Call OAM AddRoleToRole, request={}", TeslaGsonUtil.toJson(request));
    //    try {
    //        AddRoleToRoleResponse response = acsClient.getAcsResponse(request);
    //        log.info("Call OAM AddRoleToRole, response={}", gson.toJson(response));
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "AddRoleToRole failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //}
    //
    ///**
    // * 删除 Role Cell Id
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleCellId    权限点 ID
    // */
    //public void removeRoleCellFromRole(String ownerAliyunPk, String ownerBid, String roleCellId)
    //    throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    RemoveRoleCellFromRoleRequest request = new RemoveRoleCellFromRoleRequest();
    //    request.setRoleCellId(roleCellId);
    //    log.info("Call OAM RemoveRoleCellFromRole, request={}", TeslaGsonUtil.toJson(request));
    //    try {
    //        RemoveRoleCellFromRoleResponse response = acsClient.getAcsResponse(request);
    //        log.info("Call OAM RemoveRoleCellFromRole, response={}", gson.toJson(response));
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "RemoveRoleCellFromRole failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //}
    //
    ///**
    // * 获取指定角色拥有哪些权限节点
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名称
    // * @return 权限列表
    // */
    //public List<ListRoleCellsByRoleNameResponse.OamRoleCell> listRoleCellsByRoleName(
    //    String ownerAliyunPk, String ownerBid, String roleName) throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    ListRoleCellsByRoleNameRequest request = new ListRoleCellsByRoleNameRequest();
    //    request.setRoleName(roleName);
    //    request.setPageIndex(1);
    //    request.setPageSize(1000);
    //    log.info("Call OAM ListRoleCellsByRoleName, request={}", TeslaGsonUtil.toJson(request));
    //    ListRoleCellsByRoleNameResponse response;
    //    try {
    //        response = acsClient.getAcsResponse(request);
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "ListRoleCellsByRoleName failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //    log.info("Call OAM ListRoleCellsByRoleName, response={}", gson.toJson(response));
    //    return response.getData();
    //}
    //
    ///**
    // * 创建角色
    // *
    // * @param ownerAliyunPk 所有者 Aliyun PK
    // * @param ownerBid      所有者 Bid
    // * @param roleName      角色名称
    // */
    //public void createRole(String ownerAliyunPk, String ownerBid, String roleName, String description)
    //    throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getOamClient(ownerAliyunPk, ownerBid);
    //    CreateRoleRequest request = new CreateRoleRequest();
    //    request.setRoleName(roleName);
    //    request.setDescription(description);
    //    request.setRoleType("OAM");
    //    log.info("Call OAM CreateRole, request={}", TeslaGsonUtil.toJson(request));
    //    try {
    //        CreateRoleResponse response = acsClient.getAcsResponse(request);
    //        log.info("Call OAM ListRoleCellsByRoleName, response={}", gson.toJson(response));
    //    } catch (ClientException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
    //            "CreateRole failed: " + TeslaGsonUtil.toJson(e));
    //    }
    //}
}

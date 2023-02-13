package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateInternalError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.oam.OamClient;
import com.alibaba.tesla.authproxy.service.PrivatePermissionService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.service.ao.UserRoleItemAO;
import com.alibaba.tesla.authproxy.service.constant.UserTypeEnum;
import com.alibaba.tesla.authproxy.util.DateTimeUtil;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountAddParam;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountDeleteParam;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleAccountResult;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleAccountResult.PrivatePermissionRoleAccountItem;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleResult;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleResult.PrivatePermissionRoleItem;
import com.aliyuncs.exceptions.ClientException;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PrivatePermissionServiceImpl implements PrivatePermissionService {

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private OamClient oamClient;

    /**
     * 判断当前的 role 是否需要进行跳过
     * @param roleName
     * @return
     */
    private boolean skipRole(String roleName) {
        return roleName.startsWith("bcc_admin_")
            || roleName.endsWith("guest")
            || roleName.startsWith(Constants.ABM_ROLE_PREFIX_API + "link")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "eblink")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "ecloud")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "fuxi")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "pangu")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "tesla")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "tianji")
            || roleName.contains(Constants.ABM_ROLE_PREFIX_API + "SRE");
    }

    /**
     * 获取角色列表
     */
    @Override
    public PrivatePermissionRoleResult getRoles(UserDO userDo) throws AuthProxyThirdPartyError,
        PrivateInternalError {
        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }
        PrivatePermissionRoleResult result = new PrivatePermissionRoleResult();

        //List<ListRolesResponse.OamRole> roles = oamClient
        //    .listRoles(adminUserDo.getAliyunPk(), adminUserDo.getBid(), "bcc_");
        //
        //// 生成对应的角色结果列表
        //for (ListRolesResponse.OamRole role : roles) {
        //    String roleName = role.getRoleName().trim();
        //    if (skipRole(roleName)) {
        //        continue;
        //    }
        //
        //    PrivatePermissionRoleItem item = new PrivatePermissionRoleItem();
        //    item.setBid(role.getBid());
        //    item.setRoleId(role.getRoleId());
        //    item.setRoleName(roleName);
        //    try {
        //        item.setGmtModified(new SimpleDateFormat(DateTimeUtil.DATE_TIME_FORMAT1)
        //            .parse(cleanDateTime(role.getGmtModified())));
        //    } catch (ParseException e) {
        //        item.setGmtModified(new Date(0));
        //    }
        //    item.setOwner(role.getOwner());
        //    item.setOwnerName(role.getOwnerName());
        //    item.setOwnerType(role.getOwnerType());
        //    item.setDescription(role.getDescription());
        //    try {
        //        item.setGmtExpired(new SimpleDateFormat(DateTimeUtil.DATE_TIME_FORMAT1).parse("2030-12-31 23:59:59"));
        //    } catch (ParseException e) {
        //        item.setGmtExpired(new Date(0));
        //    }
        //    result.addResult(item);
        //}
        return result;
    }

    /**
     * 获取角色列表 (针对 API 权限)
     */
    @Override
    public PrivatePermissionRoleResult getApiRoles(UserDO userDo) throws AuthProxyThirdPartyError,
        PrivateInternalError {
        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }
        PrivatePermissionRoleResult result = new PrivatePermissionRoleResult();

        //List<ListRolesResponse.OamRole> roles = oamClient.listRoles(adminUserDo.getAliyunPk(),
        //    adminUserDo.getBid(), "bcc_api_");
        //
        //// 生成对应的角色结果列表
        //for (ListRolesResponse.OamRole role : roles) {
        //    PrivatePermissionRoleItem item = new PrivatePermissionRoleItem();
        //    item.setBid(role.getBid());
        //    item.setRoleId(role.getRoleId());
        //    item.setRoleName(role.getRoleName());
        //    try {
        //        item.setGmtModified(new SimpleDateFormat(DateTimeUtil.DATE_TIME_FORMAT1)
        //            .parse(cleanDateTime(role.getGmtModified())));
        //    } catch (ParseException e) {
        //        item.setGmtModified(new Date(0));
        //    }
        //    item.setOwner(role.getOwner());
        //    item.setOwnerName(role.getOwnerName());
        //    item.setOwnerType(role.getOwnerType());
        //    item.setDescription(role.getDescription());
        //    try {
        //        item.setGmtExpired(new SimpleDateFormat(DateTimeUtil.DATE_TIME_FORMAT1).parse("2030-12-31 23:59:59"));
        //    } catch (ParseException e) {
        //        item.setGmtExpired(new Date(0));
        //    }
        //    result.addResult(item);
        //}
        return result;
    }

    /**
     * 获取角色可授权的云账号列表
     */
    @Override
    public PrivatePermissionRoleAccountResult getRoleAccounts(UserDO userDo, String roleName, String filter)
        throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError {
        // 对过滤器参数进行检查
        if (filter.length() == 0) {
            filter = "all";
        }
        if (!filter.equals("all") && !filter.equals("authorized")) {
            throw new PrivateValidationError(
                "filter", locale.msg("private.permission.role.account.validation.filter.invalid"));
        }

        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }

        // 从 OAM 获取可授权的用户列表
        //List<OamUser> oamUsers;
        //try {
        //    oamUsers = oamClient.listOperatorByRole(adminUserDo.getAliyunPk(), adminUserDo.getBid(), roleName);
        //} catch (ClientException e) {
        //    switch (e.getErrCode()) {
        //        case "ROLE_NOT_EXIST":
        //            throw new PrivateValidationError(
        //                "roleName", locale.msg("private.permission.role.account.validation.roleName.notExist"));
        //        default:
        //            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                "ListOperatorByRole failed: " + e.getMessage());
        //            se.initCause(e);
        //            throw se;
        //    }
        //}

        // OAM 系统中取出的用户与当前系统中保存的账号进行比对
        Set<String> authorizedAliyunPks = new HashSet<>();
        Map<String, UserDO> userDos = getAllTeslaUsers();
        PrivatePermissionRoleAccountResult results = new PrivatePermissionRoleAccountResult();
        //for (OamUser oamUser : oamUsers) {
        //    String aliyunPk = oamUser.getLoginId();
        //    if (!userDos.containsKey(aliyunPk)) {
        //        continue;
        //    }
        //    authorizedAliyunPks.add(aliyunPk);
        //    UserDO user = userDos.get(aliyunPk);
        //    PrivatePermissionRoleAccountItem item = new PrivatePermissionRoleAccountItem();
        //    item.setAliyunId(user.getLoginName());
        //    item.setPhone(user.getPhone());
        //    item.setStatus("authorized");
        //    results.addResult(item);
        //}

        // 如果只获取已授权的账号，那么直接返回
        if (filter.equals("authorized")) {
            return results;
        }

        // 如果获取全部账号，那么继续扫描
        for (Map.Entry<String, UserDO> user : userDos.entrySet()) {
            if (authorizedAliyunPks.contains(user.getValue().getAliyunPk())) {
                continue;
            }
            PrivatePermissionRoleAccountItem item = new PrivatePermissionRoleAccountItem();
            item.setAliyunId(user.getValue().getLoginName());
            item.setPhone(user.getValue().getPhone());
            item.setStatus("unauthorized");
            results.addResult(item);
        }
        return results;
    }

    /**
     * 授权角色到用户
     */
    @Override
    public void grantRoleToAccount(UserDO userDo, PrivatePermissionRoleAccountAddParam param)
        throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError {
        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }

        String roleName = param.getRoleName();
        List<String> aliyunIds = param.getAliyunId();
        for (String aliyunId : aliyunIds) {
            UserDO user = teslaUserService.getUserByLoginName(aliyunId);
            if (null == user) {
                throw new PrivateValidationError("aliyunId",
                    locale.msg("private.permission.role.account.add.validation.aliyunId.notExist", aliyunId));
            }
            //try {
            //    oamClient.grantRoleToOperator(adminUserDo.getAliyunPk(), adminUserDo.getBid(), roleName, user.getAliyunPk());
            //} catch (ClientException e) {
            //    AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
            //        "GrantRoleToOperator failed: " + e.getMessage());
            //    se.initCause(e);
            //    throw se;
            //}

            // 创建 user 与 role 的绑定关系 (仅当以 ABM_ROLE_PREFIX_API 为前缀的时候)
            if (roleName.startsWith(Constants.ABM_ROLE_PREFIX_API)) {
                userRoleService.create(UserRoleItemAO.builder()
                    .tenantId(Constants.DEFAULT_TENANT_ID)
                    .userId(user.getUserId())
                    .roleId(oamRoleNmaeToRoleId(roleName))
                    .build());
                log.info("Create user-role binding, userId={}, roleId={}",
                    user.getUserId(), oamRoleNmaeToRoleId(roleName));
            }
        }
    }

    /**
     * 将 OAM role name 转换为等价的权代 role id
     *
     * @param oamRoleName OAM role name
     * @return
     */
    private String oamRoleNmaeToRoleId(String oamRoleName) {
        return oamRoleName.substring(Constants.ABM_ROLE_PREFIX_API.length()).replace("_", ":");
    }

    /**
     * 对指定用户取消授权角色
     */
    @Override
    public void deleteRoleFromAccount(UserDO userDo, PrivatePermissionRoleAccountDeleteParam param)
        throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError {
        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }

        String roleName = param.getRoleName();
        List<String> aliyunIds = param.getAliyunId();
        Set<String> superUsers = Sets.newHashSet(authProperties.getAasSuperUser().split(","));
        for (String aliyunId : aliyunIds) {
            if (superUsers.contains(aliyunId)) {
                throw new PrivateValidationError("aliyunId",
                    locale.msg("private.permission.role.account.add.validation.aliyunId.containSuperUser", aliyunId));
            }
            UserDO user = teslaUserService.getUserByLoginName(aliyunId);
            if (null == user) {
                throw new PrivateValidationError("aliyunId",
                    locale.msg("private.permission.role.account.add.validation.aliyunId.notExist", aliyunId));
            }
            //try {
            //    oamClient.revokeRoleFromOperator(adminUserDo.getAliyunPk(), adminUserDo.getBid(), roleName, user.getAliyunPk());
            //} catch (ClientException e) {
            //    AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
            //        "RevokeRoleFromOperator failed: " + e.getMessage());
            //    se.initCause(e);
            //    throw se;
            //}

            // 删除 user 与 role 的绑定关系 (仅当以 ABM_ROLE_PREFIX_API 为前缀的时候)
            if (roleName.startsWith(Constants.ABM_ROLE_PREFIX_API)) {
                userRoleService.delete(UserRoleItemAO.builder()
                    .tenantId(Constants.DEFAULT_TENANT_ID)
                    .userId(user.getUserId())
                    .roleId(oamRoleNmaeToRoleId(roleName))
                    .build());
                log.info("Delete user-role binding, userId={}, roleId={}",
                    user.getUserId(), oamRoleNmaeToRoleId(roleName));
            }
        }
    }

    /**
     * 获取用户类型
     *
     * @param userDo 需要检查的用户
     * @return 用户类型
     */
    @Override
    public UserTypeEnum getUserType(UserDO userDo)
        throws AuthProxyThirdPartyError, PrivateInternalError {
        // 检查该用户是否为超级管理员
        Set<String> superUsers = Sets.newHashSet(authProperties.getAasSuperUser().split(","));
        if (superUsers.contains(userDo.getLoginName())) {
            return UserTypeEnum.SUPERADMIN;
        }

        // 获取超级管理员的用户
        UserDO adminUserDo = getAdminRoleUserDo(userDo.getAliyunPk(), userDo.getBid());
        if (null == adminUserDo) {
            throw new PrivateInternalError(locale.msg("private.permission.admin.notLogin"));
        }

        // 检查该用户是否有云账号管理权限
        Boolean checkResult = Boolean.FALSE;
        //try {
        //    checkResult = oamClient.checkPermission(
        //        adminUserDo.getAliyunPk(),
        //        adminUserDo.getBid(),
        //        userDo.getAliyunPk(),
        //        authProperties.getOamAccountResourceName()
        //    );
        //} catch (ClientException e) {
        //    switch (e.getErrCode()) {
        //        case "USER_NOT_EXIST":
        //            break;
        //        default:
        //            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                "CheckPermission failed: " + e.getMessage());
        //            se.initCause(e);
        //            throw se;
        //    }
        //}
        //if (!checkResult) {
        //    return UserTypeEnum.USER;
        //}
        return UserTypeEnum.ACCOUNTADMIN;
    }

    /**
     * 清理 OAM 返回的时间字符串为标准格式
     */
    private String cleanDateTime(String datetime) {
        if (datetime.endsWith(" UTC")) {
            return datetime.substring(0, datetime.length() - 4);
        }
        return datetime;
    }

    /**
     * 获取当前系统中的所有用户，并以 Map 的形式返回, key 为 Aliyun PK
     */
    private Map<String, UserDO> getAllTeslaUsers() {
        Map<String, UserDO> mapUsers = new HashMap<>();
        List<UserDO> userDos = teslaUserService.selectByName("");
        for (UserDO userDo : userDos) {
            mapUsers.put(userDo.getAliyunPk(), userDo);
        }
        return mapUsers;
    }

    /**
     * 获取 bcc_admin 管理角色的拥有者用户
     *
     * @param aliyunPk 当前用户 aliyun pk
     * @param bid      当前用户 bid
     */
    private UserDO getAdminRoleUserDo(String aliyunPk, String bid) throws AuthProxyThirdPartyError {
        return null;
        //GetRoleResponse.Data data;
        //try {
        //    data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
        //} catch (ClientException e) {
        //    switch (e.getErrCode()) {
        //        case "USER_NOT_EXIST":
        //            try {
        //                oamClient.getOamUserByUsername(aliyunPk);
        //                data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
        //            } catch (ClientException ie) {
        //                AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                    "GetOamUserByUserName/GetRole failed: " + ie.getMessage());
        //                se.initCause(ie);
        //                throw se;
        //            }
        //            break;
        //        default:
        //            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                "GetRole failed: " + e.getMessage());
        //            se.initCause(e);
        //            throw se;
        //    }
        //}
        //return teslaUserService.getUserByAliyunPk(data.getOwnerName());
    }
}

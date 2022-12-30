package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.SwitchViewUserService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.service.ao.UserGetConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserGetResultAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleItemAO;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.util.PermissionUtil;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.authproxy.util.UserUtil;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaMessageDigest;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Title: TeslaUserServiceImpl.java<／p>
 * <p>Description: 用户信息服务实现逻辑 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service
@Slf4j
public class TeslaUserServiceImpl implements TeslaUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private AuthPolicy authPolicy;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private SwitchViewUserService switchViewUserService;

    @Autowired
    private UserRoleRelMapper userRoleRelMapper;

    @Override
    public int save(UserDO teslaUser) throws ApplicationException {
        String loginName = teslaUser.getLoginName();
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = loginName.indexOf("@");
            if (index >= 0) {
                loginName = loginName.substring(0, index);
            }
            if (StringUtils.isEmpty(loginName) && StringUtils.isEmpty(teslaUser.getEmail())) {
                loginName = teslaUser.getEmail().split("@")[0];
            }
        }
        try {
            UserDO oldTeslaUser = getUserByLoginName(loginName);
            if (oldTeslaUser == null) {
                //兼容对内loginName为null的场景
                oldTeslaUser = getUserByEmpId(teslaUser.getEmpId());
            }
            if (null == oldTeslaUser) {
                log.info("Cannot find user [{}] in database, add it now", loginName);
                teslaUser.setTenantId(Constants.DEFAULT_TENANT_ID);
                teslaUser.setLoginName(loginName);
                teslaUser.setUserId(UserUtil.getUserId(teslaUser));
                return insert(teslaUser);
            } else {
                oldTeslaUser.setTenantId(Constants.DEFAULT_TENANT_ID);
                oldTeslaUser.setGmtModified(teslaUser.getGmtModified());
                oldTeslaUser.setLastLoginTime(teslaUser.getLastLoginTime());
                oldTeslaUser.setAliww(teslaUser.getAliww());
                oldTeslaUser.setBucId(teslaUser.getBucId());
                oldTeslaUser.setBid(teslaUser.getBid());
                oldTeslaUser.setEmail(teslaUser.getEmail());
                oldTeslaUser.setEmpId(teslaUser.getEmpId());
                oldTeslaUser.setLoginName(loginName);
                oldTeslaUser.setNickName(teslaUser.getNickName());
                if (teslaUser.getPhone() != null) {
                    oldTeslaUser.setPhone(teslaUser.getPhone());
                }
                oldTeslaUser.setLang(teslaUser.getLang());
                oldTeslaUser.setUserId(UserUtil.getUserId(oldTeslaUser));
                if (!StringUtils.isEmpty(teslaUser.getDepId())) {
                    oldTeslaUser.setDepId(teslaUser.getDepId());
                }
                return update(oldTeslaUser);
            }
        } catch (Exception e) {
            log.error("Save user info failed", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.save"));
        }
    }

    @Override
    public int update(UserDO teslaUser) throws ApplicationException {
        try {
            return userMapper.updateByPrimaryKey(teslaUser);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.update"));
        }
    }

    @Override
    public int insert(UserDO teslaUser) throws ApplicationException {
        try {
            if (!StringUtils.isEmpty(teslaUser.getLoginPwd())) {
                teslaUser.setLoginPwd(TeslaMessageDigest.getMD5(teslaUser.getLoginPwd()));
            }
            teslaUser.setGmtCreate(new Date());
            return userMapper.insert(teslaUser);
        } catch (Exception e) {
            log.error("添加用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.insert"));
        }
    }

    @Override
    public UserDO getUserByAliyunId(String aliyunId) throws ApplicationException {
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = aliyunId.indexOf("@");
            if (index >= 0) {
                aliyunId = aliyunId.substring(0, index);
            }
        }
        log.info("根据用户的aliyunId[{}]获取用户信息，调用mapper的getByLoginName方法", aliyunId);
        try {
            return userMapper.getByAliyunId(aliyunId);
        } catch (Exception e) {
            log.error("获取登录用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByLoginName(String loginName) throws ApplicationException {
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = loginName.indexOf("@");
            if (index >= 0) {
                loginName = loginName.substring(0, index);
            }
        }
        try {
            return userMapper.getByLoginName(loginName);
        } catch (Exception e) {
            log.error("Get user by login name {} failed, exception={}", loginName, ExceptionUtils.getStackTrace(e));
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByLoginNameAndPwd(String loginName, String pwd) throws ApplicationException {
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = loginName.indexOf("@");
            if (index >= 0) {
                loginName = loginName.substring(0, index);
            }
        }
        try {
            return userMapper.getByLoginNameAndPassword(loginName, TeslaMessageDigest.getMD5(pwd));
        } catch (Exception e) {
            log.error("Get user by login name and pwd {} failed, exception={}", loginName,
                ExceptionUtils.getStackTrace(e));
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByLoginNameAndEmail(String loginName, String emailAddress) throws ApplicationException {
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = loginName.indexOf("@");
            if (index >= 0) {
                loginName = loginName.substring(0, index);
            }
        }
        try {
            return userMapper.getByLoginNameAndEmail(loginName, emailAddress);
        } catch (Exception e) {
            log.error("Get user by login name and email {} failed, exception={}", loginName,
                ExceptionUtils.getStackTrace(e));
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByAliyunPk(String aliyunPk) throws ApplicationException {
        log.info("根据用户的aliyunPk[{}]获取用户信息，调用mapper的getByLoginName方法", aliyunPk);
        try {
            return userMapper.getByAliyunPk(aliyunPk);
        } catch (Exception e) {
            log.error("获取登录用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByBucId(String bucId) throws ApplicationException {
        try {
            return userMapper.getByBucId(bucId);
        } catch (Exception e) {
            log.error("获取登录用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByAccessKeyId(String accessKeyId) throws ApplicationException {
        try {
            return userMapper.getByAccessKeyId(accessKeyId);
        } catch (Exception e) {
            log.error("获取登录用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    @Override
    public UserDO getUserByEmpId(String empId) throws ApplicationException {
        try {
            return userMapper.getByEmpId(empId);
        } catch (Exception e) {
            log.error("获取登录用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.getLoginUser"));
        }
    }

    /**
     * 根据查询条件获取指定用户的信息 (包含扩展信息)
     *
     * @param condition 查询条件
     */
    @Override
    public UserGetResultAO getEnhancedUser(UserGetConditionAO condition) {
        String locale = condition.getLocale();
        String tenantId = condition.getTenantId();
        String appId = condition.getAppId();
        String empId = condition.getEmpId();

        // 用户信息获取
        UserDO user = userMapper.getByEmpId(empId);
        if (user == null) {
            user = authPolicy.getAuthServiceManager().getUserByEmpId(empId);
            if (user == null) {
                throw new AuthProxyException(String.format("Cannot get empId %s in remote user system", empId));
            }
            insert(user);
        } else {
            user.setExt(authPolicy.getAuthServiceManager().getUserExtInfo(user));
        }
        String userId = user.getUserId();
        String depId = user.getDepId();

        // 填充该用户当前应用的所有角色信息到属性中
        List<UserRoleRelDO> userRoles = getUserRoles(locale, tenantId, appId, userId, depId, 0);
        userRoles.sort((o1, o2) -> {
            String o1Name = o1.getRoleId().split(":")[1];
            String o2Name = o2.getRoleId().split(":")[1];
            return o1Name.compareTo(o2Name);
        });
        user.setRoles(userRoles);

        // 补充用户切换信息
        user.getExt().setFromEmpId(condition.getFromEmpId());
        user.getExt().setFromLoginName(condition.getFromAuthUser());
        user.getExt().setFromBucId(condition.getFromBucId());
        if (StringUtils.isEmpty(condition.getFromEmpId())) {
            user.getExt().setCanSwitchView(switchViewUserService.getByEmpId(condition.getEmpId()) != null);
        } else {
            user.getExt().setCanSwitchView(switchViewUserService.getByEmpId(condition.getFromEmpId()) != null);
        }
        return UserGetResultAO.from(user);
    }

    /**
     * 获取指定用户的角色列表
     *
     * @param locale   语言
     * @param tenantId 租户 ID
     * @param appId    应用 ID
     * @param userId   用户唯一标识 ID
     * @param depId    部门 ID
     * @param step     调用次数
     * @return 返回当前用户对应的角色列表
     */
    private List<UserRoleRelDO> getUserRoles(String locale, String tenantId, String appId,
                                             String userId, String depId, int step) {
        if (StringUtils.isEmpty(appId)) {
            return new ArrayList<>();
        }

        // 检查 roles 是否满足预设标准，如果不满足的话，进行自动填充
        List<UserRoleRelDO> roles = userRoleRelMapper
            .findAllByTenantIdAndUserIdAndAppId(locale, tenantId, userId, appId);
        Set<String> roleSet = roles.stream()
            .map(UserRoleRelDO::getRoleId)
            .collect(Collectors.toSet());
        List<UserRoleRelDO> depRoles = userRoleRelMapper
            .findAllByTenantIdAndUserIdAndAppId(locale, tenantId, PermissionUtil.getDepUserId(depId), appId);
        for (UserRoleRelDO depRole : depRoles) {
            if (!roleSet.contains(depRole.getRoleId())) {
                roles.add(depRole);
            }
        }

        boolean valid = false;
        for (UserRoleRelDO role : roles) {
            String[] roleIdArray = role.getRoleId().split(":");
            String subAppId = roleIdArray[0];
            String subRoleName = roleIdArray[roleIdArray.length - 1];
            if (appId.equals(subAppId) && Constants.DEFAULT_GUEST_ROLE.equals(subRoleName)) {
                valid = true;
                break;
            }
        }

        // 如果已经是第二次请求了，那么无论如何也要返回出去当前获取到的 roles 集合
        log.info("action=service.teslaUser||message=get user roles" +
                "||locale={}||tenantId={}||appId={}||userId={}||depId={}||step={}||roleSet={}||depRoles={}||roles={}",
            locale, tenantId, appId, userId, depId, step, TeslaGsonUtil.toJson(roleSet), TeslaGsonUtil.toJson(depRoles),
            TeslaGsonUtil.toJson(roles));
        if (valid) {
            return roles;
        } else if (step >= 1) {
            log.error("action=service.teslaUser||message=get user roles failed, more than one step needed" +
                    "||locale={}||tenantId={}||appId={}||userId={}||depId={}||step={}", locale, tenantId, appId,
                userId, depId, step);
            return roles;
        }

        // 当不存在访客角色时默认增加一个
        String roleId = appId + ":" + Constants.DEFAULT_GUEST_ROLE;
        try {
            userRoleService.create(UserRoleItemAO.builder()
                .tenantId(tenantId)
                .roleId(roleId)
                .userId(userId)
                .build());
            log.info("action=service.user||message=no default user role found, craeted" +
                "||tenantId={}||roleId={}||userId={}||depId={}", tenantId, roleId, userId, depId);
        } catch (Exception e) {
            log.error("action=service.user||message=create default user role failed" +
                    "||tenantId={}||roleId={}||userId={}||depId={}||exception={}", tenantId, roleId, userId, depId,
                ExceptionUtils.getStackTrace(e));
        }

        // 再次重新获取当前用户的 roles 列表
        return getUserRoles(locale, tenantId, appId, userId, depId, step + 1);
    }

    @Override
    public List<UserDO> selectByName(String userName) throws ApplicationException {
        if (Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            int index = userName.indexOf("@");
            if (index >= 0) {
                userName = userName.substring(0, index);
            }
        }
        try {
            List<UserDO> users = userMapper.selectByName(userName);
            for (UserDO user : users) {
                user.setExt(authPolicy.getAuthServiceManager().getUserExtInfo(user));
            }
            return users;
        } catch (Exception e) {
            log.error("查询用户信息失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.list"));
        }
    }

    /**
     * 切换语言
     *
     * @param userDo 用户
     * @param lang   语言
     * @throws PrivateValidationError 验证出错时抛出
     */
    @Override
    public void changeLanguage(UserDO userDo, String lang) throws PrivateValidationError {
        String[] availableLanguages = authProperties.getAvailableLanguages().split(",");
        Boolean invalidLanguage = Boolean.TRUE;
        for (String availableLanguage : availableLanguages) {
            if (availableLanguage.equals(lang)) {
                invalidLanguage = Boolean.FALSE;
                break;
            }
        }
        if (invalidLanguage) {
            throw new PrivateValidationError("lang", locale.msg("private.validation.lang.invalid"));
        }

        // 设置语言项
        userDo.setLang(lang);
        update(userDo);
    }

    @Override
    public void changePassword(UserDO userDo, String oldPassword, String newPassword) throws ApplicationException {
        //比较旧密码是否正确
        UserDO oldUser = null;
        try {
            oldUser = userMapper.getByLoginNameAndPassword(userDo.getLoginName(),
                TeslaMessageDigest.getMD5(oldPassword));
        } catch (Exception e) {
            log.error("update user password failed", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.changePwd"));
        }
        if (null == oldUser) {
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.password"));
        }
        //设置新密码
        try {
            userMapper.updateLoginPassword(userDo.getLoginName(), TeslaMessageDigest.getMD5(newPassword),
                new Date());
        } catch (Exception e) {
            log.error("update user password failed", e);
            throw new ApplicationException(TeslaResult.FAILURE, locale.msg("error.user.changePwd"));
        }
    }

    @Override
    public PageInfo<UserDO> listUserWithPage(int page, int size, String loginName) throws ApplicationException {
        PageHelper.startPage(page, size);

        Map<String, Object> params = new HashMap<>();
        if (!StringUtil.isEmpty(loginName)) {
            params.put("loginName", loginName);
        }
        List<UserDO> userList = userMapper.selectByParams(params);
        PageInfo<UserDO> pageInfo = new PageInfo<>(userList);
        return pageInfo;
    }

}

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

import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import datart.core.base.consts.Const;
import datart.core.base.consts.TenantManagementMode;
import datart.core.base.consts.UserIdentityType;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.ParamException;
import datart.core.base.exception.ServerException;
import datart.core.common.Application;
import datart.core.common.UUIDGenerator;
import datart.core.entity.Organization;
import datart.core.entity.Role;
import datart.core.entity.User;
import datart.core.entity.ext.UserBaseInfo;
import datart.core.mappers.ext.OrganizationMapperExt;
import datart.core.mappers.ext.RelRoleUserMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.JwtToken;
import datart.security.base.PasswordToken;
import datart.security.base.RoleType;
import datart.security.exception.AuthException;
import datart.security.util.JwtUtils;
import datart.security.util.SecurityUtils;
import datart.server.base.dto.OrganizationBaseInfo;
import datart.server.base.dto.UserProfile;
import datart.server.base.params.*;
import datart.server.service.*;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static datart.core.common.Application.getProperty;

@Service
@Slf4j
public class UserServiceImpl extends BaseService implements UserService {

    private final UserMapperExt userMapper;

    private final OrganizationMapperExt orgMapper;

    private final OrgService orgService;

    private final RoleService roleService;

    private final MailService mailService;

    @Value("${datart.user.active.send-mail:false}")
    private boolean sendEmail;

    public UserServiceImpl(UserMapperExt userMapper,
                           OrganizationMapperExt orgMapper,
                           OrgService orgService,
                           RoleService roleService,
                           MailService mailService) {
        this.userMapper = userMapper;
        this.orgMapper = orgMapper;
        this.orgService = orgService;
        this.roleService = roleService;
        this.mailService = mailService;
    }

    @Override
    public UserProfile getUserProfile() {
        User user = userMapper.selectByPrimaryKey(getCurrentUser().getId());
        UserProfile userProfile = new UserProfile(user);
        List<OrganizationBaseInfo> organizationBaseInfos = orgService.listOrganizations();
        userProfile.setOrganizations(organizationBaseInfos);
        return userProfile;
    }

    @Override
    public List<UserBaseInfo> listUsersByKeyword(String keyword) {
        keyword = StringUtils.appendIfMissing(keyword, "%");
        keyword = StringUtils.prependIfMissing(keyword, "%");
        List<User> users = userMapper.searchUsers(keyword);
        final User self = securityManager.getCurrentUser();
        return users.stream()
                .filter(user -> !user.getId().equals(self.getId()))
                .map(UserBaseInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public User getUserByName(String username) {
        return userMapper.selectByNameOrEmail(username);
    }


    /**
     * 新用户注册
     * 1: 校验用户名和邮箱不能重复
     * 2: 根据应用程序配置，决定是否为用户发送激活邮件
     * 3: 激活用户，为用户创建默认组织和角色
     *
     * @param userRegisterParam 用户注册信息
     * @return 注册是否成功
     */
    @Override
    @Transactional
    public boolean register(UserRegisterParam userRegisterParam) throws MessagingException, UnsupportedEncodingException {
        return register(userRegisterParam, sendEmail);
    }

    @Override
    @Transactional
    public boolean register(UserRegisterParam userRegisterParam, boolean sendMail) throws MessagingException, UnsupportedEncodingException {
        if (!checkUserName(userRegisterParam.getUsername())) {
            log.error("The username({}) has been registered", userRegisterParam.getUsername());
            Exceptions.tr(ParamException.class, "error.param.occupied", "resource.user.username");
        }
        if (!checkEmail(userRegisterParam.getEmail())) {
            log.info("The email({}) has been registered", userRegisterParam.getEmail());
            Exceptions.tr(ParamException.class, "error.param.occupied", "resource.user.email");
        }
        User user = new User();
        BeanUtils.copyProperties(userRegisterParam, user);
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setId(UUIDGenerator.generate());
        user.setCreateBy(user.getId());
        user.setCreateTime(new Date());
        user.setActive(!sendMail);
        userMapper.insert(user);
        if (!sendMail) {
            initUser(user);
            return true;
        }
        mailService.sendActiveMail(user);
        return true;
    }

    @Override
    @Transactional
    public String activeUser(String activeString) {
        JwtToken jwtToken = null;
        try {
            jwtToken = JwtUtils.toJwtToken(activeString);
        } catch (ExpiredJwtException e) {
            Exceptions.msg("message.user.confirm.mail.timeout");
        }
        User user = userMapper.selectByUsername(jwtToken.getSubject());
        if (user == null) {
            Exceptions.notFound("resource.not-exist", "resource.user");
        }
        //更新用户激活状态至已激活
        int count = userMapper.updateToActiveById(user.getId());
        if (count != 1) {
            Exceptions.tr(BaseException.class, "message.user.active.fail", user.getUsername());
        }
        initUser(user);
        log.info("User({}) activation success", user.getUsername());
        jwtToken.setPwdHash(user.getPassword().hashCode());
        jwtToken.setExp(null);
        jwtToken.setCreateTime(System.currentTimeMillis());
        return JwtUtils.toJwtString(jwtToken);
    }

    @Override
    public boolean sendActiveMail(String usernameOrEmail) throws UnsupportedEncodingException, MessagingException {
        User user = userMapper.selectByNameOrEmail(usernameOrEmail);
        if (user == null) {
            Exceptions.notFound("base.not.exists", usernameOrEmail);
        }
        if (user.getActive()) {
            Exceptions.tr(BaseException.class, "message.user.active.fail", usernameOrEmail);
        }
        mailService.sendActiveMail(user);
        return true;
    }

    @Override
    @Transactional
    public boolean changeUserPassword(ChangeUserPasswordParam passwordParam) {
        User user = securityManager.getCurrentUser();
        user = userMapper.selectByPrimaryKey(user.getId());
        if (!BCrypt.checkpw(passwordParam.getOldPassword(), user.getPassword())) {
            Exceptions.tr(ParamException.class, "error.param.invalid", "resource.user.password");
        }
        User update = new User();
        update.setId(user.getId());
        update.setUpdateTime(new Date());
        update.setUpdateBy(user.getId());
        update.setPassword(BCrypt.hashpw(passwordParam.getNewPassword(), BCrypt.gensalt()));
        boolean success = userMapper.updateByPrimaryKeySelective(update) == 1;
        if (success) {
            log.info("User({}) password changed by {}", user.getUsername(), user.getUsername());
        }
        return success;
    }

    public boolean checkUserName(Object value) {
        return userMapper.selectByNameOrEmail(value.toString()) == null;
    }

    private boolean checkEmail(String email) {
        return userMapper.countEmail(email) == 0;
    }

    /**
     * 初始化用户:
     * 1: 单组织模式：默认加入组织
     * 2: 正常模式：创建默认组织 -> 初始化组织
     *
     * @param user 需要初始化的用户
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void initUser(User user) {
        if (Application.getCurrMode().equals(TenantManagementMode.TEAM)) {
            List<Organization> organizationList = orgMapper.list();
            if (organizationList.size() == 1) {
                Organization organization = organizationList.get(0);
                orgService.addUserToOrg(user.getId(), organization.getId());
                log.info("The user({}) is joined the default organization({}).", user.getUsername(), organization.getName());
                return;
            } else if (organizationList.size() > 1) {
                Exceptions.msg("There is more than one organization in team tenant-management-mode.");
            } else {
                Exceptions.msg("There is no organization to join.");
            }
        }
        //创建默认组织
        log.info("Create default organization for user({})", user.getUsername());
        Organization organization = new Organization();
        organization.setId(UUIDGenerator.generate());
        organization.setCreateBy(user.getId());
        organization.setCreateTime(new Date());
        organization.setName(user.getUsername() + "'s Organization");
        orgMapper.insert(organization);
        log.info("Init organization({})", organization.getName());
        orgService.initOrganization(organization, user);
    }

    @Override
    public boolean updateAvatar(String path) {
        User update = new User();
        update.setId(getCurrentUser().getId());
        update.setAvatar(path);
        return userMapper.updateByPrimaryKeySelective(update) == 1;
    }

    @Override
    public String login(PasswordToken passwordToken) {
        try {
            securityManager.login(passwordToken);
        } catch (Exception e) {
            String tokenStr = ldapLogin(passwordToken);
            if (StringUtils.isNotBlank(tokenStr)) {
                return tokenStr;
            }
            log.error("Login error ({} {})", passwordToken.getSubject(), passwordToken.getPassword());
            Exceptions.msg("login.fail");
            return null;
        }
        User user = userMapper.selectByNameOrEmail(passwordToken.getSubject());
        if (user == null) {
            Exceptions.tr(AuthException.class, "login.fail");
        }
        return JwtUtils.toJwtString(JwtUtils.createJwtToken(user));
    }

    private String ldapLogin(PasswordToken passwordToken) {
        String token = "";
        try {
            log.info("try to login with ldap ({}).", passwordToken.getSubject());
            ExternalRegisterService externalRegisterService = Application.getBean(ExternalRegisterService.class);
            token = externalRegisterService.ldapRegister(passwordToken.getSubject(), passwordToken.getPassword());
            if (StringUtils.isNotBlank(token)) {
                securityManager.login(token);
            }
        } catch (Exception e) {
            Exceptions.e(e);
        }
        return token;
    }


    @Override
    public String forgetPassword(UserIdentityType type, String principal) {
        User user = null;
        switch (type) {
            case EMAIL:
                user = userMapper.selectByEmail(principal);
                break;
            case USERNAME:
                user = userMapper.selectByUsername(principal);
                break;
            default:
        }
        if (user == null) {
            Exceptions.notFound("resource.user");
        }
        try {
            String verifyCode = SecurityUtils.randomPassword();
            user.setPassword(verifyCode);
            mailService.sendVerifyCode(user);
            return JwtUtils.toJwtString(new PasswordToken(user.getUsername(), verifyCode, System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Verify Code Mail send error", e);
            Exceptions.tr(BaseException.class, "message.email.send.error");
        }
        return null;
    }

    @Override
    public boolean resetPassword(UserResetPasswordParam passwordParam) {
        boolean valid = JwtUtils.validTimeout(passwordParam.getToken());
        if (!valid) {
            Exceptions.tr(ParamException.class, "message.user.verify.code.timeout");
        }
        JwtToken jwtToken = JwtUtils.toJwtToken(passwordParam.getToken());
        if (jwtToken.getPwdHash() != passwordParam.getVerifyCode().hashCode()) {
            Exceptions.tr(ParamException.class, "message.user.verify.code.error");
        }
        User user = userMapper.selectByUsername(jwtToken.getSubject());
        if (user == null) {
            Exceptions.notFound("resource.not-exist", "resource.user");
        }
        User update = new User();
        update.setId(user.getId());
        update.setUpdateBy(user.getId());
        update.setUpdateTime(new Date());
        update.setPassword(BCrypt.hashpw(passwordParam.getNewPassword(), BCrypt.gensalt()));
        return userMapper.updateByPrimaryKeySelective(update) == 1;
    }

    @Override
    public User externalRegist(OAuth2AuthenticationToken oauthAuthToken) throws ServerException {
        OAuth2User oauthUser = oauthAuthToken.getPrincipal();

        User user = getUserByName(oauthUser.getName());
        if (user != null) {
            return user;
        }
        user = new User();

        String emailMapping = getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.email", oauthAuthToken.getAuthorizedClientRegistrationId()));
        String nameMapping = getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.name", oauthAuthToken.getAuthorizedClientRegistrationId()));
        String avatarMapping = getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.avatar", oauthAuthToken.getAuthorizedClientRegistrationId()));
        JSONObject jsonObj = new JSONObject(oauthUser.getAttributes());

        user.setId(UUIDGenerator.generate());
        user.setCreateBy(user.getId());
        user.setCreateTime(new Date());
        user.setName(JsonPath.read(jsonObj, nameMapping));
        user.setUsername(oauthUser.getName());
        user.setActive(true);
        //todo: oauth2登录后需要设置随机密码，此字段作为密文，显然无法对应原文，即不会有任何密码对应以下值
        user.setPassword(BCrypt.hashpw("xxx", BCrypt.gensalt()));
        if (emailMapping != null) {
            user.setEmail(JsonPath.read(jsonObj, emailMapping));
        }
        if (avatarMapping != null) {
            user.setAvatar(JsonPath.read(jsonObj, avatarMapping));
        }
        int insert = userMapper.insert(user);
        if (insert > 0) {
            return user;
        } else {
            log.info("regist fail: {}", oauthUser.getName());
            throw new ServerException("regist fail: unspecified error");
        }
    }

    @Override
    @Transactional
    public User addUserToOrg(UserAddParam userAddParam, String orgId) throws MessagingException, UnsupportedEncodingException {
        securityManager.requireOrgOwner(orgId);
        if (StringUtils.isBlank(userAddParam.getPassword())) {
            userAddParam.setPassword(Const.USER_DEFAULT_PSW);
        }
        if (StringUtils.isBlank(userAddParam.getEmail())) {
            String str = userAddParam.getUsername() + LocalDate.now();
            userAddParam.setEmail(DigestUtils.md5Hex(str) + "@datart.generate");
        }
        UserRegisterParam userRegisterParam = new UserRegisterParam();
        BeanUtils.copyProperties(userAddParam, userRegisterParam);
        register(userRegisterParam, false);
        User user = this.userMapper.selectByUsername(userAddParam.getUsername());
        user.setName(userAddParam.getName());
        user.setDescription(userAddParam.getDescription());
        this.userMapper.updateByPrimaryKeySelective(user);
        orgService.addUserToOrg(user.getId(), orgId);
        roleService.updateRolesForUser(user.getId(), orgId, userAddParam.getRoleIds());
        return user;
    }

    @Override
    @Transactional
    public boolean deleteUserFromOrg(String orgId, String userId) {
        securityManager.requireOrgOwner(orgId);
        //删除组织关联
        orgService.removeUser(orgId, userId);
        //删除角色关联
        RelRoleUserMapperExt rruMapper = Application.getBean(RelRoleUserMapperExt.class);
        List<String> roleIds = rruMapper.listByUserId(userId).stream().map(Role::getId).collect(Collectors.toList());
        rruMapper.deleteByUserAndRoles(userId, roleIds);
        Role role = roleService.getPerUserRole(orgId, userId);
        if (role != null) {
            roleService.delete(role.getId());
        }
        //删除用户信息
        UserSettingService orgSettingService = Application.getBean(UserSettingService.class);
        orgSettingService.deleteByUserId(userId);
        userMapper.deleteByPrimaryKey(userId);
        return true;
    }

    @Override
    public boolean updateUserFromOrg(UserUpdateByIdParam userUpdateParam, String orgId) {
        securityManager.requireOrgOwner(orgId);
        User user = retrieve(userUpdateParam.getId());
        if (!user.getEmail().equals(userUpdateParam.getEmail()) && !checkEmail(userUpdateParam.getEmail())) {
            log.error("The email({}) has been registered", userUpdateParam.getEmail());
            Exceptions.tr(ParamException.class, "error.param.occupied", "resource.user.email");
        }
        if (StringUtils.isBlank(userUpdateParam.getPassword())) {
            userUpdateParam.setPassword(user.getPassword());
        } else if (!userUpdateParam.getPassword().equals(user.getPassword())) {
            userUpdateParam.setPassword(BCrypt.hashpw(userUpdateParam.getPassword(), BCrypt.gensalt()));
        }
        roleService.updateRolesForUser(user.getId(), orgId, userUpdateParam.getRoleIds());
        return update(userUpdateParam);
    }

    @Override
    public UserUpdateByIdParam selectUserById(String userId, String orgId) {
        securityManager.requireOrgOwner(orgId);
        UserUpdateByIdParam res = new UserUpdateByIdParam();
        User user = this.userMapper.selectByPrimaryKey(userId);
        BeanUtils.copyProperties(user, res);
        Set<String> roleIds = roleService.listUserRoles(orgId, userId).stream().map(Role::getId).collect(Collectors.toSet());
        res.setRoleIds(roleIds);
        return res;
    }

    @Override
    @Transactional
    public boolean setupUser(UserRegisterParam user) throws MessagingException, UnsupportedEncodingException {
        boolean res;
        if (Application.getCurrMode().equals(TenantManagementMode.PLATFORM)) {
            res = register(user, false);
            return res;
        }
        Organization organization = orgService.checkTeamOrg();
        if (organization == null) {
            Application.setCurrMode(TenantManagementMode.PLATFORM);
            res = register(user, false);
            Application.setCurrMode(TenantManagementMode.TEAM);
        } else {
            res = register(user, false);
            User setupUser = userMapper.selectByNameOrEmail(user.getUsername());
            if (setupUser != null) {
                securityManager.runAs(setupUser.getUsername());
                Role role = roleService.getDefaultMapper().selectOrgOwnerRole(organization.getId());
                if (role == null) {
                    orgService.createDefaultRole(RoleType.ORG_OWNER, setupUser, organization);
                }
                roleService.grantOrgOwner(organization.getId(), setupUser.getId(), false);
            }
        }
        return res;
    }

    @Override
    public void requirePermission(User entity, int permission) {

    }

}

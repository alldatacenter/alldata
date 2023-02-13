/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.user;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.DateUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.util.RSAUtils;
import org.apache.inlong.manager.common.util.SHAUtils;
import org.apache.inlong.manager.dao.entity.UserEntity;
import org.apache.inlong.manager.dao.mapper.UserEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserLoginLockStatus;
import org.apache.inlong.manager.pojo.user.UserLoginRequest;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User service layer implementation
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final Integer SECRET_KEY_SIZE = 16;

    /**
     * locked time, the unit is minute
     */
    private static final Integer LOCKED_TIME = 3;
    private static final Integer LOCKED_THRESHOLD = 10;

    private final Map<String, UserLoginLockStatus> loginLockStatusMap = new ConcurrentHashMap<>();

    @Autowired
    private UserEntityMapper userMapper;

    @Override
    public Integer save(UserRequest request, String currentUser) {
        String username = request.getName();
        UserEntity userExists = userMapper.selectByName(username);
        String password = request.getPassword();
        Preconditions.checkNull(userExists, "username [" + username + "] already exists");
        Preconditions.checkTrue(StringUtils.isNotBlank(password), "password cannot be blank");

        UserEntity entity = new UserEntity();
        entity.setName(username);
        entity.setPassword(SHAUtils.encrypt(password));
        entity.setAccountType(request.getAccountType());
        entity.setDueDate(DateUtils.getExpirationDate(request.getValidDays()));
        entity.setCreator(currentUser);
        entity.setModifier(currentUser);
        entity.setExtParams(request.getExtParams());
        try {
            Map<String, String> keyPairs = RSAUtils.generateRSAKeyPairs();
            String publicKey = keyPairs.get(RSAUtils.PUBLIC_KEY);
            String privateKey = keyPairs.get(RSAUtils.PRIVATE_KEY);
            String secretKey = RandomStringUtils.randomAlphanumeric(SECRET_KEY_SIZE);
            Integer encryptVersion = AESUtils.getCurrentVersion(null);
            entity.setEncryptVersion(encryptVersion);
            entity.setPublicKey(AESUtils.encryptToString(publicKey.getBytes(StandardCharsets.UTF_8), encryptVersion));
            entity.setPrivateKey(AESUtils.encryptToString(privateKey.getBytes(StandardCharsets.UTF_8), encryptVersion));
            entity.setSecretKey(AESUtils.encryptToString(secretKey.getBytes(StandardCharsets.UTF_8), encryptVersion));
        } catch (Exception e) {
            String errMsg = String.format("generate rsa key error: %s", e.getMessage());
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }

        Preconditions.checkTrue(userMapper.insert(entity) > 0, "Create user failed");
        LOGGER.debug("success to create user info={}", request);
        return entity.getId();
    }

    @Override
    public UserInfo getById(Integer userId, String currentUser) {
        Preconditions.checkNotNull(userId, "User id cannot be null");
        UserEntity entity = userMapper.selectById(userId);
        Preconditions.checkNotNull(entity, "User not exists with id " + userId);

        UserEntity curUser = userMapper.selectByName(currentUser);
        Preconditions.checkTrue(Objects.equals(UserTypeEnum.ADMIN.getCode(), curUser.getAccountType())
                || Objects.equals(entity.getName(), currentUser),
                "Current user does not have permission to get other users' info");

        UserInfo result = new UserInfo();
        result.setId(entity.getId());
        result.setName(entity.getName());
        result.setValidDays(DateUtils.getValidDays(entity.getCreateTime(), entity.getDueDate()));
        result.setAccountType(entity.getAccountType());
        result.setVersion(entity.getVersion());

        if (StringUtils.isNotBlank(entity.getSecretKey()) && StringUtils.isNotBlank(entity.getPublicKey())) {
            try {
                // decipher according to stored key version
                // note that if the version is null then the string is treated as unencrypted plain text
                Integer version = entity.getEncryptVersion();
                byte[] secretKeyBytes = AESUtils.decryptAsString(entity.getSecretKey(), version);
                byte[] publicKeyBytes = AESUtils.decryptAsString(entity.getPublicKey(), version);
                result.setSecretKey(new String(secretKeyBytes, StandardCharsets.UTF_8));
                result.setPublicKey(new String(publicKeyBytes, StandardCharsets.UTF_8));
            } catch (Exception e) {
                String errMsg = String.format("decryption error: %s", e.getMessage());
                LOGGER.error(errMsg, e);
                throw new BusinessException(errMsg);
            }
        }

        LOGGER.debug("success to get user info by id={}", userId);
        return result;
    }

    @Override
    public UserInfo getByName(String name) {
        Preconditions.checkNotNull(name, "User name cannot be null");
        UserEntity entity = userMapper.selectByName(name);
        if (entity == null) {
            return null;
        }
        UserInfo userInfo = CommonBeanUtils.copyProperties(entity, UserInfo::new);
        userInfo.setValidDays(DateUtils.getValidDays(entity.getCreateTime(), entity.getDueDate()));
        return userInfo;
    }

    @Override
    public PageResult<UserInfo> list(UserRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<UserEntity> entityPage = (Page<UserEntity>) userMapper.selectByCondition(request);
        List<UserInfo> userList = CommonBeanUtils.copyListProperties(entityPage, UserInfo::new);

        // Check whether the user account has expired
        userList.forEach(entity -> entity.setStatus(entity.getDueDate().after(new Date()) ? "valid" : "invalid"));

        PageResult<UserInfo> pageResult = new PageResult<>(userList, entityPage.getTotal(),
                entityPage.getPageNum(), entityPage.getPageSize());

        LOGGER.debug("success to list users for request={}, result size={}", request, pageResult.getTotal());
        return pageResult;
    }

    @Override
    public Integer update(UserRequest request, String currentUser) {
        LOGGER.debug("begin to update user info={} by {}", request, currentUser);
        Preconditions.checkNotNull(request, "Userinfo cannot be null");
        Preconditions.checkNotNull(request.getId(), "User id cannot be null");

        // Whether the current user is a manager
        UserEntity currentUserEntity = userMapper.selectByName(currentUser);
        String updateName = request.getName();
        boolean isAdmin = Objects.equals(UserTypeEnum.ADMIN.getCode(), currentUserEntity.getAccountType());
        Preconditions.checkTrue(isAdmin || Objects.equals(updateName, currentUser),
                "You are not a manager and do not have permission to update other users");

        // manager cannot set himself as an ordinary
        boolean managerToOrdinary = isAdmin
                && Objects.equals(UserTypeEnum.OPERATOR.getCode(), request.getAccountType())
                && Objects.equals(currentUser, updateName);
        Preconditions.checkFalse(managerToOrdinary, "You are a manager and you cannot change to an ordinary user");

        // target username must not exist
        UserEntity updateUserEntity = userMapper.selectById(request.getId());
        Preconditions.checkNotNull(updateUserEntity, "User not exists with id=" + request.getId());
        String errMsg = String.format("user has already updated with username=%s, curVersion=%s",
                updateName, request.getVersion());
        if (!Objects.equals(updateUserEntity.getVersion(), request.getVersion())) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        UserEntity targetUserEntity = userMapper.selectByName(updateName);
        Preconditions.checkTrue(Objects.isNull(targetUserEntity)
                || Objects.equals(targetUserEntity.getName(), updateUserEntity.getName()),
                "Username [" + updateName + "] already exists");

        // if the current user is not a manager, needs to check the password before updating user info
        if (!isAdmin) {
            String oldPassword = request.getPassword();
            String oldPasswordHash = SHAUtils.encrypt(oldPassword);
            Preconditions.checkTrue(oldPasswordHash.equals(updateUserEntity.getPassword()), "Old password is wrong");
            Integer validDays = DateUtils.getValidDays(updateUserEntity.getCreateTime(), updateUserEntity.getDueDate());
            Preconditions.checkTrue((request.getValidDays() <= validDays),
                    "Ordinary users are not allowed to add valid days");
            Preconditions.checkTrue(Objects.equals(updateUserEntity.getAccountType(), request.getAccountType()),
                    "Ordinary users are not allowed to update account type");
        }

        // update password
        if (!StringUtils.isBlank(request.getNewPassword())) {
            String newPasswordHash = SHAUtils.encrypt(request.getNewPassword());
            updateUserEntity.setPassword(newPasswordHash);
        }
        updateUserEntity.setDueDate(DateUtils.getExpirationDate(request.getValidDays()));
        updateUserEntity.setAccountType(request.getAccountType());
        updateUserEntity.setName(updateName);
        updateUserEntity.setExtParams(request.getExtParams());

        int rowCount = userMapper.updateByPrimaryKeySelective(updateUserEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        LOGGER.debug("success to update user info={} by {}", request, currentUser);
        return updateUserEntity.getId();
    }

    @Override
    public Boolean delete(Integer userId, String currentUser) {
        Preconditions.checkNotNull(userId, "User id should not be empty");

        // Whether the current user is an administrator
        UserEntity curUser = userMapper.selectByName(currentUser);
        UserEntity entity = userMapper.selectById(userId);
        Preconditions.checkTrue(curUser.getAccountType().equals(UserTypeEnum.ADMIN.getCode()),
                "Current user is not a manager and does not have permission to delete users");
        Preconditions.checkTrue(!Objects.equals(entity.getName(), currentUser),
                "Current user does not have permission to delete himself");
        userMapper.deleteById(userId);

        LOGGER.debug("success to delete user by id={}, current user={}", userId, currentUser);
        return true;
    }

    /**
     * This implementation is just to intercept some error requests and reduce the pressure on the database.
     * <p/>
     * This is a memory-based implementation. There is a problem with concurrency security when there are
     * multiple service nodes because the data in memory cannot be shared.
     *
     * @param req username login request
     */
    @Override
    public void login(UserLoginRequest req) {
        String username = req.getUsername();
        UserLoginLockStatus userLoginLockStatus = loginLockStatusMap.getOrDefault(username, new UserLoginLockStatus());
        LocalDateTime lockoutTime = userLoginLockStatus.getLockoutTime();
        if (lockoutTime != null && lockoutTime.isAfter(LocalDateTime.now())) {
            // part of a minute counts as one minute
            long waitMinutes = Duration.between(LocalDateTime.now(), lockoutTime).toMinutes() + 1;
            throw new BusinessException("account has been locked, please try again in " + waitMinutes + " minutes");
        }

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, req.getPassword());
        try {
            subject.login(token);
        } catch (AuthenticationException e) {
            LOGGER.error("login error for request {}", req, e);
            int loginErrorCount = userLoginLockStatus.getLoginErrorCount() + 1;

            if (loginErrorCount % LOCKED_THRESHOLD == 0) {
                LocalDateTime lockedTime = LocalDateTime.now().plusMinutes(LOCKED_TIME);
                userLoginLockStatus.setLockoutTime(lockedTime);
                LOGGER.error("account {} is locked, lockout time: {}", username, lockedTime);
            }
            userLoginLockStatus.setLoginErrorCount(loginErrorCount);

            loginLockStatusMap.put(username, userLoginLockStatus);
            throw e;
        }

        LoginUserUtils.setUserLoginInfo((UserInfo) subject.getPrincipal());

        // login successfully, clear error count
        userLoginLockStatus.setLoginErrorCount(0);
        loginLockStatusMap.put(username, userLoginLockStatus);
    }

}

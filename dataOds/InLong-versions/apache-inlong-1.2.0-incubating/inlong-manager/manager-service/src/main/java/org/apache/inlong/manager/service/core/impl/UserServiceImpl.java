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

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.pojo.user.PasswordChangeRequest;
import org.apache.inlong.manager.common.pojo.user.UserDetailListVO;
import org.apache.inlong.manager.common.pojo.user.UserDetailPageRequest;
import org.apache.inlong.manager.common.pojo.user.UserInfo;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.util.SmallTools;
import org.apache.inlong.manager.dao.entity.UserEntity;
import org.apache.inlong.manager.dao.entity.UserEntityExample;
import org.apache.inlong.manager.dao.entity.UserEntityExample.Criteria;
import org.apache.inlong.manager.dao.mapper.UserEntityMapper;
import org.apache.inlong.manager.service.core.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.apache.inlong.manager.common.util.SmallTools.getOverDueDate;

/**
 * User service layer implementation
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserEntityMapper userMapper;

    @Override
    public UserEntity getByName(String username) {
        UserEntityExample example = new UserEntityExample();
        example.createCriteria().andNameEqualTo(username);
        List<UserEntity> list = userMapper.selectByExample(example);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public UserInfo getById(Integer userId) {
        Preconditions.checkNotNull(userId, "User id should not be empty");

        UserEntity entity = userMapper.selectByPrimaryKey(userId);
        Preconditions.checkNotNull(entity, "User not exists with id " + userId);

        UserInfo result = new UserInfo();
        result.setId(entity.getId());
        result.setUsername(entity.getName());
        result.setValidDays(SmallTools.getValidDays(entity.getCreateTime(), entity.getDueDate()));
        result.setType(entity.getAccountType());

        log.debug("success to get user info by id={}", userId);
        return result;
    }

    @Override
    public boolean create(UserInfo userInfo) {
        String username = userInfo.getUsername();
        UserEntity userExists = getByName(username);
        Preconditions.checkNull(userExists, "User [" + username + "] already exists");

        UserEntity entity = new UserEntity();
        entity.setAccountType(userInfo.getType());
        entity.setPassword(SmallTools.passwordMd5(userInfo.getPassword()));
        entity.setDueDate(getOverDueDate(userInfo.getValidDays()));
        entity.setCreateBy(LoginUserUtils.getLoginUserDetail().getUserName());
        entity.setName(username);
        entity.setCreateTime(new Date());
        Preconditions.checkTrue(userMapper.insert(entity) > 0, "Create user failed");

        log.debug("success to create user info={}", userInfo);
        return true;
    }

    @Override
    public int update(UserInfo userInfo, String currentUser) {
        Preconditions.checkNotNull(userInfo, "User info should not be null");
        Preconditions.checkNotNull(userInfo.getId(), "User id should not be null");

        // Whether the current user is an administrator
        UserEntity currentUserEntity = getByName(currentUser);
        Preconditions.checkTrue(currentUserEntity.getAccountType().equals(UserTypeEnum.Admin.getCode()),
                "The current user is not a manager and does not have permission to update users");

        UserEntity entity = userMapper.selectByPrimaryKey(userInfo.getId());
        Preconditions.checkNotNull(entity, "User not exists with id " + userInfo.getId());

        // update password by updatePassword()
        entity.setDueDate(getOverDueDate(userInfo.getValidDays()));
        entity.setAccountType(userInfo.getType());
        entity.setName(userInfo.getUsername());

        log.debug("success to update user info={}", userInfo);
        return userMapper.updateByPrimaryKeySelective(entity);
    }

    @Override
    public Integer updatePassword(PasswordChangeRequest request) {
        String username = request.getName();
        UserEntity entity = getByName(username);
        Preconditions.checkNotNull(entity, "User [" + username + "] not exists");

        String oldPassword = request.getOldPassword();
        String oldPasswordMd = SmallTools.passwordMd5(oldPassword);
        Preconditions.checkTrue(oldPasswordMd.equals(entity.getPassword()), "Old password is wrong");

        String newPasswordMd5 = SmallTools.passwordMd5(request.getNewPassword());
        entity.setPassword(newPasswordMd5);

        log.debug("success to update user password, username={}", username);
        return userMapper.updateByPrimaryKey(entity);
    }

    @Override
    public Boolean delete(Integer userId, String currentUser) {
        Preconditions.checkNotNull(userId, "User id should not be empty");

        // Whether the current user is an administrator
        UserEntity entity = getByName(currentUser);
        Preconditions.checkTrue(entity.getAccountType().equals(UserTypeEnum.Admin.getCode()),
                "The current user is not a manager and does not have permission to delete users");

        userMapper.deleteByPrimaryKey(userId);
        log.debug("success to delete user by id={}, current user={}", userId, currentUser);
        return true;
    }

    @Override
    public PageInfo<UserDetailListVO> list(UserDetailPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        UserEntityExample example = new UserEntityExample();
        Criteria criteria = example.createCriteria();
        if (request.getUserName() != null) {
            criteria.andNameLike(request.getUserName() + "%");
        }

        Page<UserEntity> entityPage = (Page<UserEntity>) userMapper.selectByExample(example);
        List<UserDetailListVO> detailList = CommonBeanUtils.copyListProperties(entityPage, UserDetailListVO::new);
        // Check whether the user account has expired
        detailList.forEach(
                entity -> entity.setStatus(entity.getDueDate().after(new Date()) ? "valid" : "invalid"));
        PageInfo<UserDetailListVO> page = new PageInfo<>(detailList);
        page.setTotal(entityPage.getTotal());

        log.debug("success to list all user, result size={}", page.getTotal());
        return page;
    }

}

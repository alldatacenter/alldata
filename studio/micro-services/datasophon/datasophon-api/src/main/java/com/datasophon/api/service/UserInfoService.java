package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;

/**
 * 用户信息表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface UserInfoService extends IService<UserInfoEntity> {

    UserInfoEntity queryUser(String username, String password);

    Result createUser(UserInfoEntity userInfo);

    Result getUserListByPage(String username, Integer page, Integer pageSize);
}


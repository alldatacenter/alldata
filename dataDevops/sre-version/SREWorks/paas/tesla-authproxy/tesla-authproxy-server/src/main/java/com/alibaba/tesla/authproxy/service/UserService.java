package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;

/**
 * 老tesla主站用户表 users
 *
 * @author cdx
 * @date 2019/11/7 14:53
 */
public interface UserService {

    int saveUser(TeslaServiceUserDO teslaServiceUserDO);
}

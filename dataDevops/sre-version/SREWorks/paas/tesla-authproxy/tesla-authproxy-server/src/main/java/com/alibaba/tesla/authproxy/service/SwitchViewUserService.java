package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.SwitchViewUserDO;

import java.util.List;

/**
 * 切换视图用户服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface SwitchViewUserService {

    /**
     * 增加新用户
     *
     * @param empId 工号
     */
    int addUser(String empId) throws Exception;

    /**
     * 删除用户
     *
     * @param empId 工号
     */
    int deleteUser(String empId);

    /**
     * 获取当前全量的白名单用户列表
     */
    List<SwitchViewUserDO> select();

    /**
     * 根据 empId 获取用户
     *
     * @return true or false
     */
    SwitchViewUserDO getByEmpId(String empId);
}

package com.alibaba.tesla.authproxy.service;

import java.util.List;
import java.util.Map;

import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.vo.UserInfoVO;

/**
 * Describe:用户服务
 *
 * @author shuaibiao.csb@alibaba-inc.com
 * Created on 2018-03-20 16:09
 */
public interface BucUserService {
    /**
     * 由用户工号获取用户信息
     *
     * @param empId
     * @return
     */
    UserInfoVO getUserByEmpId(String empId);

    /**
     * 由关键字获取用户列表
     *
     * @param key
     * @return
     */
    List<UserInfoVO> queryByKey(String key);

    /**
     * 由工号集合获取用户信息
     *
     * @param empIds
     * @return
     */
    Map<String, UserInfoVO> findUsers(List<String> empIds);

    /**
     * 由工号tesla用户用户信息
     *
     * @param empId
     * @return
     */
    UserDO getTeslaUser(String empId);

    /**
     * 由域账户获取buc及tesla用户用户信息
     *
     * @param name
     * @return
     */
    Map<String, Object> getUserInfoByName(String name);

    /**
     * 由tesla用户名获取用户信息
     *
     * @param name
     * @return
     */
    UserDO getTeslaUserByName(String name);

    /**
     * 由工号获取buc及tesla用户用户信息
     *
     * @param empId
     * @return
     */
    Map<String, Object> getUserInfoByEmpId(String empId);

}

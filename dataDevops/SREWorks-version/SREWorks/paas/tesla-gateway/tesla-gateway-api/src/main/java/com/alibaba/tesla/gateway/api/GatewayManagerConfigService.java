package com.alibaba.tesla.gateway.api;

import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface GatewayManagerConfigService {

    /**
     * 获取doc admin users
     * @return empIds
     */
    List<String> listDocAdminUsers();

    /**
     * 删除doc admin user
     * @param empId
     * @return
     */
    boolean addDocAdminUser(String empId);

    /**
     * 移除 doc admin user
     * @param empId
     * @return
     */
    boolean removeDocAdminUser(String empId);

    /**
     * 是否是 dock admin user
     * @param empId empId
     * @return
     */
    boolean isDocAdminUser(String empId);
}

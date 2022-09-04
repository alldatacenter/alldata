package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.PermissionDO;

import java.util.List;

/**
 * <p>Title: AclAuthServiceManager.java<／p>
 * <p>Description: Tesla权限认证模式，此类用来对用户的权限数据进行管理，提供查询权限数据的服务 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年4月9日
 */
public interface TeslaPermissionService {

    /**
     * 查询用户的所有权限数据
     *
     * @param userId
     * @return
     * @throws ApplicationException
     */
    List<PermissionDO> listPermission(long userId) throws ApplicationException;

}

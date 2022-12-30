package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;

/**
 * <p>Description: 应用信息服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface TeslaAppService {

    /**
     * 添加应用信息
     *
     * @param appDo
     * @param loginUser
     * @return
     * @throws ApplicationException
     */
    int insert(AppDO appDo, UserDO loginUser) throws ApplicationException;

    /**
     * 根据APPID获取APP信息
     *
     * @param appId
     * @return
     * @throws ApplicationException
     */
    AppDO getByAppId(String appId) throws ApplicationException;

    /**
     * 更新应用信息
     *
     * @param appDo 应用
     * @return
     */
    int update(AppDO appDo);

}
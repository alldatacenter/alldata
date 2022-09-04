package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.AppExtDO;

/**
 * <p>Description: 外部应用服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface AppExtService {

    /**
     * 根据资源路径查询
     *
     * @param extAppName
     * @return
     */
    AppExtDO getByName(String extAppName);

}
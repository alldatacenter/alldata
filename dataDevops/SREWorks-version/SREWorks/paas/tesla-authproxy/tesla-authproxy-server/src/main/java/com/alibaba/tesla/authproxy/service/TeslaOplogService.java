package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.OplogDO;

/**
 * <p>Description: 操作日志服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface TeslaOplogService {

    /**
     * 添加操作日志
     *
     * @param oplogDo
     * @return
     * @throws ApplicationException
     */
    public int insert(OplogDO oplogDo) throws ApplicationException;

}

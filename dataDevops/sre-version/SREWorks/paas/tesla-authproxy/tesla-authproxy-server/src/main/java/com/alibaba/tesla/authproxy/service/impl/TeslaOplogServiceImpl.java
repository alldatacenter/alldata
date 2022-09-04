package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.mapper.OplogMapper;
import com.alibaba.tesla.authproxy.model.OplogDO;
import com.alibaba.tesla.authproxy.service.TeslaOplogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>Description: 操作日志服务实现 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service
@Slf4j
public class TeslaOplogServiceImpl implements TeslaOplogService {

    /**
     * 日志数据访问接口
     */
    @Autowired
    OplogMapper oplogMapper;

    @Override
    public int insert(OplogDO oplog) throws ApplicationException {
        return this.oplogMapper.insert(oplog);
    }
}

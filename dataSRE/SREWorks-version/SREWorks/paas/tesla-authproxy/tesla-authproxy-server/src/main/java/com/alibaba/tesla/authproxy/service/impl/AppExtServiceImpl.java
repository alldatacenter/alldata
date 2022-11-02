package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.mapper.AppExtMapper;
import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.service.AppExtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>Description: 应用信息服务实现 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service
public class AppExtServiceImpl implements AppExtService {

    @Autowired
    AppExtMapper appExtMapper;

    @Override
    public AppExtDO getByName(String extAppName) {
        return appExtMapper.getByName(extAppName);
    }
}

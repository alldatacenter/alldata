package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * <p>Description: 应用信息服务实现 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service("teslaAppService")
@Slf4j
@Transactional(rollbackForClassName = "*")
public class TeslaAppServiceImpl implements TeslaAppService {

    /**
     * 应用信息数据访问接口
     */
    @Autowired
    AppMapper appMapper;

    @Override
    public int insert(AppDO app, UserDO loginUser) throws ApplicationException {
        AppDO appDo = appMapper.getByAppId(app.getAppId());
        if (null != appDo) {
            throw new ApplicationException(TeslaResult.FAILURE, "error.app.exits");
        }
        return appMapper.insert(app);
    }

    @Override
    public AppDO getByAppId(String appId) throws ApplicationException {
        return appMapper.getByAppId(appId);
    }

    @Override
    public int update(AppDO appDo) {
        appDo.setGmtModified(new Date());
        return appMapper.updateByPrimaryKey(appDo);
    }
}

package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.oam.OamClient;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
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
@Service("teslaAppPrivateService")
@Slf4j
@Transactional(rollbackForClassName = "*")
public class TeslaAppPrivateServiceImpl implements TeslaAppService {

    /**
     * 应用信息数据访问接口
     */
    @Autowired
    AppMapper appMapper;

    @Autowired
    OamClient oamClient;

    @Autowired
    AuthPolicy authPolicy;

    @Autowired
    AuthProperties authProperties;

    /**
     * 创建应用并创建对应的oam角色
     * @param app
     * @return
     * @throws ApplicationException
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        rollbackForClassName = "*",
        isolation = Isolation.SERIALIZABLE
    )
    @Override
    public int insert(AppDO app, UserDO loginUser) throws ApplicationException {
        AppDO appDo = appMapper.getByAppId(app.getAppId());
        if (null == appDo) {
            appMapper.insert(app);
            log.info("app {} already exits, create oam role only", TeslaGsonUtil.toJson(app));
        } else {
            log.info("create app {} success, create oam now", TeslaGsonUtil.toJson(app));
        }

        // OXS 或环境下不作处理，直接返回
        if (Constants.ENVIRONMENT_OXS.equals(authProperties.getEnvironment())) {
            return 1;
        }

        String roleName = authPolicy.getAuthServiceManager().createRoleNameByAppId(app.getAppId());
        try {
            //oamClient.createRole(loginUser.getAliyunPk(), loginUser.getBid(), roleName, app.getMemo());
        } catch (Exception e) {
            log.error("create oam role failed, aliyunPk={}, loginUser.bid={}, roleName={}, app.Memo={}, message={}",
                loginUser.getAliyunPk(), loginUser.getBid(), roleName, app.getMemo(), e.getMessage());
        }
        return 1;
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

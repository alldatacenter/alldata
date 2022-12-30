package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.SpringContextHolder;
import com.alibaba.tesla.authproxy.interceptor.LoginInterceptor;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>Description: 权限策略管理，通过此类获取不同的登录和权限处理类 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Component
@Slf4j
public class AuthPolicy {

    @Autowired
    private AuthProperties authProperties;

    /**
     * 根据配置加载权限认证策略处理类
     *
     * @return
     */
    public AuthServiceManager getAuthServiceManager() {
        ClassLoader classLoader = AuthPolicy.class.getClassLoader();
        try {
            Class<AuthServiceManager> asm = (Class<AuthServiceManager>)classLoader.loadClass(
                authProperties.getAuthPolicy());
            if (log.isDebugEnabled()) {
                log.debug("权限认证策略->" + asm.getName());
            }
            return SpringContextHolder.getBean(asm);
        } catch (ClassNotFoundException e) {
            log.error("权限认证策略配置错误", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.policy");
        }
    }

    /**
     * 根据配置加载登录认证策略处理类
     *
     * @return
     */
    public LoginInterceptor getLoginServiceManager() {
        ClassLoader classLoader = AuthPolicy.class.getClassLoader();
        try {
            Class<LoginInterceptor> asm = (Class<LoginInterceptor>)classLoader.loadClass(
                authProperties.getLoginPolicy());
            if (log.isDebugEnabled()) {
                log.debug("登录认证策略->" + asm.getName());
            }
            return SpringContextHolder.getBean(asm);
        } catch (ClassNotFoundException e) {
            log.error("登录认证策略配置错误", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.policy");
        }
    }
}

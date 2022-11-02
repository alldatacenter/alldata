package com.alibaba.tesla.authproxy.lib.shiro;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityManager {

    @Autowired
    private DatabaseAuthorizingRealm realm;

    @Bean
    public DefaultSecurityManager defaultSecurityManager() {
        DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();
        defaultSecurityManager.setRealm(realm);
        defaultSecurityManager.setCacheManager(ehCacheManager());
        SecurityUtils.setSecurityManager(defaultSecurityManager);
        return defaultSecurityManager;
    }

    @Bean
    public EhCacheManager ehCacheManager(){
        System.out.println("ShiroConfiguration.getEhCacheManager()");
        EhCacheManager cacheManager = new EhCacheManager();
        cacheManager.setCacheManagerConfigFile("classpath:ehcache-shiro.xml");
        return cacheManager;
    }
}

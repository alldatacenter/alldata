/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.web.auth;

import org.apache.inlong.manager.common.auth.InlongShiro;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Inlong hiro config.
 */
@Configuration
public class ShiroConfig {

    @Resource
    private InlongShiro inlongShiro;

    @Bean
    public Collection<Realm> shiroRealms() {
        return inlongShiro.getShiroRealms();
    }

    @Bean
    public WebSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager) inlongShiro.getWebSecurityManager();
        securityManager.setRealms(shiroRealms());
        return securityManager;
    }

    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = (DefaultWebSessionManager) inlongShiro.getWebSessionManager();
        sessionManager.setGlobalSessionTimeout(1000 * 60 * 60);
        return sessionManager;
    }

    /**
     * Filter for annon / authc
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = inlongShiro.getShiroFilter(securityManager);
        return shiroFilterFactoryBean;
    }

    /**
     * Enable permission verification annotation
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor() {
        return inlongShiro.getAuthorizationAttributeSourceAdvisor(securityManager());
    }
}

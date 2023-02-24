/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.security;

import org.apache.atlas.web.filters.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.KeycloakLogoutHandler;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticatedActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakSecurityContextRequestFilter;
import org.keycloak.adapters.springsecurity.filter.QueryParamPresenceRequestMatcher;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.AtlasConstants.ATLAS_MIGRATION_MODE_FILENAME;
import static org.apache.atlas.web.filters.HeadersUtil.SERVER_KEY;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@KeycloakConfiguration
public class AtlasSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasSecurityConfig.class);

    private final AtlasAuthenticationProvider authenticationProvider;
    private final AtlasAuthenticationSuccessHandler successHandler;
    private final AtlasAuthenticationFailureHandler failureHandler;
    private final AtlasKnoxSSOAuthenticationFilter ssoAuthenticationFilter;
    private final AtlasAuthenticationFilter atlasAuthenticationFilter;
    private final AtlasCSRFPreventionFilter csrfPreventionFilter;
    private final AtlasAuthenticationEntryPoint atlasAuthenticationEntryPoint;

    // Our own Atlas filters need to be registered as well
    private final Configuration configuration;
    private final StaleTransactionCleanupFilter staleTransactionCleanupFilter;
    private final ActiveServerFilter activeServerFilter;

    public static final RequestMatcher KEYCLOAK_REQUEST_MATCHER = new OrRequestMatcher(new RequestMatcher[]{new AntPathRequestMatcher("/login.jsp"), new RequestHeaderRequestMatcher("Authorization"), new QueryParamPresenceRequestMatcher("access_token")});

    @Value("${keycloak.configurationFile:WEB-INF/keycloak.json}")
    private Resource keycloakConfigFileResource;
    @Autowired(required = false)
    private KeycloakConfigResolver keycloakConfigResolver;

    private final boolean keycloakEnabled;

    @Inject
    public AtlasSecurityConfig(AtlasKnoxSSOAuthenticationFilter ssoAuthenticationFilter,
                               AtlasCSRFPreventionFilter atlasCSRFPreventionFilter,
                               AtlasAuthenticationFilter atlasAuthenticationFilter,
                               AtlasAuthenticationProvider authenticationProvider,
                               AtlasAuthenticationSuccessHandler successHandler,
                               AtlasAuthenticationFailureHandler failureHandler,
                               AtlasAuthenticationEntryPoint atlasAuthenticationEntryPoint,
                               Configuration configuration,
                               StaleTransactionCleanupFilter staleTransactionCleanupFilter,
                               ActiveServerFilter activeServerFilter) {
        this.ssoAuthenticationFilter = ssoAuthenticationFilter;
        this.csrfPreventionFilter = atlasCSRFPreventionFilter;
        this.atlasAuthenticationFilter = atlasAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.atlasAuthenticationEntryPoint = atlasAuthenticationEntryPoint;
        this.configuration = configuration;
        this.staleTransactionCleanupFilter = staleTransactionCleanupFilter;
        this.activeServerFilter = activeServerFilter;

        this.keycloakEnabled = configuration.getBoolean(AtlasAuthenticationProvider.KEYCLOAK_AUTH_METHOD, false);
    }

    public AuthenticationEntryPoint getAuthenticationEntryPoint() throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint;

        if (keycloakEnabled) {
            KeycloakAuthenticationEntryPoint keycloakAuthenticationEntryPoint = new KeycloakAuthenticationEntryPoint(adapterDeploymentContext());
            keycloakAuthenticationEntryPoint.setRealm("atlas.com");
            keycloakAuthenticationEntryPoint.setLoginUri("/login.jsp");
            authenticationEntryPoint = keycloakAuthenticationEntryPoint;
        } else {
            LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPointMap = new LinkedHashMap<>();
            entryPointMap.put(new RequestHeaderRequestMatcher(HeadersUtil.USER_AGENT_KEY, HeadersUtil.USER_AGENT_VALUE), atlasAuthenticationEntryPoint);
            AtlasDelegatingAuthenticationEntryPoint basicAuthenticationEntryPoint = new AtlasDelegatingAuthenticationEntryPoint(entryPointMap);
            authenticationEntryPoint = basicAuthenticationEntryPoint;
        }
        return authenticationEntryPoint;
    }

    public DelegatingAuthenticationEntryPoint getDelegatingAuthenticationEntryPoint() throws Exception {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPointMap = new LinkedHashMap<>();
        entryPointMap.put(new RequestHeaderRequestMatcher(HeadersUtil.USER_AGENT_KEY, HeadersUtil.USER_AGENT_VALUE), atlasAuthenticationEntryPoint);
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(entryPointMap);
        entryPoint.setDefaultEntryPoint(getAuthenticationEntryPoint());
        return entryPoint;
    }

    @Inject
    protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) {
        authenticationManagerBuilder.authenticationProvider(authenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        List<String> matchers = new ArrayList<>(
          Arrays.asList("/css/**","/n/css/**",
            "/img/**",
            "/n/img/**",
            "/libs/**",
            "/n/libs/**",
            "/js/**",
            "/n/js/**",
            "/ieerror.html",
            "/migration-status.html",
            "/api/atlas/admin/status"));

        if (!keycloakEnabled) {
            matchers.add("/login.jsp");
        }

        web.ignoring()
                .antMatchers(matchers.toArray(new String[matchers.size()]));
    }

    protected void configure(HttpSecurity httpSecurity) throws Exception {
        //@formatter:off
        httpSecurity
                .authorizeRequests().anyRequest().authenticated()
                .and()
                    .headers()
                .addHeaderWriter(new StaticHeadersWriter(HeadersUtil.CONTENT_SEC_POLICY_KEY, HeadersUtil.headerMap.get(HeadersUtil.CONTENT_SEC_POLICY_KEY)))
                .addHeaderWriter(new StaticHeadersWriter(SERVER_KEY, HeadersUtil.headerMap.get(SERVER_KEY)))
                        .and()
                    .servletApi()
                .and()
                    .csrf().disable()
                    .sessionManagement()
                    .enableSessionUrlRewriting(false)
                    .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                    .sessionFixation()
                    .newSession()
                .and()
                .httpBasic()
                .authenticationEntryPoint(getDelegatingAuthenticationEntryPoint())
                .and()
                    .formLogin()
                        .loginPage("/login.jsp")
                        .loginProcessingUrl("/j_spring_security_check")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .usernameParameter("j_username")
                        .passwordParameter("j_password")
                .and()
                    .logout()
                        .logoutSuccessUrl("/login.jsp")
                        .deleteCookies("ATLASSESSIONID")
                        .logoutUrl("/logout.html");

        //@formatter:on

        boolean configMigrationEnabled = !StringUtils.isEmpty(configuration.getString(ATLAS_MIGRATION_MODE_FILENAME));
        if (configuration.getBoolean("atlas.server.ha.enabled", false) ||
                configMigrationEnabled) {
            if(configMigrationEnabled) {
                LOG.info("Atlas is in Migration Mode, enabling ActiveServerFilter");
            } else {
                LOG.info("Atlas is in HA Mode, enabling ActiveServerFilter");
            }
            httpSecurity.addFilterAfter(activeServerFilter, BasicAuthenticationFilter.class);
        }
        httpSecurity
                .addFilterAfter(staleTransactionCleanupFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(ssoAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(atlasAuthenticationFilter, SecurityContextHolderAwareRequestFilter.class)
                .addFilterAfter(csrfPreventionFilter, AtlasAuthenticationFilter.class);

        if (keycloakEnabled) {
            httpSecurity
              .logout().addLogoutHandler(keycloakLogoutHandler()).and()
              .addFilterBefore(keycloakAuthenticationProcessingFilter(), BasicAuthenticationFilter.class)
              .addFilterBefore(keycloakPreAuthActionsFilter(), LogoutFilter.class)
              .addFilterAfter(keycloakSecurityContextRequestFilter(), SecurityContextHolderAwareRequestFilter.class)
              .addFilterAfter(keycloakAuthenticatedActionsRequestFilter(), KeycloakSecurityContextRequestFilter.class);
        }
    }


    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    protected AdapterDeploymentContext adapterDeploymentContext() throws Exception {
        AdapterDeploymentContextFactoryBean factoryBean;
        String fileName = configuration.getString("atlas.authentication.method.keycloak.file");
        if (fileName != null && !fileName.isEmpty()) {
            keycloakConfigFileResource = new FileSystemResource(fileName);
            factoryBean = new AdapterDeploymentContextFactoryBean(keycloakConfigFileResource);
        } else {
            Configuration conf = configuration.subset("atlas.authentication.method.keycloak");
            AdapterConfig cfg = new AdapterConfig();
            cfg.setRealm(conf.getString("realm", "atlas.com"));
            cfg.setAuthServerUrl(conf.getString("auth-server-url", "https://localhost/auth"));
            cfg.setResource(conf.getString("resource", "none"));

            Map<String,Object> credentials = new HashMap<>();
            credentials.put("secret", conf.getString("credentials-secret", "nosecret"));
            cfg.setCredentials(credentials);
            KeycloakDeployment dep = KeycloakDeploymentBuilder.build(cfg);
            factoryBean = new AdapterDeploymentContextFactoryBean(new KeycloakConfigResolver() {
                @Override
                public KeycloakDeployment resolve(HttpFacade.Request request) {
                    return dep;
                }
            });
        }

        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    protected KeycloakPreAuthActionsFilter keycloakPreAuthActionsFilter() {
        return new KeycloakPreAuthActionsFilter(httpSessionManager());
    }

    @Bean
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }

    protected KeycloakLogoutHandler keycloakLogoutHandler() throws Exception {
        return new KeycloakLogoutHandler(adapterDeploymentContext());
    }

    @Bean
    protected KeycloakSecurityContextRequestFilter keycloakSecurityContextRequestFilter() {
        return new KeycloakSecurityContextRequestFilter();
    }

    @Bean
    protected KeycloakAuthenticatedActionsFilter keycloakAuthenticatedActionsRequestFilter() {
        return new KeycloakAuthenticatedActionsFilter();
    }

    @Bean
    protected KeycloakAuthenticationProcessingFilter keycloakAuthenticationProcessingFilter() throws Exception {
        KeycloakAuthenticationProcessingFilter filter = new KeycloakAuthenticationProcessingFilter(authenticationManagerBean(), KEYCLOAK_REQUEST_MATCHER);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        return filter;
    }
}

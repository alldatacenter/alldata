/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.atlas.web.security;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.web.model.User;
import org.apache.commons.configuration.ConfigurationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class AtlasPamAuthenticationProvider extends AtlasAbstractAuthenticationProvider {

    private static Logger LOG = LoggerFactory.getLogger(AtlasPamAuthenticationProvider.class);
    private boolean isDebugEnabled = LOG.isDebugEnabled();
    private static String loginModuleName = "org.apache.atlas.web.security.PamLoginModule";
    private static AppConfigurationEntry.LoginModuleControlFlag controlFlag =
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
    private Map<String, String> options = new HashMap<String, String>();
    private boolean groupsFromUGI;
    private DefaultJaasAuthenticationProvider jaasAuthenticationProvider =
            new DefaultJaasAuthenticationProvider();

    @PostConstruct
    public void setup() {
        setPamProperties();
        init();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = getPamAuthentication(authentication);
        if (auth != null && auth.isAuthenticated()) {
            return auth;
        } else {
            throw new AtlasAuthenticationException("PAM Authentication Failed");
        }
    }

    private Authentication getPamAuthentication(Authentication authentication) {
        if (isDebugEnabled) {
            LOG.debug("==> AtlasPamAuthenticationProvider getPamAuthentication");
        }
        try {
            String userName = authentication.getName();
            String userPassword = "";
            if (authentication.getCredentials() != null) {
                userPassword = authentication.getCredentials().toString();
            }

            // getting user authenticated
            if (userName != null && userPassword != null
                    && !userName.trim().isEmpty()
                    && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = getAuthorities(userName);

                final UserDetails principal = new User(userName, userPassword,
                        grantedAuths);

                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
                        principal, userPassword, grantedAuths);

                authentication = jaasAuthenticationProvider
                        .authenticate(finalAuthentication);

                if(groupsFromUGI) {
                    authentication = getAuthenticationWithGrantedAuthorityFromUGI(authentication);
                } else {
                    authentication = getAuthenticationWithGrantedAuthority(authentication);
                }
                return authentication;
            } else {
                return authentication;
            }

        } catch (Exception e) {
            LOG.debug("Pam Authentication Failed:", e);
        }
        if (isDebugEnabled) {
            LOG.debug("<== AtlasPamAuthenticationProvider getPamAuthentication : " + jaasAuthenticationProvider);
        }
        return authentication;
    }

    private void setPamProperties() {
        try {
            this.groupsFromUGI = ApplicationProperties.get().getBoolean("atlas.authentication.method.pam.ugi-groups", true);
            Properties properties = ConfigurationConverter.getProperties(ApplicationProperties.get()
                    .subset("atlas.authentication.method.pam"));
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                options.put(key, value);
            }
            if (!options.containsKey("service")) {
                options.put("service", "atlas-login");
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("AtlasPAMAuthenticationProvider{groupsFromUGI= "+ groupsFromUGI +'\'' +
                        ", options=" + options +
                         '}');
            }

        } catch (Exception e) {
            LOG.error("Exception while setLdapProperties", e);
        }
    }

    private void init() {
        try {
            AppConfigurationEntry appConfigurationEntry = new AppConfigurationEntry(
                    loginModuleName, controlFlag, options);
            AppConfigurationEntry[] appConfigurationEntries = new AppConfigurationEntry[]{appConfigurationEntry};
            Map<String, AppConfigurationEntry[]> appConfigurationEntriesOptions =
                    new HashMap<String, AppConfigurationEntry[]>();
            appConfigurationEntriesOptions.put("SPRINGSECURITY",
                    appConfigurationEntries);
            Configuration configuration = new InMemoryConfiguration(
                    appConfigurationEntriesOptions);
            jaasAuthenticationProvider.setConfiguration(configuration);
            UserAuthorityGranter authorityGranter = new UserAuthorityGranter();
            UserAuthorityGranter[] authorityGranters = new UserAuthorityGranter[]{authorityGranter};
            jaasAuthenticationProvider.setAuthorityGranters(authorityGranters);
            jaasAuthenticationProvider.afterPropertiesSet();

            if(LOG.isDebugEnabled()) {
                LOG.debug("AtlasPAMAuthenticationProvider{" +
                        "jaasAuthenticationProvider='" + jaasAuthenticationProvider + '\'' +
                        ", loginModuleName='" + loginModuleName + '\'' +
                        ", controlFlag='" + controlFlag + '\'' +
                        ", options='" + options + '}');
            }


        } catch (Exception e) {
            LOG.error("Failed to init PAM Authentication", e);
        }
    }
}

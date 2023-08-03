/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.security;

import org.apache.atlas.ApplicationProperties;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
@Scope("prototype")
public class AtlasAuthenticationProvider extends AtlasAbstractAuthenticationProvider {
    private static final Logger LOG = LoggerFactory
            .getLogger(AtlasAuthenticationProvider.class);

    private boolean fileAuthenticationMethodEnabled = true;
    private boolean pamAuthenticationEnabled = false;
    private boolean keycloakAuthenticationEnabled = false;

    private String ldapType = "NONE";
    public static final String FILE_AUTH_METHOD = "atlas.authentication.method.file";
    public static final String LDAP_AUTH_METHOD = "atlas.authentication.method.ldap";
    public static final String LDAP_TYPE = "atlas.authentication.method.ldap.type";
    public static final String PAM_AUTH_METHOD = "atlas.authentication.method.pam";
    public static final String KEYCLOAK_AUTH_METHOD = "atlas.authentication.method.keycloak";



    private boolean ssoEnabled = false;

    final AtlasLdapAuthenticationProvider ldapAuthenticationProvider;

    final AtlasFileAuthenticationProvider fileAuthenticationProvider;

    final AtlasADAuthenticationProvider adAuthenticationProvider;

    final AtlasPamAuthenticationProvider pamAuthenticationProvider;

    final AtlasKeycloakAuthenticationProvider atlasKeycloakAuthenticationProvider;

    @Inject
    public AtlasAuthenticationProvider(AtlasLdapAuthenticationProvider ldapAuthenticationProvider,
                                       AtlasFileAuthenticationProvider fileAuthenticationProvider,
                                       AtlasADAuthenticationProvider adAuthenticationProvider,
                                       AtlasPamAuthenticationProvider pamAuthenticationProvider,
                                       AtlasKeycloakAuthenticationProvider atlasKeycloakAuthenticationProvider) {
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.fileAuthenticationProvider = fileAuthenticationProvider;
        this.adAuthenticationProvider = adAuthenticationProvider;
        this.pamAuthenticationProvider = pamAuthenticationProvider;
        this.atlasKeycloakAuthenticationProvider = atlasKeycloakAuthenticationProvider;
    }

    @PostConstruct
    void setAuthenticationMethod() {
        try {
            Configuration configuration = ApplicationProperties.get();

            this.fileAuthenticationMethodEnabled = configuration.getBoolean(FILE_AUTH_METHOD, true);

            this.pamAuthenticationEnabled = configuration.getBoolean(PAM_AUTH_METHOD, false);

            this.keycloakAuthenticationEnabled = configuration.getBoolean(KEYCLOAK_AUTH_METHOD, false);

            boolean ldapAuthenticationEnabled = configuration.getBoolean(LDAP_AUTH_METHOD, false);

            if (ldapAuthenticationEnabled) {
                this.ldapType = configuration.getString(LDAP_TYPE, "NONE");
            } else {
                this.ldapType = "NONE";
            }
        } catch (Exception e) {
            LOG.error("Error while getting atlas.login.method application properties", e);
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        if(ssoEnabled){
            if (authentication != null){
                authentication = getSSOAuthentication(authentication);
                if(authentication!=null && authentication.isAuthenticated()){
                    return authentication;
                }
            }
        } else {

            if (ldapType.equalsIgnoreCase("LDAP")) {
                try {
                    authentication = ldapAuthenticationProvider.authenticate(authentication);
                } catch (Exception ex) {
                    LOG.error("Error while LDAP authentication", ex);
                }
            } else if (ldapType.equalsIgnoreCase("AD")) {
                try {
                    authentication = adAuthenticationProvider.authenticate(authentication);
                } catch (Exception ex) {
                    LOG.error("Error while AD authentication", ex);
                }
            } else if (pamAuthenticationEnabled) {
                try {
                    authentication = pamAuthenticationProvider.authenticate(authentication);
                } catch (Exception ex) {
                    LOG.error("Error while PAM authentication", ex);
                }
            } else if (keycloakAuthenticationEnabled) {
                try {
                    authentication = atlasKeycloakAuthenticationProvider.authenticate(authentication);
                } catch (Exception ex) {
                    LOG.error("Error while Keycloak authentication", ex);
                }
            }
        }

        if (authentication != null) {
            if (authentication.isAuthenticated()) {
                return authentication;
            } else if (fileAuthenticationMethodEnabled) {  // If the LDAP/AD/PAM authentication fails try the local filebased login method
                authentication = fileAuthenticationProvider.authenticate(authentication);

                if (authentication != null && authentication.isAuthenticated()) {
                    return authentication;
                }
            }
        }

        LOG.error("Authentication failed.");
        throw new AtlasAuthenticationException("Authentication failed.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (pamAuthenticationEnabled) {
            return pamAuthenticationProvider.supports(authentication);
        } else if (ldapType.equalsIgnoreCase("LDAP")) {
            return ldapAuthenticationProvider.supports(authentication);
        } else if (ldapType.equalsIgnoreCase("AD")) {
            return adAuthenticationProvider.supports(authentication);
        } else if (keycloakAuthenticationEnabled) {
            return atlasKeycloakAuthenticationProvider.supports(authentication);
        } else {
            return super.supports(authentication);
        }
    }

    public boolean isSsoEnabled() {
        return ssoEnabled;
    }

    public void setSsoEnabled(boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    private Authentication getSSOAuthentication(Authentication authentication) throws AuthenticationException{
        return authentication;
    }
}

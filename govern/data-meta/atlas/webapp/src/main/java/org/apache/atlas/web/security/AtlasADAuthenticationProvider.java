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
import org.apache.atlas.web.model.User;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

@Component
public class AtlasADAuthenticationProvider extends AtlasAbstractAuthenticationProvider {
    private static Logger LOG = LoggerFactory.getLogger(AtlasADAuthenticationProvider.class);

    private String adURL;
    private String adDomain;
    private String adBindDN;
    private String adBindPassword;
    private String adUserSearchFilter;
    private String adBase;
    private String adReferral;
    private String adDefaultRole;
    private boolean groupsFromUGI;

    @PostConstruct
    public void setup() {
        setADProperties();
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
            Authentication auth = getADBindAuthentication(authentication);
            if (auth != null && auth.isAuthenticated()) {
                return auth;
            } else {
                auth = getADAuthentication(authentication);
                if (auth != null && auth.isAuthenticated()) {
                    return auth;
                }
            }
            if (auth == null) {
                throw new AtlasAuthenticationException("AD Authentication Failed");
            }
            return auth;
    }

    private Authentication getADBindAuthentication (Authentication authentication) {
        try {
            String userName = authentication.getName();
            String userPassword = "";
            if (authentication.getCredentials() != null) {
                userPassword = authentication.getCredentials().toString();
            }

            LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(adURL);
            ldapContextSource.setUserDn(adBindDN);
            ldapContextSource.setPassword(adBindPassword);
            ldapContextSource.setReferral(adReferral);
            ldapContextSource.setCacheEnvironmentProperties(true);
            ldapContextSource.setAnonymousReadOnly(false);
            ldapContextSource.setPooled(true);
            ldapContextSource.afterPropertiesSet();

            FilterBasedLdapUserSearch userSearch=new FilterBasedLdapUserSearch(adBase, adUserSearchFilter,ldapContextSource);
            userSearch.setSearchSubtree(true);

            BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
            bindAuthenticator.setUserSearch(userSearch);
            bindAuthenticator.afterPropertiesSet();

			LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);

            if (userName != null && userPassword != null
                    && !userName.trim().isEmpty()
                    && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = getAuthorities(userName);
                final UserDetails principal = new User(userName, userPassword,
                        grantedAuths);
                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
                        principal, userPassword, grantedAuths);
                authentication = ldapAuthenticationProvider.authenticate(finalAuthentication);
                if (groupsFromUGI) {
                    authentication = getAuthenticationWithGrantedAuthorityFromUGI(authentication);
                }
                return authentication;
            } else {
                LOG.error("AD Authentication Failed userName or userPassword is null or empty");
                return null;
            }
        } catch (Exception e) {
            LOG.error("AD Authentication Failed:", e);
            return null;
        }
    }

    private Authentication getADAuthentication(Authentication authentication) {
        try {
            String userName = authentication.getName();
            String userPassword = "";
            if (authentication.getCredentials() != null) {
                userPassword = authentication.getCredentials().toString();
            }

            ActiveDirectoryLdapAuthenticationProvider adAuthenticationProvider =
                    new ActiveDirectoryLdapAuthenticationProvider(adDomain, adURL);
            adAuthenticationProvider.setConvertSubErrorCodesToExceptions(true);
			adAuthenticationProvider.setUseAuthenticationRequestCredentials(true);
            adAuthenticationProvider.setSearchFilter(adUserSearchFilter);

            if (userName != null && userPassword != null
                    && !userName.trim().isEmpty()
                    && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = getAuthorities(userName);
                final UserDetails principal = new User(userName, userPassword,
                        grantedAuths);
                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
                        principal, userPassword, grantedAuths);
                authentication = adAuthenticationProvider.authenticate(finalAuthentication);
                if(groupsFromUGI) {
                    authentication = getAuthenticationWithGrantedAuthorityFromUGI(authentication);
                }
                return authentication;
            } else {
                LOG.error("AD Authentication Failed userName or userPassword is null or empty");
                return null;
            }
        } catch (Exception e) {
            LOG.error("AD Authentication Failed:", e);
            return null;
        }
    }

    private void setADProperties() {
        try {

            Configuration configuration = ApplicationProperties.get();
            Properties properties = ConfigurationConverter.getProperties(configuration.subset("atlas.authentication.method.ldap.ad"));
            this.adDomain = properties.getProperty("domain");
            this.adURL = properties.getProperty("url");
            this.adBindDN = properties.getProperty("bind.dn");
            this.adBindPassword = properties.getProperty("bind.password");
            this.adUserSearchFilter = properties.getProperty("user.searchfilter");
            if (adUserSearchFilter==null || adUserSearchFilter.trim().isEmpty()) {
                adUserSearchFilter="(sAMAccountName={0})";
            }
            this.adBase = properties.getProperty("base.dn");
            this.adReferral = properties.getProperty("referral");
            this.adDefaultRole = properties.getProperty("default.role");

            this.groupsFromUGI = configuration.getBoolean("atlas.authentication.method.ldap.ugi-groups", true);

            if(LOG.isDebugEnabled()) {
                LOG.debug("AtlasADAuthenticationProvider{" +
                        "adURL='" + adURL + '\'' +
                        ", adDomain='" + adDomain + '\'' +
                        ", adBindDN='" + adBindDN + '\'' +
                        ", adUserSearchFilter='" + adUserSearchFilter + '\'' +
                        ", adBase='" + adBase + '\'' +
                        ", adReferral='" + adReferral + '\'' +
                        ", adDefaultRole='" + adDefaultRole + '\'' +
                        ", groupsFromUGI=" + groupsFromUGI +
                        '}');
            }


        } catch (Exception e) {
            LOG.error("Exception while setADProperties", e);
        }
    }

}

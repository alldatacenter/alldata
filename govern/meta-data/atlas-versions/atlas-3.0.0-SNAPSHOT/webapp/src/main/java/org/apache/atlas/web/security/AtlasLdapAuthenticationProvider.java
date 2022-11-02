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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

@Component
public class AtlasLdapAuthenticationProvider extends
        AtlasAbstractAuthenticationProvider {
    private static Logger LOG = LoggerFactory.getLogger(AtlasLdapAuthenticationProvider.class);
    private boolean isDebugEnabled = LOG.isDebugEnabled();

    private String ldapURL;
    private String ldapUserDNPattern;
    private String ldapGroupSearchBase;
    private String ldapGroupSearchFilter;
    private String ldapGroupRoleAttribute;
    private String ldapBindDN;
    private String ldapBindPassword;
    private String ldapDefaultRole;
    private String ldapUserSearchFilter;
    private String ldapReferral;
    private String ldapBase;
    private boolean groupsFromUGI;

    @PostConstruct
    public void setup() {
        setLdapProperties();
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        try {
            authentication = getLdapBindAuthentication(authentication);
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication;
            } else {
                authentication = getLdapAuthentication(authentication);
                if (authentication != null && authentication.isAuthenticated()) {
                    return authentication;
                }
            }
        } catch (Exception e) {
            throw new AtlasAuthenticationException(e.getMessage(), e.getCause());
        }
        return authentication;
    }

    private Authentication getLdapBindAuthentication(
            Authentication authentication) {
        try {
            if (isDebugEnabled) {
                LOG.debug("==> AtlasLdapAuthenticationProvider getLdapBindAuthentication");
            }
            String userName = authentication.getName();
            String userPassword = "";
            if (authentication.getCredentials() != null) {
                userPassword = authentication.getCredentials().toString();
            }

            LdapContextSource ldapContextSource = getLdapContextSource();

            DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = getDefaultLdapAuthoritiesPopulator(ldapContextSource);

            if (ldapUserSearchFilter == null
                    || ldapUserSearchFilter.trim().isEmpty()) {
                ldapUserSearchFilter = "(uid={0})";
            }

            FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                    ldapBase, ldapUserSearchFilter, ldapContextSource);
            userSearch.setSearchSubtree(true);

            BindAuthenticator bindAuthenticator = getBindAuthenticator(
                    userSearch, ldapContextSource);

            LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(
                    bindAuthenticator, defaultLdapAuthoritiesPopulator);

            if (userName != null && userPassword != null
                    && !userName.trim().isEmpty()
                    && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = getAuthorities(userName);
                final UserDetails principal = new User(userName, userPassword,
                        grantedAuths);
                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
                        principal, userPassword, grantedAuths);
                authentication = ldapAuthenticationProvider.authenticate(finalAuthentication);
                if(groupsFromUGI) {
                    authentication = getAuthenticationWithGrantedAuthorityFromUGI(authentication);
                }
                return authentication;
            } else {
                LOG.error("LDAP Authentication::userName or userPassword is null or empty for userName "
                        + userName);
            }
        } catch (Exception e) {
            LOG.error(" getLdapBindAuthentication LDAP Authentication Failed:", e);
        }
        if (isDebugEnabled) {
            LOG.debug("<== AtlasLdapAuthenticationProvider getLdapBindAuthentication");
        }
        return authentication;
    }

    private Authentication getLdapAuthentication(Authentication authentication) {

        if (isDebugEnabled) {
            LOG.debug("==> AtlasLdapAuthenticationProvider getLdapAuthentication");
        }

        try {
            // taking the user-name and password from the authentication
            // object.
            String userName = authentication.getName();
            String userPassword = "";
            if (authentication.getCredentials() != null) {
                userPassword = authentication.getCredentials().toString();
            }

            // populating LDAP context source with LDAP URL and user-DN-pattern
            LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(
                    ldapURL);

            ldapContextSource.setCacheEnvironmentProperties(false);
            ldapContextSource.setAnonymousReadOnly(true);

            // Creating BindAuthenticator using Ldap Context Source.
            BindAuthenticator bindAuthenticator = new BindAuthenticator(
                    ldapContextSource);
            //String[] userDnPatterns = new String[] { rangerLdapUserDNPattern };
            String[] userDnPatterns = ldapUserDNPattern.split(";");
            bindAuthenticator.setUserDnPatterns(userDnPatterns);

            LdapAuthenticationProvider ldapAuthenticationProvider = null;

            if (!StringUtils.isEmpty(ldapGroupSearchBase) && !StringUtils.isEmpty(ldapGroupSearchFilter)) {
                // Creating LDAP authorities populator using Ldap context source and
                // Ldap group search base.
                // populating LDAP authorities populator with group search
                // base,group role attribute, group search filter.
                DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
                        ldapContextSource, ldapGroupSearchBase);
                defaultLdapAuthoritiesPopulator.setGroupRoleAttribute(ldapGroupRoleAttribute);
                defaultLdapAuthoritiesPopulator.setGroupSearchFilter(ldapGroupSearchFilter);
                defaultLdapAuthoritiesPopulator.setIgnorePartialResultException(true);

                // Creating Ldap authentication provider using BindAuthenticator and Ldap authentication populator
                ldapAuthenticationProvider = new LdapAuthenticationProvider(
                        bindAuthenticator, defaultLdapAuthoritiesPopulator);
            } else {
                ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
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

                authentication = ldapAuthenticationProvider
                        .authenticate(finalAuthentication);
                if (groupsFromUGI) {
                    authentication = getAuthenticationWithGrantedAuthorityFromUGI(authentication);
                }
                return authentication;
            } else {
                return authentication;
            }
        } catch (Exception e) {
            LOG.error("getLdapAuthentication LDAP Authentication Failed:", e);
        }
        if (isDebugEnabled) {
            LOG.debug("<== AtlasLdapAuthenticationProvider getLdapAuthentication");
        }
        return authentication;
    }

    private void setLdapProperties() {
        try {
            Configuration configuration = ApplicationProperties.get();
            Properties properties = ConfigurationConverter.getProperties(configuration.subset("atlas.authentication.method.ldap"));
            ldapURL = properties.getProperty("url");
            ldapUserDNPattern = properties.getProperty("userDNpattern");
            ldapGroupSearchBase = properties.getProperty("groupSearchBase");
            ldapGroupSearchFilter = properties.getProperty("groupSearchFilter");
            ldapGroupRoleAttribute = properties.getProperty("groupRoleAttribute");
            ldapBindDN = properties.getProperty("bind.dn");
            ldapBindPassword = properties.getProperty("bind.password");
            ldapDefaultRole = properties.getProperty("default.role");
            ldapUserSearchFilter = properties.getProperty("user.searchfilter");
            ldapReferral = properties.getProperty("referral");
            ldapBase = properties.getProperty("base.dn");
            groupsFromUGI = configuration.getBoolean("atlas.authentication.method.ldap.ugi-groups", true);

            if(LOG.isDebugEnabled()) {
                LOG.debug("AtlasLdapAuthenticationProvider{" +
                        "ldapURL='" + ldapURL + '\'' +
                        ", ldapUserDNPattern='" + ldapUserDNPattern + '\'' +
                        ", ldapGroupSearchBase='" + ldapGroupSearchBase + '\'' +
                        ", ldapGroupSearchFilter='" + ldapGroupSearchFilter + '\'' +
                        ", ldapGroupRoleAttribute='" + ldapGroupRoleAttribute + '\'' +
                        ", ldapBindDN='" + ldapBindDN + '\'' +
                        ", ldapDefaultRole='" + ldapDefaultRole + '\'' +
                        ", ldapUserSearchFilter='" + ldapUserSearchFilter + '\'' +
                        ", ldapReferral='" + ldapReferral + '\'' +
                        ", ldapBase='" + ldapBase + '\'' +
                        ", groupsFromUGI=" + groupsFromUGI +
                        '}');
            }

        } catch (Exception e) {
            LOG.error("Exception while setLdapProperties", e);
        }

    }

    private LdapContextSource getLdapContextSource() throws Exception {
        LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(
                ldapURL);
        ldapContextSource.setUserDn(ldapBindDN);
        ldapContextSource.setPassword(ldapBindPassword);
        ldapContextSource.setReferral(ldapReferral);
        ldapContextSource.setCacheEnvironmentProperties(false);
        ldapContextSource.setAnonymousReadOnly(false);
        ldapContextSource.setPooled(true);
        ldapContextSource.afterPropertiesSet();
        return ldapContextSource;
    }

    private DefaultLdapAuthoritiesPopulator getDefaultLdapAuthoritiesPopulator(
            LdapContextSource ldapContextSource) {
        DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
                ldapContextSource, ldapGroupSearchBase);
        defaultLdapAuthoritiesPopulator
                .setGroupRoleAttribute(ldapGroupRoleAttribute);
        defaultLdapAuthoritiesPopulator
                .setGroupSearchFilter(ldapGroupSearchFilter);
        defaultLdapAuthoritiesPopulator.setIgnorePartialResultException(true);
        return defaultLdapAuthoritiesPopulator;
    }

    private BindAuthenticator getBindAuthenticator(
            FilterBasedLdapUserSearch userSearch,
            LdapContextSource ldapContextSource) throws Exception {
        BindAuthenticator bindAuthenticator = new BindAuthenticator(
                ldapContextSource);
        bindAuthenticator.setUserSearch(userSearch);
        String[] userDnPatterns = new String[] { ldapUserDNPattern };
        bindAuthenticator.setUserDnPatterns(userDnPatterns);
        bindAuthenticator.afterPropertiesSet();
        return bindAuthenticator;
    }
}

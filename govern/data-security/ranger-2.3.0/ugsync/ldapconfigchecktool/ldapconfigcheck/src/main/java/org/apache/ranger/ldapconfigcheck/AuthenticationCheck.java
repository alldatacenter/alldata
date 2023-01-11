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

package org.apache.ranger.ldapconfigcheck;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;


public class AuthenticationCheck {
    private String ldapUrl = null;
    private String authMethod = "NONE";
    private String adDomain = null;
    private String userDnPattern = null;
    private String roleAttribute = null;
    private String groupSearchBase = null;
    private String groupSearchFilter = null;

    private PrintStream logFile = null;
    private PrintStream ambariProps = null;
    private PrintStream installProps = null;

    public AuthenticationCheck(String ldapUrl, UserSync userSyncObj, PrintStream logFile,
                               PrintStream ambariProps, PrintStream installProps) {

        this.logFile = logFile;
        this.ambariProps = ambariProps;
        this.installProps = installProps;

        if (userSyncObj.getUserNameAttribute().equalsIgnoreCase("sAMAccountName")) {
            authMethod = "AD";
        } else {
            authMethod = "LDAP";
        }
        this.ldapUrl = ldapUrl;
        adDomain = userSyncObj.getSearchBase();
        userDnPattern = userSyncObj.getUserNameAttribute() + "={0}," + userSyncObj.getUserSearchBase();
        roleAttribute = userSyncObj.getGroupNameAttrName();
        groupSearchBase = userSyncObj.getGroupSearchBase();
        groupSearchFilter = userSyncObj.getGroupMemberName() + "=" + userDnPattern;

    }

    public void discoverAuthProperties() {

        ambariProps.println("\n# Possible values for authentication properties:");
        installProps.println("\n# Possible values for authentication properties:");
        if (authMethod.equalsIgnoreCase("AD")) {
            installProps.println("xa_ldap_ad_url=" + ldapUrl);
            installProps.println("xa_ldap_ad_domain=" + adDomain);
        } else {
            installProps.println("xa_ldap_url=" + ldapUrl);
            installProps.println("xa_ldap_userDNpattern=" + userDnPattern);
            installProps.println("xa_ldap_groupRoleAttribute=" + roleAttribute);
            installProps.println("xa_ldap_groupSearchBase=" + groupSearchBase);
            installProps.println("xa_ldap_groupSearchFilter=" + groupSearchFilter);
        }

        ambariProps.println("ranger.authentication.method=" + authMethod);
        if (authMethod.equalsIgnoreCase("AD")) {
            ambariProps.println("ranger.ldap.ad.url=" + ldapUrl);
            ambariProps.println("ranger.ldap.ad.domain=" + adDomain);
        } else {
            ambariProps.println("ranger.ldap.url=" + ldapUrl);
            ambariProps.println("ranger.ldap.user.dnpattern=" + userDnPattern);
            ambariProps.println("ranger.ldap.group.roleattribute=" + roleAttribute);
            ambariProps.println("ranger.ldap.group.searchbase=" + groupSearchBase);
            ambariProps.println("ranger.ldap.group.searchfilter=" + groupSearchFilter);
        }
    }

    public boolean isAuthenticated(String ldapUrl, String bindDn, String bindPassword, String userName,
                                   String userPassword) {
        boolean isAuthenticated = false;
        //Verify Authentication
        Authentication authentication;
        if (authMethod.equalsIgnoreCase("AD")) {
            authentication = getADBindAuthentication(ldapUrl, bindDn, bindPassword, userName, userPassword);
        } else {
            authentication = getLdapBindAuthentication(ldapUrl, bindDn, bindPassword, userName, userPassword);
        }
        if (authentication != null) {
            isAuthenticated = authentication.isAuthenticated();
        }

        return isAuthenticated;
    }

    private Authentication getADBindAuthentication(String ldapUrl, String bindDn, String bindPassword,
                                                   String userName, String userPassword) {
        Authentication result = null;
        try {
            LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(ldapUrl);
            ldapContextSource.setUserDn(bindDn);
            ldapContextSource.setPassword(bindPassword);
            ldapContextSource.setReferral("follow");
            ldapContextSource.setCacheEnvironmentProperties(true);
            ldapContextSource.setAnonymousReadOnly(false);
            ldapContextSource.setPooled(true);
            ldapContextSource.afterPropertiesSet();

            String searchFilter="(sAMAccountName={0})";
            FilterBasedLdapUserSearch userSearch=new FilterBasedLdapUserSearch(adDomain, searchFilter,ldapContextSource);
            userSearch.setSearchSubtree(true);

            BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
            bindAuthenticator.setUserSearch(userSearch);
            bindAuthenticator.afterPropertiesSet();

            LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);

            if (userName != null && userPassword != null && !userName.trim().isEmpty() && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = new ArrayList<>();
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
                final UserDetails principal = new User(userName, userPassword, grantedAuths);
                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, userPassword, grantedAuths);

                result = ldapAuthenticationProvider.authenticate(finalAuthentication);
            }

        } catch (BadCredentialsException bce) {
            logFile.println("ERROR: LDAP Authentication Failed. Please verify values for ranger.admin.auth.sampleuser and " +
                    "ranger.admin.auth.samplepassword\n");
        } catch (Exception e) {
            logFile.println("ERROR: LDAP Authentication Failed: " + e);
        }
        return result;
    }

    private Authentication getLdapBindAuthentication(String ldapUrl, String bindDn, String bindPassword,
                                                     String userName, String userPassword) {
        Authentication result = null;
        try {
            LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(ldapUrl);
            ldapContextSource.setUserDn(bindDn);
            ldapContextSource.setPassword(bindPassword);
            ldapContextSource.setReferral("follow");
            ldapContextSource.setCacheEnvironmentProperties(false);
            ldapContextSource.setAnonymousReadOnly(true);
            ldapContextSource.setPooled(true);
            ldapContextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(ldapContextSource, groupSearchBase);
            defaultLdapAuthoritiesPopulator.setGroupRoleAttribute(roleAttribute);
            defaultLdapAuthoritiesPopulator.setGroupSearchFilter(groupSearchFilter);
            defaultLdapAuthoritiesPopulator.setIgnorePartialResultException(true);

            String searchFilter="(uid={0})";
            FilterBasedLdapUserSearch userSearch=new FilterBasedLdapUserSearch(adDomain, searchFilter,ldapContextSource);
            userSearch.setSearchSubtree(true);

            BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
            bindAuthenticator.setUserSearch(userSearch);
            String[] userDnPatterns = new String[] { userDnPattern };
            bindAuthenticator.setUserDnPatterns(userDnPatterns);
            bindAuthenticator.afterPropertiesSet();

            LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator,defaultLdapAuthoritiesPopulator);

            if (userName != null && userPassword != null && !userName.trim().isEmpty() && !userPassword.trim().isEmpty()) {
                final List<GrantedAuthority> grantedAuths = new ArrayList<>();
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
                final UserDetails principal = new User(userName, userPassword, grantedAuths);
                final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, userPassword, grantedAuths);

                result = ldapAuthenticationProvider.authenticate(finalAuthentication);
            }
        } catch (BadCredentialsException bce) {
            logFile.println("ERROR: LDAP Authentication Failed. Please verify values for ranger.admin.auth.sampleuser and " +
                    "ranger.admin.auth.samplepassword\n");
        } catch (Exception e) {
            logFile.println("ERROR: LDAP Authentication Failed: " + e);
        }
        return result;
    }
}



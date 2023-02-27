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

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.Groups;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import org.apache.atlas.utils.AuthenticationUtil;

public abstract class AtlasAbstractAuthenticationProvider implements AuthenticationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasAbstractAuthenticationProvider.class);

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 
     * @param authentication
     * @return
     */
    public Authentication getAuthenticationWithGrantedAuthority(
            Authentication authentication) {
        UsernamePasswordAuthenticationToken result = null;
        if (authentication != null && authentication.isAuthenticated()) {
            final List<GrantedAuthority> grantedAuths = getAuthorities(authentication
                    .getName());
            final UserDetails userDetails = new User(authentication.getName(), authentication.getCredentials().toString(),
                    grantedAuths);
            result = new UsernamePasswordAuthenticationToken(userDetails,
                    authentication.getCredentials(), grantedAuths);
            result.setDetails(authentication.getDetails());
            return result;
        }
        return authentication;
    }

    /**
     * This method will be modified when actual roles are introduced.
     * 
     */
    protected List<GrantedAuthority> getAuthorities(String username) {
        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("DATA_SCIENTIST"));
        return grantedAuths;
    }


    public Authentication getAuthenticationWithGrantedAuthorityFromUGI(
            Authentication authentication) {
        UsernamePasswordAuthenticationToken result = null;
        if (authentication != null && authentication.isAuthenticated()) {

            List<GrantedAuthority> grantedAuthsUGI = getAuthoritiesFromUGI(authentication
                    .getName());

            final UserDetails userDetails = new User(authentication.getName(), authentication.getCredentials().toString(),
                    grantedAuthsUGI);
            result = new UsernamePasswordAuthenticationToken(userDetails,
                    authentication.getCredentials(), grantedAuthsUGI);
            result.setDetails(authentication.getDetails());
            return result;
        }
        return authentication;
    }

    public static List<GrantedAuthority> getAuthoritiesFromUGI(String userName) {
        Set<String>          userGroups = new HashSet<>();
        UserGroupInformation ugi        = UserGroupInformation.createRemoteUser(userName);

        if (ugi != null) {
            String[] groups = ugi.getGroupNames();

            if(LOG.isDebugEnabled()) {
                LOG.debug("UserGroupInformation userGroups=" + Arrays.toString(groups));
            }

            if (groups != null) {
                for (String group : groups) {
                    userGroups.add(group);
                }
            }
        }

        // if group empty take groups from Hadoop LDAP-based group mapping
        if (CollectionUtils.isEmpty(userGroups) || AuthenticationUtil.includeHadoopGroups()) {
            try {
                Configuration config = new Configuration();
                Groups        gp     = new Groups(config);
                List<String>  groups = gp.getGroups(userName);

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Hadoop userGroups=" + groups);
                }

                if (groups != null) {
                    for (String group : groups) {
                        userGroups.add(group);
                    }
                }
            } catch (java.io.IOException e) {
                LOG.error("Exception while fetching groups ", e);
            }
        }

        List<GrantedAuthority> ret = new ArrayList<>();

        for (String userGroup : userGroups) {
            ret.add(new SimpleGrantedAuthority(userGroup));
        }

        return ret;
    }

}

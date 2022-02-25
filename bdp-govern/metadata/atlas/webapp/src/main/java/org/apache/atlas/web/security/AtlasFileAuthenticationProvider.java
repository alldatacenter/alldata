/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliRance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.atlas.web.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
 

@Component
public class AtlasFileAuthenticationProvider extends AtlasAbstractAuthenticationProvider {

    private static Logger logger = LoggerFactory.getLogger(AtlasFileAuthenticationProvider.class);

    private final UserDetailsService userDetailsService;

    @Inject
    public AtlasFileAuthenticationProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (username == null || username.isEmpty()) {
            logger.error("Username can't be null or empty.");

            throw new BadCredentialsException("Username can't be null or empty.");
        }

        if (password == null || password.isEmpty()) {
            logger.error("Password can't be null or empty.");

            throw new BadCredentialsException("Password can't be null or empty.");
        }

        UserDetails user            = userDetailsService.loadUserByUsername(username);
        boolean     isValidPassword = UserDao.checkEncrypted(password, user.getPassword(), username);

        if (!isValidPassword) {
            logger.error("Wrong password " + username);

            throw new BadCredentialsException("Wrong password");
        }

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        authentication = new UsernamePasswordAuthenticationToken(username, password, authorities);

        return authentication;
    }
}

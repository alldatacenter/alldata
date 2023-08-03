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
import org.apache.atlas.web.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Properties;

import static org.testng.Assert.assertTrue;

public class UserDaoTest {

    @Test
    public void testUserDaowithValidUserLoginAndPassword() {

        Properties userLogins = new Properties();
        userLogins.put("admin", "ADMIN::admin123");

        UserDao user = new UserDao();
        user.setUserLogins(userLogins);
        User userBean = user.loadUserByUsername("admin");
        assertTrue(userBean.getPassword().equals("admin123"));

        Collection<? extends GrantedAuthority> authorities = userBean.getAuthorities();
        String role = "";
        for (GrantedAuthority gauth : authorities) {
            role = gauth.getAuthority();
        }
        assertTrue("ADMIN".equals(role));
    }

    @Test
    public void testUserDaowithInValidLogin() {
        boolean hadException = false;
        Properties userLogins = new Properties();
        userLogins.put("admin", "ADMIN::admin123");
        userLogins.put("test", "DATA_STEWARD::test123");

        UserDao user = new UserDao();
        user.setUserLogins(userLogins);
        try {
            User userBean = user.loadUserByUsername("xyz");
        } catch (UsernameNotFoundException uex) {
            hadException = true;
        }
        assertTrue(hadException);
    }

    @Test
    public void testUserDaowithencodePassword() {
        assertTrue(UserDao.checkEncrypted("admin", "a4a88c0872bf652bb9ed803ece5fd6e82354838a9bf59ab4babb1dab322154e1", "admin"));
    }

}
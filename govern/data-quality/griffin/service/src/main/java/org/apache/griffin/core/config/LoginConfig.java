/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.config;

import org.apache.griffin.core.login.LoginService;
import org.apache.griffin.core.login.LoginServiceDefaultImpl;
import org.apache.griffin.core.login.LoginServiceLdapImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoginConfig {

    @Value("${login.strategy}")
    private String strategy;
    @Value("${ldap.url}")
    private String url;
    @Value("${ldap.email}")
    private String email;
    @Value("${ldap.searchBase}")
    private String searchBase;
    @Value("${ldap.searchPattern}")
    private String searchPattern;
    @Value("${ldap.sslSkipVerify:false}")
    private boolean sslSkipVerify;
    @Value("${ldap.bindDN:}")
    private String bindDN;
    @Value("${ldap.bindPassword:}")
    private String bindPassword;

    @Bean
    public LoginService loginService() {
        switch (strategy) {
            case "default":
                return new LoginServiceDefaultImpl();
            case "ldap":
                return new LoginServiceLdapImpl(url, email, searchBase,
                    searchPattern, sslSkipVerify, bindDN, bindPassword);
            default:
                return null;
        }
    }

}

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
package org.apache.ambari.server.configuration.spring;

import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authentication.AmbariDelegatingAuthenticationFilter;
import org.apache.ambari.server.security.authentication.AmbariLocalAuthenticationProvider;
import org.apache.ambari.server.security.authentication.jwt.AmbariJwtAuthenticationProvider;
import org.apache.ambari.server.security.authentication.kerberos.AmbariAuthToLocalUserDetailsService;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosAuthenticationProvider;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosTicketValidator;
import org.apache.ambari.server.security.authentication.kerberos.AmbariProxiedUserDetailsService;
import org.apache.ambari.server.security.authentication.pam.AmbariPamAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Import(GuiceBeansConfig.class)
@ComponentScan("org.apache.ambari.server.security")
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter{

  private final GuiceBeansConfig guiceBeansConfig;

  @Autowired
  private AmbariEntryPoint ambariEntryPoint;
  @Autowired
  private AmbariDelegatingAuthenticationFilter delegatingAuthenticationFilter;
  @Autowired
  private AmbariAuthorizationFilter authorizationFilter;

  public ApiSecurityConfig(GuiceBeansConfig guiceBeansConfig) {
    this.guiceBeansConfig = guiceBeansConfig;
  }

  @Autowired
  public void configureAuthenticationManager(AuthenticationManagerBuilder auth,
                                             AmbariJwtAuthenticationProvider ambariJwtAuthenticationProvider,
                                             AmbariPamAuthenticationProvider ambariPamAuthenticationProvider,
                                             AmbariLocalAuthenticationProvider ambariLocalAuthenticationProvider,
                                             AmbariLdapAuthenticationProvider ambariLdapAuthenticationProvider,
                                             AmbariInternalAuthenticationProvider ambariInternalAuthenticationProvider,
                                             AmbariKerberosAuthenticationProvider ambariKerberosAuthenticationProvider
  ) {
    auth.authenticationProvider(ambariJwtAuthenticationProvider)
        .authenticationProvider(ambariPamAuthenticationProvider)
        .authenticationProvider(ambariLocalAuthenticationProvider)
        .authenticationProvider(ambariLdapAuthenticationProvider)
        .authenticationProvider(ambariInternalAuthenticationProvider)
        .authenticationProvider(ambariKerberosAuthenticationProvider);
  }

  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests().anyRequest().authenticated()
        .and()
        .headers().frameOptions().disable().and()
        .exceptionHandling().authenticationEntryPoint(ambariEntryPoint)
        .and()
        .addFilterBefore(guiceBeansConfig.ambariUserAuthorizationFilter(), BasicAuthenticationFilter.class)
        .addFilterAt(delegatingAuthenticationFilter, BasicAuthenticationFilter.class)
        .addFilterBefore(authorizationFilter, FilterSecurityInterceptor.class);
  }

  @Bean
  public AmbariKerberosAuthenticationProvider ambariKerberosAuthenticationProvider(
      AmbariKerberosTicketValidator ambariKerberosTicketValidator,
      AmbariAuthToLocalUserDetailsService authToLocalUserDetailsService,
      AmbariProxiedUserDetailsService proxiedUserDetailsService) {

    return new AmbariKerberosAuthenticationProvider(authToLocalUserDetailsService,
        proxiedUserDetailsService,
        ambariKerberosTicketValidator);
  }
}

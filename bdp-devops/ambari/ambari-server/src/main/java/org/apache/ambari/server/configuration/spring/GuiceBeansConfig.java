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

import org.apache.ambari.server.agent.stomp.AgentsRegistrationQueue;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandlerImpl;
import org.apache.ambari.server.security.authentication.AmbariLocalAuthenticationProvider;
import org.apache.ambari.server.security.authentication.jwt.AmbariJwtAuthenticationProvider;
import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.apache.ambari.server.security.authentication.pam.AmbariPamAuthenticationProvider;
import org.apache.ambari.server.security.authentication.tproxy.AmbariTProxyConfigurationProvider;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariUserAuthorizationFilter;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Injector;

@Configuration
public class GuiceBeansConfig {

  @Autowired
  //ignore warning, inherited from parent context, injected as field to reduce number of warnings
  private Injector injector;

  @Bean
  public org.apache.ambari.server.configuration.Configuration ambariConfig() {
    return injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return injector.getInstance(PasswordEncoder.class);
  }

  @Bean
  public AuditLogger auditLogger() {
    return injector.getInstance(AuditLogger.class);
  }

  @Bean
  public PermissionHelper permissionHelper() {
    return injector.getInstance(PermissionHelper.class);
  }

  @Bean
  public AmbariLdapAuthenticationProvider ambariLdapAuthenticationProvider() {
    return injector.getInstance(AmbariLdapAuthenticationProvider.class);
  }

  @Bean
  public AmbariLdapDataPopulator ambariLdapDataPopulator() {
    return injector.getInstance(AmbariLdapDataPopulator.class);
  }

  @Bean
  public AmbariUserAuthorizationFilter ambariUserAuthorizationFilter() {
    return injector.getInstance(AmbariUserAuthorizationFilter.class);
  }

  @Bean
  public AmbariInternalAuthenticationProvider ambariInternalAuthenticationProvider() {
    return injector.getInstance(AmbariInternalAuthenticationProvider.class);
  }
  @Bean
  public AmbariJwtAuthenticationProvider ambariJwtAuthenticationProvider() {
    return injector.getInstance(AmbariJwtAuthenticationProvider.class);
  }

  @Bean
  public JwtAuthenticationPropertiesProvider jwtAuthenticationPropertiesProvider() {
    return injector.getInstance(JwtAuthenticationPropertiesProvider.class);
  }

  @Bean
  public AmbariPamAuthenticationProvider ambariPamAuthenticationProvider() {
    return injector.getInstance(AmbariPamAuthenticationProvider.class);
  }

  @Bean
  public AmbariLocalAuthenticationProvider ambariLocalAuthenticationProvider() {
    return injector.getInstance(AmbariLocalAuthenticationProvider.class);
  }

  @Bean
  public AmbariAuthenticationEventHandlerImpl ambariAuthenticationEventHandler() {
    return injector.getInstance(AmbariAuthenticationEventHandlerImpl.class);
  }


  @Bean
  public AgentRegisteringQueueChecker agentRegisteringQueueChecker() {
    return new AgentRegisteringQueueChecker();
  }

  @Bean
  public AgentsRegistrationQueue agentsRegistrationQueue() {
    return new AgentsRegistrationQueue(injector);
  }

  @Bean
  public AmbariTProxyConfigurationProvider ambariTProxyConfigurationProvider() {
    return injector.getInstance(AmbariTProxyConfigurationProvider.class);
  }

}

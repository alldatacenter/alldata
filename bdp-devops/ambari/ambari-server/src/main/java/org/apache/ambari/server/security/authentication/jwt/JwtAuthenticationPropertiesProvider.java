/*
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

package org.apache.ambari.server.security.authentication.jwt;

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;

import java.util.Collection;

import org.apache.ambari.server.configuration.AmbariServerConfigurationProvider;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * JwtAuthenticationPropertiesProvider manages a {@link JwtAuthenticationProperties} instance by
 * lazily loading the properties if needed and refreshing the properties if updates are made to the
 * sso-configuration category of the Ambari configuration data.
 * <p>
 * The {@link JwtAuthenticationProperties} are updated upon events received from the {@link AmbariEventPublisher}.
 */
@Singleton
public class JwtAuthenticationPropertiesProvider extends AmbariServerConfigurationProvider<JwtAuthenticationProperties> {

  @Inject
  public JwtAuthenticationPropertiesProvider(AmbariEventPublisher ambariEventPublisher, AmbariJpaPersistService ambariJpaPersistService) {
    super(SSO_CONFIGURATION, ambariEventPublisher, ambariJpaPersistService);
  }

  /**
   * Creates a JwtAuthenticationProperties from a list of {@link AmbariConfigurationEntity}s.
   *
   * @param configurationEntities a list of {@link AmbariConfigurationEntity}s
   * @return a filled in {@link JwtAuthenticationProperties}
   */
  @Override
  protected JwtAuthenticationProperties loadInstance(Collection<AmbariConfigurationEntity> configurationEntities) {
    return new JwtAuthenticationProperties(toProperties(configurationEntities));
  }
}

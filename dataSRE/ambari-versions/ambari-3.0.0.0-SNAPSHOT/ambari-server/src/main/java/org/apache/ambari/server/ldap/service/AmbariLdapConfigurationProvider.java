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

package org.apache.ambari.server.ldap.service;

import java.util.Collection;

import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationProvider;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * Provider implementation for LDAP configurations.
 * It needs to be registered in the related GUICE module as a provider.
 * It's responsible for managing LDAP configurations in the application.
 * Whenever requested, this provider returns an AmbariLdapConfiguration which is always in sync with the persisted LDAP
 * configuration resource.
 * <p>
 * The provider receives notifications on CRUD operations related to the persisted resource and reloads the cached
 * configuration instance accordingly.
 *
 * @see AmbariServerConfigurationProvider
 */
@Singleton
public class AmbariLdapConfigurationProvider extends AmbariServerConfigurationProvider<AmbariLdapConfiguration> {

  @Inject
  public AmbariLdapConfigurationProvider(AmbariEventPublisher publisher, AmbariJpaPersistService persistService) {
    super(AmbariServerConfigurationCategory.LDAP_CONFIGURATION, publisher, persistService);
  }

  @Override
  protected AmbariLdapConfiguration loadInstance(Collection<AmbariConfigurationEntity> configurationEntities) {
    return new AmbariLdapConfiguration(toProperties(configurationEntities));
  }
}

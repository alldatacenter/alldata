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

package org.apache.ambari.server.actionmanager;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHostEvent;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class HostRoleCommandFactoryImpl implements HostRoleCommandFactory {

  private Injector injector;

  @Inject
  public HostRoleCommandFactoryImpl(Injector injector) {
    this.injector = injector;
  }

  /**
   * Constructor via factory.
   * @param hostName Host name
   * @param role Action to run
   * @param event Event on the host and component
   * @param command Type of command
   * @return An instance constructed where retryAllowed defaults to false
   */
  @Override
  public HostRoleCommand create(String hostName, Role role,
                                ServiceComponentHostEvent event, RoleCommand command) {
    return new HostRoleCommand(hostName, role, event, command,
        injector.getInstance(HostDAO.class),
        injector.getInstance(ExecutionCommandDAO.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HostRoleCommand create(String hostName, Role role, ServiceComponentHostEvent event,
      RoleCommand command, boolean retryAllowed, boolean autoSkipFailure) {
    return new HostRoleCommand(hostName, role, event, command, retryAllowed, autoSkipFailure,
        injector.getInstance(HostDAO.class), injector.getInstance(ExecutionCommandDAO.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HostRoleCommand create(Host host, Role role, ServiceComponentHostEvent event,
      RoleCommand command, boolean retryAllowed, boolean autoSkipFailure) {
    return new HostRoleCommand(host, role, event, command, retryAllowed, autoSkipFailure,
        injector.getInstance(HostDAO.class), injector.getInstance(ExecutionCommandDAO.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }

  /**
   * Constructor via factory
   * @param hostRoleCommandEntity Object to copy fields from.
   * @return An instance constructed from the input object.
   */
  @Override
  public HostRoleCommand createExisting(HostRoleCommandEntity hostRoleCommandEntity) {
    return new HostRoleCommand(hostRoleCommandEntity,
        injector.getInstance(HostDAO.class),
        injector.getInstance(ExecutionCommandDAO.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }
}

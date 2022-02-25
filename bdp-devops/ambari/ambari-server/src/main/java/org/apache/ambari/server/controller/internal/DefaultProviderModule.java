/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule extends AbstractProviderModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProviderModule.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a default provider module.
   */
  public DefaultProviderModule() {
    super();
  }


  // ----- utility methods ---------------------------------------------------

  @Override
  protected ResourceProvider createResourceProvider(Resource.Type type) {

    LOGGER.debug("Creating resource provider for the type: {}", type);

    switch (type.getInternalType()) {
      case Workflow:
        return new WorkflowResourceProvider();
      case Job:
        return new JobResourceProvider();
      case TaskAttempt:
        return new TaskAttemptResourceProvider();
      case View:
        return new ViewResourceProvider();
      case ViewVersion:
        return new ViewVersionResourceProvider();
      case ViewURL:
        return new ViewURLResourceProvider();
      case StackServiceComponentDependency:
        return new StackDependencyResourceProvider();
      case Permission:
        return new PermissionResourceProvider();
      case AmbariPrivilege:
        return new AmbariPrivilegeResourceProvider();
      case ViewPrivilege:
        return new ViewPrivilegeResourceProvider();
      case ViewPermission:
        return new ViewPermissionResourceProvider();
      case ClusterPrivilege:
        return new ClusterPrivilegeResourceProvider();
      case LdapSyncEvent:
        return new LdapSyncEventResourceProvider(managementController);
      case UserPrivilege:
        return new UserPrivilegeResourceProvider();
      case GroupPrivilege:
        return new GroupPrivilegeResourceProvider();
      case Alert:
        return new AlertResourceProvider(managementController);
      case Mpack:
        return new MpackResourceProvider(managementController);
      case AlertDefinition:
        return new AlertDefinitionResourceProvider(managementController);
      case AlertHistory:
        return new AlertHistoryResourceProvider(managementController);
      case AlertGroup:
        return new AlertGroupResourceProvider(managementController);
      case AlertNotice:
        return new AlertNoticeResourceProvider(managementController);
      case UpgradeGroup:
        return new UpgradeGroupResourceProvider(managementController);
      case UpgradeItem:
        return new UpgradeItemResourceProvider(managementController);
      case UpgradeSummary:
        return new UpgradeSummaryResourceProvider(managementController);
      case PreUpgradeCheck:
        return new PreUpgradeCheckResourceProvider(managementController);
      case HostStackVersion:
        return new HostStackVersionResourceProvider(managementController);
      case Stage:
        return new StageResourceProvider(managementController);
      case OperatingSystem:
        return new OperatingSystemResourceProvider(managementController);
      case Repository:
        return new RepositoryResourceProvider(managementController);
      case Setting:
        return new SettingResourceProvider();
      case Artifact:
        return new ArtifactResourceProvider(managementController);
      case RemoteCluster:
        return new RemoteClusterResourceProvider();
      default:
        LOGGER.debug("Delegating creation of resource provider for: {} to the AbstractControllerResourceProvider", type.getInternalType());
        return AbstractControllerResourceProvider.getResourceProvider(type, managementController);
    }
  }
}

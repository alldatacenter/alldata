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


package org.apache.ambari.server.controller;

import javax.inject.Named;

import org.apache.ambari.server.controller.internal.AlertTargetResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterStackVersionResourceProvider;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewInstanceResourceProvider;
import org.apache.ambari.server.controller.spi.ResourceProvider;


public interface ResourceProviderFactory {
  @Named("host")
  ResourceProvider getHostResourceProvider(AmbariManagementController managementController);

  @Named("hostComponent")
  ResourceProvider getHostComponentResourceProvider(AmbariManagementController managementController);

  @Named("service")
  ResourceProvider getServiceResourceProvider(AmbariManagementController managementController);

  @Named("component")
  ResourceProvider getComponentResourceProvider(AmbariManagementController managementController);

  @Named("member")
  ResourceProvider getMemberResourceProvider(AmbariManagementController managementController);

  @Named("user")
  ResourceProvider getUserResourceProvider(AmbariManagementController managementController);

  @Named("userAuthenticationSource")
  ResourceProvider getUserAuthenticationSourceResourceProvider();

  @Named("hostKerberosIdentity")
  ResourceProvider getHostKerberosIdentityResourceProvider(AmbariManagementController managementController);

  @Named("credential")
  ResourceProvider getCredentialResourceProvider(AmbariManagementController managementController);

  @Named("repositoryVersion")
  ResourceProvider getRepositoryVersionResourceProvider();

  @Named("kerberosDescriptor")
  ResourceProvider getKerberosDescriptorResourceProvider(AmbariManagementController managementController);

  @Named("upgrade")
  UpgradeResourceProvider getUpgradeResourceProvider(AmbariManagementController managementController);

  @Named("rootServiceHostComponentConfiguration")
  ResourceProvider getRootServiceHostComponentConfigurationResourceProvider();

  @Named("clusterStackVersion")
  ClusterStackVersionResourceProvider getClusterStackVersionResourceProvider(AmbariManagementController managementController);

  @Named("alertTarget")
  AlertTargetResourceProvider getAlertTargetResourceProvider();

  @Named("viewInstance")
  ViewInstanceResourceProvider getViewInstanceResourceProvider();

}
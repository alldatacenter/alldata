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
package org.apache.ambari.server.controller.utilities;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.utilities.UsedIdentities.ComponentExclude;
import org.apache.ambari.server.controller.utilities.UsedIdentities.ServiceExclude;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;

/**
 * I represent a group of kerberos identities which are to be deleted after a service or a component was removed.
 * My instances provide methods for removing the candidates, excluding those that are still used by other components or services.
 */
@Experimental(feature = ExperimentalFeature.ORPHAN_KERBEROS_IDENTITY_REMOVAL)
public class RemovableIdentities {
  private final List<KerberosIdentityDescriptor> candidateIdentities;
  private final UsedIdentities usedIdentities;
  private final Cluster cluster;
  private final List<Component> components;

  /**
   * Populate the identities with the identities of the removed service and its components
   */
  public static RemovableIdentities ofService(Cluster cluster, ServiceRemovedEvent event, KerberosHelper kerberosHelper) throws AmbariException {
    if (cluster.getSecurityType() != SecurityType.KERBEROS) {
      return RemovableIdentities.none();
    }
    KerberosServiceDescriptor serviceDescriptor = kerberosHelper.getKerberosDescriptor(cluster, false).getService(event.getServiceName());
    if (serviceDescriptor == null) {
      return RemovableIdentities.none();
    }
    UsedIdentities usedIdentities = UsedIdentities.populate(cluster, excludeService(event.getServiceName()), ComponentExclude.NONE, kerberosHelper);
    return new RemovableIdentities(
      serviceDescriptor.getIdentitiesSkipReferences(),
      usedIdentities,
      cluster,
      event.getComponents());
  }

  /**
   * Populate the identities with the identities of the removed component
   */
  public static RemovableIdentities ofComponent(Cluster cluster, ServiceComponentUninstalledEvent event, KerberosHelper kerberosHelper) throws AmbariException {
    if (cluster.getSecurityType() != SecurityType.KERBEROS) {
      return RemovableIdentities.none();
    }
    KerberosServiceDescriptor serviceDescriptor = kerberosHelper.getKerberosDescriptor(cluster, false).getService(event.getServiceName());
    if (serviceDescriptor == null) {
      return RemovableIdentities.none();
    }
    UsedIdentities usedIdentities = UsedIdentities.populate(
      cluster,
      ServiceExclude.NONE,
      excludeComponent(event.getServiceName(), event.getComponentName(), event.getHostName()),
      kerberosHelper);
    return new RemovableIdentities(
      componentIdentities(singletonList(event.getComponentName()), serviceDescriptor),
      usedIdentities,
      cluster,
      singletonList(event.getComponent()));
  }

  /**
   * Populates the identities with an empty list
   */
  public static RemovableIdentities none() throws AmbariException {
    return new RemovableIdentities(emptyList(), UsedIdentities.none(), null, null);
  }

  private static ServiceExclude excludeService(String excludedServiceName) {
    return serviceName -> excludedServiceName.equals(serviceName);
  }

  private static ComponentExclude excludeComponent(String excludedServiceName, String excludedComponentName, String excludedHostName) {
    return (serviceName, componentName, hosts) -> excludedServiceName.equals(serviceName)
      && excludedComponentName.equals(componentName)
      && hostNames(hosts).equals(singletonList(excludedHostName));
  }

  private static List<String> hostNames(Collection<ServiceComponentHost> hosts) {
    return hosts.stream().map(ServiceComponentHost::getHostName).collect(toList());
  }

  private static List<KerberosIdentityDescriptor> componentIdentities(List<String> componentNames, KerberosServiceDescriptor serviceDescriptor) throws AmbariException {
    return componentNames.stream()
      .map(componentName -> serviceDescriptor.getComponent(componentName))
      .filter(Objects::nonNull)
      .flatMap(componentDescriptor -> componentDescriptor.getIdentitiesSkipReferences().stream())
      .collect(toList());
  }

  private RemovableIdentities(List<KerberosIdentityDescriptor> candidateIdentities, UsedIdentities usedIdentities, Cluster cluster, List<Component> components) {
    this.candidateIdentities = candidateIdentities;
    this.usedIdentities = usedIdentities;
    this.cluster = cluster;
    this.components = components;
  }

  /**
   * Remove all identities which are related to the specified set of components and not used by
   * other services or components
   */
  public void remove(KerberosHelper kerberosHelper) throws AmbariException, KerberosOperationException {
    // TODO: Fix the implementation identifying specific identities in the event we need to  pinpoint
    // TODO: certain identities.  This is not currently needed here since we are only handing removing
    // TODO: identities tied to a specific service, component, and/or host. The logic to determine
    // TODO: whether an identity should be removed is handled elsewhere - unfortunately in different
    // TODO: places
    kerberosHelper.deleteIdentities(cluster, components, null);
  }

  private List<KerberosIdentityDescriptor> skipUsed() throws AmbariException {
    return candidateIdentities.stream().filter(each -> !usedIdentities.contains(each)).collect(toList());
  }
}

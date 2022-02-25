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
import static java.util.stream.Collectors.toList;
import static org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor.nullToEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;

/**
 * I represent a group of identities that are still used by any non-excluded component or service
 */
public class UsedIdentities {
  private final List<KerberosIdentityDescriptor> used;

  public static UsedIdentities none() throws AmbariException {
    return new UsedIdentities(emptyList());
  }

  /**
   * Get all identities of the installed services and components. Skip service or component that is excluded.
   */
  public static UsedIdentities populate(Cluster cluster, ServiceExclude serviceExclude, ComponentExclude componentExclude, KerberosHelper kerberosHelper) throws AmbariException {
    List<KerberosIdentityDescriptor> result = new ArrayList<>();
    KerberosDescriptor root = kerberosHelper.getKerberosDescriptor(cluster, false);
    result.addAll(nullToEmpty(root.getIdentities()));
    for (Service service : cluster.getServices().values()) {
      if (serviceExclude.shouldExclude(service.getName())) {
        continue;
      }
      KerberosServiceDescriptor serviceDescriptor = root.getService(service.getName());
      if (serviceDescriptor != null) {
        result.addAll(nullToEmpty(serviceDescriptor.getIdentities()));
        result.addAll(nullToEmpty(componentIdentities(serviceDescriptor, service, componentExclude)));
      }
    }
    return new UsedIdentities(result);
  }

  private static List<KerberosIdentityDescriptor> componentIdentities(KerberosServiceDescriptor serviceDescriptor, Service service, ComponentExclude componentExclude) {
    return service.getServiceComponents().values()
      .stream()
      .filter(component -> !isComponentExcluded(service, componentExclude, component))
      .flatMap(component -> serviceDescriptor.getComponentIdentities(component.getName()).stream())
      .collect(toList());
  }

  private static boolean isComponentExcluded(Service service, ComponentExclude componentExclude, ServiceComponent component) {
    return component.getServiceComponentHosts().isEmpty()
      || componentExclude.shouldExclude(service.getName(), component.getName(), component.getServiceComponentHosts().values());
  }

  private UsedIdentities(List<KerberosIdentityDescriptor> used) {
    this.used = used;
  }

  /**
   * @return true if there is an identity in the used list with the same keytab or principal name than the given identity
   */
  public boolean contains(KerberosIdentityDescriptor identity) {
    return used.stream().anyMatch(each -> identity.isShared(each));
  }

  public interface ServiceExclude {
    boolean shouldExclude(String serviceName);
    ServiceExclude NONE = serviceName -> false; // default implementation, exclude nothing
  }

  public interface ComponentExclude {
    boolean shouldExclude(String serviceName, String componentName, Collection<ServiceComponentHost> hosts);
    ComponentExclude NONE = (serviceName, componentName, hosts) -> false; // default implementation, exclude nothing
  }
}

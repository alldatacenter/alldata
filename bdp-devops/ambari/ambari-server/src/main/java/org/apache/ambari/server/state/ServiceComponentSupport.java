/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.state;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.union;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Collects services and components which are installed but not supported in the target stack
 */
@Singleton
public class ServiceComponentSupport {
  private final Provider<AmbariMetaInfo> metaInfo;

  @Inject
  public ServiceComponentSupport(Provider<AmbariMetaInfo> metaInfo) {
    this.metaInfo = metaInfo;
  }

  /**
   * Collects the service names from the cluster which are not supported (service doesn't exist or was deleted) in the given stack.
   */
  public Set<String> unsupportedServices(Cluster cluster, String stackName, String stackVersion) {
    return cluster.getServices().keySet().stream()
      .filter(serviceName -> !isServiceSupported(serviceName, stackName, stackVersion))
      .collect(toSet());
  }

  /**
   * Checks if the service is supported by the stack using metainfo
   */
  public boolean isServiceSupported(String serviceName, String stackName, String stackVersion) {
    try {
      ServiceInfo service = metaInfo.get().getServices(stackName, stackVersion).get(serviceName);
      return service != null && !service.isDeleted();
    } catch (AmbariException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Collects the ServiceComponents from the cluster which are not supported (component doesn't exist or was deleted) in the given stack.
   * Non versionAdvertised components are ignored.
   */
  public Set<ServiceComponent> unsupportedComponents(Cluster cluster, String stackName, String stackVersion) throws AmbariException {

    Set<ServiceComponent> unsupportedComponents = new HashSet<>();
    for (Service service : cluster.getServices().values()) {
      for (ServiceComponent component : service.getServiceComponents().values()) {
        if (!component.isVersionAdvertised())
          continue;
        if (!isComponentSupported(service.getName(), component.getName(), stackName, stackVersion)) {
          unsupportedComponents.add(component);
        }
      }
    }
    return unsupportedComponents;
  }

  private boolean isComponentSupported(String serviceName, String componentName, String stackName, String stackVersion) throws AmbariException {
    try {
      ComponentInfo component = metaInfo.get().getComponent(stackName, stackVersion, serviceName, componentName);
      return !component.isDeleted();
    } catch (StackAccessException e) {
      return false;
    }
  }

  /**
   * @return the union of unsupported components and services
   */
  public Collection<String> allUnsupported(Cluster cluster, String stackName, String stackVersion) throws AmbariException {
    return union(
      unsupportedServices(cluster, stackName, stackVersion),
      names(unsupportedComponents(cluster, stackName, stackVersion)));
  }

  private Set<String> names(Set<ServiceComponent> serviceComponents) {
    return serviceComponents.stream().map(each -> each.getName()).collect(toSet());
  }
}

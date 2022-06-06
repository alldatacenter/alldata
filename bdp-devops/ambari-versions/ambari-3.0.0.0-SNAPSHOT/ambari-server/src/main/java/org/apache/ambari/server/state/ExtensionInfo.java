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

package org.apache.ambari.server.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ExtensionVersionResponse;
import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.apache.ambari.server.utils.VersionUtils;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class ExtensionInfo implements Comparable<ExtensionInfo>, Validable{
  private String name;
  private String version;
  private Collection<ServiceInfo> services;
  private String parentExtensionVersion;

  private List<ExtensionMetainfoXml.Stack> stacks;
  private List<ExtensionMetainfoXml.Extension> extensions;
  private boolean valid = true;
  private boolean autoLink = false;
  private boolean active = false;

  /**
   *
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   *
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  private Set<String> errorSet = new HashSet<>();

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }

  //private String stackHooksFolder;

  private String upgradesFolder = null;

  private volatile Map<String, PropertyInfo> requiredProperties;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public synchronized Collection<ServiceInfo> getServices() {
    if (services == null) services = new ArrayList<>();
    return services;
  }

  public ServiceInfo getService(String name) {
    Collection<ServiceInfo> services = getServices();
    for (ServiceInfo service : services) {
      if (service.getName().equals(name)) {
        return service;
      }
    }
    //todo: exception?
    return null;
  }

  public synchronized void setServices(Collection<ServiceInfo> services) {
    this.services = services;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Extension name:" + name + "\nversion:" +
      version + " \nvalid:" + isValid());
    if (services != null) {
      sb.append("\n\t\tService:");
      for (ServiceInfo service : services) {
        sb.append("\t\t");
        sb.append(service);
      }
    }

    return sb.toString();
  }


  @Override
  public int hashCode() {
    return 31  + name.hashCode() + version.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ExtensionInfo)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    ExtensionInfo extInfo = (ExtensionInfo) obj;
    return getName().equals(extInfo.getName()) && getVersion().equals(extInfo.getVersion());
  }

  public ExtensionVersionResponse convertToResponse() {
    Collection<ServiceInfo> serviceInfos = getServices();
    // The collection of service descriptor files. A Set is being used because some Kerberos descriptor
    // files contain multiple services, therefore the same File may be encountered more than once.
    // For example the YARN directory may contain YARN and MAPREDUCE2 services.
    Collection<File> serviceDescriptorFiles = new HashSet<>();
    if (serviceInfos != null) {
      for (ServiceInfo serviceInfo : serviceInfos) {
        File file = serviceInfo.getKerberosDescriptorFile();
        if (file != null) {
          serviceDescriptorFiles.add(file);
        }
      }
    }

    return new ExtensionVersionResponse(getVersion(), getParentExtensionVersion(),
                                        isValid(), getErrors());
  }

  public String getParentExtensionVersion() {
    return parentExtensionVersion;
  }

  public void setParentExtensionVersion(String parentExtensionVersion) {
    this.parentExtensionVersion = parentExtensionVersion;
  }

  @Override
  public int compareTo(ExtensionInfo o) {
    if (name.equals(o.name)) {
      return VersionUtils.compareVersions(version, o.version);
    }
    return name.compareTo(o.name);
  }

  public List<ExtensionMetainfoXml.Stack> getStacks() {
    return stacks;
  }

  public void setStacks(List<ExtensionMetainfoXml.Stack> stacks) {
    this.stacks = stacks;
  }

  public List<ExtensionMetainfoXml.Extension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<ExtensionMetainfoXml.Extension> extensions) {
    this.extensions = extensions;
  }

  public boolean isAutoLink() {
    return autoLink;
  }

  public void setAutoLink(boolean autoLink) {
    this.autoLink = autoLink;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}

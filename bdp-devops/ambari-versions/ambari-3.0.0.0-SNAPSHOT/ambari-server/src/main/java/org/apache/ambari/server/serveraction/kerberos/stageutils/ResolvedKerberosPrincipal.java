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


package org.apache.ambari.server.serveraction.kerberos.stageutils;

import org.apache.ambari.server.utils.StageUtils;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Class that represents principal and it info(host, keytab path, service and component mapping).
 */
public class ResolvedKerberosPrincipal {
  private Long hostId;
  private String hostName;
  private String principal;
  private boolean isService;
  private String cacheFile;
  private Multimap<String, String> serviceMapping = ArrayListMultimap.create();
  private String keytabPath;
  private ResolvedKerberosKeytab resolvedKerberosKeytab;

  public ResolvedKerberosPrincipal(Long hostId, String hostName, String principal, boolean isService, String cacheFile, String serviceName, String componentName, String keytabPath) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.principal = principal;
    this.isService = isService;
    this.cacheFile = cacheFile;
    this.keytabPath = keytabPath;
    addComponentMapping(serviceName, componentName);
  }

  public ResolvedKerberosPrincipal(Long hostId, String hostName, String principal, boolean isService, String cacheFile, String keytabPath) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.principal = principal;
    this.isService = isService;
    this.cacheFile = cacheFile;
    this.keytabPath = keytabPath;
  }

  public ResolvedKerberosPrincipal(Long hostId, String hostName, String principal, boolean isService, String cacheFile, String keytabPath, Multimap<String, String> serviceMapping) {
    this.hostId = hostId;
    this.hostName = hostName;
    this.principal = principal;
    this.isService = isService;
    this.cacheFile = cacheFile;
    this.keytabPath = keytabPath;
    this.serviceMapping = serviceMapping;
  }

  public void addComponentMapping(String serviceName, String componentName) {
    if (serviceName == null){
      serviceName = "";
    }
    if (componentName == null) {
      componentName = "*";
    }
    serviceMapping.get(serviceName).add(componentName);
  }

  public void mergeComponentMapping(ResolvedKerberosPrincipal other) {
    serviceMapping.putAll(other.getServiceMapping());
  }

  public String getKeytabPath() {
    return keytabPath;
  }

  public void setKeytabPath(String keytabPath) {
    this.keytabPath = keytabPath;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public String getHostName() {
    if (hostName == null) {
      return StageUtils.getHostName();
    }
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public boolean isService() {
    return isService;
  }

  public void setService(boolean service) {
    isService = service;
  }

  public String getCacheFile() {
    return cacheFile;
  }

  public void setCacheFile(String cacheFile) {
    this.cacheFile = cacheFile;
  }

  public Multimap<String, String> getServiceMapping() {
    return serviceMapping;
  }

  public void setServiceMapping(Multimap<String, String>  serviceMapping) {
    this.serviceMapping = serviceMapping;
  }

  public ResolvedKerberosKeytab getResolvedKerberosKeytab() {
    return resolvedKerberosKeytab;
  }

  public void setResolvedKerberosKeytab(ResolvedKerberosKeytab resolvedKerberosKeytab) {
    this.resolvedKerberosKeytab = resolvedKerberosKeytab;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResolvedKerberosPrincipal principal1 = (ResolvedKerberosPrincipal) o;
    return isService == principal1.isService &&
      Objects.equal(hostId, principal1.hostId) &&
      Objects.equal(hostName, principal1.hostName) &&
      Objects.equal(principal, principal1.principal) &&
      Objects.equal(cacheFile, principal1.cacheFile) &&
      Objects.equal(serviceMapping, principal1.serviceMapping) &&
      Objects.equal(keytabPath, principal1.keytabPath);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hostId, hostName, principal, isService, cacheFile, serviceMapping, keytabPath);
  }
}

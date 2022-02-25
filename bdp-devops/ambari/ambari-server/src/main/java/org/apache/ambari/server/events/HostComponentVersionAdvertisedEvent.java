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
package org.apache.ambari.server.events;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * The {@link HostComponentVersionAdvertisedEvent}
 * occurs when a Host Component advertises it's current version value.
 */
public class HostComponentVersionAdvertisedEvent extends ClusterEvent {

  protected Cluster cluster;
  protected ServiceComponentHost sch;
  protected String version;
  protected Long repoVersionId;

  /**
   * Constructor.
   *
   * @param cluster: cluster.
   * @param sch: the service component host
   */
  public HostComponentVersionAdvertisedEvent(Cluster cluster, ServiceComponentHost sch,
      String version, Long repoVersionId) {
    this(cluster, sch, version);
    this.repoVersionId = repoVersionId;
  }

  /**
   * Constructor.
   *
   * @param cluster: cluster.
   * @param sch: the service component host
   */
  public HostComponentVersionAdvertisedEvent(Cluster cluster, ServiceComponentHost sch,
                                             String version) {
    super(AmbariEventType.HOST_COMPONENT_VERSION_ADVERTISED, cluster.getClusterId());
    this.cluster = cluster;
    this.sch = sch;
    this.version = version;
  }

  public ServiceComponentHost getServiceComponentHost() {
    return sch;
  }

  public Cluster getCluster() {
    return cluster;
  }

  public String getVersion() {
    return version;
  }

  public Long getRepositoryVersionId() {
    return repoVersionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("HostComponentVersionAdvertisedEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append(", serviceName=").append(sch.getServiceName());
    buffer.append(", componentName=").append(sch.getServiceComponentName());
    buffer.append(", hostName=").append(sch.getHostName());
    buffer.append(", version=").append(version);
    buffer.append(", repo_version_id=").append(repoVersionId);
    buffer.append("}");
    return buffer.toString();
  }
}

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

package org.apache.ambari.server.stack;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * Wrapper around a collection of hosts for components.  Some components
 * also have master/secondary designators.
 */
@Experimental(feature=ExperimentalFeature.REFACTOR_TO_SPI)
public class HostsType {
  /**
   * List of HA hosts (master - secondaries pairs), if any.
   */
  private final List<HighAvailabilityHosts> highAvailabilityHosts;

  /**
   * Ordered collection of hosts.  This represents all hosts where an upgrade
   * or downgrade must occur.  For an upgrade, all hosts are set.  For a downgrade,
   * this list may be a subset of all hosts where the downgrade MUST be executed.
   * That is to say, a downgrade only occurs where the current version is not
   * the target version.
   */
  private LinkedHashSet<String> hosts;

  /**
   * Unhealthy hosts are those which are explicitly put into maintenance mode.
   * If there is a host which is not heartbeating (or is generally unhealthy)
   * but not in maintenance mode, then the prerequisite upgrade checks will let
   * the administrator know that it must be put into maintenance mode before an
   * upgrade can begin.
   */
  public List<ServiceComponentHost> unhealthy = new ArrayList<>();

  /**
   * @return true if master components list is not empty
   */
  public boolean hasMasters() {
    return !getMasters().isEmpty();
  }

  public List<HighAvailabilityHosts> getHighAvailabilityHosts() {
    return highAvailabilityHosts;
  }

  /**
   * Order the hosts so that for each HA host the secondaries come first.
   * For example: [sec1, sec2, master1, sec3, sec4, master2]
   */
  public void arrangeHostSecondariesFirst() {
    hosts = getHighAvailabilityHosts().stream()
      .flatMap(each -> Stream.concat(each.getSecondaries().stream(), Stream.of(each.getMaster())))
      .collect(toCollection(LinkedHashSet::new));
  }

  /**
   * @return true if both master and secondary components lists are not empty
   */
  public boolean hasMastersAndSecondaries() {
    return !getMasters().isEmpty() && !getSecondaries().isEmpty();
  }

  /**
   * A master and secondary host(s). In HA mode there is one master and one secondary host,
   * in federated mode there can be more than one secondaries.
   */
  public static class HighAvailabilityHosts {
    private final String master;
    private final List<String> secondaries;

    public HighAvailabilityHosts(String master, List<String> secondaries) {
      if (master == null) {
        throw new IllegalArgumentException("Master host is missing");
      }
      this.master = master;
      this.secondaries = secondaries;
    }

    public String getMaster() {
      return master;
    }

    public List<String> getSecondaries() {
      return secondaries;
    }
  }

  /**
   * Creates an instance from the optional master and secondary components and with the given host set
   */
  public static HostsType from(String master, String secondary, LinkedHashSet<String> hosts) {
    return master == null
      ? normal(hosts)
      : new HostsType(singletonList(new HighAvailabilityHosts(master, secondary != null ? singletonList(secondary) : emptyList())), hosts);

  }

  /**
   * Create an instance with exactly one high availability host (master-secondary pair) and with the given host set
   */
  public static HostsType highAvailability(String master, String secondary, LinkedHashSet<String> hosts) {
    return new HostsType(singletonList(new HighAvailabilityHosts(master, singletonList(secondary))), hosts);
  }

  /**
   * Create an instance with an arbitrary chosen high availability host.
   */
  public static HostsType guessHighAvailability(LinkedHashSet<String> hosts) {
    if (hosts.isEmpty()) {
      throw new IllegalArgumentException("Cannot guess HA, empty hosts.");
    }
    String master = hosts.iterator().next();
    List<String> secondaries = hosts.stream().skip(1).collect(toList());
    return new HostsType(singletonList(new HighAvailabilityHosts(master, secondaries)), hosts);
  }

  /**
   * Create an instance with multiple high availability hosts.
   */
  public static HostsType federated(List<HighAvailabilityHosts> highAvailabilityHosts, LinkedHashSet<String> hosts) {
    return new HostsType(highAvailabilityHosts, hosts);
  }

  /**
   * Create an instance without high availability hosts.
   */
  public static HostsType normal(LinkedHashSet<String> hosts) {
    return new HostsType(emptyList(), hosts);
  }

  /**
   * Create an instance without high availability hosts.
   */
  public static HostsType normal(String... hosts) {
    return new HostsType(emptyList(), new LinkedHashSet<>(asList(hosts)));
  }

  /**
   * Create an instance with a single (non high availability) host.
   */
  public static HostsType single(String host) {
    return HostsType.normal(host);
  }

  /**
   * Create an instance with all healthy hosts.
   */
  public static HostsType healthy(Cluster cluster) {
    LinkedHashSet<String> hostNames = new LinkedHashSet<>();
    for (Host host : cluster.getHosts()) {
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      if (maintenanceState == MaintenanceState.OFF) {
        hostNames.add(host.getHostName());
      }
    }

    return normal(hostNames);
  }

  private HostsType(List<HighAvailabilityHosts> highAvailabilityHosts, LinkedHashSet<String> hosts) {
    this.highAvailabilityHosts = highAvailabilityHosts;
    this.hosts = hosts;
  }

  public LinkedHashSet<String> getMasters() {
    return highAvailabilityHosts.stream().map(each -> each.getMaster()).collect(toCollection(LinkedHashSet::new));
  }

  public LinkedHashSet<String> getSecondaries() {
    return highAvailabilityHosts.stream().flatMap(each -> each.getSecondaries().stream()).collect(toCollection(LinkedHashSet::new));
  }

  public Set<String> getHosts() {
    return hosts;
  }

  public void setHosts(LinkedHashSet<String> hosts) {
    this.hosts = hosts;
  }
}

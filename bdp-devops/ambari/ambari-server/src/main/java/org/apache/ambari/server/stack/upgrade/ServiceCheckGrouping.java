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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grouping that is used to create stages that are service checks for a cluster.
 */
@XmlType(name="service-check")
public class ServiceCheckGrouping extends Grouping {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceCheckGrouping.class);

  /**
   * During a Rolling Upgrade, the priority services are ran first, then the remaining services in the cluster.
   * During a Stop-and-Start Upgrade, only the priority services are ran.
   */
  @XmlElementWrapper(name="priority")
  @XmlElement(name="service")
  private Set<String> priorityServices = new LinkedHashSet<>();

  /**
   * During a Rolling Upgrade, exclude certain services.
   */
  @XmlElementWrapper(name="exclude")
  @XmlElement(name="service")
  private Set<String> excludeServices = new HashSet<>();

  @Override
  protected boolean serviceCheckAfterProcessing() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceCheckBuilder getBuilder() {
    return new ServiceCheckBuilder(this);
  }

  /**
   * @return the set of service names that should be given priority
   */
  public Set<String> getPriorities() {
    return priorityServices;
  }

  /**
   * @return the set of service names that should be excluded
   */
  public Set<String> getExcluded() {
    return excludeServices;
  }

  /**
   * Used to build stages for service check groupings.
   */
  public class ServiceCheckBuilder extends StageWrapperBuilder {

    private Cluster m_cluster;
    private AmbariMetaInfo m_metaInfo;

    /**
     * Constructor.
     *
     * @param grouping
     *          the upgrade/downgrade grouping (not {@code null}).
     */
    protected ServiceCheckBuilder(Grouping grouping) {
      super(grouping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(UpgradeContext ctx, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
      // !!! nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {
      m_cluster = upgradeContext.getCluster();
      m_metaInfo = upgradeContext.getAmbariMetaInfo();

      List<StageWrapper> result = new ArrayList<>(stageWrappers);
      if (upgradeContext.getDirection().isDowngrade()) {
        return result;
      }

      Map<String, Service> serviceMap = m_cluster.getServices();

      Set<String> clusterServices = new LinkedHashSet<>(serviceMap.keySet());

      // create stages for the priorities
      for (String service : priorityServices) {
        if (checkServiceValidity(upgradeContext, service, serviceMap)) {
          StageWrapper wrapper = new ServiceCheckStageWrapper(service,
              upgradeContext.getServiceDisplay(service), true);

          result.add(wrapper);
          clusterServices.remove(service);
        }
      }

      if (upgradeContext.getType() == UpgradeType.ROLLING) {
        // During Rolling Upgrade, create stages for everything else, as long it is valid
        for (String service : clusterServices) {
          if (excludeServices.contains(service)) {
            continue;
          }

          if (checkServiceValidity(upgradeContext, service, serviceMap)) {
            StageWrapper wrapper = new ServiceCheckStageWrapper(service,
                upgradeContext.getServiceDisplay(service), false);

            result.add(wrapper);
          }
        }
      }
      return result;
    }

    /**
     * Checks if the service is valid for a service check
     * @param ctx             the upgrade context to set the display name
     * @param service         the name of the service to check
     * @param clusterServices the map of available services for a cluster
     * @return {@code true} if the service is valid and can execute a service check
     */
    private boolean checkServiceValidity(UpgradeContext ctx, String service, Map<String, Service> clusterServices) {
      if (clusterServices.containsKey(service)) {
        Service svc = clusterServices.get(service);
        if (null != svc) {
          // Services that only have clients such as Pig can still have service check scripts.
          StackId stackId = svc.getDesiredStackId();
          try {
            ServiceInfo si = m_metaInfo.getService(stackId.getStackName(), stackId.getStackVersion(), service);
            CommandScriptDefinition script = si.getCommandScript();
            if (null != script && null != script.getScript() && !script.getScript().isEmpty()) {
              ctx.setServiceDisplay(service, si.getDisplayName());
              return true;
            }
          } catch (AmbariException e) {
            LOG.error("Could not determine if service " + service + " can run a service check. Exception: " + e.getMessage());
          }
        }
      }
      return false;
    }
  }

  /**
   * Attempts to merge all the service check groupings.  This merges the excluded list and
   * the priorities.  The priorities are merged in an order specific manner.
   */
  @Override
  public void merge(Iterator<Grouping> iterator) throws AmbariException {
    List<String> priorities = new ArrayList<>();
    priorities.addAll(getPriorities());
    Map<String, Set<String>> skippedPriorities = new HashMap<>();
    while (iterator.hasNext()) {
      Grouping next = iterator.next();
      if (!(next instanceof ServiceCheckGrouping)) {
        throw new AmbariException("Invalid group type " + next.getClass().getSimpleName() + " expected service check group");
      }
      ServiceCheckGrouping checkGroup = (ServiceCheckGrouping) next;
      getExcluded().addAll(checkGroup.getExcluded());

      boolean added = addPriorities(priorities, checkGroup.getPriorities(), checkGroup.addAfterGroupEntry);
      if (added) {
        addSkippedPriorities(priorities, skippedPriorities, checkGroup.getPriorities());
      }
      else {
        // store these services until later
        if (skippedPriorities.containsKey(checkGroup.addAfterGroupEntry)) {
          Set<String> tmp = skippedPriorities.get(checkGroup.addAfterGroupEntry);
          tmp.addAll(checkGroup.getPriorities());
        }
        else {
          skippedPriorities.put(checkGroup.addAfterGroupEntry, checkGroup.getPriorities());
        }
      }
    }
    getPriorities().clear();
    getPriorities().addAll(priorities);
  }

  /**
   * Add the given child priorities if the service they are supposed to come after have been added.
   */
  private boolean addPriorities(List<String> priorities, Set<String> childPriorities, String after) {
    if (after == null) {
      priorities.addAll(childPriorities);
      return true;
    }
    else {
      // Check the current priorities, if the "after" priority is there then add these
      for (int index = priorities.size() - 1; index >= 0; index--) {
        String priority = priorities.get(index);
        if (after.equals(priority)) {
          priorities.addAll(index + 1, childPriorities);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Add the skipped priorities if the services they are supposed to come after have been added
   */
  private void addSkippedPriorities(List<String> priorities, Map<String, Set<String>> skippedPriorites, Set<String> prioritiesJustAdded) {
    for (String priority : prioritiesJustAdded) {
      if (skippedPriorites.containsKey(priority)) {
        Set<String> prioritiesToAdd = skippedPriorites.remove(priority);
        addPriorities(priorities, prioritiesToAdd, priority);
        addSkippedPriorities(priorities, skippedPriorites, prioritiesToAdd);
      }
    }
  }

  /**
   * Special type of stage wrapper that allows inspection of the service check for
   * merging if required.  This prevents consecutive service checks from running, particularly
   * for Patch or Maintenance types of upgrades.
   */
  public static class ServiceCheckStageWrapper extends StageWrapper {
    public String service;
    public boolean priority;


    ServiceCheckStageWrapper(String service, String serviceDisplay, boolean priority) {
      this(service, serviceDisplay, priority, null);
    }

    public ServiceCheckStageWrapper(String service, String serviceDisplay, boolean priority, String host) {
      super(StageWrapper.Type.SERVICE_CHECK,
          String.format("Service Check %s", serviceDisplay),
          new TaskWrapper(service, "",
              null == host ? Collections.emptySet() : Collections.singleton(host),
              new ServiceCheckTask()));

      this.service = service;
      this.priority = priority;
    }

    @Override
    public int hashCode() {
      return service.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!other.getClass().equals(getClass())) {
        return false;
      }

      if (other == this) {
        return true;
      }

      return ((ServiceCheckStageWrapper) other).service.equals(service);

    }

  }

}

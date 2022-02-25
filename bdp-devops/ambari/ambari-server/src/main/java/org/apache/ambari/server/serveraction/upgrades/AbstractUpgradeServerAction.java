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
package org.apache.ambari.server.serveraction.upgrades;

import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackInfo;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Abstract class that reads values from command params in a consistent way.
 */
public abstract class AbstractUpgradeServerAction extends AbstractServerAction {

  public Clusters getClusters() {
    return m_clusters;
  }

  @Inject
  protected Clusters m_clusters;

  /**
   * Used to move desired repo versions forward.
   */
  @Inject
  protected UpgradeHelper m_upgradeHelper;

  /**
   * Used to lookup or update {@link UpgradeEntity} instances.
   */
  @Inject
  protected UpgradeDAO m_upgradeDAO;

  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies.
   */
  @Inject
  private UpgradeContextFactory m_upgradeContextFactory;

  /**
   * Used for updating push data to the agents.
   */
  @Inject
  protected AgentConfigsHolder agentConfigsHolder;

  /**
   * Used for getting references to objects like {@link StackInfo}.
   */
  @Inject
  protected Provider<AmbariMetaInfo> m_metainfoProvider;

  /**
   * Used for manipulting configurations, such as removing entire types and
   * creating new ones.
   */
  @Inject
  protected ConfigHelper m_configHelper;

  /**
   * Who knows what this is used for or why it even exists.
   */
  @Inject
  protected AmbariManagementController m_amc;

  /**
   * Gets the injected instance of the {@link Gson} serializer/deserializer.
   *
   * @return the injected {@link Gson} instance.
   */
  protected Gson getGson() {
    return gson;
  }

  /**
   * Gets an initialized {@link UpgradeContext} for the in-progress upgrade.
   */
  protected UpgradeContext getUpgradeContext(Cluster cluster) {
    UpgradeEntity upgrade = cluster.getUpgradeInProgress();
    UpgradeContext upgradeContext = m_upgradeContextFactory.create(cluster, upgrade);
    return upgradeContext;
  }
}

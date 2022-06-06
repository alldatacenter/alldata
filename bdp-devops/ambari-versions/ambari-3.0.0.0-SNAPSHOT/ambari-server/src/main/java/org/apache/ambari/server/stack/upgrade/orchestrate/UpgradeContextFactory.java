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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;

/**
 * The {@link UpgradeContextFactory} is used to create dependency-injected
 * instances of {@link UpgradeContext}s.
 */
public interface UpgradeContextFactory {

  /**
   * Creates an {@link UpgradeContext} which is injected with dependencies.
   *
   * @param cluster
   *          the cluster that the upgrade is for (not {@code null}).
   * @param upgradeRequestMap
   *          the original map of parameters used to create the upgrade (not
   *          {@code null}).
   *
   * @return an initialized {@link UpgradeContext}.
   */
  UpgradeContext create(Cluster cluster, Map<String, Object> upgradeRequestMap)
      throws AmbariException;

  /**
   * Creates an {@link UpgradeContext} which is injected with dependencies.
   *
   * @param cluster
   *          the cluster that the upgrade is for (not {@code null}).
   * @param upgradeEntity
   *          the upgrade entity (not {@code null}).
   * @return an initialized {@link UpgradeContext}.
   */
  UpgradeContext create(Cluster cluster, UpgradeEntity upgradeEntity);
}

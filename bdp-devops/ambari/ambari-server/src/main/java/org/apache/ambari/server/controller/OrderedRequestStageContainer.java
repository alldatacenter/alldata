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
package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;

/**
 * An extension of RequestStageContainer that takes the role command order into consideration when adding stages
 */
public class OrderedRequestStageContainer {
  private final RoleGraphFactory roleGraphFactory;
  private final RoleCommandOrder roleCommandOrder;
  private final RequestStageContainer requestStageContainer;

  public OrderedRequestStageContainer(RoleGraphFactory roleGraphFactory, RoleCommandOrder roleCommandOrder, RequestStageContainer requestStageContainer) {
    this.roleGraphFactory = roleGraphFactory;
    this.roleCommandOrder = roleCommandOrder;
    this.requestStageContainer = requestStageContainer;
  }

  public void addStage(Stage stage) throws AmbariException {
    RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
    roleGraph.build(stage);
    requestStageContainer.addStages(roleGraph.getStages());
  }

  public long getLastStageId() {
    return requestStageContainer.getLastStageId();
  }

  public long getId() {
    return requestStageContainer.getId();
  }

  public RequestStageContainer getRequestStageContainer() {
    return requestStageContainer;
  }
}

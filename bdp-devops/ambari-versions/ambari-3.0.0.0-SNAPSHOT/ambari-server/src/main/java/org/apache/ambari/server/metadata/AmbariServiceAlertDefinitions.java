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
package org.apache.ambari.server.metadata;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AmbariServiceAlertDefinitions} class is used to represent the
 * alerts defined in {@code alerts.json} which are for
 * {@link RootComponent#AMBARI_AGENT} and {@link RootComponent#AMBARI_SERVER}. These
 * alerts are bound to the host and are not part of a cluster or hadoop service.
 */
@Singleton
public class AmbariServiceAlertDefinitions {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AmbariServiceAlertDefinitions.class);

  /**
   * The agent host definitions.
   */
  private List<AlertDefinition> m_agentDefinitions = null;

  /**
   * The server definitions.
   */
  private List<AlertDefinition> m_serverDefinitions = null;

  /**
   * The factory that will load the definitions from the alerts.json file.
   */
  @Inject
  private AlertDefinitionFactory m_factory;

  /**
   * Gets all of the {@link AlertDefinition}s that exist on the path for all
   * agent hosts.
   *
   * @return the alerts with {@link RootComponent#AMBARI_AGENT} as the component
   *         and {@code AMBARI} as the service.
   */
  public List<AlertDefinition> getAgentDefinitions() {
    if (null != m_agentDefinitions) {
      return m_agentDefinitions;
    }

    m_agentDefinitions = getDefinitions(RootComponent.AMBARI_AGENT);
    return m_agentDefinitions;
  }

  /**
   * Gets all of the {@link AlertDefinition}s that exist on the path for
   * {@link RootComponent#AMBARI_SERVER}.
   *
   * @return the alerts with {@link RootComponent#AMBARI_SERVER} as the component
   *         and {@code AMBARI} as the service.
   */
  public List<AlertDefinition> getServerDefinitions() {
    if (null != m_serverDefinitions) {
      return m_serverDefinitions;
    }

    m_serverDefinitions = getDefinitions(RootComponent.AMBARI_SERVER);
    return m_serverDefinitions;
  }

  /**
   * Loads the definitions for the {@code AMBARI} service for the specified
   * component.
   *
   * @param component
   *          the component (not {@code null}).
   * @return the alert definitions for {@code AMBARI} service for the given
   *         component.
   */
  private List<AlertDefinition> getDefinitions(RootComponent component) {
    List<AlertDefinition> definitions = new ArrayList<>();

    InputStream inputStream = ClassLoader.getSystemResourceAsStream("alerts.json");
    InputStreamReader reader = new InputStreamReader(inputStream);

    try {
      Set<AlertDefinition> allDefinitions = m_factory.getAlertDefinitions(
          reader, RootService.AMBARI.name());

      String componentName = component.name();

      for (AlertDefinition definition : allDefinitions) {
        if (componentName.equals(definition.getComponentName())) {
          definitions.add(definition);
        }
      }

    } catch (Exception exception) {
      LOG.error("Unable to load the Ambari alerts JSON file", exception);
    }

    return definitions;
  }
}

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.serveraction.upgrades.AddComponentAction;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;

import com.google.gson.annotations.Expose;

/**
 * The {@link AddComponentTask} is used for adding components during an upgrade.
 * Components which are added via the task will also be scheduled for a restart
 * if they appear in the upgrade pack as part of a restart group. This is true
 * even if they do not exist in the cluster yet.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "add_component")
public class AddComponentTask extends ServerSideActionTask {

  /**
   * The key which represents this task serialized.
   */
  public static final String PARAMETER_SERIALIZED_ADD_COMPONENT_TASK = "add-component-task";

  @Expose
  @XmlTransient
  private Task.Type type = Type.ADD_COMPONENT;

  /**
   * The hosts to run the task on. Default to running on
   * {@link ExecuteHostType#ANY}.
   */
  @Expose
  @XmlAttribute
  public ExecuteHostType hosts = ExecuteHostType.ANY;

  /**
   * The service which owns the component to add.
   */
  @Expose
  @XmlAttribute
  public String service;

  /**
   * The component to add.
   */
  @Expose
  @XmlAttribute
  public String component;

  /**
   * Specifies the hosts which are valid for adding the new component,
   * restricted by service.
   */
  @Expose
  @XmlAttribute(name = "host-service")
  public String hostService;

  /**
   * Specifies the hosts which are valid for adding teh new component,
   * restricted by component.
   */
  @Expose
  @XmlAttribute(name = "host-component")
  public String hostComponent;

  /**
   * Constructor.
   *
   */
  public AddComponentTask() {
    implClass = AddComponentAction.class.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Task.Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.SERVER_SIDE_ACTION;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getActionVerb() {
    return "Adding";
  }

  /**
   * Gets a JSON representation of this task.
   *
   * @return a JSON representation of this task.
   */
  public String toJson() {
    return GSON.toJson(this);
  }

  /**
   * Gets a string which is comprised of serviceName/componentName
   *
   * @return a string which represents this add component task.
   */
  public String getServiceAndComponentAsString() {
    return service + "/" + component;
  }
}

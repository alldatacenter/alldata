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

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * Represents a single-stage execution that happens as part of a cluster-wide
 * upgrade or downgrade.
 */
public class ExecuteStage {
  @XmlAttribute(name="title")
  public String title;

  /**
   * An optional ID which can be used to uniquely identified any execution
   * stage.
   */
  @XmlAttribute(name="id")
  public String id;

  @XmlElement(name="direction")
  public Direction intendedDirection = null;

  /**
   * Optional service name, can be ""
   */
  @XmlAttribute(name="service")
  public String service;

  /**
   * Optional component name, can be ""
   */
  @XmlAttribute(name="component")
  public String component;

  @XmlElement(name="task")
  public Task task;

  @XmlElement(name="scope")
  public UpgradeScope scope = UpgradeScope.ANY;

  /**
   * A condition element with can prevent this stage from being scheduled in
   * the upgrade.
   */
  @XmlElement(name = "condition")
  public Condition condition;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("title",
        title).omitNullValues().toString();
  }

  /**
   * If a task is found that is configure, set its associated service.  This is used
   * if the configuration type cannot be isolated by service.
   */
  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (task.getType().equals(Task.Type.CONFIGURE) && StringUtils.isNotEmpty(service)) {
      ((ConfigureTask) task).associatedService = service;
    } else if (task.getType().equals(Task.Type.CREATE_AND_CONFIGURE) && StringUtils.isNotEmpty(service)) {
      ((CreateAndConfigureTask) task).associatedService = service;
    }
  }


}
/**
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.serveraction.upgrades.CreateAndConfigureAction;

/**
 * The {@link CreateAndConfigureTask} represents a two step change where the create is for creating a config type if it does not exist
 * followed by the configuration change.
 * This task contains id of change. Change definitions are located in a separate file (config
 * upgrade pack). IDs of change definitions share the same namespace within all stacks.
 *
 *
 * <p/>
 *
 * <pre>
 * {@code
 * <task xsi:type="create_and_configure" id="hdp_2_3_0_0-UpdateHiveConfig"/>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="create_and_configure")
public class CreateAndConfigureTask extends ConfigureTask {

  public static final String actionVerb = "CreateAndConfiguring";

  /**
   * Constructor.
   */
  public CreateAndConfigureTask() {
    implClass = CreateAndConfigureAction.class.getName();
  }
}
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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;

/**
 * The {@link Condition} interface represents a condition in an
 * {@link UpgradePack} which must be satisified in order for a {@link Grouping}
 * or {@link ExecuteStage} to be scheduled.
 */
@XmlRootElement
@XmlSeeAlso(value = { SecurityCondition.class, ConfigurationCondition.class })
public class Condition {

  /**
   * Asks the condition whether it is currently satisfied.
   *
   * @param upgradeContext
   *          the upgrade context.
   * @return {@code true} if the condition is satisfied, {@code false}
   *         otherwise.
   */
  public boolean isSatisfied(UpgradeContext upgradeContext) {
    return false;
  }
}

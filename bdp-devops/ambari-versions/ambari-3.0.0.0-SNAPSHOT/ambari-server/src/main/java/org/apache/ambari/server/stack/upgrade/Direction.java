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

import org.apache.commons.lang.StringUtils;

/**
 * Indicates if an sequence of Groups should be for an upgrade or a downgrade.
 */
public enum Direction {
  UPGRADE,
  DOWNGRADE;

  /**
   * @return {@code true} if the direction is for upgrade.  Convenience instead
   * of equality checking.
   */
  public boolean isUpgrade() {
    return this == UPGRADE;
  }

  /**
   * @return {@code true} if the direction is for downgrade.  Convenience instead
   * of equality checking.
   */
  public boolean isDowngrade() {
    return this == DOWNGRADE;
  }


  /**
   * @param proper {@code true} to make the first letter captilized
   * @return "upgrade" or "downgrade"
   */
  public String getText(boolean proper) {
    return proper ? StringUtils.capitalize(name().toLowerCase()) :
      name().toLowerCase();
  }

  /**
   * @param proper {@code true} to make the first letter captilized
   * @return "upgraded" or "downgraded"
   */
  public String getPast(boolean proper) {
    return getText(proper) + "d";
  }

  /**
   * @param proper {@code true} to make the first letter captilized
   * @return "upgrades" or "downgrades"
   */
  public String getPlural(boolean proper) {
    return getText(proper) + "s";
  }

  /**
   * @param proper {@code true} to make the first letter captilized
   * @return "upgrading" or "downgrading"
   */
  public String getVerb(boolean proper) {
    String verb = (this == UPGRADE) ? "upgrading" : "downgrading";

    return proper ? StringUtils.capitalize(verb) : verb;
  }


  /**
   * Gets the preposition based on the direction. Since the repository is
   * singular, it will either be "to repo" or "from repo".
   *
   * @return "to" or "from"
   */
  public String getPreposition() {
    return (this == UPGRADE) ? "to" : "from";
  }
}

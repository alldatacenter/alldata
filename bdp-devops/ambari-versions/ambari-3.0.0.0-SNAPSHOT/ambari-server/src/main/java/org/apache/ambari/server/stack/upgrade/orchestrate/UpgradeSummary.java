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

import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeType;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link UpgradeSummary} class is a simple POJO used to serialize the
 * infomration about and upgrade.
 */
public class UpgradeSummary {
  @SerializedName("direction")
  public Direction direction;

  @SerializedName("type")
  public UpgradeType type;

  @SerializedName("orchestration")
  public RepositoryType orchestration;

  @SerializedName("isRevert")
  public boolean isRevert = false;

  @SerializedName("downgradeAllowed")
  public boolean isDowngradeAllowed = true;

  @SerializedName("services")
  public Map<String, UpgradeServiceSummary> services;

  /**
   * The ID of the repository associated with the upgrade. For an
   * {@link Direction#UPGRADE}, this is the target repository, for a
   * {@link Direction#DOWNGRADE} this was the repository being downgraded
   * from.
   */
  @SerializedName("associatedRepositoryId")
  public long associatedRepositoryId;

  /**
   * The ID of the repository associated with the upgrade. For an
   * {@link Direction#UPGRADE}, this is the target stack, for a
   * {@link Direction#DOWNGRADE} this was the stack that is being downgraded
   * from.
   */
  @SerializedName("associatedStackId")
  public String associatedStackId;

  /**
   * The ID of the repository associated with the upgrade. For an
   * {@link Direction#UPGRADE}, this is the target versopm, for a
   * {@link Direction#DOWNGRADE} this was the version that is being downgraded
   * from.
   */
  @SerializedName("associatedVersion")
  public String associatedVersion;

  /**
   * MAINT or PATCH upgrades are meant to just be switching the bits and no other
   * incompatible changes.
   */
  @SerializedName("isSwitchBits")
  public boolean isSwitchBits = false;
}
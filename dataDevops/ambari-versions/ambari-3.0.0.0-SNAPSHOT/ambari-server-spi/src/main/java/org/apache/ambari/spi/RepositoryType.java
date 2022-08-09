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
package org.apache.ambari.spi;

import java.util.EnumSet;

/**
 * Identifies the type of repository
 */
public enum RepositoryType {

  /**
   * Repository should be considered to have all components for a cluster
   * deployment.
   */
  STANDARD,

  /**
   * Repository may have only minimum components and is used for patching
   * purposes.
   */
  PATCH,

  /**
   * Repository is used as Maintenance release, which could be several patches rolled up in one.
   * Orchestration should treat Maintenance just as it does for Patch..
   */
  MAINT,

  /**
   * Repository is used to update services.
   */
  SERVICE;

  /**
   * The types of repositories which are revertable.
   */
  public static final EnumSet<RepositoryType> REVERTABLE = EnumSet.of(RepositoryType.MAINT,
      RepositoryType.PATCH);

  /**
   * The types of repositories which can participate in an upgrade where only
   * some services are orchestrated.
   */
  public static final EnumSet<RepositoryType> PARTIAL = EnumSet.of(RepositoryType.MAINT,
      RepositoryType.PATCH, RepositoryType.SERVICE);

  /**
   * Gets whether applications of this repository are revertable after they have
   * been finalized.
   *
   * @return {@code true} if the repository can be revert, {@code false}
   *         otherwise.
   */
  public boolean isRevertable() {
    switch (this) {
      case MAINT:
      case PATCH:
        return true;
      case SERVICE:
      case STANDARD:
        return false;
      default:
        return false;
    }
  }

  /**
   * Gets whether this repository type can be used to upgrade only a subset of
   * services.
   *
   * @return {@code true} if the repository can be be applied to a subset of
   *         isntalled services, {@code false} otherwise.
   */
  public boolean isPartial() {
    return PARTIAL.contains(this);
  }
}

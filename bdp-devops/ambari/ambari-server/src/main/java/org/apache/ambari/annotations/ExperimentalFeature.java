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
package org.apache.ambari.annotations;

/**
 * The {@link ExperimentalFeature} enumeration is meant to be used with the
 * {@link Experimental} annotation to indicate which feature set experimental
 * code belongs to.
 */
public enum ExperimentalFeature {
  /**
   * The caching of current alert information in order to reduce overall load on
   * the database by preventing frequent updates and JPA entity invalidation.
   */
  ALERT_CACHING,

  /**
   * Used for code that is targeted for patch upgrades
   */
  PATCH_UPGRADES,

  /**
   * For code that is for multi-service
   */
  MULTI_SERVICE,

  /**
   * Support for service-specific repos for custom services
   */
  CUSTOM_SERVICE_REPOS,

  /**
   * Automatically removing Kerberos identities when a service or component is
   * removed.
   */
  ORPHAN_KERBEROS_IDENTITY_REMOVAL,

  /**
   * Member should be refactored to SPI project
   */
  REFACTOR_TO_SPI;
}

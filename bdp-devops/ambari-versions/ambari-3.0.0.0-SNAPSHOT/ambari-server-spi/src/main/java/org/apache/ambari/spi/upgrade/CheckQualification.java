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
package org.apache.ambari.spi.upgrade;

import org.apache.ambari.server.AmbariException;

/**
 * The {@link CheckQualification} interface is used to provide multiple
 * different qualifications against which an upgrade check is determined to be
 * applicable to the upgrade.
 */
public interface CheckQualification {

  /**
   * Gets whether the upgrade check meets this qualification and should
   * therefore be run before the upgrade.
   *
   * @param request
   *          the upgrade check request.
   * @return {@code true} if the upgrade check is applicable to the specific
   *         upgrade request, or {@code false} otherwise.
   * @throws AmbariException
   *           if there was an error attempting to determine if the upgrade
   *           check is applicable to the desired upgrade.
   */
  boolean isApplicable(UpgradeCheckRequest request) throws AmbariException;
}
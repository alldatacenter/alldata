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
package org.apache.ambari.spi.upgrade;

import org.apache.ambari.server.AmbariException;

/**
 * The {@link PriorCheckQualification} class is used to determine if a prior
 * check has run.
 */
public class PriorCheckQualification implements CheckQualification {

  private final UpgradeCheckDescription m_checkDescription;

  public PriorCheckQualification(UpgradeCheckDescription checkDescription) {
    m_checkDescription = checkDescription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckStatus checkStatus = request.getResult(m_checkDescription);
    if (null != checkStatus && checkStatus == UpgradeCheckStatus.FAIL) {
      return false;
    }

    return true;
  }
}
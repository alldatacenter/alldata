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
package org.apache.ambari.server.sample.checks;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.ClusterCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;

import com.google.common.collect.ImmutableMap;

public class SampleServiceCheck extends ClusterCheck {

  public SampleServiceCheck() {
    super(new UpgradeCheckDescription("SAMPLE_SERVICE_CHECK",
          UpgradeCheckType.HOST,
          "Sample service check description.",
          new ImmutableMap.Builder<String, String>()
                          .put(UpgradeCheckDescription.DEFAULT,
                              "Sample service check default property description.").build()));
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);
    result.setFailReason("Sample service check always fails.");
    result.setStatus(UpgradeCheckStatus.FAIL);

    return result;
  }


}

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
package org.apache.ambari.server.checks;

import java.util.List;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.spi.upgrade.CheckQualification;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the {@link UpgradeTypeQualification}.
 */
public class UpgradeTypeQualificationTest {

  /**
   * Tests whether the {@link UpgradeCheckInfo} annotation is correctly
   * analyzed.
   */
  @Test
  public void testRequired() throws Exception {
    UpgradeCheckRequest rolling = new UpgradeCheckRequest(null, UpgradeType.ROLLING, null, null, null);
    UpgradeCheckRequest express = new UpgradeCheckRequest(null, UpgradeType.NON_ROLLING, null, null, null);

    UpgradeTypeQualification rollingQualification = new UpgradeTypeQualification(RollingTestCheckImpl.class);

    Assert.assertTrue(rollingQualification.isApplicable(rolling));
    Assert.assertFalse(rollingQualification.isApplicable(express));

    UpgradeTypeQualification notRequiredQualification = new UpgradeTypeQualification(NotRequiredCheckTest.class);
    Assert.assertTrue(notRequiredQualification.isApplicable(rolling));
    Assert.assertTrue(notRequiredQualification.isApplicable(express));
  }

  @UpgradeCheckInfo(
      group = UpgradeCheckGroup.DEFAULT,
      order = 1.0f,
      required = { UpgradeType.ROLLING })
  private class RollingTestCheckImpl implements UpgradeCheck {

    @Override
    public Set<String> getApplicableServices() {
      return null;
    }

    @Override
    public List<CheckQualification> getQualifications() {
      return null;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
      return null;
    }

    @Override
    public UpgradeCheckDescription getCheckDescription() {
      return null;
    }
  }

  @UpgradeCheckInfo(group = UpgradeCheckGroup.DEFAULT, order = 1.0f)
  private class NotRequiredCheckTest implements UpgradeCheck {
    @Override
    public Set<String> getApplicableServices() {
      return null;
    }

    @Override
    public List<CheckQualification> getQualifications() {
      return null;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
      return null;
    }

    @Override
    public UpgradeCheckDescription getCheckDescription() {
      return null;
    }
  }

}

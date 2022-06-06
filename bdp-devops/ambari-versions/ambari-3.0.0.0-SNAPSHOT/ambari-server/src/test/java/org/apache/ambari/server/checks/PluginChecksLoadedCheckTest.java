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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.Provider;

/**
 * Tests for {@link PluginChecksLoadedCheck}
 */
public class PluginChecksLoadedCheckTest extends EasyMockSupport {

  private final PluginChecksLoadedCheck m_check = new PluginChecksLoadedCheck();
  private final UpgradeCheckRegistry m_upgradeCheckRegistry = createNiceMock(
      UpgradeCheckRegistry.class);

  @Before
  public void before() throws Exception {

    m_check.m_upgradeCheckRegistryProvider = new Provider<UpgradeCheckRegistry>() {
      @Override
      public UpgradeCheckRegistry get() {
        return m_upgradeCheckRegistry;
      }
    };
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    Set<String> failedClasses = Sets.newHashSet("foo.bar.Baz", "foo.bar.Baz2");
    expect(m_upgradeCheckRegistry.getFailedPluginClassNames()).andReturn(
        failedClasses).atLeastOnce();

    replayAll();

    UpgradeCheckRequest request = new UpgradeCheckRequest(null, UpgradeType.ROLLING, null, null, null);
    UpgradeCheckResult check = m_check.perform(request);

    Assert.assertEquals(UpgradeCheckStatus.WARNING, check.getStatus());
    List<Object> failedDetails = check.getFailedDetail();
    assertEquals(2, failedDetails.size());
    assertEquals(2, check.getFailedOn().size());
    assertTrue(check.getFailedOn().contains("Baz"));

    verifyAll();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPerformWithSuccess() throws Exception {
    expect(m_upgradeCheckRegistry.getFailedPluginClassNames()).andReturn(
        new HashSet<>()).atLeastOnce();
    replayAll();

    UpgradeCheckRequest request = new UpgradeCheckRequest(null, UpgradeType.ROLLING, null, null, null);
    UpgradeCheckResult check = m_check.perform(request);

    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(StringUtils.isBlank(check.getFailReason()));

    verifyAll();
  }
}

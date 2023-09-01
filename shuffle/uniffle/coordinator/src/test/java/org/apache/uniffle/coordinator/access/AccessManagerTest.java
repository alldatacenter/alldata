/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator.access;

import java.util.Collections;
import java.util.Random;

import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.AccessManager;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.access.checker.AbstractAccessChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessManagerTest {
  @BeforeEach
  public void setUp() {
    CoordinatorMetrics.register();
  }

  @AfterEach
  public void clear() {
    CoordinatorMetrics.clear();
  }

  @Test
  public void test() throws Exception {
    // test init
    CoordinatorConf conf = new CoordinatorConf();
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(), " , ");
    ApplicationManager applicationManager = new ApplicationManager(conf);
    try {
      new AccessManager(conf, null, applicationManager.getQuotaManager(), new Configuration());
    } catch (RuntimeException e) {
      String expectedMessage = "Empty classes";
      assertTrue(e.getMessage().startsWith(expectedMessage));
    }
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(),
        "com.Dummy,org.apache.uniffle.coordinator.access.AccessManagerTest$MockAccessChecker");
    try {
      new AccessManager(conf, null, applicationManager.getQuotaManager(), new Configuration());
    } catch (RuntimeException e) {
      String expectedMessage = "java.lang.ClassNotFoundException: com.Dummy";
      assertTrue(e.getMessage().startsWith(expectedMessage));
    }
    // test empty checkers
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(), "");
    AccessManager accessManager = new AccessManager(conf, null,
        applicationManager.getQuotaManager(), new Configuration());
    assertTrue(accessManager.handleAccessRequest(
            new AccessInfo(String.valueOf(new Random().nextInt()),
                Sets.newHashSet(Constants.SHUFFLE_SERVER_VERSION), Collections.emptyMap(), "user"))
        .isSuccess());
    accessManager.close();
    // test mock checkers
    String alwaysTrueClassName = MockAccessCheckerAlwaysTrue.class.getName();
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(),
        alwaysTrueClassName + ",");
    accessManager = new AccessManager(conf, null,
        applicationManager.getQuotaManager(), new Configuration());
    assertEquals(1, accessManager.getAccessCheckers().size());
    assertTrue(accessManager.handleAccessRequest(new AccessInfo("mock1")).isSuccess());
    assertTrue(accessManager.handleAccessRequest(new AccessInfo("mock2")).isSuccess());
    String alwaysFalseClassName = MockAccessCheckerAlwaysFalse.class.getName();
    conf.setString(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS.key(), alwaysTrueClassName + "," + alwaysFalseClassName);
    accessManager = new AccessManager(conf, null,
        applicationManager.getQuotaManager(), new Configuration());
    assertEquals(2, accessManager.getAccessCheckers().size());
    assertFalse(accessManager.handleAccessRequest(new AccessInfo("mock1")).isSuccess());
    accessManager.close();
  }

  public static class MockAccessCheckerAlwaysTrue extends AbstractAccessChecker {
    public MockAccessCheckerAlwaysTrue(AccessManager accessManager) throws Exception {
      super(accessManager);
    }

    public void close() {
    }

    public AccessCheckResult check(AccessInfo accessInfo) {
      return new AccessCheckResult(true, "");
    }
  }

  public static class MockAccessCheckerAlwaysFalse extends AbstractAccessChecker {
    public MockAccessCheckerAlwaysFalse(AccessManager accessManager) throws Exception {
      super(accessManager);
    }

    public void close() {

    }

    public AccessCheckResult check(AccessInfo accessInfo) {
      return new AccessCheckResult(false, "MockAccessCheckerAlwaysFalse");
    }
  }
}

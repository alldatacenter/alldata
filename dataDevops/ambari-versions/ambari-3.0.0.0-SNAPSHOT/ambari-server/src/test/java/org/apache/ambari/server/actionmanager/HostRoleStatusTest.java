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

package org.apache.ambari.server.actionmanager;

import org.junit.Assert;
import org.junit.Test;

/**
 * HostRoleStatus Tests.
 */
public class HostRoleStatusTest {
  @Test
  public void testIsFailedState() throws Exception {
    Assert.assertTrue(HostRoleStatus.ABORTED.isFailedState());
    Assert.assertFalse(HostRoleStatus.COMPLETED.isFailedState());
    Assert.assertTrue(HostRoleStatus.FAILED.isFailedState());
    Assert.assertFalse(HostRoleStatus.IN_PROGRESS.isFailedState());
    Assert.assertFalse(HostRoleStatus.PENDING.isFailedState());
    Assert.assertFalse(HostRoleStatus.QUEUED.isFailedState());
    Assert.assertTrue(HostRoleStatus.TIMEDOUT.isFailedState());
    Assert.assertFalse(HostRoleStatus.HOLDING.isFailedState());
    Assert.assertFalse(HostRoleStatus.HOLDING_FAILED.isFailedState());
    Assert.assertFalse(HostRoleStatus.HOLDING_TIMEDOUT.isFailedState());
  }

  @Test
  public void testIsCompletedState() throws Exception {
    Assert.assertTrue(HostRoleStatus.ABORTED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.COMPLETED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.FAILED.isCompletedState());
    Assert.assertFalse(HostRoleStatus.IN_PROGRESS.isCompletedState());
    Assert.assertFalse(HostRoleStatus.PENDING.isCompletedState());
    Assert.assertFalse(HostRoleStatus.QUEUED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.TIMEDOUT.isCompletedState());
    Assert.assertFalse(HostRoleStatus.HOLDING.isCompletedState());
    Assert.assertFalse(HostRoleStatus.HOLDING_FAILED.isCompletedState());
    Assert.assertFalse(HostRoleStatus.HOLDING_TIMEDOUT.isCompletedState());
  }

  @Test
  public void testIsHoldingState() throws Exception {
    Assert.assertFalse(HostRoleStatus.ABORTED.isHoldingState());
    Assert.assertFalse(HostRoleStatus.COMPLETED.isHoldingState());
    Assert.assertFalse(HostRoleStatus.FAILED.isHoldingState());
    Assert.assertFalse(HostRoleStatus.IN_PROGRESS.isHoldingState());
    Assert.assertFalse(HostRoleStatus.PENDING.isHoldingState());
    Assert.assertFalse(HostRoleStatus.QUEUED.isHoldingState());
    Assert.assertFalse(HostRoleStatus.TIMEDOUT.isHoldingState());
    Assert.assertTrue(HostRoleStatus.HOLDING.isHoldingState());
    Assert.assertTrue(HostRoleStatus.HOLDING_FAILED.isHoldingState());
    Assert.assertTrue(HostRoleStatus.HOLDING_TIMEDOUT.isHoldingState());
  }
}

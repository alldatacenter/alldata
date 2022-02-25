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

package org.apache.ambari.server.serveraction;

import org.junit.Test;

import junit.framework.Assert;

public class ActionLogTest {

  @Test
  public void testWriteStdErr() throws Exception {
    ActionLog actionLog = new ActionLog();

    // Null message should not produce ill effects, they should be ignored
    actionLog.writeStdErr(null);
    Assert.assertEquals("", actionLog.getStdErr());
    Assert.assertEquals("", actionLog.getStdOut());

    // Writing to STDERR, shouldn't alter STDOUT
    actionLog.writeStdErr("This is a test message");
    Assert.assertNotNull(actionLog.getStdErr());
    Assert.assertTrue(actionLog.getStdErr().contains("This is a test message"));
    Assert.assertEquals("", actionLog.getStdOut());
  }

  @Test
  public void testWriteStdOut() throws Exception {
    ActionLog actionLog = new ActionLog();

    // Null message should not produce ill effects, they should be ignored
    actionLog.writeStdOut(null);
    Assert.assertEquals("", actionLog.getStdOut());
    Assert.assertEquals("", actionLog.getStdErr());

    // Writing to STDOUT, shouldn't alter STDERR
    actionLog.writeStdOut("This is a test message");
    Assert.assertNotNull(actionLog.getStdErr());
    Assert.assertTrue(actionLog.getStdOut().contains("This is a test message"));
    Assert.assertEquals("", actionLog.getStdErr());
  }
}
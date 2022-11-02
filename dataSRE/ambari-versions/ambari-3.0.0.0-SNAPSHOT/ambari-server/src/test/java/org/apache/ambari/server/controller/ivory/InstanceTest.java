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

package org.apache.ambari.server.controller.ivory;

import org.junit.Assert;
import org.junit.Test;

/**
 * Instance tests.
 */
public class InstanceTest {
  @Test
  public void testGetFeedName() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("Feed1", instance.getFeedName());
  }

  @Test
  public void testGetId() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("Instance1", instance.getId());
  }

  @Test
  public void testGetStatus() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("SUBMITTED", instance.getStatus());
  }

  @Test
  public void testGetStartTime() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("start", instance.getStartTime());
  }

  @Test
  public void testGetEndTime() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("end", instance.getEndTime());
  }

  @Test
  public void testGetDetails() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("details", instance.getDetails());
  }

  @Test
  public void testGetLog() throws Exception {
    Instance instance = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");
    Assert.assertEquals("log", instance.getLog());
  }
}

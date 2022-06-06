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

package org.apache.ambari.server.controller;

import org.junit.Assert;
import org.junit.Test;

public class ActionRequestTest {

  @Test
  public void testBasicGetAndSet() {
    ActionRequest adr1 =
        new ActionRequest("a1", "SYSTEM", "fileName", "HDFS", "DATANODE", "Desc1", "Any", "100");

    Assert.assertEquals("a1", adr1.getActionName());
    Assert.assertEquals("SYSTEM", adr1.getActionType());
    Assert.assertEquals("fileName", adr1.getInputs());
    Assert.assertEquals("HDFS", adr1.getTargetService());
    Assert.assertEquals("DATANODE", adr1.getTargetComponent());
    Assert.assertEquals("Desc1", adr1.getDescription());
    Assert.assertEquals("Any", adr1.getTargetType());
    Assert.assertEquals("100", adr1.getDefaultTimeout());

    adr1.setDescription("Desc2");
    adr1.setActionType("USER");

    Assert.assertEquals("Desc2", adr1.getDescription());
    Assert.assertEquals("USER", adr1.getActionType());
  }

  @Test
  public void testToString() {
    ActionRequest r1 = ActionRequest.getAllRequest();
    r1.toString();
  }

}

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

package com.bytedance.bitsail.base.ratelimit;

import org.junit.Assert;
import org.junit.Test;

public class CommunicationToolTest {

  @Test
  public void testCommunication() {
    Communication communication = new Communication();

    communication.setCounterVal(CommunicationTool.SUCCEED_RECORDS, 1L);
    communication.setCounterVal(CommunicationTool.SUCCEED_BYTES, 101L);
    communication.setCounterVal(CommunicationTool.FAILED_RECORDS, 3L);
    communication.setCounterVal(CommunicationTool.FAILED_BYTES, 33L);

    Assert.assertEquals(4L, CommunicationTool.getTotalRecords(communication));
    Assert.assertEquals(134L, CommunicationTool.getTotalBytes(communication));
  }
}

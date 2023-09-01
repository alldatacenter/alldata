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

package org.apache.uniffle.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ShuffleServerInfoTest {

  @Test
  public void testEquals() {
    ShuffleServerInfo info = new ShuffleServerInfo("1", "localhost", 1234);
    ShuffleServerInfo info2 = new ShuffleServerInfo("1", "localhost", 1234);
    assertEquals(info, info);
    assertEquals(info.hashCode(), info.hashCode());
    assertEquals(info, info2);
    assertEquals(info.hashCode(), info2.hashCode());
    assertNotEquals(info, null);
    assertNotEquals(info, new Object());

    ShuffleServerInfo info3 = new ShuffleServerInfo("2", "localhost", 1234);
    ShuffleServerInfo info4 = new ShuffleServerInfo("1", "host1", 1234);
    ShuffleServerInfo info5 = new ShuffleServerInfo("1", "localhost", 1235);
    assertNotEquals(info, info3);
    assertNotEquals(info, info4);
    assertNotEquals(info, info5);
  }


  @Test
  public void testToString() {
    ShuffleServerInfo info = new ShuffleServerInfo("1", "localhost", 1234);
    assertEquals("ShuffleServerInfo{id[" + info.getId()
        + "], host[" + info.getHost()
        + "], port[" + info.getPort()
        + "]}", info.toString());
  }

}

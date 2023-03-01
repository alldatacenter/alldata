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

package org.apache.uniffle.coordinator;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoordinatorConfTest {

  @Test
  public void test() {
    final String filePath = Objects.requireNonNull(
        getClass().getClassLoader().getResource("coordinator.conf")).getFile();
    CoordinatorConf conf = new CoordinatorConf(filePath);

    // test base conf
    assertEquals(9527, conf.getInteger(CoordinatorConf.RPC_SERVER_PORT));
    assertEquals("testRpc", conf.getString(CoordinatorConf.RPC_SERVER_TYPE));
    assertEquals(9526, conf.getInteger(CoordinatorConf.JETTY_HTTP_PORT));

    // test coordinator specific conf
    assertEquals("/a/b/c", conf.getString(CoordinatorConf.COORDINATOR_EXCLUDE_NODES_FILE_PATH));
    assertEquals(37, conf.getInteger(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX));
    assertEquals(123, conf.getLong(CoordinatorConf.COORDINATOR_HEARTBEAT_TIMEOUT));

    // test default conf
    assertEquals("PARTITION_BALANCE", conf.get(CoordinatorConf.COORDINATOR_ASSIGNMENT_STRATEGY).name());
    assertEquals(256, conf.getInteger(CoordinatorConf.JETTY_CORE_POOL_SIZE));
    assertEquals(60 * 1000, conf.getLong(CoordinatorConf.COORDINATOR_EXCLUDE_NODES_CHECK_INTERVAL));

  }

}

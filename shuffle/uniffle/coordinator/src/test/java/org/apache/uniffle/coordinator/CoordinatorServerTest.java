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

import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.util.ExitUtils;
import org.apache.uniffle.common.util.ExitUtils.ExitException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CoordinatorServerTest {

  @Test
  public void test() {
    try {
      CoordinatorConf coordinatorConf = new CoordinatorConf();
      coordinatorConf.setInteger("rss.rpc.server.port", 9537);
      coordinatorConf.setInteger("rss.jetty.http.port", 9528);
      coordinatorConf.setInteger("rss.rpc.executor.size", 10);

      CoordinatorServer cs1 = new CoordinatorServer(coordinatorConf);
      CoordinatorServer cs2 = new CoordinatorServer(coordinatorConf);
      cs1.start();

      ExitUtils.disableSystemExit();
      String expectMessage = "Fail to start jetty http server";
      final int expectStatus = 1;
      try {
        cs2.start();
      } catch (Exception e) {
        assertEquals(expectMessage, e.getMessage());
        assertEquals(expectStatus, ((ExitException) e).getStatus());
      }

      coordinatorConf.setInteger("rss.jetty.http.port", 9529);
      cs2 = new CoordinatorServer(coordinatorConf);
      expectMessage = "Fail to start grpc server";
      try {
        cs2.start();
      } catch (Exception e) {
        assertEquals(expectMessage, e.getMessage());
        assertEquals(expectStatus, ((ExitException) e).getStatus());
      }

      final Thread t = new Thread(null, () -> {
        throw new AssertionError("TestUncaughtException");
      }, "testThread");
      t.start();
      t.join();



    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

}

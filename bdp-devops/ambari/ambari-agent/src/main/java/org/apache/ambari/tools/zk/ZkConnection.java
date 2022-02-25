/**
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

package org.apache.ambari.tools.zk;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * I can open connections to ZooKeeper
 */
public class ZkConnection {

  /**
   * Opens a connection to zookeeper and waits until the connection established
   */
  public static ZooKeeper open(String serverAddress, int sessionTimeoutMillis, int connectionTimeoutMillis)
    throws IOException, InterruptedException, IllegalStateException
  {
    final CountDownLatch connSignal = new CountDownLatch(1);
    ZooKeeper zooKeeper = new ZooKeeper(serverAddress, sessionTimeoutMillis, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        if (event.getState() == SyncConnected) {
          connSignal.countDown();
        }
      }
    });
    connSignal.await(connectionTimeoutMillis, MILLISECONDS);
    return zooKeeper;
  }
}

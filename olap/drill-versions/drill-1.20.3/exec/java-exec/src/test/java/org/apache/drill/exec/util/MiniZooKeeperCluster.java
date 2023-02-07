/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.fs.FileUtil;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mostly Copied from HBase's MiniZooKeeperCluster, but without the Hadoop dependency.
 */
public class MiniZooKeeperCluster {

  private static final Logger logger = LoggerFactory.getLogger(MiniZooKeeperCluster.class);

  private static final int TICK_TIME = 2000;
  private static final int CONNECTION_TIMEOUT = 10000;
  public static final int DEFAULT_PORT = 2181;

  private boolean started;

  /**
   * The default port. If zero, we use a random port.
   */
  private int defaultClientPort = 0;

  private int clientPort;

  private List<NIOServerCnxnFactory> standaloneServerFactoryList;
  private List<ZooKeeperServer> zooKeeperServers;
  private List<Integer> clientPortList;

  private int activeZKServerIndex;
  private int tickTime = 0;

  public MiniZooKeeperCluster() {
    this.started = false;
//    this.configuration = configuration;
    activeZKServerIndex = -1;
    zooKeeperServers = new ArrayList<>();
    clientPortList = new ArrayList<>();
    standaloneServerFactoryList = new ArrayList<>();
  }

  public void setDefaultClientPort(int clientPort) {
    if (clientPort <= 0) {
      throw new IllegalArgumentException("Invalid default ZK client port: "
        + clientPort);
    }
    this.defaultClientPort = clientPort;
  }

  /**
   * Selects a ZK client port. Returns the default port if specified.
   * Otherwise, returns a random port. The random port is selected from the
   * range between 49152 to 65535. These ports cannot be registered with IANA
   * and are intended for dynamic allocation (see http://bit.ly/dynports).
   */
  private int selectClientPort() {
    if (defaultClientPort > 0) {
      return defaultClientPort;
    }
    return 0xc000 + new Random().nextInt(0x3f00);
  }

  public void setTickTime(int tickTime) {
    this.tickTime = tickTime;
  }

  public int getBackupZooKeeperServerNum() {
    return zooKeeperServers.size() - 1;
  }

  public int getZooKeeperServerNum() {
    return zooKeeperServers.size();
  }

  // / XXX: From o.a.zk.t.ClientBase
  private static void setupTestEnv() {
    // during the tests we run with 100K prealloc in the logs.
    // on windows systems prealloc of 64M was seen to take ~15seconds
    // resulting in test failure (client timeout on first session).
    // set env and directly in order to handle static init/gc issues
    System.setProperty("zookeeper.preAllocSize", "100");
    FileTxnLog.setPreallocSize(100 * 1024);
  }

  public int startup(File baseDir) throws IOException, InterruptedException {
    return startup(baseDir, 1);
  }

  /**
   * @param baseDir
   * @param numZooKeeperServers
   * @return ClientPort server bound to.
   * @throws IOException
   * @throws InterruptedException
   */
  public int startup(File baseDir, int numZooKeeperServers) throws IOException,
    InterruptedException {
    if (numZooKeeperServers <= 0) {
      return -1;
    }

    setupTestEnv();
    shutdown();

    int tentativePort = selectClientPort();

    // running all the ZK servers
    for (int i = 0; i < numZooKeeperServers; i++) {
      File dir = new File(baseDir, "zookeeper_" + i).getAbsoluteFile();
      recreateDir(dir);
      int tickTimeToUse;
      if (this.tickTime > 0) {
        tickTimeToUse = this.tickTime;
      } else {
        tickTimeToUse = TICK_TIME;
      }

      ZooKeeperServer server = new ZooKeeperServer(dir, dir, tickTimeToUse);
      NIOServerCnxnFactory standaloneServerFactory;

      while (true) {
        try {
          standaloneServerFactory = new NIOServerCnxnFactory();
          standaloneServerFactory.configure(new InetSocketAddress(tentativePort), 1000);
        } catch (BindException e) {
          logger.debug("Failed binding ZK Server to client port: {}", tentativePort);
          // This port is already in use, try to use another.
          tentativePort++;
          continue;
        }

        // Start up this ZK server

        try {
          standaloneServerFactory.startup(server);
        } catch (IOException e) {
          logger.error("Zookeeper startup error", e);
          tentativePort++;
          continue;
        }

        if (!waitForServerUp(server, CONNECTION_TIMEOUT)) {
          server.shutdown();
          server = new ZooKeeperServer(dir, dir, tickTimeToUse);
          tentativePort++;
          continue;
        }

        break;
      }

      // We have selected this port as a client port.
      clientPortList.add(tentativePort);
      standaloneServerFactoryList.add(standaloneServerFactory);
      zooKeeperServers.add(server);
    }

    // set the first one to be active ZK; Others are backups
    activeZKServerIndex = 0;
    started = true;
    clientPort = clientPortList.get(activeZKServerIndex);
    logger.info("Started MiniZK Cluster and connect 1 ZK server " +
      "on client port: {}", clientPort);
    return clientPort;
  }

  private void recreateDir(File dir) throws IOException {
    if (dir.exists()) {
      FileUtil.fullyDelete(dir);
    }
    try {
      dir.mkdirs();
    } catch (SecurityException e) {
      throw new IOException("creating dir: " + dir, e);
    }
  }

  /**
   * @throws IOException
   */
  public void shutdown() throws IOException {
    if (!started) {
      return;
    }
    // shut down all the zk servers
    for (int i = 0; i < standaloneServerFactoryList.size(); i++) {
      NIOServerCnxnFactory standaloneServerFactory =
        standaloneServerFactoryList.get(i);
      int clientPort = clientPortList.get(i);

      standaloneServerFactory.shutdown();
      if (!waitForServerDown(clientPort, CONNECTION_TIMEOUT)) {
        throw new IOException("Waiting for shutdown of standalone server");
      }
    }

    // clear everything
    started = false;
    activeZKServerIndex = 0;
    standaloneServerFactoryList.clear();
    clientPortList.clear();
    zooKeeperServers.clear();

    logger.info("Shutdown MiniZK cluster with all ZK servers");
  }

  /**
   * @return clientPort return clientPort if there is another ZK backup can run
   *         when killing the current active; return -1, if there is no backups.
   * @throws IOException
   * @throws InterruptedException
   */
  public int killCurrentActiveZooKeeperServer() throws IOException,
    InterruptedException {
    if (!started || activeZKServerIndex < 0) {
      return -1;
    }

    // Shutdown the current active one
    NIOServerCnxnFactory standaloneServerFactory =
      standaloneServerFactoryList.get(activeZKServerIndex);
    int clientPort = clientPortList.get(activeZKServerIndex);

    standaloneServerFactory.shutdown();
    if (!waitForServerDown(clientPort, CONNECTION_TIMEOUT)) {
      throw new IOException("Waiting for shutdown of standalone server");
    }

    // remove the current active zk server
    standaloneServerFactoryList.remove(activeZKServerIndex);
    clientPortList.remove(activeZKServerIndex);
    zooKeeperServers.remove(activeZKServerIndex);
    logger.info("Kill the current active ZK servers in the cluster " +
      "on client port: {}", clientPort);

    if (standaloneServerFactoryList.size() == 0) {
      // there is no backup servers;
      return -1;
    }
    clientPort = clientPortList.get(activeZKServerIndex);
    logger.info("Activate a backup zk server in the cluster " +
      "on client port: {}", clientPort);
    // return the next back zk server's port
    return clientPort;
  }

  /**
   * Kill one back up ZK servers
   *
   * @throws IOException
   * @throws InterruptedException
   */
  public void killOneBackupZooKeeperServer() throws IOException,
    InterruptedException {
    if (!started || activeZKServerIndex < 0 ||
      standaloneServerFactoryList.size() <= 1) {
      return;
    }

    int backupZKServerIndex = activeZKServerIndex + 1;
    // Shutdown the current active one
    NIOServerCnxnFactory standaloneServerFactory =
      standaloneServerFactoryList.get(backupZKServerIndex);
    int clientPort = clientPortList.get(backupZKServerIndex);

    standaloneServerFactory.shutdown();
    if (!waitForServerDown(clientPort, CONNECTION_TIMEOUT)) {
      throw new IOException("Waiting for shutdown of standalone server");
    }

    // remove this backup zk server
    standaloneServerFactoryList.remove(backupZKServerIndex);
    clientPortList.remove(backupZKServerIndex);
    zooKeeperServers.remove(backupZKServerIndex);
    logger.info("Kill one backup ZK servers in the cluster " +
      "on client port: {}", clientPort);
  }

  // XXX: From o.a.zk.t.ClientBase
  private static boolean waitForServerDown(int port, long timeout) {
    long start = System.currentTimeMillis();
    while (true) {
      try {
        Socket sock = new Socket("localhost", port);
        try {
          OutputStream outstream = sock.getOutputStream();
          outstream.write("stat".getBytes());
          outstream.flush();
        } finally {
          sock.close();
        }
      } catch (IOException e) {
        return true;
      }

      if (System.currentTimeMillis() > start + timeout) {
        break;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    return false;
  }

  // XXX: From o.a.zk.t.ClientBase
  private static boolean waitForServerUp(ZooKeeperServer server, long timeout) {
    long start = System.currentTimeMillis();
    while (true) {
      // Doing it this way instead of openning a connection to zookeeper because of ZOOKEEPER-2383

      if (server.isRunning()) {
        return true;
      }

      if (System.currentTimeMillis() > start + timeout) {
        break;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    return false;
  }

  public int getClientPort() {
    return clientPort;
  }

}

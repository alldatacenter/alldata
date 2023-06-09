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
package org.apache.drill.yarn.zk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.work.foreman.DrillbitStatusListener;
import org.apache.drill.yarn.appMaster.AMRegistrar;

/**
 * Driver class for the ZooKeeper cluster coordinator. Provides defaults for
 * most options, but allows customizing each. Provides a {@link #build()} method
 * to create <i>and start</i> the ZK service. Obtains the initial set of
 * Drillbits (which should be empty for a YARN-defined cluster) which can be
 * retrieved after building.
 * <p>
 * Maintains the ZK connection and monitors for disconnect. This class simply
 * detects a disconnect timeout, it does not send a disconnect event itself to
 * avoid creating a timer thread just for this purpose. Instead, the caller can
 * poll {@link #hasFailed()}.
 * <p>
 * Defaults match those in Drill. (Actual Drill defaults are not yet used due to
 * code incompatibility issues.)
 */

public class ZKClusterCoordinatorDriver implements AMRegistrar {
  private static final Pattern ZK_COMPLEX_STRING = Pattern
      .compile("(^.*?)/(.*)/([^/]*)$");

  // Defaults are taken from java-exec's drill-module.conf

  private String connect = "localhost:2181";
  private String clusterId = "drillbits1";
  private String zkRoot = "drill";
  private int retryCount = 7200;
  private int connectTimeoutMs = 5_000;
  private int retryDelayMs = 500;

  // Default timeout before we declare that ZK is down: 2 minutes.

  private int failureTimeoutMs = 120_000;

  // Maximum ZK startup wait defaults to 30 seconds. It is only 10 seconds
  // in the Drill implementation.

  private int maxStartWaitMs = 30_000;

  // Expected ports used to match ZK registries with
  // containers. ZK lists the ports as part of its key, we have to anticipate
  // these values in order to match.

  private int userPort = 31010;
  private int controlPort = 31011;
  private int dataPort = 31012;

  private List<DrillbitEndpoint> initialEndpoints;
  private ConnectionStateListener stateListener = new ConnectionStateListener() {

    @Override
    public void stateChanged(CuratorFramework client,
        ConnectionState newState) {
      ZKClusterCoordinatorDriver.this.stateChanged(newState);
    }
  };

  private ZKClusterCoordinator zkCoord;

  private long connectionLostTime;

  private AMRegistry amRegistry;

  public ZKClusterCoordinatorDriver() {
  }

  /**
   * Specify connect string in the form: host:/zkRoot/clusterId
   *
   * @param connect
   * @return This {@link ZKClusterCoordinatorDriver}.
   * @throws ZKConfigException
   */
  public ZKClusterCoordinatorDriver setConnect(String connect)
      throws ZKConfigException {

    // check if this is a complex zk string. If so, parse into components.
    Matcher m = ZK_COMPLEX_STRING.matcher(connect);
    if (!m.matches()) {
      throw new ZKConfigException("Bad connect string: " + connect);
    }
    this.connect = m.group(1);
    zkRoot = m.group(2);
    clusterId = m.group(3);
    return this;
  }

  public ZKClusterCoordinatorDriver setConnect(String connect, String zkRoot,
      String clusterId) {
    this.connect = connect;
    this.zkRoot = zkRoot;
    this.clusterId = clusterId;
    return this;
  }

  public ZKClusterCoordinatorDriver setRetryCount(int n) {
    retryCount = n;
    return this;
  }

  public ZKClusterCoordinatorDriver setConnectTimeoutMs(int ms) {
    connectTimeoutMs = ms;
    return this;
  }

  public ZKClusterCoordinatorDriver setRetryDelayMs(int ms) {
    retryDelayMs = ms;
    return this;
  }

  public ZKClusterCoordinatorDriver setMaxStartWaitMs(int ms) {
    maxStartWaitMs = ms;
    return this;
  }

  public ZKClusterCoordinatorDriver setFailureTimoutMs(int ms) {
    failureTimeoutMs = ms;
    return this;
  }

  public ZKClusterCoordinatorDriver setPorts(int userPort, int controlPort,
      int dataPort) {
    this.userPort = userPort;
    this.controlPort = controlPort;
    this.dataPort = dataPort;
    return this;
  }

  /**
   * Builds and starts the ZooKeeper cluster coordinator, translating any errors
   * that occur. After this call, the listener will start receiving messages.
   *
   * @return This {@link ZKClusterCoordinatorDriver}.
   * @throws ZKRuntimeException
   *           if ZK startup fails
   */
  public ZKClusterCoordinatorDriver build() throws ZKRuntimeException {
    try {
      zkCoord = new ZKClusterCoordinator(connect, zkRoot, clusterId, retryCount,
          retryDelayMs, connectTimeoutMs);
    } catch (IOException e) {
      throw new ZKRuntimeException(
          "Failed to initialize the ZooKeeper cluster coordination", e);
    }
    try {
      zkCoord.start(maxStartWaitMs);
    } catch (Exception e) {
      throw new ZKRuntimeException(
          "Failed to start the ZooKeeper cluster coordination after "
              + maxStartWaitMs + " ms.",
          e);
    }
    initialEndpoints = new ArrayList<>(zkCoord.getAvailableEndpoints());
    zkCoord.getCurator().getConnectionStateListenable()
        .addListener(stateListener);
    amRegistry = new AMRegistry(zkCoord);
    amRegistry.useLocalRegistry(zkRoot, clusterId);
    return this;
  }

  public void addDrillbitListener(DrillbitStatusListener listener) {
    zkCoord.addDrillbitStatusListener(listener);
  }

  public void removeDrillbitListener(DrillbitStatusListener listener) {
    zkCoord.removeDrillbitStatusListener(listener);
  }

  /**
   * Returns the set of Drillbits registered at the time of the {@link #build()}
   * call. Should be empty for a cluster managed by YARN.
   *
   * @return The set of Drillbits registered at the time of the {@link #build()}
   * call.
   */

  public List<DrillbitEndpoint> getInitialEndpoints() {
    return initialEndpoints;
  }

  /**
   * Convenience method to convert a Drillbit to a string. Note that ZK does not
   * advertise the HTTP port, so it does not appear in the generated string.
   *
   * @param bit
   * @return A string representation of a Drillbit.
   */

  public static String asString(DrillbitEndpoint bit) {
    return formatKey(bit.getAddress(), bit.getUserPort(), bit.getControlPort(),
        bit.getDataPort());
  }

  public String toKey(String host) {
    return formatKey(host, userPort, controlPort, dataPort);
  }

  public static String formatKey(String host, int userPort, int controlPort,
      int dataPort) {
    StringBuilder buf = new StringBuilder();
    buf.append(host).append(":").append(userPort).append(':')
        .append(controlPort).append(':').append(dataPort);
    return buf.toString();
  }

  /**
   * Translate ZK connection events into a connected/disconnected state along
   * with the time of the first disconnect not followed by a connect.
   *
   * @param newState
   */

  protected void stateChanged(ConnectionState newState) {
    switch (newState) {
    case CONNECTED:
    case READ_ONLY:
    case RECONNECTED:
      if (connectionLostTime != 0) {
        ZKClusterCoordinator.logger.info("ZK connection regained");
      }
      connectionLostTime = 0;
      break;
    case LOST:
    case SUSPENDED:
      if (connectionLostTime == 0) {
        ZKClusterCoordinator.logger.info("ZK connection lost");
        connectionLostTime = System.currentTimeMillis();
      }
      break;
    }
  }

  /**
   * Reports our best guess as to whether ZK has failed. We assume ZK has failed
   * if we received a connection lost notification without a subsequent connect
   * notification, and we received the disconnect notification log enough ago
   * that we assume that a timeout has occurred.
   *
   * @return True if we think zookeeper has failed. False otherwise.
   */

  public boolean hasFailed() {
    if (connectionLostTime == 0) {
      return false;
    }
    return System.currentTimeMillis() - connectionLostTime > failureTimeoutMs;
  }

  public long getLostConnectionDurationMs() {
    if (connectionLostTime == 0) {
      return 0;
    }
    return System.currentTimeMillis() - connectionLostTime;
  }

  public void close() {
    if (zkCoord == null) {
      return;
    }
    zkCoord.getCurator().getConnectionStateListenable()
        .removeListener(stateListener);
    try {
      zkCoord.close();
    } catch (Exception e) {
      ZKClusterCoordinator.logger.error("Error occurred on ZK close, ignored",
          e);
    }
    zkCoord = null;
  }

  @Override
  public void register(String amHost, int amPort, String appId)
      throws AMRegistrationException {
    try {
      amRegistry.register(amHost, amPort, appId);
    } catch (ZKRuntimeException e) {
      throw new AMRegistrationException(e);
    }
  }

  @Override
  public void deregister() {
    // Nothing to do: ZK does it for us.
  }
}

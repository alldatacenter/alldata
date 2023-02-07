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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.drill.exec.coord.DrillServiceInstanceHelper;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.work.foreman.DrillbitStatusListener;
import org.apache.drill.test.BaseTest;
import org.apache.drill.yarn.appMaster.EventContext;
import org.apache.drill.yarn.appMaster.RegistryHandler;
import org.apache.drill.yarn.appMaster.Task;
import org.apache.drill.yarn.appMaster.TaskLifecycleListener.Event;
import org.apache.drill.yarn.zk.ZKRegistry.DrillbitTracker;
import org.junit.Test;

/**
 * Tests for the AM version of the cluster coordinator. The AM version has no
 * dependencies on the DoY config system or other systems, making it easy to
 * test in isolation using the Curator-provided test server.
 */

public class TestZkRegistry extends BaseTest {
  private static final String BARNEY_HOST = "barney";
  private static final String WILMA_HOST = "wilma";
  private static final String TEST_HOST = "host";
  private static final String FRED_HOST = "fred";
  public static final int TEST_USER_PORT = 123;
  public static final int TEST_CONTROL_PORT = 456;
  public static final int TEST_DATA_PORT = 789;
  public static final String ZK_ROOT = "test-root";
  public static final String CLUSTER_ID = "test-cluster";

  /**
   * Validate that the key format used for endpoint is the same as that
   * generated for hosts coming from YARN.
   */

  @Test
  public void testFormat() {
    DrillbitEndpoint dbe = makeEndpoint(TEST_HOST);
    assertEquals(makeKey(TEST_HOST), ZKClusterCoordinatorDriver.asString(dbe));

    ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
        .setPorts(123, 456, 789);
    assertEquals(makeKey(TEST_HOST), driver.toKey(TEST_HOST));

    // Internal default ports (used mostly for testing.)

    driver = new ZKClusterCoordinatorDriver();
    assertEquals("fred:31010:31011:31012", driver.toKey(FRED_HOST));
  }

  public static String makeKey(String host) {
    return host + ":" + TEST_USER_PORT + ":" + TEST_CONTROL_PORT + ":"
        + TEST_DATA_PORT;
  }

  /**
   * Basic setup: start a ZK and verify that the initial endpoint list is empty.
   * Also validates the basics of the test setup (mock server, etc.)
   *
   * @throws Exception
   */

  @Test
  public void testBasics() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();
      String connStr = server.getConnectString();
      ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
          .setConnect(connStr, "drill", "drillbits").build();
      assertTrue(driver.getInitialEndpoints().isEmpty());
      driver.close();
      server.stop();
    }
  }

  private class TestDrillbitStatusListener implements DrillbitStatusListener {
    protected Set<DrillbitEndpoint> added;
    protected Set<DrillbitEndpoint> removed;

    @Override
    public void drillbitUnregistered(
        Set<DrillbitEndpoint> unregisteredDrillbits) {
      removed = unregisteredDrillbits;
    }

    @Override
    public void drillbitRegistered(Set<DrillbitEndpoint> registeredDrillbits) {
      added = registeredDrillbits;
    }

    public void clear() {
      added = null;
      removed = null;
    }
  }

  /**
   * Test a typical life cycle: existing Drillbit on AM start, add a Drilbit
   * (simulates a drillbit starting), and remove a drillbit (simulates a
   * Drillbit ending).
   *
   * @throws Exception
   */

  @Test
  public void testCycle() throws Exception {
    TestingServer server = new TestingServer();
    server.start();
    String connStr = server.getConnectString();

    CuratorFramework probeZk = connectToZk(connStr);
    addDrillbit(probeZk, FRED_HOST);

    ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
        .setConnect(connStr, ZK_ROOT, CLUSTER_ID).build();
    List<DrillbitEndpoint> bits = driver.getInitialEndpoints();
    assertEquals(1, bits.size());
    assertEquals(makeKey(FRED_HOST),
        ZKClusterCoordinatorDriver.asString(bits.get(0)));

    TestDrillbitStatusListener listener = new TestDrillbitStatusListener();
    driver.addDrillbitListener(listener);

    addDrillbit(probeZk, WILMA_HOST);
    Thread.sleep(50);
    assertNull(listener.removed);
    assertNotNull(listener.added);
    assertEquals(1, listener.added.size());
    for (DrillbitEndpoint dbe : listener.added) {
      assertEquals(makeKey(WILMA_HOST),
          ZKClusterCoordinatorDriver.asString(dbe));
    }

    listener.clear();
    removeDrillbit(probeZk, FRED_HOST);
    Thread.sleep(50);
    assertNull(listener.added);
    assertNotNull(listener.removed);
    assertEquals(1, listener.removed.size());
    for (DrillbitEndpoint dbe : listener.removed) {
      assertEquals(makeKey(FRED_HOST),
          ZKClusterCoordinatorDriver.asString(dbe));
    }

    probeZk.close();
    driver.close();
    server.stop();
    server.close();
  }

  /**
   * Make a Drill endpoint using the hard-coded test ports and the given host
   * name.
   *
   * @param host
   * @return
   */

  private DrillbitEndpoint makeEndpoint(String host) {
    return DrillbitEndpoint.newBuilder().setAddress(host)
        .setControlPort(TEST_CONTROL_PORT).setDataPort(TEST_DATA_PORT)
        .setUserPort(TEST_USER_PORT).build();
  }

  /**
   * Pretend to be a Drillbit creating its ZK entry. Real Drillbits use a GUID
   * as the key, but we just use the host name, which is good enough for our
   * purposes here.
   *
   * @param zk
   * @param host
   * @throws Exception
   */

  private void addDrillbit(CuratorFramework zk, String host) throws Exception {
    DrillbitEndpoint dbe = makeEndpoint(host);
    ServiceInstance<DrillbitEndpoint> si = ServiceInstance
        .<DrillbitEndpoint> builder().name(CLUSTER_ID).payload(dbe).build();
    byte data[] = DrillServiceInstanceHelper.SERIALIZER.serialize(si);
    zk.create().forPath("/" + host, data);
  }

  private void removeDrillbit(CuratorFramework zk, String host)
      throws Exception {
    zk.delete().forPath("/" + host);
  }

  /**
   * Connect to the test ZK for the simulated Drillbit side of the test. (The AM
   * side of the test uses the actual AM code, which is what we're testing
   * here...)
   *
   * @param connectString
   * @return
   */

  public static CuratorFramework connectToZk(String connectString) {
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .namespace(ZK_ROOT + "/" + CLUSTER_ID).connectString(connectString)
        .retryPolicy(new RetryNTimes(3, 1000)).build();
    client.start();
    return client;
  }

  private static class TestRegistryHandler implements RegistryHandler {
    String reserved;
    String released;
    Task start;
    Task end;

    public void clear() {
      reserved = null;
      released = null;
      start = null;
      end = null;
    }

    @Override
    public void reserveHost(String hostName) {
      assertNull(reserved);
      reserved = hostName;
    }

    @Override
    public void releaseHost(String hostName) {
      assertNull(released);
      released = hostName;
    }

    @Override
    public void startAck(Task task, String propertyKey, Object value) {
      start = task;
    }

    @Override
    public void completionAck(Task task, String endpointProperty) {
      end = task;
    }

    @Override
    public void registryDown() {
      // TODO Auto-generated method stub

    }
  }

  public static class TestTask extends Task {
    private String host;

    public TestTask(String host) {
      super(null, null);
      this.host = host;
    }

    @Override
    public String getHostName() {
      return host;
    }

    @Override
    public void resetTrackingState() {
      trackingState = TrackingState.NEW;
    }
  }

  @Test
  public void testZKRegistry() throws Exception {
    TestingServer server = new TestingServer();
    server.start();
    String connStr = server.getConnectString();

    CuratorFramework probeZk = connectToZk(connStr);
    addDrillbit(probeZk, FRED_HOST);

    ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
        .setConnect(connStr, ZK_ROOT, CLUSTER_ID)
        .setPorts(TEST_USER_PORT, TEST_CONTROL_PORT, TEST_DATA_PORT).build();
    ZKRegistry registry = new ZKRegistry(driver);
    TestRegistryHandler handler = new TestRegistryHandler();
    registry.start(handler);

    // We started with one "stray" drillbit that will be reported as unmanaged.

    assertEquals(FRED_HOST, handler.reserved);
    List<String> unmanaged = registry.listUnmanagedDrillits();
    assertEquals(1, unmanaged.size());
    String fredsKey = makeKey(FRED_HOST);
    assertEquals(fredsKey, unmanaged.get(0));
    Map<String, DrillbitTracker> trackers = registry.getRegistryForTesting();
    assertEquals(1, trackers.size());
    assertTrue(trackers.containsKey(fredsKey));
    DrillbitTracker fredsTracker = trackers.get(fredsKey);
    assertEquals(fredsKey, fredsTracker.key);
    assertEquals(DrillbitTracker.State.UNMANAGED, fredsTracker.state);
    assertNull(fredsTracker.task);
    assertEquals(fredsKey,
        ZKClusterCoordinatorDriver.asString(fredsTracker.endpoint));

    // The handler should have been told about the initial stray.

    assertEquals(FRED_HOST, handler.reserved);

    // Pretend to start a new Drillbit.

    Task wilmasTask = new TestTask(WILMA_HOST);
    EventContext context = new EventContext(wilmasTask);

    // Registry ignores the created event.

    registry.stateChange(Event.CREATED, context);
    assertEquals(1, registry.getRegistryForTesting().size());

    // But, does care about the allocated event.

    registry.stateChange(Event.ALLOCATED, context);
    assertEquals(2, registry.getRegistryForTesting().size());
    String wilmasKey = makeKey(WILMA_HOST);
    DrillbitTracker wilmasTracker = registry.getRegistryForTesting()
        .get(wilmasKey);
    assertNotNull(wilmasTracker);
    assertEquals(wilmasTask, wilmasTracker.task);
    assertNull(wilmasTracker.endpoint);
    assertEquals(wilmasKey, wilmasTracker.key);
    assertEquals(DrillbitTracker.State.NEW, wilmasTracker.state);
    handler.clear();

    // Time goes on. The Drillbit starts and registers itself.

    addDrillbit(probeZk, WILMA_HOST);
    Thread.sleep(100);
    assertEquals(wilmasTask, handler.start);
    assertEquals(DrillbitTracker.State.REGISTERED, wilmasTracker.state);
    assertEquals(handler.start, wilmasTask);

    // Create another task: Barney

    Task barneysTask = new TestTask(BARNEY_HOST);
    context = new EventContext(barneysTask);
    registry.stateChange(Event.CREATED, context);

    // Start Barney, but assume a latency in Yarn, but not ZK.
    // We get the ZK registration before the YARN launch confirmation.

    handler.clear();
    addDrillbit(probeZk, BARNEY_HOST);
    Thread.sleep(100);
    assertEquals(BARNEY_HOST, handler.reserved);
    String barneysKey = makeKey(BARNEY_HOST);
    DrillbitTracker barneysTracker = registry.getRegistryForTesting()
        .get(barneysKey);
    assertNotNull(barneysTracker);
    assertEquals(DrillbitTracker.State.UNMANAGED, barneysTracker.state);
    assertNull(barneysTracker.task);
    assertEquals(2, registry.listUnmanagedDrillits().size());

    handler.clear();
    registry.stateChange(Event.ALLOCATED, context);
    assertEquals(DrillbitTracker.State.REGISTERED, barneysTracker.state);
    assertEquals(handler.start, barneysTask);
    assertEquals(barneysTask, barneysTracker.task);
    assertEquals(1, registry.listUnmanagedDrillits().size());

    // Barney is having problems, it it drops out of ZK.

    handler.clear();
    removeDrillbit(probeZk, BARNEY_HOST);
    Thread.sleep(100);
    assertEquals(barneysTask, handler.end);
    assertEquals(DrillbitTracker.State.DEREGISTERED, barneysTracker.state);

    // Barney comes alive (presumably before the controller gives up and kills
    // the Drillbit.)

    handler.clear();
    addDrillbit(probeZk, BARNEY_HOST);
    Thread.sleep(100);
    assertEquals(barneysTask, handler.start);
    assertEquals(DrillbitTracker.State.REGISTERED, barneysTracker.state);

    // Barney is killed by the controller.
    // ZK entry drops. Tracker is removed, controller is notified.

    handler.clear();
    removeDrillbit(probeZk, BARNEY_HOST);
    Thread.sleep(100);
    assertNotNull(registry.getRegistryForTesting().get(barneysKey));
    assertEquals(barneysTask, handler.end);

    // The controller tells the registry to stop tracking the Drillbit.

    handler.clear();
    registry.stateChange(Event.ENDED, context);
    assertNull(handler.end);
    assertNull(registry.getRegistryForTesting().get(barneysKey));

    // The stray drillbit deregisters from ZK. The tracker is removed.

    handler.clear();
    removeDrillbit(probeZk, FRED_HOST);
    Thread.sleep(100);
    assertNull(registry.getRegistryForTesting().get(fredsKey));
    assertNull(handler.end);
    assertEquals(FRED_HOST, handler.released);

    // Wilma is killed by the controller.

    handler.clear();
    removeDrillbit(probeZk, WILMA_HOST);
    Thread.sleep(100);
    assertEquals(wilmasTask, handler.end);
    assertNull(handler.released);
    assertEquals(DrillbitTracker.State.DEREGISTERED, wilmasTracker.state);
    assertNotNull(registry.getRegistryForTesting().get(wilmasKey));

    handler.clear();
    context = new EventContext(wilmasTask);
    registry.stateChange(Event.ENDED, context);
    assertNull(registry.getRegistryForTesting().get(wilmasKey));
    assertNull(handler.released);
    assertNull(handler.end);

    // All drillbits should be gone.

    assertTrue(registry.getRegistryForTesting().isEmpty());

    probeZk.close();
    driver.close();
    server.stop();
    server.close();
  }

}

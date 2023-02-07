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
package org.apache.drill.exec.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ZookeeperHelper;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.server.Drillbit;

import org.apache.drill.exec.server.RemoteServiceSet;

import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class ConnectTriesPropertyTestClusterBits extends BaseTest {

  public static StringBuilder bitInfo;
  public static final String fakeBitsInfo = "127.0.0.1:5000,127.0.0.1:5001";
  public static List<Drillbit> drillbits;
  public static final int drillBitCount = 1;
  public static ZookeeperHelper zkHelper;
  public static RemoteServiceSet remoteServiceSet;
  public static DrillConfig drillConfig;

  @BeforeClass
  public static void testSetUp() throws Exception {
    remoteServiceSet = RemoteServiceSet.getLocalServiceSet();
    zkHelper = new ZookeeperHelper();
    zkHelper.startZookeeper(1);

    // Creating Drillbits
    drillConfig = zkHelper.getConfig();
    try {
      int drillBitStarted = 0;
      drillbits = new ArrayList<>();
      while(drillBitStarted < drillBitCount){
        drillbits.add(Drillbit.start(drillConfig, remoteServiceSet));
        ++drillBitStarted;
      }
    } catch (DrillbitStartupException e) {
      throw new RuntimeException("Failed to start drillbits.", e);
    }
    bitInfo = new StringBuilder();

    for (int i = 0; i < drillBitCount; ++i) {
      final DrillbitEndpoint currentEndPoint = drillbits.get(i).getContext().getEndpoint();
      final String currentBitIp = currentEndPoint.getAddress();
      final int currentBitPort = currentEndPoint.getUserPort();
      bitInfo.append(",");
      bitInfo.append(currentBitIp);
      bitInfo.append(":");
      bitInfo.append(currentBitPort);
    }
  }

  @AfterClass
  public static void testCleanUp() throws Exception {
    AutoCloseables.close(drillbits);
    zkHelper.stopZookeeper();
  }

  @Test
  public void testSuccessUsingDirectConnectionAndFakeDrillbitPresent() throws Exception {
    final StringBuilder endpoints = new StringBuilder(fakeBitsInfo);
    endpoints.append(bitInfo);

    Properties props = new Properties();
    props.setProperty("drillbit", endpoints.toString());
    props.setProperty("connect_limit", "3");

    // Test with direct connection
    DrillClient client = new DrillClient(true);
    client.connect(props);
    client.close();
  }

  @Test
  public void testSuccessDirectConnectionDefaultConnectTriesAndFakeDrillbits() throws Exception {
    final StringBuilder endpoints = new StringBuilder(fakeBitsInfo);
    endpoints.append(bitInfo);

    Properties props = new Properties();
    props.setProperty("drillbit", endpoints.toString());

    // Test with direct connection
    DrillClient client = new DrillClient(true);
    client.connect(props);
    client.close();
  }

  @Test
  public void testFailureUsingDirectConnectionAllFakeBits() throws Exception {
    final StringBuilder endpoints = new StringBuilder(fakeBitsInfo);

    Properties props = new Properties();
    props.setProperty("drillbit", endpoints.toString());
    props.setProperty("tries", "2");

    // Test with direct connection
    DrillClient client = new DrillClient(true);

    try{
      client.connect(props);
      fail();
    }catch(RpcException ex){
      assertTrue(ex.getCause() instanceof ExecutionException);
      client.close();
    }
  }

  @Test
  public void testSuccessUsingZKWithNoFakeBits() throws Exception {
    Properties props = new Properties();
    props.setProperty("tries", "2");

    // Test with Cluster Coordinator connection
    DrillClient client = new DrillClient(drillConfig, remoteServiceSet.getCoordinator());
    client.connect(props);
    client.close();
  }

  @Test
  public void testSuccessUsingZKWithFakeBits() throws Exception {
    Properties props = new Properties();
    props.setProperty("tries", "3");

    // Test with Cluster Coordinator connection
    DrillClient client = new DrillClient(drillConfig, remoteServiceSet.getCoordinator());
    // Create couple of fake drillbit endpoints and register with cluster coordinator
    DrillbitEndpoint fakeEndPoint1 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5000).build();
    DrillbitEndpoint fakeEndPoint2 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5001).build();

    ClusterCoordinator.RegistrationHandle fakeEndPoint1Handle = remoteServiceSet.getCoordinator()
                                                                                .register(fakeEndPoint1);
    ClusterCoordinator.RegistrationHandle fakeEndPoint2Handle = remoteServiceSet.getCoordinator()
                                                                                .register(fakeEndPoint2);

    client.connect(props);
    client.close();

    // Remove the fake drillbits so that other tests are not affected
    remoteServiceSet.getCoordinator().unregister(fakeEndPoint1Handle);
    remoteServiceSet.getCoordinator().unregister(fakeEndPoint2Handle);
  }

  @Test
  public void testSuccessUsingZKWithDefaultConnectTriesFakeBits() throws Exception {
    // Test with Cluster Coordinator connection
    DrillClient client = new DrillClient(drillConfig, remoteServiceSet.getCoordinator());

    // Create couple of fake drillbit endpoints and register with cluster coordinator
    DrillbitEndpoint fakeEndPoint1 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5000).build();
    DrillbitEndpoint fakeEndPoint2 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5001).build();

    ClusterCoordinator.RegistrationHandle fakeEndPoint1Handle = remoteServiceSet.getCoordinator()
                                                                                .register(fakeEndPoint1);
    ClusterCoordinator.RegistrationHandle fakeEndPoint2Handle = remoteServiceSet.getCoordinator()
                                                                                .register(fakeEndPoint2);

    client.connect(new Properties());
    client.close();

    // Remove the fake drillbits so that other tests are not affected
    remoteServiceSet.getCoordinator().unregister(fakeEndPoint1Handle);
    remoteServiceSet.getCoordinator().unregister(fakeEndPoint2Handle);
  }

  @Test
  public void testInvalidConnectTriesValue() throws Exception {
    Properties props = new Properties();
    props.setProperty("tries", "abc");

    // Test with Cluster Cordinator connection
    DrillClient client = new DrillClient(drillConfig, remoteServiceSet.getCoordinator());

    try {
      client.connect(props);
      fail();
    } catch (RpcException ex) {
      assertTrue(ex instanceof InvalidConnectionInfoException);
      client.close();
    }
  }

  @Test
  public void testConnectFailureUsingZKWithOnlyFakeBits() throws Exception {
    Properties props = new Properties();
    props.setProperty("tries", "3");

    // Test with Cluster Coordinator connection
    RemoteServiceSet localServiceSet = RemoteServiceSet.getLocalServiceSet();
    DrillClient client = new DrillClient(drillConfig, localServiceSet.getCoordinator());

    // Create couple of fake drillbit endpoints and register with cluster coordinator
    DrillbitEndpoint fakeEndPoint1 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5000).build();
    DrillbitEndpoint fakeEndPoint2 = DrillbitEndpoint.newBuilder().setAddress("127.0.0.1").setUserPort(5001).build();

    ClusterCoordinator.RegistrationHandle fakeEndPoint1Handle = localServiceSet.getCoordinator()
                                                                               .register(fakeEndPoint1);
    ClusterCoordinator.RegistrationHandle fakeEndPoint2Handle = localServiceSet.getCoordinator()
                                                                               .register(fakeEndPoint2);

    try {
      client.connect(props);
      fail();
    } catch (RpcException ex) {
      assertTrue(ex.getCause() instanceof ExecutionException);
      client.close();
    } finally {
      // Remove the fake drillbits from local cluster cordinator
      localServiceSet.getCoordinator().unregister(fakeEndPoint1Handle);
      localServiceSet.getCoordinator().unregister(fakeEndPoint2Handle);
    }
  }
}

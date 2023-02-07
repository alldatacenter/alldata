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
package org.apache.drill.exec.rpc.control;

import io.netty.buffer.ByteBuf;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos;
import org.apache.drill.exec.proto.GeneralRPCProtos;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.work.batch.ControlMessageHandler;
import org.apache.drill.test.BaseTest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestLocalControlConnectionManager extends BaseTest {

  private static final DrillbitEndpoint localEndpoint = DrillbitEndpoint.newBuilder()
    .setAddress("10.0.0.1")
    .setControlPort(31011)
    .setState(DrillbitEndpoint.State.STARTUP)
    .build();

  private static ControlConnectionConfig mockConfig;

  private static ControlMessageHandler mockHandler;

  private static ControlTunnel controlTunnel;

  private static CountDownLatch latch;

  private static final String NEGATIVE_ACK_MESSAGE = "Negative Ack received";

  private static final RpcOutcomeListener<GeneralRPCProtos.Ack> outcomeListener =
    new RpcOutcomeListener<GeneralRPCProtos.Ack>() {
    @Override
    public void failed(RpcException ex) {
      throw new IllegalStateException(ex);
    }

    @Override
    public void success(GeneralRPCProtos.Ack value, ByteBuf buffer) {
      if (value.getOk()) {
        latch.countDown();
      } else {
        throw new IllegalStateException(NEGATIVE_ACK_MESSAGE);
      }
    }

    @Override
    public void interrupted(InterruptedException e) {
      // Do nothing
    }
  };

  @Rule
  public ExpectedException exceptionThrown = ExpectedException.none();

  @BeforeClass
  public static void setup() {
    mockConfig = mock(ControlConnectionConfig.class);
    final ConnectionManagerRegistry registry = new ConnectionManagerRegistry(mockConfig);
    registry.setLocalEndpoint(localEndpoint);
    ControlConnectionManager manager = registry.getConnectionManager(localEndpoint);
    assertTrue(manager instanceof LocalControlConnectionManager);
    controlTunnel = new ControlTunnel(manager);
  }

  @Before
  public void setupForTest() {
    mockHandler = mock(ControlMessageHandler.class);
    when(mockConfig.getMessageHandler()).thenReturn(mockHandler);
  }

  /**
   * Verify that SendFragmentStatus is handled correctly using ControlTunnel with LocalControlConnectionManager
   */
  @Test
  public void testLocalSendFragmentStatus_Success() throws Exception {
    final UserBitShared.QueryId mockQueryId = UserBitShared.QueryId.getDefaultInstance();
    final UserBitShared.QueryProfile mockProfile = UserBitShared.QueryProfile.getDefaultInstance();
    when(mockHandler.requestQueryStatus(mockQueryId)).thenReturn(mockProfile);
    final UserBitShared.QueryProfile returnedProfile = controlTunnel.requestQueryProfile(mockQueryId).checkedGet();
    assertEquals(returnedProfile, mockProfile);
  }

  /**
   * Verify that SendFragmentStatus failure scenario is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalSendFragmentStatus_Failure() throws Exception {
    final UserBitShared.QueryId mockQueryId = UserBitShared.QueryId.getDefaultInstance();
    final String exceptionMessage = "Testing failure case";
    exceptionThrown.expect(RpcException.class);
    exceptionThrown.expectMessage(exceptionMessage);
    when(mockHandler.requestQueryStatus(mockQueryId)).thenThrow(new RpcException(exceptionMessage));
    controlTunnel.requestQueryProfile(mockQueryId).checkedGet();
  }

  /**
   * Verify that CancelFragment with positive ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalCancelFragment_PositiveAck() throws Exception {
    final ExecProtos.FragmentHandle mockHandle = ExecProtos.FragmentHandle.getDefaultInstance();
    latch = new CountDownLatch(1);
    final GeneralRPCProtos.Ack mockResponse = Acks.OK;
    when(mockHandler.cancelFragment(mockHandle)).thenReturn(mockResponse);
    controlTunnel.cancelFragment(outcomeListener, mockHandle);
    latch.await();
  }

  /**
   * Verify that CancelFragment with negative ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalCancelFragment_NegativeAck() throws Exception {
    final ExecProtos.FragmentHandle mockHandle = ExecProtos.FragmentHandle.getDefaultInstance();
    latch = new CountDownLatch(1);
    exceptionThrown.expect(IllegalStateException.class);
    exceptionThrown.expectMessage(NEGATIVE_ACK_MESSAGE);
    final GeneralRPCProtos.Ack mockResponse = Acks.FAIL;
    when(mockHandler.cancelFragment(mockHandle)).thenReturn(mockResponse);
    controlTunnel.cancelFragment(outcomeListener, mockHandle);
    latch.await();
  }

  /**
   * Verify that InitializeFragments with positive ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalSendFragments_PositiveAck() throws Exception {
    final BitControl.InitializeFragments mockFragments = BitControl.InitializeFragments.getDefaultInstance();
    latch = new CountDownLatch(1);
    final GeneralRPCProtos.Ack mockResponse = Acks.OK;
    when(mockHandler.initializeFragment(mockFragments)).thenReturn(mockResponse);
    controlTunnel.sendFragments(outcomeListener, mockFragments);
    latch.await();
  }

  /**
   * Verify that InitializeFragments with negative ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalSendFragments_NegativeAck() throws Exception {
    final BitControl.InitializeFragments mockFragments = BitControl.InitializeFragments.getDefaultInstance();
    latch = new CountDownLatch(1);
    exceptionThrown.expect(IllegalStateException.class);
    exceptionThrown.expectMessage(NEGATIVE_ACK_MESSAGE);
    final GeneralRPCProtos.Ack mockResponse = Acks.FAIL;
    when(mockHandler.initializeFragment(mockFragments)).thenReturn(mockResponse);
    controlTunnel.sendFragments(outcomeListener, mockFragments);
    latch.await();
  }

  /**
   * Verify that InitializeFragments failure case is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testLocalSendFragments_Failure() throws Exception {
    final BitControl.InitializeFragments mockFragments = BitControl.InitializeFragments.getDefaultInstance();
    latch = new CountDownLatch(1);
    exceptionThrown.expect(IllegalStateException.class);
    exceptionThrown.expectCause(new TypeSafeMatcher<Throwable>(RpcException.class) {
      @Override
      protected boolean matchesSafely(Throwable throwable) {
        return (throwable != null && throwable instanceof RpcException);
      }

      @Override
      public void describeTo(Description description) {
        // Do nothing
      }
    });
    when(mockHandler.initializeFragment(mockFragments)).thenThrow(new RpcException("Failed to initialize"));
    controlTunnel.sendFragments(outcomeListener, mockFragments);
    latch.await();
  }

  /**
   * Verify that UnpauseFragment is handled correctly using ControlTunnel with LocalControlConnectionManager
   */
  @Test
  public void testUnpauseFragments() throws Exception {
    final ExecProtos.FragmentHandle mockHandle = ExecProtos.FragmentHandle.getDefaultInstance();
    latch = new CountDownLatch(1);
    final GeneralRPCProtos.Ack mockResponse = Acks.OK;
    when(mockHandler.resumeFragment(mockHandle)).thenReturn(mockResponse);
    controlTunnel.unpauseFragment(outcomeListener, mockHandle);
    latch.await();
  }

  /**
   * Verify that RequestQueryStatus is handled correctly using ControlTunnel with LocalControlConnectionManager
   */
  @Test
  public void testRequestQueryStatus() throws Exception {
    final UserBitShared.QueryId mockQueryId = UserBitShared.QueryId.getDefaultInstance();
    final UserBitShared.QueryProfile mockProfile = UserBitShared.QueryProfile.getDefaultInstance();
    when(mockHandler.requestQueryStatus(mockQueryId)).thenReturn(mockProfile);
    final UserBitShared.QueryProfile returnedProfile = controlTunnel.requestQueryProfile(mockQueryId).checkedGet();
    assertEquals(returnedProfile, mockProfile);
  }

  /**
   * Verify that CancelQuery with positive ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testCancelQuery_PositiveAck() throws Exception {
    final UserBitShared.QueryId mockQueryId = UserBitShared.QueryId.getDefaultInstance();
    final GeneralRPCProtos.Ack mockResponse = Acks.OK;
    when(mockHandler.requestQueryCancel(mockQueryId)).thenReturn(mockResponse);
    GeneralRPCProtos.Ack response = controlTunnel.requestCancelQuery(mockQueryId).checkedGet();
    assertEquals(response, mockResponse);
  }

  /**
   * Verify that CancelQuery with negative ack is handled correctly using ControlTunnel with
   * LocalControlConnectionManager
   */
  @Test
  public void testCancelQuery_NegativeAck() throws Exception {
    final UserBitShared.QueryId mockQueryId = UserBitShared.QueryId.getDefaultInstance();
    final GeneralRPCProtos.Ack mockResponse = Acks.FAIL;
    when(mockHandler.requestQueryCancel(mockQueryId)).thenReturn(mockResponse);
    GeneralRPCProtos.Ack response = controlTunnel.requestCancelQuery(mockQueryId).checkedGet();
    assertEquals(response, mockResponse);
  }

  /**
   * Verify that FinishedReceiver is handled correctly using ControlTunnel with LocalControlConnectionManager
   */
  @Test
  public void testInformReceiverFinished_success() throws Exception {
    final BitControl.FinishedReceiver finishedReceiver = BitControl.FinishedReceiver.getDefaultInstance();
    latch = new CountDownLatch(1);
    final GeneralRPCProtos.Ack mockResponse = Acks.OK;
    when(mockHandler.receivingFragmentFinished(finishedReceiver)).thenReturn(mockResponse);
    controlTunnel.informReceiverFinished(outcomeListener, finishedReceiver);
    latch.await();
  }
}

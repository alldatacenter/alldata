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
package org.apache.drill.exec.work.fragment;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.ops.FragmentStats;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.FragmentState;
import org.apache.drill.exec.rpc.control.ControlTunnel;
import org.apache.drill.exec.rpc.control.Controller;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.apache.drill.exec.proto.UserBitShared.FragmentState.CANCELLATION_REQUESTED;
import static org.apache.drill.exec.proto.UserBitShared.FragmentState.FAILED;
import static org.apache.drill.exec.proto.UserBitShared.FragmentState.RUNNING;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FragmentStatusReporterTest extends BaseTest {

  private FragmentStatusReporter statusReporter;

  private ControlTunnel foremanTunnel;

  @Before
  public void setUp() throws Exception {
    final FragmentContextImpl context = mock(FragmentContextImpl.class);
    Controller controller = mock(Controller.class);

    // Create Foreman Endpoint and it's tunnel
    DrillbitEndpoint foremanEndpoint = DrillbitEndpoint.newBuilder().setAddress("10.0.0.2").build();
    foremanTunnel = mock(ControlTunnel.class);

    when(context.getController()).thenReturn(controller);
    when(controller.getTunnel(foremanEndpoint)).thenReturn(foremanTunnel);

    when(context.getStats()).thenReturn(mock(FragmentStats.class));
    when(context.getHandle()).thenReturn(FragmentHandle.getDefaultInstance());
    when(context.getAllocator()).thenReturn(mock(BufferAllocator.class));
    when(context.getForemanEndpoint()).thenReturn(foremanEndpoint);
    statusReporter = new FragmentStatusReporter(context);
  }

  @Test
  public void testStateChanged() throws Exception {
    for (FragmentState state : FragmentState.values()) {
      try {
        statusReporter.stateChanged(state);
        if (state == FAILED) {
          fail("Expected exception: " + IllegalStateException.class.getName());
        }
      } catch (IllegalStateException e) {
        if (state != FAILED) {
          fail("Unexpected exception: " + e.toString());
        }
      }
    }
    verify(foremanTunnel, times(FragmentState.values().length - 2)) /* exclude SENDING and FAILED */
        .sendFragmentStatus(any(FragmentStatus.class));
  }

  @Test
  public void testFail() throws Exception {
    statusReporter.fail(null);
    verify(foremanTunnel).sendFragmentStatus(any(FragmentStatus.class));
  }

  @Test
  public void testClose() throws Exception {
    statusReporter.close();
    verifyZeroInteractions(foremanTunnel);
  }

  @Test
  public void testCloseClosed() throws Exception {
    statusReporter.close();
    statusReporter.close();
    verifyZeroInteractions(foremanTunnel);
  }

  @Test
  public void testStateChangedAfterClose() throws Exception {
    statusReporter.stateChanged(RUNNING);
    verify(foremanTunnel).sendFragmentStatus(any(FragmentStatus.class));
    statusReporter.close();
    statusReporter.stateChanged(CANCELLATION_REQUESTED);
    verify(foremanTunnel).sendFragmentStatus(any(FragmentStatus.class));
  }
}

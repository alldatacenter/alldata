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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.FragmentState;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.control.ControlTunnel;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The status reporter is responsible for receiving changes in fragment state and propagating the status back to the
 * Foreman either through a control tunnel or locally.
 */
public class FragmentStatusReporter implements AutoCloseable {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FragmentStatusReporter.class);

  protected final ExecutorFragmentContext context;

  protected final AtomicReference<DrillbitEndpoint> foremanDrillbit;

  public FragmentStatusReporter(final ExecutorFragmentContext context) {
    this.context = context;
    this.foremanDrillbit = new AtomicReference<>(context.getForemanEndpoint());
  }

  /**
   * Returns a {@link FragmentStatus} with the given state. {@link FragmentStatus} has additional information like
   * metrics, etc. that is gathered from the {@link ExecutorFragmentContext}.
   *
   * @param state the state to include in the status
   * @return the status
   */
  FragmentStatus getStatus(final FragmentState state) {
    return getStatus(state, null);
  }

  private FragmentStatus getStatus(final FragmentState state, final UserException ex) {
    final FragmentStatus.Builder status = FragmentStatus.newBuilder();
    final MinorFragmentProfile.Builder b = MinorFragmentProfile.newBuilder();
    context.getStats().addMetricsToStatus(b);
    b.setState(state);
    if (ex != null) {
      final boolean verbose = context.getOptions().getOption(ExecConstants.ENABLE_VERBOSE_ERRORS_KEY).bool_val;
      b.setError(ex.getOrCreatePBError(verbose));
    }
    status.setHandle(context.getHandle());
    b.setMemoryUsed(context.getAllocator().getAllocatedMemory());
    b.setMinorFragmentId(context.getHandle().getMinorFragmentId());
    status.setProfile(b);
    return status.build();
  }

  /**
   * Reports the state change to the Foreman. The state is wrapped in a {@link FragmentStatus} that has additional
   * information like metrics, etc. This additional information is gathered from the {@link ExecutorFragmentContext}.
   * NOTE: Use {@link #fail} to report state change to {@link FragmentState#FAILED}.
   *
   * @param newState the new state
   */
  void stateChanged(final FragmentState newState) {
    final FragmentStatus status = getStatus(newState, null);
    logger.info("{}: State to report: {}", QueryIdHelper.getQueryIdentifier(context.getHandle()), newState);
    switch (newState) {
      case AWAITING_ALLOCATION:
      case CANCELLATION_REQUESTED:
      case CANCELLED:
      case FINISHED:
      case RUNNING:
        sendStatus(status);
        break;
      case SENDING:
        // no op.
        break;
      case FAILED:
        // shouldn't get here since fail() should be called.
      default:
        throw new IllegalStateException(String.format("Received state changed event for unexpected state of %s.", newState));
    }
  }


  /**
   * Sends status to remote Foreman node using Control Tunnel or to Local Foreman bypassing
   * Control Tunnel and using WorkEventBus.
   *
   * @param status
   */
  void sendStatus(final FragmentStatus status) {

    DrillbitEndpoint foremanNode = foremanDrillbit.get();

    if (foremanNode == null) {
      logger.warn("{}: State {} is not reported as {} is closed", QueryIdHelper.getQueryIdentifier(context.getHandle()), status.getProfile().getState(), this);
      return;
    }

    // Send status for both local and remote foreman node via Tunnel. For local there won't be any network connection
    // created and it will be submitted locally using LocalControlConnectionManager
    final ControlTunnel tunnel = context.getController().getTunnel(foremanNode);
    tunnel.sendFragmentStatus(status);
  }

  /**
   * {@link FragmentStatus} with the {@link FragmentState#FAILED} state is reported to the Foreman. The
   * {@link FragmentStatus} has additional information like metrics, etc. that is gathered from the
   * {@link ExecutorFragmentContext}.
   *
   * @param ex the exception related to the failure
   */
  void fail(final UserException ex) {
    final FragmentStatus status = getStatus(FragmentState.FAILED, ex);
    sendStatus(status);
  }

  @Override
  public void close() {
    final DrillbitEndpoint foremanNode = foremanDrillbit.getAndSet(null);
    if (foremanNode != null) {
      logger.debug("Closing {}", this);
    } else {
      logger.warn("{} was already closed", this);
    }
  }
}

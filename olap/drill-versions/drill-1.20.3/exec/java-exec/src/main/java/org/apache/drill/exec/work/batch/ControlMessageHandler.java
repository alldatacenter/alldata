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
package org.apache.drill.exec.work.batch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.proto.BitControl.CustomMessage;
import org.apache.drill.exec.proto.BitControl.FinishedReceiver;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.InitializeFragments;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.RequestHandler;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.UserRpcException;
import org.apache.drill.exec.rpc.control.ControlConnection;
import org.apache.drill.exec.rpc.control.ControlRpcConfig;
import org.apache.drill.exec.rpc.control.CustomHandlerRegistry;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.work.WorkManager.WorkerBee;
import org.apache.drill.exec.work.foreman.Foreman;
import org.apache.drill.exec.work.fragment.FragmentExecutor;
import org.apache.drill.exec.work.fragment.FragmentManager;
import org.apache.drill.exec.work.fragment.FragmentStatusReporter;
import org.apache.drill.exec.work.fragment.NonRootFragmentManager;

import static org.apache.drill.exec.rpc.RpcBus.get;

public class ControlMessageHandler implements RequestHandler<ControlConnection> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlMessageHandler.class);
  private final WorkerBee bee;
  private final CustomHandlerRegistry handlerRegistry = new CustomHandlerRegistry();

  public ControlMessageHandler(final WorkerBee bee) {
    this.bee = bee;
  }

  @Override
  public void handle(ControlConnection connection, int rpcType, ByteBuf pBody, ByteBuf dBody,
                     ResponseSender sender) throws RpcException {
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("Received bit com message of type {}", rpcType);
    }

    switch (rpcType) {

    case RpcType.REQ_CANCEL_FRAGMENT_VALUE: {
      final FragmentHandle handle = get(pBody, FragmentHandle.PARSER);
      cancelFragment(handle);
      sender.send(ControlRpcConfig.OK);
      break;
    }

    case RpcType.REQ_CUSTOM_VALUE: {
      final CustomMessage customMessage = get(pBody, CustomMessage.PARSER);
      sender.send(handlerRegistry.handle(customMessage, (DrillBuf) dBody));
      break;
    }

    case RpcType.REQ_RECEIVER_FINISHED_VALUE: {
      final FinishedReceiver finishedReceiver = get(pBody, FinishedReceiver.PARSER);
      receivingFragmentFinished(finishedReceiver);
      sender.send(ControlRpcConfig.OK);
      break;
    }

    case RpcType.REQ_FRAGMENT_STATUS_VALUE:
      final FragmentStatus status = get(pBody, FragmentStatus.PARSER);
      requestFragmentStatus(status);
      // TODO: Support a type of message that has no response.
      sender.send(ControlRpcConfig.OK);
      break;

    case RpcType.REQ_QUERY_CANCEL_VALUE: {
      final QueryId queryId = get(pBody, QueryId.PARSER);
      final Ack cancelStatus = requestQueryCancel(queryId);
      if (cancelStatus.getOk()) {
        sender.send(ControlRpcConfig.OK);
      } else {
        sender.send(ControlRpcConfig.FAIL);
      }
      break;
    }

    case RpcType.REQ_INITIALIZE_FRAGMENTS_VALUE: {
      final InitializeFragments fragments = get(pBody, InitializeFragments.PARSER);
      initializeFragment(fragments);
      sender.send(ControlRpcConfig.OK);
      break;
    }

    case RpcType.REQ_QUERY_STATUS_VALUE: {
      final QueryId queryId = get(pBody, QueryId.PARSER);
      final QueryProfile profile = requestQueryStatus(queryId);
      sender.send(new Response(RpcType.RESP_QUERY_STATUS, profile));
      break;
    }

    case RpcType.REQ_UNPAUSE_FRAGMENT_VALUE: {
      final FragmentHandle handle = get(pBody, FragmentHandle.PARSER);
      resumeFragment(handle);
      sender.send(ControlRpcConfig.OK);
      break;
    }

    default:
      throw new RpcException("Not yet supported.");
    }
  }

  /**
   * Start a new fragment on this node. These fragments can be leaf or intermediate fragments
   * which are scheduled by remote or local Foreman node.
   * @param fragment
   * @throws UserRpcException
   */
  public void startNewFragment(final PlanFragment fragment, final DrillbitContext drillbitContext)
      throws UserRpcException {
    logger.debug("Received remote fragment start instruction: {}", fragment);

    try {
      final FragmentContextImpl fragmentContext = new FragmentContextImpl(drillbitContext, fragment,
          drillbitContext.getFunctionImplementationRegistry());
      final FragmentStatusReporter statusReporter = new FragmentStatusReporter(fragmentContext);
      final FragmentExecutor fragmentExecutor = new FragmentExecutor(fragmentContext, fragment, statusReporter);

      // we either need to start the fragment if it is a leaf fragment, or set up a fragment manager if it is non leaf.
      if (fragment.getLeafFragment()) {
        bee.addFragmentRunner(fragmentExecutor);
      } else {
        // isIntermediate, store for incoming data.
        final NonRootFragmentManager manager = new NonRootFragmentManager(fragment, fragmentExecutor, statusReporter);
        drillbitContext.getWorkBus().addFragmentManager(manager);
      }

    } catch (final ExecutionSetupException ex) {
      throw new UserRpcException(drillbitContext.getEndpoint(), "Failed to create fragment context", ex);
    } catch (final Exception e) {
        throw new UserRpcException(drillbitContext.getEndpoint(),
            "Failure while trying to start remote fragment", e);
    } catch (final OutOfMemoryError t) {
      if (t.getMessage().startsWith("Direct buffer")) {
        throw new UserRpcException(drillbitContext.getEndpoint(),
            "Out of direct memory while trying to start remote fragment", t);
      } else {
        throw t;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.drill.exec.work.batch.BitComHandler#cancelFragment(org.apache.drill.exec.proto.ExecProtos.FragmentHandle)
   */
  public Ack cancelFragment(final FragmentHandle handle) {
    /**
     * For case 1, see {@link org.apache.drill.exec.work.foreman.QueryManager#cancelExecutingFragments}.
     * In comments below, "active" refers to fragment states: SENDING, AWAITING_ALLOCATION, RUNNING and
     * "inactive" refers to FINISHED, CANCELLATION_REQUESTED, CANCELLED, FAILED
     */

    // Case 2: Cancel active intermediate fragment. Such a fragment will be in the work bus. Delegate cancel to the
    // work bus.
    final boolean removed = bee.getContext().getWorkBus().removeFragmentManager(handle, true);
    if (removed) {
      return Acks.OK;
    }

    // Case 3: Cancel active leaf fragment. Such a fragment will be with the worker bee if and only if it is running.
    // Cancel directly in this case.
    final FragmentExecutor runner = bee.getFragmentRunner(handle);
    if (runner != null) {
      runner.cancel();
      return Acks.OK;
    }

    // Other cases: Fragment completed or does not exist. Currently known cases:
    // (1) Leaf or intermediate fragment that is inactive: although we should not receive a cancellation
    //     request; it is possible that before the fragment state was updated in the QueryManager, this handler
    //     received a cancel signal.
    // (2) Unknown fragment.
    logger.warn("Dropping request to cancel fragment. {} does not exist.", QueryIdHelper.getQueryIdentifier(handle));
    return Acks.OK;
  }

  public Ack resumeFragment(final FragmentHandle handle) {
    // resume a pending fragment
    final FragmentManager manager = bee.getContext().getWorkBus().getFragmentManager(handle);
    if (manager != null) {
      manager.unpause();
      return Acks.OK;
    }

    // resume a paused fragment
    final FragmentExecutor runner = bee.getFragmentRunner(handle);
    if (runner != null) {
      runner.unpause();
      return Acks.OK;
    }

    // fragment completed or does not exist
    logger.warn("Dropping request to resume fragment. {} does not exist.", QueryIdHelper.getQueryIdentifier(handle));
    return Acks.OK;
  }

  public Ack receivingFragmentFinished(final FinishedReceiver finishedReceiver) {

    final FragmentManager manager = bee.getContext().getWorkBus().getFragmentManager(finishedReceiver.getSender());

    if (manager != null) {
      manager.receivingFragmentFinished(finishedReceiver.getReceiver());
    } else {
      final FragmentExecutor executor = bee.getFragmentRunner(finishedReceiver.getSender());
      if (executor != null) {
        executor.receivingFragmentFinished(finishedReceiver.getReceiver());
      } else {
        logger.warn(
            "Dropping request for early fragment termination for path {} -> {} as path to executor unavailable.",
            QueryIdHelper.getQueryIdentifier(finishedReceiver.getSender()),
            QueryIdHelper.getQueryIdentifier(finishedReceiver.getReceiver()));
      }
    }

    return Acks.OK;
  }

  public Ack requestFragmentStatus(FragmentStatus status) {
    bee.getContext().getWorkBus().statusUpdate( status);
    return Acks.OK;
  }

  public Ack requestQueryCancel(QueryId queryId) {
    return bee.cancelForeman(queryId, null) ? Acks.OK : Acks.FAIL;
  }

  public Ack initializeFragment(InitializeFragments fragments) throws RpcException {
    final DrillbitContext drillbitContext = bee.getContext();
    for (int i = 0; i < fragments.getFragmentCount(); i++) {
      startNewFragment(fragments.getFragment(i), drillbitContext);
    }

    return Acks.OK;
  }

  public QueryProfile requestQueryStatus(QueryId queryId) throws RpcException {
    final Foreman foreman = bee.getForemanForQueryId(queryId);
    if (foreman == null) {
      throw new RpcException("Query not running on node.");
    }
    return foreman.getQueryManager().getQueryProfile();
  }

  public CustomHandlerRegistry getHandlerRegistry() {
    return handlerRegistry;
  }

}

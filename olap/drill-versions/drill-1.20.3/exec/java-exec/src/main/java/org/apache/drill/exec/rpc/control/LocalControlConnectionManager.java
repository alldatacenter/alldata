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
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.BasicClient;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.RpcCommand;
import org.apache.drill.exec.rpc.RpcConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.work.batch.ControlMessageHandler;

public class LocalControlConnectionManager extends ControlConnectionManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LocalControlConnectionManager.class);

  private final ControlConnectionConfig config;

  public LocalControlConnectionManager(ControlConnectionConfig config, DrillbitEndpoint localEndpoint) {
    super(localEndpoint, localEndpoint);
    this.config = config;
  }

  @Override
  protected BasicClient<?, ControlConnection, BitControl.BitControlHandshake, ?> getNewClient() {
    throw new UnsupportedOperationException("LocalControlConnectionManager doesn't support creating a control client");
  }

  @Override
  public void runCommand(RpcCommand cmd) {
    final int rpcType = cmd.getRpcType().getNumber();
    final ControlMessageHandler messageHandler = config.getMessageHandler();

    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("Received bit com message of type {} over local connection manager", rpcType);
    }

    switch (rpcType) {

      case BitControl.RpcType.REQ_CANCEL_FRAGMENT_VALUE: {
        final ControlTunnel.SignalFragment signalFragment = ((ControlTunnel.SignalFragment) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = signalFragment.getOutcomeListener();
        final Ack ackResponse = messageHandler.cancelFragment(signalFragment.getMessage());
        outcomeListener.success(ackResponse, null);
        break;
      }

      case BitControl.RpcType.REQ_CUSTOM_VALUE: {
        final ByteBuf[] dataBodies;
        final RpcOutcomeListener<BitControl.CustomMessage> outcomeListener;

        if (cmd instanceof ControlTunnel.CustomMessageSender) {
          dataBodies = ((ControlTunnel.CustomMessageSender)cmd).getDataBodies();
          outcomeListener = ((ControlTunnel.CustomMessageSender)cmd).getOutcomeListener();
        } else if (cmd instanceof ControlTunnel.SyncCustomMessageSender) {
          dataBodies = ((ControlTunnel.SyncCustomMessageSender)cmd).getDataBodies();
          outcomeListener = ((ControlTunnel.SyncCustomMessageSender)cmd).getOutcomeListener();
        } else {
          throw new UnsupportedOperationException("Unknown Custom Type control message received");
        }

        DrillBuf reqDrillBuff;
        try {
          reqDrillBuff = convertToByteBuf(dataBodies);
        } catch (Exception ex) {
          outcomeListener.failed(new RpcException("Failed to allocate memory while sending request in " +
            "LocalControlConnectionManager#convertToByteBuff", ex));
          return;
        } finally {
          releaseByteBuf(dataBodies);
        }

        try {
          BitControl.CustomMessage message = (BitControl.CustomMessage) cmd.getMessage();
          final Response response = messageHandler.getHandlerRegistry().handle(message, reqDrillBuff);
          DrillBuf responseBuffer;
          try {
            responseBuffer = convertToByteBuf(response.dBodies);
          } catch (Exception ex) {
            outcomeListener.failed(new RpcException("Failed to allocate memory while sending response in " +
              "LocalControlConnectionManager#convertToByteBuff", ex));
            return;
          } finally {
            releaseByteBuf(response.dBodies);
          }

          // Passed responseBuffer will be owned by consumer
          outcomeListener.success((BitControl.CustomMessage)response.pBody, responseBuffer);
        } catch (RpcException ex) {
          cmd.getOutcomeListener().failed(ex);
        } finally {
          // Release the reqDrillBuff passed into handler
          releaseByteBuf(reqDrillBuff);
        }
        break;
      }

      case BitControl.RpcType.REQ_RECEIVER_FINISHED_VALUE: {
        final ControlTunnel.ReceiverFinished receiverFinished = ((ControlTunnel.ReceiverFinished) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = receiverFinished.getOutcomeListener();
        final Ack ackResponse = messageHandler.receivingFragmentFinished(receiverFinished.getMessage());
        outcomeListener.success(ackResponse, null);
        break;
      }

      case BitControl.RpcType.REQ_FRAGMENT_STATUS_VALUE: {
        final ControlTunnel.SendFragmentStatus fragmentStatus = ((ControlTunnel.SendFragmentStatus) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = fragmentStatus.getOutcomeListener();
        final Ack ackResponse = messageHandler.requestFragmentStatus(fragmentStatus.getMessage());
        outcomeListener.success(ackResponse, null);
        break;
      }

      case BitControl.RpcType.REQ_QUERY_CANCEL_VALUE: {
        final ControlTunnel.CancelQuery cancelQuery = ((ControlTunnel.CancelQuery) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = cancelQuery.getOutcomeListener();
        final Ack ackResponse = messageHandler.requestQueryCancel(cancelQuery.getMessage());
        outcomeListener.success(ackResponse, null);
        break;
      }

      case BitControl.RpcType.REQ_INITIALIZE_FRAGMENTS_VALUE: {
        final ControlTunnel.SendFragment sendFragment = ((ControlTunnel.SendFragment) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = sendFragment.getOutcomeListener();

        try {
          final Ack ackResponse = messageHandler.initializeFragment(sendFragment.getMessage());
          outcomeListener.success(ackResponse, null);
        } catch (RpcException ex) {
          outcomeListener.failed(ex);
        }
        break;
      }

      case BitControl.RpcType.REQ_QUERY_STATUS_VALUE: {
        final ControlTunnel.RequestProfile requestProfile = ((ControlTunnel.RequestProfile) cmd);
        final RpcOutcomeListener<UserBitShared.QueryProfile> outcomeListener = requestProfile.getOutcomeListener();

        try {
          final UserBitShared.QueryProfile profile = messageHandler.requestQueryStatus(requestProfile.getMessage());
          outcomeListener.success(profile, null);
        } catch (RpcException ex) {
          outcomeListener.failed(ex);
        }
        break;
      }

      case BitControl.RpcType.REQ_UNPAUSE_FRAGMENT_VALUE: {
        final ControlTunnel.SignalFragment signalFragment = ((ControlTunnel.SignalFragment) cmd);
        final RpcOutcomeListener<Ack> outcomeListener = signalFragment.getOutcomeListener();
        final Ack ackResponse = messageHandler.resumeFragment(signalFragment.getMessage());
        outcomeListener.success(ackResponse, null);
        break;
      }

      default:
        final RpcException rpcException = new RpcException(String.format("Unsupported control request type %s " +
          "received on LocalControlConnectionManager", rpcType));
        cmd.getOutcomeListener().failed(rpcException);
    }
  }

  /**
   * Copies ByteBuf in the input array into a single DrillBuf
   * @param byteBuffArray - input array of ByteBuf's
   * @return DrillBuf - output Drillbuf with all the input bytes.
   * @throws OutOfMemoryException
   */
  private DrillBuf convertToByteBuf(ByteBuf[] byteBuffArray) throws OutOfMemoryException {

    if (byteBuffArray == null) {
      return null;
    }

    int bytesToCopy = 0;
    for (ByteBuf b : byteBuffArray) {
      final int validBytes = b.readableBytes();
      if (0 == validBytes) {
        b.release();
      } else {
        bytesToCopy += validBytes;
      }
    }
    final DrillBuf drillBuff = config.getAllocator().buffer(bytesToCopy);

    for (ByteBuf b : byteBuffArray) {
      final int validBytes = b.readableBytes();
      drillBuff.writeBytes(b, 0, validBytes);
    }

    return drillBuff;
  }

  /**
   * Releases all the ByteBuf inside the input ByteBuf array
   * @param byteBuffArray - input array of ByteBuf's
   */
  private void releaseByteBuf(ByteBuf[] byteBuffArray) {
    if (byteBuffArray != null) {
      for (ByteBuf b : byteBuffArray) {
        b.release();
      }
    }
  }

  /**
   * Releases the input ByteBuf
   * @param byteBuf - input ByteBuf
   */
  private void releaseByteBuf(ByteBuf byteBuf) {
    if (byteBuf != null) {
      byteBuf.release();
    }
  }
}

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
package org.apache.drill.exec.rpc;

import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.common.exceptions.DrillException;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @param <CC> Client Connection Listener
 * @param <HS> Outbound handshake message type
 * @param <HR> Inbound handshake message type
 * @param <BC> BasicClient type
 *             <p>
 *             Implements a wrapper class that allows a client connection to associate different behaviours after
 *             establishing a connection with the server. The client can choose to send an application handshake, or
 *             in the case of SSL, wait for a SSL handshake completion and then send an application handshake.
 */

public class ConnectionMultiListener<T extends EnumLite, CC extends ClientConnection, HS extends MessageLite, HR extends MessageLite, BC extends BasicClient<T, CC, HS, HR>> {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectionMultiListener.class);

  private final RpcConnectionHandler<CC> connectionListener;
  private final HS handshakeValue;
  private final BC parent;

  private ConnectionMultiListener(RpcConnectionHandler<CC> connectionListener, HS handshakeValue,
      BC basicClient) {
    assert connectionListener != null;
    assert handshakeValue != null;

    this.connectionListener = connectionListener;
    this.handshakeValue = handshakeValue;
    this.parent = basicClient;
  }

  public static <T extends EnumLite, CC extends ClientConnection, HS extends MessageLite, HR extends MessageLite, BC extends BasicClient<T, CC, HS, HR>>
  Builder<T, CC, HS, HR, BC>
  newBuilder(RpcConnectionHandler<CC> connectionListener, HS handshakeValue,
      BC basicClient) {
    return new Builder<>(connectionListener, handshakeValue, basicClient);
  }

  ConnectionHandler connectionHandler = null;
  private HandshakeSendHandler handshakeSendHandler = null;
  private SSLConnectionHandler sslConnectionHandler = null;

  /**
   * Manages connection establishment outcomes.
   */
  private class ConnectionHandler implements GenericFutureListener<ChannelFuture> {

    @Override
    public void operationComplete(ChannelFuture future) {
      boolean isInterrupted = false;

      // We want to wait for at least 120 secs when interrupts occur. Establishing a connection fails/succeeds quickly,
      // So there is no point propagating the interruption as failure immediately.
      long remainingWaitTimeMills = 120000;
      long startTime = System.currentTimeMillis();
      // logger.debug("Connection operation finished.  Success: {}", future.isSuccess());
      while (true) {
        try {
          future.get(remainingWaitTimeMills, TimeUnit.MILLISECONDS);
          if (future.isSuccess()) {
            SocketAddress remote = future.channel().remoteAddress();
            SocketAddress local = future.channel().localAddress();
            parent.setAddresses(remote, local);
            // if SSL is enabled send the handshake after the ssl handshake is completed, otherwise send it
            // now
            if(!parent.isSslEnabled()) {
              // send a handshake on the current thread. This is the only time we will send from within the event thread.
              // We can do this because the connection will not be backed up.
              parent.send(handshakeSendHandler, handshakeValue, true);
            }
          } else {
            connectionListener.connectionFailed(RpcConnectionHandler.FailureType.CONNECTION,
                new RpcException("General connection failure."));
          }
          // logger.debug("Handshake queued for send.");
          break;
        } catch (final InterruptedException interruptEx) {
          remainingWaitTimeMills -= (System.currentTimeMillis() - startTime);
          startTime = System.currentTimeMillis();
          isInterrupted = true;
          if (remainingWaitTimeMills < 1) {
            connectionListener.connectionFailed(RpcConnectionHandler.FailureType.CONNECTION, interruptEx);
            break;
          }
          // Ignore the interrupt and continue to wait until we elapse remainingWaitTimeMills.
        } catch (final Exception ex) {
          logger.error("Failed to establish connection", ex);
          connectionListener.connectionFailed(RpcConnectionHandler.FailureType.CONNECTION, ex);
          break;
        }
      }

      if (isInterrupted) {
        // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
        // interruption and respond to it if it wants to.
        Thread.currentThread().interrupt();
      }
    }
  }

  private class SSLConnectionHandler implements GenericFutureListener<Future<Channel>> {
    @Override
    public void operationComplete(Future<Channel> future) {
      // send the handshake
      parent.send(handshakeSendHandler, handshakeValue, true);
    }
  }

  /**
   * manages handshake outcomes.
   */
  private class HandshakeSendHandler implements RpcOutcomeListener<HR> {

    @Override
    public void failed(RpcException ex) {
      logger.debug("Failure while initiating handshake", ex);
      connectionListener.connectionFailed(RpcConnectionHandler.FailureType.HANDSHAKE_COMMUNICATION, ex);
    }

    @Override
    public void success(HR value, ByteBuf buffer) {
      // logger.debug("Handshake received. {}", value);
      try {
        final List<String> serverAuthMechanisms = parent.validateHandshake(value);
        parent.finalizeConnection(value, parent.connection);

        // If auth is required then start the SASL handshake
        if (serverAuthMechanisms != null) {
          parent.prepareSaslHandshake(connectionListener, serverAuthMechanisms);
        } else {
          connectionListener.connectionSucceeded(parent.connection);
          logger.debug("Handshake completed successfully.");
        }
      } catch (NonTransientRpcException ex) {
        logger.error("Failure while validating client and server sasl compatibility", ex);
        connectionListener.connectionFailed(RpcConnectionHandler.FailureType.AUTHENTICATION, ex);
      } catch (Throwable t) {
        logger.error("Failure while validating handshake", t);
        connectionListener.connectionFailed(RpcConnectionHandler.FailureType.HANDSHAKE_VALIDATION, t);
      }
    }

    @Override
    public void interrupted(final InterruptedException ex) {
      logger.warn("Interrupted while waiting for handshake response", ex);
      connectionListener.connectionFailed(RpcConnectionHandler.FailureType.HANDSHAKE_COMMUNICATION, ex);
    }
  }

  /*
    The SSL Handshake listener is special in that it is needed at the time of initializing an SSL
    enabled pipeline and so is instantiated before the instance of the outer class may be needed.
    We create an instance and set a reference back to the outer class instance when it is created
    at the time of connection.
   */
  public static class SSLHandshakeListener implements GenericFutureListener<Future<Channel>> {
    ConnectionMultiListener parent;
    SSLHandshakeListener() {
    }

    public void setParent(ConnectionMultiListener cml){
      this.parent = cml;
    }

    @Override
    public void operationComplete(Future<Channel> future) throws Exception {
      if(parent != null){
        if(future.isSuccess()) {
          Channel c = future.get();
          parent.sslConnectionHandler.operationComplete(future);
          parent.parent.setSslChannel(c);
        } else {
          throw new DrillException("SSL handshake failed.", future.cause());
        }
      } else {
        throw new RpcException("RPC Setup error. SSL handshake complete handler is not set up.");
      }
    }
  }


  public static class Builder<T extends EnumLite, CC extends ClientConnection, HS extends MessageLite, HR extends MessageLite, BC extends BasicClient<T, CC, HS, HR> > {

    private final RpcConnectionHandler<CC> connectionListener;
    private final HS handshakeValue;
    private final BC basicClient;
    boolean enableSSL = false;

    private ConnectionMultiListener<T, CC, HS, HR, BC> cml;

    private Builder(RpcConnectionHandler<CC> connectionListener, HS handshakeValue, BC basicClient) {
      this.connectionListener = connectionListener;
      this.handshakeValue = handshakeValue;
      this.basicClient = basicClient;
    }

    Builder<T, CC, HS, HR, BC> enableSSL() {
      enableSSL = true;
      return this;
    }

    public ConnectionMultiListener<T, CC, HS, HR, BC> build() {
      this.cml = new ConnectionMultiListener<>(connectionListener, handshakeValue, basicClient);
      cml.connectionHandler = cml.new ConnectionHandler();
      cml.handshakeSendHandler = cml.new HandshakeSendHandler();
      if(enableSSL) {
        cml.sslConnectionHandler = cml.new SSLConnectionHandler();
      }
      return cml;
    }

  }

}

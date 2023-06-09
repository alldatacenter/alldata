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
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.AbstractFuture;
import com.google.protobuf.MessageLite;

/**
 * Manager all connections between two particular bits.
 */
public abstract class ReconnectingConnection<C extends ClientConnection, HS extends MessageLite>
    implements Closeable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReconnectingConnection.class);

  private final AtomicReference<C> connectionHolder = new AtomicReference<C>();
  private final String host;
  private final int port;
  private final HS handshake;

  public ReconnectingConnection(HS handshake, String host, int port) {
    Preconditions.checkNotNull(host);
    Preconditions.checkArgument(port > 0);
    this.host = host;
    this.port = port;
    this.handshake = handshake;
  }

  protected abstract BasicClient<?, C, HS, ?> getNewClient();

  public <T extends MessageLite, E extends EnumLite, M extends MessageLite,
    R extends RpcCommand<T, C, E, M>> void runCommand(R cmd) {
//    if(logger.isDebugEnabled()) logger.debug(String.format("Running command %s sending to host %s:%d", cmd, host, port));
    C connection = connectionHolder.get();
    if (connection != null) {
      if (connection.isActive()) {
        cmd.connectionAvailable(connection);
//        logger.debug("Connection available and active, command run inline.");
        return;
      } else {
        // remove the old connection. (don't worry if we fail since someone else should have done it.
        connectionHolder.compareAndSet(connection, null);
      }
    }

    /**
     * We've arrived here without a connection, let's make sure only one of us makes a connection. (fyi, another
     * endpoint could create a reverse connection
     **/
    synchronized (this) {
      connection = connectionHolder.get();
      if (connection != null) {
        cmd.connectionAvailable(connection);

      } else {
//        logger.debug("No connection active, opening client connection.");
        BasicClient<?, C, HS, ?> client = getNewClient();
        ConnectionListeningFuture<T,E,M> future = new ConnectionListeningFuture<>(cmd);
        client.connectAsClient(future, handshake, host, port);
        future.waitAndRun();
//        logger.debug("Connection available and active, command now being run inline.");
      }
      return;

    }
  }

  public class ConnectionListeningFuture<R extends MessageLite, E extends EnumLite, M extends MessageLite>
      extends AbstractFuture<C>
      implements RpcConnectionHandler<C> {

    private RpcCommand<R, C, E, M> cmd;

    public ConnectionListeningFuture(RpcCommand<R, C, E, M> cmd) {
      super();
      this.cmd = cmd;
    }

    /**
     * Called by
     */
    public void waitAndRun() {
      boolean isInterrupted = false;

      // We want to wait for at least 120 secs when interrupts occur. Establishing a connection fails/succeeds quickly,
      // So there is no point propagating the interruption as failure immediately.
      long remainingWaitTimeMills = 120000;
      long startTime = System.currentTimeMillis();

      while(true) {
        try {
          //        logger.debug("Waiting for connection.");
          C connection = this.get(remainingWaitTimeMills, TimeUnit.MILLISECONDS);

          if (connection == null) {
            //          logger.debug("Connection failed.");
          } else {
            //          logger.debug("Connection received. {}", connection);
            cmd.connectionSucceeded(connection);
            //          logger.debug("Finished connection succeeded activity.");
          }
          break;
        } catch (final InterruptedException interruptEx) {
          remainingWaitTimeMills -= (System.currentTimeMillis() - startTime);
          startTime = System.currentTimeMillis();
          isInterrupted = true;
          if (remainingWaitTimeMills < 1) {
            cmd.connectionFailed(FailureType.CONNECTION, interruptEx);
            break;
          }
          // Ignore the interrupt and continue to wait until we elapse remainingWaitTimeMills.
        } catch (final ExecutionException | TimeoutException ex) {
          logger.error("Failed to establish connection", ex);
          cmd.connectionFailed(FailureType.CONNECTION, ex);
          break;
        }
      }

      if (isInterrupted) {
        // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
        // interruption and respond to it if it wants to.
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void connectionFailed(FailureType type, Throwable t) {
      set(null);
      cmd.connectionFailed(type, t);
    }

    @Override
    public void connectionSucceeded(C incoming) {
      C connection = connectionHolder.get();
      while (true) {
        boolean setted = connectionHolder.compareAndSet(null, incoming);
        if (setted) {
          connection = incoming;
          break;
        }
        connection = connectionHolder.get();
        if (connection != null) {
          break;
        }
      }

      if (connection != incoming) {
        // close the incoming because another channel was created in the mean time (unless this is a self connection).
        logger.debug("Closing incoming connection because a connection was already set.");
        incoming.getChannel().close();
      }
      set(connection);
    }
  }

  /** Factory for close handlers **/
  public class CloseHandlerCreator {
    public GenericFutureListener<ChannelFuture> getHandler(C connection,
                                                           GenericFutureListener<ChannelFuture> parent) {
      return new CloseHandler(connection, parent);
    }
  }

  /**
   * Listens for connection closes and clears connection holder.
   */
  protected class CloseHandler implements GenericFutureListener<ChannelFuture> {
    private C connection;
    private GenericFutureListener<ChannelFuture> parent;

    public CloseHandler(C connection, GenericFutureListener<ChannelFuture> parent) {
      super();
      this.connection = connection;
      this.parent = parent;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      connectionHolder.compareAndSet(connection, null);
      parent.operationComplete(future);
    }
  }

  public CloseHandlerCreator getCloseHandlerCreator() {
    return new CloseHandlerCreator();
  }

  public void addExternalConnection(C connection) {
    // if the connection holder is not set, set it to this incoming connection. We'll simply ignore if already set.
    this.connectionHolder.compareAndSet(null, connection);
  }

  @Override
  public void close() {
    C c = connectionHolder.getAndSet(null);
    if (c != null) {
      c.getChannel().close();
    }
  }
}

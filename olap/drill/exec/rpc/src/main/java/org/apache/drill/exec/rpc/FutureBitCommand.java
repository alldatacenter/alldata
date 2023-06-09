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

import org.apache.drill.shaded.guava.com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;

public abstract class FutureBitCommand<T extends MessageLite, C extends RemoteConnection,
  E extends EnumLite, M extends MessageLite> implements RpcCommand<T, C, E, M> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FutureBitCommand.class);

  protected final SettableFuture<T> settableFuture;
  private final RpcCheckedFuture<T> parentFuture;

  private final RpcOutcomeListener<T> outcomeListener;

  public FutureBitCommand() {
    this.settableFuture = SettableFuture.create();
    this.parentFuture = new RpcCheckedFuture<T>(settableFuture);
    outcomeListener = new DeferredRpcOutcome();
  }

  public abstract void doRpcCall(RpcOutcomeListener<T> outcomeListener, C connection);

  @Override
  public void connectionAvailable(C connection) {

    doRpcCall(outcomeListener, connection);
  }

  @Override
  public void connectionSucceeded(C connection) {
    connectionAvailable(connection);
  }

  private class DeferredRpcOutcome implements RpcOutcomeListener<T> {

    @Override
    public void failed(RpcException ex) {
      settableFuture.setException(ex);
    }

    @Override
    public void success(T value, ByteBuf buf) {
      parentFuture.setBuffer(buf);
      settableFuture.set(value);
    }

    @Override
    public void interrupted(final InterruptedException e) {
      // If we are interrupted while performing the command, consider as failure.
      logger.warn("Interrupted while running the command", e);
      failed(new RpcException(e));
    }
  }

  public DrillRpcFuture<T> getFuture() {
    return parentFuture;
  }

  @Override
  public RpcOutcomeListener<T> getOutcomeListener() {
    return outcomeListener;
  }

  @Override
  public void connectionFailed(FailureType type, Throwable t) {
    settableFuture.setException(RpcException.mapException(
        String.format("Command failed while establishing connection.  Failure type %s.", type), t));
  }
}

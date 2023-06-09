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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;

/**
 * Manages the creation of rpc futures for a particular socket <--> socket
 * connection. Generally speaking, there will be two threads working with this
 * class (the socket thread and the Request generating thread). Synchronization
 * is simple with the map being the only thing that is protected. Everything
 * else works via Atomic variables.
 */
class RequestIdMap {
  private static final Logger logger = LoggerFactory.getLogger(RequestIdMap.class);

  private final AtomicInteger lastCoordinationId = new AtomicInteger();
  private final AtomicBoolean isOpen = new AtomicBoolean(true);

  /** Access to map must be protected. **/
  private final IntObjectHashMap<RpcOutcome<?>> map;

  public RequestIdMap() {
    map = new IntObjectHashMap<>();
  }

  void channelClosed(Throwable ex) {
    isOpen.set(false);
    if (ex != null) {
      final RpcException e = RpcException.mapException(ex);
      IntObjectHashMap<RpcOutcome<?>> clonedMap;
      synchronized (map) {
        clonedMap = map.clone();
        map.clear();
      }
      clonedMap.forEach(new SetExceptionProcedure(e));
    }
  }

  private class SetExceptionProcedure implements IntObjectProcedure<RpcOutcome<?>> {
    final RpcException exception;

    public SetExceptionProcedure(RpcException exception) {
      this.exception = exception;
    }

    @Override
    public void apply(int key, RpcOutcome<?> value) {
      try {
        value.setException(exception);
      } catch (final Exception e){
        logger.warn("Failure while attempting to fail rpc response.", e);
      }
    }
  }

  public <V> ChannelListenerWithCoordinationId createNewRpcListener(RpcOutcomeListener<V> handler, Class<V> clazz,
      RemoteConnection connection) {
    final int i = lastCoordinationId.incrementAndGet();
    final RpcListener<V> future = new RpcListener<>(handler, clazz, i, connection);
    final Object old;
    synchronized (map) {
      Preconditions.checkArgument(isOpen.get(),
          "Attempted to send a message when connection is no longer valid.");
      old = map.put(i, future);
    }
    Preconditions.checkArgument(old == null,
        "You attempted to reuse a coordination id when the previous coordination id has not been removed.  "
        + "This is likely rpc future callback memory leak.");
    return future;
  }

  private class RpcListener<T> implements ChannelListenerWithCoordinationId, RpcOutcome<T> {
    final RpcOutcomeListener<T> handler;
    final Class<T> clazz;
    final int coordinationId;
    @SuppressWarnings("unused")
    final RemoteConnection connection;

    public RpcListener(RpcOutcomeListener<T> handler, Class<T> clazz, int coordinationId, RemoteConnection connection) {
      super();
      this.handler = handler;
      this.clazz = clazz;
      this.coordinationId = coordinationId;
      this.connection = connection;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      if (!future.isSuccess()) {
        try {
          removeFromMap(coordinationId);
        } finally {
          final Throwable cause = future.cause();
          if (future.channel().isActive()) {
            setException(cause == null ? new RpcException("Unknown ChannelFuture operation failure") : cause);
          } else {
            setException(cause == null ? new ChannelClosedException() : new ChannelClosedException(cause));
          }
        }
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void set(Object value, ByteBuf buffer) {
      assert clazz.isAssignableFrom(value.getClass());
      handler.success((T) value, buffer);
    }

    @Override
    public void setException(Throwable t) {
      handler.failed(RpcException.mapException(t));
    }

    @Override
    public Class<T> getOutcomeType() { return clazz; }

    @Override
    public int getCoordinationId() { return coordinationId; }
  }

  private RpcOutcome<?> removeFromMap(int coordinationId) {
    final RpcOutcome<?> rpc;
    synchronized (map) {
      rpc = map.remove(coordinationId);
    }
    if (rpc == null) {
      throw new IllegalStateException(
          "Attempting to retrieve an rpc that wasn't first stored in the rpc coordination queue.  This would most likely happen if you're opposite endpoint sent multiple messages on the same coordination id.");
    }
    return rpc;
  }

  @SuppressWarnings("unchecked")
  public <V> RpcOutcome<V> getAndRemoveRpcOutcome(int rpcType, int coordinationId, Class<V> clazz) {

    final RpcOutcome<?> rpc = removeFromMap(coordinationId);
    // logger.debug("Got rpc from map {}", rpc);
    final Class<?> outcomeClass = rpc.getOutcomeType();

    if (outcomeClass != clazz) {
      throw new IllegalStateException(String.format(
          "RPC Engine had a submission and response configuration mismatch.  The RPC request that you submitted was defined with an expected response type of %s.  However, "
              + "when the response returned, a call to getResponseDefaultInstance() with Rpc number %d provided an expected class of %s.  This means either your submission uses the wrong type definition"
              + "or your getResponseDefaultInstance() method responds the wrong instance type ",
          clazz.getCanonicalName(), rpcType, outcomeClass.getCanonicalName()));
    }

    return (RpcOutcome<V>) rpc;
  }

  public void recordRemoteFailure(int coordinationId, DrillPBError failure) {
    try {
      final RpcOutcome<?> rpc = removeFromMap(coordinationId);
      rpc.setException(new UserRemoteException(failure));
    } catch (final Exception ex) {
      logger.warn("Failed to remove from map.  Not a problem since we were updating on failed future.", ex);
    }
  }
}

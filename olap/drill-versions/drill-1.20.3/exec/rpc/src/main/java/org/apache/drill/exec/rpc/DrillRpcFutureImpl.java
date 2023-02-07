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

import io.netty.buffer.ByteBuf;

import org.apache.drill.common.concurrent.AbstractCheckedFuture;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.AbstractFuture;

class DrillRpcFutureImpl<V> extends AbstractCheckedFuture<V, RpcException>
    implements DrillRpcFuture<V>, RpcOutcomeListener<V> {
  private volatile ByteBuf buffer;

  public DrillRpcFutureImpl() {
    super(new InnerFuture<>());
  }

  @Override
  protected RpcException mapException(Exception ex) {
    return RpcException.mapException(ex);
  }

  private static class InnerFuture<T> extends AbstractFuture<T> {
    // we rewrite these so that the parent can see them

    void setValue(T value) {
      super.set(value);
    }

    @Override
    protected boolean setException(Throwable t) {
      return super.setException(t);
    }
  }

  @Override
  public void failed(RpcException ex) {
    ((InnerFuture<V>) delegate()).setException(ex);
  }

  @Override
  public void success(V value, ByteBuf buffer) {
    this.buffer = buffer;
    ((InnerFuture<V>) delegate()).setValue(value);
  }

  @Override
  public void interrupted(final InterruptedException ex) {
    // Propagate the interrupt to inner future
    delegate().cancel(true);
  }

  @Override
  public ByteBuf getBuffer() {
    return buffer;
  }

  public void release() {
    if (buffer != null) {
      buffer.release();
    }
  }

}

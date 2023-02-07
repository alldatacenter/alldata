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
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;

public class RpcCheckedFuture<T> extends AbstractCheckedFuture<T, RpcException> implements DrillRpcFuture<T> {

  private volatile ByteBuf buffer;

  public RpcCheckedFuture(ListenableFuture<T> delegate) {
    super(delegate);
  }

  public void setBuffer(ByteBuf buffer) {
    if (buffer != null) {
      buffer.retain();
      this.buffer = buffer;
    }
  }

  @Override
  protected RpcException mapException(Exception e) {
    return RpcException.mapException(e);
  }

  @Override
  public ByteBuf getBuffer() {
    return buffer;
  }

}

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
import io.netty.buffer.ByteBufInputStream;

import java.io.InputStream;

import org.apache.drill.exec.proto.GeneralRPCProtos.RpcMode;

public class InboundRpcMessage extends RpcMessage{
  public ByteBuf pBody;
  public ByteBuf dBody;

  public InboundRpcMessage(RpcMode mode, int rpcType, int coordinationId, ByteBuf pBody, ByteBuf dBody) {
    super(mode, rpcType, coordinationId);
    this.pBody = pBody;
    this.dBody = dBody;
  }

  @Override
  public int getBodySize() {
    int len = pBody.capacity();
    if (dBody != null) {
      len += dBody.capacity();
    }
    return len;
  }

  @Override
  void release() {
    if (pBody != null) {
      pBody.release();
    }
    if (dBody != null) {
      dBody.release();
    }
  }

  @Override
  public String toString() {
    return "InboundRpcMessage [pBody=" + pBody + ", mode=" + mode + ", rpcType=" + rpcType + ", coordinationId="
        + coordinationId + ", dBody=" + dBody + "]";
  }

  public InputStream getProtobufBodyAsIS() {
    return new ByteBufInputStream(pBody);
  }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.plugin.flink.network;

import java.nio.charset.StandardCharsets;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;

import org.apache.celeborn.common.network.buffer.NettyManagedBuffer;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.plugin.flink.buffer.FlinkNettyManagedBuffer;
import org.apache.celeborn.plugin.flink.protocol.ReadData;

public class MessageDecoderExt {
  public static Message decode(Message.Type type, ByteBuf in, boolean decodeBody) {
    long requestId;
    // cannot use actual class decode method because common module cannot refer flink shaded netty.
    switch (type) {
      case RPC_REQUEST:
        requestId = in.readLong();
        in.readInt();
        if (decodeBody) {
          return new RpcRequest(requestId, new FlinkNettyManagedBuffer(in));
        } else {
          return new RpcRequest(requestId, NettyManagedBuffer.EmptyBuffer);
        }

      case RPC_RESPONSE:
        requestId = in.readLong();
        in.readInt();
        if (decodeBody) {
          return new RpcResponse(requestId, new FlinkNettyManagedBuffer(in));
        } else {
          return new RpcResponse(requestId, NettyManagedBuffer.EmptyBuffer);
        }

      case RPC_FAILURE:
        requestId = in.readLong();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String errorString = new String(bytes, StandardCharsets.UTF_8);
        return new RpcFailure(requestId, errorString);

      case ONE_WAY_MESSAGE:
        in.readInt();
        if (decodeBody) {
          return new OneWayMessage(new FlinkNettyManagedBuffer(in));
        } else {
          return new OneWayMessage(NettyManagedBuffer.EmptyBuffer);
        }

      case READ_ADD_CREDIT:
        long streamId = in.readLong();
        int credit = in.readInt();
        return new ReadAddCredit(streamId, credit);

      case READ_DATA:
        streamId = in.readLong();
        int backlog = in.readInt();
        long offset = in.readLong();
        return new ReadData(streamId, backlog, offset);

      case BACKLOG_ANNOUNCEMENT:
        streamId = in.readLong();
        backlog = in.readInt();
        return new BacklogAnnouncement(streamId, backlog);

      default:
        throw new IllegalArgumentException("Unexpected message type: " + type);
    }
  }
}

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

package org.apache.celeborn.common.network.protocol;

import java.nio.ByteBuffer;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.network.buffer.NettyManagedBuffer;

/** An on-the-wire transmittable message. */
public abstract class Message implements Encodable {
  private ManagedBuffer body;

  protected Message() {
    this(null);
  }

  protected Message(ManagedBuffer body) {
    this.body = body;
  }

  /** Used to identify this request type. */
  public abstract Type type();

  /** An optional body for the message. */
  public ManagedBuffer body() {
    return body;
  }

  public void setBody(ByteBuf buf) {
    this.body = new NettyManagedBuffer(buf);
  }

  public void setBody(ByteBuffer buf) {
    this.body = new NettyManagedBuffer(buf);
  }

  /** Whether the body should be copied out in frame decoder. */
  public boolean needCopyOut() {
    return false;
  }

  protected boolean equals(Message other) {
    return Objects.equal(body, other.body);
  }

  public ByteBuffer toByteBuffer() {
    // Allow room for encoded message, plus the type byte
    ByteBuf buf = Unpooled.buffer(encodedLength() + 1);
    buf.writeByte(type().id());
    encode(buf);
    assert buf.writableBytes() == 0 : "Writable bytes remain: " + buf.writableBytes();
    return buf.nioBuffer();
  }

  /** Preceding every serialized Message is its type, which allows us to deserialize it. */
  public enum Type implements Encodable {
    UNKNOWN_TYPE(-1),
    CHUNK_FETCH_REQUEST(0),
    CHUNK_FETCH_SUCCESS(1),
    CHUNK_FETCH_FAILURE(2),
    RPC_REQUEST(3),
    RPC_RESPONSE(4),
    RPC_FAILURE(5),
    OPEN_STREAM(6),
    STREAM_HANDLE(7),
    ONE_WAY_MESSAGE(9),
    PUSH_DATA(11),
    PUSH_MERGED_DATA(12),
    REGION_START(13),
    REGION_FINISH(14),
    PUSH_DATA_HAND_SHAKE(15),
    READ_ADD_CREDIT(16),
    READ_DATA(17),
    OPEN_STREAM_WITH_CREDIT(18),
    BACKLOG_ANNOUNCEMENT(19);
    private final byte id;

    Type(int id) {
      assert id < 128 : "Cannot have more than 128 message types";
      this.id = (byte) id;
    }

    public byte id() {
      return id;
    }

    @Override
    public int encodedLength() {
      return 1;
    }

    @Override
    public void encode(ByteBuf buf) {
      buf.writeByte(id);
    }

    public static Type decode(ByteBuffer buffer) {
      ByteBuf buf = Unpooled.wrappedBuffer(buffer);
      return decode(buf);
    }

    public static Type decode(ByteBuf buf) {
      byte id = buf.readByte();
      switch (id) {
        case 0:
          return CHUNK_FETCH_REQUEST;
        case 1:
          return CHUNK_FETCH_SUCCESS;
        case 2:
          return CHUNK_FETCH_FAILURE;
        case 3:
          return RPC_REQUEST;
        case 4:
          return RPC_RESPONSE;
        case 5:
          return RPC_FAILURE;
        case 6:
          return OPEN_STREAM;
        case 7:
          return STREAM_HANDLE;
        case 9:
          return ONE_WAY_MESSAGE;
        case 11:
          return PUSH_DATA;
        case 12:
          return PUSH_MERGED_DATA;
        case 13:
          return REGION_START;
        case 14:
          return REGION_FINISH;
        case 15:
          return PUSH_DATA_HAND_SHAKE;
        case 16:
          return READ_ADD_CREDIT;
        case 17:
          return READ_DATA;
        case 18:
          return OPEN_STREAM_WITH_CREDIT;
        case 19:
          return BACKLOG_ANNOUNCEMENT;
        case -1:
          throw new IllegalArgumentException("User type messages cannot be decoded.");
        default:
          throw new IllegalArgumentException("Unknown message type: " + id);
      }
    }
  }

  public static Message decode(Type msgType, ByteBuf in) {
    return decode(msgType, in, true);
  }

  public static Message decode(Type msgType, ByteBuf in, boolean decodeBody) {
    switch (msgType) {
      case CHUNK_FETCH_REQUEST:
        return ChunkFetchRequest.decode(in);

      case CHUNK_FETCH_SUCCESS:
        return ChunkFetchSuccess.decode(in, decodeBody);

      case CHUNK_FETCH_FAILURE:
        return ChunkFetchFailure.decode(in);

      case RPC_REQUEST:
        return RpcRequest.decode(in, decodeBody);

      case RPC_RESPONSE:
        return RpcResponse.decode(in, decodeBody);

      case RPC_FAILURE:
        return RpcFailure.decode(in);

      case OPEN_STREAM:
        return OpenStream.decode(in);

      case STREAM_HANDLE:
        return StreamHandle.decode(in);

      case ONE_WAY_MESSAGE:
        return OneWayMessage.decode(in, decodeBody);

      case PUSH_DATA:
        return PushData.decode(in, decodeBody);

      case PUSH_MERGED_DATA:
        return PushMergedData.decode(in, decodeBody);

      case REGION_START:
        return RegionStart.decode(in);

      case REGION_FINISH:
        return RegionFinish.decode(in);

      case PUSH_DATA_HAND_SHAKE:
        return PushDataHandShake.decode(in);

      case READ_ADD_CREDIT:
        return ReadAddCredit.decode(in);

      case OPEN_STREAM_WITH_CREDIT:
        return OpenStreamWithCredit.decode(in);

      case BACKLOG_ANNOUNCEMENT:
        return BacklogAnnouncement.decode(in);

      default:
        throw new IllegalArgumentException("Unexpected message type: " + msgType);
    }
  }

  public static Message decode(ByteBuffer buffer) {
    ByteBuf buf = Unpooled.wrappedBuffer(buffer);
    Type type = Type.decode(buf);
    return decode(type, buf);
  }
}

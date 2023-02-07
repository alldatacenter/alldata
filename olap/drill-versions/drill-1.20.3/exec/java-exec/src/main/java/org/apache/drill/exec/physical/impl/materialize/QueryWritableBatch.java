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
package org.apache.drill.exec.physical.impl.materialize;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

import org.apache.drill.exec.proto.UserBitShared.QueryData;

public class QueryWritableBatch {

  private final QueryData header;
  private final ByteBuf[] buffers;

  public QueryWritableBatch(QueryData header, ByteBuf... buffers) {
    this.header = header;
    this.buffers = buffers;
  }

  public ByteBuf[] getBuffers() {
    return buffers;
  }

  public long getByteCount() {
    long n = 0;
    for (ByteBuf buf : buffers) {
      n += buf.readableBytes();
    }
    return n;
  }

  public QueryData getHeader() {
    return header;
  }

  @Override
  public String toString() {
    return "QueryWritableBatch [header=" + header + ", buffers=" + Arrays.toString(buffers) + "]";
  }
}

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

import static org.apache.celeborn.common.network.protocol.Message.Type.OPEN_STREAM_WITH_CREDIT;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;

/** Buffer stream used in Map partition scenario. */
public final class OpenStreamWithCredit extends RequestMessage {
  public final byte[] shuffleKey;
  public final byte[] fileName;
  public final int startIndex;
  public final int endIndex;
  public final int initialCredit;

  public OpenStreamWithCredit(
      byte[] shuffleKey, byte[] fileName, int startIndex, int endIndex, int initialCredit) {
    this.shuffleKey = shuffleKey;
    this.fileName = fileName;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.initialCredit = initialCredit;
  }

  public OpenStreamWithCredit(
      String shuffleKey, String fileName, int startIndex, int endIndex, int initialCredit) {
    this(
        shuffleKey.getBytes(StandardCharsets.UTF_8),
        fileName.getBytes(StandardCharsets.UTF_8),
        startIndex,
        endIndex,
        initialCredit);
  }

  @Override
  public int encodedLength() {
    return 4 + shuffleKey.length + 4 + fileName.length + 4 + 4 + 4;
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeInt(shuffleKey.length);
    buf.writeBytes(shuffleKey);
    buf.writeInt(fileName.length);
    buf.writeBytes(fileName);
    buf.writeInt(startIndex);
    buf.writeInt(endIndex);
    buf.writeInt(initialCredit);
  }

  @Override
  public Message.Type type() {
    return OPEN_STREAM_WITH_CREDIT;
  }

  public static OpenStreamWithCredit decode(ByteBuf in) {
    int shuffleKeyLength = in.readInt();
    byte[] tmpShuffleKey = new byte[shuffleKeyLength];
    in.readBytes(tmpShuffleKey);
    int fileNameLength = in.readInt();
    byte[] tmpFileName = new byte[fileNameLength];
    in.readBytes(tmpFileName);
    int startSubIndex = in.readInt();
    int endSubIndex = in.readInt();
    int initialCredit = in.readInt();
    return new OpenStreamWithCredit(
        tmpShuffleKey, tmpFileName, startSubIndex, endSubIndex, initialCredit);
  }
}

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

package com.bytedance.bitsail.base.decoder;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

/**
 * Created 2020/10/20.
 */
public class MessageDecodeCompositor implements MessageDecoder {

  private final List<MessageDecoder> decoders = Lists.newLinkedList();

  /**
   * Decode input byte array recursively by decoders.
   * Note that the order of decoder call is the same as the order these decoders were added.
   *
   * @param input An input byte array to decode.
   * @return Decoded byte array by all added decoders.
   */
  @Override
  public byte[] decode(byte[] input) {
    byte[] tmp = input;
    for (MessageDecoder decoder : decoders) {
      tmp = decoder.decode(tmp);
    }
    return tmp;
  }

  /**
   * Add initialized decoder to compositor.
   *
   * @param decoder An initialized decoder.
   */
  public void addDecoder(MessageDecoder decoder) {
    if (Objects.nonNull(decoder)) {
      decoders.add(decoder);
    }
  }
}

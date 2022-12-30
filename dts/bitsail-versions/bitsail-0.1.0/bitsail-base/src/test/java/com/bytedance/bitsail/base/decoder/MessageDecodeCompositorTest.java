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

import org.junit.Assert;
import org.junit.Test;

public class MessageDecodeCompositorTest {

  private final byte[] output1 = new byte[] {4, 5, 6};
  private final byte[] output2 = new byte[] {7, 8, 9};

  MessageDecoder decoder1 = input -> output1;
  MessageDecoder decoder2 = input -> output2;

  @Test
  public void testMessageDecoderCompositor() {
    MessageDecodeCompositor compositor = new MessageDecodeCompositor();
    compositor.addDecoder(decoder1);
    compositor.addDecoder(decoder2);
    Assert.assertArrayEquals(output2, compositor.decode(new byte[0]));

    compositor = new MessageDecodeCompositor();
    compositor.addDecoder(decoder2);
    compositor.addDecoder(null);
    compositor.addDecoder(decoder1);
    Assert.assertArrayEquals(output1, compositor.decode(new byte[0]));
  }
}

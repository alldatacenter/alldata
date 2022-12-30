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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.decoder;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import com.google.common.primitives.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created 2020/10/20.
 */
public class MessageDecodeCompositorTest {

  @Test
  public void testSkipMessageDecoder() {
    BitSailConfiguration jonConf = BitSailConfiguration.newDefault();
    jonConf.set(FileSystemCommonOptions.DUMP_SKIP_BYTES, 8);
    SkipMessageDecoder decoder = new SkipMessageDecoder(jonConf);
    String test = "test";
    byte[] prefix = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};

    byte[] decode = decoder.decode(Bytes.concat(prefix, test.getBytes()));
    Assert.assertEquals(test, new String(decode));

    jonConf.set(FileSystemCommonOptions.DUMP_SKIP_BYTES, 0);
    decoder = new SkipMessageDecoder(jonConf);
    decode = decoder.decode(Bytes.concat(prefix, test.getBytes()));
    Assert.assertEquals(decode.length, 12);
  }

}
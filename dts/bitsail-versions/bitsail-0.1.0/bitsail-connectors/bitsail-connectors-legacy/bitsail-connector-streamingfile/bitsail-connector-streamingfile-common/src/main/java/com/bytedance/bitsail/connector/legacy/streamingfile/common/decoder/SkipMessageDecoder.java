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

import com.bytedance.bitsail.base.decoder.MessageDecoder;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import com.google.common.base.Preconditions;

import java.util.Arrays;

/**
 * Created 2020/10/20.
 */
public class SkipMessageDecoder implements MessageDecoder {

  private BitSailConfiguration jobConf;

  private int skipByteLength;

  public SkipMessageDecoder(BitSailConfiguration jobConf) {
    this.jobConf = jobConf;
    this.skipByteLength = jobConf.get(FileSystemCommonOptions.DUMP_SKIP_BYTES);
  }

  @Override
  public byte[] decode(byte[] input) {
    if (skipByteLength > 0) {
      Preconditions.checkArgument(input.length > skipByteLength,
          "Skip message length bigger %d than message length %d.",
          skipByteLength, input.length);
      return Arrays.copyOfRange(input, skipByteLength, input.length);
    }
    return input;
  }
}

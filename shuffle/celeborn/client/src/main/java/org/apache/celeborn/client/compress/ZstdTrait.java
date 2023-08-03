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

package org.apache.celeborn.client.compress;

public abstract class ZstdTrait {
  protected static final byte[] MAGIC = new byte[] {'Z', 'S', 'T', 'D', 'B', 'l', 'o', 'c', 'k'};
  protected static final int MAGIC_LENGTH = MAGIC.length;

  public static final int HEADER_LENGTH =
      MAGIC_LENGTH // magic bytes
          + 1 // compress method
          + 4 // compressed length
          + 4 // decompressed length
          + 4; // checksum

  protected static final int COMPRESSION_METHOD_RAW = 0x10;
  protected static final int COMPRESSION_METHOD_ZSTD = 0x30;
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.utils;

import java.util.UUID;

/**
 * This is a random id generator.
 */
public class IdGenerator {
  public static final int ID_BYTE_LENGTH = 4;

  /**
   * generate a upstream job id
   *
   * @return four length bytes id
   */
  public static byte[] generateUpstreamId() {
    byte[] upstreamId = new byte[ID_BYTE_LENGTH];
    String uuidStr = UUID.randomUUID().toString();
    for (int i = 0; i < ID_BYTE_LENGTH; i++) {
      upstreamId[i] = (byte) uuidStr.charAt(i);
    }
    return upstreamId;
  }

  /**
   * generate a random id
   * @return random id
   */
  public static long randomId() {
    UUID uuid = UUID.randomUUID();
    long mostSignificantBits = uuid.getMostSignificantBits();
    long leastSignificantBits = uuid.getLeastSignificantBits();
    return (mostSignificantBits ^ leastSignificantBits) & Long.MAX_VALUE;
  }
}

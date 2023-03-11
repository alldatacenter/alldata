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

package com.netease.arctic.log;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Arrays;

import static com.netease.arctic.log.LogData.MAGIC_NUMBER;

/**
 * Log format version.
 */
public enum FormatVersion {
  FORMAT_VERSION_V1(new MessageBytes().append(MAGIC_NUMBER).append((byte) 1).toBytes());

  /**
   * the version of log format, contains a fixed magic number and actual version number,
   * the byte array length of the version must be equal to four..
   */
  byte[] version;
  byte versionNum;
  byte[] magicNum;

  FormatVersion(byte[] version) {
    Preconditions.checkArgument(null != version && version.length == 4,
        "format version is null or length is not equal to 4.");
    this.version = version;
    this.magicNum = Bytes.subByte(version, 0, 3);
    versionNum = Bytes.subByte(version, 3, 1)[0];
  }

  public byte[] asBytes() {
    return version;
  }

  public String asString() {
    return new String(magicNum) + versionNum;
  }

  public byte getVersionNum() {
    return versionNum;
  }

  public static FormatVersion fromBytes(byte[] data) {
    for (FormatVersion formatVersion : FormatVersion.values()) {
      byte[] expected = formatVersion.asBytes();
      if (Arrays.equals(expected, data)) {
        return formatVersion;
      }
    }
    return null;
  }
}

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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.common.row;

public enum RowKind {

  // Note: Enums have no stable hash code across different JVMs, use toByteValue() for
  // this purpose.

  /**
   * Insertion operation.
   */
  INSERT("+I", (byte) 0),

  /**
   * Update operation with the previous content of the updated row.
   *
   * <p>This kind SHOULD occur together with {@link #UPDATE_AFTER} for modelling an update that needs
   * to retract the previous row first. It is useful in cases of a non-idempotent update, i.e., an
   * update of a row that is not uniquely identifiable by a key.
   */
  UPDATE_BEFORE("-U", (byte) 1),

  /**
   * Update operation with new content of the updated row.
   *
   * <p>This kind CAN occur together with {@link #UPDATE_BEFORE} for modelling an update that
   * needs to retract the previous row first. OR it describes an idempotent update, i.e., an update
   * of a row that is uniquely identifiable by a key.
   */
  UPDATE_AFTER("+U", (byte) 2),

  /**
   * Deletion operation.
   */
  DELETE("-D", (byte) 3);

  private final String shortString;

  private final byte value;

  RowKind(String shortString, byte value) {
    this.shortString = shortString;
    this.value = value;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public static RowKind fromByteValue(byte value) {
    switch (value) {
      case 0:
        return INSERT;
      case 1:
        return UPDATE_BEFORE;
      case 2:
        return UPDATE_AFTER;
      case 3:
        return DELETE;
      default:
        throw new UnsupportedOperationException(
            "Unsupported byte value '" + value + "' for row kind.");
    }
  }

  /**
   * <p><ul>
   * <li>"+I" represents {@link #INSERT}.</li>
   * <li>"-U" represents {@link #UPDATE_BEFORE}.</li>
   * <li>"+U" represents {@link #UPDATE_AFTER}.</li>
   * <li>"-D" represents {@link #DELETE}.</li>
   * </ul>
   */
  public String shortString() {
    return shortString;
  }

  /**
   * <p><ul>
   * <li>"0" represents {@link #INSERT}.</li>
   * <li>"1" represents {@link #UPDATE_BEFORE}.</li>
   * <li>"2" represents {@link #UPDATE_AFTER}.</li>
   * <li>"3" represents {@link #DELETE}.</li>
   * </ul>
   */
  public byte toByteValue() {
    return value;
  }
}

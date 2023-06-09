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

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.data.ChangeAction;
import org.apache.flink.types.RowKind;

/**
 * An util that converts between {@link RowKind} and {@link ChangeAction}.
 */
public class RowKindUtil {

  public static RowKind convertToFlinkRowKind(ChangeAction changeAction) {
    switch (changeAction) {
      case INSERT:
        return RowKind.INSERT;
      case UPDATE_BEFORE:
        return RowKind.UPDATE_BEFORE;
      case UPDATE_AFTER:
        return RowKind.UPDATE_AFTER;
      case DELETE:
        return RowKind.DELETE;
      default:
        throw new IllegalArgumentException("This is unsupported ChangeAction:" + changeAction);
    }
  }

  public static ChangeAction transformFromFlinkRowKind(RowKind rowKind) {
    switch (rowKind) {
      case INSERT:
        return ChangeAction.INSERT;
      case UPDATE_BEFORE:
        return ChangeAction.UPDATE_BEFORE;
      case UPDATE_AFTER:
        return ChangeAction.UPDATE_AFTER;
      case DELETE:
        return ChangeAction.DELETE;
      default:
        throw new IllegalArgumentException("This is unsupported RowKind:" + rowKind);
    }
  }
}

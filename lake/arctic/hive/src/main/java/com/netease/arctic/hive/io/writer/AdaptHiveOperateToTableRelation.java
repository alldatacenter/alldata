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

package com.netease.arctic.hive.io.writer;

import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.ChangeLocationKind;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.WriteOperationKind;

public class AdaptHiveOperateToTableRelation implements OperateToTableRelation {

  public static final AdaptHiveOperateToTableRelation INSTANT = new AdaptHiveOperateToTableRelation();

  @Override
  public LocationKind getLocationKindsFromOperateKind(
      ArcticTable arcticTable, WriteOperationKind writeOperationKind) {
    if (arcticTable.isKeyedTable()) {
      if (TableTypeUtil.isHive(arcticTable)) {
        switch (writeOperationKind) {
          case APPEND:
            return ChangeLocationKind.INSTANT;
          case MINOR_OPTIMIZE:
          case MAJOR_OPTIMIZE:
            return BaseLocationKind.INSTANT;
          case OVERWRITE:
          case FULL_OPTIMIZE:
            return HiveLocationKind.INSTANT;
        }
      } else {
        switch (writeOperationKind) {
          case APPEND:
            return ChangeLocationKind.INSTANT;
          case MINOR_OPTIMIZE:
          case MAJOR_OPTIMIZE:
          case OVERWRITE:
          case FULL_OPTIMIZE:
            return BaseLocationKind.INSTANT;
        }
      }
    } else {
      if (TableTypeUtil.isHive(arcticTable)) {
        switch (writeOperationKind) {
          case APPEND:
          case MAJOR_OPTIMIZE:
            return BaseLocationKind.INSTANT;
          case OVERWRITE:
          case FULL_OPTIMIZE:
            return HiveLocationKind.INSTANT;
          case MINOR_OPTIMIZE:
            throw new IllegalArgumentException("UnKeyed table don't support minor optimize");
        }
      } else {
        return BaseLocationKind.INSTANT;
      }
    }
    return null;
  }
}

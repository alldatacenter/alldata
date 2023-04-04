/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.TableFileUtils;
import com.netease.arctic.utils.TableTypeUtil;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.base.Strings;

import java.util.Map;

public class TableTrashManagers {
  public static String DEFAULT_TRASH_DIR = ".trash";

  /**
   * Build {@link TableTrashManager} from {@link ArcticTable}.
   *
   * @param table - {@link ArcticTable}
   * @return TableTrashManager
   */
  public static TableTrashManager build(ArcticTable table) {
    String tableRootLocation;
    if (!TableTypeUtil.isIcebergTableFormat(table) && table.isUnkeyedTable()) {
      tableRootLocation = TableFileUtils.getFileDir(table.location());
    } else {
      tableRootLocation = table.location();
    }
    return build(table.id(), tableRootLocation, table.properties(), table.io());
  }

  /**
   * Build {@link TableTrashManager}.
   *
   * @param tableIdentifier - table identifier
   * @param tableLocation   - table root location
   * @param tableProperties - table properties
   * @param fileIO          - table file io
   * @return - built table trash manager
   */
  public static TableTrashManager build(TableIdentifier tableIdentifier, String tableLocation,
                                        Map<String, String> tableProperties, ArcticFileIO fileIO) {
    String customTrashRootLocation = tableProperties.get(TableProperties.TABLE_TRASH_CUSTOM_ROOT_LOCATION);
    String trashLocation = getTrashLocation(tableIdentifier, tableLocation, customTrashRootLocation);
    return new BasicTableTrashManager(tableIdentifier, fileIO, tableLocation, trashLocation);
  }

  /**
   * Get trash location.
   *
   * @param tableIdentifier         - table identifier
   * @param tableLocation           - table root location
   * @param customTrashRootLocation - from the table property table-trash.custom-root-location
   * @return trash location
   */
  @VisibleForTesting
  static String getTrashLocation(TableIdentifier tableIdentifier, String tableLocation,
                                        String customTrashRootLocation) {
    String trashParentLocation;
    if (Strings.isNullOrEmpty(customTrashRootLocation)) {
      trashParentLocation = tableLocation;
    } else {
      trashParentLocation = getTrashParentLocation(tableIdentifier, customTrashRootLocation);
    }
    return trashParentLocation + "/" + DEFAULT_TRASH_DIR;
  }

  /**
   * Get trash parent location, when table are deleted, the trash parent location should be deleted.
   *
   * @param tableIdentifier         - table identifier
   * @param customTrashRootLocation - from the table property table-trash.custom-root-location
   * @return trash parent location.
   */
  public static String getTrashParentLocation(TableIdentifier tableIdentifier, String customTrashRootLocation) {
    Preconditions.checkNotNull(customTrashRootLocation);
    if (!customTrashRootLocation.endsWith("/")) {
      customTrashRootLocation = customTrashRootLocation + "/";
    }
    return customTrashRootLocation + tableIdentifier.getCatalog() + "/" + tableIdentifier.getDatabase() + "/" +
        tableIdentifier.getTableName();
  }
}

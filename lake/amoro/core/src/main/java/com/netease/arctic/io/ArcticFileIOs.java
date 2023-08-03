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

package com.netease.arctic.io;

import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.iceberg.util.PropertyUtil;

import java.util.Map;

public class ArcticFileIOs {
  
  public static final boolean CLOSE_TRASH = true;

  public static ArcticHadoopFileIO buildRecoverableHadoopFileIO(
      TableIdentifier tableIdentifier, String tableLocation,
      Map<String, String> tableProperties, TableMetaStore tableMetaStore,
      Map<String, String> catalogProperties) {
    tableProperties = CatalogUtil.mergeCatalogPropertiesToTable(tableProperties, catalogProperties);
    if (!CLOSE_TRASH && PropertyUtil.propertyAsBoolean(tableProperties, TableProperties.ENABLE_TABLE_TRASH,
        TableProperties.ENABLE_TABLE_TRASH_DEFAULT)) {
      ArcticHadoopFileIO fileIO = new ArcticHadoopFileIO(tableMetaStore);
      TableTrashManager trashManager =
          TableTrashManagers.build(tableIdentifier, tableLocation, tableProperties, fileIO);
      String trashFilePattern = PropertyUtil.propertyAsString(tableProperties, TableProperties.TABLE_TRASH_FILE_PATTERN,
          TableProperties.TABLE_TRASH_FILE_PATTERN_DEFAULT);

      return new RecoverableHadoopFileIO(tableMetaStore, trashManager, trashFilePattern);
    } else {
      return new ArcticHadoopFileIO(tableMetaStore);
    }
  }

  public static ArcticHadoopFileIO buildHadoopFileIO(TableMetaStore tableMetaStore) {
    return new ArcticHadoopFileIO(tableMetaStore);
  }
}

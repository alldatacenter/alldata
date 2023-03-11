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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.table.KeyedTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ChangeFilesUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ChangeFilesUtil.class);

  public static void tryClearChangeFiles(KeyedTable keyedTable, List<DataFile> changeFiles) {
    try {
      if (keyedTable.primaryKeySpec().primaryKeyExisted()) {
        int step = 3000;
        for (int startIndex = 0; startIndex < changeFiles.size(); startIndex += step) {
          int end = startIndex + step;
          List<DataFile> tableFiles = subList(changeFiles, startIndex, end);
          if (tableFiles.isEmpty()) {
            break;
          }
          LOG.info("{} delete {} change files", keyedTable.id(), tableFiles.size());
          deleteChangeFiles(keyedTable, tableFiles);
          LOG.info("{} change committed, delete {} files, complete {}/{}", keyedTable.id(),
              tableFiles.size(), Math.min(end, changeFiles.size()), changeFiles.size());
        }
      }
    } catch (Throwable t) {
      LOG.error(keyedTable.id() + " failed to delete change files, ignore", t);
    }
  }

  private static <T> List<T> subList(List<T> list, int from, int end) {
    List<T> subList = new ArrayList<>();
    for (int i = from; i < end; i++) {
      if (i >= list.size()) {
        break;
      }
      subList.add(list.get(i));
    }
    return subList;
  }

  private static void deleteChangeFiles(KeyedTable keyedTable, List<DataFile> changeFiles) {
    if (CollectionUtils.isEmpty(changeFiles)) {
      return;
    }
    DeleteFiles changeDelete = keyedTable.changeTable().newDelete();
    changeFiles.forEach(changeDelete::deleteFile);
    changeDelete.commit();
  }
}

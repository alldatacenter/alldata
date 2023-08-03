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

package com.netease.arctic.spark.test.helper;

import com.netease.arctic.utils.StructLikeSet;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TableFiles {

  public final Set<DataFile> baseDataFiles;
  public final Set<DeleteFile> baseDeleteFiles;

  public final Set<DataFile> changeInsertFiles;
  public final Set<DataFile> changeEqDeleteFiles;

  public TableFiles(
      Set<DataFile> baseDataFiles, Set<DeleteFile> baseDeleteFiles,
      Set<DataFile> changeInsertFiles, Set<DataFile> changeEqDeleteFiles) {
    this.baseDataFiles = baseDataFiles;
    this.baseDeleteFiles = baseDeleteFiles;
    this.changeInsertFiles = changeInsertFiles;
    this.changeEqDeleteFiles = changeEqDeleteFiles;
  }

  public TableFiles(Set<DataFile> baseDataFiles, Set<DeleteFile> baseDeleteFiles) {
    this.baseDataFiles = baseDataFiles;
    this.baseDeleteFiles = baseDeleteFiles;
    this.changeInsertFiles = Collections.emptySet();
    this.changeEqDeleteFiles = Collections.emptySet();
  }

  public TableFiles filterByPartitions(StructLikeSet partitions) {
    return filter(f -> partitions.contains(f.partition()));
  }

  public TableFiles removeFiles(Set<String> filePathSet) {
    return filter(f -> !filePathSet.contains(f.path().toString()));
  }

  private TableFiles filter(Predicate<ContentFile<?>> filter) {
    Set<DataFile> base = baseDataFiles.stream()
        .filter(filter)
        .collect(Collectors.toSet());

    Set<DeleteFile> baseDelete = baseDeleteFiles.stream()
        .filter(filter)
        .collect(Collectors.toSet());

    Set<DataFile> changeInsert = changeInsertFiles.stream()
        .filter(filter)
        .collect(Collectors.toSet());

    Set<DataFile> changeDelete = changeEqDeleteFiles.stream()
        .filter(filter)
        .collect(Collectors.toSet());
    return new TableFiles(base, baseDelete, changeInsert, changeDelete);
  }
}

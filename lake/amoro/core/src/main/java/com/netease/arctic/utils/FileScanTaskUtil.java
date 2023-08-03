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

import com.netease.arctic.scan.ArcticFileScanTask;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Utility class for working with file scan tasks.
 */
public class FileScanTaskUtil {

  /**
   * Converts a collection of ArcticFileScanTask objects to a string representation.
   * The string representation includes details about each file, such as its path, type, mask, index,
   * transaction ID, file size in bytes, and record count.
   *
   * @param tasks the collection of ArcticFileScanTask objects to convert to a string
   * @return a string representation of the ArcticFileScanTask objects
   */
  public static String toString(Collection<ArcticFileScanTask> tasks) {
    if (tasks == null) {
      return "[]";
    }
    return Iterables.toString(tasks.stream()
        .map(ArcticFileScanTask::file)
        .map(primaryKeyedFile ->
            MoreObjects.toStringHelper(primaryKeyedFile)
                .add("\n\tfile", primaryKeyedFile.path().toString())
                .add("\n\ttype", primaryKeyedFile.type().shortName())
                .add("\n\tmask", primaryKeyedFile.node().mask())
                .add("\n\tindex", primaryKeyedFile.node().index())
                .add("\n\ttransactionId", primaryKeyedFile.transactionId())
                .add("\n\tfileSizeInBytes", primaryKeyedFile.fileSizeInBytes())
                .add("\n\trecordCount", primaryKeyedFile.recordCount())
                .toString()).collect(Collectors.toList()));
  }
}

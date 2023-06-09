/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.scan.v3.file;

import java.util.List;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class FileScanUtils {

  // Default file metadata column names; primarily for testing.

  public static final String FILE_NAME_COL = "filename";
  public static final String FULLY_QUALIFIED_NAME_COL = "fqn";
  public static final String FILE_PATH_COL = "filepath";
  public static final String SUFFIX_COL = "suffix";
  public static final String PARTITION_COL = "dir";

  public static String partitionColName(int partition) {
    return PARTITION_COL + partition;
  }

  public static List<SchemaPath> expandMetadata(int dirCount) {
    // Use of Guava rather than Arrays.asList() because
    // we need a mutable list so we an add the partition
    // columns.
    List<String> selected = Lists.newArrayList(
        FULLY_QUALIFIED_NAME_COL,
        FILE_PATH_COL,
        FILE_NAME_COL,
        SUFFIX_COL);

    for (int i = 0; i < dirCount; i++) {
      selected.add(PARTITION_COL + Integer.toString(i));
    }
    return RowSetTestUtils.projectList(selected);
  }

  public static List<SchemaPath> projectAllWithMetadata(int dirCount) {
    return RowSetTestUtils.concat(
        RowSetTestUtils.projectAll(),
        expandMetadata(dirCount));
  }
}

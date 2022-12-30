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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.types.Row;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@link PartitionComputer} for {@link Row}.
 */
@Internal
public class RowPartitionComputer implements PartitionComputer<Row> {

  private static final long serialVersionUID = 1L;

  private final String defaultPartName;
  private final String[] partitionColumns;
  private final int[] nonPartitionIndexes;
  private final int[] partitionIndexes;

  public RowPartitionComputer(String defaultPartName, String[] columnNames, String[] partitionColumns) {
    this.defaultPartName = defaultPartName;
    this.partitionColumns = partitionColumns;
    List<String> columnList = Arrays.asList(columnNames);
    this.partitionIndexes = Arrays.stream(partitionColumns).mapToInt(columnList::indexOf).toArray();
    List<Integer> partitionIndexList = Arrays.stream(partitionIndexes).boxed().collect(Collectors.toList());
    this.nonPartitionIndexes = IntStream.range(0, columnNames.length)
        .filter(c -> !partitionIndexList.contains(c))
        .toArray();
  }

  @Override
  public LinkedHashMap<String, String> generatePartValues(Row in) {
    LinkedHashMap<String, String> partSpec = new LinkedHashMap<>();

    for (int i = 0; i < partitionIndexes.length; i++) {
      int index = partitionIndexes[i];
      Object field = in.getField(index);
      String partitionValue = field != null ? field.toString() : null;
      if (partitionValue == null || "".equals(partitionValue)) {
        partitionValue = defaultPartName;
      }
      partSpec.put(partitionColumns[i], partitionValue);
    }
    return partSpec;
  }

  @Override
  public Row projectColumnsToWrite(Row in) {
    return partitionIndexes.length == 0 ? in : Row.project(in, nonPartitionIndexes);
  }

  @Override
  public Tuple2<LinkedHashMap<String, String>, Row> generatePartitionAndRowData(Row in) throws Exception {
    return new Tuple2<>(generatePartValues(in), projectColumnsToWrite(in));
  }
}

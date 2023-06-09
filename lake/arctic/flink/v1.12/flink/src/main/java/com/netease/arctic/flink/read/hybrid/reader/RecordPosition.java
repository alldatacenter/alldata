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

package com.netease.arctic.flink.read.hybrid.reader;

import com.netease.arctic.flink.read.source.ChangeLogDataIterator;
import com.netease.arctic.flink.read.source.DataIterator;

/**
 * This class contains the file offset and record offset with actual record.
 */
public class RecordPosition {
  private int currentInsertFileOffset;
  private int currentDeleteFileOffset;
  private long currentInsertRecordOffset;
  private long currentDeleteRecordOffset;

  public RecordPosition() {
  }

  void set(DataIterator dataIterator) {
    if (dataIterator instanceof ChangeLogDataIterator) {
      ChangeLogDataIterator changelog = (ChangeLogDataIterator) dataIterator;
      currentInsertFileOffset = changelog.insertFileOffset();
      currentInsertRecordOffset = changelog.insertRecordOffset();
      currentDeleteFileOffset = changelog.deleteFileOffset();
      currentDeleteRecordOffset = changelog.deleteRecordOffset();
    } else {
      currentInsertFileOffset = dataIterator.fileOffset();
      currentInsertRecordOffset = dataIterator.recordOffset();
    }
  }

  public int currentInsertFileOffset() {
    return currentInsertFileOffset;
  }

  public int currentDeleteFileOffset() {
    return currentDeleteFileOffset;
  }

  public long currentInsertRecordOffset() {
    return currentInsertRecordOffset;
  }

  public long currentDeleteRecordOffset() {
    return currentDeleteRecordOffset;
  }
}

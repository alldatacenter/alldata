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

package com.netease.arctic.spark.writer;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.spark.sql.connector.write.WriterCommitMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WriteTaskCommit implements WriterCommitMessage {
  private final DataFile[] dataFiles;
  private final DeleteFile[] deleteFiles;

  WriteTaskCommit(List<DataFile> dataFiles, List<DeleteFile> deleteFiles) {
    this.dataFiles = dataFiles.toArray(new DataFile[0]);
    this.deleteFiles = deleteFiles.toArray(new DeleteFile[0]);
  }

  DataFile[] files() {
    return dataFiles;
  }

  public static Iterable<DataFile> files(WriterCommitMessage[] messages) {
    if (messages.length > 0) {
      return Iterables.concat(Iterables.transform(Arrays.asList(messages), message -> message != null ?
          ImmutableList.copyOf(((WriteTaskCommit) message).files()) :
          ImmutableList.of()));
    }
    return ImmutableList.of();
  }

  DeleteFile[] deleteFiles() {
    return deleteFiles;
  }

  public static Iterable<DeleteFile> deleteFiles(WriterCommitMessage[] messages) {
    if (messages.length > 0) {
      return Iterables.concat(Iterables.transform(Arrays.asList(messages), message -> message != null ?
          ImmutableList.copyOf(((WriteTaskCommit) message).deleteFiles()) :
          ImmutableList.of()));
    }
    return ImmutableList.of();
  }

  public static class Builder {
    private final List<DataFile> dataFiles;
    private final List<DeleteFile> deleteFiles;

    Builder() {
      this.dataFiles = new ArrayList<>();
      this.deleteFiles = new ArrayList<>();
    }

    public Builder add(WriteTaskCommit result) {
      addDataFiles(result.dataFiles);
      addDeleteFiles(result.deleteFiles);
      return this;
    }

    public Builder addAll(Iterable<WriteTaskCommit> results) {
      results.forEach(this::add);
      return this;
    }

    public Builder addDataFiles(DataFile... files) {
      Collections.addAll(dataFiles, files);
      return this;
    }

    public Builder addDataFiles(Iterable<DataFile> files) {
      Iterables.addAll(dataFiles, files);
      return this;
    }

    public Builder addDeleteFiles(DeleteFile... files) {
      Collections.addAll(deleteFiles, files);
      return this;
    }

    public Builder addDeleteFiles(Iterable<DeleteFile> files) {
      Iterables.addAll(deleteFiles, files);
      return this;
    }

    public WriteTaskCommit build() {
      return new WriteTaskCommit(dataFiles, deleteFiles);
    }
  }
}

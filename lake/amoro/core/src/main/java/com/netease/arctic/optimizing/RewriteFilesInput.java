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

package com.netease.arctic.optimizing;

import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RewriteFilesInput extends BaseOptimizingInput {
  private final IcebergDataFile[] rewrittenDataFiles;
  private final IcebergDataFile[] rePosDeletedDataFiles;
  private final IcebergContentFile<?>[] readOnlyDeleteFiles;
  private final IcebergContentFile<?>[] rewrittenDeleteFiles;
  private ArcticTable table;

  public RewriteFilesInput(
      IcebergDataFile[] rewrittenDataFiles,
      IcebergDataFile[] rePosDeletedDataFiles,
      IcebergContentFile<?>[] readOnlyDeleteFiles,
      IcebergContentFile<?>[] rewrittenDeleteFiles,
      ArcticTable table) {
    this.rewrittenDataFiles = rewrittenDataFiles;
    this.rePosDeletedDataFiles = rePosDeletedDataFiles;
    this.readOnlyDeleteFiles = readOnlyDeleteFiles;
    this.rewrittenDeleteFiles = rewrittenDeleteFiles;
    this.table = table;
  }

  public IcebergDataFile[] rewrittenDataFiles() {
    return rewrittenDataFiles;
  }

  public IcebergDataFile[] rePosDeletedDataFiles() {
    return rePosDeletedDataFiles;
  }

  public IcebergContentFile<?>[] readOnlyDeleteFiles() {
    return readOnlyDeleteFiles;
  }

  public IcebergContentFile<?>[] rewrittenDeleteFiles() {
    return rewrittenDeleteFiles;
  }

  public List<PrimaryKeyedFile> rewrittenDataFilesForMixed() {
    if (rewrittenDataFiles == null) {
      return null;
    }
    return Arrays.stream(rewrittenDataFiles)
        .map(s -> (DefaultKeyedFile) s.internalDataFile())
        .collect(Collectors.toList());
  }

  public List<PrimaryKeyedFile> rePosDeletedDataFilesForMixed() {
    if (rePosDeletedDataFiles == null) {
      return null;
    }
    return Arrays.stream(rePosDeletedDataFiles)
        .map(s -> (DefaultKeyedFile) s.internalDataFile())
        .collect(Collectors.toList());
  }

  public List<DeleteFile> positionDeleteForMixed() {
    return Arrays.stream(deleteFiles())
        .filter(s -> s instanceof DeleteFile)
        .map(s -> (IcebergDeleteFile) s)
        .map(IcebergDeleteFile::internalFile)
        .collect(Collectors.toList());
  }

  public List<PrimaryKeyedFile> equalityDeleteForMixed() {
    return Arrays.stream(deleteFiles())
        .filter(s -> s instanceof DataFile)
        .map(IcebergContentFile::internalFile)
        .map(s -> (PrimaryKeyedFile) s)
        .collect(Collectors.toList());
  }

  public IcebergContentFile<?>[] readOnlyDeleteFilesForMixed() {
    return readOnlyDeleteFiles;
  }

  public IcebergContentFile<?>[] rewrittenDeleteFilesForMixed() {
    return rewrittenDeleteFiles;
  }

  public IcebergContentFile<?>[] deleteFiles() {
    List<IcebergContentFile<?>> list = new ArrayList<>();
    if (readOnlyDeleteFiles != null) {
      list.addAll(Arrays.asList(readOnlyDeleteFiles));
    }
    if (rewrittenDeleteFiles != null) {
      list.addAll(Arrays.asList(rewrittenDeleteFiles));
    }
    return list.toArray(new IcebergContentFile<?>[0]);
  }

  public IcebergDataFile[] dataFiles() {
    List<IcebergDataFile> list = new ArrayList<>();
    if (rewrittenDataFiles != null) {
      list.addAll(Arrays.asList(rewrittenDataFiles));
    }
    if (rePosDeletedDataFiles != null) {
      list.addAll(Arrays.asList(rePosDeletedDataFiles));
    }
    return list.toArray(new IcebergDataFile[0]);
  }

  public IcebergContentFile<?>[] allFiles() {
    List<IcebergContentFile<?>> list = new ArrayList<>();
    if (rewrittenDataFiles != null) {
      list.addAll(Arrays.asList(rewrittenDataFiles));
    }
    if (rePosDeletedDataFiles != null) {
      list.addAll(Arrays.asList(rePosDeletedDataFiles));
    }
    if (readOnlyDeleteFiles != null) {
      Arrays.stream(readOnlyDeleteFiles).forEach(list::add);
    }
    if (rewrittenDeleteFiles != null) {
      Arrays.stream(rewrittenDeleteFiles).forEach(list::add);
    }
    return list.toArray(new IcebergContentFile<?>[0]);
  }

  public ArcticTable getTable() {
    return table;
  }

  @Override
  public String toString() {
    return "RewriteFilesInput{" +
        "rewrittenDataFilesSize=" + (rewrittenDataFiles == null ? 0 : rewrittenDataFiles.length) +
        ", rePosDeletedDataFilesSize=" + (rePosDeletedDataFiles == null ? 0 : rePosDeletedDataFiles.length) +
        ", readOnlyDeleteFilesSize=" + (readOnlyDeleteFiles == null ? 0 : readOnlyDeleteFiles.length) +
        ", rewrittenDeleteFilesSize=" + (rewrittenDeleteFiles == null ? 0 : rewrittenDeleteFiles.length) +
        "} " + super.toString();
  }
}

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

package com.netease.arctic.scan;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.data.PrimaryKeyedFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic implementation of {@link ArcticFileScanTask}
 */
public class BasicArcticFileScanTask implements ArcticFileScanTask {

  private final PrimaryKeyedFile baseFile;

  private final List<DeleteFile> posDeleteFiles;

  private final PartitionSpec spec;

  private final Expression expression;

  private FileScanTask fileScanTask;

  public BasicArcticFileScanTask(PrimaryKeyedFile baseFile, List<DeleteFile> posDeleteFiles, PartitionSpec spec) {
    this(baseFile, posDeleteFiles, spec, Expressions.alwaysTrue());
  }

  public BasicArcticFileScanTask(
      PrimaryKeyedFile baseFile, List<DeleteFile> posDeleteFiles, PartitionSpec spec,
      Expression expression) {
    this.baseFile = baseFile;
    this.posDeleteFiles = posDeleteFiles == null ? Collections.emptyList() :
        posDeleteFiles.stream().filter(s -> {
          DataTreeNode node = FileNameRules.parseFileNodeFromFileName(s.path().toString());
          return baseFile.node().isSonOf(node) || baseFile.node().equals(node);
        }).collect(Collectors.toList());
    this.spec = spec;
    this.expression = expression;
  }

  /**
   * Only for iceberg wrap
   * @param fileScanTask
   */
  public BasicArcticFileScanTask(FileScanTask fileScanTask) {
    this(DefaultKeyedFile.parseBase(fileScanTask.file()), fileScanTask.deletes(),
        fileScanTask.spec(), fileScanTask.residual());
    this.fileScanTask = fileScanTask;
  }

  @Override
  public PrimaryKeyedFile file() {
    return baseFile;
  }

  @Override
  public List<DeleteFile> deletes() {
    return posDeleteFiles;
  }

  @Override
  public PartitionSpec spec() {
    return spec;
  }

  @Override
  public long start() {
    if (fileScanTask != null) {
      return fileScanTask.start();
    }
    return 0;
  }

  @Override
  public long length() {
    if (fileScanTask != null) {
      return fileScanTask.length();
    }
    return baseFile.fileSizeInBytes();
  }

  @Override
  public Expression residual() {
    return expression;
  }

  @Override
  public Iterable<FileScanTask> split(long splitSize) {
    return ImmutableList.of(this);
  }
}

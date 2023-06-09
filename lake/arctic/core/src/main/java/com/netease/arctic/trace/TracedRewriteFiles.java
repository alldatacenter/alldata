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

package com.netease.arctic.trace;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.Snapshot;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Wrap {@link RewriteFiles} with {@link TableTracer}.
 */
public class TracedRewriteFiles implements RewriteFiles {
  private final RewriteFiles rewriteFiles;
  private final TableTracer tracer;

  public TracedRewriteFiles(RewriteFiles rewriteFiles, TableTracer tracer) {
    this.rewriteFiles = rewriteFiles;
    this.tracer = tracer;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> filesToDelete, Set<DataFile> filesToAdd) {
    rewriteFiles.rewriteFiles(filesToDelete, filesToAdd);
    filesToAdd.forEach(tracer::addDataFile);
    filesToDelete.forEach(tracer::deleteDataFile);
    return this;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> filesToDelete, Set<DataFile> filesToAdd, long sequenceNumber) {
    rewriteFiles.rewriteFiles(filesToDelete, filesToAdd, sequenceNumber);
    filesToAdd.forEach(tracer::addDataFile);
    filesToDelete.forEach(tracer::deleteDataFile);
    return this;
  }

  @Override
  public RewriteFiles rewriteFiles(Set<DataFile> dataFilesToReplace, Set<DeleteFile> deleteFilesToReplace,
                                   Set<DataFile> dataFilesToAdd, Set<DeleteFile> deleteFilesToAdd) {
    rewriteFiles.rewriteFiles(dataFilesToReplace, deleteFilesToReplace, dataFilesToAdd, deleteFilesToAdd);
    dataFilesToAdd.forEach(tracer::addDataFile);
    dataFilesToReplace.forEach(tracer::deleteDataFile);
    deleteFilesToAdd.forEach(tracer::addDeleteFile);
    deleteFilesToReplace.forEach(tracer::deleteDeleteFile);
    return this;
  }

  @Override
  public RewriteFiles validateFromSnapshot(long snapshotId) {
    rewriteFiles.validateFromSnapshot(snapshotId);
    return this;
  }

  @Override
  public RewriteFiles set(String property, String value) {
    rewriteFiles.set(property, value);
    tracer.setSnapshotSummary(property, value);
    return this;
  }

  @Override
  public RewriteFiles deleteWith(Consumer<String> deleteFunc) {
    rewriteFiles.deleteWith(deleteFunc);
    return this;
  }

  @Override
  public RewriteFiles stageOnly() {
    rewriteFiles.stageOnly();
    return this;
  }

  @Override
  public Snapshot apply() {
    return rewriteFiles.apply();
  }

  @Override
  public void commit() {
    rewriteFiles.commit();
    tracer.commit();
  }

  @Override
  public Object updateEvent() {
    return rewriteFiles.updateEvent();
  }
}

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
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.expressions.Expression;

import java.util.function.Consumer;

/**
 * Wrap {@link DeleteFiles} with {@link TableTracer}.
 */
public class TracedDeleteFiles implements DeleteFiles {

  private final DeleteFiles deleteFiles;
  private final TableTracer tracer;

  public TracedDeleteFiles(DeleteFiles deleteFiles, TableTracer tracer) {
    this.deleteFiles = deleteFiles;
    this.tracer = tracer;
  }

  @Override
  public DeleteFiles deleteFile(CharSequence path) {
    throw new UnsupportedOperationException("this method is not supported");
  }

  @Override
  public DeleteFiles deleteFile(DataFile file) {
    deleteFiles.deleteFile(file);
    tracer.deleteDataFile(file);
    return this;
  }

  @Override
  public DeleteFiles deleteFromRowFilter(Expression expr) {
    deleteFiles.deleteFromRowFilter(expr);
    return this;
  }

  @Override
  public DeleteFiles caseSensitive(boolean caseSensitive) {
    deleteFiles.caseSensitive(caseSensitive);
    return this;
  }

  @Override
  public DeleteFiles set(String property, String value) {
    deleteFiles.set(property, value);
    tracer.setSnapshotSummary(property, value);
    return this;
  }

  @Override
  public DeleteFiles deleteWith(Consumer<String> deleteFunc) {
    deleteFiles.deleteWith(deleteFunc);
    return this;
  }

  @Override
  public DeleteFiles stageOnly() {
    deleteFiles.stageOnly();
    return this;
  }

  @Override
  public Snapshot apply() {
    return deleteFiles.apply();
  }

  @Override
  public void commit() {
    deleteFiles.commit();
    tracer.commit();
  }
}

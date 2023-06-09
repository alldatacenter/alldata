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

import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnResolver.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnResolver.ParseResult;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.StaticBatchBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.StaticBatchBuilder.RepeatedBatchBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.hadoop.fs.Path;

/**
 * Manages implicit columns for files and partition columns for
 * directories. Splits the work into a schema-time implicit column
 * resolver, and a run-time class to populate the resulting
 * vectors.
 * <p>
 * Uses system/session options for the configured name of the
 * implicit columns.
 */
public class ImplicitFileColumnsHandler {

  private final DrillFileSystem dfs;
  private final ImplicitColumnResolver parser;
  private final ResultVectorCache vectorCache;
  private final Path rootDir;
  private final ParseResult parseResult;
  private final boolean isCompressible;

  public ImplicitFileColumnsHandler(DrillFileSystem dfs, OptionSet options,
      FileScanLifecycleBuilder scanOptions,
      ResultVectorCache vectorCache, ScanSchemaTracker schemaTracker) {
    ImplicitColumnOptions implicitOptions = new ImplicitColumnOptions()
        .optionSet(options)
        .dfs(dfs)
        .maxPartitionDepth(scanOptions.maxPartitionDepth())
        .useLegacyWildcardExpansion(scanOptions.useLegacyWildcardExpansion());
    this.dfs = dfs;
    this.rootDir = scanOptions.rootDir();
    this.parser = new ImplicitColumnResolver(implicitOptions, scanOptions.errorContext());
    this.vectorCache = vectorCache;
    this.parseResult = parser.parse(schemaTracker);
    this.isCompressible = scanOptions.isCompressible();
  }

  public FileDescrip makeDescrip(FileWork fileWork) {
    FileDescrip descrip = new FileDescrip(dfs, fileWork, rootDir);
    descrip.setCompressible(isCompressible);
    return descrip;
  }

  public StaticBatchBuilder forFile(FileDescrip fileInfo) {
    List<ImplicitColumnMarker> columns = parseResult.columns();
    if (columns.isEmpty()) {
      return null;
    }
    Object values[] = new Object[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      values[i] = columns.get(i).resolve(fileInfo);
    }
    return new RepeatedBatchBuilder(vectorCache, parseResult.schema(), values);
  }

  public boolean isMetadataScan() { return parseResult.isMetadataScan(); }
}

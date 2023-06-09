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

import java.io.IOException;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ReaderLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ScanLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.SchemaNegotiatorImpl;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.StaticBatchBuilder;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The file scan framework adds into the scan framework support for
 * reading from DFS splits (a file and a block) and for the file-related
 * implicit and partition columns. The file scan builder gathers
 * file-related options for the scan as a whole, including the list
 * of splits. The associated {@link FileSchemaNegotiator} passes
 * file information to each reader.
 * <p>
 * Only a single storage plugin uses the file scan framework:
 * the {@link FileSystemPlugin} via the {@link EasyFormatPlugin}. To
 * make client code as simple as possible, the Drill file system and list
 * of files is passed though this framework to the
 * {@link FileReaderFactory}, then to the {@link FileSchemaNegotiator}
 * which presents them to the reader. This approach avoids the need
 * for each format handle this common boilerplate code.
 * <p>
 * The {@link FileScanOptions} holds the list of splits to scan. The
 * {@link FileReaderFactory} iterates over those splits, and
 * creates each reader just-in-time to process that split.
 * <p>
 * Implicit columns are defined here at the beginning of the scan as
 * part of the scan schema mechanism. Each consists of a column "marker"
 * that identifies the column purposes. Then, on each file, the implicit
 * column is resolved to a value specific to that file. A
 * {@link StaticBatchBuilder} then fills in the needed column values
 * for each batch which the reader produces.
 */
public class FileScanLifecycle extends ScanLifecycle {
  private static final Logger logger = LoggerFactory.getLogger(FileScanLifecycle.class);

  private final DrillFileSystem dfs;
  private final ImplicitFileColumnsHandler implicitColumnsHandler;

  public FileScanLifecycle(OperatorContext context, FileScanLifecycleBuilder options) {
    super(context, options);

    // Create the Drill file system.
    try {
      dfs = context.newFileSystem(options.fileSystemConfig());
    } catch (IOException e) {
      throw UserException.dataReadError(e)
        .addContext("Failed to create FileSystem")
        .addContext(options.errorContext())
        .build(logger);
    }

    // Create the implicit columns manager
    this.implicitColumnsHandler = new ImplicitFileColumnsHandler(
        dfs, context.getFragmentContext().getOptions(),
        options, vectorCache(), schemaTracker());

    // Bind the reader factory which initializes the list
    // of splits from the builder.
    FileReaderFactory readerFactory = (FileReaderFactory) readerFactory();
    readerFactory.bind(this);
  }

  public FileScanLifecycleBuilder fileScanOptions() { return (FileScanLifecycleBuilder) options(); }
  public DrillFileSystem fileSystem() { return dfs; }
  public ImplicitFileColumnsHandler implicitColumnsHandler() { return implicitColumnsHandler; }

  @Override
  protected SchemaNegotiatorImpl newNegotiator(ReaderLifecycle readerLifecycle) {
    return new FileSchemaNegotiatorImpl(readerLifecycle);
  }

  @Override
  public void close() {
    super.close();
    try {
      dfs.close();
    } catch (IOException e) {
      logger.warn("Failed to close the Drill file system", e);
    }
  }
}

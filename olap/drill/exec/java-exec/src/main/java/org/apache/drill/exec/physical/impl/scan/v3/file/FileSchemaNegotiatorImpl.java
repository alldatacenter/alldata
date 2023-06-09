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

import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException.Builder;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.ReaderLifecycle;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.SchemaNegotiatorImpl;
import org.apache.drill.exec.physical.impl.scan.v3.lifecycle.StaticBatchBuilder;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.hadoop.mapred.FileSplit;

/**
 * Implementation of the file-level schema negotiator which holds the
 * file split which the reader is to process. This class presents the
 * split in both Hadoop and Drill formats. Adds the file name to the
 * error context.
 */
public class FileSchemaNegotiatorImpl extends SchemaNegotiatorImpl
    implements FileSchemaNegotiator {

  public class SplitErrorContext extends ChildErrorContext {

    public SplitErrorContext(CustomErrorContext parent) {
      super(parent);
    }

    @Override
    public void addContext(Builder builder) {
      super.addContext(builder);
      FileSplit split = fileDescrip.split();
      builder.addContext("File", split.getPath().toString());
      if (split.getStart() != 0) {
        builder.addContext("Offset", split.getStart());
        builder.addContext("Length", split.getLength());
      }
    }
  }

  private FileDescrip fileDescrip;

  public FileSchemaNegotiatorImpl(ReaderLifecycle readerLifecycle) {
    super(readerLifecycle);
    baseErrorContext = new SplitErrorContext(baseErrorContext);
    readerErrorContext = baseErrorContext;
  }

  public void bindSplit(FileWork fileWork) {
    fileDescrip = fileScan().implicitColumnsHandler().makeDescrip(fileWork);
  }

  @Override
  public FileDescrip file() { return fileDescrip; }

  @Override
  @SuppressWarnings("unchecked")
  public ManagedReader newReader(ReaderFactory<?> readerFactory) throws EarlyEofException {
    return ((ReaderFactory<FileSchemaNegotiator>) readerFactory).next(this);
  }

  @Override
  public StaticBatchBuilder implicitColumnsLoader() {
    return fileScan().implicitColumnsHandler().forFile(fileDescrip);
  }

  private FileScanLifecycle fileScan() {
    return (FileScanLifecycle) readerLifecycle.scanLifecycle();
  }

  @Override
  protected void onEndBatch() {

    // If this is is a metadata scan, and this file has no rows (this is
    // the first batch and contains no data), then add a dummy row so
    // we have something to aggregate upon.
    ImplicitFileColumnsHandler handler = fileScan().implicitColumnsHandler();
    if (!handler.isMetadataScan()) {
      return;
    }
    ResultSetLoader tableLoader = readerLifecycle.tableLoader();
    if (tableLoader.batchCount() == 0 && !tableLoader.hasRows()) {

      // This is admittedly a hack. The table may contain non-nullable
      // columns, but we are asking for null values for those columns.
      // We'll fill in defaults, with is not ideal.
      tableLoader.writer().start();
      tableLoader.writer().save();
      fileDescrip.markEmpty();
    }
  }
}

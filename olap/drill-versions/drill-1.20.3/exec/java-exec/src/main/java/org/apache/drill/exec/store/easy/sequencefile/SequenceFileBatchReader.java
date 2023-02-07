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
package org.apache.drill.exec.store.easy.sequencefile;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileAsBinaryInputFormat;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceFileBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(SequenceFileBatchReader.class);

  private final SequenceFileFormatConfig config;
  private final EasySubScan scan;
  private FileSplit split;
  private String queryUserName;
  private String opUserName;
  public static final String KEY_SCHEMA = "binary_key";
  public static final String VALUE_SCHEMA = "binary_value";
  private final BytesWritable key = new BytesWritable();
  private final BytesWritable value = new BytesWritable();
  private final int maxRecords;
  private RowSetLoader loader;
  private ScalarWriter keyWriter;
  private ScalarWriter valueWriter;
  private RecordReader<BytesWritable, BytesWritable> reader;
  private CustomErrorContext errorContext;
  private Stopwatch watch;

  public SequenceFileBatchReader(SequenceFileFormatConfig config, EasySubScan scan) {
    this.config = config;
    this.scan = scan;
    this.maxRecords = scan.getMaxRecords();
  }

  private TupleMetadata defineMetadata() {
    SchemaBuilder builder = new SchemaBuilder();
    builder.addNullable(KEY_SCHEMA, MinorType.VARBINARY);
    builder.addNullable(VALUE_SCHEMA, MinorType.VARBINARY);
    return builder.buildSchema();
  }

  private void processReader(FileSchemaNegotiator negotiator) throws ExecutionSetupException {
    final SequenceFileAsBinaryInputFormat inputFormat = new SequenceFileAsBinaryInputFormat();
    split = negotiator.split();
    // After defined the split, We should also define the errorContext.
    errorContext = negotiator.parentErrorContext();
    opUserName = scan.getUserName();
    queryUserName = negotiator.context().getFragmentContext().getQueryUserName();
    final JobConf jobConf = new JobConf(negotiator.fileSystem().getConf());
    jobConf.setInputFormat(inputFormat.getClass());
    reader = getRecordReader(inputFormat, jobConf);
  }

  private RecordReader<BytesWritable, BytesWritable> getRecordReader(
    final InputFormat<BytesWritable, BytesWritable> inputFormat, final JobConf jobConf)
    throws ExecutionSetupException {
    try {
      final UserGroupInformation ugi = ImpersonationUtil.createProxyUgi(opUserName, queryUserName);
      return ugi.doAs(new PrivilegedExceptionAction<RecordReader<BytesWritable, BytesWritable>>() {
        @Override
        public RecordReader<BytesWritable, BytesWritable> run() throws Exception {
          return inputFormat.getRecordReader(split, jobConf, Reporter.NULL);
        }
      });
    } catch (IOException | InterruptedException e) {
      throw UserException
              .dataReadError(e)
              .message("Error in creating sequencefile reader for file: %s, start: %d, length: %d. "
               + e.getMessage(), split.getPath(), split.getStart(), split.getLength())
              .addContext(errorContext)
              .build(logger);
    }
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    negotiator.tableSchema(defineMetadata(), true);
    logger.debug("The config is {}, root is {}, columns has {}", config, scan.getSelectionRoot(), scan.getColumns());
    // open Sequencefile
    try {
      processReader(negotiator);
    } catch (ExecutionSetupException e) {
      throw UserException
        .dataReadError(e)
        .message("Failure in initial sequencefile reader. " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
    ResultSetLoader setLoader = negotiator.build();
    loader = setLoader.writer();
    keyWriter = loader.scalar(KEY_SCHEMA);
    valueWriter = loader.scalar(VALUE_SCHEMA);
    return true;
  }

  @Override
  public boolean next() {
    int recordCount = 0;
    if (watch == null) {
      watch = Stopwatch.createStarted();
    }
    try {
      while (!loader.isFull()) {
        if (reader.next(key, value)) {
          loader.start();
          keyWriter.setBytes(key.getBytes(), key.getLength());
          valueWriter.setBytes(value.getBytes(), value.getLength());
          loader.save();
          ++ recordCount;
        } else {
          logger.debug("Read {} records in {} ms", recordCount, watch.elapsed(TimeUnit.MILLISECONDS));
          return false;
        }
        if (loader.limitReached(maxRecords)) {
          return false;
        }
      }
    } catch (IOException e) {
      throw UserException
              .dataReadError(e)
              .message("An error occurred while reading the next key/value pair from the sequencefile reader. "
               + e.getMessage())
              .addContext(errorContext)
              .build(logger);
    }
    return true;
  }

  @Override
  public void close() {
    try {
      // Hadoop 2 compat: {@link org.apache.hadoop.mapred.RecordReader} does not
      // support AutoCloseable and must be closed manually.
      if (reader != null) {
        reader.close();
        reader = null;
      }
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Error closing sequencefile reader: " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
  }
}

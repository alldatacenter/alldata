/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.hadoop.sink;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.connector.hadoop.util.HdfsUtils;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.legacy.connector.Pluggable;

import com.github.rholder.retry.RetryException;
import org.apache.flink.api.common.io.CleanupWhenUnsuccessful;
import org.apache.flink.api.common.io.FileOutputFormat;
import org.apache.flink.api.common.io.InitializeOnMaster;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.fs.SafetyNetWrapperFileSystem;
import org.apache.flink.runtime.fs.hdfs.HadoopFileSystem;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @desc:
 */
public abstract class FileOutputFormatPlugin<E extends Row> extends OutputFormatPlugin<E> implements
    InitializeOnMaster, CleanupWhenUnsuccessful, Pluggable {
  /**
   * The key under which the name of the target path is stored in the configuration.
   */
  public static final String FILE_PARAMETER_KEY = "flink.output.file";

  // --------------------------------------------------------------------------------------------
  /**
   * The LOG for logging messages in this class.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(FileOutputFormat.class);

  // --------------------------------------------------------------------------------------------
  private static final long serialVersionUID = 1L;
  private static WriteMode DEFAULT_WRITE_MODE;
  private static OutputDirectoryMode DEFAULT_OUTPUT_DIRECTORY_MODE;

  static {
    initDefaultsFromConfiguration(GlobalConfiguration.loadConfiguration());
  }

  // --------------------------------------------------------------------------------------------

  /**
   * The path of the file to be written.
   */
  protected Path outputFilePath;
  /**
   * The write mode of the output.
   */
  protected WriteMode writeMode;
  /**
   * The output directory mode
   */
  protected OutputDirectoryMode outputDirectoryMode;
  protected List<ColumnInfo> columns;
  /**
   * The path that is actually written to (may a a file in a the directory defined by {@code outputFilePath} )
   */
  protected transient Path actualFilePath;
  /* Move file to the path if finished */
  protected transient Path finishedFilePath;

  // --------------------------------------------------------------------------------------------
  /**
   * Flag indicating whether this format actually created a file, which should be removed on cleanup.
   */
  protected transient boolean fileCreated;
  /**
   * taskNumber
   */
  protected int taskNumber = -1;

  public FileOutputFormatPlugin() {
  }

  public FileOutputFormatPlugin(Path outputPath) {
    this.outputFilePath = outputPath;
  }

  /**
   * Initialize defaults for output format. Needs to be a static method because it is configured for local
   * cluster execution, see LocalFlinkMiniCluster.
   *
   * @param configuration The configuration to load defaults from
   */
  private static void initDefaultsFromConfiguration(Configuration configuration) {
    final boolean overwrite = configuration.getBoolean(
        ConfigConstants.FILESYSTEM_DEFAULT_OVERWRITE_KEY,
        ConfigConstants.DEFAULT_FILESYSTEM_OVERWRITE);

    DEFAULT_WRITE_MODE = overwrite ? WriteMode.OVERWRITE : WriteMode.NO_OVERWRITE;

    final boolean alwaysCreateDirectory = configuration.getBoolean(
        ConfigConstants.FILESYSTEM_OUTPUT_ALWAYS_CREATE_DIRECTORY_KEY,
        ConfigConstants.DEFAULT_FILESYSTEM_ALWAYS_CREATE_DIRECTORY);

    DEFAULT_OUTPUT_DIRECTORY_MODE = alwaysCreateDirectory ? OutputDirectoryMode.ALWAYS : OutputDirectoryMode.PARONLY;
  }

  private static int getBuffSize(FileSystem fileSystem, int defaultBufferSize) {
    if (fileSystem instanceof SafetyNetWrapperFileSystem) {
      fileSystem = ((SafetyNetWrapperFileSystem) fileSystem).getWrappedDelegate();
    }
    if (fileSystem instanceof HadoopFileSystem) {
      return ((HadoopFileSystem) fileSystem).getHadoopFileSystem()
          .getConf().getInt("io.file.buffer.size", defaultBufferSize);
    }
    return defaultBufferSize;
  }

  public Path getOutputFilePath() {
    return this.outputFilePath;
  }

  public void setOutputFilePath(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Output file path may not be null.");
    }

    this.outputFilePath = path;
  }

  public WriteMode getWriteMode() {
    return this.writeMode;
  }

  public void setWriteMode(WriteMode mode) {
    if (mode == null) {
      throw new NullPointerException();
    }

    this.writeMode = mode;
  }

  public OutputDirectoryMode getOutputDirectoryMode() {
    return this.outputDirectoryMode;
  }

  public void setOutputDirectoryMode(OutputDirectoryMode mode) {
    if (mode == null) {
      throw new NullPointerException();
    }

    this.outputDirectoryMode = mode;
  }


  // ----------------------------------------------------------------

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);

    // get the output file path, if it was not yet set
    if (this.outputFilePath == null) {
      // get the file parameter
      String filePath = parameters.getString(FILE_PARAMETER_KEY, null);
      if (filePath == null) {
        throw new IllegalArgumentException("The output path has been specified neither via constructor/setters" +
            ", nor via the Configuration.");
      }

      try {
        this.outputFilePath = new Path(filePath);
      } catch (RuntimeException rex) {
        throw new RuntimeException("Could not create a valid URI from the given file path name: " + rex.getMessage());
      }
    }

    // check if have not been set and use the defaults in that case
    if (this.writeMode == null) {
      this.writeMode = DEFAULT_WRITE_MODE;
    }

    if (this.outputDirectoryMode == null) {
      this.outputDirectoryMode = DEFAULT_OUTPUT_DIRECTORY_MODE;
    }
  }

  @Override
  public void tryCleanupOnError() {
    this.fileCreated = false;
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    this.taskNumber = taskNumber;
  }

  public void removeAttemptFolder() throws ExecutionException, RetryException, IOException {
    HdfsUtils.deleteIfExist(getAttemptFolder());
  }

  /**
   * The attempt folder which task write in first, after task finished, it will be renamed to middle folder.
   */
  public abstract Path getAttemptFolder();

  /**
   * The attempt file name.
   */
  public abstract String getFileName();

  public abstract Path getCommitFolder();

  public Path getAttemptFileName(int taskNumber, int attemptNumber) {
    return getAttemptFolder().suffix(getFileName() + "-" + taskNumber + "-" + attemptNumber);
  }

  public Path getAttemptFileName() {
    return getAttemptFileName(taskNumber, getRuntimeContext().getAttemptNumber());
  }

  public Path getCommitFileName() {
    return getCommitFileName(taskNumber);
  }

  public Path getCommitFileName(int taskNumber) {
    return getCommitFolder().suffix(getFileName() + "-" + taskNumber);
  }

  public void commitAttempt(int taskNumber, int attemptNumber) throws Exception {
    Path src = getAttemptFileName(taskNumber, attemptNumber);
    Path dst = getCommitFileName(taskNumber);
    HdfsUtils.renameIfNotExist(src, dst);
  }

  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    super.onSuccessComplete(result);
    removeAttemptFolder();
  }

  @Override
  public void initPlugin() throws Exception {
    setColumnsFromConf();

    String writeMode = outputSliceConfig.get(WriterOptions.BaseWriterOptions.WRITE_MODE);
    setWriteMode(WriteMode.valueOf(writeMode.toUpperCase()));
  }

  @Override
  public int getMaxParallelism() {
    return Integer.MAX_VALUE;
  }

  private void setColumnsFromConf() {
    this.columns = outputSliceConfig.get(WriterOptions.BaseWriterOptions.COLUMNS);
  }

  /**
   * Behavior for creating output directories.
   */
  public enum OutputDirectoryMode {

    /**
     * A directory is always created, regardless of number of write tasks.
     */
    ALWAYS,

    /**
     * A directory is only created for parallel output tasks, i.e., number of output tasks &gt; 1.
     * If number of output tasks = 1, the output is written to a single file.
     */
    PARONLY
  }
}

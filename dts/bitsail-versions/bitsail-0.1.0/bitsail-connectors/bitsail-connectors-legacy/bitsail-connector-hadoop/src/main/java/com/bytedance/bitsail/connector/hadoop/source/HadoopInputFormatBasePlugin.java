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
 */

package com.bytedance.bitsail.connector.hadoop.source;

import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.hadoop.option.HadoopReaderOptions;
import com.bytedance.bitsail.connector.hadoop.split.OptimizedHadoopInputSplit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.io.FileInputFormat.FileBaseStatistics;
import org.apache.flink.api.common.io.LocatableInputSplitAssigner;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.java.hadoop.mapred.utils.HadoopUtils;
import org.apache.flink.api.java.hadoop.mapred.wrapper.HadoopDummyReporter;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @desc: A common base for both "mapred" and "mapreduce" Hadoop input formats.
 */
@Slf4j
public abstract class HadoopInputFormatBasePlugin<K, V, T extends Row> extends
    HadoopInputFormatCommonBasePlugin<T, OptimizedHadoopInputSplit> {
  private static final long serialVersionUID = 1L;

  private static final String OPTION_LIST_STATUS_NUM_THREADS = "mapreduce.input.fileinputformat.list-status.num-threads";
  private static final String OPTION_SPLIT_NUM_THREADS = "mapreduce.input.fileinputformat.split.num-threads";

  private static final String DEFAULT_LIST_STATUS_NUM_THREADS = "5";
  private static final String DEFAULT_SPLIT_NUM_THREADS = "8";

  // Mutexes to avoid concurrent operations on Hadoop InputFormats.
  // Hadoop parallelizes tasks across JVMs which is why they might rely on this JVM isolation.
  // In contrast, Flink parallelizes using Threads, so multiple Hadoop InputFormat instances
  // might be used in the same JVM.
  private static final Object OPEN_MUTEX = new Object();
  private static final Object CONFIGURE_MUTEX = new Object();
  private static final Object CLOSE_MUTEX = new Object();

  protected org.apache.hadoop.mapred.InputFormat<K, V> mapredInputFormat;
  protected JobConf jobConf;

  protected transient K key;
  protected transient V value;

  protected transient RecordReader<K, V> recordReader;

  protected transient BaseStatistics cachedStatistics;

  private transient InputSplit inputSplit;

  public HadoopInputFormatBasePlugin(JobConf job) {
    super(job.getCredentials());
    jobConf = job;
  }

  public HadoopInputFormatBasePlugin(org.apache.hadoop.mapred.InputFormat<K, V> mapredInputFormat, Class<K> key, Class<V> value, JobConf job) {
    super(job.getCredentials());
    this.mapredInputFormat = mapredInputFormat;
    HadoopUtils.mergeHadoopConf(job);
    this.jobConf = job;
    ReflectionUtils.setConf(mapredInputFormat, jobConf);
  }

  public JobConf getJobConf() {
    return jobConf;
  }

  // --------------------------------------------------------------------------------------------
  //  InputFormat
  // --------------------------------------------------------------------------------------------

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
    // enforce sequential configuration() calls
    synchronized (CONFIGURE_MUTEX) {
      // configure MR InputFormat if necessary
      if (this.mapredInputFormat instanceof Configurable) {
        ((Configurable) this.mapredInputFormat).setConf(this.jobConf);
      } else if (this.mapredInputFormat instanceof JobConfigurable) {
        ((JobConfigurable) this.mapredInputFormat).configure(this.jobConf);
      }
    }
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics cachedStats) {
    if (this.cachedStatistics != null) {
      return cachedStatistics;
    }

    // only gather base statistics for FileInputFormats
    if (!(mapredInputFormat instanceof FileInputFormat)) {
      return null;
    }

    final FileBaseStatistics cachedFileStats = (cachedStats instanceof FileBaseStatistics) ?
        (FileBaseStatistics) cachedStats : null;

    try {
      final org.apache.hadoop.fs.Path[] paths = FileInputFormat.getInputPaths(this.jobConf);

      org.apache.hadoop.fs.Path[] actualHadoopPaths = getHadoopParsedPaths(paths).toArray(new org.apache.hadoop.fs.Path[0]);

      this.cachedStatistics = getFileStats(cachedFileStats, actualHadoopPaths, new ArrayList<>(1));
      return this.cachedStatistics;
    } catch (IOException ioex) {
      if (log.isWarnEnabled()) {
        log.warn("Could not determine statistics due to an io error: "
            + ioex.getMessage());
      }
    } catch (Throwable t) {
      if (log.isErrorEnabled()) {
        log.error("Unexpected problem while getting the file statistics: "
            + t.getMessage(), t);
      }
    }

    // no statistics available
    return null;
  }

  @Override
  public OptimizedHadoopInputSplit[] createSplits(int minNumSplits)
      throws IOException {
    org.apache.hadoop.mapred.InputSplit[] splitArray = mapredInputFormat.getSplits(jobConf, minNumSplits);
    OptimizedHadoopInputSplit[] hiSplit = new OptimizedHadoopInputSplit[splitArray.length];
    for (int i = 0; i < splitArray.length; i++) {
      hiSplit[i] = new OptimizedHadoopInputSplit(i, splitArray[i]);
    }
    return hiSplit;
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(OptimizedHadoopInputSplit[] inputSplits) {
    Arrays.stream(inputSplits).forEach(split -> split.initInputSplit(jobConf));
    return new LocatableInputSplitAssigner(inputSplits);
  }

  @Override
  public void open(OptimizedHadoopInputSplit split) throws IOException {
    split.initInputSplit(jobConf);

    // enforce sequential open() calls
    synchronized (OPEN_MUTEX) {

      this.recordReader = this.mapredInputFormat.getRecordReader(split.getHadoopInputSplit(), jobConf, new HadoopDummyReporter());
      this.inputSplit = split.getHadoopInputSplit();
      if (this.recordReader instanceof Configurable) {
        ((Configurable) this.recordReader).setConf(jobConf);
      }
      key = this.recordReader.createKey();
      value = this.recordReader.createValue();
    }

    log.info("Start to process split number: {}", split.getSplitNumber());

    log.info("split info: {}", inputSplit);

    // skip several lines
    if (inputSplit instanceof FileSplit) {
      // check the split start position
      long splitStart = ((FileSplit) inputSplit).getStart();
      long skipLines = 0;

      if (inputSliceConfig != null) {
        skipLines = inputSliceConfig.get(HadoopReaderOptions.SKIP_LINES);
        log.info("hadoop file system skip line size: {}", skipLines);
      }

      // skip the first line, if we are at the beginning of a file and have the option set
      if (skipLines > 0 && splitStart == 0) {
        for (int i = 0; i < skipLines; i++) {
          hasNext = this.recordReader.next(key, value);
        }
      }
    }
  }

  @Override
  public boolean isSplitEnd() throws IOException {
    try {
      hasNext = this.recordReader.next(key, value);
      return !hasNext;
    } catch (Exception e) {
      throw new RuntimeException("Error while processing input split: " + inputSplit, e);
    }
  }

  @Override
  public void close() throws IOException {
    if (this.recordReader != null) {

      // enforce sequential close() calls
      synchronized (CLOSE_MUTEX) {
        this.recordReader.close();
      }
    }
  }

  // --------------------------------------------------------------------------------------------
  //  Helper methods
  // --------------------------------------------------------------------------------------------

  private FileBaseStatistics getFileStats(FileBaseStatistics cachedStats, org.apache.hadoop.fs.Path[] hadoopFilePaths,
                                          ArrayList<FileStatus> files) throws IOException {

    long latestModTime = 0L;

    // get the file info and check whether the cached statistics are still valid.
    for (org.apache.hadoop.fs.Path hadoopPath : hadoopFilePaths) {

      final Path filePath = new Path(hadoopPath.toUri());
      final FileSystem fs = FileSystem.get(filePath.toUri());

      final FileStatus file = fs.getFileStatus(filePath);
      latestModTime = Math.max(latestModTime, file.getModificationTime());

      // enumerate all files and check their modification time stamp.
      if (file.isDir()) {
        FileStatus[] fss = fs.listStatus(filePath);
        files.ensureCapacity(files.size() + fss.length);

        for (FileStatus s : fss) {
          if (!s.isDir()) {
            files.add(s);
            latestModTime = Math.max(s.getModificationTime(), latestModTime);
          }
        }
      } else {
        files.add(file);
      }
    }

    // check whether the cached statistics are still valid, if we have any
    if (cachedStats != null && latestModTime <= cachedStats.getLastModificationTime()) {
      return cachedStats;
    }

    // calculate the whole length
    long len = 0;
    for (FileStatus s : files) {
      len += s.getLen();
    }

    // sanity check
    if (len <= 0) {
      len = BaseStatistics.SIZE_UNKNOWN;
    }

    return new FileBaseStatistics(latestModTime, len, BaseStatistics.AVG_RECORD_BYTES_UNKNOWN);
  }

  private List<org.apache.hadoop.fs.Path> getHadoopParsedPaths(org.apache.hadoop.fs.Path[] hadoopFilePaths) throws IOException {
    List<org.apache.hadoop.fs.Path> actualHadoopPaths = new ArrayList<>();
    for (org.apache.hadoop.fs.Path hadoopPath : hadoopFilePaths) {
      org.apache.hadoop.fs.FileStatus[] hadoopFileStatuses = hadoopPath.getFileSystem(jobConf).globStatus(hadoopPath, path -> {
        String name = path.getName();
        return !name.startsWith("_") && !name.startsWith(".");
      });
      for (org.apache.hadoop.fs.FileStatus hadoopFileStatus : hadoopFileStatuses) {
        actualHadoopPaths.add(hadoopFileStatus.getPath());
      }
    }
    return actualHadoopPaths;
  }

  protected void setMapredInputJobConf() {
    String hadoopJobConf = inputSliceConfig.get(HadoopReaderOptions.HADOOP_CONF);
    jobConf = setHadoopJobConf(jobConf, hadoopJobConf);

    HadoopUtils.mergeHadoopConf(jobConf);
    ReflectionUtils.setConf(mapredInputFormat, jobConf);
  }

  public JobConf setHadoopJobConf(JobConf jobConf, String hadoopJobConf) {
    boolean userDefinedListStatusThreadsNum = false;
    boolean userDefinedSplitThreadsNum = false;

    if (!StringUtils.isBlank(hadoopJobConf)) {
      Map<String, String> hadoopProperties = JsonSerializer.parseToMap(hadoopJobConf);
      for (Map.Entry<String, String> entry : hadoopProperties.entrySet()) {
        final String key = StringUtils.trim(entry.getKey());
        final String value = StringUtils.trim(entry.getValue());
        jobConf.set(key, value);
        if (StringUtils.equalsIgnoreCase(key, OPTION_LIST_STATUS_NUM_THREADS)) {
          userDefinedListStatusThreadsNum = true;
        }
        if (StringUtils.equalsIgnoreCase(key, OPTION_SPLIT_NUM_THREADS)) {
          userDefinedSplitThreadsNum = true;

        }
      }
    }

    if (!userDefinedListStatusThreadsNum) {
      jobConf.set(OPTION_LIST_STATUS_NUM_THREADS, DEFAULT_LIST_STATUS_NUM_THREADS);    // this setting has default value "1" in jobConf
      log.info("Hadoop job conf {} is set to default value {}!", OPTION_LIST_STATUS_NUM_THREADS, DEFAULT_LIST_STATUS_NUM_THREADS);
    } else {
      log.info("Hadoop job conf {} is set to {}!", OPTION_LIST_STATUS_NUM_THREADS, jobConf.getInt(OPTION_LIST_STATUS_NUM_THREADS, -1));
    }

    if (!userDefinedSplitThreadsNum) {
      jobConf.set(OPTION_SPLIT_NUM_THREADS, DEFAULT_SPLIT_NUM_THREADS);
      log.info("Hadoop job conf {} is set to default value {}!", OPTION_SPLIT_NUM_THREADS, DEFAULT_SPLIT_NUM_THREADS);
    } else {
      log.info("Hadoop job conf {} is set to {}", OPTION_SPLIT_NUM_THREADS, jobConf.getInt(OPTION_SPLIT_NUM_THREADS, -1));
    }

    return jobConf;
  }

  // --------------------------------------------------------------------------------------------
  //  Custom serialization methods
  // --------------------------------------------------------------------------------------------

  private void writeObject(ObjectOutputStream out) throws IOException {
    super.write(out);
    out.writeUTF(mapredInputFormat.getClass().getName());
    jobConf.write(out);
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    super.read(in);

    String hadoopInputFormatClassName = in.readUTF();
    if (jobConf == null) {
      jobConf = new JobConf();
    }
    jobConf.readFields(in);
    try {
      this.mapredInputFormat = (org.apache.hadoop.mapred.InputFormat<K, V>) Class.forName(
          hadoopInputFormatClassName, true, Thread.currentThread().getContextClassLoader()).newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Unable to instantiate the hadoop input format", e);
    }
    ReflectionUtils.setConf(mapredInputFormat, jobConf);

    jobConf.getCredentials().addAll(this.credentials);
    Credentials currentUserCreds = getCredentialsFromUGI(UserGroupInformation.getCurrentUser());
    if (currentUserCreds != null) {
      jobConf.getCredentials().addAll(currentUserCreds);
    }
  }

  public boolean getHasNext() {
    return hasNext;
  }

  public void setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
  }

}

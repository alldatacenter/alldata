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

package org.apache.hadoop.mapreduce.task.reduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.IFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Merger;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapreduce.CryptoUtils;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ReflectionUtils;


public class RssInMemoryRemoteMerger<K, V> extends MergeThread<InMemoryMapOutput<K,V>, K, V> {
  private static final Log LOG = LogFactory.getLog(RssInMemoryRemoteMerger.class);

  private static final String SPILL_OUTPUT_PREFIX = "spill";
  private final RssRemoteMergeManagerImpl<K, V> manager;
  private final JobConf jobConf;
  private final FileSystem remoteFs;
  private final Path spillPath;
  private final String taskAttemptId;
  private final CompressionCodec codec;
  private final Progressable reporter;
  private final Counters.Counter spilledRecordsCounter;
  private final Class<? extends Reducer> combinerClass;
  private final Task.CombineOutputCollector<K,V> combineCollector;
  private final Counters.Counter reduceCombineInputCounter;
  private final Counters.Counter mergedMapOutputsCounter;

  public RssInMemoryRemoteMerger(
      RssRemoteMergeManagerImpl<K, V> manager,
      JobConf jobConf,
      FileSystem remoteFs,
      Path spillPath,
      String taskId,
      CompressionCodec codec,
      Progressable reporter,
      Counters.Counter spilledRecordsCounter,
      Class<? extends Reducer> combinerClass,
      ExceptionReporter exceptionReporter,
      Task.CombineOutputCollector<K,V> combineCollector,
      Counters.Counter reduceCombineInputCounter,
      Counters.Counter mergedMapOutputsCounter) {
    super(manager, Integer.MAX_VALUE, exceptionReporter);
    this.setName("RssInMemoryMerger - Thread to merge in-memory map-outputs");
    this.setDaemon(true);
    this.manager = manager;
    this.jobConf = jobConf;
    this.remoteFs = remoteFs;
    this.spillPath = spillPath;
    this.taskAttemptId = taskId;
    this.codec = codec;
    this.reporter = reporter;
    this.spilledRecordsCounter = spilledRecordsCounter;
    this.combinerClass = combinerClass;
    this.combineCollector = combineCollector;
    this.reduceCombineInputCounter = reduceCombineInputCounter;
    this.mergedMapOutputsCounter = mergedMapOutputsCounter;
  }

  @Override
  public void merge(List<InMemoryMapOutput<K, V>> inputs) throws IOException {
    if (inputs == null || inputs.size() == 0) {
      return;
    }

    long start = System.currentTimeMillis();
    TaskAttemptID mapId = inputs.get(0).getMapId();

    List<Merger.Segment<K, V>> inMemorySegments = new ArrayList<Merger.Segment<K, V>>();
    createInMemorySegments(inputs, inMemorySegments);
    int noInMemorySegments = inMemorySegments.size();

    String filePath = SPILL_OUTPUT_PREFIX + Path.SEPARATOR + taskAttemptId + Path.SEPARATOR + mapId;
    Path outputPath = new Path(spillPath, filePath);

    FSDataOutputStream out = CryptoUtils.wrapIfNecessary(jobConf, remoteFs.create(outputPath));
    IFile.Writer<K, V> writer = new IFile.Writer<K, V>(jobConf, out,
        (Class<K>) jobConf.getMapOutputKeyClass(),
        (Class<V>) jobConf.getMapOutputValueClass(), codec, null, true);

    RawKeyValueIterator rIter = null;
    try {
      LOG.info("Initiating in-memory merge with " + noInMemorySegments + " segments...");

      // tmpDir won't be used. tmpDir is used for onDiskMerger
      rIter = Merger.merge(jobConf, remoteFs,
          (Class<K>)jobConf.getMapOutputKeyClass(),
          (Class<V>)jobConf.getMapOutputValueClass(),
          inMemorySegments, inMemorySegments.size(),
          new Path(taskAttemptId),
          (RawComparator<K>)jobConf.getOutputKeyComparator(),
          reporter, spilledRecordsCounter, null, null);

      if (null == combinerClass) {
        Merger.writeFile(rIter, writer, reporter, jobConf);
      } else {
        combineCollector.setWriter(writer);
        combineAndSpill(rIter, reduceCombineInputCounter);
      }
      writer.close();

      // keep this for final merge
      manager.closeOnHDFSFile(outputPath);

      LOG.info(taskAttemptId + " Merge of the " + noInMemorySegments
          + " files in-memory complete."
          + " Local file is " + outputPath + " of size "
          + remoteFs.getFileStatus(outputPath).getLen()
          + " cost time " + (System.currentTimeMillis() - start) + " ms");
    } catch (IOException e) {
      // make sure that we delete the ondisk file that we created
      // earlier when we invoked cloneFileAttributes
      remoteFs.delete(outputPath, true);
      throw e;
    }

  }

  private void combineAndSpill(
      RawKeyValueIterator kvIter,
      Counters.Counter inCounter) throws IOException {
    JobConf job = jobConf;
    Reducer combiner = ReflectionUtils.newInstance(combinerClass, job);
    Class<K> keyClass = (Class<K>) job.getMapOutputKeyClass();
    Class<V> valClass = (Class<V>) job.getMapOutputValueClass();
    RawComparator<K> comparator =
        (RawComparator<K>) job.getCombinerKeyGroupingComparator();
    try {
      Task.CombineValuesIterator values = new Task.CombineValuesIterator(
          kvIter, comparator, keyClass, valClass, job, Reporter.NULL,
          inCounter);
      while (values.more()) {
        combiner.reduce(values.getKey(), values, combineCollector,
            Reporter.NULL);
        values.nextKey();
      }
    } finally {
      combiner.close();
    }
  }

  private long createInMemorySegments(
      List<InMemoryMapOutput<K,V>> inMemoryMapOutputs,
      List<Merger.Segment<K, V>> inMemorySegments) throws IOException {
    long totalSize = 0L;
    // We could use fullSize could come from the RamManager, but files can be
    // closed but not yet present in inMemoryMapOutputs
    long fullSize = 0L;
    for (InMemoryMapOutput<K,V> mo : inMemoryMapOutputs) {
      fullSize += mo.getMemory().length;
    }
    while (fullSize > 0) {
      InMemoryMapOutput<K,V> mo = inMemoryMapOutputs.remove(0);
      byte[] data = mo.getMemory();
      long size = data.length;
      totalSize += size;
      fullSize -= size;
      IFile.Reader<K,V> reader = new InMemoryReader<K,V>(manager,
          mo.getMapId(), data, 0, (int)size, jobConf);
      inMemorySegments.add(new Merger.Segment<K,V>(reader, true, mergedMapOutputsCounter));
    }
    return totalSize;
  }
}

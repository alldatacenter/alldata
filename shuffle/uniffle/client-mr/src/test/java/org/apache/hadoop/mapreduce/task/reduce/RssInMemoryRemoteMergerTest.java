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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.IFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MROutputFiles;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.CryptoUtils;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.util.Progress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RssInMemoryRemoteMergerTest {

  @Test
  public void mergerTest(@TempDir File tmpDir) throws IOException {
    JobConf jobConf = new JobConf();
    FileSystem fs = FileSystem.getLocal(jobConf);
    LocalDirAllocator lda = new LocalDirAllocator(MRConfig.LOCAL_DIR);
    JobID jobId = new JobID("a", 0);
    final TaskAttemptID mapId1 = new TaskAttemptID(
        new TaskID(jobId, TaskType.MAP, 1), 0);
    final TaskAttemptID mapId2 = new TaskAttemptID(
        new TaskID(jobId, TaskType.MAP, 2), 0);
    TaskAttemptID reduceId1 = new TaskAttemptID(
        new TaskID(jobId, TaskType.REDUCE, 0), 0);
    final RssRemoteMergeManagerImpl<Text, Text> mergeManager = new RssRemoteMergeManagerImpl<Text, Text>(
        "app", reduceId1, jobConf, tmpDir.toString(),  1,5,  fs, lda, Reporter.NULL,
        null, null, null, null, null,
        null, null, new Progress(), new MROutputFiles(), new JobConf());

    // write map outputs
    Map<String, String> map1 = new TreeMap<String, String>();
    map1.put("apple", "disgusting");
    map1.put("carrot", "delicious");
    Map<String, String> map2 = new TreeMap<String, String>();
    map2.put("banana", "pretty good");
    byte[] mapOutputBytes1 = writeMapOutput(jobConf, map1);
    byte[] mapOutputBytes2 = writeMapOutput(jobConf, map2);
    InMemoryMapOutput<Text, Text> mapOutput1 = new InMemoryMapOutput<Text, Text>(
        jobConf, mapId1, mergeManager, mapOutputBytes1.length, null, true);
    InMemoryMapOutput<Text, Text> mapOutput2 = new InMemoryMapOutput<Text, Text>(
        jobConf, mapId2, mergeManager, mapOutputBytes2.length, null, true);
    System.arraycopy(mapOutputBytes1, 0, mapOutput1.getMemory(), 0,
        mapOutputBytes1.length);
    System.arraycopy(mapOutputBytes2, 0, mapOutput2.getMemory(), 0,
        mapOutputBytes2.length);

    Path spillPath = new Path("test");
    MergeThread<InMemoryMapOutput<Text, Text>, Text, Text> inMemoryMerger =
        new RssInMemoryRemoteMerger<Text, Text>(mergeManager, jobConf, fs, spillPath,
            "test", null, Reporter.NULL, null,
            null, null, null, null, null);
    List<InMemoryMapOutput<Text, Text>> mapOutputs1 =
        new ArrayList<InMemoryMapOutput<Text, Text>>();
    mapOutputs1.add(mapOutput1);
    mapOutputs1.add(mapOutput2);

    String filePath = "spill" + Path.SEPARATOR + "test" + Path.SEPARATOR + mapId1;
    Path mergePath = new Path(spillPath, filePath);
    inMemoryMerger.merge(mapOutputs1);
    assertTrue(fs.exists(mergePath));
    List<String> keys = Lists.newArrayList();
    List<String> values = Lists.newArrayList();
    readOnDiskMapOutput(jobConf, fs, mergePath, keys, values);
    fs.delete(spillPath, true);
    List<String> actualKeys = Lists.newArrayList("apple", "banana", "carrot");
    List<String> actualValues = Lists.newArrayList("disgusting", "pretty good", "delicious");
    for (int i = 0; i < 3; i++) {
      assertEquals(keys.get(i), actualKeys.get(i));
      assertEquals(values.get(i), actualValues.get(i));
    }
  }

  private byte[] writeMapOutput(Configuration conf, Map<String, String> keysToValues)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FSDataOutputStream fsdos = new FSDataOutputStream(baos, null);
    IFile.Writer<Text, Text> writer = new IFile.Writer<Text, Text>(conf, fsdos,
        Text.class, Text.class, null, null);
    for (String key : keysToValues.keySet()) {
      String value = keysToValues.get(key);
      writer.append(new Text(key), new Text(value));
    }
    writer.close();
    return baos.toByteArray();
  }

  private void readOnDiskMapOutput(Configuration conf, FileSystem fs, Path path,
                                   List<String> keys, List<String> values) throws IOException {
    FSDataInputStream in = CryptoUtils.wrapIfNecessary(conf, fs.open(path));

    IFile.Reader<Text, Text> reader = new IFile.Reader<Text, Text>(conf, in,
        fs.getFileStatus(path).getLen(), null, null);
    DataInputBuffer keyBuff = new DataInputBuffer();
    DataInputBuffer valueBuff = new DataInputBuffer();
    Text key = new Text();
    Text value = new Text();
    while (reader.nextRawKey(keyBuff)) {
      key.readFields(keyBuff);
      keys.add(key.toString());
      reader.nextRawValue(valueBuff);
      value.readFields(valueBuff);
      values.add(value.toString());
    }
  }
}

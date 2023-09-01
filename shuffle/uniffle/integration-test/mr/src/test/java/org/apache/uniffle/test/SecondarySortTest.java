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

package org.apache.uniffle.test;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.SecondarySort;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RssMRConfig;
import org.apache.hadoop.util.Tool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

public class SecondarySortTest extends MRIntegrationTestBase {

  String inputPath = "secondary_sort_input";

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), HDFS_URI + "rss/test");
    dynamicConf.put(RssMRConfig.RSS_STORAGE_TYPE, StorageType.MEMORY_LOCALFILE_HDFS.name());
    addDynamicConf(coordinatorConf, dynamicConf);
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Test
  public void secondarySortTest() throws Exception {
    generateInputFile();
    run();
  }

  private void generateInputFile() throws Exception {
    FSDataOutputStream outputStream = fs.create(new Path(inputPath));
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      int first = random.nextInt();
      int second = random.nextInt();
      String str = first + " " + second + "\n";
      outputStream.writeBytes(str);
    }
    outputStream.close();
  }

  @Override
  protected Tool getTestTool() {
    return new TestTool();
  }

  private class TestTool  extends SecondarySort implements Tool, Configurable {

    String outputPath = "secondary_sort_output/" + System.currentTimeMillis();
    Configuration toolConf;

    @Override
    public int run(String[] strings) throws Exception {
      JobConf conf = (JobConf) getConf();
      FileInputFormat.setInputPaths(conf, new Path(inputPath));
      FileOutputFormat.setOutputPath(conf, new Path(outputPath));
      Job job = new Job(conf);
      job.setJarByClass(SecondarySort.class);
      job.setMapperClass(SecondarySort.MapClass.class);
      job.setReducerClass(SecondarySort.Reduce.class);
      job.setPartitionerClass(SecondarySort.FirstPartitioner.class);
      job.setGroupingComparatorClass(SecondarySort.FirstGroupingComparator.class);
      job.setMapOutputKeyClass(SecondarySort.IntPair.class);
      job.setMapOutputValueClass(IntWritable.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(IntWritable.class);
      return job.waitForCompletion(true) ? 0 : 1;
    }

    @Override
    public void setConf(Configuration configuration) {
      this.toolConf = configuration;
    }

    @Override
    public Configuration getConf() {
      return toolConf;
    }
  }
}

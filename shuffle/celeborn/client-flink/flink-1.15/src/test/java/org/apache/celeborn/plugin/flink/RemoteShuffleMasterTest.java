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

package org.apache.celeborn.plugin.flink;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.jobgraph.IntermediateResultPartitionID;
import org.apache.flink.runtime.shuffle.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.plugin.flink.utils.FlinkUtils;

public class RemoteShuffleMasterTest {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleMasterTest.class);
  private RemoteShuffleMaster remoteShuffleMaster;
  private Configuration configuration;

  @Before
  public void setUp() {
    configuration = new Configuration();
    remoteShuffleMaster = createShuffleMaster(configuration);
  }

  @Test
  public void testRegisterJob() {
    JobShuffleContext jobShuffleContext = createJobShuffleContext(JobID.generate());
    remoteShuffleMaster.registerJob(jobShuffleContext);

    // reRunRegister job
    try {
      remoteShuffleMaster.registerJob(jobShuffleContext);
    } catch (Exception e) {
      Assert.assertTrue(true);
    }

    // unRegister job
    remoteShuffleMaster.unregisterJob(jobShuffleContext.getJobId());
    remoteShuffleMaster.registerJob(jobShuffleContext);
  }

  @Test
  public void testRegisterPartitionWithProducer()
      throws UnknownHostException, ExecutionException, InterruptedException {
    JobID jobID = JobID.generate();
    JobShuffleContext jobShuffleContext = createJobShuffleContext(jobID);
    remoteShuffleMaster.registerJob(jobShuffleContext);

    IntermediateDataSetID intermediateDataSetID = new IntermediateDataSetID();
    PartitionDescriptor partitionDescriptor = createPartitionDescriptor(intermediateDataSetID, 0);
    ProducerDescriptor producerDescriptor = createProducerDescriptor();
    RemoteShuffleDescriptor remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    ShuffleResource shuffleResource = remoteShuffleDescriptor.getShuffleResource();
    ShuffleResourceDescriptor mapPartitionShuffleDescriptor =
        shuffleResource.getMapPartitionShuffleDescriptor();

    LOG.info("remoteShuffleDescriptor:{}", remoteShuffleDescriptor);
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getPartitionId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getAttemptId());
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getMapId());

    // use same dataset id
    partitionDescriptor = createPartitionDescriptor(intermediateDataSetID, 1);
    remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    mapPartitionShuffleDescriptor =
        remoteShuffleDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getMapId());

    // use another attemptId
    producerDescriptor = createProducerDescriptor();
    remoteShuffleDescriptor =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID, partitionDescriptor, producerDescriptor)
            .get();
    mapPartitionShuffleDescriptor =
        remoteShuffleDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();
    Assert.assertEquals(0, mapPartitionShuffleDescriptor.getShuffleId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getAttemptId());
    Assert.assertEquals(1, mapPartitionShuffleDescriptor.getMapId());
  }

  @Test
  public void testRegisterMultipleJobs()
      throws UnknownHostException, ExecutionException, InterruptedException {
    JobID jobID1 = JobID.generate();
    JobShuffleContext jobShuffleContext1 = createJobShuffleContext(jobID1);
    remoteShuffleMaster.registerJob(jobShuffleContext1);

    JobID jobID2 = JobID.generate();
    JobShuffleContext jobShuffleContext2 = createJobShuffleContext(jobID2);
    remoteShuffleMaster.registerJob(jobShuffleContext2);

    IntermediateDataSetID intermediateDataSetID = new IntermediateDataSetID();
    PartitionDescriptor partitionDescriptor = createPartitionDescriptor(intermediateDataSetID, 0);
    ProducerDescriptor producerDescriptor = createProducerDescriptor();
    RemoteShuffleDescriptor remoteShuffleDescriptor1 =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID1, partitionDescriptor, producerDescriptor)
            .get();

    // use same datasetId but different jobId
    RemoteShuffleDescriptor remoteShuffleDescriptor2 =
        remoteShuffleMaster
            .registerPartitionWithProducer(jobID2, partitionDescriptor, producerDescriptor)
            .get();

    Assert.assertEquals(
        remoteShuffleDescriptor1
            .getShuffleResource()
            .getMapPartitionShuffleDescriptor()
            .getShuffleId(),
        0);
    Assert.assertEquals(
        remoteShuffleDescriptor2
            .getShuffleResource()
            .getMapPartitionShuffleDescriptor()
            .getShuffleId(),
        1);
  }

  @Test
  public void testShuffleMemoryAnnouncing() {
    Map<IntermediateDataSetID, Integer> numberOfInputGateChannels = new HashMap<>();
    Map<IntermediateDataSetID, Integer> numbersOfResultSubpartitions = new HashMap<>();
    Map<IntermediateDataSetID, ResultPartitionType> resultPartitionTypes = new HashMap<>();
    IntermediateDataSetID inputDataSetID0 = new IntermediateDataSetID();
    IntermediateDataSetID inputDataSetID1 = new IntermediateDataSetID();
    IntermediateDataSetID outputDataSetID0 = new IntermediateDataSetID();
    IntermediateDataSetID outputDataSetID1 = new IntermediateDataSetID();
    IntermediateDataSetID outputDataSetID2 = new IntermediateDataSetID();
    Random random = new Random();
    numberOfInputGateChannels.put(inputDataSetID0, random.nextInt(1000));
    numberOfInputGateChannels.put(inputDataSetID1, random.nextInt(1000));
    numbersOfResultSubpartitions.put(outputDataSetID0, random.nextInt(1000));
    numbersOfResultSubpartitions.put(outputDataSetID1, random.nextInt(1000));
    numbersOfResultSubpartitions.put(outputDataSetID2, random.nextInt(1000));
    resultPartitionTypes.put(outputDataSetID0, ResultPartitionType.BLOCKING);
    resultPartitionTypes.put(outputDataSetID1, ResultPartitionType.BLOCKING);
    resultPartitionTypes.put(outputDataSetID2, ResultPartitionType.BLOCKING);
    MemorySize calculated =
        remoteShuffleMaster.computeShuffleMemorySizeForTask(
            TaskInputsOutputsDescriptor.from(
                numberOfInputGateChannels, numbersOfResultSubpartitions, resultPartitionTypes));

    CelebornConf conf = FlinkUtils.toCelebornConf(configuration);

    long numBytesPerGate = conf.clientFlinkMemoryPerInputGate();
    long expectedInput = 2 * numBytesPerGate;

    long numBytesPerResultPartition = conf.clientFlinkMemoryPerResultPartition();
    long expectedOutput = 3 * numBytesPerResultPartition;
    MemorySize expected = new MemorySize(expectedInput + expectedOutput);

    Assert.assertEquals(expected, calculated);
  }

  @After
  public void tearDown() {
    if (remoteShuffleMaster != null) {
      try {
        remoteShuffleMaster.close();
      } catch (Exception e) {
        LOG.warn(e.getMessage(), e);
      }
    }
  }

  public RemoteShuffleMaster createShuffleMaster(Configuration configuration) {
    remoteShuffleMaster =
        new RemoteShuffleMaster(
            new ShuffleMasterContext() {
              @Override
              public Configuration getConfiguration() {
                return configuration;
              }

              @Override
              public void onFatalError(Throwable throwable) {
                System.exit(-1);
              }
            },
            new SimpleResultPartitionAdapter());

    return remoteShuffleMaster;
  }

  public JobShuffleContext createJobShuffleContext(JobID jobId) {
    return new JobShuffleContext() {
      @Override
      public org.apache.flink.api.common.JobID getJobId() {
        return jobId;
      }

      @Override
      public CompletableFuture<?> stopTrackingAndReleasePartitions(
          Collection<ResultPartitionID> collection) {
        return CompletableFuture.completedFuture(null);
      }
    };
  }

  public PartitionDescriptor createPartitionDescriptor(
      IntermediateDataSetID intermediateDataSetId, int partitionNum) {
    IntermediateResultPartitionID intermediateResultPartitionId =
        new IntermediateResultPartitionID(intermediateDataSetId, partitionNum);
    return new PartitionDescriptor(
        intermediateDataSetId,
        10,
        intermediateResultPartitionId,
        ResultPartitionType.BLOCKING,
        5,
        1);
  }

  public ProducerDescriptor createProducerDescriptor() throws UnknownHostException {
    ExecutionAttemptID executionAttemptId = new ExecutionAttemptID();
    return new ProducerDescriptor(
        ResourceID.generate(), executionAttemptId, InetAddress.getLocalHost(), 100);
  }
}

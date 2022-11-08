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

package com.bytedance.bitsail.connector.legacy.redis;

import com.bytedance.bitsail.connector.legacy.redis.core.AbstractPipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.core.Command;
import com.bytedance.bitsail.connector.legacy.redis.core.api.FailureHandler;
import com.bytedance.bitsail.connector.legacy.redis.core.api.PipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.core.api.SplitPolicy;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class RedisPipelineProcessor extends AbstractPipelineProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(RedisPipelineProcessor.class);

  public RedisPipelineProcessor(JedisPool jedisPool,
                                Retryer.RetryerCallable<Jedis> jedisFetcher,
                                int commandSize,
                                long processorId,
                                int logSampleInterval,
                                boolean complexTypeWithTtl) throws ExecutionException, RetryException {
    super(jedisPool, jedisFetcher, commandSize, processorId, logSampleInterval, complexTypeWithTtl);

    this.failureHandler = new FailureHandler() {
      @Override
      public void resolve(Command command, JedisDataException exception, PipelineProcessor processor) {
        processor.handleNeedRetriedRecords(command, exception);
      }

      @Override
      public void caughtUnexpectedError(Command command, Throwable failure, PipelineProcessor processor) {
        processor.handleUnexpectedFailedRecord(command, failure);
      }
    };

    this.splitPolicy = new SplitPolicy() {
      @Override
      public List<List<Command>> split(List<Command> requests, int splitGroups) {
        return noSplit(requests);
      }
    };
  }

  @Override
  public void acquireConnection(boolean logConnection) throws ExecutionException, RetryException {
    this.jedis = jedisFetcher.call();
    this.pipeline = jedis.pipelined();
    if (logConnection) {
      if (jedis != null) {
        LOG.info("the {} attempt will connect to {}", attemptNumber.get(), jedis.getClient());
      }
    }
  }

  @Override
  public void preExecute() throws Exception {
    super.preExecute();
    splitRequests = splitPolicy.noSplit(requests);
    if (!isFirstRun()) {
      clear();
    }
    if (hitLogSampling()) {
      LOG.info("start to pipeline [{}] records, attempt number:[{}], split groups:[{}], processor id:[{}]",
          requests.size(), attemptNumber.get(), splitRequests.size(), processorId);
    }
  }
}


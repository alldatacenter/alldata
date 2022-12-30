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

package com.bytedance.bitsail.connector.legacy.redis.core;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.legacy.redis.core.api.FailureHandler;
import com.bytedance.bitsail.connector.legacy.redis.core.api.PipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.core.api.SplitPolicy;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisCommand;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.redis.error.UnexpectedException;
import com.bytedance.bitsail.connector.legacy.redis.sink.JedisOutputFormat;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Wrapping up the native Jedis pipeline, making it retryable.
 **/
public abstract class AbstractPipelineProcessor implements PipelineProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractPipelineProcessor.class);

  /**
   * Incremental ID in a single split which is used to print data every other period of time.
   *
   */
  protected long processorId;

  /**
   * Jedis connection pool
   */
  protected final JedisPool jedisPool;

  /**
   * When fetching jedis instance, it may create connection (lazy load).
   * So we need retryer.
   */
  protected final Retryer.RetryerCallable<Jedis> jedisFetcher;

  /**
   * A native jedis connection.
   */
  protected Jedis jedis;

  /**
   * A native jedis pipeline.
   */
  protected Pipeline pipeline;

  /**
   * Commands to execute in this pipeline.
   */
  protected List<Command> requests;

  /**
   * Requests list after splitting.
   */
  @Getter
  protected List<List<Command>> splitRequests;

  /**
   * Store succeeded commands.
   */
  @Getter
  protected List<Command> successfulRecords;

  /**
   * Records with unexpected failures.
   */
  @Getter
  protected List<Pair<Command, Throwable>> unexpectedFailedRecords;

  /**
   * Records that no need to be retried.
   */
  @Getter
  protected List<Pair<Command, Throwable>> dirtyRecords;

  /**
   * Records to retry.
   */
  @Getter
  protected List<Pair<Command, Throwable>> needRetriedRecords;

  /**
   * Failure handler.
   */
  protected FailureHandler failureHandler;

  /**
   * Split policy.
   */
  protected SplitPolicy splitPolicy;

  /**
   * Use atomic to prevent concurrency problem.
   */
  protected AtomicInteger attemptNumber;

  /**
   * Cache message of each key.
   */
  private String cachedErrorMessage;

  /**
   * Number of commands to process.
   */
  protected int commandSize;

  /**
   * Complex data type (with ttl) needs two command, so responseSize=2.
   */
  private final int responseSize;
  protected final boolean complexTypeWithTtl;

  /**
   * Log interval.
   */
  protected int logSampleInterval;

  protected AbstractPipelineProcessor(JedisPool jedisPool,
                                      Retryer.RetryerCallable<Jedis> jedisFetcher,
                                      int commandSize,
                                      long processorId,
                                      int logSampleInterval,
                                      boolean complexTypeWithTtl) {
    this.jedisPool = jedisPool;
    this.jedisFetcher = jedisFetcher;
    this.commandSize = commandSize;

    this.requests = new ArrayList<>(commandSize);
    this.successfulRecords = new ArrayList<>(commandSize);
    this.unexpectedFailedRecords = new ArrayList<>();
    this.dirtyRecords = new ArrayList<>();
    this.needRetriedRecords = new ArrayList<>();
    this.attemptNumber = new AtomicInteger(0);
    this.processorId = processorId;
    this.logSampleInterval = logSampleInterval;
    this.complexTypeWithTtl = complexTypeWithTtl;
    this.responseSize = complexTypeWithTtl ? 2 : 1;
  }

  /**
   * Acquire connection from pool.
   */
  public abstract void acquireConnection(boolean logConnection) throws ExecutionException, RetryException;

  /**
   * Release old connection.
   */
  public void releaseConnection() {
    try {
      if (pipeline != null) {
        pipeline.close();
      }
      if (jedis != null) {
        jedis.close();
      }
    } catch (Exception e) {
      LOG.warn("release jedis connection occurs error.", e);
    }
  }

  @Override
  public void preExecute() throws Exception {
    attemptNumber.getAndIncrement();
    acquireConnection(!isFirstRun());
  }

  /**
   * Execute the pipeline and retry the pipeline if it fails.
   *
   * @return If need retry.
   */
  @Override
  public boolean run() {
    try {
      preExecute();
      for (List<Command> curSplitRequests : splitRequests) {
        try {
          curSplitRequests.forEach(this::submitCommand);
          List<Object> responses = this.pipeline.syncAndReturnAll();
          postExecute(curSplitRequests, responses);
          if (!isFirstRun()) {
            TimeUnit.SECONDS.sleep(1);
          }
        } catch (InterruptedException e) {
          LOG.error("serially submitting pipeline was interrupted, maybe the task manager is doing fail over.", e);
          postExecute(curSplitRequests, e);
        } catch (Throwable throwable) {
          LOG.warn("execute pipeline occurs error, current batch will be retried, error message is: ", throwable);
          postExecuteWithRetry(curSplitRequests, throwable);
        }
      }
    } catch (Exception e) {
      LOG.error("pre execute pipeline occurs error, may caused by connection acquiring.", e);
      postExecuteWithRetry(requests, e);
    } finally {
      releaseConnection();
    }
    return needRetry();
  }

  @Override
  public void postExecute(List<Command> curRequests, List<Object> responses) {
    int size = curRequests.size();
    for (int i = 0; i < size; i++) {
      Object response = responses.get(i * responseSize);
      Object ttlResponse = this.complexTypeWithTtl ? responses.get(i * responseSize + 1) : null;
      Command command = curRequests.get(i);
      if (response instanceof Throwable) {
        LOG.warn("command failed! connection info: [{}], command is: {}, error is: {}",
            jedis.getClient(),
            command.print(), ((Throwable) response).getMessage(), (Throwable) response);
        
        failureHandler.handle(command, (Throwable) response, this);
      } else if (ttlResponse instanceof Throwable) {
        LOG.warn("Set ttl error! " + ((Throwable) ttlResponse).getMessage(), (Throwable) ttlResponse);
        failureHandler.handle(command, (Throwable) ttlResponse, this);
      } else {
        // success
        this.successfulRecords.add(command);
      }
    }
  }

  @Override
  public void postExecute(List<Command> curRequests, Throwable failure) {
    for (Command command : curRequests) {
      handleUnexpectedFailedRecord(command, failure);
    }
  }

  @Override
  public void postExecuteWithRetry(List<Command> curRequests, Throwable failure) {
    for (Command command : curRequests) {
      handleNeedRetriedRecords(command, failure);
    }
  }

  @Override
  public boolean needRetry() {

    if (CollectionUtils.isNotEmpty(unexpectedFailedRecords)) {
      throw new UnexpectedException(printErrorMessage(), unexpectedFailedRecords.get(0).getSecond());
    }
    boolean needRetry = attemptNumber.get() < JedisOutputFormat.MAX_ATTEMPT_NUM
        && CollectionUtils.isNotEmpty(needRetriedRecords);

    if (!needRetry) {
      if (needRetriedRecords.isEmpty()) {
        if (hitLogSampling()) {
          LOG.info("finished pipeline [{}] records, attempt number:[{}], processor id:[{}]",
              commandSize, attemptNumber.get(), processorId);
        }
      } else {
        Throwable throwable = needRetriedRecords.get(0).getSecond();
        if (throwable instanceof JedisDataException) {
          LOG.error("pipeline finally failed after {} attempts. {}", attemptNumber.get(), printErrorMessage());
          throw new BitSailException(JedisPluginErrorCode.PIPELINE_ERROR, printErrorMessage());
        } else {
          throw new UnexpectedException(printErrorMessage(), unexpectedFailedRecords.get(0).getSecond());
        }
      }
    } else {
      LOG.error("pipeline temporarily failed after {} attempts. {}", attemptNumber.get(), printErrorMessage());
    }
    return needRetry;
  }

  @Override
  public void addInitialCommand(Command command) {
    this.requests.add(command);
  }

  @Override
  public void handleUnexpectedFailedRecord(Command command, Throwable throwable) {
    this.unexpectedFailedRecords.add(Pair.newPair(command, throwable));
  }

  @Override
  public void handleDirtyRecords(Command command, JedisDataException exception) {
    this.dirtyRecords.add(Pair.newPair(command, exception));
  }

  @Override
  public void handleNeedRetriedRecords(Command command, Throwable exception) {
    this.needRetriedRecords.add(Pair.newPair(command, exception));
  }

  /**
   * Submit command to pipeline.
   */
  protected void submitCommand(Command command) {
    switch (command.getJedisCommand()) {
      case SET:
        this.pipeline.set(command.getKey(), command.getValue());
        break;
      case SETEX:
        this.pipeline.setex(command.getKey(), command.getTtlInSeconds(), command.getValue());
        break;
      case SADD:
        this.pipeline.sadd(command.getKey(), command.getValue());
        break;
      case HSET:
        this.pipeline.hset(command.getKey(), command.getHashField(), command.getValue());
        break;
      case ZADD:
        this.pipeline.zadd(command.getKey(), command.getScore(), command.getValue());
        break;
      default:
        // cannot reach here
        break;
    }
    if (command.getJedisCommand() != JedisCommand.SET
        && command.getJedisCommand() != JedisCommand.SETEX
        && command.getTtlInSeconds() > 0) {
      this.pipeline.expire(command.getKey(), command.getTtlInSeconds());
    }
  }

  @Override
  public void close() {
    this.pipeline = null;
    this.jedis = null;
  }

  /**
   * Reset pipeline
   */
  public void clear() {
    this.needRetriedRecords.clear();
    this.cachedErrorMessage = null;
  }

  @Override
  public boolean isFirstRun() {
    return attemptNumber.get() <= 1;
  }

  /**
   * Make sure call this method in the last retry.
   *
   * @return Error message.
   */
  public String printErrorMessage() {
    if (cachedErrorMessage == null) {
      StringBuilder sb = new StringBuilder();

      sb.append(String.format("jedis connection info: [%s]. ", jedis.getClient()));

      if (CollectionUtils.isNotEmpty(unexpectedFailedRecords)) {
        sb.append(String.format("There are [%s] records failed caused by unexpected error," +
                " pipeline error message is:[%s], These keys may be failed:[%s]\n",
            unexpectedFailedRecords.size(), unexpectedFailedRecords.get(0).getSecond(),
            unexpectedFailedRecords.stream().map(r -> r.getFirst().print()).collect(Collectors.joining(",")))
        );
      }

      if (CollectionUtils.isNotEmpty(dirtyRecords)) {
        sb.append(String.format("There are [%s] records failed caused by dirty," +
                " pipeline error message is:[%s], These keys may be failed:[%s]\n",
            dirtyRecords.size(), dirtyRecords.get(0).getSecond(),
            dirtyRecords.stream().map(r -> r.getFirst().print()).collect(Collectors.joining(",")))
        );
      }

      if (CollectionUtils.isNotEmpty(needRetriedRecords)) {
        sb.append(String.format("There are [%s] records failed caused by server error," +
                " pipeline error message is:[%s], These keys may be failed:[%s]\n",
            needRetriedRecords.size(), needRetriedRecords.get(0).getSecond(),
            needRetriedRecords.stream().map(r -> r.getFirst().print()).collect(Collectors.joining(",")))
        );
      }
      cachedErrorMessage = sb.toString();
    }
    return cachedErrorMessage;
  }

  /**
   * @return Determine if to log.
   */
  @Override
  public boolean hitLogSampling() {
    return (processorId & (logSampleInterval - 1)) == 0;
  }
}


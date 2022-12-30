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

package com.bytedance.bitsail.connector.legacy.redis.sink;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.redis.core.Command;
import com.bytedance.bitsail.connector.legacy.redis.core.TtlType;
import com.bytedance.bitsail.connector.legacy.redis.core.api.PipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisCommand;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisCommandDescription;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisDataType;
import com.bytedance.bitsail.connector.legacy.redis.error.UnexpectedException;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class JedisOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(JedisOutputFormat.class);

  private static final long serialVersionUID = -2257717951626656731L;

  /**
   * Jedis connection pool.
   */
  protected transient JedisPool jedisPool;

  /**
   * Retryer for obtaining jedis.
   */
  protected transient Retryer.RetryerCallable<Jedis> jedisFetcher;

  protected transient Retryer<Boolean> retryer;

  /**
   * pipeline id for logging.
   */
  protected long processorId;

  /**
   * Expiring times in seconds.
   */
  protected int ttlInSeconds;

  /**
   * Batch send by pipeline after 'batchInterval' records.
   */
  protected int batchInterval;

  /**
   * A buffer for batch send.
   */
  protected transient CircularFifoQueue<Row> recordQueue;

  /**
   * Command used in the job.
   */
  protected JedisCommandDescription commandDescription;

  /**
   * Number of columns to send in each record.
   */
  protected int columnSize;

  /**
   * Log interval of pipelines.
   */
  protected int logSampleInterval;

  protected RowTypeInfo rowTypeInfo;

  /**
   * Complex type command with ttl.
   */
  private boolean complexTypeWithTtl;

  private static final int SORTED_SET_OR_HASH_COLUMN_SIZE = 3;

  public static final int MAX_ATTEMPT_NUM = 5;

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
    this.recordQueue = new CircularFifoQueue<>(batchInterval);
  }

  @Override
  public void writeRecordInternal(Row record) {
    validate(record);

    this.recordQueue.add(record);
    if (recordQueue.isAtFullCapacity()) {
      flush();
    }
  }

  /**
   * Flush all buffered commands by pipeline.
   */
  @SneakyThrows
  protected void flush() throws BitSailException {
    processorId++;
    try (PipelineProcessor processor = genPipelineProcessor(recordQueue.size(), this.complexTypeWithTtl)) {
      Row record;
      while ((record = recordQueue.poll()) != null) {

        //***************** todo: remove this logic *****************
        byte[] key = generateKey((byte[]) record.getField(0));
        byte[] value = (byte[]) record.getField(1);
        byte[] scoreOrHashKey = value;
        if (columnSize == SORTED_SET_OR_HASH_COLUMN_SIZE) {
          value = (byte[]) record.getField(2);
          // Replace empty key with additionalKey in sorted set and hash.
          if (key.length == 0) {
            key = commandDescription.getAdditionalKey().getBytes();
          }
        }
        //*******************************************************

        if (commandDescription.getJedisCommand() == JedisCommand.ZADD) {
          // sortedSet
          processor.addInitialCommand(new Command(commandDescription, key, parseScoreFromBytes(scoreOrHashKey), value));
        } else if (commandDescription.getJedisCommand() == JedisCommand.HSET) {
          // hash
          processor.addInitialCommand(new Command(commandDescription, key, scoreOrHashKey, value));
        } else {
          // set and string
          processor.addInitialCommand(new Command(commandDescription, key, value));
        }
      }
      retryer.call(processor::run);

    } catch (ExecutionException | RetryException e) {
      if (e.getCause() instanceof BitSailException) {
        throw (BitSailException) e.getCause();
      } else if (e.getCause() instanceof UnexpectedException) {
        throw (UnexpectedException) e.getCause();
      }
      throw e;
    } catch (IOException e) {
      throw new RuntimeException("Error while init jedis client.", e);
    }
  }

  /**
   * Gen pipeline processor for jedis commands.
   *
   * @return Jedis pipeline.
   */
  protected abstract PipelineProcessor genPipelineProcessor(int commandSize, boolean complexTypeWithTtl) throws ExecutionException, RetryException;

  /**
   * Pre-check data.
   */
  protected void validate(Row record) throws BitSailException {
    if (record.getArity() != columnSize) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("The record's size is %d , but supposed to be %d!", record.getArity(), columnSize));
    }
    for (int i = 0; i < columnSize; i++) {
      if (record.getField(i) == null) {
        throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING,
            String.format("record contains null element in index[%d]", i));
      }
    }
    if (commandDescription.getJedisCommand() == JedisCommand.ZADD) {
      parseScoreFromBytes((byte[]) record.getField(1));
    }
  }


  /**
   * check if score field can be parsed to double.
   */
  @VisibleForTesting
  public double parseScoreFromBytes(byte[] scoreInBytes) throws BitSailException {
    try {
      return Double.parseDouble(new String(scoreInBytes));
    } catch (NumberFormatException exception) {
      throw new BitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("The score can't convert to double. And the score is %s.",
              new String(scoreInBytes)));
    }
  }

  /**
   * Produce key.
   */
  protected abstract byte[] generateKey(byte[] key);


  /**
   * When a split is closed and queue still has records,
   * the rest records are flushed and we need to catch exception.
   */
  @Override
  public void close() throws IOException {

    Row sample = null;
    try {
      if (CollectionUtils.isNotEmpty(recordQueue)) {
        sample = recordQueue.get(0);
        flush();
      }
    } catch (BitSailException e) {
      messenger.addFailedRecord(sample, e);
      dirtyCollector.collectDirty(sample, e, System.currentTimeMillis());
      LOG.warn("flush the last pipeline occurs dts exception, caused by dirty records.", e);
    } catch (Exception e) {
      LOG.error("flush the last pipeline occurs error, cause by other error.", e);
      throw e;
    } finally {
      super.close();
      jedisPool.close();
    }
  }

  @Override
  public void tryCleanupOnError() {
  }

  protected int getTtlInSeconds(int ttl, TtlType ttlType) {
    if (ttl <= 0) {
      return -1;
    }
    return ttl * ttlType.getContainSeconds();
  }

  @VisibleForTesting
  public JedisCommandDescription initJedisCommandDescription(String redisDataType, int ttlSeconds, String additionalKey) {
    JedisDataType dataType = JedisDataType.valueOf(redisDataType.toUpperCase());
    JedisCommand jedisCommand;
    this.complexTypeWithTtl = ttlSeconds > 0;
    switch (dataType) {
      case STRING:
        jedisCommand = JedisCommand.SET;
        if (ttlSeconds > 0) {
          jedisCommand = JedisCommand.SETEX;
          this.complexTypeWithTtl = false;
        }
        break;
      case SET:
        jedisCommand = JedisCommand.SADD;
        break;
      case HASH:
        jedisCommand = JedisCommand.HSET;
        break;
      case SORTED_SET:
        jedisCommand = JedisCommand.ZADD;
        break;
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "The configure date type " + redisDataType +
            " is not supported, only support string, set, hash, sorted set.");
    }
    if (ttlSeconds <= 0) {
      return new JedisCommandDescription(jedisCommand, additionalKey);
    }
    return new JedisCommandDescription(jedisCommand, additionalKey, ttlSeconds);
  }

  @Override
  public boolean uniformedParallelism() {
    return true;
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  protected RowTypeInfo getRowTypeInfo(List<ColumnInfo> columns) {
    return commandDescription.getJedisCommand().getRowTypeInfo();
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }
}

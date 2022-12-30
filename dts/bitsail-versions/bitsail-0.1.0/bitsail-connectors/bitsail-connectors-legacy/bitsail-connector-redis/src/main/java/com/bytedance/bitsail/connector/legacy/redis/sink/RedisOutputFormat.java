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
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.redis.RedisPipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.constant.RedisConstants;
import com.bytedance.bitsail.connector.legacy.redis.core.TtlType;
import com.bytedance.bitsail.connector.legacy.redis.core.api.PipelineProcessor;
import com.bytedance.bitsail.connector.legacy.redis.error.RedisPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.redis.option.RedisWriterOptions;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.bytedance.bitsail.connector.legacy.redis.constant.RedisConstants.MAX_PARALLELISM_OUTPUT_REDIS;

public class RedisOutputFormat extends JedisOutputFormat {
  private static final Logger LOG = LoggerFactory.getLogger(RedisOutputFormat.class);

  private static final long serialVersionUID = 1865789744864217944L;

  private String redisHost;

  private int redisPort;

  private String redisPassword;

  private int timeout;

  @Override
  public void initPlugin() {
    // STEP1: get redis info
    this.redisHost = outputSliceConfig.getNecessaryOption(RedisWriterOptions.HOST, RedisPluginErrorCode.REQUIRED_VALUE);
    this.redisPort = outputSliceConfig.getNecessaryOption(RedisWriterOptions.PORT, RedisPluginErrorCode.REQUIRED_VALUE);
    this.redisPassword = outputSliceConfig.get(RedisWriterOptions.PASSWORD);
    this.timeout = outputSliceConfig.get(RedisWriterOptions.CLIENT_TIMEOUT_MS);

    // STEP2: initialize ttl
    int ttl = outputSliceConfig.getUnNecessaryOption(RedisWriterOptions.TTL, -1);
    TtlType ttlType;
    try {
      ttlType = TtlType.valueOf(StringUtils.upperCase(outputSliceConfig.get(RedisWriterOptions.TTL_TYPE)));
    } catch (IllegalArgumentException e) {
      throw BitSailException.asBitSailException(RedisPluginErrorCode.ILLEGAL_VALUE,
          String.format("unknown ttl type: %s", outputSliceConfig.get(RedisWriterOptions.TTL_TYPE)));
    }
    ttlInSeconds = getTtlInSeconds(ttl, ttlType);
    LOG.info("ttl is {}(s)", ttlInSeconds);

    // STEP3: initialize commandDescription
    String redisDataType = StringUtils.upperCase(outputSliceConfig.get(RedisWriterOptions.REDIS_DATA_TYPE));
    String additionalKey = outputSliceConfig.getUnNecessaryOption(RedisWriterOptions.ADDITIONAL_KEY, "default_redis_key");
    this.commandDescription = initJedisCommandDescription(redisDataType, ttlInSeconds, additionalKey);
    this.columnSize = this.commandDescription.getJedisCommand().getColumnSize();
    List<ColumnInfo> columns = outputSliceConfig.get(RedisWriterOptions.COLUMNS);

    // STEP4: initialize other parameters
    this.batchInterval = outputSliceConfig.get(RedisWriterOptions.WRITE_BATCH_INTERVAL);
    this.rowTypeInfo = getRowTypeInfo(columns);
    LOG.info("Output Row Type Info: " + rowTypeInfo);

    this.logSampleInterval = outputSliceConfig.get(RedisWriterOptions.LOG_SAMPLE_INTERVAL);
    LOG.info("log_sample_interval is:[{}]", this.logSampleInterval);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);

    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setMaxTotal(RedisConstants.DEFAULT_MAX_TOTAL_CONNECTIONS);
    jedisPoolConfig.setMaxIdle(RedisConstants.DEFAULT_MAX_IDLE_CONNECTIONS);
    jedisPoolConfig.setMinIdle(RedisConstants.DEFAULT_MIN_IDLE_CONNECTIONS);
    jedisPoolConfig.setMaxWaitMillis(RedisConstants.DEFAULT_MAX_WAIT_TIME_IN_MILLS);

    if (StringUtils.isEmpty(redisPassword)) {
      this.jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, timeout);
    } else {
      this.jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, timeout, redisPassword);
    }
    this.jedisFetcher = RetryerBuilder.<Jedis>newBuilder()
        .retryIfResult(Objects::isNull)
        .retryIfRuntimeException()
        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
        .withWaitStrategy(WaitStrategies.exponentialWait(100, 5, TimeUnit.MINUTES))
        .build()
        .wrap(jedisPool::getResource);
    this.retryer = RetryerBuilder.<Boolean>newBuilder()
        .retryIfResult(needRetry -> Objects.equals(needRetry, true))
        .retryIfException(e -> !(e instanceof BitSailException))
        .withWaitStrategy(WaitStrategies.fixedWait(3, TimeUnit.SECONDS))
        .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_ATTEMPT_NUM))
        .build();
  }

  @Override
  protected PipelineProcessor genPipelineProcessor(int commandSize, boolean complexTypeWithTtl) throws ExecutionException, RetryException {
    return new RedisPipelineProcessor(jedisPool, jedisFetcher, commandSize, processorId, logSampleInterval, complexTypeWithTtl);
  }

  @Override
  protected byte[] generateKey(byte[] key) {
    return key;
  }

  @Override
  public String getType() {
    return "Redis";
  }

  @Override
  public int getMaxParallelism() {
    return MAX_PARALLELISM_OUTPUT_REDIS;
  }
}


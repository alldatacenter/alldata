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

package com.bytedance.bitsail.test.connector.test.testcontainers.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class RedisContainer implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(RedisContainer.class);

  private static final String REDIS_VERSION =  "redis:6.2.6";
  private static final int DEFAULT_EXPOSED_PORT = 6379;

  private GenericContainer redis;
  private RedisBackedCache backedCache;

  private final int exposedPort;

  @Getter
  private String host;

  @Getter
  private int port;

  public RedisContainer(int exposedPort) {
    this.exposedPort = exposedPort;
  }

  public RedisContainer() {
    this(DEFAULT_EXPOSED_PORT);
  }

  public void start() {
    redis = new GenericContainer(DockerImageName.parse(REDIS_VERSION))
        .withExposedPorts(exposedPort);
    redis.start();
    this.host = redis.getHost();
    this.port = redis.getFirstMappedPort();
    LOG.info("Redis container starts! Host is: [{}], port is: [{}].", host, port);
  }

  @Override
  public void close() throws IOException {
    if (Objects.nonNull(backedCache)) {
      backedCache.close();
    }
    redis.close();
    LOG.info("Redis container closed.");
  }

  /**
   * Only support searching kv.
   */
  public String getKey(String key) {
    if (Objects.isNull(backedCache)) {
      backedCache = new RedisBackedCache(host, port);
    }
    return backedCache.get(key);
  }

  public int getKeyCount() {
    if (Objects.isNull(backedCache)) {
      backedCache = new RedisBackedCache(host, port);
    }
    List<String> keys = backedCache.getAllKeys();
    LOG.info("Get {} keys from redis.", keys.size());
    return keys.size();
  }

  static class RedisBackedCache implements Closeable {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    public RedisBackedCache(String host, int port) {
      client = RedisClient.create(String.format("redis://%s:%d/0", host, port));
      connection = client.connect();
    }

    public String get(String key) {
      return connection.sync().get(key);
    }

    public List<String> getAllKeys() {
      return connection.sync().keys("*");
    }

    @Override
    public void close() {
      connection.close();
      client.close();
    }
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hbase;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.hbase.HBaseStoragePlugin.HBaseConnectionKey;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalListener;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalNotification;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * <p>A singleton class which manages the lifecycle of HBase connections.</p>
 * <p>One connection per storage plugin instance is maintained.</p>
 */
public final class HBaseConnectionManager
    extends CacheLoader<HBaseConnectionKey, Connection> implements RemovalListener<HBaseConnectionKey, Connection> {
  private static final Logger logger = LoggerFactory.getLogger(HBaseConnectionManager.class);

  public static final HBaseConnectionManager INSTANCE = new HBaseConnectionManager();

  private final LoadingCache<HBaseConnectionKey, Connection> connectionCache;

  private HBaseConnectionManager() {
    this.connectionCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS) // Connections will be closed after 1 hour of inactivity
        .removalListener(this)
        .build(this);
  }

  private boolean isValid(Connection conn) {
    return conn != null
        && !conn.isAborted()
        && !conn.isClosed();
  }

  @Override
  public Connection load(HBaseConnectionKey key) throws Exception {
    Connection connection = ConnectionFactory.createConnection(key.getHBaseConf());
    logger.info("HBase connection '{}' created.", connection);
    return connection;
  }

  @Override
  public void onRemoval(RemovalNotification<HBaseConnectionKey, Connection> notification) {
    try {
      Connection conn = notification.getValue();
      if (isValid(conn)) {
        conn.close();
      }
      logger.info("HBase connection '{}' closed.", conn);
    } catch (Throwable t) {
      logger.warn("Error while closing HBase connection.", t);
    }
  }

  public Connection getConnection(HBaseConnectionKey key) {
    checkNotNull(key);
    try {
      Connection conn = connectionCache.get(key);
      if (!isValid(conn)) {
        key.lock(); // invalidate the connection with a per storage plugin lock
        try {
          conn = connectionCache.get(key);
          if (!isValid(conn)) {
            connectionCache.invalidate(key);
            conn = connectionCache.get(key);
          }
        } finally {
          key.unlock();
        }
      }
      return conn;
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw UserException.dataReadError(e.getCause()).build(logger);
    }
  }

  public void closeConnection(HBaseConnectionKey key) {
    checkNotNull(key);
    connectionCache.invalidate(key);
  }

}

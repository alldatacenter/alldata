/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive;

import com.netease.arctic.table.TableMetaStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.iceberg.ClientPoolImpl;
import org.apache.iceberg.common.DynConstructors;
import org.apache.iceberg.hive.RuntimeMetaException;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended implementation of {@link ClientPoolImpl} with {@link TableMetaStore} to support authenticated hive
 * cluster.
 */
public class ArcticHiveClientPool extends ClientPoolImpl<HMSClient, TException> {
  private final TableMetaStore metaStore;

  private final HiveConf hiveConf;
  private static final Logger LOG = LoggerFactory.getLogger(ArcticHiveClientPool.class);

  private static final DynConstructors.Ctor<HiveMetaStoreClient> CLIENT_CTOR = DynConstructors.builder()
      .impl(HiveMetaStoreClient.class, HiveConf.class)
      .impl(HiveMetaStoreClient.class, Configuration.class)
      .build();

  public ArcticHiveClientPool(TableMetaStore tableMetaStore, int poolSize) {
    super(poolSize, TTransportException.class, true);
    this.hiveConf = new HiveConf(tableMetaStore.getConfiguration(), ArcticHiveClientPool.class);
    this.hiveConf.addResource(tableMetaStore.getConfiguration());
    this.hiveConf.addResource(tableMetaStore.getHiveSiteLocation().orElse(null));
    this.metaStore = tableMetaStore;
  }

  @Override
  protected HMSClient newClient() {
    return metaStore.doAs(() -> {
          try {
            try {
              HiveMetaStoreClient client = CLIENT_CTOR.newInstance(hiveConf);
              return new HMSClientImpl(client);
            } catch (RuntimeException e) {
              // any MetaException would be wrapped into RuntimeException during reflection, so let's double-check type
              // here
              if (e.getCause() instanceof MetaException) {
                throw (MetaException) e.getCause();
              }
              throw e;
            }
          } catch (MetaException e) {
            throw new RuntimeMetaException(e, "Failed to connect to Hive Metastore");
          } catch (Throwable t) {
            if (t.getMessage().contains("Another instance of Derby may have already booted")) {
              throw new RuntimeMetaException(t, "Failed to start an embedded metastore because embedded " +
                  "Derby supports only one client at a time. To fix this, use a metastore that supports " +
                  "multiple clients.");
            }

            throw new RuntimeMetaException(t, "Failed to connect to Hive Metastore");
          }
        }
    );
  }

  @Override
  protected HMSClient reconnect(HMSClient client) {
    try {
      return metaStore.doAs(() -> {
        try {
          client.close();
          client.reconnect();
        } catch (MetaException e) {
          throw new RuntimeMetaException(e, "Failed to reconnect to Hive Metastore");
        }
        return client;
      });
    } catch (Exception e) {
      LOG.error("hive metastore client reconnected failed", e);
      throw e;
    }
  }

  @Override
  protected boolean isConnectionException(Exception e) {
    return super.isConnectionException(e) || (e != null && e instanceof MetaException &&
        e.getMessage().contains("Got exception: org.apache.thrift.transport.TTransportException"));
  }

  @Override
  protected void close(HMSClient client) {
    client.close();
  }
}
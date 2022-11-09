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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive;

import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveMeta;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;
import com.bytedance.bitsail.shaded.hive.shim.HiveShim;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.apache.flink.core.fs.Path;
import org.apache.flink.table.catalog.hive.util.HiveTableUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Hive {@link TableMetaStoreFactory}, use {@link IMetaStoreClient} to communicate with
 * hive meta store.
 */
public class HiveTableMetaStoreFactory implements TableMetaStoreFactory {
  private static final long serialVersionUID = 1L;
  public static final int DEFAULT_SLEEP_TIME = 100;
  public static final int DEFAULT_RETRY_TIMES = 5;
  private static final Logger LOG = LoggerFactory.getLogger(HiveTableMetaStoreFactory.class);
  private static final Retryer<Object> RETRYER = RetryerBuilder.newBuilder()
      .retryIfException()
      .withWaitStrategy(WaitStrategies.fixedWait(DEFAULT_SLEEP_TIME, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(DEFAULT_RETRY_TIMES))
      .build();

  private final String database;
  private final String tableName;
  private final Map<String, String> metaStoreProperties;
  private final HiveShim hiveShim;

  public HiveTableMetaStoreFactory(
      String database,
      String tableName,
      String metaStoreProperties) {
    this.database = database;
    this.tableName = tableName;
    this.metaStoreProperties = JsonSerializer.parseToMap(metaStoreProperties);
    LOG.info("Meta store properties: {}.", metaStoreProperties);
    this.hiveShim = HiveMetaClientUtil.getHiveShim();
  }

  public static HiveMeta createHiveMeta(HiveTableMetaStoreFactory factory) {
    try {
      try (HiveTableMetaStore hiveMetaStore = factory.createTableMetaStore()) {
        return hiveMetaStore.createHiveMeta();
      }
    } catch (Exception e) {
      LOG.error("Create hive mate info failed.", e);
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public HiveTableMetaStore createTableMetaStore() {
    return new HiveTableMetaStore(metaStoreProperties);
  }

  public class HiveTableMetaStore implements TableMetaStoreFactory.TableMetaStore {
    private IMetaStoreClient client;
    private HiveConf hiveConf;

    private HiveTableMetaStore(Map<String, String> metaStoreProperties) {
      hiveConf = HiveMetaClientUtil.getHiveConf(metaStoreProperties);
    }

    private IMetaStoreClient getMetastoreClient() throws Exception {
      if (client == null) {
        client = initMetastoreClientWrapper();
      }
      SessionState.start(hiveConf);
      return client;
    }

    public StorageDescriptor getStorageDescriptor() {
      try {
        return getMetastoreClient().getTable(database, tableName).getSd();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public IMetaStoreClient initMetastoreClientWrapper() throws TException {
      try {
        return (IMetaStoreClient) RETRYER.call(() -> {

          LOG.info("get new hive metaclient and start session");
          return hiveShim.getHiveMetastoreClient(hiveConf);
        });
      } catch (ExecutionException | RetryException e) {
        Throwable cause = e.getCause();
        if (cause instanceof TException) {
          throw (TException) cause;
        }
        throw new RuntimeException("Error while calling getMetastoreClientWrapper. " + e.getMessage(), e);
      }
    }

    @Override
    public Path getLocationPath() {
      return new Path(getStorageDescriptor().getLocation());
    }

    @Override
    public Optional<Path> getPartition(
        LinkedHashMap<String, String> partSpec) throws Exception {
      try {
        return Optional.of(new Path(getMetastoreClient().getPartition(
                database,
                tableName,
                new ArrayList<>(partSpec.values()))
            .getSd().getLocation()));
      } catch (NoSuchObjectException ignore) {
        return Optional.empty();
      }
    }

    @Override
    public Path getPartitionPath(LinkedHashMap<String, String> partitionSpec) {
      Path partitionPath = getLocationPath();
      for (Map.Entry<String, String> spec : partitionSpec.entrySet()) {
        partitionPath = partitionPath.suffix("/" + spec.getKey() + "=" + spec.getValue());
      }
      return partitionPath;
    }

    @Override
    public void createPartition(LinkedHashMap<String, String> partSpec, Path path) throws Exception {
      StorageDescriptor newSd = new StorageDescriptor(getStorageDescriptor());
      newSd.setLocation(path.toString());
      Partition partition = HiveTableUtil.createHivePartition(database, tableName,
          new ArrayList<>(partSpec.values()), newSd, new HashMap<>());

      partition.setValues(new ArrayList<>(partSpec.values()));
      LOG.info("Add Hive Partition: " + partition);
      getMetastoreClient().add_partition(partition);
    }

    public Properties getTableProperties() {
      try {
        return (Properties) RETRYER.call(() -> RETRYER.call(() ->
            HiveMetaClientUtil.getHiveTableMetadata(hiveConf, database, tableName)));
      } catch (ExecutionException | RetryException e) {
        throw new RuntimeException("Error while calling getTableProperties. " + e.getMessage(), e);
      }
    }

    public HiveMeta createHiveMeta() {
      StorageDescriptor storageDescriptor = getStorageDescriptor();
      Map<String, String> tableSchema = getTableSchema(storageDescriptor);
      return new HiveMeta(tableSchema.get("columns"),
          tableSchema.get("columns.types"),
          storageDescriptor.getInputFormat(),
          storageDescriptor.getOutputFormat(),
          storageDescriptor.getSerdeInfo().getSerializationLib(),
          storageDescriptor.getSerdeInfo().getParameters());
    }

    public Map<String, String> getTableSchema(StorageDescriptor storageDescriptor) {
      try {
        return (Map<String, String>) RETRYER.call(() -> {
          Map<String, String> tableSchema = new HashMap<>();
          StringBuilder columnNames = new StringBuilder();
          StringBuilder columnTypes = new StringBuilder();

          List<FieldSchema> fields = storageDescriptor.getCols();
          for (FieldSchema fieldSchema : fields) {
            String name = fieldSchema.getName();
            String type = fieldSchema.getType();
            columnNames.append(name).append(",");
            columnTypes.append(type).append(":");
          }

          if (columnNames.length() > 0 && columnTypes.length() > 0) {
            columnNames.deleteCharAt(columnNames.length() - 1);
            columnTypes.deleteCharAt(columnTypes.length() - 1);
          }

          tableSchema.put("columns", columnNames.toString());
          tableSchema.put("columns.types", columnTypes.toString());

          return tableSchema;
        });
      } catch (ExecutionException | RetryException e) {
        throw new RuntimeException("Error while calling getTableSchema. " + e.getMessage(), e);
      }
    }

    public Map<String, String> getTableSchema() {
      return getTableSchema(getStorageDescriptor());
    }

    @Override
    public void close() throws IOException {
      LOG.info("recycle hive meta client after use");
      if (client != null) {
        client.close();
      }
    }
  }
}

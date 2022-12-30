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

package com.bytedance.bitsail.shaded.hive.client;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.constants.Constants;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.common.util.timelimit.FixedAttemptTimeLimit;

import com.bytedance.bitsail.shaded.hive.shim.HiveShim;
import com.bytedance.bitsail.shaded.hive.shim.HiveShimLoader;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Strings;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @desc: Common operations of hive metastore
 */
public class HiveMetaClientUtil {

  private static final Logger LOG = LoggerFactory.getLogger(HiveMetaClientUtil.class);
  public static final int DEFAULT_DURATION = 15;
  private static final Retryer<Object> RETRYER = RetryerBuilder.newBuilder()
      .retryIfException()
      .withRetryListener(new RetryListener() {
        @Override
        public <V> void onRetry(Attempt<V> attempt) {
          if (attempt.hasException()) {
            LOG.error("Retry get hive meta client failed.", attempt.getExceptionCause());
          }
        }
      })
      .withAttemptTimeLimiter(new FixedAttemptTimeLimit(DEFAULT_DURATION, TimeUnit.MINUTES))
      .withWaitStrategy(WaitStrategies.fixedWait(Constants.RETRY_DELAY, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(Constants.RETRY_TIMES))
      .build();
  private static HiveShim hiveShim = null;

  public static synchronized void init() {
    if (hiveShim == null) {
      hiveShim = HiveShimLoader.loadHiveShim();
    }
  }

  public static HiveShim getHiveShim() {
    if (hiveShim == null) {
      init();
    }
    return hiveShim;
  }

  public static Pair<String, String> getTableSchema(HiveConf hiveConf,
                                                    String db, String tableName)
      throws TException {
    try {
      return (Pair<String, String>) RETRYER.call(() -> {
        StringBuilder columnNames = new StringBuilder();
        StringBuilder columnTypes = new StringBuilder();

        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          List<FieldSchema> fields = client.getFields(db, tableName);
          for (FieldSchema fieldSchema : fields) {
            String name = fieldSchema.getName();
            String type = fieldSchema.getType();
            columnNames.append(name).append(",");
            columnTypes.append(type).append(":");
          }
        } finally {
          if (null != client) {
            client.close();
          }
        }
        if (columnNames.length() > 0 && columnTypes.length() > 0) {
          columnNames.deleteCharAt(columnNames.length() - 1);
          columnTypes.deleteCharAt(columnTypes.length() - 1);
        }

        return new Pair<String, String>(columnNames.toString(), columnTypes.toString());
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static void addColumnInfos(HiveConf hiveConf,
                                    String database,
                                    String table,
                                    List<ColumnInfo> columns) {
    //todo
  }

  public static void updateColumnInfos(HiveConf hiveConf,
                                       String database,
                                       String table,
                                       List<ColumnInfo> columns) {
    //todo
  }

  public static List<ColumnInfo> getColumnInfo(HiveConf hiveConf,
                                               String db, String tableName) throws TException {
    LOG.info("start to fetch hive column info...");
    try {
      return (List<ColumnInfo>) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          List<FieldSchema> fields = client.getFields(db, tableName);
          LOG.info("finished fetch hive column info.");
          return fields.stream().map(fieldSchema -> {
            String name = fieldSchema.getName();
            String type = fieldSchema.getType();
            String comment = fieldSchema.getComment();
            return new ColumnInfo(name, type, comment);
          }).collect(Collectors.toList());

        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static StorageDescriptor getTableFormat(HiveConf hiveConf,
                                                 String db, String tableName) throws TException {
    try {
      return (StorageDescriptor) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table table = client.getTable(db, tableName);
          return table.getSd();
        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static Map<String, String> getSerdeParameters(HiveConf hiveConf,
                                                       String db, String tableName) throws TException {
    try {
      return (Map<String, String>) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table table = client.getTable(db, tableName);
          StorageDescriptor sd = table.getSd();
          return sd.getSerdeInfo().getParameters();
        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static Map<String, String> getPartitionKeys(HiveConf hiveConf,
                                                     String db, String tableName) throws TException {
    try {
      return (Map<String, String>) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table table = client.getTable(db, tableName);
          List<FieldSchema> partitions = table.getPartitionKeys();
          if (partitions.size() > 0) {
            return partitions.stream().collect(Collectors.toMap(FieldSchema::getName, FieldSchema::getType));
          }
          return new HashMap<>();
        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static String getTablePath(HiveConf hiveConf,
                                    String db, String tableName, String partition)
      throws TException {
    try {
      return (String) RETRYER.call(() -> {
        long st = System.currentTimeMillis();

        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table t = client.getTable(db, tableName);
          StringBuilder location = new StringBuilder(t.getSd().getLocation());
          if (null != partition) {
            String[] parts = partition.split(",");
            location.append("/").append(String.join("/", parts));
          }
          LOG.info("fetch database: {} table: {} location: {}, Taken: {} sec.", db, tableName,
              location, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st));

          return location.toString();
        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static List<String> getPartitionPathList(HiveConf hiveConf,
                                                  String db, String tableName, String partition)
      throws TException {
    try {
      return (List<String>) RETRYER.call(() -> {
        long st = System.currentTimeMillis();

        List<String> pathList = new ArrayList<>();
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table t = client.getTable(db, tableName);
          String location = t.getSd().getLocation();
          if (null != partition) {
            String[] partitionPaths = partition.split(",");
            for (String partitionPath : partitionPaths) {
              String tablePath = location + "/" + partitionPath.trim();
              pathList.add(tablePath);
            }
          } else {
            pathList.add(location);
          }
          LOG.info("fetch {}.{} partition path location: {}, Taken: {} sec", db, tableName, location, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st));
          return pathList;
        } finally {
          if (null != client) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static void dropPartition(HiveConf hiveConf,
                                   String db, String tableName, String partition)
      throws TException {
    try {
      RETRYER.call(() -> {
        long st = System.currentTimeMillis();

        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          List<String> partVals = new ArrayList<String>();
          String[] parts = partition.split(",");
          for (String part : parts) {
            String[] kv = part.split("=");
            partVals.add(kv[1]);
          }
          client.dropPartition(db, tableName, partVals, true);
          LOG.info("[Hive Operation] Drop table partition {}.{}({}) finished. Taken: {}.", db, tableName, partVals,
              TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
        } finally {
          if (null != client) {
            client.close();
          }
        }
        return null;
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema.", e);
    }
  }

  public static Properties getHiveTableMetadata(HiveConf hiveConf, String db, String tableName) throws TException {
    try {
      return (Properties) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table table = client.getTable(db, tableName);
          try {
            Method method = hiveShim.getMetaStoreUtilsClass().getMethod("getTableMetadata", Table.class);
            return (Properties) method.invoke(null, table);
          } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Failed to invoke MetaStoreUtils.getTableMetadata()", e);
          }
        } finally {
          if (client != null) {
            client.close();
          }
        }
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getTableSchema. " + e.getMessage(), e);
    }
  }

  public static boolean hasPartition(HiveConf hiveConf,
                                     String db, String tableName, String partition)
      throws TException {
    try {
      return (Boolean) RETRYER.call(() -> {
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          List<String> partVals = new ArrayList<String>();
          String[] parts = partition.split(",");
          for (String part : parts) {
            String[] kv = part.split("=");
            partVals.add(kv[1]);
          }
          Partition p = client.getPartition(db, tableName, partVals);
        } catch (NoSuchObjectException nsoe) {
          return false;
        } finally {
          if (null != client) {
            client.close();
          }
        }
        return true;
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::hasPartition. " + e.getMessage(), e);
    }
  }

  public static void addPartition(HiveConf hiveConf,
                                  String db,
                                  String tableName,
                                  String partitions,
                                  String location,
                                  long numRows)
      throws TException {
    try {
      RETRYER.call(() -> {
        LOG.info("Start to add partition " + partitions + " to " + db + "." + tableName + " on location " + location);
        long st = System.currentTimeMillis();
        IMetaStoreClient client = null;
        try {
          client = getMetastoreClient(hiveConf);
          Table table = client.getTable(db, tableName);
          String[] parts = partitions.split(",");
          List<String> vals = new ArrayList<>();
          for (String part : parts) {
            String[] kv = part.split("=");
            vals.add(kv[1]);
          }

          StorageDescriptor sd = table.getSd();
          sd.setLocation(location);
          Map<String, String> parameters = new HashMap<>();
          if (numRows >= 0) {
            parameters.put("numRows", String.valueOf(numRows));
            parameters.put("STATS_GENERATED_VIA_STATS_TASK", "true");
          }
          Partition p = new Partition(vals, db, tableName, (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
              (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), sd, parameters);
          try {
            client.add_partition(p);
          } catch (AlreadyExistsException alreadyExistsException) {
            LOG.warn("Partition already exists, we will ignore it!", alreadyExistsException);
          }

          LOG.info("Add partitions {} to {}.{} Finished. Taken: {} sec", partitions, db, table, +TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st));
        } finally {
          if (null != client) {
            client.close();
          }
        }
        return null;
      });
    } catch (ExecutionException | RetryException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof TException) {
        throw (TException) cause;
      }
      throw new RuntimeException("Error while calling HiveMetaClientUtil::hasPartition. " + e.getMessage(), e);
    }
  }

  public static IMetaStoreClient getMetastoreClient(HiveConf hiveConf) {
    try {
      if (hiveShim == null) {
        init();
      }
      return hiveShim.getHiveMetastoreClient(hiveConf);
    } catch (Exception e) {
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getMetastoreClient. " + e.getMessage(), e);
    }
  }

  public static HiveConf getHiveConf(String location) {
    try {
      HiveConf hiveConf = new HiveConf();
      hiveConf.addResource(new org.apache.hadoop.fs.Path(location));
      return hiveConf;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static HiveConf getHiveConf(Map<String, String> properties) {
    try {
      HiveConf hiveConf = new HiveConf();
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        hiveConf.verifyAndSet(entry.getKey(), Strings.nullToEmpty(entry.getValue()));
      }
      return hiveConf;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Map<String, Integer> getColumnMapping(Pair<String, String> hiveSchema) {
    try {
      return (Map<String, Integer>) RETRYER.call(() -> {
        String[] columnNames = hiveSchema.getFirst().split(",");
        Map<String, Integer> columnMapping = new HashMap<String, Integer>();
        for (int i = 0; i < columnNames.length; i++) {
          columnMapping.put(columnNames[i].toUpperCase(), i);
        }
        return columnMapping;
      });
    } catch (ExecutionException | RetryException e) {
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getColumnMapping. " + e.getMessage(), e);
    }
  }

}

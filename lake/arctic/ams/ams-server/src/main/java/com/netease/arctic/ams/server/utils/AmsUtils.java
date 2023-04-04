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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.model.AMSColumnInfo;
import com.netease.arctic.ams.server.model.AMSTransactionsOfTable;
import com.netease.arctic.ams.server.model.BaseMajorCompactRecord;
import com.netease.arctic.ams.server.model.CompactRangeType;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.ams.server.model.TransactionsOfTable;
import org.apache.hadoop.hive.metastore.api.FieldSchema;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.CLUSTER_NAME;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.HA_ENABLE;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.THRIFT_BIND_HOST;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.THRIFT_BIND_PORT;
import static com.netease.arctic.ams.server.config.ArcticMetaStoreConf.ZOOKEEPER_SERVER;

public class AmsUtils {
  public static TableIdentifier toTableIdentifier(com.netease.arctic.table.TableIdentifier tableIdentifier) {
    if (tableIdentifier == null) {
      return null;
    }
    return new TableIdentifier(tableIdentifier.getCatalog(), tableIdentifier.getDatabase(),
        tableIdentifier.getTableName());
  }

  public static AMSTransactionsOfTable toTransactionsOfTable(
      TransactionsOfTable info) {
    AMSTransactionsOfTable transactionsOfTable = new AMSTransactionsOfTable();
    transactionsOfTable.setTransactionId(info.getTransactionId() + "");
    transactionsOfTable.setFileCount(info.getFileCount());
    transactionsOfTable.setFileSize(byteToXB(info.getFileSize()));
    transactionsOfTable.setCommitTime(info.getCommitTime());
    transactionsOfTable.setSnapshotId(info.getTransactionId() + "");
    return transactionsOfTable;
  }

  public static BaseMajorCompactRecord transferToBaseMajorCompactRecord(
      OptimizeHistory record) {
    BaseMajorCompactRecord result = new BaseMajorCompactRecord();
    result.setTableIdentifier(record.getTableIdentifier());
    result.setOptimizeType(record.getOptimizeType());
    result.setCompactRange(toCompactRange(record.getOptimizeRange()));
    result.setVisibleTime(record.getVisibleTime());
    result.setPlanTime(record.getPlanTime());
    result.setDuration(record.getDuration());
    result.setTotalFilesStatBeforeCompact(record.getTotalFilesStatBeforeOptimize());
    result.setTotalFilesStatAfterCompact(record.getTotalFilesStatAfterOptimize());
    result.setCommitTime(record.getCommitTime());
    result.setRecordId(record.getRecordId());
    return result;
  }

  private static CompactRangeType toCompactRange(com.netease.arctic.ams.api.OptimizeRangeType compactRange) {
    if (compactRange == null) {
      return null;
    }
    return CompactRangeType.valueOf(compactRange.name());
  }

  public static Long longOrNull(String value) {
    if (value == null) {
      return null;
    } else {
      return Long.parseLong(value);
    }
  }

  /**
   * Convert size to a different unit, ensuring that the converted value is > 1
   */
  public static String byteToXB(long size) {
    String[] units = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
    float result = size, tmpResult = size;
    int unitIdx = 0;
    int unitCnt = units.length;
    while (true) {
      result = result / 1024;
      if (result < 1 || unitIdx >= unitCnt - 1) {
        return String.format("%2.2f%s", tmpResult, units[unitIdx]);
      }
      tmpResult = result;
      unitIdx += 1;
    }
  }

  public static String getFileName(String path) {
    return path == null ? null : new File(path).getName();
  }

  public static List<AMSColumnInfo> transforHiveSchemaToAMSColumnInfos(List<FieldSchema> fields) {
    return fields.stream()
        .map(f -> {
          AMSColumnInfo columnInfo = new AMSColumnInfo();
          columnInfo.setField(f.getName());
          columnInfo.setType(f.getType());
          columnInfo.setComment(f.getComment());
          return columnInfo;
        }).collect(Collectors.toList());
  }

  public static Map<String, String> getNotDeprecatedAndNotInternalStaticFields(Class<?> clazz)
      throws IllegalAccessException {

    List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
        // filter out the non-static fields
        .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
        .filter(f -> f.getAnnotation(Deprecated.class) == null)

        // collect to list
        .collect(Collectors.toList());

    Map<String, String> result = new HashMap<>();
    for (Field field : fields) {
      field.setAccessible(true);
      result.put(field.getName(), field.get(null) + "");
    }
    return result;
  }

  public static String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter printWriter = new PrintWriter(sw);

    try {
      throwable.printStackTrace(printWriter);
      return sw.toString();
    } finally {
      printWriter.close();
    }
  }

  public static InetAddress getLocalHostExactAddress(String prefix) {
    if (prefix.startsWith("0")) {
      throw new RuntimeException("config " + ArcticMetaStoreConf.THRIFT_BIND_HOST_PREFIX.key() + " can't start with 0");
    }
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface iface = networkInterfaces.nextElement();
        for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
          InetAddress inetAddr = inetAddrs.nextElement();
          if (checkHostAddress(inetAddr, prefix)) {
            return inetAddr;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getAMSHaAddress() {
    if (ArcticMetaStore.conf.getBoolean(HA_ENABLE)) {
      return "zookeeper://" + ArcticMetaStore.conf.getString(ZOOKEEPER_SERVER) + "/" +
          ArcticMetaStore.conf.getString(CLUSTER_NAME) + "/";
    } else {
      return "thrift://" + ArcticMetaStore.conf.getString(THRIFT_BIND_HOST) + ":" +
          ArcticMetaStore.conf.getInteger(THRIFT_BIND_PORT) + "/";
    }
  }

  public static String formatString(String beforeFormat) {
    StringBuilder result = new StringBuilder();
    int flag = 0;
    for (int i = 0; i < beforeFormat.length(); i++) {
      String s = String.valueOf(beforeFormat.toCharArray()[i]);
      if (!s.matches("[a-zA-Z]+")) {
        flag = 0;
        result.append(" ");
      } else {
        result.append(flag == 0 ? s.toUpperCase() : s.toLowerCase());
        flag++;
      }
    }
    return result.toString();
  }

  private static boolean checkHostAddress(InetAddress address, String prefix) {
    return address != null && address.getHostAddress().startsWith(prefix);
  }
}

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

package com.netease.arctic.server.dashboard.utils;

import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.server.dashboard.model.AMSColumnInfo;
import com.netease.arctic.server.dashboard.model.AMSTransactionsOfTable;
import com.netease.arctic.server.dashboard.model.TransactionsOfTable;
import com.netease.arctic.server.utils.Configurations;
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

import static com.netease.arctic.server.ArcticManagementConf.HA_CLUSTER_NAME;
import static com.netease.arctic.server.ArcticManagementConf.HA_ENABLE;
import static com.netease.arctic.server.ArcticManagementConf.HA_ZOOKEEPER_ADDRESS;
import static com.netease.arctic.server.ArcticManagementConf.OPTIMIZING_SERVICE_THRIFT_BIND_PORT;
import static com.netease.arctic.server.ArcticManagementConf.SERVER_EXPOSE_HOST;
import static com.netease.arctic.server.ArcticManagementConf.TABLE_SERVICE_THRIFT_BIND_PORT;

public class AmsUtil {

  private static final String ZOOKEEPER_ADDRESS_FORMAT = "zookeeper://%s/%s";
  private static final String THRIFT_ADDRESS_FORMAT = "thrift://%s:%s";

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
        .filter(f -> Modifier.isStatic(f.getModifiers()))
        .filter(f -> f.getAnnotation(Deprecated.class) == null)
        // collect to list
        .collect(Collectors.toList());

    Map<String, String> result = new HashMap<>();
    for (Field field : fields) {
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

  public static InetAddress lookForBindHost(String prefix) {
    if (prefix.startsWith("0")) {
      throw new RuntimeException(
          "config " + SERVER_EXPOSE_HOST.key() + " can't start with 0");
    }
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        for (Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();
             enumeration.hasMoreElements(); ) {
          InetAddress inetAddress = enumeration.nextElement();
          if (checkHostAddress(inetAddress, prefix)) {
            return inetAddress;
          }
        }
      }
      throw new IllegalArgumentException("Can't find host address start with " + prefix);
    } catch (Exception e) {
      throw new RuntimeException("Look for bind host failed", e);
    }
  }

  public static String getAMSThriftAddress(Configurations conf, String serviceName) {
    if (conf.getBoolean(HA_ENABLE)) {
      return String.format(ZOOKEEPER_ADDRESS_FORMAT, conf.getString(HA_ZOOKEEPER_ADDRESS),
          conf.getString(HA_CLUSTER_NAME));
    } else {
      if (Constants.THRIFT_TABLE_SERVICE_NAME.equals(serviceName)) {
        return String.format(THRIFT_ADDRESS_FORMAT, conf.getString(SERVER_EXPOSE_HOST),
            conf.getInteger(TABLE_SERVICE_THRIFT_BIND_PORT));
      } else if (Constants.THRIFT_OPTIMIZING_SERVICE_NAME.equals(serviceName)) {
        return String.format(THRIFT_ADDRESS_FORMAT, conf.getString(SERVER_EXPOSE_HOST),
            conf.getInteger(OPTIMIZING_SERVICE_THRIFT_BIND_PORT));
      } else {
        throw new IllegalArgumentException(String.format("Unknown service name %s", serviceName));
      }
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

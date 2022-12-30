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

package com.bytedance.bitsail.connector.hbase;

import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.hbase.auth.KerberosAuthenticator;

import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Objects;

public class HBaseHelper {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseHelper.class);

  /**
   * Get connection with hbase.
   * @param hbaseConfigMap HBase configuration.
   */
  public static org.apache.hadoop.hbase.client.Connection getHbaseConnection(Map<String, Object> hbaseConfigMap) {
    Preconditions.checkState(MapUtils.isNotEmpty(hbaseConfigMap), "HBaseConfig cannot be empty!");

    if (KerberosAuthenticator.openKerberos(hbaseConfigMap)) {
      return getConnectionWithKerberos(hbaseConfigMap);
    }

    try {
      Configuration hbaseConfiguration = getConfig(hbaseConfigMap);
      return ConnectionFactory.createConnection(hbaseConfiguration);
    } catch (IOException e) {
      LOG.error("Get connection fail with config:{}", hbaseConfigMap);
      throw new RuntimeException(e);
    }
  }

  /**
   * Get hadoop configuration for hbase.
   * @param hbaseConfigMap User defined hbase configurations.
   * @return A hadoop configuration.
   */
  public static Configuration getConfig(Map<String, Object> hbaseConfigMap) {
    Configuration hbaseConfiguration = HBaseConfiguration.create();
    if (MapUtils.isEmpty(hbaseConfigMap)) {
      return hbaseConfiguration;
    }

    for (Map.Entry<String, Object> entry : hbaseConfigMap.entrySet()) {
      if (entry.getValue() != null && !(entry.getValue() instanceof Map)) {
        hbaseConfiguration.set(entry.getKey(), entry.getValue().toString());
      }
    }
    return hbaseConfiguration;
  }

  /**
   * Close mutator.
   */
  public static void closeBufferedMutator(BufferedMutator bufferedMutator) {
    closeWithException(bufferedMutator);
  }

  /**
   * Close hbase connection.
   */
  public static void closeConnection(Connection hbaseConnection) {
    closeWithException(hbaseConnection);
  }

  /**
   * Login kerberos based on configurations, and then get connection with hbase.
   * @param hbaseConfigMap HBase configuration.
   * @return Connection with hbase.
   */
  private static org.apache.hadoop.hbase.client.Connection getConnectionWithKerberos(Map<String, Object> hbaseConfigMap) {
    try {
      UserGroupInformation ugi = KerberosAuthenticator.getUgi(hbaseConfigMap);
      return ugi.doAs((PrivilegedAction<Connection>) () -> {
        try {
          Configuration hbaseConfiguration = getConfig(hbaseConfigMap);
          return ConnectionFactory.createConnection(hbaseConfiguration);
        } catch (IOException e) {
          LOG.error("Get connection fail with config:{}", hbaseConfigMap);
          throw new RuntimeException(e);
        }
      });
    } catch (Exception e) {
      throw new RuntimeException("Login kerberos error", e);
    }
  }

  /**
   * Close an object and throw exception if failed to close.
   */
  private static void closeWithException(Closeable closeable) {
    try {
      if (Objects.nonNull(closeable)) {
        closeable.close();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to close.", e);
    }
  }
}

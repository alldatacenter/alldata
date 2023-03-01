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

package org.apache.uniffle.common.config;

import java.util.List;
import java.util.Map;

import org.apache.uniffle.common.ClientType;

public class RssBaseConf extends RssConf {

  public static final ConfigOption<String> RSS_COORDINATOR_QUORUM = ConfigOptions
      .key("rss.coordinator.quorum")
      .stringType()
      .noDefaultValue()
      .withDescription("Coordinator quorum");

  public static final ConfigOption<String> RPC_SERVER_TYPE = ConfigOptions
      .key("rss.rpc.server.type")
      .stringType()
      .defaultValue("GRPC")
      .withDescription("Shuffle server type, default is grpc");

  public static final ConfigOption<Integer> RPC_SERVER_PORT = ConfigOptions
      .key("rss.rpc.server.port")
      .intType()
      .defaultValue(19999)
      .withDescription("Shuffle server service port");

  public static final ConfigOption<Boolean> RPC_METRICS_ENABLED = ConfigOptions
      .key("rss.rpc.metrics.enabled")
      .booleanType()
      .defaultValue(true)
      .withDescription("If enable metrics for rpc connection");

  public static final ConfigOption<Integer> JETTY_HTTP_PORT = ConfigOptions
      .key("rss.jetty.http.port")
      .intType()
      .defaultValue(19998)
      .withDescription("jetty http port");

  public static final ConfigOption<Integer> JETTY_CORE_POOL_SIZE = ConfigOptions
      .key("rss.jetty.corePool.size")
      .intType()
      .defaultValue(256)
      .withDescription("jetty corePool size");

  public static final ConfigOption<Integer> JETTY_MAX_POOL_SIZE = ConfigOptions
      .key("rss.jetty.maxPool.size")
      .intType()
      .defaultValue(256)
      .withDescription("jetty max pool size");

  public static final ConfigOption<Boolean> JETTY_SSL_ENABLE = ConfigOptions
      .key("rss.jetty.ssl.enable")
      .booleanType()
      .defaultValue(false)
      .withDescription("jetty ssl enable");

  public static final ConfigOption<Integer> JETTY_HTTPS_PORT = ConfigOptions
      .key("rss.jetty.https.port")
      .intType()
      .noDefaultValue()
      .withDescription("jetty https port");

  public static final ConfigOption<String> JETTY_SSL_KEYSTORE_PATH = ConfigOptions
      .key("rss.jetty.ssl.keystore.path")
      .stringType()
      .noDefaultValue()
      .withDescription("jetty ssl keystore path");

  public static final ConfigOption<String> JETTY_SSL_KEYMANAGER_PASSWORD = ConfigOptions
      .key("rss.jetty.ssl.keymanager.password")
      .stringType()
      .noDefaultValue()
      .withDescription("jetty ssl keymanager password");

  public static final ConfigOption<String> JETTY_SSL_KEYSTORE_PASSWORD = ConfigOptions
      .key("rss.jetty.ssl.keystore.password")
      .stringType()
      .noDefaultValue()
      .withDescription("jetty ssl keystore password");

  public static final ConfigOption<String> JETTY_SSL_TRUSTSTORE_PASSWORD = ConfigOptions
      .key("rss.jetty.ssl.truststore.password")
      .stringType()
      .noDefaultValue()
      .withDescription("jetty ssl truststore password");

  public static final ConfigOption<Long> JETTY_STOP_TIMEOUT = ConfigOptions
      .key("rss.jetty.stop.timeout")
      .longType()
      .defaultValue(30 * 1000L)
      .withDescription("jetty stop timeout (ms) ");

  public static final ConfigOption<Long> JETTY_HTTP_IDLE_TIMEOUT = ConfigOptions
      .key("rss.jetty.http.idle.timeout")
      .longType()
      .defaultValue(30 * 1000L)
      .withDescription("jetty http idle timeout (ms) ");

  public static final ConfigOption<Long> RPC_MESSAGE_MAX_SIZE = ConfigOptions
      .key("rss.rpc.message.max.size")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_INTEGER_VALIDATOR,
        "The value must be positive integer")
      .defaultValue(1024L * 1024L * 1024L)
      .withDescription("Max size of rpc message (byte)");

  public static final ConfigOption<ClientType> RSS_CLIENT_TYPE = ConfigOptions
      .key("rss.rpc.client.type")
      .enumType(ClientType.class)
      .defaultValue(ClientType.GRPC)
      .withDescription("client type for rss");

  public static final ConfigOption<String> RSS_STORAGE_TYPE = ConfigOptions
      .key("rss.storage.type")
      .stringType()
      .noDefaultValue()
      .withDescription("Data storage for remote shuffle service");

  public static final ConfigOption<Integer> RSS_STORAGE_DATA_REPLICA = ConfigOptions
      .key("rss.storage.data.replica")
      .intType()
      .defaultValue(1)
      .withDescription("Data replica in storage");

  public static final ConfigOption<List<String>> RSS_STORAGE_BASE_PATH = ConfigOptions
      .key("rss.storage.basePath")
      .stringType()
      .asList()
      .noDefaultValue()
      .withDescription("Common storage path for remote shuffle data");

  public static final ConfigOption<Integer> RPC_EXECUTOR_SIZE = ConfigOptions
      .key("rss.rpc.executor.size")
      .intType()
      .defaultValue(1000)
      .withDescription("Thread number for grpc to process request");

  public static final ConfigOption<Boolean> RSS_JVM_METRICS_VERBOSE_ENABLE = ConfigOptions
      .key("rss.jvm.metrics.verbose.enable")
      .booleanType()
      .defaultValue(true)
      .withDescription("The switch for jvm metrics verbose");

  public static final ConfigOption<Boolean> RSS_SECURITY_HADOOP_KERBEROS_ENABLE = ConfigOptions
      .key("rss.security.hadoop.kerberos.enable")
      .booleanType()
      .defaultValue(false)
      .withDescription("Whether enable visiting secured hadoop cluster.");

  public static final ConfigOption<String> RSS_SECURITY_HADOOP_KRB5_CONF_FILE = ConfigOptions
      .key("rss.security.hadoop.kerberos.krb5-conf.file")
      .stringType()
      .noDefaultValue()
      .withDescription("The file path of krb5.conf. And only when "
          + RSS_SECURITY_HADOOP_KERBEROS_ENABLE.key() + " enabled, the option will be valid.");

  public static final ConfigOption<String> RSS_SECURITY_HADOOP_KERBEROS_KEYTAB_FILE = ConfigOptions
      .key("rss.security.hadoop.kerberos.keytab.file")
      .stringType()
      .noDefaultValue()
      .withDescription("The kerberos keytab file path. And only when "
          + RSS_SECURITY_HADOOP_KERBEROS_ENABLE.key() + " enabled, the option will be valid.");

  public static final ConfigOption<String> RSS_SECURITY_HADOOP_KERBEROS_PRINCIPAL = ConfigOptions
      .key("rss.security.hadoop.kerberos.principal")
      .stringType()
      .noDefaultValue()
      .withDescription("The kerberos keytab principal. And only when "
          + RSS_SECURITY_HADOOP_KERBEROS_ENABLE.key() + " enabled, the option will be valid.");

  public static final ConfigOption<Long> RSS_SECURITY_HADOOP_KERBEROS_RELOGIN_INTERVAL_SEC = ConfigOptions
      .key("rss.security.hadoop.kerberos.relogin.interval.sec")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_INTEGER_VALIDATOR, "The value must be positive integer")
      .defaultValue(60L)
      .withDescription("The kerberos authentication relogin interval. unit: sec");

  public static final ConfigOption<Boolean> RSS_TEST_MODE_ENABLE = ConfigOptions
      .key("rss.test.mode.enable")
      .booleanType()
      .defaultValue(false)
      .withDescription("Whether enable test mode for the shuffle server.");

  public static final ConfigOption<String> RSS_METRICS_REPORTER_CLASS = ConfigOptions
      .key("rss.metrics.reporter.class")
      .stringType()
      .noDefaultValue()
      .withDescription("The class of metrics reporter.");

  public static final ConfigOption<Long> RSS_RECONFIGURE_INTERVAL_SEC = ConfigOptions
      .key("rss.reconfigure.interval.sec")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR, "The value must be posite long")
      .defaultValue(5L)
      .withDescription("Reconfigure check interval.");

  public boolean loadCommonConf(Map<String, String> properties) {
    if (properties == null) {
      return false;
    }

    List<ConfigOption<Object>> configOptions = ConfigUtils.getAllConfigOptions(RssBaseConf.class);
    properties.forEach((k, v) -> {
      configOptions.forEach(config -> {
        if (config.key().equalsIgnoreCase(k)) {
          set(config, ConfigUtils.convertValue(v, config.getClazz()));
        }
      });
    });

    return true;
  }

}

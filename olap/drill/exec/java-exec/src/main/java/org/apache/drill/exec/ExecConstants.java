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
package org.apache.drill.exec;

import java.util.Arrays;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.physical.impl.common.HashTable;
import org.apache.drill.exec.rpc.user.InboundImpersonationManager;
import org.apache.drill.exec.server.options.OptionValidator;
import org.apache.drill.exec.server.options.OptionValidator.OptionDescription;
import org.apache.drill.exec.server.options.TypeValidators.AdminUserGroupsValidator;
import org.apache.drill.exec.server.options.TypeValidators.AdminUsersValidator;
import org.apache.drill.exec.server.options.TypeValidators.BooleanValidator;
import org.apache.drill.exec.server.options.TypeValidators.DateTimeFormatValidator;
import org.apache.drill.exec.server.options.TypeValidators.DoubleValidator;
import org.apache.drill.exec.server.options.TypeValidators.EnumeratedStringValidator;
import org.apache.drill.exec.server.options.TypeValidators.IntegerValidator;
import org.apache.drill.exec.server.options.TypeValidators.LongValidator;
import org.apache.drill.exec.server.options.TypeValidators.MaxWidthValidator;
import org.apache.drill.exec.server.options.TypeValidators.PositiveLongValidator;
import org.apache.drill.exec.server.options.TypeValidators.PowerOfTwoLongValidator;
import org.apache.drill.exec.server.options.TypeValidators.RangeDoubleValidator;
import org.apache.drill.exec.server.options.TypeValidators.RangeLongValidator;
import org.apache.drill.exec.server.options.TypeValidators.StringValidator;
import org.apache.drill.exec.store.parquet.ParquetFormatPlugin;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.vector.ValueVector;

public final class ExecConstants {
  private ExecConstants() {
    // Don't allow instantiation
  }

  public static final String ZK_RETRY_TIMES = "drill.exec.zk.retry.count";
  public static final String ZK_RETRY_DELAY = "drill.exec.zk.retry.delay";
  public static final String ZK_CONNECTION = "drill.exec.zk.connect";
  public static final String ZK_TIMEOUT = "drill.exec.zk.timeout";
  public static final String ZK_ROOT = "drill.exec.zk.root";
  public static final String ZK_REFRESH = "drill.exec.zk.refresh";
  public static final String ZK_ACL_PROVIDER = "drill.exec.zk.acl_provider";
  public static final String ZK_APPLY_SECURE_ACL = "drill.exec.zk.apply_secure_acl";
  public static final String BIT_RETRY_TIMES = "drill.exec.rpc.bit.server.retry.count";
  public static final String BIT_RETRY_DELAY = "drill.exec.rpc.bit.server.retry.delay";
  public static final String BIT_TIMEOUT = "drill.exec.bit.timeout";
  public static final String SERVICE_NAME = "drill.exec.cluster-id";
  public static final String INITIAL_BIT_PORT = "drill.exec.rpc.bit.server.port";
  public static final String INITIAL_DATA_PORT = "drill.exec.rpc.bit.server.dataport";
  public static final String BIT_RPC_TIMEOUT = "drill.exec.rpc.bit.timeout";
  public static final String INITIAL_USER_PORT = "drill.exec.rpc.user.server.port";
  public static final String USER_RPC_TIMEOUT = "drill.exec.rpc.user.timeout";
  public static final String METRICS_CONTEXT_NAME = "drill.exec.metrics.context";
  public static final String USE_IP_ADDRESS = "drill.exec.rpc.use.ip";
  public static final String CLIENT_RPC_THREADS = "drill.exec.rpc.user.client.threads";
  public static final String BIT_SERVER_RPC_THREADS = "drill.exec.rpc.bit.server.threads";
  public static final String USER_SERVER_RPC_THREADS = "drill.exec.rpc.user.server.threads";
  public static final String FRAG_RUNNER_RPC_TIMEOUT = "drill.exec.rpc.fragrunner.timeout";
  public static final PositiveLongValidator FRAG_RUNNER_RPC_TIMEOUT_VALIDATOR = new PositiveLongValidator(FRAG_RUNNER_RPC_TIMEOUT, Long.MAX_VALUE, null);
  public static final String TRACE_DUMP_DIRECTORY = "drill.exec.trace.directory";
  public static final String TRACE_DUMP_FILESYSTEM = "drill.exec.trace.filesystem";
  public static final String TEMP_DIRECTORIES = "drill.exec.tmp.directories";
  public static final String TEMP_FILESYSTEM = "drill.exec.tmp.filesystem";
  public static final String INCOMING_BUFFER_IMPL = "drill.exec.buffer.impl";
  /** incoming buffer size (number of batches) */
  public static final String INCOMING_BUFFER_SIZE = "drill.exec.buffer.size";
  public static final String SPOOLING_BUFFER_DELETE = "drill.exec.buffer.spooling.delete";
  public static final String SPOOLING_BUFFER_MEMORY = "drill.exec.buffer.spooling.size";
  public static final String UNLIMITED_BUFFER_MAX_MEMORY_SIZE = "drill.exec.buffer.unlimited_receiver.max_size";
  public static final String BATCH_PURGE_THRESHOLD = "drill.exec.sort.purge.threshold";

  // Spill boot-time Options common to all spilling operators
  // (Each individual operator may override the common options)

  public static final String SPILL_FILESYSTEM = "drill.exec.spill.fs";
  public static final String SPILL_DIRS = "drill.exec.spill.directories";

  public static final String OUTPUT_BATCH_SIZE = "drill.exec.memory.operator.output_batch_size";
  // Output Batch Size in Bytes. We have a small lower bound so we can test with unit tests without the
  // need to produce very large batches that take up lot of memory.
  public static final LongValidator OUTPUT_BATCH_SIZE_VALIDATOR = new RangeLongValidator(OUTPUT_BATCH_SIZE, 128, 512 * 1024 * 1024,
      new OptionDescription("Available as of Drill 1.13. Limits the amount of memory that the Flatten, Merge Join, and External Sort operators allocate to outgoing batches."));

  // Based on available memory, adjust output batch size for buffered operators by this factor.
  public static final String OUTPUT_BATCH_SIZE_AVAIL_MEM_FACTOR = "drill.exec.memory.operator.output_batch_size_avail_mem_factor";
  public static final DoubleValidator OUTPUT_BATCH_SIZE_AVAIL_MEM_FACTOR_VALIDATOR = new RangeDoubleValidator(OUTPUT_BATCH_SIZE_AVAIL_MEM_FACTOR, 0.01, 1.0,
      new OptionDescription("Based on the available system memory, adjusts the output batch size for buffered operators by the factor set."));

  // External Sort Boot configuration

  public static final String EXTERNAL_SORT_TARGET_SPILL_BATCH_SIZE = "drill.exec.sort.external.spill.batch.size";
  public static final String EXTERNAL_SORT_SPILL_GROUP_SIZE = "drill.exec.sort.external.spill.group.size";
  public static final String EXTERNAL_SORT_SPILL_THRESHOLD = "drill.exec.sort.external.spill.threshold";
  public static final String EXTERNAL_SORT_SPILL_DIRS = "drill.exec.sort.external.spill.directories";
  public static final String EXTERNAL_SORT_SPILL_FILESYSTEM = "drill.exec.sort.external.spill.fs";
  public static final String EXTERNAL_SORT_SPILL_FILE_SIZE = "drill.exec.sort.external.spill.file_size";
  public static final String EXTERNAL_SORT_MSORT_MAX_BATCHSIZE = "drill.exec.sort.external.msort.batch.maxsize";
  public static final String EXTERNAL_SORT_MERGE_LIMIT = "drill.exec.sort.external.merge_limit";
  public static final String EXTERNAL_SORT_SPILL_BATCH_SIZE = "drill.exec.sort.external.spill.spill_batch_size";
  public static final String EXTERNAL_SORT_MERGE_BATCH_SIZE = "drill.exec.sort.external.spill.merge_batch_size";
  public static final String EXTERNAL_SORT_MAX_MEMORY = "drill.exec.sort.external.mem_limit";
  public static final String EXTERNAL_SORT_BATCH_LIMIT = "drill.exec.sort.external.batch_limit";

  // External Sort Runtime options

  @Deprecated // Managed sort is the only implementation
  public static final String EXTERNAL_SORT_DISABLE_MANAGED = "drill.exec.sort.external.disable_managed";
  @Deprecated
  public static final BooleanValidator EXTERNAL_SORT_DISABLE_MANAGED_OPTION = new BooleanValidator("exec.sort.disable_managed", null);

  // Hash Join Options
  public static final String HASHJOIN_HASHTABLE_CALC_TYPE_KEY = "exec.hashjoin.hash_table_calc_type";
  public static final EnumeratedStringValidator HASHJOIN_HASHTABLE_CALC_TYPE = new EnumeratedStringValidator(HASHJOIN_HASHTABLE_CALC_TYPE_KEY,
      new OptionDescription("Sets the Hash Join Memory Calculator type. Default is LEAN. This option also accepts CONSERVATIVE as a value."),
      "LEAN", "CONSERVATIVE");
  public static final String HASHJOIN_SAFETY_FACTOR_KEY = "exec.hashjoin.safety_factor";
  public static final DoubleValidator HASHJOIN_SAFETY_FACTOR = new RangeDoubleValidator(HASHJOIN_SAFETY_FACTOR_KEY, 1.0, Double.MAX_VALUE,
      new OptionDescription("Sets the Hash Join Memory Calculation Safety; multiplies the internal size estimate. Default is 1.0"));
  public static final String HASHJOIN_HASH_DOUBLE_FACTOR_KEY = "exec.hashjoin.hash_double_factor";
  public static final DoubleValidator HASHJOIN_HASH_DOUBLE_FACTOR = new RangeDoubleValidator(HASHJOIN_HASH_DOUBLE_FACTOR_KEY, 1.0, Double.MAX_VALUE,
      new OptionDescription("Sets the Hash Join Memory Calculation; doubling factor for the Hash-Table. Default is 2.0"));
  public static final String HASHJOIN_FRAGMENTATION_FACTOR_KEY = "exec.hashjoin.fragmentation_factor";
  public static final DoubleValidator HASHJOIN_FRAGMENTATION_FACTOR = new RangeDoubleValidator(HASHJOIN_FRAGMENTATION_FACTOR_KEY, 1.0, Double.MAX_VALUE,
      new OptionDescription("Sets the Hash Join Memory Calculations; multiplies the internal estimates to account for fragmentation. Default is 1.33"));
  public static final String HASHJOIN_NUM_ROWS_IN_BATCH_KEY = "exec.hashjoin.num_rows_in_batch";
  public static final LongValidator HASHJOIN_NUM_ROWS_IN_BATCH_VALIDATOR = new RangeLongValidator(HASHJOIN_NUM_ROWS_IN_BATCH_KEY, 1, 65536,
      new OptionDescription("Sets the number of rows in the internal batches for Hash Join operations. Default is 1024"));
  public static final String HASHJOIN_MAX_BATCHES_IN_MEMORY_KEY = "exec.hashjoin.max_batches_in_memory";
  public static final LongValidator HASHJOIN_MAX_BATCHES_IN_MEMORY_VALIDATOR = new RangeLongValidator(HASHJOIN_MAX_BATCHES_IN_MEMORY_KEY, 0, 65536,
      new OptionDescription("Sets the maximum number of batches allowed in memory before spilling is enforced for Hash Join operations; used for testing purposes."));
  public static final String HASHJOIN_NUM_PARTITIONS_KEY = "exec.hashjoin.num_partitions";
  public static final LongValidator HASHJOIN_NUM_PARTITIONS_VALIDATOR = new RangeLongValidator(HASHJOIN_NUM_PARTITIONS_KEY, 1, 128,
      new OptionDescription("Sets the initial number of internal partitions for Hash Join operations. Default is 32. May reduce when memory is too small. Disables spilling if set to 1.")); // 1 means - no spilling
  public static final String HASHJOIN_MAX_MEMORY_KEY = "drill.exec.hashjoin.mem_limit";
  public static final LongValidator HASHJOIN_MAX_MEMORY_VALIDATOR = new RangeLongValidator(HASHJOIN_MAX_MEMORY_KEY, 0L, Long.MAX_VALUE,
      new OptionDescription("Enforces the maximum memory limit for the Hash Join operator (if non-zero); used for testing purposes. Default is 0 (disabled)."));
  public static final String HASHJOIN_SPILL_DIRS = "drill.exec.hashjoin.spill.directories";
  public static final String HASHJOIN_SPILL_FILESYSTEM = "drill.exec.hashjoin.spill.fs";
  public static final String HASHJOIN_FALLBACK_ENABLED_KEY = "drill.exec.hashjoin.fallback.enabled";
  public static final BooleanValidator HASHJOIN_FALLBACK_ENABLED_VALIDATOR = new BooleanValidator(HASHJOIN_FALLBACK_ENABLED_KEY,
      new OptionDescription("Hash Joins ignore memory limits when this option is enabled (true). When disabled (false), Hash Joins fail when memory is set too low."));
  public static final String HASHJOIN_ENABLE_RUNTIME_FILTER_KEY = "exec.hashjoin.enable.runtime_filter";
  public static final BooleanValidator HASHJOIN_ENABLE_RUNTIME_FILTER = new BooleanValidator(HASHJOIN_ENABLE_RUNTIME_FILTER_KEY, null);
  public static final String HASHJOIN_BLOOM_FILTER_MAX_SIZE_KEY = "exec.hashjoin.bloom_filter.max.size";
  public static final IntegerValidator HASHJOIN_BLOOM_FILTER_MAX_SIZE = new IntegerValidator(HASHJOIN_BLOOM_FILTER_MAX_SIZE_KEY, null);
  public static final String HASHJOIN_BLOOM_FILTER_FPP_KEY = "exec.hashjoin.bloom_filter.fpp";
  public static final DoubleValidator HASHJOIN_BLOOM_FILTER_FPP_VALIDATOR = new RangeDoubleValidator(HASHJOIN_BLOOM_FILTER_FPP_KEY, Double.MIN_VALUE, 1.0, null);
  public static final String HASHJOIN_RUNTIME_FILTER_WAITING_ENABLE_KEY = "exec.hashjoin.runtime_filter.waiting.enable";
  public static final BooleanValidator HASHJOIN_ENABLE_RUNTIME_FILTER_WAITING = new BooleanValidator(HASHJOIN_RUNTIME_FILTER_WAITING_ENABLE_KEY, null);
  public static final String HASHJOIN_RUNTIME_FILTER_MAX_WAITING_TIME_KEY = "exec.hashjoin.runtime_filter.max.waiting.time";
  public static final PositiveLongValidator HASHJOIN_RUNTIME_FILTER_MAX_WAITING_TIME = new PositiveLongValidator(HASHJOIN_RUNTIME_FILTER_MAX_WAITING_TIME_KEY, Character.MAX_VALUE, null);


  // Hash Aggregate Options
  public static final String HASHAGG_NUM_PARTITIONS_KEY = "exec.hashagg.num_partitions";
  public static final LongValidator HASHAGG_NUM_PARTITIONS_VALIDATOR = new RangeLongValidator(HASHAGG_NUM_PARTITIONS_KEY, 1, 128,
      new OptionDescription("Sets the initial number of internal partitions for Hash Aggregates. Default is 32. May reduce when memory is too small. Disables spilling if set to 1.")); // 1 means - no spilling
  public static final String HASHAGG_MAX_MEMORY_KEY = "exec.hashagg.mem_limit";
  public static final LongValidator HASHAGG_MAX_MEMORY_VALIDATOR = new RangeLongValidator(HASHAGG_MAX_MEMORY_KEY, 0, Integer.MAX_VALUE,
      new OptionDescription("Enforces the value set as the maximum memory for the Hash Aggregates. Default is 0 (disabled)."));
  // min batches is used for tuning (each partition needs so many batches when planning the number of partitions,
  // or reserve this number when calculating whether the remaining available memory is too small and requires a spill.)
  // Low value may OOM (e.g., when incoming rows become wider), higher values use fewer partitions but are safer
  public static final String HASHAGG_MIN_BATCHES_PER_PARTITION_KEY = "exec.hashagg.min_batches_per_partition";
  public static final LongValidator HASHAGG_MIN_BATCHES_PER_PARTITION_VALIDATOR = new RangeLongValidator(HASHAGG_MIN_BATCHES_PER_PARTITION_KEY, 1, 5,
      new OptionDescription("Sets the safety assumption for the minimum number of batches needed for each partition when performing hash aggregation. Default is 2. "
          + "Low value may OOM if incoming rows become very wide. See Spill-to-disk for Hash Aggregate operator for more information."));
  // Can be turned off mainly for testing. Memory prediction is used to decide on when to spill to disk; with this option off,
  // spill would be triggered only by another mechanism -- "catch OOMs and then spill".
  public static final String HASHAGG_USE_MEMORY_PREDICTION_KEY = "exec.hashagg.use_memory_prediction";
  public static final BooleanValidator HASHAGG_USE_MEMORY_PREDICTION_VALIDATOR = new BooleanValidator(HASHAGG_USE_MEMORY_PREDICTION_KEY,
      new OptionDescription("Enables Hash Aggregates to use memory predictions to proactively spill early. Default is true."));

  public static final String HASHAGG_SPILL_DIRS = "drill.exec.hashagg.spill.directories";
  public static final String HASHAGG_SPILL_FILESYSTEM = "drill.exec.hashagg.spill.fs";
  public static final String HASHAGG_FALLBACK_ENABLED_KEY = "drill.exec.hashagg.fallback.enabled";
  public static final BooleanValidator HASHAGG_FALLBACK_ENABLED_VALIDATOR = new BooleanValidator(HASHAGG_FALLBACK_ENABLED_KEY,
      new OptionDescription("Hash Aggregates ignore memory limits when enabled (true). When disabled (false), Hash Aggregates fail when memory is set too low."));

  // Partitioner options
  public static final String PARTITIONER_MEMORY_REDUCTION_THRESHOLD_KEY = "exec.partition.mem_throttle";
  public static final LongValidator PARTITIONER_MEMORY_REDUCTION_THRESHOLD_VALIDATOR =
      new RangeLongValidator(PARTITIONER_MEMORY_REDUCTION_THRESHOLD_KEY, 0, Integer.MAX_VALUE,
      new OptionDescription("Linearly reduces partition sender buffer row count after this number of receivers. Default is 0 (disabled). (Since Drill 1.18)"));

  public static final String SSL_PROVIDER = "drill.exec.ssl.provider"; // valid values are "JDK", "OPENSSL" // default JDK
  public static final String SSL_PROTOCOL = "drill.exec.ssl.protocol"; // valid values are SSL, SSLV2, SSLV3, TLS, TLSV1, TLSv1.1, TLSv1.2(default)
  public static final String SSL_KEYSTORE_TYPE = "drill.exec.ssl.keyStoreType";
  public static final String SSL_KEYSTORE_PATH = "drill.exec.ssl.keyStorePath";     // path to keystore. default : $JRE_HOME/lib/security/keystore.jks
  public static final String SSL_KEYSTORE_PASSWORD = "drill.exec.ssl.keyStorePassword"; // default: changeit
  public static final String SSL_KEY_PASSWORD = "drill.exec.ssl.keyPassword"; //
  public static final String SSL_TRUSTSTORE_TYPE = "drill.exec.ssl.trustStoreType"; // valid values are jks(default), jceks, pkcs12
  public static final String SSL_TRUSTSTORE_PATH = "drill.exec.ssl.trustStorePath"; // path to keystore. default : $JRE_HOME/lib/security/cacerts.jks
  public static final String SSL_TRUSTSTORE_PASSWORD = "drill.exec.ssl.trustStorePassword"; // default: changeit
  public static final String SSL_USE_HADOOP_CONF = "drill.exec.ssl.useHadoopConfig"; // Initialize ssl params from hadoop if not provided by drill. default: true
  public static final String SSL_USE_MAPR_CONFIG = "drill.exec.ssl.useMapRSSLConfig"; // Use keyStore and trustStore credentials provided by MapR platform.
  public static final String SSL_HANDSHAKE_TIMEOUT = "drill.exec.security.user.encryption.ssl.handshakeTimeout"; // Default 10 seconds

  public static final String TEXT_LINE_READER_BATCH_SIZE = "drill.exec.storage.file.text.batch.size";
  public static final String TEXT_LINE_READER_BUFFER_SIZE = "drill.exec.storage.file.text.buffer.size";
  public static final String HAZELCAST_SUBNETS = "drill.exec.cache.hazel.subnets";
  public static final String HTTP_ENABLE = "drill.exec.http.enabled";
  public static final String HTTP_MAX_PROFILES = "drill.exec.http.max_profiles";
  public static final String HTTP_PROFILES_PER_PAGE = "drill.exec.http.profiles_per_page";
  public static final String HTTP_PORT = "drill.exec.http.port";
  public static final String HTTP_PORT_HUNT = "drill.exec.http.porthunt";
  public static final String HTTP_JETTY_SERVER_DUMP_AFTER_START = "drill.exec.http.jetty.server.dumpAfterStart";
  public static final String HTTP_JETTY_SERVER_ACCEPTORS = "drill.exec.http.jetty.server.acceptors";
  public static final String HTTP_JETTY_SERVER_SELECTORS = "drill.exec.http.jetty.server.selectors";
  public static final String HTTP_JETTY_SERVER_HANDLERS = "drill.exec.http.jetty.server.handlers";
  public static final String HTTP_JETTY_SERVER_RESPONSE_HEADERS = "drill.exec.http.jetty.server.response.headers";
  public static final String HTTP_JETTY_SSL_CONTEXT_FACTORY_OPTIONS_PREFIX = "drill.exec.http.jetty.server.sslContextFactory";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_CERT_ALIAS = "drill.exec.http.jetty.server.sslContextFactory.certAlias";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_CRL_PATH = "drill.exec.http.jetty.server.sslContextFactory.crlPath";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_ENABLE_CRLDP = "drill.exec.http.jetty.server.sslContextFactory.enableCRLDP";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_ENABLE_OCSP = "drill.exec.http.jetty.server.sslContextFactory.enableOCSP";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_ENDPOINT_IDENTIFICATION_ALGORITHM = "drill.exec.http.jetty.server.sslContextFactory.endpointIdentificationAlgorithm";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_EXCLUDE_CIPHER_SUITES = "drill.exec.http.jetty.server.sslContextFactory.excludeCipherSuites";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_EXCLUDE_PROTOCOLS = "drill.exec.http.jetty.server.sslContextFactory.excludeProtocols";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_INCLUDE_CIPHER_SUITES = "drill.exec.http.jetty.server.sslContextFactory.includeCipherSuites";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_INCLUDE_PROTOCOLS = "drill.exec.http.jetty.server.sslContextFactory.includeProtocols";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_KEY_MANAGER_FACTORY_ALGORITHM = "drill.exec.http.jetty.server.sslContextFactory.keyManagerFactoryAlgorithm";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_KEYSTORE_PROVIDER = "drill.exec.http.jetty.server.sslContextFactory.keyStoreProvider";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_KEYSTORE_TYPE = "drill.exec.http.jetty.server.sslContextFactory.keyStoreType";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_MAX_CERT_PATH_LENGTH = "drill.exec.http.jetty.server.sslContextFactory.maxCertPathLength";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_NEED_CLIENT_AUTH = "drill.exec.http.jetty.server.sslContextFactory.needClientAuth";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_OCSP_RESPONDER_URL = "drill.exec.http.jetty.server.sslContextFactory.ocspResponderURL";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_PROVIDER = "drill.exec.http.jetty.server.sslContextFactory.provider";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_RENEGOTIATION_ALLOWED = "drill.exec.http.jetty.server.sslContextFactory.renegotiationAllowed";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_RENEGOTIATION_LIMIT = "drill.exec.http.jetty.server.sslContextFactory.renegotiationLimit";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_SECURE_RANDOM_ALGORITHM = "drill.exec.http.jetty.server.sslContextFactory.secureRandomAlgorithm";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_SESSION_CACHING_ENABLED = "drill.exec.http.jetty.server.sslContextFactory.sessionCachingEnabled";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_SSL_SESSION_CACHE_SIZE = "drill.exec.http.jetty.server.sslContextFactory.sslSessionCacheSize";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_SSL_SESSION_TIMEOUT = "drill.exec.http.jetty.server.sslContextFactory.sslSessionTimeout";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_TRUSTMANAGERFACTORY_ALGORITHM = "drill.exec.http.jetty.server.sslContextFactory.trustManagerFactoryAlgorithm";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_TRUSTSTORE_PROVIDER = "drill.exec.http.jetty.server.sslContextFactory.trustStoreProvider";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_TRUSTSTORE_TYPE = "drill.exec.http.jetty.server.sslContextFactory.trustStoreType";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_USE_CIPHER_SUITE_ORDER = "drill.exec.http.jetty.server.sslContextFactory.useCipherSuiteOrder";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_VALIDATE_CERTS = "drill.exec.http.jetty.server.sslContextFactory.validateCerts";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_VALIDATE_PEER_CERTS = "drill.exec.http.jetty.server.sslContextFactory.validatePeerCerts";
  public static final String HTTP_JETTY_SERVER_SSL_CONTEXT_FACTORY_WANT_CLIENT_AUTH = "drill.exec.http.jetty.server.sslContextFactory.wantClientAuth";

  public static final String HTTP_ENABLE_SSL = "drill.exec.http.ssl_enabled";
  public static final String HTTP_CLIENT_TIMEOUT = "drill.exec.http.client.timeout";
  public static final String HTTP_CORS_ENABLED = "drill.exec.http.cors.enabled";
  public static final String HTTP_CORS_ALLOWED_ORIGINS = "drill.exec.http.cors.allowedOrigins";
  public static final String HTTP_CORS_ALLOWED_METHODS = "drill.exec.http.cors.allowedMethods";
  public static final String HTTP_CORS_ALLOWED_HEADERS = "drill.exec.http.cors.allowedHeaders";
  public static final String HTTP_CORS_CREDENTIALS = "drill.exec.http.cors.credentials";
  public static final String HTTP_SESSION_MEMORY_RESERVATION = "drill.exec.http.session.memory.reservation";
  public static final String HTTP_SESSION_MEMORY_MAXIMUM = "drill.exec.http.session.memory.maximum";
  public static final String HTTP_SESSION_MAX_IDLE_SECS = "drill.exec.http.session_max_idle_secs";
  public static final String HTTP_KEYSTORE_PATH = SSL_KEYSTORE_PATH;
  public static final String HTTP_KEYSTORE_PASSWORD = SSL_KEYSTORE_PASSWORD;
  public static final String HTTP_TRUSTSTORE_PATH = SSL_TRUSTSTORE_PATH;
  public static final String HTTP_TRUSTSTORE_PASSWORD = SSL_TRUSTSTORE_PASSWORD;
  public static final String HTTP_AUTHENTICATION_MECHANISMS = "drill.exec.http.auth.mechanisms";
  public static final String HTTP_SPNEGO_PRINCIPAL = "drill.exec.http.auth.spnego.principal";
  public static final String HTTP_SPNEGO_KEYTAB = "drill.exec.http.auth.spnego.keytab";
  //Control Web UI Resultset
  public static final String HTTP_WEB_CLIENT_RESULTSET_AUTOLIMIT_CHECKED = "drill.exec.http.web.client.resultset.autolimit.checked";
  public static final String HTTP_WEB_CLIENT_RESULTSET_AUTOLIMIT_ROWS = "drill.exec.http.web.client.resultset.autolimit.rows";
  public static final String HTTP_WEB_CLIENT_RESULTSET_ROWS_PER_PAGE_VALUES = "drill.exec.http.web.client.resultset.rowsPerPageValues";
  @Deprecated // TODO: Remove any logic based on this option now that REST query results stream.
  public static final String HTTP_MEMORY_HEAP_FAILURE_THRESHOLD = "drill.exec.http.memory.heap.failure.threshold";
  //Customize filters in options
  public static final String HTTP_WEB_OPTIONS_FILTERS = "drill.exec.http.web.options.filters";
  public static final String SYS_STORE_PROVIDER_CLASS = "drill.exec.sys.store.provider.class";
  public static final String SYS_STORE_PROVIDER_LOCAL_PATH = "drill.exec.sys.store.provider.local.path";
  public static final String SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE = "drill.exec.sys.store.provider.local.write";
  public static final String PROFILES_STORE_INMEMORY = "drill.exec.profiles.store.inmemory";
  public static final String PROFILES_STORE_CAPACITY = "drill.exec.profiles.store.capacity";
  public static final String IMPERSONATION_ENABLED = "drill.exec.impersonation.enabled";
  public static final String IMPERSONATION_MAX_CHAINED_USER_HOPS = "drill.exec.impersonation.max_chained_user_hops";
  public static final String AUTHENTICATION_MECHANISMS = "drill.exec.security.auth.mechanisms";
  public static final String USER_AUTHENTICATION_ENABLED = "drill.exec.security.user.auth.enabled";
  public static final String USER_AUTHENTICATOR_IMPL = "drill.exec.security.user.auth.impl";
  public static final String HTPASSWD_AUTHENTICATOR_PATH = "drill.exec.security.user.auth.htpasswd.path";
  public static final String PAM_AUTHENTICATOR_PROFILES = "drill.exec.security.user.auth.pam_profiles";
  public static final String BIT_AUTHENTICATION_ENABLED = "drill.exec.security.bit.auth.enabled";
  public static final String BIT_AUTHENTICATION_MECHANISM = "drill.exec.security.bit.auth.mechanism";
  public static final String USE_LOGIN_PRINCIPAL = "drill.exec.security.bit.auth.use_login_principal";
  public static final String USER_ENCRYPTION_SASL_ENABLED = "drill.exec.security.user.encryption.sasl.enabled";
  public static final String USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE = "drill.exec.security.user.encryption.sasl.max_wrapped_size";
  private static final String SERVICE_LOGIN_PREFIX = "drill.exec.security.auth";
  public static final String SERVICE_PRINCIPAL = SERVICE_LOGIN_PREFIX + ".principal";
  public static final String SERVICE_KEYTAB_LOCATION = SERVICE_LOGIN_PREFIX + ".keytab";
  public static final String KERBEROS_NAME_MAPPING = SERVICE_LOGIN_PREFIX + ".auth_to_local";

  public static final String USER_SSL_ENABLED = "drill.exec.security.user.encryption.ssl.enabled";
  public static final String BIT_ENCRYPTION_SASL_ENABLED = "drill.exec.security.bit.encryption.sasl.enabled";
  public static final String BIT_ENCRYPTION_SASL_MAX_WRAPPED_SIZE = "drill.exec.security.bit.encryption.sasl.max_wrapped_size";

  /** Size of JDBC batch queue (in batches) above which throttling begins. */
  public static final String JDBC_BATCH_QUEUE_THROTTLING_THRESHOLD =
      "drill.jdbc.batch_queue_throttling_threshold";
  // Thread pool size for scan threads. Used by the Parquet scan.
  public static final String SCAN_THREADPOOL_SIZE = "drill.exec.scan.threadpool_size";
  // The size of the thread pool used by a scan to decode the data. Used by Parquet
  public static final String SCAN_DECODE_THREADPOOL_SIZE = "drill.exec.scan.decode_threadpool_size";

  /**
   * Currently if a query is cancelled, but one of the fragments reports the status as FAILED instead of CANCELLED or
   * FINISHED we report the query result as CANCELLED by swallowing the failures occurred in fragments. This BOOT
   * setting allows the user to see the query status as failure. Useful for developers/testers.
   */
  public static final String RETURN_ERROR_FOR_FAILURE_IN_CANCELLED_FRAGMENTS = "drill.exec.debug.return_error_for_failure_in_cancelled_fragments";

  public static final String CLIENT_SUPPORT_COMPLEX_TYPES = "drill.client.supports-complex-types";

  /**
   * Configuration properties connected with dynamic UDFs support
   */
  public static final String UDF_RETRY_ATTEMPTS = "drill.exec.udf.retry-attempts";
  public static final String UDF_DIRECTORY_LOCAL = "drill.exec.udf.directory.local";
  public static final String UDF_DIRECTORY_FS = "drill.exec.udf.directory.fs";
  public static final String UDF_DIRECTORY_ROOT = "drill.exec.udf.directory.root";
  public static final String UDF_DIRECTORY_STAGING = "drill.exec.udf.directory.staging";
  public static final String UDF_DIRECTORY_REGISTRY = "drill.exec.udf.directory.registry";
  public static final String UDF_DIRECTORY_TMP = "drill.exec.udf.directory.tmp";
  public static final String UDF_DISABLE_DYNAMIC = "drill.exec.udf.disable_dynamic";

  /**
   * Local temporary directory is used as base for temporary storage of Dynamic UDF jars.
   */
  public static final String DRILL_TMP_DIR = "drill.tmp-dir";

  /**
   * Temporary tables can be created ONLY in default temporary workspace.
   */
  public static final String DEFAULT_TEMPORARY_WORKSPACE = "drill.exec.default_temporary_workspace";

  public static final String OUTPUT_FORMAT_OPTION = "store.format";
  public static final OptionValidator OUTPUT_FORMAT_VALIDATOR = new StringValidator(OUTPUT_FORMAT_OPTION,
      new OptionDescription("Output format for data written to tables with the CREATE TABLE AS (CTAS) command. Allowed values are parquet, json, psv, csv, or tsv."));
  public static final String PARQUET_WRITER_USE_SINGLE_FS_BLOCK = "store.parquet.writer.use_single_fs_block";
  public static final OptionValidator PARQUET_WRITER_USE_SINGLE_FS_BLOCK_VALIDATOR = new BooleanValidator(PARQUET_WRITER_USE_SINGLE_FS_BLOCK,
      new OptionDescription("Instructs the Parquet writer to create files with the configured block size (instead of the default filesystem block size)."));
  public static final String PARQUET_BLOCK_SIZE = "store.parquet.block-size";
  public static final OptionValidator PARQUET_BLOCK_SIZE_VALIDATOR = new PositiveLongValidator(PARQUET_BLOCK_SIZE, Integer.MAX_VALUE,
      new OptionDescription("Sets the row group size (in bytes) for output Parquet files to a number that is less than or equal to the block size in MFS, HDFS, or the file system."));
  public static final String PARQUET_PAGE_SIZE = "store.parquet.page-size";
  public static final OptionValidator PARQUET_PAGE_SIZE_VALIDATOR = new PositiveLongValidator(PARQUET_PAGE_SIZE, Integer.MAX_VALUE,
      new OptionDescription("Sets the page size for output Parquet files."));
  public static final String PARQUET_DICT_PAGE_SIZE = "store.parquet.dictionary.page-size";
  public static final OptionValidator PARQUET_DICT_PAGE_SIZE_VALIDATOR = new PositiveLongValidator(PARQUET_DICT_PAGE_SIZE, Integer.MAX_VALUE,
      new OptionDescription("For internal use. Do not change."));
  public static final String PARQUET_WRITER_COMPRESSION_TYPE = "store.parquet.compression";
  public static final OptionValidator PARQUET_WRITER_COMPRESSION_TYPE_VALIDATOR = new EnumeratedStringValidator(
      PARQUET_WRITER_COMPRESSION_TYPE,
      new OptionDescription("Compression type for storing Parquet output. Allowed values: none, brotli, gzip, lz4, lzo, snappy, zstd"),
      "none", "brotli", "gzip", "lz4", "lzo", "snappy", "zstd"
  );
  public static final String PARQUET_WRITER_ENABLE_DICTIONARY_ENCODING = "store.parquet.enable_dictionary_encoding";
  public static final OptionValidator PARQUET_WRITER_ENABLE_DICTIONARY_ENCODING_VALIDATOR = new BooleanValidator(
      PARQUET_WRITER_ENABLE_DICTIONARY_ENCODING,
      new OptionDescription("For internal use. Do not change."));

  public static final String PARQUET_WRITER_USE_PRIMITIVE_TYPES_FOR_DECIMALS = "store.parquet.writer.use_primitive_types_for_decimals";
  public static final OptionValidator PARQUET_WRITER_USE_PRIMITIVE_TYPES_FOR_DECIMALS_VALIDATOR = new BooleanValidator(PARQUET_WRITER_USE_PRIMITIVE_TYPES_FOR_DECIMALS,
      new OptionDescription("Instructs the Parquet writer to convert decimal to primitive types whenever possible."));

  public static final String PARQUET_WRITER_LOGICAL_TYPE_FOR_DECIMALS = "store.parquet.writer.logical_type_for_decimals";
  public static final OptionValidator PARQUET_WRITER_LOGICAL_TYPE_FOR_DECIMALS_VALIDATOR = new EnumeratedStringValidator(PARQUET_WRITER_LOGICAL_TYPE_FOR_DECIMALS,
      new OptionDescription("Parquet writer logical type for decimal; supported types \'fixed_len_byte_array\' and \'binary\'"),
      "fixed_len_byte_array", "binary");
  public static final String PARQUET_WRITER_FORMAT_VERSION = "store.parquet.writer.format_version";
  public static final OptionValidator PARQUET_WRITER_FORMAT_VERSION_VALIDATOR = new EnumeratedStringValidator(
    PARQUET_WRITER_FORMAT_VERSION,
    new OptionDescription(
      "Parquet format version used for storing Parquet output.  Allowed values:" +
        Arrays.toString(ParquetFormatPlugin.PARQUET_FORMAT_VERSIONS)
    ),
    ParquetFormatPlugin.PARQUET_FORMAT_VERSIONS
  );

  // TODO - The below two options don't seem to be used in the Drill code base
  @Deprecated // TODO: DRILL-6527
  public static final String PARQUET_VECTOR_FILL_THRESHOLD = "store.parquet.vector_fill_threshold";
  @Deprecated // TODO: DRILL-6527
  public static final OptionValidator PARQUET_VECTOR_FILL_THRESHOLD_VALIDATOR = new PositiveLongValidator(PARQUET_VECTOR_FILL_THRESHOLD, 99L,
      new OptionDescription("Deprecated."));
  @Deprecated // TODO: DRILL-6527
  public static final String PARQUET_VECTOR_FILL_CHECK_THRESHOLD = "store.parquet.vector_fill_check_threshold";
  @Deprecated // TODO: DRILL-6527
  public static final OptionValidator PARQUET_VECTOR_FILL_CHECK_THRESHOLD_VALIDATOR = new PositiveLongValidator(PARQUET_VECTOR_FILL_CHECK_THRESHOLD, 100L,
      new OptionDescription("Deprecated."));

  public static final String PARQUET_NEW_RECORD_READER = "store.parquet.use_new_reader";
  public static final OptionValidator PARQUET_RECORD_READER_IMPLEMENTATION_VALIDATOR = new BooleanValidator(PARQUET_NEW_RECORD_READER,
      new OptionDescription("Not supported in this release."));
  public static final String PARQUET_READER_INT96_AS_TIMESTAMP = "store.parquet.reader.int96_as_timestamp";
  public static final OptionValidator PARQUET_READER_INT96_AS_TIMESTAMP_VALIDATOR = new BooleanValidator(PARQUET_READER_INT96_AS_TIMESTAMP,
      new OptionDescription("Enables Drill to implicitly interpret the INT96 timestamp data type in Parquet files."));

  public static final String PARQUET_READER_STRINGS_SIGNED_MIN_MAX = "store.parquet.reader.strings_signed_min_max";
  public static final StringValidator PARQUET_READER_STRINGS_SIGNED_MIN_MAX_VALIDATOR = new EnumeratedStringValidator(PARQUET_READER_STRINGS_SIGNED_MIN_MAX,
    new OptionDescription("Allows binary statistics usage for files created prior to 1.9.1 parquet library version where " +
      "statistics was incorrectly calculated for UTF-8 data. For cases when user exactly knows " +
      "that data in binary columns is in ASCII (not UTF-8), turning this property to 'true' " +
      "enables statistics usage for varchar and decimal data types. Default is unset, i.e. empty string. " +
      "Allowed values: 'true', 'false', '' (empty string)."), "true", "false", "");

  public static final String PARQUET_PAGEREADER_ASYNC = "store.parquet.reader.pagereader.async";
  public static final OptionValidator PARQUET_PAGEREADER_ASYNC_VALIDATOR = new BooleanValidator(PARQUET_PAGEREADER_ASYNC,
      new OptionDescription("Enable the asynchronous page reader. This pipelines the reading of data from disk for high performance."));

  // Number of pages the Async Parquet page reader will read before blocking
  public static final String PARQUET_PAGEREADER_QUEUE_SIZE = "store.parquet.reader.pagereader.queuesize";
  public static final OptionValidator PARQUET_PAGEREADER_QUEUE_SIZE_VALIDATOR = new  PositiveLongValidator(PARQUET_PAGEREADER_QUEUE_SIZE, Integer.MAX_VALUE,
      new OptionDescription("Sets the number of pages that the Parquet reader prefetches per column."));

  public static final String PARQUET_PAGEREADER_ENFORCETOTALSIZE = "store.parquet.reader.pagereader.enforceTotalSize";
  public static final OptionValidator PARQUET_PAGEREADER_ENFORCETOTALSIZE_VALIDATOR = new BooleanValidator(PARQUET_PAGEREADER_ENFORCETOTALSIZE,
      new OptionDescription("Instructs the Parquet reader to read no more than the advertised page size."));

  public static final String PARQUET_COLUMNREADER_ASYNC = "store.parquet.reader.columnreader.async";
  public static final OptionValidator PARQUET_COLUMNREADER_ASYNC_VALIDATOR = new BooleanValidator(PARQUET_COLUMNREADER_ASYNC,
      new OptionDescription("Turn on parallel decoding of column data from Parquet to the in memory format. This increases CPU usage and is most useful for compressed fixed width data. With increasing concurrency, this option may cause queries to run slower and should be turned on only for performance critical queries."));

  // Use a buffering reader for Parquet page reader
  public static final String PARQUET_PAGEREADER_USE_BUFFERED_READ = "store.parquet.reader.pagereader.bufferedread";
  public static final OptionValidator PARQUET_PAGEREADER_USE_BUFFERED_READ_VALIDATOR = new  BooleanValidator(PARQUET_PAGEREADER_USE_BUFFERED_READ,
      new OptionDescription("Enable buffered page reading. Can improve disk scan speeds by buffering data, but increases memory usage. This option is less useful when the number of columns increases."));

  // Size in MiB of the buffer the Parquet page reader will use to read from disk. Default is 1 MiB
  public static final String PARQUET_PAGEREADER_BUFFER_SIZE = "store.parquet.reader.pagereader.buffersize";
  public static final OptionValidator PARQUET_PAGEREADER_BUFFER_SIZE_VALIDATOR = new  LongValidator(PARQUET_PAGEREADER_BUFFER_SIZE,
      new OptionDescription("The size of the buffer (in bytes) to use if bufferedread is true. Has no effect otherwise."));

  // try to use fadvise if available
  public static final String PARQUET_PAGEREADER_USE_FADVISE = "store.parquet.reader.pagereader.usefadvise";
  public static final OptionValidator PARQUET_PAGEREADER_USE_FADVISE_VALIDATOR = new  BooleanValidator(PARQUET_PAGEREADER_USE_FADVISE,
      new OptionDescription("If the file system supports it, the Parquet file reader issues an fadvise call to enable file server side sequential reading and caching. Since many HDFS implementations do not support this and because this may have no effect in conditions of high concurrency, the option is set to false. Useful for benchmarks and for performance critical queries."));

  // scalar replacement strategy
  public final static String SCALAR_REPLACEMENT_OPTION = "org.apache.drill.exec.compile.ClassTransformer.scalar_replacement";
  public final static EnumeratedStringValidator SCALAR_REPLACEMENT_VALIDATOR = new EnumeratedStringValidator( SCALAR_REPLACEMENT_OPTION,
      new OptionDescription("Enables Drill to attempt scalar replacement. If an error occurs during the attempt, Drill falls back to the previous behavior. Default is 'try'. Accepted values are 'try', 'on', and 'off'. (Drill 0.8+)"),
      "try", "on", "off");

  // Controls whether to enable bulk parquet reader processing
  public static final String PARQUET_FLAT_READER_BULK = "store.parquet.flat.reader.bulk";
  public static final OptionValidator PARQUET_FLAT_READER_BULK_VALIDATOR = new BooleanValidator(PARQUET_FLAT_READER_BULK,
      new OptionDescription("Parquet Reader which uses bulk processing (default)."));

  // Controls the flat parquet reader batching constraints (number of record and memory limit)
  public static final String PARQUET_FLAT_BATCH_NUM_RECORDS = "store.parquet.flat.batch.num_records";
  public static final OptionValidator PARQUET_FLAT_BATCH_NUM_RECORDS_VALIDATOR = new RangeLongValidator(PARQUET_FLAT_BATCH_NUM_RECORDS, 1, ValueVector.MAX_ROW_COUNT -1,
      new OptionDescription("Parquet Reader maximum number of records per batch."));
  public static final String PARQUET_FLAT_BATCH_MEMORY_SIZE = "store.parquet.flat.batch.memory_size";
  // This configuration is used to overwrite the common memory batch sizing configuration property
  public static final OptionValidator PARQUET_FLAT_BATCH_MEMORY_SIZE_VALIDATOR = new RangeLongValidator(PARQUET_FLAT_BATCH_MEMORY_SIZE, 0, Integer.MAX_VALUE,
      new OptionDescription("Flat Parquet Reader maximum memory size per batch."));

  // Controls the complex parquet reader batch sizing configuration
  public static final String PARQUET_COMPLEX_BATCH_NUM_RECORDS = "store.parquet.complex.batch.num_records";
  public static final OptionValidator PARQUET_COMPLEX_BATCH_NUM_RECORDS_VALIDATOR = new RangeLongValidator(PARQUET_COMPLEX_BATCH_NUM_RECORDS, 1, ValueVector.MAX_ROW_COUNT -1,
      new OptionDescription("Complex Parquet Reader maximum number of records per batch."));

  public static final String JSON_ALL_TEXT_MODE = "store.json.all_text_mode";
  public static final BooleanValidator JSON_READER_ALL_TEXT_MODE_VALIDATOR = new BooleanValidator(JSON_ALL_TEXT_MODE,
      new OptionDescription("Drill reads all data from the JSON files as VARCHAR. Prevents schema change errors."));
  public static final String JSON_EXTENDED_TYPES_KEY = "store.json.extended_types";
  public static final BooleanValidator JSON_EXTENDED_TYPES = new BooleanValidator(JSON_EXTENDED_TYPES_KEY,
      new OptionDescription("Turns on special JSON structures that Drill serializes for storing more type information than the four basic JSON types."));
  public static final String JSON_WRITER_UGLIFY_KEY = "store.json.writer.uglify";
  public static final BooleanValidator JSON_WRITER_UGLIFY = new BooleanValidator(JSON_WRITER_UGLIFY_KEY,
      new OptionDescription("Enables Drill to return compact JSON output files; Drill does not separate records. Default is false. (Drill 1.4+)"));
  public static final String JSON_WRITER_SKIP_NULL_FIELDS_KEY = "store.json.writer.skip_null_fields";
  public static final BooleanValidator JSON_WRITER_SKIPNULLFIELDS = new BooleanValidator(JSON_WRITER_SKIP_NULL_FIELDS_KEY,
      new OptionDescription("Enables Drill to skip extraneous NULL fields in JSON output files when executing the CTAS statement. Default is true. (Drill 1.6+)"));
  public static final String JSON_READER_SKIP_INVALID_RECORDS_FLAG = "store.json.reader.skip_invalid_records";
  public static final BooleanValidator JSON_SKIP_MALFORMED_RECORDS_VALIDATOR = new BooleanValidator(JSON_READER_SKIP_INVALID_RECORDS_FLAG,
      new OptionDescription("Allows queries to progress when the JSON record reader skips bad records in JSON files. Default is false. (Drill 1.9+)"));
  public static final String JSON_READER_PRINT_INVALID_RECORDS_LINE_NOS_FLAG = "store.json.reader.print_skipped_invalid_record_number";
  public static final BooleanValidator JSON_READER_PRINT_INVALID_RECORDS_LINE_NOS_FLAG_VALIDATOR = new BooleanValidator(JSON_READER_PRINT_INVALID_RECORDS_LINE_NOS_FLAG,
      new OptionDescription("Enables Drill to log the bad records that the JSON record reader skips when reading JSON files. Default is false. (Drill 1.9+)"));
  public static final String TEXT_ESTIMATED_ROW_SIZE_KEY = "store.text.estimated_row_size_bytes";
  public static final DoubleValidator TEXT_ESTIMATED_ROW_SIZE = new RangeDoubleValidator(TEXT_ESTIMATED_ROW_SIZE_KEY, 1, Long.MAX_VALUE,
      new OptionDescription("Estimate of the row size in a delimited text file, such as csv. The closer to actual, the better the query plan. Used for all csv files in the system/session where the value is set. Impacts the decision to plan a broadcast join or not."));

  public static final String TEXT_WRITER_ADD_HEADER = "store.text.writer.add_header";
  public static final BooleanValidator TEXT_WRITER_ADD_HEADER_VALIDATOR = new BooleanValidator(TEXT_WRITER_ADD_HEADER,
    new OptionDescription("Enables the TEXT writer to write header in newly created file. Default is true. (Drill 1.17+)"));

  public static final String TEXT_WRITER_FORCE_QUOTES = "store.text.writer.force_quotes";
  public static final BooleanValidator TEXT_WRITER_FORCE_QUOTES_VALIDATOR = new BooleanValidator(TEXT_WRITER_FORCE_QUOTES,
    new OptionDescription("Enables the TEXT writer to enclose in quotes all fields. Default is false. (Drill 1.17+)"));

  /**
   * Json writer option for writing `NaN` and `Infinity` tokens as numbers (not enclosed with double quotes)
   */
  public static final String JSON_WRITER_NAN_INF_NUMBERS = "store.json.writer.allow_nan_inf";
  public static final BooleanValidator JSON_WRITER_NAN_INF_NUMBERS_VALIDATOR = new BooleanValidator(JSON_WRITER_NAN_INF_NUMBERS,
      new OptionDescription("Enables the JSON writer in Drill to write `NaN` and `Infinity` tokens as numbers (not enclosed with double quotes) to a JSON file. Default is true. (Drill 1.13+)"));
  /**
   * Json reader option that enables parser to read `NaN` and `Infinity` tokens as numbers
   */
  public static final String JSON_READER_NAN_INF_NUMBERS = "store.json.reader.allow_nan_inf";
  public static final BooleanValidator JSON_READER_NAN_INF_NUMBERS_VALIDATOR = new BooleanValidator(JSON_READER_NAN_INF_NUMBERS,
      new OptionDescription("Enables the JSON record reader in Drill to read `NaN` and `Infinity` tokens in JSON data as numbers. Default is true. (Drill 1.13+)"));

  /**
   * Json reader option that enables parser to escape any characters
   */
  public static final String JSON_READER_ESCAPE_ANY_CHAR = "store.json.reader.allow_escape_any_char";
  public static final BooleanValidator JSON_READER_ESCAPE_ANY_CHAR_VALIDATOR = new BooleanValidator(JSON_READER_ESCAPE_ANY_CHAR,
    new OptionDescription("Enables the JSON record reader in Drill to escape any character. Default is false. (Drill 1.16+)"));

  public static final String STORE_TABLE_USE_SCHEMA_FILE = "store.table.use_schema_file";
  public static final BooleanValidator STORE_TABLE_USE_SCHEMA_FILE_VALIDATOR = new BooleanValidator(STORE_TABLE_USE_SCHEMA_FILE,
    new OptionDescription("Controls if schema file stored in table root directory will be used during query execution. (Drill 1.16+)"));

  /**
   * The column label (for directory levels) in results when querying files in a directory
   * E.g.  labels: dir0   dir1<pre>
   *    structure: foo
   *                |-    bar  -  a.parquet
   *                |-    baz  -  b.parquet</pre>
   */
  public static final String FILESYSTEM_PARTITION_COLUMN_LABEL = "drill.exec.storage.file.partition.column.label";
  public static final StringValidator FILESYSTEM_PARTITION_COLUMN_LABEL_VALIDATOR = new StringValidator(FILESYSTEM_PARTITION_COLUMN_LABEL,
      new OptionDescription("The column label for directory levels in results of queries of files in a directory. Accepts a string input."));

  /**
   * Implicit file columns
   */
  public static final String IMPLICIT_FILENAME_COLUMN_LABEL = "drill.exec.storage.implicit.filename.column.label";
  public static final OptionValidator IMPLICIT_FILENAME_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_FILENAME_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.10. Sets the implicit column name for the filename column."));
  public static final String IMPLICIT_SUFFIX_COLUMN_LABEL = "drill.exec.storage.implicit.suffix.column.label";
  public static final OptionValidator IMPLICIT_SUFFIX_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_SUFFIX_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.10. Sets the implicit column name for the suffix column."));
  public static final String IMPLICIT_FQN_COLUMN_LABEL = "drill.exec.storage.implicit.fqn.column.label";
  public static final StringValidator IMPLICIT_FQN_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_FQN_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.10. Sets the implicit column name for the fqn column."));
  public static final String IMPLICIT_FILEPATH_COLUMN_LABEL = "drill.exec.storage.implicit.filepath.column.label";
  public static final StringValidator IMPLICIT_FILEPATH_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_FILEPATH_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.10. Sets the implicit column name for the filepath column."));
  public static final String IMPLICIT_ROW_GROUP_INDEX_COLUMN_LABEL = "drill.exec.storage.implicit.row_group_index.column.label";
  public static final StringValidator IMPLICIT_ROW_GROUP_INDEX_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_ROW_GROUP_INDEX_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.17. Sets the implicit column name for the row group index (rgi) column. " +
          "For internal usage when producing Metastore analyze."));

  public static final String IMPLICIT_ROW_GROUP_START_COLUMN_LABEL = "drill.exec.storage.implicit.row_group_start.column.label";
  public static final StringValidator IMPLICIT_ROW_GROUP_START_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_ROW_GROUP_START_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.17. Sets the implicit column name for the row group start (rgs) column. " +
          "For internal usage when producing Metastore analyze."));

  public static final String IMPLICIT_ROW_GROUP_LENGTH_COLUMN_LABEL = "drill.exec.storage.implicit.row_group_length.column.label";
  public static final StringValidator IMPLICIT_ROW_GROUP_LENGTH_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_ROW_GROUP_LENGTH_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.17. Sets the implicit column name for the row group length (rgl) column. " +
          "For internal usage when producing Metastore analyze."));

  public static final String IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL = "drill.exec.storage.implicit.last_modified_time.column.label";
  public static final StringValidator IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.17. Sets the implicit column name for the lastModifiedTime column. " +
          "For internal usage when producing Metastore analyze."));

  public static final String IMPLICIT_PROJECT_METADATA_COLUMN_LABEL = "drill.exec.storage.implicit.project_metadata.column.label";
  public static final StringValidator IMPLICIT_PROJECT_METADATA_COLUMN_LABEL_VALIDATOR = new StringValidator(IMPLICIT_PROJECT_METADATA_COLUMN_LABEL,
      new OptionDescription("Available as of Drill 1.18. Sets the implicit column name for the $project_metadata$ column. " +
          "For internal usage when producing Metastore analyze."));

  public static final String JSON_READ_NUMBERS_AS_DOUBLE = "store.json.read_numbers_as_double";
  public static final BooleanValidator JSON_READ_NUMBERS_AS_DOUBLE_VALIDATOR = new BooleanValidator(JSON_READ_NUMBERS_AS_DOUBLE,
      new OptionDescription("Reads numbers with or without a decimal point as DOUBLE. Prevents schema change errors."));

  public static final String MONGO_ALL_TEXT_MODE = "store.mongo.all_text_mode";
  public static final OptionValidator MONGO_READER_ALL_TEXT_MODE_VALIDATOR = new BooleanValidator(MONGO_ALL_TEXT_MODE,
      new OptionDescription("Similar to store.json.all_text_mode for MongoDB."));
  public static final String MONGO_READER_READ_NUMBERS_AS_DOUBLE = "store.mongo.read_numbers_as_double";
  public static final OptionValidator MONGO_READER_READ_NUMBERS_AS_DOUBLE_VALIDATOR = new BooleanValidator(MONGO_READER_READ_NUMBERS_AS_DOUBLE,
      new OptionDescription("Similar to store.json.read_numbers_as_double."));
  public static final String MONGO_BSON_RECORD_READER = "store.mongo.bson.record.reader";
  public static final OptionValidator MONGO_BSON_RECORD_READER_VALIDATOR = new BooleanValidator(MONGO_BSON_RECORD_READER, null);

  public static final String ENABLE_UNION_TYPE_KEY = "exec.enable_union_type";
  public static final BooleanValidator ENABLE_UNION_TYPE = new BooleanValidator(ENABLE_UNION_TYPE_KEY,
      new OptionDescription("Enable support for Avro union type."));

  // Kafka plugin related options.
  public static final String KAFKA_ALL_TEXT_MODE = "store.kafka.all_text_mode";
  public static final OptionValidator KAFKA_READER_ALL_TEXT_MODE_VALIDATOR = new BooleanValidator(KAFKA_ALL_TEXT_MODE,
      new OptionDescription("Similar to store.json.all_text_mode for Kafka."));
  public static final String KAFKA_READER_READ_NUMBERS_AS_DOUBLE = "store.kafka.read_numbers_as_double";
  public static final OptionValidator KAFKA_READER_READ_NUMBERS_AS_DOUBLE_VALIDATOR = new BooleanValidator(
      KAFKA_READER_READ_NUMBERS_AS_DOUBLE, new OptionDescription("Similar to store.json.read_numbers_as_double."));
  public static final String KAFKA_RECORD_READER = "store.kafka.record.reader";
  public static final OptionValidator KAFKA_RECORD_READER_VALIDATOR = new StringValidator(KAFKA_RECORD_READER,
      new OptionDescription("The Kafka record reader configured to read incoming messages from Kafka."));
  public static final String KAFKA_POLL_TIMEOUT = "store.kafka.poll.timeout";
  public static final PositiveLongValidator KAFKA_POLL_TIMEOUT_VALIDATOR = new PositiveLongValidator(KAFKA_POLL_TIMEOUT, Long.MAX_VALUE,
      new OptionDescription("Amount of time in milliseconds allotted to the Kafka client to fetch messages from the Kafka cluster; default value is 200."));
  public static final String KAFKA_READER_SKIP_INVALID_RECORDS = "store.kafka.reader.skip_invalid_records";
  public static final BooleanValidator KAFKA_SKIP_MALFORMED_RECORDS_VALIDATOR = new BooleanValidator(KAFKA_READER_SKIP_INVALID_RECORDS,
    new OptionDescription("Allows queries to progress when the JSON record reader skips bad records in JSON files. Default is false. (Drill 1.17+)"));
  public static final String KAFKA_READER_NAN_INF_NUMBERS = "store.kafka.reader.allow_nan_inf";
  public static final BooleanValidator KAFKA_READER_NAN_INF_NUMBERS_VALIDATOR = new BooleanValidator(KAFKA_READER_NAN_INF_NUMBERS,
    new OptionDescription("Enables the Kafka JSON record reader in Drill to read `NaN` and `Infinity` tokens in JSON data as numbers. Default is true. (Drill 1.17+)"));
  public static final String KAFKA_READER_ESCAPE_ANY_CHAR = "store.kafka.reader.allow_escape_any_char";
  public static final BooleanValidator KAFKA_READER_ESCAPE_ANY_CHAR_VALIDATOR = new BooleanValidator(KAFKA_READER_ESCAPE_ANY_CHAR,
    new OptionDescription("Enables the Kafka JSON record reader in Drill to escape any character. Default is false. (Drill 1.17+)"));


  // TODO: We need to add a feature that enables storage plugins to add their own options. Currently we have to declare
  // in core which is not right. Move this option and above two mongo plugin related options once we have the feature.
  @Deprecated // TODO: DRILL-6527
  public static final String HIVE_OPTIMIZE_SCAN_WITH_NATIVE_READERS = "store.hive.optimize_scan_with_native_readers";
  @Deprecated // TODO: DRILL-6527
  public static final OptionValidator HIVE_OPTIMIZE_SCAN_WITH_NATIVE_READERS_VALIDATOR = new BooleanValidator(HIVE_OPTIMIZE_SCAN_WITH_NATIVE_READERS,
      new OptionDescription("Deprecated as of Drill 1.14. Use the store.hive.parquet.optimize_scan_with_native_reader option instead. Enables Drill to use the Drill native reader (instead of the Hive Serde interface) to optimize reads of Parquet-backed tables from Hive. Default is false."));
  public static final String HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER = "store.hive.parquet.optimize_scan_with_native_reader";
  public static final OptionValidator HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER_VALIDATOR =
      new BooleanValidator(HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER,
          new OptionDescription("Optimize reads of Parquet-backed external tables from Hive by using Drill native readers instead of the Hive Serde interface. (Drill 1.2+)"));
  public static final String HIVE_OPTIMIZE_MAPRDB_JSON_SCAN_WITH_NATIVE_READER = "store.hive.maprdb_json.optimize_scan_with_native_reader";
  public static final OptionValidator HIVE_OPTIMIZE_MAPRDB_JSON_SCAN_WITH_NATIVE_READER_VALIDATOR =
      new BooleanValidator(HIVE_OPTIMIZE_MAPRDB_JSON_SCAN_WITH_NATIVE_READER,
          new OptionDescription("Enables Drill to use the Drill native reader (instead of the Hive Serde interface) to optimize reads of MapR Database JSON tables from Hive. Default is false. (Drill 1.14+)"));

  public static final String HIVE_READ_MAPRDB_JSON_TIMESTAMP_WITH_TIMEZONE_OFFSET = "store.hive.maprdb_json.read_timestamp_with_timezone_offset";
  public static final OptionValidator HIVE_READ_MAPRDB_JSON_TIMESTAMP_WITH_TIMEZONE_OFFSET_VALIDATOR =
      new BooleanValidator(HIVE_READ_MAPRDB_JSON_TIMESTAMP_WITH_TIMEZONE_OFFSET,
          new OptionDescription("Enables Drill to read timestamp values with timezone offset when Hive plugin is used and Drill native MaprDB JSON reader usage is enabled. (Drill 1.16+)"));

  public static final String HIVE_MAPRDB_JSON_ALL_TEXT_MODE = "store.hive.maprdb_json.all_text_mode";
  public static final OptionValidator HIVE_MAPRDB_JSON_ALL_TEXT_MODE_VALIDATOR =
      new BooleanValidator(HIVE_MAPRDB_JSON_ALL_TEXT_MODE,
          new OptionDescription("Drill reads all data from the maprDB Json tables as VARCHAR when hive plugin is used and Drill native MaprDB JSON reader usage is enabled. Prevents schema change errors. (Drill 1.17+)"));

  public static final String HIVE_CONF_PROPERTIES = "store.hive.conf.properties";
  public static final OptionValidator HIVE_CONF_PROPERTIES_VALIDATOR = new StringValidator(HIVE_CONF_PROPERTIES,
      new OptionDescription("Enables the user to specify Hive properties at the session level. Do not set the property values in quotes. Separate the property name and value by =. Separate each property with a new line (\\n). Example: set `store.hive.conf.properties` = 'hive.mapred.supports.subdirectories=true\\nmapred.input.dir.recursive=true'. (Drill 1.14+)"));

  public static final String SLICE_TARGET = "planner.slice_target";
  public static final long SLICE_TARGET_DEFAULT = 100000L;
  public static final PositiveLongValidator SLICE_TARGET_OPTION = new PositiveLongValidator(SLICE_TARGET, Long.MAX_VALUE,
      new OptionDescription("The number of records manipulated within a fragment before Drill parallelizes operations."));

  public static final String CAST_EMPTY_STRING_TO_NULL = "drill.exec.functions.cast_empty_string_to_null";
  public static final BooleanValidator CAST_EMPTY_STRING_TO_NULL_OPTION = new BooleanValidator(CAST_EMPTY_STRING_TO_NULL,
      new OptionDescription("In a text file, treat empty fields as NULL values instead of empty string."));

  /**
   * HashTable runtime settings
   */
  public static final String MIN_HASH_TABLE_SIZE_KEY = "exec.min_hash_table_size";
  public static final PositiveLongValidator MIN_HASH_TABLE_SIZE = new PositiveLongValidator(MIN_HASH_TABLE_SIZE_KEY, HashTable.MAXIMUM_CAPACITY,
      new OptionDescription("Starting size in bucketsfor hash tables. Increase according to available memory to improve performance. Increasing for very large aggregations or joins when you have large amounts of memory for Drill to use. Range: 0 - 1073741824."));
  public static final String MAX_HASH_TABLE_SIZE_KEY = "exec.max_hash_table_size";
  public static final PositiveLongValidator MAX_HASH_TABLE_SIZE = new PositiveLongValidator(MAX_HASH_TABLE_SIZE_KEY, HashTable.MAXIMUM_CAPACITY,
      new OptionDescription("Ending size in buckets for hash tables. Range: 0 - 1073741824."));

  /**
   * Limits the maximum level of parallelization to this factor time the number of Drillbits
   */
  public static final String CPU_LOAD_AVERAGE_KEY = "planner.cpu_load_average";
  public static final DoubleValidator CPU_LOAD_AVERAGE = new DoubleValidator(CPU_LOAD_AVERAGE_KEY,
      new OptionDescription("Limits the maximum level of parallelization to this factor time the number of Drillbits"));
  public static final String MAX_WIDTH_PER_NODE_KEY = "planner.width.max_per_node";
  public static final MaxWidthValidator MAX_WIDTH_PER_NODE = new MaxWidthValidator(MAX_WIDTH_PER_NODE_KEY,
      new OptionDescription("Maximum number of threads that can run in parallel for a query on a node. A slice is an individual thread. This number indicates the maximum number of slices per query for the query's major fragment on a node.",
          "Max number of threads that can run in parallel for a query on a node."));

  /**
   * The maximum level or parallelization any stage of the query can do. Note that while this
   * might be the number of active Drillbits, realistically, this could be well beyond that
   * number of we want to do things like speed results return.
   */
  public static final String MAX_WIDTH_GLOBAL_KEY = "planner.width.max_per_query";
  public static final OptionValidator MAX_WIDTH_GLOBAL = new PositiveLongValidator(MAX_WIDTH_GLOBAL_KEY, Integer.MAX_VALUE,
      new OptionDescription("Same as max per node but applies to the query as executed by the entire cluster. For example, this value might be the number of active Drillbits, or a higher number to return results faster."));

  /**
   * Factor by which a node with endpoint affinity will be favored while creating assignment
   */
  public static final String AFFINITY_FACTOR_KEY = "planner.affinity_factor";
  public static final OptionValidator AFFINITY_FACTOR = new DoubleValidator(AFFINITY_FACTOR_KEY,
      new OptionDescription("Factor by which a node with endpoint affinity will be favored while creating assignment"));

  public static final String EARLY_LIMIT0_OPT_KEY = "planner.enable_limit0_optimization";
  public static final BooleanValidator EARLY_LIMIT0_OPT = new BooleanValidator(EARLY_LIMIT0_OPT_KEY,
      new OptionDescription("Enables the query planner to determine data types returned by a query during the planning phase before scanning data. Default is true. (Drill 1.9+)"));

  public static final String LATE_LIMIT0_OPT_KEY = "planner.enable_limit0_on_scan";
  public static final BooleanValidator LATE_LIMIT0_OPT = new BooleanValidator(LATE_LIMIT0_OPT_KEY,
      new OptionDescription("Enables Drill to determine data types as Drill scans data. This optimization is used when the query planner cannot infer types of columns during validation (prior to scanning). Drill exits and terminates the query immediately after resolving the types. When this optimization is applied, the query plan contains a LIMIT (0) above every SCAN, with an optional PROJECT in between. Default is true. (Drill 1.14+)"));

  public static final String ENABLE_MEMORY_ESTIMATION_KEY = "planner.memory.enable_memory_estimation";
  public static final OptionValidator ENABLE_MEMORY_ESTIMATION = new BooleanValidator(ENABLE_MEMORY_ESTIMATION_KEY,
      new OptionDescription("Toggles the state of memory estimation and re-planning of the query. When enabled, Drill conservatively estimates memory requirements and typically excludes these operators from the plan and negatively impacts performance."));

  /**
   * Maximum query memory per node (in MB). Re-plan with cheaper operators if
   * memory estimation exceeds this limit.
   * <p/>
   * DEFAULT: 2048 MB
   */
  public static final String MAX_QUERY_MEMORY_PER_NODE_KEY = "planner.memory.max_query_memory_per_node";
  public static final LongValidator MAX_QUERY_MEMORY_PER_NODE = new RangeLongValidator(MAX_QUERY_MEMORY_PER_NODE_KEY, 1024 * 1024, DrillConfig.getMaxDirectMemory(),
      new OptionDescription("Sets the maximum amount of direct memory allocated to the Sort and Hash Aggregate operators during each query on a node. This memory is split between operators. If a query plan contains multiple Sort and/or Hash Aggregate operators, the memory is divided between them. The default limit should be increased for queries on large data sets."));

  /**
   * Alternative way to compute per-query-per-node memory as a percent
   * of the total available system memory.
   * <p>
   * Suggestion for computation.
   * <ul>
   * <li>Assume an allowance for non-managed operators. Default assumption:
   * 50%</li>
   * <li>Assume a desired number of concurrent queries. Default assumption:
   * 10.</li>
   * <li>The value of this parameter is<br>
   * (1 - non-managed allowance) / concurrency</li>
   * </ul>
   * Doing the math produces the default 5% number. The actual number
   * given is no less than the <tt>max_query_memory_per_node</tt>
   * amount.
   * <p>
   * This number is used only when throttling is disabled. Setting the
   * number to 0 effectively disables this technique as it will always
   * produce values lower than <tt>max_query_memory_per_node</tt>.
   * <p>
   * DEFAULT: 5%
   */

  public static String PERCENT_MEMORY_PER_QUERY_KEY = "planner.memory.percent_per_query";
  public static DoubleValidator PERCENT_MEMORY_PER_QUERY = new RangeDoubleValidator(
      PERCENT_MEMORY_PER_QUERY_KEY, 0, 1.0, new OptionDescription("Sets the memory as a percentage of the total direct memory."));

  /**
   * Minimum memory allocated to each buffered operator instance.
   * <p/>
   * DEFAULT: 40 MB
   */
  public static final String MIN_MEMORY_PER_BUFFERED_OP_KEY = "planner.memory.min_memory_per_buffered_op";
  public static final LongValidator MIN_MEMORY_PER_BUFFERED_OP = new RangeLongValidator(MIN_MEMORY_PER_BUFFERED_OP_KEY, 1024 * 1024, Long.MAX_VALUE,
      new OptionDescription("Minimum memory allocated to each buffered operator instance"));

  /**
   * Extra query memory per node for non-blocking operators.
   * NOTE: This option is currently used only for memory estimation.
   * <p/>
   * DEFAULT: 64 MB
   * MAXIMUM: 2048 MB
   */
  public static final String NON_BLOCKING_OPERATORS_MEMORY_KEY = "planner.memory.non_blocking_operators_memory";
  public static final OptionValidator NON_BLOCKING_OPERATORS_MEMORY = new PowerOfTwoLongValidator(
      NON_BLOCKING_OPERATORS_MEMORY_KEY, 1 << 11,
      new OptionDescription("Extra query memory per node for non-blocking operators. This option is currently used only for memory estimation. Range: 0-2048 MB"));

  public static final String HASH_JOIN_TABLE_FACTOR_KEY = "planner.memory.hash_join_table_factor";
  public static final OptionValidator HASH_JOIN_TABLE_FACTOR = new DoubleValidator(HASH_JOIN_TABLE_FACTOR_KEY,
      new OptionDescription("A heuristic value for influencing the size of the hash aggregation table."));

  public static final String HASH_AGG_TABLE_FACTOR_KEY = "planner.memory.hash_agg_table_factor";
  public static final OptionValidator HASH_AGG_TABLE_FACTOR = new DoubleValidator(HASH_AGG_TABLE_FACTOR_KEY,
      new OptionDescription("A heuristic value for influencing the size of the hash aggregation table."));

  public static final String AVERAGE_FIELD_WIDTH_KEY = "planner.memory.average_field_width";
  public static final OptionValidator AVERAGE_FIELD_WIDTH = new PositiveLongValidator(AVERAGE_FIELD_WIDTH_KEY, Long.MAX_VALUE,
      new OptionDescription("Used in estimating memory requirements."));

  // Mux Exchange options.
  public static final String ORDERED_MUX_EXCHANGE = "planner.enable_ordered_mux_exchange";

  // Resource management boot-time options.
  public static final String RM_ENABLED = "drill.exec.rm.enabled";
  public static final String MAX_MEMORY_PER_NODE = "drill.exec.rm.memory_per_node";
  public static final String MAX_CPUS_PER_NODE = "drill.exec.rm.cpus_per_node";

  // Resource management system run-time options.

  // Enables queues. When running embedded, enables an in-process queue. When
  // running distributed, enables the Zookeeper-based distributed queue.

  public static final BooleanValidator ENABLE_QUEUE = new BooleanValidator("exec.queue.enable",
      new OptionDescription("Changes the state of query queues. False allows unlimited concurrent queries."));
  public static final LongValidator LARGE_QUEUE_SIZE = new PositiveLongValidator("exec.queue.large", 10_000,
      new OptionDescription("Sets the number of large queries that can run concurrently in the cluster. Range: 0-1000"));
  public static final LongValidator SMALL_QUEUE_SIZE = new PositiveLongValidator("exec.queue.small", 100_000,
      new OptionDescription("Sets the number of small queries that can run concurrently in the cluster. Range: 0-1001"));
  public static final LongValidator QUEUE_THRESHOLD_SIZE = new PositiveLongValidator("exec.queue.threshold", Long.MAX_VALUE,
      new OptionDescription("Sets the cost threshold, which depends on the complexity of the queries in queue, for determining whether query is large or small. Complex queries have higher thresholds. Range: 0-9223372036854775807"));
  public static final LongValidator QUEUE_TIMEOUT = new PositiveLongValidator("exec.queue.timeout_millis", Long.MAX_VALUE,
      new OptionDescription("Indicates how long a query can wait in queue before the query fails. Range: 0-9223372036854775807"));

  // New Smart RM boot time configs
  public static final String RM_QUERY_TAGS_KEY = "exec.rm.queryTags";
  public static final StringValidator RM_QUERY_TAGS_VALIDATOR = new StringValidator(RM_QUERY_TAGS_KEY,
    new OptionDescription("Allows user to set coma separated list of tags for all the queries submitted over a session"));

  public static final String RM_QUEUES_WAIT_FOR_PREFERRED_NODES_KEY = "exec.rm.queues.wait_for_preferred_nodes";
  public static final BooleanValidator RM_QUEUES_WAIT_FOR_PREFERRED_NODES_VALIDATOR = new BooleanValidator
    (RM_QUEUES_WAIT_FOR_PREFERRED_NODES_KEY, new OptionDescription("Allows user to enable/disable " +
      "wait_for_preferred_nodes configuration across rm queues for all the queries submitted over a session"));

  // Ratio of memory for small queries vs. large queries.
  // Each small query gets 1 unit, each large query gets QUEUE_MEMORY_RATIO units.
  // A lower limit of 1 enforces the intuition that a large query should never get
  // *less* memory than a small one.

  public static final DoubleValidator QUEUE_MEMORY_RATIO = new RangeDoubleValidator("exec.queue.memory_ratio", 1.0, 1000, null);

  public static final DoubleValidator QUEUE_MEMORY_RESERVE = new RangeDoubleValidator("exec.queue.memory_reserve_ratio", 0, 1.0, null);

  public static final String ENABLE_VERBOSE_ERRORS_KEY = "exec.errors.verbose";
  public static final OptionValidator ENABLE_VERBOSE_ERRORS = new BooleanValidator(ENABLE_VERBOSE_ERRORS_KEY,
      new OptionDescription("Toggles verbose output of executable error messages"));

  /**
   * Key used in earlier versions to use the original ("V1") text reader. Since at least Drill 1.8
   * users have used the ("compliant") ("V2") version. Deprecated in Drill 1.17; the "V3" reader
   * with schema support is always used. Retained for backward compatibility, but does
   * nothing.
   */
  @Deprecated
  public static final String ENABLE_NEW_TEXT_READER_KEY = "exec.storage.enable_new_text_reader";
  @Deprecated
  public static final OptionValidator ENABLE_NEW_TEXT_READER = new BooleanValidator(ENABLE_NEW_TEXT_READER_KEY,
      new OptionDescription("Deprecated. Drill's text reader complies with the RFC 4180 standard for text/csv files."));

  /**
   * Flag used in Drill 1.16 to select the row-set based ("V3") or the original
   * "compliant" ("V2") text reader. In Drill 1.17, the "V3" version is always
   * used. Retained for backward compatibility, but does nothing.
   */
  @Deprecated
  public static final String ENABLE_V3_TEXT_READER_KEY = "exec.storage.enable_v3_text_reader";
  @Deprecated
  public static final OptionValidator ENABLE_V3_TEXT_READER = new BooleanValidator(ENABLE_V3_TEXT_READER_KEY,
      new OptionDescription("Deprecated. The \"V3\" text reader is always used."));

  public static final String MIN_READER_WIDTH_KEY = "exec.storage.min_width";
  public static final OptionValidator MIN_READER_WIDTH = new LongValidator(MIN_READER_WIDTH_KEY,
      new OptionDescription("Min width for text readers, mostly for testing."));

  public static final String SKIP_RUNTIME_ROWGROUP_PRUNING_KEY = "exec.storage.skip_runtime_rowgroup_pruning";
  public static final OptionValidator SKIP_RUNTIME_ROWGROUP_PRUNING = new BooleanValidator(SKIP_RUNTIME_ROWGROUP_PRUNING_KEY,
    new OptionDescription("Enables skipping the runtime pruning of the rowgroups"));

  public static final String DRILL_SYS_FILE_SUFFIX = ".sys.drill";

  public static final String ENABLE_WINDOW_FUNCTIONS = "window.enable";
  public static final OptionValidator ENABLE_WINDOW_FUNCTIONS_VALIDATOR = new BooleanValidator(ENABLE_WINDOW_FUNCTIONS,
      new OptionDescription("Enable or disable window functions in Drill 1.1+"));

  public static final String DRILLBIT_CONTROL_INJECTIONS = "drill.exec.testing.controls";
  public static final OptionValidator DRILLBIT_CONTROLS_VALIDATOR = new ExecutionControls.ControlsOptionValidator(DRILLBIT_CONTROL_INJECTIONS, 1, null);

  public static final String NEW_VIEW_DEFAULT_PERMS_KEY = "new_view_default_permissions";
  public static final OptionValidator NEW_VIEW_DEFAULT_PERMS_VALIDATOR = new StringValidator(NEW_VIEW_DEFAULT_PERMS_KEY,
      new OptionDescription("Sets view permissions using an octal code in the Unix tradition."));

  public static final String CTAS_PARTITIONING_HASH_DISTRIBUTE = "store.partition.hash_distribute";
  public static final BooleanValidator CTAS_PARTITIONING_HASH_DISTRIBUTE_VALIDATOR = new BooleanValidator(CTAS_PARTITIONING_HASH_DISTRIBUTE,
      new OptionDescription("Uses a hash algorithm to distribute data on partition keys in a CTAS partitioning operation. An alpha option--for experimental use at this stage. Do not use in production systems."));


  /**
   * @deprecated option. It will not take any effect.
   * The option added as part of DRILL-4577, was used to mark that hive tables should be loaded
   * for all table names at once. Then as part of DRILL-4826 was added option to regulate bulk size,
   * because big amount of views was causing performance degradation. After last improvements for
   * DRILL-7115 both options ({@link ExecConstants#ENABLE_BULK_LOAD_TABLE_LIST_KEY}
   * and {@link ExecConstants#BULK_LOAD_TABLE_LIST_BULK_SIZE_KEY}) became obsolete and may be removed
   * in future releases.
   */
  @Deprecated
  public static final String ENABLE_BULK_LOAD_TABLE_LIST_KEY = "exec.enable_bulk_load_table_list";

  /**
   * @see ExecConstants#ENABLE_BULK_LOAD_TABLE_LIST_KEY
   */
  @Deprecated
  public static final BooleanValidator ENABLE_BULK_LOAD_TABLE_LIST = new BooleanValidator(ENABLE_BULK_LOAD_TABLE_LIST_KEY,
      new OptionDescription("Deprecated after DRILL-7115 improvement."));

  /**
   * @see ExecConstants#ENABLE_BULK_LOAD_TABLE_LIST_KEY
   */
  @Deprecated
  public static final String BULK_LOAD_TABLE_LIST_BULK_SIZE_KEY = "exec.bulk_load_table_list.bulk_size";

  /**
   * @see ExecConstants#ENABLE_BULK_LOAD_TABLE_LIST_KEY
   */
  @Deprecated
  public static final PositiveLongValidator BULK_LOAD_TABLE_LIST_BULK_SIZE = new PositiveLongValidator(BULK_LOAD_TABLE_LIST_BULK_SIZE_KEY, Integer.MAX_VALUE,
      new OptionDescription("Deprecated after DRILL-7115 improvement."));

  /**
   * Option whose value is a comma separated list of admin usernames. Admin users are users who have special privileges
   * such as changing system options.
   */
  public static final String ADMIN_USERS_KEY = "security.admin.users";
  public static final AdminUsersValidator ADMIN_USERS_VALIDATOR = new AdminUsersValidator(ADMIN_USERS_KEY,
      new OptionDescription("A comma-separated list of user names with administrator privileges."));

  /**
   * Option whose value is a comma separated list of admin usergroups.
   */
  public static final String ADMIN_USER_GROUPS_KEY = "security.admin.user_groups";
  public static final AdminUserGroupsValidator ADMIN_USER_GROUPS_VALIDATOR =
      new AdminUserGroupsValidator(ADMIN_USER_GROUPS_KEY, new OptionDescription("A comma-separated list of user groups with administrator privileges."));
  /**
   * Option whose value is a string representing list of inbound impersonation policies.
   *
   * Impersonation policy format:
   * [
   *   {
   *    proxy_principals : { users : ["..."], groups : ["..."] },
   *    target_principals : { users : ["..."], groups : ["..."] }
   *   },
   *   ...
   * ]
   */
  public static final String IMPERSONATION_POLICIES_KEY = "exec.impersonation.inbound_policies";
  public static final StringValidator IMPERSONATION_POLICY_VALIDATOR =
      new InboundImpersonationManager.InboundImpersonationPolicyValidator(IMPERSONATION_POLICIES_KEY);


  /**
   * Web settings
   */
  public static final String WEB_LOGS_MAX_LINES = "web.logs.max_lines";
  public static final OptionValidator WEB_LOGS_MAX_LINES_VALIDATOR = new PositiveLongValidator(WEB_LOGS_MAX_LINES, Integer.MAX_VALUE,
      new OptionDescription("Provides the maximum number of log file lines that display on the Logs tab in the Drill Web UI. (Drill 1.7+)"));

  public static final String WEB_DISPLAY_FORMAT_TIMESTAMP = "web.display_format.timestamp";
  public static final OptionValidator WEB_DISPLAY_FORMAT_TIMESTAMP_VALIDATOR = new DateTimeFormatValidator(WEB_DISPLAY_FORMAT_TIMESTAMP,
      new OptionDescription("Display format template for timestamp. "
          + "It will be passed to java.time.format.DateTimeFormatter. "
          + "See https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html for the details about acceptable patterns. "
          + "If empty then the default formatting will be used. (Drill 1.15+)"));

  public static final String WEB_DISPLAY_FORMAT_DATE = "web.display_format.date";
  public static final OptionValidator WEB_DISPLAY_FORMAT_DATE_VALIDATOR = new DateTimeFormatValidator(WEB_DISPLAY_FORMAT_DATE,
      new OptionDescription("Display format template for date. "
          + "It will be passed to java.time.format.DateTimeFormatter. "
          + "See https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html for the details about acceptable patterns. "
          + "If empty then the default formatting will be used. (Drill 1.15+)"));

  public static final String WEB_DISPLAY_FORMAT_TIME = "web.display_format.time";
  public static final OptionValidator WEB_DISPLAY_FORMAT_TIME_VALIDATOR = new DateTimeFormatValidator(WEB_DISPLAY_FORMAT_TIME,
      new OptionDescription("Display format template for time. "
          + "It will be passed to java.time.format.DateTimeFormatter. "
          + "See https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html for the details about acceptable patterns. "
          + "If empty then the default formatting will be used. (Drill 1.15+)"));

  public static final String CODE_GEN_EXP_IN_METHOD_SIZE = "exec.java.compiler.exp_in_method_size";
  public static final LongValidator CODE_GEN_EXP_IN_METHOD_SIZE_VALIDATOR = new LongValidator(CODE_GEN_EXP_IN_METHOD_SIZE,
      new OptionDescription("Introduced in Drill 1.8. For queries with complex or multiple expressions in the query logic, this option limits the number of expressions allowed in each method to prevent Drill from generating code that exceeds the Java limit of 64K bytes. If a method approaches the 64K limit, the Java compiler returns a message stating that the code is too large to compile. If queries return such a message, reduce the value of this option at the session level. The default value for this option is 50. The value is the count of expressions allowed in a method. Expressions are added to a method until they hit the Java 64K limit, when a new inner method is created and called from the existing method. Note: This logic has not been implemented for all operators. If a query uses operators for which the logic is not implemented, reducing the setting for this option may not resolve the error. Setting this option at the system level impacts all queries and can degrade query performance."));

  public static final String CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS = "prepare.statement.create_timeout_ms";
  public static final OptionValidator CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS_VALIDATOR =
      new PositiveLongValidator(CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS, Integer.MAX_VALUE, null);

  public static final String DYNAMIC_UDF_SUPPORT_ENABLED = "exec.udf.enable_dynamic_support";
  public static final BooleanValidator DYNAMIC_UDF_SUPPORT_ENABLED_VALIDATOR = new BooleanValidator(DYNAMIC_UDF_SUPPORT_ENABLED,
      new OptionDescription("Enables users to dynamically upload UDFs. Users must upload their UDF (source and binary) JAR files to a staging directory in the distributed file system before issuing the CREATE FUNCTION USING JAR command to register a UDF. Default is true. (Drill 1.9+)"));

  //Display estimated rows in operator overview by default
  public static final String PROFILE_STATISTICS_ESTIMATED_ROWS_SHOW = "drill.exec.http.profile.statistics.estimated_rows.show";
  //Trigger warning in UX if fragments appear to be doing no work (units are in seconds).
  public static final String PROFILE_WARNING_PROGRESS_THRESHOLD = "drill.exec.http.profile.warning.progress.threshold";
  //Trigger warning in UX if slowest fragment operator crosses min threshold and exceeds ratio with average (units are in seconds).
  public static final String PROFILE_WARNING_TIME_SKEW_MIN = "drill.exec.http.profile.warning.time.skew.min";
  //Threshold Ratio for Processing (i.e. "maxProcessing : avgProcessing" ratio must exceed this defined threshold to show a skew warning)
  public static final String PROFILE_WARNING_TIME_SKEW_RATIO_PROCESS = "drill.exec.http.profile.warning.time.skew.ratio.process";
  //Trigger warning in UX if slowest fragment SCAN crosses min threshold and exceeds ratio with average (units are in seconds).
  public static final String PROFILE_WARNING_SCAN_WAIT_MIN = "drill.exec.http.profile.warning.scan.wait.min";
  //Threshold Ratio for Waiting (i.e. "maxWait : avgWait" ratio must exceed this defined threshold to show a skew warning)
  public static final String PROFILE_WARNING_TIME_SKEW_RATIO_WAIT = "drill.exec.http.profile.warning.time.skew.ratio.wait";

  /**
   * Option to save query profiles. If false, no query profile will be saved
   * for any query.
   */
  public static final String ENABLE_QUERY_PROFILE_OPTION = "exec.query_profile.save";
  public static final BooleanValidator ENABLE_QUERY_PROFILE_VALIDATOR = new BooleanValidator(ENABLE_QUERY_PROFILE_OPTION, new OptionDescription("Save completed profiles to the persistent store"));
  //Allow to skip writing Alter Session profiles
  public static final String SKIP_ALTER_SESSION_QUERY_PROFILE = "exec.query_profile.alter_session.skip";
  public static final BooleanValidator SKIP_SESSION_QUERY_PROFILE_VALIDATOR = new BooleanValidator(SKIP_ALTER_SESSION_QUERY_PROFILE, new OptionDescription("Skip saving ALTER SESSION profiles"));

  /**
   * Profiles are normally written after the last client message to reduce latency.
   * When running tests, however, we want the profile written <i>before</i> the
   * return so that the client can immediately read the profile for test
   * verification.
   */
  public static final String QUERY_PROFILE_DEBUG_OPTION = "exec.query_profile.debug_mode";
  public static final BooleanValidator QUERY_PROFILE_DEBUG_VALIDATOR = new BooleanValidator(QUERY_PROFILE_DEBUG_OPTION, null);

  public static final String USE_DYNAMIC_UDFS_KEY = "exec.udf.use_dynamic";
  public static final BooleanValidator USE_DYNAMIC_UDFS = new BooleanValidator(USE_DYNAMIC_UDFS_KEY,
      new OptionDescription("Enables Drill to build an operator table for built-in static functions and then reuse the table across queries. Drill uses the operator table for all queries that do not need dynamic UDF support. The operator table does not include dynamic UDFs. Default is true. (Drill 1.10+)"));

  public static final String QUERY_TRANSIENT_STATE_UPDATE_KEY = "exec.query.progress.update";
  public static final BooleanValidator QUERY_TRANSIENT_STATE_UPDATE = new BooleanValidator(QUERY_TRANSIENT_STATE_UPDATE_KEY, null);

  public static final String PERSISTENT_TABLE_UMASK = "exec.persistent_table.umask";
  public static final StringValidator PERSISTENT_TABLE_UMASK_VALIDATOR = new StringValidator(PERSISTENT_TABLE_UMASK,
      new OptionDescription("Enables users to modify permissions on directories and files that result from running the CTAS command. The default is 002, which sets the default directory permissions to 775 and default file permissions to 664. (Drill 1.11+)"));

  /**
   * Enables batch iterator (operator) validation. Validation is normally enabled
   * only when assertions are enabled. This option enables iterator validation even
   * if assertions are not enabled. That is, it allows iterator validation even on
   * a "production" Drill instance.
   */
  public static final String ENABLE_ITERATOR_VALIDATION_OPTION = "debug.validate_iterators";
  public static final BooleanValidator ENABLE_ITERATOR_VALIDATOR = new BooleanValidator(ENABLE_ITERATOR_VALIDATION_OPTION, null);

  /**
   * Boot-time config option to enable validation. Primarily used for tests.
   * If true, overrrides the above. (That is validation is done if assertions are on,
   * if the above session option is set to true, or if this config option is set to true.
   */
  public static final String ENABLE_ITERATOR_VALIDATION = "drill.exec.debug.validate_iterators";

  public static final String QUERY_ROWKEYJOIN_BATCHSIZE_KEY = "exec.query.rowkeyjoin_batchsize";
  public static final PositiveLongValidator QUERY_ROWKEYJOIN_BATCHSIZE = new PositiveLongValidator(QUERY_ROWKEYJOIN_BATCHSIZE_KEY, Long.MAX_VALUE,
      new OptionDescription("Batch size (in terms of number of rows) for a 'bulk get' operation from the underlying data source during a RowKeyJoin."));
  /**
   * When iterator validation is enabled, additionally validates the vectors in
   * each batch passed to each iterator.
   */
  public static final String ENABLE_VECTOR_VALIDATION_OPTION = "debug.validate_vectors";
  public static final BooleanValidator ENABLE_VECTOR_VALIDATOR = new BooleanValidator(ENABLE_VECTOR_VALIDATION_OPTION, null);

  /**
   * Boot-time config option to enable vector validation. Primarily used for
   * tests. Add the following to the command line to enable:<br>
   * <tt>-ea -Ddrill.exec.debug.validate_vectors=true</tt>
   */
  public static final String ENABLE_VECTOR_VALIDATION = "drill.exec.debug.validate_vectors";

  public static final String OPTION_DEFAULTS_ROOT = "drill.exec.options.";

  public static String bootDefaultFor(String name) {
    return OPTION_DEFAULTS_ROOT + name;
  }
  /**
   * Boot-time config option provided to modify duration of the grace period.
   * Grace period is the amount of time where the drillbit accepts work after
   * the shutdown request is triggered. The primary use of grace period is to
   * avoid the race conditions caused by zookeeper delay in updating the state
   * information of the drillbit that is shutting down. So, it is advisable
   * to have a grace period that is at least twice the amount of zookeeper
   * refresh time.
   */
  public static final String GRACE_PERIOD = "drill.exec.grace_period_ms";

  public static final String DRILL_PORT_HUNT = "drill.exec.port_hunt";

  public static final String ALLOW_LOOPBACK_ADDRESS_BINDING = "drill.exec.allow_loopback_address_binding";

  /** Enables batch size statistics logging */
  public static final String STATS_LOGGING_BATCH_SIZE_OPTION = "drill.exec.stats.logging.batch_size";
  public static final BooleanValidator STATS_LOGGING_BATCH_SIZE_VALIDATOR = new BooleanValidator(STATS_LOGGING_BATCH_SIZE_OPTION,
      new OptionDescription("Enables batch size statistics logging."));

  /** Enables fine-grained batch size statistics logging */
  public static final String STATS_LOGGING_FG_BATCH_SIZE_OPTION = "drill.exec.stats.logging.fine_grained.batch_size";
  public static final BooleanValidator STATS_LOGGING_BATCH_FG_SIZE_VALIDATOR = new BooleanValidator(STATS_LOGGING_FG_BATCH_SIZE_OPTION,
      new OptionDescription("Enables fine-grained batch size statistics logging."));

  /** Controls the list of operators for which batch sizing stats should be enabled */
  public static final String STATS_LOGGING_BATCH_OPERATOR_OPTION = "drill.exec.stats.logging.enabled_operators";
  public static final StringValidator STATS_LOGGING_BATCH_OPERATOR_VALIDATOR = new StringValidator(STATS_LOGGING_BATCH_OPERATOR_OPTION,
      new OptionDescription("Controls the list of operators for which batch sizing statistics should be enabled."));

  public static final String LIST_FILES_RECURSIVELY = "storage.list_files_recursively";
  public static final BooleanValidator LIST_FILES_RECURSIVELY_VALIDATOR = new BooleanValidator(LIST_FILES_RECURSIVELY,
      new OptionDescription("Enables recursive files listing when querying the `INFORMATION_SCHEMA.FILES` table or executing the SHOW FILES command. " +
        "Default is false. (Drill 1.15+)"));

  public static final String RETURN_RESULT_SET_FOR_DDL = "exec.query.return_result_set_for_ddl";
  public static final BooleanValidator RETURN_RESULT_SET_FOR_DDL_VALIDATOR = new BooleanValidator(RETURN_RESULT_SET_FOR_DDL,
      new OptionDescription("Controls whether to return result set for CREATE TABLE / VIEW / FUNCTION, DROP TABLE / VIEW / FUNCTION, " +
          "SET, USE, REFRESH METADATA TABLE queries. If set to false affected rows count will be returned instead and result set will be null. " +
          "Affects JDBC connections only. Default is true. (Drill 1.15+)"));

  /**
   * Option whose value is a long value representing the number of bits required for computing ndv (using HLL).
   * Controls the trade-off between accuracy and memory requirements. The number of bits correlates positively with accuracy
   */
  public static final String HLL_ACCURACY = "exec.statistics.ndv_accuracy";
  public static final LongValidator HLL_ACCURACY_VALIDATOR = new PositiveLongValidator(HLL_ACCURACY, 30,
      new OptionDescription("Controls trade-off between NDV statistic computation memory cost and accuracy"));

  /**
   * Option whose value is a boolean value representing whether to perform deterministic sampling. It translates to using
   * the same (pre-defined) seed for the underlying pseudo-random number generator.
   */
  public static final String DETERMINISTIC_SAMPLING = "exec.statistics.deterministic_sampling";
  public static final BooleanValidator DETERMINISTIC_SAMPLING_VALIDATOR = new BooleanValidator(DETERMINISTIC_SAMPLING,
      new OptionDescription("Deterministic sampling"));

  /**
   * Option whose value is a long value representing the expected number of elements in the bloom filter. The bloom filter
   * computes the number of duplicates which is used for extrapolating the NDV when using sampling. Controls the trade-off
   * between accuracy and memory requirements. The number of elements correlates positively with accuracy.
   */
  public static final String NDV_BLOOM_FILTER_ELEMENTS = "exec.statistics.ndv_extrapolation_bf_elements";
  public static final LongValidator NDV_BLOOM_FILTER_ELEMENTS_VALIDATOR = new PositiveLongValidator(NDV_BLOOM_FILTER_ELEMENTS, Integer.MAX_VALUE,
          new OptionDescription("Controls trade-off between NDV statistic computation memory cost and sampling extrapolation accuracy"));

  /**
   * Option whose value is a double value representing the desired max false positive probability in the bloom filter. The bloom filter
   * computes the number of duplicates which is used for extrapolating the NDV when using sampling. Controls the trade-off
   * between accuracy and memory requirements. The probability correlates negatively with the accuracy.
   */
  public static final String NDV_BLOOM_FILTER_FPOS_PROB = "exec.statistics.ndv_extrapolation_bf_fpprobability";
  public static final LongValidator NDV_BLOOM_FILTER_FPOS_PROB_VALIDATOR = new PositiveLongValidator(NDV_BLOOM_FILTER_FPOS_PROB,
          100, new OptionDescription("Controls trade-off between NDV statistic computation memory cost and sampling extrapolation accuracy"));

  /**
   * Controls the 'compression' factor for the TDigest algorithm.
   */
  public static final String TDIGEST_COMPRESSION = "exec.statistics.tdigest_compression";
  public static final LongValidator TDIGEST_COMPRESSION_VALIDATOR = new PositiveLongValidator(TDIGEST_COMPRESSION, 10000,
    new OptionDescription("Controls trade-off between t-digest quantile statistic storage cost and accuracy. " +
      "Higher values use more groups (clusters) for the t-digest and improve accuracy at the expense of extra storage. "));

  /**
   * Options that have a JDBC Statement implementation already in place
   */
  public static final String QUERY_MAX_ROWS = "exec.query.max_rows";
  public static final RangeLongValidator QUERY_MAX_ROWS_VALIDATOR = new RangeLongValidator(QUERY_MAX_ROWS, 0, Integer.MAX_VALUE,
      new OptionDescription("The maximum number of rows that the query will return. This can be only set at a SYSTEM level by an admin. (Drill 1.16+)"));

  /**
   * Option that enables Drill Metastore usage.
   */
  public static final String METASTORE_ENABLED = "metastore.enabled";
  public static final BooleanValidator METASTORE_ENABLED_VALIDATOR = new BooleanValidator(METASTORE_ENABLED,
      new OptionDescription("Enables Drill Metastore usage to be able to store table metadata " +
          "during ANALYZE TABLE commands execution and to be able to read table metadata during regular " +
          "queries execution or when querying some INFORMATION_SCHEMA tables. Default is false. (Drill 1.17+)"));

  /**
   * Option for specifying maximum level depth for collecting metadata
   * which will be stored in the Drill Metastore. For example, when {@code FILE} level value
   * is set, {@code ROW_GROUP} level metadata won't be collected and stored into the Metastore.
   */
  public static final String METASTORE_METADATA_STORE_DEPTH_LEVEL = "metastore.metadata.store.depth_level";
  public static final EnumeratedStringValidator METASTORE_METADATA_STORE_DEPTH_LEVEL_VALIDATOR = new EnumeratedStringValidator(METASTORE_METADATA_STORE_DEPTH_LEVEL,
      new OptionDescription("Specifies maximum level depth for collecting metadata. Default is 'ALL'. (Drill 1.17+)"),
      "TABLE", "SEGMENT", "PARTITION", "FILE", "ROW_GROUP", "ALL");

  /**
   * Option for enabling schema usage, stored to the Metastore.
   */
  public static final String METASTORE_USE_SCHEMA_METADATA = "metastore.metadata.use_schema";
  public static final BooleanValidator METASTORE_USE_SCHEMA_METADATA_VALIDATOR = new BooleanValidator(METASTORE_USE_SCHEMA_METADATA,
      new OptionDescription("Enables schema usage, stored to the Metastore. Default is true. (Drill 1.17+)"));

  /**
   * Option for enabling statistics usage, stored in the Metastore, at the planning stage.
   */
  public static final String METASTORE_USE_STATISTICS_METADATA = "metastore.metadata.use_statistics";
  public static final BooleanValidator METASTORE_USE_STATISTICS_METADATA_VALIDATOR = new BooleanValidator(METASTORE_USE_STATISTICS_METADATA,
      new OptionDescription("Enables statistics usage, stored in the Metastore, at the planning stage. Default is true. (Drill 1.17+)"));

  /**
   * Option for collecting schema and / or column statistics for every table after CTAS and CTTAS execution.
   */
  public static final String METASTORE_CTAS_AUTO_COLLECT_METADATA = "metastore.metadata.ctas.auto-collect";
  public static final EnumeratedStringValidator METASTORE_CTAS_AUTO_COLLECT_METADATA_VALIDATOR = new EnumeratedStringValidator(METASTORE_CTAS_AUTO_COLLECT_METADATA,
      new OptionDescription("Specifies whether schema and / or column statistics will be " +
          "automatically collected for every table after CTAS and CTTAS. " +
          "This option is not active for now. Default is 'NONE'. (Drill 1.17+)"),
      "NONE", "ALL", "SCHEMA");

  /**
   * Option for allowing using file metadata cache if required metadata is absent in the Metastore.
   */
  public static final String METASTORE_FALLBACK_TO_FILE_METADATA = "metastore.metadata.fallback_to_file_metadata";
  public static final BooleanValidator METASTORE_FALLBACK_TO_FILE_METADATA_VALIDATOR = new BooleanValidator(METASTORE_FALLBACK_TO_FILE_METADATA,
      new OptionDescription("Allows using file metadata cache for the case when required metadata is absent in the Metastore. " +
          "Default is true. (Drill 1.17+)"));

  /**
   * Option for specifying the number of attempts for retrying query planning after detecting that query metadata is changed.
   */
  public static final String METASTORE_RETRIEVAL_RETRY_ATTEMPTS = "metastore.retrieval.retry_attempts";
  public static final IntegerValidator METASTORE_RETRIEVAL_RETRY_ATTEMPTS_VALIDATOR = new IntegerValidator(METASTORE_RETRIEVAL_RETRY_ATTEMPTS,
      new OptionDescription("Specifies the number of attempts for retrying query planning after detecting that query metadata is changed. " +
          "If the number of retries was exceeded, query will be planned without metadata information from the Metastore. " +
          "Default is 5. (Drill 1.17+)"));

  public static final String PARQUET_READER_ENABLE_MAP_SUPPORT = "store.parquet.reader.enable_map_support";
  public static final BooleanValidator PARQUET_READER_ENABLE_MAP_SUPPORT_VALIDATOR = new BooleanValidator(
      PARQUET_READER_ENABLE_MAP_SUPPORT, new OptionDescription("Enables Drill Parquet reader to read Parquet MAP type correctly. (Drill 1.17+)"));

  // Storage-plugin related config constants

  // Bootstrap plugin files configuration keys
  public static final String BOOTSTRAP_STORAGE_PLUGINS_FILE = "drill.exec.storage.bootstrap.storage";
  public static final String BOOTSTRAP_FORMAT_PLUGINS_FILE =  "drill.exec.storage.bootstrap.format";

  public static final String UPGRADE_STORAGE_PLUGINS_FILE = "drill.exec.storage.upgrade.storage";

  public static final String STORAGE_PLUGIN_REGISTRY_IMPL = "drill.exec.storage.registry";
  public static final String ACTION_ON_STORAGE_PLUGINS_OVERRIDE_FILE = "drill.exec.storage.action_on_plugins_override_file";

  // Extra private plugin classes, used for testing
  public static final String PRIVATE_CONNECTORS = "drill.exec.storage.private_connectors";

  public static final String ENABLE_DYNAMIC_CREDIT_BASED_FC = "exec.enable_dynamic_fc";
  public static final BooleanValidator ENABLE_DYNAMIC_CREDIT_BASED_FC_VALIDATOR = new BooleanValidator(
          ENABLE_DYNAMIC_CREDIT_BASED_FC, new OptionDescription("Enable dynamic credit based flow control.This feature allows " +
          "the sender to send out its data more rapidly, but you should know that it has a risk to OOM when the system is solving parallel " +
          "large queries until we have a more accurate resource manager."));

  public static final String ENABLE_ALIASES = "exec.enable_aliases";
  public static final BooleanValidator ENABLE_ALIASES_VALIDATOR = new BooleanValidator(
    ENABLE_ALIASES, new OptionDescription("Enable storage and tables aliases functionality. (Drill 1.20+)"));

  public static final String ENABLE_REST_VERBOSE_ERRORS_KEY = "drill.exec.http.rest.errors.verbose";
  public static final OptionValidator ENABLE_REST_VERBOSE_ERRORS = new BooleanValidator(ENABLE_REST_VERBOSE_ERRORS_KEY,
      new OptionDescription("Toggles verbose output of executable error messages in rest response"));

  // HTTP proxy configuration (Drill config)
  public static final String NET_PROXY_BASE = "drill.exec.net_proxy";
  // HTTP proxy config
  public static final String HTTP_PROXY_URL = NET_PROXY_BASE + ".http_url";
  public static final String HTTP_PROXY_TYPE = NET_PROXY_BASE + ".http.type";
  public static final String HTTP_PROXY_HOST = NET_PROXY_BASE + ".http.host";
  public static final String HTTP_PROXY_PORT = NET_PROXY_BASE + ".http.port";
  public static final String HTTP_PROXY_USER_NAME = NET_PROXY_BASE + ".http.user_name";
  public static final String HTTP_PROXY_PASSWORD = NET_PROXY_BASE + ".http.password";
  // HTTPS proxy config
  public static final String HTTPS_PROXY_URL = NET_PROXY_BASE + ".https_url";
  public static final String HTTPS_PROXY_TYPE = NET_PROXY_BASE + ".https.type";
  public static final String HTTPS_PROXY_HOST = NET_PROXY_BASE + ".https.host";
  public static final String HTTPS_PROXY_PORT = NET_PROXY_BASE + ".https.port";
  public static final String HTTPS_PROXY_USER_NAME = NET_PROXY_BASE + ".https.user_name";
  public static final String HTTPS_PROXY_PASSWORD = NET_PROXY_BASE + ".https.password";
}

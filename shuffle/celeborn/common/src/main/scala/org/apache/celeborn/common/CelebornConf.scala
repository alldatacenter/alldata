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

package org.apache.celeborn.common

import java.io.IOException
import java.util.{Collection => JCollection, Collections, HashMap => JHashMap, Locale, Map => JMap}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

import org.apache.hadoop.security.UserGroupInformation

import org.apache.celeborn.common.identity.{DefaultIdentityProvider, UserIdentifier}
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.internal.config._
import org.apache.celeborn.common.network.util.ByteUnit
import org.apache.celeborn.common.protocol.{CompressionCodec, PartitionSplitMode, PartitionType, ShuffleMode, SlotsAssignPolicy, TransportModuleConstants}
import org.apache.celeborn.common.protocol.StorageInfo.Type
import org.apache.celeborn.common.protocol.StorageInfo.Type.{HDD, SSD}
import org.apache.celeborn.common.quota.DefaultQuotaManager
import org.apache.celeborn.common.rpc.RpcTimeout
import org.apache.celeborn.common.util.Utils

class CelebornConf(loadDefaults: Boolean) extends Cloneable with Logging with Serializable {

  import CelebornConf._

  /** Create a CelebornConf that loads defaults from system properties and the classpath */
  def this() = this(true)

  private val settings = new ConcurrentHashMap[String, String]()

  @transient private lazy val reader: ConfigReader = {
    val _reader = new ConfigReader(new CelebornConfigProvider(settings))
    _reader.bindEnv(new ConfigProvider {
      override def get(key: String): Option[String] = Option(getenv(key))
    })
    _reader
  }

  private def loadFromMap(props: Map[String, String], silent: Boolean): Unit =
    settings.synchronized {
      // Load any celeborn.* or rss.* system properties
      for ((key, value) <- props if key.startsWith("celeborn.") || key.startsWith("rss.")) {
        set(key, value, silent)
      }
      this
    }

  if (loadDefaults) {
    loadFromMap(Utils.getSystemProperties, false)
  }

  /** Set a configuration variable. */
  def set(key: String, value: String): CelebornConf = {
    set(key, value, false)
  }

  private[celeborn] def set(key: String, value: String, silent: Boolean): CelebornConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException(s"null value for $key")
    }
    if (!silent) {
      logDeprecationWarning(key)
    }
    requireDefaultValueOfRemovedConf(key, value)
    settings.put(key, value)
    this
  }

  def set[T](entry: ConfigEntry[T], value: T): CelebornConf = {
    set(entry.key, entry.stringConverter(value))
    this
  }

  def set[T](entry: OptionalConfigEntry[T], value: T): CelebornConf = {
    set(entry.key, entry.rawStringConverter(value))
    this
  }

  /** Set multiple parameters together */
  def setAll(settings: Traversable[(String, String)]): CelebornConf = {
    settings.foreach { case (k, v) => set(k, v) }
    this
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): CelebornConf = {
    requireDefaultValueOfRemovedConf(key, value)
    if (settings.putIfAbsent(key, value) == null) {
      logDeprecationWarning(key)
    }
    this
  }

  def setIfMissing[T](entry: ConfigEntry[T], value: T): CelebornConf = {
    setIfMissing(entry.key, entry.stringConverter(value))
  }

  def setIfMissing[T](entry: OptionalConfigEntry[T], value: T): CelebornConf = {
    setIfMissing(entry.key, entry.rawStringConverter(value))
  }

  /** Remove a parameter from the configuration */
  def unset(key: String): CelebornConf = {
    settings.remove(key)
    this
  }

  def unset(entry: ConfigEntry[_]): CelebornConf = {
    unset(entry.key)
  }

  def clear(): Unit = {
    settings.clear()
  }

  /** Get a parameter; throws a NoSuchElementException if it's not set */
  def get(key: String): String = {
    getOption(key).getOrElse(throw new NoSuchElementException(key))
  }

  /** Get a parameter, falling back to a default if not set */
  def get(key: String, defaultValue: String): String = {
    getOption(key).getOrElse(defaultValue)
  }

  def get[T](entry: ConfigEntry[T]): T = {
    entry.readFrom(reader)
  }

  /**
   * Get a time parameter as seconds; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then seconds are assumed.
   * @throws java.util.NoSuchElementException If the time parameter is not set
   * @throws NumberFormatException            If the value cannot be interpreted as seconds
   */
  def getTimeAsSeconds(key: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsSeconds(get(key))
  }

  /**
   * Get a time parameter as seconds, falling back to a default if not set. If no
   * suffix is provided then seconds are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as seconds
   */
  def getTimeAsSeconds(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsSeconds(get(key, defaultValue))
  }

  /**
   * Get a time parameter as milliseconds; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then milliseconds are assumed.
   * @throws java.util.NoSuchElementException If the time parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as milliseconds
   */
  def getTimeAsMs(key: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsMs(get(key))
  }

  /**
   * Get a time parameter as milliseconds, falling back to a default if not set. If no
   * suffix is provided then milliseconds are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as milliseconds
   */
  def getTimeAsMs(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.timeStringAsMs(get(key, defaultValue))
  }

  /**
   * Get a size parameter as bytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then bytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key))
  }

  /**
   * Get a size parameter as bytes, falling back to a default if not set. If no
   * suffix is provided then bytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key, defaultValue))
  }

  /**
   * Get a size parameter as bytes, falling back to a default if not set.
   * @throws NumberFormatException If the value cannot be interpreted as bytes
   */
  def getSizeAsBytes(key: String, defaultValue: Long): Long = catchIllegalValue(key) {
    Utils.byteStringAsBytes(get(key, defaultValue + "B"))
  }

  /**
   * Get a size parameter as Kibibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Kibibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Kibibytes
   */
  def getSizeAsKb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsKb(get(key))
  }

  /**
   * Get a size parameter as Kibibytes, falling back to a default if not set. If no
   * suffix is provided then Kibibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Kibibytes
   */
  def getSizeAsKb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsKb(get(key, defaultValue))
  }

  /**
   * Get a size parameter as Mebibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Mebibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Mebibytes
   */
  def getSizeAsMb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsMb(get(key))
  }

  /**
   * Get a size parameter as Mebibytes, falling back to a default if not set. If no
   * suffix is provided then Mebibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Mebibytes
   */
  def getSizeAsMb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsMb(get(key, defaultValue))
  }

  /**
   * Get a size parameter as Gibibytes; throws a NoSuchElementException if it's not set. If no
   * suffix is provided then Gibibytes are assumed.
   * @throws java.util.NoSuchElementException If the size parameter is not set
   * @throws NumberFormatException If the value cannot be interpreted as Gibibytes
   */
  def getSizeAsGb(key: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsGb(get(key))
  }

  /**
   * Get a size parameter as Gibibytes, falling back to a default if not set. If no
   * suffix is provided then Gibibytes are assumed.
   * @throws NumberFormatException If the value cannot be interpreted as Gibibytes
   */
  def getSizeAsGb(key: String, defaultValue: String): Long = catchIllegalValue(key) {
    Utils.byteStringAsGb(get(key, defaultValue))
  }

  /** Get a parameter as an Option */
  def getOption(key: String): Option[String] = {
    Option(settings.get(key)).orElse(getDeprecatedConfig(key, settings))
  }

  /** Get an optional value, applying variable substitution. */
  private[celeborn] def getWithSubstitution(key: String): Option[String] = {
    getOption(key).map(reader.substitute)
  }

  /** Get all parameters as a list of pairs */
  def getAll: Array[(String, String)] = {
    settings.entrySet().asScala.map(x => (x.getKey, x.getValue)).toArray
  }

  /**
   * Get all parameters that start with `prefix`
   */
  def getAllWithPrefix(prefix: String): Array[(String, String)] = {
    getAll.filter { case (k, v) => k.startsWith(prefix) }
      .map { case (k, v) => (k.substring(prefix.length), v) }
  }

  /**
   * Get a parameter as an integer, falling back to a default if not set
   * @throws NumberFormatException If the value cannot be interpreted as an integer
   */
  def getInt(key: String, defaultValue: Int): Int = catchIllegalValue(key) {
    getOption(key).map(_.toInt).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a long, falling back to a default if not set
   * @throws NumberFormatException If the value cannot be interpreted as a long
   */
  def getLong(key: String, defaultValue: Long): Long = catchIllegalValue(key) {
    getOption(key).map(_.toLong).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a double, falling back to a default if not ste
   * @throws NumberFormatException If the value cannot be interpreted as a double
   */
  def getDouble(key: String, defaultValue: Double): Double = catchIllegalValue(key) {
    getOption(key).map(_.toDouble).getOrElse(defaultValue)
  }

  /**
   * Get a parameter as a boolean, falling back to a default if not set
   * @throws IllegalArgumentException If the value cannot be interpreted as a boolean
   */
  def getBoolean(key: String, defaultValue: Boolean): Boolean = catchIllegalValue(key) {
    getOption(key).map(_.toBoolean).getOrElse(defaultValue)
  }

  /** Does the configuration contain a given parameter? */
  def contains(key: String): Boolean = {
    settings.containsKey(key) ||
    configsWithAlternatives.get(key).toSeq.flatten.exists { alt => contains(alt.key) }
  }

  private[celeborn] def contains(entry: ConfigEntry[_]): Boolean = contains(entry.key)

  /** Copy this object */
  override def clone: CelebornConf = {
    val cloned = new CelebornConf(false)
    settings.entrySet().asScala.foreach { e =>
      cloned.set(e.getKey, e.getValue, true)
    }
    cloned
  }

  /**
   * By using this instead of System.getenv(), environment variables can be mocked
   * in unit tests.
   */
  private[celeborn] def getenv(name: String): String = System.getenv(name)

  /**
   * Wrapper method for get() methods which require some specific value format. This catches
   * any [[NumberFormatException]] or [[IllegalArgumentException]] and re-raises it with the
   * incorrectly configured key in the exception message.
   */
  private def catchIllegalValue[T](key: String)(getValue: => T): T = {
    try {
      getValue
    } catch {
      case e: NumberFormatException =>
        // NumberFormatException doesn't have a constructor that takes a cause for some reason.
        throw new NumberFormatException(s"Illegal value for config key $key: ${e.getMessage}")
          .initCause(e)
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(s"Illegal value for config key $key: ${e.getMessage}", e)
    }
  }

  // //////////////////////////////////////////////////////
  //                      Network                        //
  // //////////////////////////////////////////////////////
  def portMaxRetries: Int = get(PORT_MAX_RETRY)
  def networkTimeout: RpcTimeout =
    new RpcTimeout(get(NETWORK_TIMEOUT).milli, NETWORK_TIMEOUT.key)
  def rpcConnectThreads: Int = get(RPC_CONNECT_THREADS)
  def rpcLookupTimeout: RpcTimeout =
    new RpcTimeout(get(RPC_LOOKUP_TIMEOUT).milli, RPC_LOOKUP_TIMEOUT.key)
  def rpcAskTimeout: RpcTimeout =
    new RpcTimeout(get(RPC_ASK_TIMEOUT).milli, RPC_ASK_TIMEOUT.key)
  def haClientRpcAskTimeout: RpcTimeout =
    new RpcTimeout(get(HA_CLIENT_RPC_ASK_TIMEOUT).milli, HA_CLIENT_RPC_ASK_TIMEOUT.key)

  def networkIoMode(module: String): String = {
    val key = NETWORK_IO_MODE.key.replace("<module>", module)
    get(key, NETWORK_IO_MODE.defaultValue.get)
  }

  def networkIoPreferDirectBufs(module: String): Boolean = {
    val key = NETWORK_IO_PREFER_DIRECT_BUFS.key.replace("<module>", module)
    getBoolean(key, NETWORK_IO_PREFER_DIRECT_BUFS.defaultValue.get)
  }

  def networkIoConnectTimeoutMs(module: String): Int = {
    val key = NETWORK_IO_CONNECT_TIMEOUT.key.replace("<module>", module)
    getTimeAsMs(key, NETWORK_CONNECT_TIMEOUT.defaultValueString).toInt
  }

  def networkIoConnectionTimeoutMs(module: String): Int = {
    val key = NETWORK_IO_CONNECTION_TIMEOUT.key.replace("<module>", module)
    getTimeAsMs(key, NETWORK_TIMEOUT.defaultValueString).toInt
  }

  def networkIoNumConnectionsPerPeer(module: String): Int = {
    val key = NETWORK_IO_NUM_CONNECTIONS_PER_PEER.key.replace("<module>", module)
    getInt(key, NETWORK_IO_NUM_CONNECTIONS_PER_PEER.defaultValue.get)
  }

  def networkIoBacklog(module: String): Int = {
    val key = NETWORK_IO_BACKLOG.key.replace("<module>", module)
    getInt(key, NETWORK_IO_BACKLOG.defaultValue.get)
  }

  def networkIoServerThreads(module: String): Int = {
    val key = NETWORK_IO_SERVER_THREADS.key.replace("<module>", module)
    getInt(key, NETWORK_IO_SERVER_THREADS.defaultValue.get)
  }

  def networkIoClientThreads(module: String): Int = {
    val key = NETWORK_IO_CLIENT_THREADS.key.replace("<module>", module)
    getInt(key, NETWORK_IO_CLIENT_THREADS.defaultValue.get)
  }

  def networkIoReceiveBuf(module: String): Int = {
    val key = NETWORK_IO_RECEIVE_BUFFER.key.replace("<module>", module)
    getSizeAsBytes(key, NETWORK_IO_RECEIVE_BUFFER.defaultValueString).toInt
  }

  def networkIoSendBuf(module: String): Int = {
    val key = NETWORK_IO_SEND_BUFFER.key.replace("<module>", module)
    getSizeAsBytes(key, NETWORK_IO_SEND_BUFFER.defaultValueString).toInt
  }

  def networkIoMaxRetries(module: String): Int = {
    val key = NETWORK_IO_MAX_RETRIES.key.replace("<module>", module)
    getInt(key, NETWORK_IO_MAX_RETRIES.defaultValue.get)
  }

  def networkIoRetryWaitMs(module: String): Int = {
    val key = NETWORK_IO_RETRY_WAIT.key.replace("<module>", module)
    getTimeAsMs(key, NETWORK_IO_RETRY_WAIT.defaultValueString).toInt
  }

  def networkIoMemoryMapBytes(module: String): Int = {
    val key = STORAGE_MEMORY_MAP_THRESHOLD.key.replace("<module>", module)
    getSizeAsBytes(key, STORAGE_MEMORY_MAP_THRESHOLD.defaultValueString).toInt
  }

  def networkIoLazyFileDescriptor(module: String): Boolean = {
    val key = NETWORK_IO_LAZY_FD.key.replace("<module>", module)
    getBoolean(key, NETWORK_IO_LAZY_FD.defaultValue.get)
  }

  def networkIoVerboseMetrics(module: String): Boolean = {
    val key = NETWORK_VERBOSE_METRICS.key.replace("<module>", module)
    getBoolean(key, NETWORK_VERBOSE_METRICS.defaultValue.get)
  }

  def networkIoMaxChunksBeingTransferred(module: String): Long = {
    val key = MAX_CHUNKS_BEING_TRANSFERRED.key.replace("<module>", module)
    getSizeAsBytes(key, MAX_CHUNKS_BEING_TRANSFERRED.defaultValueString)
  }

  // //////////////////////////////////////////////////////
  //                      Master                         //
  // //////////////////////////////////////////////////////
  def slotsAssignLoadAwareDiskGroupNum: Int = get(SLOTS_ASSIGN_LOADAWARE_DISKGROUP_NUM)
  def slotsAssignLoadAwareDiskGroupGradient: Double =
    get(SLOTS_ASSIGN_LOADAWARE_DISKGROUP_GRADIENT)
  def slotsAssignExtraSlots: Int = get(SLOTS_ASSIGN_EXTRA_SLOTS)
  def slotsAssignPolicy: SlotsAssignPolicy = SlotsAssignPolicy.valueOf(get(SLOTS_ASSIGN_POLICY))
  def initialEstimatedPartitionSize: Long = get(SHUFFLE_INITIAL_ESRIMATED_PARTITION_SIZE)
  def estimatedPartitionSizeUpdaterInitialDelay: Long =
    get(SHUFFLE_ESTIMATED_PARTITION_SIZE_UPDATE_INITIAL_DELAY)
  def estimatedPartitionSizeForEstimationUpdateInterval: Long =
    get(SHUFFLE_ESTIMATED_PARTITION_SIZE_UPDATE_INTERVAL)
  def metricsAppTopDiskUsageCount: Int = get(METRICS_APP_TOP_DISK_USAGE_COUNT)
  def metricsAppTopDiskUsageWindowSize: Int = get(METRICS_APP_TOP_DISK_USAGE_WINDOW_SIZE)
  def metricsAppTopDiskUsageInterval: Long = get(METRICS_APP_TOP_DISK_USAGE_INTERVAL)
  def metricsResourceConsumptionInterval: Long = get(METRICS_RESOURCE_CONSUMPTION_INTERVAL)

  // //////////////////////////////////////////////////////
  //                      Worker                         //
  // //////////////////////////////////////////////////////
  def workerRpcPort: Int = get(WORKER_RPC_PORT)
  def workerPushPort: Int = get(WORKER_PUSH_PORT)
  def workerFetchPort: Int = get(WORKER_FETCH_PORT)
  def workerReplicatePort: Int = get(WORKER_REPLICATE_PORT)

  def workerPushIoThreads: Option[Int] = get(WORKER_PUSH_IO_THREADS)

  def workerFetchIoThreads: Option[Int] = get(WORKER_FETCH_IO_THREADS)

  def workerReplicateIoThreads: Option[Int] = get(WORKER_REPLICATE_IO_THREADS)
  def registerWorkerTimeout: Long = get(WORKER_REGISTER_TIMEOUT)
  def workerNonEmptyDirExpireDuration: Long = get(WORKER_NON_EMPTY_DIR_EXPIRE_DURATION)
  def workerWorkingDir: String = get(WORKER_WORKING_DIR)
  def workerCloseIdleConnections: Boolean = get(WORKER_CLOSE_IDLE_CONNECTIONS)
  def workerReplicateFastFailDuration: Long = get(WORKER_REPLICATE_FAST_FAIL_DURATION)
  def workerReplicateRandomConnectionEnabled: Boolean =
    get(WORKER_REPLICATE_RANDOM_CONNECTION_ENABLED)
  def workerDeviceStatusCheckTimeout: Long = get(WORKER_DEVICE_STATUS_CHECK_TIMEOUT)
  def workerCheckFileCleanMaxRetries: Int = get(WORKER_CHECK_FILE_CLEAN_MAX_RETRIES)
  def workerCheckFileCleanTimeout: Long = get(WORKER_CHECK_FILE_CLEAN_TIMEOUT)
  def workerHeartbeatTimeout: Long = get(WORKER_HEARTBEAT_TIMEOUT)
  def workerReplicateThreads: Int = get(WORKER_REPLICATE_THREADS)
  def workerCommitThreads: Int = get(WORKER_COMMIT_THREADS)
  def workerShuffleCommitTimeout: Long = get(WORKER_SHUFFLE_COMMIT_TIMEOUT)
  def minPartitionSizeToEstimate: Long = get(SHUFFLE_MIN_PARTITION_SIZE_TO_ESTIMATE)
  def partitionSorterSortPartitionTimeout: Long = get(PARTITION_SORTER_SORT_TIMEOUT)
  def partitionSorterReservedMemoryPerPartition: Long =
    get(PARTITION_SORTER_PER_PARTITION_RESERVED_MEMORY)

  // //////////////////////////////////////////////////////
  //                      Client                         //
  // //////////////////////////////////////////////////////
  def clientMaxTries: Int = get(CLIENT_MAX_RETRIES)
  def clientCloseIdleConnections: Boolean = get(CLIENT_CLOSE_IDLE_CONNECTIONS)
  def shuffleWriterMode: ShuffleMode = ShuffleMode.valueOf(get(SHUFFLE_WRITER_MODE))
  def shuffleForceFallbackEnabled: Boolean = get(SHUFFLE_FORCE_FALLBACK_ENABLED)
  def shuffleForceFallbackPartitionThreshold: Long = get(SHUFFLE_FORCE_FALLBACK_PARTITION_THRESHOLD)
  def shuffleManagerPort: Int = get(SHUFFLE_MANAGER_PORT)
  def shuffleChunkSize: Long = get(SHUFFLE_CHUCK_SIZE)
  def registerShuffleMaxRetry: Int = get(SHUFFLE_REGISTER_MAX_RETRIES)
  def registerShuffleRetryWaitMs: Long = get(SHUFFLE_REGISTER_RETRY_WAIT)
  def reserveSlotsMaxRetries: Int = get(RESERVE_SLOTS_MAX_RETRIES)
  def reserveSlotsRetryWait: Long = get(RESERVE_SLOTS_RETRY_WAIT)
  def rpcMaxParallelism: Int = get(CLIENT_RPC_MAX_PARALLELISM)
  def appHeartbeatTimeoutMs: Long = get(APPLICATION_HEARTBEAT_TIMEOUT)
  def appHeartbeatIntervalMs: Long = get(APPLICATION_HEARTBEAT_INTERVAL)
  def shuffleExpiredCheckIntervalMs: Long = get(SHUFFLE_EXPIRED_CHECK_INTERVAL)
  def workerExcludedCheckIntervalMs: Long = get(WORKER_EXCLUDED_INTERVAL)
  def workerExcludedExpireTimeout: Long = get(WORKER_EXCLUDED_EXPIRE_TIMEOUT)
  def blacklistSlaveEnabled: Boolean = get(BLACKLIST_SLAVE_ENABLED)
  def shuffleRangeReadFilterEnabled: Boolean = get(SHUFFLE_RANGE_READ_FILTER_ENABLED)
  def shufflePartitionType: PartitionType = PartitionType.valueOf(get(SHUFFLE_PARTITION_TYPE))
  def requestCommitFilesMaxRetries: Int = get(COMMIT_FILE_REQUEST_MAX_RETRY)

  // //////////////////////////////////////////////////////
  //               Shuffle Compression                   //
  // //////////////////////////////////////////////////////
  def shuffleCompressionCodec: CompressionCodec =
    CompressionCodec.valueOf(get(SHUFFLE_COMPRESSION_CODEC))
  def shuffleCompressionZstdCompressLevel: Int = get(SHUFFLE_COMPRESSION_ZSTD_LEVEL)

  // //////////////////////////////////////////////////////
  //               Address && HA && RATIS                //
  // //////////////////////////////////////////////////////
  def masterEndpoints: Array[String] =
    get(MASTER_ENDPOINTS).toArray.map { endpoint =>
      Utils.parseHostPort(endpoint) match {
        case (host, 0) => s"$host:${HA_MASTER_NODE_PORT.defaultValue.get}"
        case (host, port) => s"$host:$port"
      }
    }

  def masterHost: String = get(MASTER_HOST)

  def masterPort: Int = get(MASTER_PORT)

  def haEnabled: Boolean = get(HA_ENABLED)

  def haMasterNodeId: Option[String] = get(HA_MASTER_NODE_ID)

  def haMasterNodeIds: Array[String] = {
    def extractPrefix(original: String, stop: String): String = {
      val i = original.indexOf(stop)
      assert(i >= 0, s"$original does not contain $stop")
      original.substring(0, i)
    }

    val nodeConfPrefix = extractPrefix(HA_MASTER_NODE_HOST.key, "<id>")
    getAllWithPrefix(nodeConfPrefix)
      .map(_._1)
      .filterNot(_.equals("id"))
      .map(k => extractPrefix(k, "."))
      .distinct
  }

  def haMasterNodeHost(nodeId: String): String = {
    val key = HA_MASTER_NODE_HOST.key.replace("<id>", nodeId)
    get(key, Utils.localHostName)
  }

  def haMasterNodePort(nodeId: String): Int = {
    val key = HA_MASTER_NODE_PORT.key.replace("<id>", nodeId)
    getInt(key, HA_MASTER_NODE_PORT.defaultValue.get)
  }

  def haMasterRatisHost(nodeId: String): String = {
    val key = HA_MASTER_NODE_RATIS_HOST.key.replace("<id>", nodeId)
    val fallbackKey = HA_MASTER_NODE_HOST.key.replace("<id>", nodeId)
    get(key, get(fallbackKey))
  }

  def haMasterRatisPort(nodeId: String): Int = {
    val key = HA_MASTER_NODE_RATIS_PORT.key.replace("<id>", nodeId)
    getInt(key, HA_MASTER_NODE_RATIS_PORT.defaultValue.get)
  }

  def haMasterRatisRpcType: String = get(HA_MASTER_RATIS_RPC_TYPE)
  def haMasterRatisStorageDir: String = get(HA_MASTER_RATIS_STORAGE_DIR)
  def haMasterRatisLogSegmentSizeMax: Long = get(HA_MASTER_RATIS_LOG_SEGMENT_SIZE_MAX)
  def haMasterRatisLogPreallocatedSize: Long = get(HA_MASTER_RATIS_LOG_PREALLOCATED_SIZE)
  def haMasterRatisLogAppenderQueueNumElements: Int =
    get(HA_MASTER_RATIS_LOG_APPENDER_QUEUE_NUM_ELEMENTS)
  def haMasterRatisLogAppenderQueueBytesLimit: Long =
    get(HA_MASTER_RATIS_LOG_APPENDER_QUEUE_BYTE_LIMIT)
  def haMasterRatisLogPurgeGap: Int = get(HA_MASTER_RATIS_LOG_PURGE_GAP)
  def haMasterRatisLogInstallSnapshotEnabled: Boolean =
    get(HA_MASTER_RATIS_LOG_INSTABLL_SNAPSHOT_ENABLED)
  def haMasterRatisRpcRequestTimeout: Long = get(HA_MASTER_RATIS_RPC_REQUEST_TIMEOUT)
  def haMasterRatisRetryCacheExpiryTime: Long = get(HA_MASTER_RATIS_SERVER_RETRY_CACHE_EXPIRY_TIME)
  def haMasterRatisRpcTimeoutMin: Long = get(HA_MASTER_RATIS_RPC_TIMEOUT_MIN)
  def haMasterRatisRpcTimeoutMax: Long = get(HA_MASTER_RATIS_RPC_TIMEOUT_MAX)
  def haMasterRatisNotificationNoLeaderTimeout: Long =
    get(HA_MASTER_RATIS_NOTIFICATION_NO_LEADER_TIMEOUT)
  def haMasterRatisRpcSlownessTimeout: Long = get(HA_MASTER_RATIS_RPC_SLOWNESS_TIMEOUT)
  def haMasterRatisRoleCheckInterval: Long = get(HA_MASTER_RATIS_ROLE_CHECK_INTERVAL)
  def haMasterRatisSnapshotAutoTriggerEnabled: Boolean =
    get(HA_MASTER_RATIS_SNAPSHOT_AUTO_TRIGGER_ENABLED)
  def haMasterRatisSnapshotAutoTriggerThreshold: Long =
    get(HA_MASTER_RATIS_SNAPSHOT_AUTO_TRIGGER_THRESHOLD)
  def haMasterRatisSnapshotRetentionFileNum: Int = get(HA_MASTER_RATIS_SNAPSHOT_RETENTION_FILE_NUM)

  // //////////////////////////////////////////////////////
  //                 Metrics System                      //
  // //////////////////////////////////////////////////////
  def metricsSystemEnable: Boolean = get(METRICS_ENABLED)
  def metricsSampleRate: Double = get(METRICS_SAMPLE_RATE)
  def metricsSlidingWindowSize: Int = get(METRICS_SLIDING_WINDOW_SIZE)
  def metricsCollectCriticalEnabled: Boolean = get(METRICS_COLLECT_CRITICAL_ENABLED)
  def metricsCapacity: Int = get(METRICS_CAPACITY)
  def masterPrometheusMetricHost: String = get(MASTER_PROMETHEUS_HOST)
  def masterPrometheusMetricPort: Int = get(MASTER_PROMETHEUS_PORT)
  def workerPrometheusMetricHost: String = get(WORKER_PROMETHEUS_HOST)
  def workerPrometheusMetricPort: Int = get(WORKER_PROMETHEUS_PORT)

  // //////////////////////////////////////////////////////
  //                      Quota                         //
  // //////////////////////////////////////////////////////
  def quotaEnabled: Boolean = get(QUOTA_ENABLED)
  def quotaIdentityProviderClass: String = get(QUOTA_IDENTITY_PROVIDER)
  def quotaManagerClass: String = get(QUOTA_MANAGER)
  def quotaConfigurationPath: Option[String] = get(QUOTA_CONFIGURATION_PATH)

  // //////////////////////////////////////////////////////
  //               Shuffle Client Fetch                  //
  // //////////////////////////////////////////////////////
  def fetchTimeoutMs: Long = get(FETCH_TIMEOUT)
  def fetchMaxReqsInFlight: Int = get(FETCH_MAX_REQS_IN_FLIGHT)
  def fetchMaxRetries: Int = get(FETCH_MAX_RETRIES)

  // //////////////////////////////////////////////////////
  //               Shuffle Client Push                   //
  // //////////////////////////////////////////////////////
  def pushReplicateEnabled: Boolean = get(PUSH_REPLICATE_ENABLED)
  def pushBufferInitialSize: Int = get(PUSH_BUFFER_INITIAL_SIZE).toInt
  def pushBufferMaxSize: Int = get(PUSH_BUFFER_MAX_SIZE).toInt
  def pushQueueCapacity: Int = get(PUSH_QUEUE_CAPACITY)
  def pushMaxReqsInFlight: Int = get(PUSH_MAX_REQS_IN_FLIGHT)
  def pushMaxReviveTimes: Int = get(PUSH_MAX_REVIVE_TIMES)
  def pushSortMemoryThreshold: Long = get(PUSH_SORT_MEMORY_THRESHOLD)
  def pushSortPipelineEnabled: Boolean = get(PUSH_SORT_PIPELINE_ENABLED)
  def pushSortRandomizePartitionIdEnabled: Boolean = get(PUSH_SORT_RANDOMIZE_PARITION_ENABLED)
  def pushRetryThreads: Int = get(PUSH_RETRY_THREADS)
  def pushStageEndTimeout: Long =
    get(PUSH_STAGE_END_TIMEOUT).getOrElse(get(RPC_ASK_TIMEOUT) * (requestCommitFilesMaxRetries + 1))
  def pushLimitStrategy: String = get(PUSH_LIMIT_STRATEGY)
  def pushSlowStartInitialSleepTime: Long = get(PUSH_SLOW_START_INITIAL_SLEEP_TIME)
  def pushSlowStartMaxSleepMills: Long = get(PUSH_SLOW_START_MAX_SLEEP_TIME)
  def pushLimitInFlightTimeoutMs: Long =
    if (pushReplicateEnabled) {
      get(PUSH_LIMIT_IN_FLIGHT_TIMEOUT).getOrElse(pushDataTimeoutMs * 4)
    } else {
      get(PUSH_LIMIT_IN_FLIGHT_TIMEOUT).getOrElse(pushDataTimeoutMs * 2)
    }
  def pushLimitInFlightSleepDeltaMs: Long = get(PUSH_LIMIT_IN_FLIGHT_SLEEP_INTERVAL)
  def pushSplitPartitionThreads: Int = get(PUSH_SPLIT_PARTITION_THREADS)
  def partitionSplitMode: PartitionSplitMode = PartitionSplitMode.valueOf(get(PARTITION_SPLIT_MODE))
  def partitionSplitThreshold: Long = get(PARTITION_SPLIT_THRESHOLD)
  def batchHandleChangePartitionEnabled: Boolean = get(BATCH_HANDLE_CHANGE_PARTITION_ENABLED)
  def batchHandleChangePartitionNumThreads: Int = get(BATCH_HANDLE_CHANGE_PARTITION_THREADS)
  def batchHandleChangePartitionRequestInterval: Long = get(BATCH_HANDLE_CHANGE_PARTITION_INTERVAL)
  def batchHandleCommitPartitionEnabled: Boolean = get(BATCH_HANDLE_COMMIT_PARTITION_ENABLED)
  def batchHandleCommitPartitionNumThreads: Int = get(BATCH_HANDLE_COMMIT_PARTITION_THREADS)
  def batchHandleCommitPartitionRequestInterval: Long = get(BATCH_HANDLED_COMMIT_PARTITION_INTERVAL)
  def rpcCacheSize: Int = get(RPC_CACHE_SIZE)
  def rpcCacheConcurrencyLevel: Int = get(RPC_CACHE_CONCURRENCY_LEVEL)
  def rpcCacheExpireTime: Long = get(RPC_CACHE_EXPIRE_TIME)
  def pushDataTimeoutMs: Long = get(PUSH_DATA_TIMEOUT)
  def pushTimeoutCheckInterval: Long = get(PUSH_TIMEOUT_CHECK_INTERVAL)
  def registerShuffleRpcAskTimeout: RpcTimeout =
    new RpcTimeout(
      get(REGISTER_SHUFFLE_RPC_ASK_TIMEOUT).map(_.milli)
        .getOrElse(rpcAskTimeout.duration * (reserveSlotsMaxRetries + 2)),
      REGISTER_SHUFFLE_RPC_ASK_TIMEOUT.key)
  def requestPartitionLocationRpcAskTimeout: RpcTimeout =
    new RpcTimeout(
      get(REQUEST_PARTITION_LOCATION_RPC_ASK_TIMEOUT).map(_.milli)
        .getOrElse(rpcAskTimeout.duration * (reserveSlotsMaxRetries + 1)),
      REQUEST_PARTITION_LOCATION_RPC_ASK_TIMEOUT.key)
  def getReducerFileGroupRpcAskTimeout: RpcTimeout =
    new RpcTimeout(
      get(GET_REDUCER_FILE_GROUP_RPC_ASK_TIMEOUT).map(_.milli)
        .getOrElse(rpcAskTimeout.duration * (requestCommitFilesMaxRetries + 2)),
      GET_REDUCER_FILE_GROUP_RPC_ASK_TIMEOUT.key)

  // //////////////////////////////////////////////////////
  //            Graceful Shutdown & Recover              //
  // //////////////////////////////////////////////////////
  def workerGracefulShutdown: Boolean = get(WORKER_GRACEFUL_SHUTDOWN_ENABLED)
  def shutdownTimeoutMs: Long = get(WORKER_GRACEFUL_SHUTDOWN_TIMEOUT)
  def checkSlotsFinishedInterval: Long = get(WORKER_CHECK_SLOTS_FINISHED_INTERVAL)
  def checkSlotsFinishedTimeoutMs: Long = get(WORKER_CHECK_SLOTS_FINISHED_TIMEOUT)
  def workerRecoverPath: String = get(WORKER_RECOVER_PATH)
  def partitionSorterCloseAwaitTimeMs: Long = get(PARTITION_SORTER_SHUTDOWN_TIMEOUT)
  def workerFlusherShutdownTimeoutMs: Long = get(WORKER_FLUSHER_SHUTDOWN_TIMEOUT)

  // //////////////////////////////////////////////////////
  //                      Flusher                        //
  // //////////////////////////////////////////////////////
  def workerFlusherBufferSize: Long = get(WORKER_FLUSHER_BUFFER_SIZE)
  def writerCloseTimeoutMs: Long = get(WORKER_WRITER_CLOSE_TIMEOUT)
  def hddFlusherThreads: Int = get(WORKER_FLUSHER_HDD_THREADS)
  def ssdFlusherThreads: Int = get(WORKER_FLUSHER_SSD_THREADS)
  def hdfsFlusherThreads: Int = get(WORKER_FLUSHER_HDFS_THREADS)
  def avgFlushTimeSlidingWindowSize: Int = get(WORKER_FLUSHER_AVGFLUSHTIME_SLIDINGWINDOW_SIZE)
  def avgFlushTimeSlidingWindowMinCount: Int =
    get(WORKER_FLUSHER_AVGFLUSHTIME_SLIDINGWINDOW_MINCOUNT)
  def diskReserveSize: Long = get(WORKER_DISK_RESERVE_SIZE)
  def diskMonitorEnabled: Boolean = get(WORKER_DISK_MONITOR_ENABLED)
  def diskMonitorCheckList: Seq[String] = get(WORKER_DISK_MONITOR_CHECKLIST)
  def diskMonitorCheckInterval: Long = get(WORKER_DISK_MONITOR_CHECK_INTERVAL)
  def diskMonitorSysBlockDir: String = get(WORKER_DISK_MONITOR_SYS_BLOCK_DIR)
  def diskMonitorNotifyErrorThreshold: Int = get(WORKER_DISK_MONITOR_NOTIFY_ERROR_THRESHOLD)
  def diskMonitorNotifyErrorExpireTimeout: Long =
    get(WORKER_DISK_MONITOR_NOTIFY_ERROR_EXPIRE_TIMEOUT)
  def createWriterMaxAttempts: Int = get(WORKER_WRITER_CREATE_MAX_ATTEMPTS)
  def workerStorageBaseDirPrefix: String = get(WORKER_STORAGE_BASE_DIR_PREFIX)
  def workerStorageBaseDirNumber: Int = get(WORKER_STORAGE_BASE_DIR_COUNT)

  // //////////////////////////////////////////////////////
  //                  Memory Manager                    //
  // //////////////////////////////////////////////////////
  def workerDirectMemoryRatioToPauseReceive: Double = get(WORKER_DIRECT_MEMORY_RATIO_PAUSE_RECEIVE)
  def workerDirectMemoryRatioToPauseReplicate: Double =
    get(WORKER_DIRECT_MEMORY_RATIO_PAUSE_REPLICATE)
  def workerDirectMemoryRatioToResume: Double = get(WORKER_DIRECT_MEMORY_RATIO_RESUME)
  def partitionSorterDirectMemoryRatioThreshold: Double =
    get(PARTITION_SORTER_DIRECT_MEMORY_RATIO_THRESHOLD)
  def workerDirectMemoryPressureCheckIntervalMs: Long = get(WORKER_DIRECT_MEMORY_CHECK_INTERVAL)
  def workerDirectMemoryReportIntervalSecond: Long = get(WORKER_DIRECT_MEMORY_REPORT_INTERVAL)
  def workerDirectMemoryRatioForReadBuffer: Double = get(WORKER_DIRECT_MEMORY_RATIO_FOR_READ_BUFFER)
  def workerDirectMemoryRatioForShuffleStorage: Double =
    get(WORKER_DIRECT_MEMORY_RATIO_FOR_SHUFFLE_STORAGE)

  def partitionReadBuffersMin: Int = get(WORKER_PARTITION_READ_BUFFERS_MIN)

  def partitionReadBuffersMax: Int = get(WORKER_PARTITION_READ_BUFFERS_MAX)
  def bufferStreamThreadsPerMountpoint: Int = get(WORKER_BUFFERSTREAM_THREADS_PER_MOUNTPOINT)

  // //////////////////////////////////////////////////////
  //                  Rate Limit controller              //
  // //////////////////////////////////////////////////////
  def workerCongestionControlEnabled: Boolean = get(WORKER_CONGESTION_CONTROL_ENABLED)
  def workerCongestionControlSampleTimeWindowSeconds: Long =
    get(WORKER_CONGESTION_CONTROL_SAMPLE_TIME_WINDOW)
  // TODO related to `WORKER_DIRECT_MEMORY_RATIO_PAUSE_RECEIVE`,
  // `WORKER_DIRECT_MEMORY_RATIO_PAUSE_REPLICATE`and `WORKER_DIRECT_MEMORY_RATIO_RESUME`,
  // we'd better refine the logic among them
  def workerCongestionControlLowWatermark: Option[Long] =
    get(WORKER_CONGESTION_CONTROL_LOW_WATERMARK)
  def workerCongestionControlHighWatermark: Option[Long] =
    get(WORKER_CONGESTION_CONTROL_HIGH_WATERMARK)
  def workerCongestionControlUserInactiveIntervalMs: Long =
    get(WORKER_CONGESTION_CONTROL_USER_INACTIVE_INTERVAL)

  /**
   * @return workingDir, usable space, flusher thread count, disk type
   *         check more details at CONFIGURATION_GUIDE.md
   */
  def workerBaseDirs: Seq[(String, Long, Int, Type)] = {
    // I assume there is no disk is bigger than 1 PB in recent days.
    val defaultMaxCapacity = Utils.byteStringAsBytes("1PB")
    get(WORKER_STORAGE_DIRS).map { storageDirs: Seq[String] =>
      storageDirs.map { str =>
        var maxCapacity = defaultMaxCapacity
        var diskType = HDD
        var flushThread = get(WORKER_FLUSHER_THREADS)
        val (dir, attributes) = str.split(":").toList match {
          case _dir :: tail => (_dir, tail)
          case nil => throw new IllegalArgumentException(s"Illegal storage dir: $nil")
        }
        var flushThreadsDefined = false
        attributes.foreach {
          case capacityStr if capacityStr.toLowerCase.startsWith("capacity=") =>
            maxCapacity = Utils.byteStringAsBytes(capacityStr.split("=")(1))
          case diskTypeStr if diskTypeStr.toLowerCase.startsWith("disktype=") =>
            diskType = Type.valueOf(diskTypeStr.split("=")(1))
            if (diskType == Type.MEMORY) {
              throw new IOException(s"Invalid diskType: $diskType")
            }
            if (!flushThreadsDefined) {
              flushThread = diskType match {
                case HDD => hddFlusherThreads
                case SSD => ssdFlusherThreads
                case _ => flushThread
              }
            }
          case threadCountStr if threadCountStr.toLowerCase.startsWith("flushthread=") =>
            flushThread = threadCountStr.split("=")(1).toInt
            flushThreadsDefined = true
          case illegal =>
            throw new IllegalArgumentException(s"Illegal attribute: $illegal")
        }
        (dir, maxCapacity, flushThread, diskType)
      }
    }.getOrElse {
      val prefix = workerStorageBaseDirPrefix
      val number = workerStorageBaseDirNumber
      (1 to number).map { i =>
        (s"$prefix$i", defaultMaxCapacity, hddFlusherThreads, HDD)
      }
    }
  }

  def partitionSplitMinimumSize: Long = get(PARTITION_SPLIT_MIN)

  def hdfsDir: String = {
    get(HDFS_DIR).map {
      hdfsDir =>
        if (!Utils.isHdfsPath(hdfsDir)) {
          log.error(s"${HDFS_DIR.key} configuration is wrong $hdfsDir. Disable HDFS support.")
          ""
        } else {
          hdfsDir
        }
    }.getOrElse("")
  }

  // //////////////////////////////////////////////////////
  //                 Columnar Shuffle                    //
  // //////////////////////////////////////////////////////
  def columnarShuffleEnabled: Boolean = get(COLUMNAR_SHUFFLE_ENABLED)
  def columnarShuffleBatchSize: Int = get(COLUMNAR_SHUFFLE_BATCH_SIZE)
  def columnarShuffleOffHeapEnabled: Boolean = get(COLUMNAR_SHUFFLE_OFF_HEAP_ENABLED)
  def columnarShuffleDictionaryEnabled: Boolean = get(COLUMNAR_SHUFFLE_DICTIONARY_ENCODING_ENABLED)
  def columnarShuffleDictionaryMaxFactor: Double =
    get(COLUMNAR_SHUFFLE_DICTIONARY_ENCODING_MAX_FACTOR)

  def columnarShuffleCodeGenEnabled: Boolean = get(COLUMNAR_SHUFFLE_CODEGEN_ENABLED)

  // //////////////////////////////////////////////////////
  //                      test                           //
  // //////////////////////////////////////////////////////
  def testFetchFailure: Boolean = get(TEST_FETCH_FAILURE)
  def testRetryCommitFiles: Boolean = get(TEST_RETRY_COMMIT_FILE)
  def testPushMasterDataTimeout: Boolean = get(TEST_PUSH_MASTER_DATA_TIMEOUT)
  def testPushSlaveDataTimeout: Boolean = get(TEST_PUSH_SLAVE_DATA_TIMEOUT)
  def testRetryRevive: Boolean = get(TEST_RETRY_REVIVE)
}

object CelebornConf extends Logging {

  /**
   * Holds information about keys that have been deprecated and do not have a replacement.
   *
   * @param key                The deprecated key.
   * @param version            The version in which the key was deprecated.
   * @param deprecationMessage Message to include in the deprecation warning.
   */
  private case class DeprecatedConfig(
      key: String,
      version: String,
      deprecationMessage: String)

  /**
   * Information about an alternate configuration key that has been deprecated.
   *
   * @param key         The deprecated config key.
   * @param version     The version in which the key was deprecated.
   * @param translation A translation function for converting old config values into new ones.
   */
  private case class AlternateConfig(
      key: String,
      version: String,
      translation: String => String = null)

  /**
   * Holds information about keys that have been removed.
   *
   * @param key          The removed config key.
   * @param version      The version in which key was removed.
   * @param defaultValue The default config value. It can be used to notice
   *                     users that they set non-default value to an already removed config.
   * @param comment      Additional info regarding to the removed config.
   */
  case class RemovedConfig(key: String, version: String, defaultValue: String, comment: String)

  /**
   * Maps deprecated config keys to information about the deprecation.
   *
   * The extra information is logged as a warning when the config is present in the user's
   * configuration.
   */
  private val deprecatedConfigs: Map[String, DeprecatedConfig] = {
    val configs = Seq(
      DeprecatedConfig("none", "1.0", "None"))

    Map(configs.map { cfg => (cfg.key -> cfg) }: _*)
  }

  /**
   * The map contains info about removed SQL configs. Keys are SQL config names,
   * map values contain extra information like the version in which the config was removed,
   * config's default value and a comment.
   *
   * Please, add a removed configuration property here only when it affects behaviours.
   * By this, it makes migrations to new versions painless.
   */
  val removedConfigs: Map[String, RemovedConfig] = {
    val masterEndpointsTips = "The behavior is controlled by `celeborn.master.endpoints` now, " +
      "please check the documentation for details."
    val configs = Seq(
      RemovedConfig("rss.ha.master.hosts", "0.2.0", null, masterEndpointsTips),
      RemovedConfig("rss.ha.service.id", "0.2.0", "rss", "configuration key removed."),
      RemovedConfig("rss.ha.nodes.rss", "0.2.0", "1,2,3,", "configuration key removed."))
    Map(configs.map { cfg => cfg.key -> cfg }: _*)
  }

  /**
   * Maps a current config key to alternate keys that were used in previous version.
   *
   * The alternates are used in the order defined in this map. If deprecated configs are
   * present in the user's configuration, a warning is logged.
   */
  private val configsWithAlternatives = Map[String, Seq[AlternateConfig]](
    "none" -> Seq(
      AlternateConfig("none", "1.0")))

  /**
   * A view of `configsWithAlternatives` that makes it more efficient to look up deprecated
   * config keys.
   *
   * Maps the deprecated config name to a 2-tuple (new config name, alternate config info).
   */
  private val allAlternatives: Map[String, (String, AlternateConfig)] = {
    configsWithAlternatives.keys.flatMap { key =>
      configsWithAlternatives(key).map { cfg => (cfg.key -> (key -> cfg)) }
    }.toMap
  }

  /**
   * Looks for available deprecated keys for the given config option, and return the first
   * value available.
   */
  def getDeprecatedConfig(key: String, conf: JMap[String, String]): Option[String] = {
    configsWithAlternatives.get(key).flatMap { alts =>
      alts.collectFirst {
        case alt if conf.containsKey(alt.key) =>
          val value = conf.get(alt.key)
          if (alt.translation != null) alt.translation(value) else value
      }
    }
  }

  private def requireDefaultValueOfRemovedConf(key: String, value: String): Unit = {
    removedConfigs.get(key).foreach {
      case RemovedConfig(configName, version, defaultValue, comment) =>
        if (value != defaultValue) {
          throw new IllegalArgumentException(
            s"The config '$configName' was removed in v$version. $comment")
        }
    }
  }

  /**
   * Logs a warning message if the given config key is deprecated.
   */
  private def logDeprecationWarning(key: String): Unit = {
    deprecatedConfigs.get(key).foreach { cfg =>
      logWarning(
        s"The configuration key '$key' has been deprecated in v${cfg.version} and " +
          s"may be removed in the future. ${cfg.deprecationMessage}")
      return
    }

    allAlternatives.get(key).foreach { case (newKey, cfg) =>
      logWarning(
        s"The configuration key '$key' has been deprecated in v${cfg.version} and " +
          s"may be removed in the future. Please use the new key '$newKey' instead.")
      return
    }
  }

  private[this] val confEntriesUpdateLock = new Object

  @volatile
  private[celeborn] var confEntries: JMap[String, ConfigEntry[_]] = Collections.emptyMap()

  private def register(entry: ConfigEntry[_]): Unit = confEntriesUpdateLock.synchronized {
    require(
      !confEntries.containsKey(entry.key),
      s"Duplicate CelebornConfigEntry. ${entry.key} has been registered")
    val updatedMap = new JHashMap[String, ConfigEntry[_]](confEntries)
    updatedMap.put(entry.key, entry)
    confEntries = updatedMap
  }

  private[celeborn] def unregister(entry: ConfigEntry[_]): Unit =
    confEntriesUpdateLock.synchronized {
      val updatedMap = new JHashMap[String, ConfigEntry[_]](confEntries)
      updatedMap.remove(entry.key)
      confEntries = updatedMap
    }

  private[celeborn] def getConfigEntry(key: String): ConfigEntry[_] = {
    confEntries.get(key)
  }

  private[celeborn] def getConfigEntries: JCollection[ConfigEntry[_]] = {
    confEntries.values()
  }

  private[celeborn] def containsConfigEntry(entry: ConfigEntry[_]): Boolean = {
    getConfigEntry(entry.key) == entry
  }

  private[celeborn] def containsConfigKey(key: String): Boolean = {
    confEntries.containsKey(key)
  }

  def buildConf(key: String): ConfigBuilder = ConfigBuilder(key).onCreate(register)

  val NETWORK_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.network.timeout")
      .withAlternative("rss.network.timeout")
      .categories("network")
      .version("0.2.0")
      .doc("Default timeout for network operations.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("240s")

  val NETWORK_CONNECT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.network.connect.timeout")
      .withAlternative("rss.network.connect.timeout")
      .categories("network")
      .doc("Default socket connect timeout.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10s")

  val RPC_CONNECT_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.rpc.connect.threads")
      .categories("network")
      .version("0.2.0")
      .intConf
      .createWithDefault(64)

  val RPC_LOOKUP_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.rpc.lookupTimeout")
      .withAlternative("rss.rpc.lookupTimeout")
      .categories("network")
      .version("0.2.0")
      .doc("Timeout for RPC lookup operations.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("30s")

  val RPC_ASK_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.rpc.askTimeout")
      .withAlternative("rss.rpc.askTimeout")
      .categories("network")
      .version("0.2.0")
      .doc("Timeout for RPC ask operations.")
      .fallbackConf(NETWORK_TIMEOUT)

  val HA_CLIENT_RPC_ASK_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.rpc.haClient.askTimeout")
      .withAlternative("rss.haclient.rpc.askTimeout")
      .categories("network")
      .version("0.2.0")
      .doc("Timeout for HA client RPC ask operations.")
      .fallbackConf(NETWORK_TIMEOUT)

  val NETWORK_IO_MODE: ConfigEntry[String] =
    buildConf("celeborn.<module>.io.mode")
      .categories("network")
      .doc("Netty EventLoopGroup backend, available options: NIO, EPOLL.")
      .stringConf
      .transform(_.toUpperCase)
      .checkValues(Set("NIO", "EPOLL"))
      .createWithDefault("NIO")

  val NETWORK_IO_PREFER_DIRECT_BUFS: ConfigEntry[Boolean] =
    buildConf("celeborn.<module>.io.preferDirectBufs")
      .categories("network")
      .doc("If true, we will prefer allocating off-heap byte buffers within Netty.")
      .booleanConf
      .createWithDefault(true)

  val NETWORK_IO_CONNECT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.<module>.io.connectTimeout")
      .categories("network")
      .doc("Socket connect timeout.")
      .fallbackConf(NETWORK_CONNECT_TIMEOUT)

  val NETWORK_IO_CONNECTION_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.<module>.io.connectionTimeout")
      .categories("network")
      .doc("Connection active timeout.")
      .fallbackConf(NETWORK_TIMEOUT)

  val NETWORK_IO_NUM_CONNECTIONS_PER_PEER: ConfigEntry[Int] =
    buildConf("celeborn.<module>.io.numConnectionsPerPeer")
      .categories("network")
      .doc("Number of concurrent connections between two nodes.")
      .intConf
      .createWithDefault(2)

  val NETWORK_IO_BACKLOG: ConfigEntry[Int] =
    buildConf("celeborn.<module>.io.backLog")
      .categories("network")
      .doc(
        "Requested maximum length of the queue of incoming connections. Default 0 for no backlog.")
      .intConf
      .createWithDefault(0)

  val NETWORK_IO_SERVER_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.<module>.io.serverThreads")
      .categories("network")
      .doc("Number of threads used in the server thread pool. Default to 0, which is 2x#cores.")
      .intConf
      .createWithDefault(0)

  val NETWORK_IO_CLIENT_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.<module>.io.clientThreads")
      .categories("network")
      .doc("Number of threads used in the client thread pool. Default to 0, which is 2x#cores.")
      .intConf
      .createWithDefault(0)

  val NETWORK_IO_RECEIVE_BUFFER: ConfigEntry[Long] =
    buildConf("celeborn.<module>.io.receiveBuffer")
      .categories("network")
      .doc("Receive buffer size (SO_RCVBUF). Note: the optimal size for receive buffer and send buffer " +
        "should be latency * network_bandwidth. Assuming latency = 1ms, network_bandwidth = 10Gbps " +
        "buffer size should be ~ 1.25MB.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefault(0)

  val NETWORK_IO_SEND_BUFFER: ConfigEntry[Long] =
    buildConf("celeborn.<module>.io.sendBuffer")
      .categories("network")
      .doc("Send buffer size (SO_SNDBUF).")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefault(0)

  val NETWORK_IO_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.<module>.io.maxRetries")
      .categories("network")
      .doc(
        "Max number of times we will try IO exceptions (such as connection timeouts) per request. " +
          "If set to 0, we will not do any retries.")
      .intConf
      .createWithDefault(3)

  val NETWORK_IO_RETRY_WAIT: ConfigEntry[Long] =
    buildConf("celeborn.<module>.io.retryWait")
      .categories("network")
      .doc("Time that we will wait in order to perform a retry after an IOException. " +
        "Only relevant if maxIORetries > 0.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("5s")

  val NETWORK_IO_LAZY_FD: ConfigEntry[Boolean] =
    buildConf("celeborn.<module>.io.lazyFD")
      .categories("network")
      .doc("Whether to initialize FileDescriptor lazily or not. If true, file descriptors are created only " +
        "when data is going to be transferred. This can reduce the number of open files.")
      .booleanConf
      .createWithDefault(true)

  val NETWORK_VERBOSE_METRICS: ConfigEntry[Boolean] =
    buildConf("celeborn.<module>.io.enableVerboseMetrics")
      .categories("network")
      .doc("Whether to track Netty memory detailed metrics. If true, the detailed metrics of Netty " +
        "PoolByteBufAllocator will be gotten, otherwise only general memory usage will be tracked.")
      .booleanConf
      .createWithDefault(false)

  val STORAGE_MEMORY_MAP_THRESHOLD: ConfigEntry[Long] =
    buildConf("celeborn.storage.memoryMapThreshold")
      .withAlternative("rss.storage.memoryMapThreshold")
      .categories("network")
      .internal
      .doc("Minimum size of a block that we should start using memory map rather than reading in through " +
        "normal IO operations. This prevents Celeborn from memory mapping very small blocks. In general, " +
        "memory mapping has high overhead for blocks close to or below the page size of the OS.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("2m")

  val MAX_CHUNKS_BEING_TRANSFERRED: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.maxChunksBeingTransferred")
      .withAlternative("rss.shuffle.maxChunksBeingTransferred")
      .categories("network")
      .doc("The max number of chunks allowed to be transferred at the same time on shuffle service. Note " +
        "that new incoming connections will be closed when the max number is hit. The client will retry " +
        "according to the shuffle retry configs (see `celeborn.shuffle.io.maxRetries` and " +
        "`celeborn.shuffle.io.retryWait`), if those limits are reached the task will fail with fetch failure.")
      .version("0.2.0")
      .longConf
      .createWithDefault(Long.MaxValue)

  val MASTER_ENDPOINTS: ConfigEntry[Seq[String]] =
    buildConf("celeborn.master.endpoints")
      .categories("client", "worker")
      .doc("Endpoints of master nodes for celeborn client to connect, allowed pattern " +
        "is: `<host1>:<port1>[,<host2>:<port2>]*`, e.g. `clb1:9097,clb2:9098,clb3:9099`. " +
        "If the port is omitted, 9097 will be used.")
      .version("0.2.0")
      .stringConf
      .transform(_.replace("<localhost>", Utils.localHostName))
      .toSequence
      .checkValue(
        endpoints => endpoints.map(_ => Try(Utils.parseHostPort(_))).forall(_.isSuccess),
        "Allowed pattern is: `<host1>:<port1>[,<host2>:<port2>]*`")
      .createWithDefaultString(s"<localhost>:9097")

  val SHUFFLE_WRITER_MODE: ConfigEntry[String] =
    buildConf("celeborn.shuffle.writer")
      .withAlternative("rss.shuffle.writer.mode")
      .categories("client")
      .doc("Celeborn supports the following kind of shuffle writers. 1. hash: hash-based shuffle writer " +
        "works fine when shuffle partition count is normal; 2. sort: sort-based shuffle writer works fine " +
        "when memory pressure is high or shuffle partition count is huge.")
      .version("0.2.0")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set(ShuffleMode.HASH.name, ShuffleMode.SORT.name))
      .createWithDefault(ShuffleMode.HASH.name)

  val PUSH_REPLICATE_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.push.replicate.enabled")
      .withAlternative("rss.push.data.replicate")
      .categories("client")
      .doc("When true, Celeborn worker will replicate shuffle data to another Celeborn worker " +
        "asynchronously to ensure the pushed shuffle data won't be lost after the node failure.")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(true)

  val PUSH_BUFFER_INITIAL_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.push.buffer.initial.size")
      .withAlternative("rss.push.data.buffer.initial.size")
      .categories("client")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("8k")

  val PUSH_BUFFER_MAX_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.push.buffer.max.size")
      .withAlternative("rss.push.data.buffer.size")
      .categories("client")
      .version("0.2.0")
      .doc("Max size of reducer partition buffer memory for shuffle hash writer. The pushed " +
        "data will be buffered in memory before sending to Celeborn worker. For performance " +
        "consideration keep this buffer size higher than 32K. Example: If reducer amount is " +
        "2000, buffer size is 64K, then each task will consume up to `64KiB * 2000 = 125MiB` " +
        "heap memory.")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("64k")

  val PUSH_QUEUE_CAPACITY: ConfigEntry[Int] =
    buildConf("celeborn.push.queue.capacity")
      .withAlternative("rss.push.data.queue.capacity")
      .categories("client")
      .version("0.2.0")
      .doc("Push buffer queue size for a task. The maximum memory is " +
        "`celeborn.push.buffer.max.size` * `celeborn.push.queue.capacity`, " +
        "default: 64KiB * 512 = 32MiB")
      .intConf
      .createWithDefault(512)

  val PUSH_MAX_REQS_IN_FLIGHT: ConfigEntry[Int] =
    buildConf("celeborn.push.maxReqsInFlight")
      .withAlternative("rss.push.data.maxReqsInFlight")
      .categories("client")
      .version("0.2.0")
      .doc("Amount of Netty in-flight requests per worker. The maximum memory is " +
        "`celeborn.push.maxReqsInFlight` * `celeborn.push.buffer.max.size` * " +
        "compression ratio(1 in worst case), default: 64Kib * 32 = 2Mib")
      .intConf
      .createWithDefault(4)

  val PUSH_MAX_REVIVE_TIMES: ConfigEntry[Int] =
    buildConf("celeborn.push.revive.maxRetries")
      .categories("client")
      .version("0.3.0")
      .doc("Max retry times for reviving when celeborn push data failed.")
      .intConf
      .createWithDefault(5)

  val TEST_RETRY_REVIVE: ConfigEntry[Boolean] =
    buildConf("celeborn.test.retryRevive")
      .categories("client")
      .doc("Fail push data and request for test")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val PUSH_TIMEOUT_CHECK_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.push.timeoutCheck.interval")
      .categories("common")
      .doc("Interval for checking push data timeout.")
      .version("0.3.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("30s")

  val FETCH_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.fetch.timeout")
      .withAlternative("rss.fetch.chunk.timeout")
      .categories("client")
      .version("0.2.0")
      .doc("Timeout for a task to fetch chunk.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("120s")

  val FETCH_MAX_REQS_IN_FLIGHT: ConfigEntry[Int] =
    buildConf("celeborn.fetch.maxReqsInFlight")
      .withAlternative("rss.fetch.chunk.maxReqsInFlight")
      .categories("client")
      .version("0.2.0")
      .doc("Amount of in-flight chunk fetch request.")
      .intConf
      .createWithDefault(3)

  val CLIENT_RPC_MAX_PARALLELISM: ConfigEntry[Int] =
    buildConf("celeborn.rpc.maxParallelism")
      .withAlternative("rss.rpc.max.parallelism")
      .categories("client")
      .version("0.2.0")
      .doc("Max parallelism of client on sending RPC requests.")
      .intConf
      .createWithDefault(1024)

  val FETCH_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.fetch.maxRetries")
      .categories("client")
      .version("0.2.0")
      .doc("Max retries of fetch chunk")
      .intConf
      .createWithDefault(3)

  val TEST_FETCH_FAILURE: ConfigEntry[Boolean] =
    buildConf("celeborn.test.fetchFailure")
      .categories("client")
      .version("0.2.0")
      .doc("Whether to test fetch chunk failure")
      .booleanConf
      .createWithDefault(false)

  val APPLICATION_HEARTBEAT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.application.heartbeat.timeout")
      .withAlternative("rss.application.timeout")
      .categories("master")
      .version("0.2.0")
      .doc("Application heartbeat timeout.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("120s")

  val APPLICATION_HEARTBEAT_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.application.heartbeatInterval")
      .withAlternative("rss.application.heartbeatInterval")
      .categories("client")
      .version("0.2.0")
      .doc("Interval for client to send heartbeat message to master.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10s")

  val SHUFFLE_EXPIRED_CHECK_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.expired.checkInterval")
      .withAlternative("rss.remove.shuffle.delay")
      .categories("client")
      .version("0.2.0")
      .doc("Interval for client to check expired shuffles.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("60s")

  val WORKER_EXCLUDED_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.excluded.checkInterval")
      .withAlternative("rss.get.blacklist.delay")
      .categories("client")
      .version("0.2.0")
      .doc("Interval for client to refresh excluded worker list.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("30s")

  val WORKER_EXCLUDED_EXPIRE_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.excluded.expireTimeout")
      .categories("client")
      .version("0.2.0")
      .doc("Timeout time for LifecycleManager to clear reserved excluded worker.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("600s")

  val BLACKLIST_SLAVE_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.client.blacklistSlave.enabled")
      .categories("client")
      .version("0.3.0")
      .doc("When true, Celeborn will add partition's peer worker into blacklist " +
        "when push data to slave failed.")
      .booleanConf
      .createWithDefault(true)

  val SHUFFLE_CHUCK_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.chuck.size")
      .withAlternative("rss.chunk.size")
      .categories("client", "worker")
      .version("0.2.0")
      .doc("Max chunk size of reducer's merged shuffle data. For example, if a reducer's " +
        "shuffle data is 128M and the data will need 16 fetch chunk requests to fetch.")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("8m")

  val SHUFFLE_REGISTER_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.shuffle.register.maxRetries")
      .withAlternative("rss.register.shuffle.max.retry")
      .categories("client")
      .version("0.2.0")
      .doc("Max retry times for client to register shuffle.")
      .intConf
      .createWithDefault(3)

  val SHUFFLE_REGISTER_RETRY_WAIT: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.register.retryWait")
      .withAlternative("rss.register.shuffle.retry.wait")
      .categories("client")
      .version("0.2.0")
      .doc("Wait time before next retry if register shuffle failed.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("3s")

  val RESERVE_SLOTS_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.slots.reserve.maxRetries")
      .withAlternative("rss.reserve.slots.max.retry")
      .categories("client")
      .version("0.2.0")
      .doc("Max retry times for client to reserve slots.")
      .intConf
      .createWithDefault(3)

  val RESERVE_SLOTS_RETRY_WAIT: ConfigEntry[Long] =
    buildConf("celeborn.slots.reserve.retryWait")
      .withAlternative("rss.reserve.slots.retry.wait")
      .categories("client")
      .version("0.2.0")
      .doc("Wait time before next retry if reserve slots failed.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("3s")

  val COMMIT_FILE_REQUEST_MAX_RETRY: ConfigEntry[Int] =
    buildConf("celeborn.rpc.requestCommitFiles.maxRetries")
      .categories("client")
      .doc("Max retry times for requestCommitFiles RPC.")
      .version("1.0.0")
      .intConf
      .checkValue(v => v > 0, "value must be positive")
      .createWithDefault(2)

  val TEST_RETRY_COMMIT_FILE: ConfigEntry[Boolean] =
    buildConf("celeborn.test.retryCommitFiles")
      .categories("client")
      .doc("Fail commitFile request for test")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val MASTER_HOST: ConfigEntry[String] =
    buildConf("celeborn.master.host")
      .categories("master")
      .withAlternative("rss.master.host")
      .version("0.2.0")
      .doc("Hostname for master to bind.")
      .stringConf
      .transform(_.replace("<localhost>", Utils.localHostName))
      .createWithDefaultString("<localhost>")

  val MASTER_PORT: ConfigEntry[Int] =
    buildConf("celeborn.master.port")
      .withAlternative("rss.master.port")
      .categories("master")
      .version("0.2.0")
      .doc("Port for master to bind.")
      .intConf
      .checkValue(p => p >= 1024 && p < 65535, "invalid port")
      .createWithDefault(9097)

  val HA_ENABLED: ConfigEntry[Boolean] = buildConf("celeborn.ha.enabled")
    .withAlternative("rss.ha.enabled")
    .categories("master")
    .version("0.2.0")
    .doc("When true, master nodes run as Raft cluster mode.")
    .booleanConf
    .createWithDefault(false)

  val HA_MASTER_NODE_ID: OptionalConfigEntry[String] =
    buildConf("celeborn.ha.master.node.id")
      .doc("Node id for master raft cluster in HA mode, if not define, " +
        "will be inferred by hostname.")
      .version("0.2.0")
      .stringConf
      .createOptional

  val HA_MASTER_NODE_HOST: ConfigEntry[String] =
    buildConf("celeborn.ha.master.node.<id>.host")
      .categories("master")
      .doc("Host to bind of master node <id> in HA mode.")
      .version("0.2.0")
      .stringConf
      .createWithDefaultString("<required>")

  val HA_MASTER_NODE_PORT: ConfigEntry[Int] =
    buildConf("celeborn.ha.master.node.<id>.port")
      .categories("master")
      .doc("Port to bind of master node <id> in HA mode.")
      .version("0.2.0")
      .intConf
      .checkValue(p => p >= 1024 && p < 65535, "invalid port")
      .createWithDefault(9097)

  val HA_MASTER_NODE_RATIS_HOST: OptionalConfigEntry[String] =
    buildConf("celeborn.ha.master.node.<id>.ratis.host")
      .internal
      .categories("master")
      .doc("Ratis host to bind of master node <id> in HA mode. If not provided, " +
        s"fallback to ${HA_MASTER_NODE_HOST.key}.")
      .version("0.2.0")
      .stringConf
      .createOptional

  val HA_MASTER_NODE_RATIS_PORT: ConfigEntry[Int] =
    buildConf("celeborn.ha.master.node.<id>.ratis.port")
      .categories("master")
      .doc("Ratis port to bind of master node <id> in HA mode.")
      .version("0.2.0")
      .intConf
      .checkValue(p => p >= 1024 && p < 65535, "invalid port")
      .createWithDefault(9872)

  val HA_MASTER_RATIS_RPC_TYPE: ConfigEntry[String] =
    buildConf("celeborn.ha.master.ratis.raft.rpc.type")
      .withAlternative("rss.ha.rpc.type")
      .categories("master")
      .doc("RPC type for Ratis, available options: netty, grpc.")
      .version("0.2.0")
      .stringConf
      .transform(_.toLowerCase)
      .checkValues(Set("netty", "grpc"))
      .createWithDefault("netty")

  val HA_MASTER_RATIS_STORAGE_DIR: ConfigEntry[String] =
    buildConf("celeborn.ha.master.ratis.raft.server.storage.dir")
      .categories("master")
      .withAlternative("rss.ha.storage.dir")
      .version("0.2.0")
      .stringConf
      .createWithDefault("/tmp/ratis")

  val HA_MASTER_RATIS_LOG_SEGMENT_SIZE_MAX: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.segment.size.max")
      .withAlternative("rss.ha.ratis.segment.size")
      .internal
      .categories("master")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("4MB")

  val HA_MASTER_RATIS_LOG_PREALLOCATED_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.preallocated.size")
      .withAlternative("rss.ratis.segment.preallocated.size")
      .internal
      .categories("master")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("4MB")

  val HA_MASTER_RATIS_LOG_APPENDER_QUEUE_NUM_ELEMENTS: ConfigEntry[Int] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.appender.buffer.element-limit")
      .withAlternative("rss.ratis.log.appender.queue.num-elements")
      .internal
      .categories("master")
      .version("0.2.0")
      .intConf
      .createWithDefault(1024)

  val HA_MASTER_RATIS_LOG_APPENDER_QUEUE_BYTE_LIMIT: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.appender.buffer.byte-limit")
      .withAlternative("rss.ratis.log.appender.queue.byte-limit")
      .internal
      .categories("master")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("32MB")

  val HA_MASTER_RATIS_LOG_INSTABLL_SNAPSHOT_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.appender.install.snapshot.enabled")
      .internal
      .categories("master")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(true)

  val HA_MASTER_RATIS_LOG_PURGE_GAP: ConfigEntry[Int] =
    buildConf("celeborn.ha.master.ratis.raft.server.log.purge.gap")
      .withAlternative("rss.ratis.log.purge.gap")
      .internal
      .categories("master")
      .version("0.2.0")
      .intConf
      .createWithDefault(1000000)

  val HA_MASTER_RATIS_RPC_REQUEST_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.rpc.request.timeout")
      .withAlternative("rss.ratis.server.request.timeout")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("3s")

  val HA_MASTER_RATIS_SERVER_RETRY_CACHE_EXPIRY_TIME: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.retrycache.expirytime")
      .withAlternative("rss.ratis.server.retry.cache.timeout")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("600s")

  val HA_MASTER_RATIS_RPC_TIMEOUT_MIN: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.rpc.timeout.min")
      .withAlternative("rss.ratis.minimum.timeout")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("3s")

  val HA_MASTER_RATIS_RPC_TIMEOUT_MAX: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.rpc.timeout.max")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("5s")

  val HA_MASTER_RATIS_NOTIFICATION_NO_LEADER_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.notification.no-leader.timeout")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("120s")

  val HA_MASTER_RATIS_RPC_SLOWNESS_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.rpc.slowness.timeout")
      .withAlternative("rss.ratis.server.failure.timeout.duration")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("120s")

  val HA_MASTER_RATIS_ROLE_CHECK_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.role.check.interval")
      .withAlternative("rss.ratis.server.role.check.interval")
      .internal
      .categories("master")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("1s")

  val HA_MASTER_RATIS_SNAPSHOT_AUTO_TRIGGER_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.ha.master.ratis.raft.server.snapshot.auto.trigger.enabled")
      .withAlternative("rss.ha.ratis.snapshot.auto.trigger.enabled")
      .internal
      .categories("master")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(true)

  val HA_MASTER_RATIS_SNAPSHOT_AUTO_TRIGGER_THRESHOLD: ConfigEntry[Long] =
    buildConf("celeborn.ha.master.ratis.raft.server.snapshot.auto.trigger.threshold")
      .withAlternative("rss.ha.ratis.snapshot.auto.trigger.threshold")
      .internal
      .categories("master")
      .version("0.2.0")
      .longConf
      .createWithDefault(200000L)

  val HA_MASTER_RATIS_SNAPSHOT_RETENTION_FILE_NUM: ConfigEntry[Int] =
    buildConf("celeborn.ha.master.ratis.raft.server.snapshot.retention.file.num")
      .withAlternative("rss.ratis.snapshot.retention.file.num")
      .internal
      .categories("master")
      .version("0.2.0")
      .intConf
      .createWithDefault(3)

  val WORKER_STORAGE_DIRS: OptionalConfigEntry[Seq[String]] =
    buildConf("celeborn.worker.storage.dirs")
      .withAlternative("rss.worker.base.dirs")
      .categories("worker")
      .version("0.2.0")
      .doc("Directory list to store shuffle data. It's recommended to configure one directory " +
        "on each disk. Storage size limit can be set for each directory. For the sake of " +
        "performance, there should be no more than 2 flush threads " +
        "on the same disk partition if you are using HDD, and should be 8 or more flush threads " +
        "on the same disk partition if you are using SSD. For example: " +
        "`dir1[:capacity=][:disktype=][:flushthread=],dir2[:capacity=][:disktype=][:flushthread=]`")
      .stringConf
      .toSequence
      .createOptional

  val WORKER_STORAGE_BASE_DIR_PREFIX: ConfigEntry[String] =
    buildConf("celeborn.worker.storage.baseDir.prefix")
      .withAlternative("rss.worker.base.dir.prefix")
      .categories("worker")
      .version("0.2.0")
      .doc("Base directory for Celeborn worker to write if " +
        s"`${WORKER_STORAGE_DIRS.key}` is not set.")
      .stringConf
      .createWithDefault("/mnt/disk")

  val WORKER_STORAGE_BASE_DIR_COUNT: ConfigEntry[Int] =
    buildConf("celeborn.worker.storage.baseDir.number")
      .withAlternative("rss.worker.base.dir.number")
      .categories("worker")
      .version("0.2.0")
      .doc(s"How many directories will be used if `${WORKER_STORAGE_DIRS.key}` is not set. " +
        s"The directory name is a combination of `${WORKER_STORAGE_BASE_DIR_PREFIX.key}` " +
        "and from one(inclusive) to `celeborn.worker.storage.baseDir.number`(inclusive) " +
        "step by one.")
      .intConf
      .createWithDefault(16)

  val WORKER_RPC_PORT: ConfigEntry[Int] =
    buildConf("celeborn.worker.rpc.port")
      .withAlternative("rss.worker.rpc.port")
      .categories("worker")
      .doc("Server port for Worker to receive RPC request.")
      .version("0.2.0")
      .intConf
      .createWithDefault(0)

  val WORKER_PUSH_PORT: ConfigEntry[Int] =
    buildConf("celeborn.worker.push.port")
      .withAlternative("rss.push.port")
      .categories("worker")
      .doc("Server port for Worker to receive push data request from ShuffleClient.")
      .version("0.2.0")
      .intConf
      .createWithDefault(0)

  val WORKER_FETCH_PORT: ConfigEntry[Int] =
    buildConf("celeborn.worker.fetch.port")
      .withAlternative("rss.fetchserver.port")
      .categories("worker")
      .doc("Server port for Worker to receive fetch data request from ShuffleClient.")
      .version("0.2.0")
      .intConf
      .createWithDefault(0)

  val WORKER_REPLICATE_PORT: ConfigEntry[Int] =
    buildConf("celeborn.worker.replicate.port")
      .withAlternative("rss.replicateserver.port")
      .categories("worker")
      .doc("Server port for Worker to receive replicate data request from other Workers.")
      .version("0.2.0")
      .intConf
      .createWithDefault(0)

  val WORKER_PUSH_IO_THREADS: OptionalConfigEntry[Int] =
    buildConf("celeborn.worker.push.io.threads")
      .withAlternative("rss.push.io.threads")
      .categories("worker")
      .doc("Netty IO thread number of worker to handle client push data. " +
        s"The default threads number is the number of flush thread.")
      .version("0.2.0")
      .intConf
      .createOptional

  val WORKER_FETCH_IO_THREADS: OptionalConfigEntry[Int] =
    buildConf("celeborn.worker.fetch.io.threads")
      .withAlternative("rss.fetch.io.threads")
      .categories("worker")
      .doc("Netty IO thread number of worker to handle client fetch data. " +
        s"The default threads number is the number of flush thread.")
      .version("0.2.0")
      .intConf
      .createOptional

  val WORKER_REPLICATE_IO_THREADS: OptionalConfigEntry[Int] =
    buildConf("celeborn.worker.replicate.io.threads")
      .withAlternative("rss.replicate.io.threads")
      .categories("worker")
      .doc("Netty IO thread number of worker to replicate shuffle data. " +
        s"The default threads number is the number of flush thread.")
      .version("0.2.0")
      .intConf
      .createOptional

  val WORKER_REGISTER_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.register.timeout")
      .withAlternative("rss.register.worker.timeout")
      .categories("worker")
      .doc("Worker register timeout.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("180s")

  val WORKER_NON_EMPTY_DIR_EXPIRE_DURATION: ConfigEntry[Long] =
    buildConf("celeborn.worker.noneEmptyDirExpireDuration")
      .withAlternative("rss.expire.nonEmptyDir.duration")
      .categories("worker")
      .doc("If a non-empty application shuffle data dir have not been operated during le duration time, will mark this application as expired.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("1d")

  val WORKER_WORKING_DIR: ConfigEntry[String] =
    buildConf("celeborn.worker.workingDir")
      .withAlternative("rss.worker.workingDirName")
      .categories("worker")
      .doc("Worker's working dir path name.")
      .version("0.2.0")
      .stringConf
      .createWithDefault("hadoop/rss-worker/shuffle_data")

  val WORKER_CLOSE_IDLE_CONNECTIONS: ConfigEntry[Boolean] =
    buildConf("celeborn.worker.closeIdleConnections")
      .withAlternative("rss.worker.closeIdleConnections")
      .categories("worker")
      .doc("Whether worker will close idle connections.")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val WORKER_REPLICATE_FAST_FAIL_DURATION: ConfigEntry[Long] =
    buildConf("celeborn.worker.replicate.fastFail.duration")
      .withAlternative("rss.replicate.fastfail.duration")
      .categories("worker")
      .doc("If a replicate request not replied during the duration, worker will mark the replicate data request as failed.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("60s")

  val WORKER_REPLICATE_RANDOM_CONNECTION_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.worker.replicate.randomConnection.enabled")
      .categories("worker")
      .doc("Whether worker will create random connection to peer when replicate data. When false, worker tend to " +
        "reuse the same cached TransportClient to a specific replicate worker; when true, worker tend to use " +
        "different cached TransportClient. Netty will use the same thread to serve the same connection, so " +
        "with more connections replicate server can leverage more netty threads")
      .version("0.2.1")
      .booleanConf
      .createWithDefault(true)

  val WORKER_DEVICE_STATUS_CHECK_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.disk.check.timeout")
      .withAlternative("rss.worker.status.check.timeout")
      .categories("worker")
      .doc("Timeout time for worker check device status.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("30s")

  val WORKER_CHECK_FILE_CLEAN_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.worker.disk.checkFileClean.maxRetries")
      .withAlternative("rss.worker.checkFileCleanRetryTimes")
      .categories("worker")
      .doc("The number of retries for a worker to check if the working directory is cleaned up before registering with the master.")
      .version("0.2.0")
      .intConf
      .createWithDefault(3)

  val WORKER_CHECK_FILE_CLEAN_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.disk.checkFileClean.timeout")
      .withAlternative("rss.worker.checkFileCleanTimeoutMs")
      .categories("worker")
      .doc("The wait time per retry for a worker to check if the working directory is cleaned up before registering with the master.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("1000ms")

  val WORKER_HEARTBEAT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.heartbeat.timeout")
      .withAlternative("rss.worker.timeout")
      .categories("master", "worker")
      .version("0.2.0")
      .doc("Worker heartbeat timeout.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("120s")

  val WORKER_REPLICATE_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.replicate.threads")
      .withAlternative("rss.worker.replicate.numThreads")
      .categories("worker")
      .version("0.2.0")
      .doc("Thread number of worker to replicate shuffle data.")
      .intConf
      .createWithDefault(64)

  val WORKER_COMMIT_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.commit.threads")
      .withAlternative("rss.worker.asyncCommitFiles.numThreads")
      .categories("worker")
      .version("0.2.0")
      .doc("Thread number of worker to commit shuffle data files asynchronously.")
      .intConf
      .createWithDefault(32)

  val WORKER_SHUFFLE_COMMIT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.shuffle.commit.timeout")
      .withAlternative("rss.flush.timeout")
      .categories("worker")
      .doc("Timeout for a Celeborn worker to commit files of a shuffle.")
      .version("0.2.0")
      .fallbackConf(RPC_ASK_TIMEOUT)

  val PARTITION_SORTER_SORT_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.partitionSorter.sort.timeout")
      .withAlternative("rss.partition.sort.timeout")
      .categories("worker")
      .doc("Timeout for a shuffle file to sort.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("220s")

  val PARTITION_SORTER_PER_PARTITION_RESERVED_MEMORY: ConfigEntry[Long] =
    buildConf("celeborn.worker.partitionSorter.reservedMemoryPerPartition")
      .withAlternative("rss.worker.initialReserveSingleSortMemory")
      .categories("worker")
      .doc("Reserved memory when sorting a shuffle file off-heap.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("1mb")

  val WORKER_FLUSHER_BUFFER_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.worker.flusher.buffer.size")
      .withAlternative("rss.worker.flush.buffer.size")
      .categories("worker")
      .version("0.2.0")
      .doc("Size of buffer used by a single flusher.")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("256k")

  val WORKER_WRITER_CLOSE_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.writer.close.timeout")
      .withAlternative("rss.filewriter.timeout")
      .categories("worker")
      .doc("Timeout for a file writer to close")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("120s")

  val WORKER_FLUSHER_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.threads")
      .withAlternative("rss.flusher.thread.count")
      .categories("worker")
      .doc("Flusher's thread count per disk for unkown-type disks.")
      .version("0.2.0")
      .intConf
      .createWithDefault(2)

  val WORKER_FLUSHER_HDD_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.hdd.threads")
      .withAlternative("rss.flusher.hdd.thread.count")
      .categories("worker")
      .doc("Flusher's thread count per disk used for write data to HDD disks.")
      .version("0.2.0")
      .intConf
      .createWithDefault(1)

  val WORKER_FLUSHER_SSD_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.ssd.threads")
      .withAlternative("rss.flusher.ssd.thread.count")
      .categories("worker")
      .doc("Flusher's thread count per disk used for write data to SSD disks.")
      .version("0.2.0")
      .intConf
      .createWithDefault(8)

  val WORKER_FLUSHER_HDFS_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.hdfs.threads")
      .withAlternative("rss.worker.hdfs.flusher.thread.count")
      .categories("worker")
      .doc("Flusher's thread count used for write data to HDFS.")
      .version("0.2.0")
      .intConf
      .createWithDefault(4)

  val WORKER_FLUSHER_SHUTDOWN_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.flusher.shutdownTimeout")
      .withAlternative("rss.worker.diskFlusherShutdownTimeoutMs")
      .categories("worker")
      .doc("Timeout for a flusher to shutdown.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("3s")

  val WORKER_DISK_RESERVE_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.worker.disk.reserve.size")
      .withAlternative("rss.disk.minimum.reserve.size")
      .categories("worker")
      .doc("Celeborn worker reserved space for each disk.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("5G")

  val WORKER_FLUSHER_AVGFLUSHTIME_SLIDINGWINDOW_SIZE: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.avgFlushTime.slidingWindow.size")
      .withAlternative("rss.flusher.avg.time.window")
      .categories("worker")
      .doc("The size of sliding windows used to calculate statistics about flushed time and count.")
      .version("0.2.0")
      .intConf
      .createWithDefault(20)

  val WORKER_FLUSHER_AVGFLUSHTIME_SLIDINGWINDOW_MINCOUNT: ConfigEntry[Int] =
    buildConf("celeborn.worker.flusher.avgFlushTime.slidingWindow.minCount")
      .withAlternative("rss.flusher.avg.time.minimum.count")
      .categories("worker")
      .doc("The minimum flush count to enter a sliding window" +
        " to calculate statistics about flushed time and count.")
      .version("0.2.0")
      .internal
      .intConf
      .createWithDefault(1000)

  val SLOTS_ASSIGN_LOADAWARE_DISKGROUP_NUM: ConfigEntry[Int] =
    buildConf("celeborn.slots.assign.loadAware.numDiskGroups")
      .withAlternative("rss.disk.groups")
      .categories("master")
      .doc("This configuration is a guidance for load-aware slot allocation algorithm. " +
        "This value is control how many disk groups will be created.")
      .version("0.2.0")
      .intConf
      .createWithDefault(5)

  val SLOTS_ASSIGN_LOADAWARE_DISKGROUP_GRADIENT: ConfigEntry[Double] =
    buildConf("celeborn.slots.assign.loadAware.diskGroupGradient")
      .withAlternative("rss.disk.groups.gradient")
      .categories("master")
      .doc("This value means how many more workload will be placed into a faster disk group " +
        "than a slower group.")
      .version("0.2.0")
      .doubleConf
      .createWithDefault(0.1)

  val SLOTS_ASSIGN_EXTRA_SLOTS: ConfigEntry[Int] =
    buildConf("celeborn.slots.assign.extraSlots")
      .withAlternative("rss.offer.slots.extra.size")
      .categories("master")
      .version("0.2.0")
      .doc("Extra slots number when master assign slots.")
      .intConf
      .createWithDefault(2)

  val SLOTS_ASSIGN_POLICY: ConfigEntry[String] =
    buildConf("celeborn.slots.assign.policy")
      .withAlternative("rss.offer.slots.algorithm")
      .categories("master")
      .version("0.2.0")
      .doc("Policy for master to assign slots, Celeborn supports two types of policy: roundrobin and loadaware.")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set(SlotsAssignPolicy.ROUNDROBIN.name, SlotsAssignPolicy.LOADAWARE.name))
      .createWithDefault(SlotsAssignPolicy.ROUNDROBIN.name)

  val SHUFFLE_INITIAL_ESRIMATED_PARTITION_SIZE: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.initialEstimatedPartitionSize")
      .withAlternative("rss.initial.partition.size")
      .categories("master")
      .doc("Initial partition size for estimation, it will change according to runtime stats.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("64mb")

  val SHUFFLE_MIN_PARTITION_SIZE_TO_ESTIMATE: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.minPartitionSizeToEstimate")
      .withAlternative("rss.minimum.estimate.partition.size")
      .categories("worker")
      .doc(
        "Ignore partition size smaller than this configuration of partition size for estimation.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("8mb")

  val SHUFFLE_ESTIMATED_PARTITION_SIZE_UPDATE_INITIAL_DELAY: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.estimatedPartitionSize.update.initialDelay")
      .withAlternative("rss.partition.size.update.initial.delay")
      .categories("master")
      .doc("Initial delay time before start updating partition size for estimation.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("5min")

  val SHUFFLE_ESTIMATED_PARTITION_SIZE_UPDATE_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.estimatedPartitionSize.update.interval")
      .withAlternative("rss.partition.size.update.interval")
      .categories("master")
      .doc("Interval of updating partition size for estimation.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10min")

  val METRICS_APP_TOP_DISK_USAGE_COUNT: ConfigEntry[Int] =
    buildConf("celeborn.metrics.app.topDiskUsage.count")
      .withAlternative("rss.metrics.app.topDiskUsage.count")
      .categories("master")
      .doc("Size for top items about top disk usage applications list.")
      .version("0.2.0")
      .intConf
      .createWithDefault(50)

  val METRICS_APP_TOP_DISK_USAGE_WINDOW_SIZE: ConfigEntry[Int] =
    buildConf("celeborn.metrics.app.topDiskUsage.windowSize")
      .withAlternative("rss.metrics.app.topDiskUsage.windowSize")
      .categories("master")
      .doc("Window size about top disk usage application list.")
      .version("0.2.0")
      .intConf
      .createWithDefault(24)

  val METRICS_APP_TOP_DISK_USAGE_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.metrics.app.topDiskUsage.interval")
      .withAlternative("rss.metrics.app.topDiskUsage.interval")
      .categories("master")
      .doc("Time length for a window about top disk usage application list.")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("10min")

  val METRICS_RESOURCE_CONSUMPTION_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.metrics.userResourceConsumption.update.interval")
      .categories("master")
      .doc("Time length for a window about compute user resource consumption.")
      .version("0.3.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("30s")

  val PUSH_STAGE_END_TIMEOUT: OptionalConfigEntry[Long] =
    buildConf("celeborn.push.stageEnd.timeout")
      .withAlternative("rss.stage.end.timeout")
      .categories("client")
      .doc(s"Timeout for waiting StageEnd. " +
        s"Default value should be `${RPC_ASK_TIMEOUT.key} * (${COMMIT_FILE_REQUEST_MAX_RETRY.key} + 1)`.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createOptional
  val PUSH_LIMIT_STRATEGY: ConfigEntry[String] =
    buildConf("celeborn.push.limit.strategy")
      .categories("client")
      .doc("The strategy used to control the push speed. " +
        "Valid strategies are SIMPLE and SLOWSTART. the SLOWSTART strategy is usually cooperate with " +
        "congest control mechanism in the worker side.")
      .version("0.3.0")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set("SIMPLE", "SLOWSTART"))
      .createWithDefaultString("SIMPLE")

  val PUSH_SLOW_START_INITIAL_SLEEP_TIME: ConfigEntry[Long] =
    buildConf("celeborn.push.slowStart.initialSleepTime")
      .categories("client")
      .version("0.3.0")
      .doc(s"The initial sleep time if the current max in flight requests is 0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("500ms")

  val PUSH_SLOW_START_MAX_SLEEP_TIME: ConfigEntry[Long] =
    buildConf("celeborn.push.slowStart.maxSleepTime")
      .categories("client")
      .version("0.3.0")
      .doc(s"If ${PUSH_LIMIT_STRATEGY.key} is set to SLOWSTART, push side will " +
        "take a sleep strategy for each batch of requests, this controls " +
        "the max sleep time if the max in flight requests limit is 1 for a long time")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("2s")

  val PUSH_DATA_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.push.data.timeout")
      .withAlternative("rss.push.data.rpc.timeout")
      .categories("client")
      .version("0.2.0")
      .doc(s"Timeout for a task to push data rpc message. This value should better be more than twice of `${PUSH_TIMEOUT_CHECK_INTERVAL.key}`")
      .timeConf(TimeUnit.MILLISECONDS)
      .checkValue(_ > 0, "celeborn.push.data.timeout must be positive!")
      .createWithDefaultString("120s")

  val TEST_PUSH_MASTER_DATA_TIMEOUT: ConfigEntry[Boolean] =
    buildConf("celeborn.test.pushMasterDataTimeout")
      .categories("worker")
      .version("0.2.0")
      .doc("Whether to test push master data timeout")
      .booleanConf
      .createWithDefault(false)

  val TEST_PUSH_SLAVE_DATA_TIMEOUT: ConfigEntry[Boolean] =
    buildConf("celeborn.test.pushSlaveDataTimeout")
      .categories("worker")
      .version("0.3.0")
      .doc("Whether to test push slave data timeout")
      .booleanConf
      .createWithDefault(false)

  val PUSH_LIMIT_IN_FLIGHT_TIMEOUT: OptionalConfigEntry[Long] =
    buildConf("celeborn.push.limit.inFlight.timeout")
      .withAlternative("rss.limit.inflight.timeout")
      .categories("client")
      .doc("Timeout for netty in-flight requests to be done." +
        s"Default value should be `${PUSH_DATA_TIMEOUT.key} * 2`.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createOptional

  val PUSH_LIMIT_IN_FLIGHT_SLEEP_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.push.limit.inFlight.sleepInterval")
      .withAlternative("rss.limit.inflight.sleep.delta")
      .categories("client")
      .doc("Sleep interval when check netty in-flight requests to be done.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("50ms")

  val PUSH_SORT_MEMORY_THRESHOLD: ConfigEntry[Long] =
    buildConf("celeborn.push.sortMemory.threshold")
      .withAlternative("rss.sort.push.data.threshold")
      .categories("client")
      .doc("When SortBasedPusher use memory over the threshold, will trigger push data.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("64m")

  val PUSH_SORT_PIPELINE_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.push.sort.pipeline.enabled")
      .categories("client")
      .doc("Whether to enable pipelining for sort based shuffle writer. If true, double buffering" +
        " will be used to pipeline push")
      .version("0.2.1")
      .booleanConf
      .createWithDefault(false)

  val PUSH_SORT_RANDOMIZE_PARITION_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.push.sort.randomizePartitionId.enabled")
      .categories("client")
      .doc(
        "Whether to randomize partitionId in push sorter. If true, partitionId will be randomized " +
          "when sort data to avoid skew when push to worker")
      .version("0.2.1")
      .booleanConf
      .createWithDefault(false)

  val PUSH_RETRY_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.push.retry.threads")
      .withAlternative("rss.pushdata.retry.thread.num")
      .categories("client")
      .doc("Thread number to process shuffle re-send push data requests.")
      .version("0.2.0")
      .intConf
      .createWithDefault(8)

  val PUSH_SPLIT_PARTITION_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.push.splitPartition.threads")
      .withAlternative("rss.client.split.pool.size")
      .categories("client")
      .doc("Thread number to process shuffle split request in shuffle client.")
      .version("0.2.0")
      .intConf
      .createWithDefault(8)

  val PARTITION_SPLIT_THRESHOLD: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.partitionSplit.threshold")
      .withAlternative("rss.partition.split.threshold")
      .categories("client")
      .doc("Shuffle file size threshold, if file size exceeds this, trigger split.")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("1G")

  val PARTITION_SPLIT_MODE: ConfigEntry[String] =
    buildConf("celeborn.shuffle.partitionSplit.mode")
      .withAlternative("rss.partition.split.mode")
      .categories("client")
      .doc("soft: the shuffle file size might be larger than split threshold. " +
        "hard: the shuffle file size will be limited to split threshold.")
      .version("0.2.0")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set(PartitionSplitMode.SOFT.name, PartitionSplitMode.HARD.name))
      .createWithDefault(PartitionSplitMode.SOFT.name)

  val PARTITION_SPLIT_MIN: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.partitionSplit.min")
      .withAlternative("rss.partition.split.minimum.size")
      .categories("worker")
      .doc("Min size for a partition to split")
      .version("0.2.0")
      .bytesConf(ByteUnit.BYTE)
      .createWithDefaultString("1m")

  val BATCH_HANDLE_CHANGE_PARTITION_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.shuffle.batchHandleChangePartition.enabled")
      .withAlternative("rss.change.partition.batch.enabled")
      .categories("client")
      .doc("When true, LifecycleManager will handle change partition request in batch. " +
        "Otherwise, LifecycleManager will process the requests one by one")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val BATCH_HANDLE_CHANGE_PARTITION_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.shuffle.batchHandleChangePartition.threads")
      .withAlternative("rss.change.partition.numThreads")
      .categories("client")
      .doc("Threads number for LifecycleManager to handle change partition request in batch.")
      .version("0.2.0")
      .intConf
      .createWithDefault(8)

  val BATCH_HANDLE_CHANGE_PARTITION_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.batchHandleChangePartition.interval")
      .withAlternative("rss.change.partition.batchInterval")
      .categories("client")
      .doc("Interval for LifecycleManager to schedule handling change partition requests in batch.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("100ms")

  val BATCH_HANDLE_COMMIT_PARTITION_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.shuffle.batchHandleCommitPartition.enabled")
      .categories("client")
      .doc("When true, LifecycleManager will handle commit partition request in batch. " +
        "Otherwise, LifecycleManager won't commit partition before stage end")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val BATCH_HANDLE_COMMIT_PARTITION_THREADS: ConfigEntry[Int] =
    buildConf("celeborn.shuffle.batchHandleCommitPartition.threads")
      .categories("client")
      .doc("Threads number for LifecycleManager to handle commit partition request in batch.")
      .version("0.2.0")
      .intConf
      .createWithDefault(8)

  val BATCH_HANDLED_COMMIT_PARTITION_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.batchHandleCommitPartition.interval")
      .categories("client")
      .doc("Interval for LifecycleManager to schedule handling commit partition requests in batch.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("5s")

  val REGISTER_SHUFFLE_RPC_ASK_TIMEOUT: OptionalConfigEntry[Long] =
    buildConf("celeborn.rpc.registerShuffle.askTimeout")
      .categories("client")
      .version("0.2.0")
      .doc(s"Timeout for ask operations during register shuffle. " +
        s"Default value should be `${RPC_ASK_TIMEOUT.key} * (${RESERVE_SLOTS_MAX_RETRIES.key} + 1 + 1)`.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createOptional

  val REQUEST_PARTITION_LOCATION_RPC_ASK_TIMEOUT: OptionalConfigEntry[Long] =
    buildConf("celeborn.rpc.requestPartition.askTimeout")
      .categories("client")
      .version("0.2.0")
      .doc(s"Timeout for ask operations during request change partition location, such as revive or split partition. " +
        s"Default value should be `${RPC_ASK_TIMEOUT.key} * (${RESERVE_SLOTS_MAX_RETRIES.key} + 1)`.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createOptional

  val GET_REDUCER_FILE_GROUP_RPC_ASK_TIMEOUT: OptionalConfigEntry[Long] =
    buildConf("celeborn.rpc.getReducerFileGroup.askTimeout")
      .categories("client")
      .version("0.2.0")
      .doc(s"Timeout for ask operations during get reducer file group. " +
        s"Default value should be `${RPC_ASK_TIMEOUT.key} * (${COMMIT_FILE_REQUEST_MAX_RETRY.key} + 1 + 1)`.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createOptional

  val PORT_MAX_RETRY: ConfigEntry[Int] =
    buildConf("celeborn.port.maxRetries")
      .withAlternative("rss.master.port.maxretry")
      .categories("network")
      .doc("When port is occupied, we will retry for max retry times.")
      .version("0.2.0")
      .intConf
      .createWithDefault(1)

  val CLIENT_MAX_RETRIES: ConfigEntry[Int] =
    buildConf("celeborn.client.maxRetries")
      .withAlternative("rss.ha.client.maxTries")
      .categories("client", "worker")
      .doc("Max retry times for client to connect master endpoint")
      .version("0.2.0")
      .intConf
      .createWithDefault(15)

  val CLIENT_CLOSE_IDLE_CONNECTIONS: ConfigEntry[Boolean] =
    buildConf("celeborn.client.closeIdleConnections")
      .categories("client")
      .doc("Whether client will close idle connections.")
      .version("0.3.0")
      .booleanConf
      .createWithDefault(true)

  val METRICS_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.metrics.enabled")
      .withAlternative("rss.metrics.system.enabled")
      .categories("master", "worker", "metrics")
      .doc("When true, enable metrics system.")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(true)

  val METRICS_SAMPLE_RATE: ConfigEntry[Double] =
    buildConf("celeborn.metrics.sample.rate")
      .withAlternative("rss.metrics.system.sample.rate")
      .categories("master", "worker", "metrics")
      .doc("It controls if Celeborn collect timer metrics for some operations. Its value should be in [0.0, 1.0].")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 1.0, "should be in [0.0, 1.0].")
      .createWithDefault(1.0)

  val METRICS_SLIDING_WINDOW_SIZE: ConfigEntry[Int] =
    buildConf("celeborn.metrics.timer.slidingWindow.size")
      .withAlternative("rss.metrics.system.sliding.window.size")
      .categories("master", "worker", "metrics")
      .doc("The sliding window size of timer metric.")
      .version("0.2.0")
      .intConf
      .createWithDefault(4096)

  val METRICS_COLLECT_CRITICAL_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.metrics.collectPerfCritical.enabled")
      .withAlternative("rss.metrics.system.sample.perf.critical")
      .categories("master", "worker", "metrics")
      .doc("It controls whether to collect metrics which may affect performance. When enable, Celeborn collects them.")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val METRICS_CAPACITY: ConfigEntry[Int] =
    buildConf("celeborn.metrics.capacity")
      .withAlternative("rss.inner.metrics.size")
      .categories("master", "worker", "metrics")
      .doc("The maximum number of metrics which a source can use to generate output strings.")
      .version("0.2.0")
      .intConf
      .createWithDefault(4096)

  val MASTER_PROMETHEUS_HOST: ConfigEntry[String] =
    buildConf("celeborn.master.metrics.prometheus.host")
      .withAlternative("rss.master.prometheus.metric.host")
      .categories("master", "metrics")
      .doc("Master's Prometheus host.")
      .version("0.2.0")
      .stringConf
      .createWithDefault("0.0.0.0")

  val MASTER_PROMETHEUS_PORT: ConfigEntry[Int] =
    buildConf("celeborn.master.metrics.prometheus.port")
      .withAlternative("rss.master.prometheus.metric.port")
      .categories("master", "metrics")
      .doc("Master's Prometheus port.")
      .version("0.2.0")
      .intConf
      .checkValue(p => p >= 1024 && p < 65535, "invalid port")
      .createWithDefault(9098)

  val WORKER_PROMETHEUS_HOST: ConfigEntry[String] =
    buildConf("celeborn.worker.metrics.prometheus.host")
      .withAlternative("rss.worker.prometheus.metric.host")
      .categories("worker", "metrics")
      .doc("Worker's Prometheus host.")
      .version("0.2.0")
      .stringConf
      .createWithDefault("0.0.0.0")

  val WORKER_PROMETHEUS_PORT: ConfigEntry[Int] =
    buildConf("celeborn.worker.metrics.prometheus.port")
      .withAlternative("rss.worker.prometheus.metric.port")
      .categories("worker", "metrics")
      .doc("Worker's Prometheus port.")
      .version("0.2.0")
      .intConf
      .checkValue(p => p >= 1024 && p < 65535, "invalid port")
      .createWithDefault(9096)

  val QUOTA_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.quota.enabled")
      .withAlternative("rss.cluster.checkQuota.enabled")
      .categories("quota")
      .doc("When true, before registering shuffle, LifecycleManager should check " +
        "if current user have enough quota space, if cluster don't have enough " +
        "quota space for current user, fallback to Spark's default shuffle")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(true)

  val QUOTA_IDENTITY_PROVIDER: ConfigEntry[String] =
    buildConf("celeborn.quota.identity.provider")
      .withAlternative("rss.identity.provider")
      .categories("quota")
      .doc(s"IdentityProvider class name. Default class is " +
        s"`${classOf[DefaultIdentityProvider].getName}`, return `${classOf[UserIdentifier].getName}` " +
        s"with default tenant id and username from `${classOf[UserGroupInformation].getName}`. ")
      .version("0.2.0")
      .stringConf
      .createWithDefault(classOf[DefaultIdentityProvider].getName)

  val QUOTA_MANAGER: ConfigEntry[String] =
    buildConf("celeborn.quota.manager")
      .withAlternative("rss.quota.manager")
      .categories("quota")
      .doc(s"QuotaManger class name. Default class is `${classOf[DefaultQuotaManager].getName}`.")
      .version("0.2.0")
      .stringConf
      .createWithDefault(classOf[DefaultQuotaManager].getName)

  val QUOTA_CONFIGURATION_PATH: OptionalConfigEntry[String] =
    buildConf("celeborn.quota.configuration.path")
      .withAlternative("rss.quota.configuration.path")
      .categories("quota")
      .doc("Quota configuration file path. The file format should be yaml. Quota configuration file template can be " +
        "found under conf directory.")
      .version("0.2.0")
      .stringConf
      .createOptional

  val SHUFFLE_FORCE_FALLBACK_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.shuffle.forceFallback.enabled")
      .withAlternative("rss.force.fallback")
      .categories("client")
      .version("0.2.0")
      .doc("Whether force fallback shuffle to Spark's default.")
      .booleanConf
      .createWithDefault(false)

  val SHUFFLE_FORCE_FALLBACK_PARTITION_THRESHOLD: ConfigEntry[Long] =
    buildConf("celeborn.shuffle.forceFallback.numPartitionsThreshold")
      .withAlternative("rss.max.partition.number")
      .categories("client")
      .version("0.2.0")
      .doc(
        "Celeborn will only accept shuffle of partition number lower than this configuration value.")
      .longConf
      .createWithDefault(500000)

  val SHUFFLE_MANAGER_PORT: ConfigEntry[Int] =
    buildConf("celeborn.shuffle.manager.port")
      .withAlternative("rss.driver.metaService.port")
      .categories("client")
      .version("0.2.0")
      .doc("Port used by the LifecycleManager on the Driver.")
      .intConf
      .checkValue(
        (port: Int) => {
          if (port != 0) {
            logWarning(
              "The user specifies the port used by the LifecycleManager on the Driver, and its" +
                s" values is $port, which may cause port conflicts and startup failure.")
          }
          true
        },
        "")
      .createWithDefault(0)

  val WORKER_DISK_MONITOR_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.worker.monitor.disk.enabled")
      .withAlternative("rss.device.monitor.enabled")
      .categories("worker")
      .version("0.2.0")
      .doc("When true, worker will monitor device and report to master.")
      .booleanConf
      .createWithDefault(true)

  val WORKER_DISK_MONITOR_CHECKLIST: ConfigEntry[Seq[String]] =
    buildConf("celeborn.worker.monitor.disk.checklist")
      .withAlternative("rss.device.monitor.checklist")
      .categories("worker")
      .version("0.2.0")
      .doc("Monitor type for disk, available items are: " +
        "iohang, readwrite and diskusage.")
      .stringConf
      .transform(_.toLowerCase)
      .toSequence
      .createWithDefaultString("readwrite,diskusage")

  val WORKER_DISK_MONITOR_CHECK_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.monitor.disk.checkInterval")
      .withAlternative("rss.disk.check.interval")
      .categories("worker")
      .version("0.2.0")
      .doc("Intervals between device monitor to check disk.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("60s")

  val WORKER_DISK_MONITOR_SYS_BLOCK_DIR: ConfigEntry[String] =
    buildConf("celeborn.worker.monitor.disk.sys.block.dir")
      .withAlternative("rss.sys.block.dir")
      .categories("worker")
      .version("0.2.0")
      .doc("The directory where linux file block information is stored.")
      .stringConf
      .createWithDefault("/sys/block")

  val WORKER_DISK_MONITOR_NOTIFY_ERROR_THRESHOLD: ConfigEntry[Int] =
    buildConf("celeborn.worker.monitor.disk.notifyError.threshold")
      .categories("worker")
      .version("0.3.0")
      .doc("Device monitor will only notify critical error once the accumulated valid non-critical error number " +
        "exceeding this threshold.")
      .intConf
      .createWithDefault(64)

  val WORKER_DISK_MONITOR_NOTIFY_ERROR_EXPIRE_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.monitor.disk.notifyError.expireTimeout")
      .categories("worker")
      .version("0.3.0")
      .doc("The expire timeout of non-critical device error. Only notify critical error when the number of non-critical " +
        "errors for a period of time exceeds threshold.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10m")

  val WORKER_WRITER_CREATE_MAX_ATTEMPTS: ConfigEntry[Int] =
    buildConf("celeborn.worker.writer.create.maxAttempts")
      .withAlternative("rss.create.file.writer.retry.count")
      .categories("worker")
      .version("0.2.0")
      .doc("Retry count for a file writer to create if its creation was failed.")
      .intConf
      .createWithDefault(3)

  val SHUFFLE_PARTITION_TYPE: ConfigEntry[String] =
    buildConf("celeborn.shuffle.partition.type")
      .withAlternative("rss.partition.type")
      .categories("client")
      .doc("Type of shuffle's partition.")
      .version("0.2.0")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set(
        PartitionType.REDUCE.name,
        PartitionType.MAP.name,
        PartitionType.MAPGROUP.name))
      .createWithDefault(PartitionType.REDUCE.name)

  val SHUFFLE_COMPRESSION_CODEC: ConfigEntry[String] =
    buildConf("celeborn.shuffle.compression.codec")
      .withAlternative("rss.client.compression.codec")
      .categories("client")
      .doc("The codec used to compress shuffle data. By default, Celeborn provides two codecs: `lz4` and `zstd`.")
      .version("0.2.0")
      .stringConf
      .transform(_.toUpperCase(Locale.ROOT))
      .checkValues(Set(CompressionCodec.LZ4.name, CompressionCodec.ZSTD.name))
      .createWithDefault(CompressionCodec.LZ4.name)

  val SHUFFLE_COMPRESSION_ZSTD_LEVEL: ConfigEntry[Int] =
    buildConf("celeborn.shuffle.compression.zstd.level")
      .withAlternative("rss.client.compression.zstd.level")
      .categories("client")
      .doc("Compression level for Zstd compression codec, its value should be an integer " +
        "between -5 and 22. Increasing the compression level will result in better compression " +
        "at the expense of more CPU and memory.")
      .version("0.2.0")
      .intConf
      .checkValue(
        value => value >= -5 && value <= 22,
        s"Compression level for Zstd compression codec should be an integer between -5 and 22.")
      .createWithDefault(1)

  val PARTITION_SORTER_DIRECT_MEMORY_RATIO_THRESHOLD: ConfigEntry[Double] =
    buildConf("celeborn.worker.partitionSorter.directMemoryRatioThreshold")
      .withAlternative("rss.partition.sort.memory.max.ratio")
      .categories("worker")
      .doc("Max ratio of partition sorter's memory for sorting, when reserved memory is higher than max partition " +
        "sorter memory, partition sorter will stop sorting.")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 1.0, "should be in [0.0, 1.0].")
      .createWithDefault(0.1)

  val WORKER_DIRECT_MEMORY_RATIO_FOR_READ_BUFFER: ConfigEntry[Double] =
    buildConf("celeborn.worker.directMemoryRatioForReadBuffer")
      .categories("worker")
      .doc("Max ratio of direct memory for read buffer")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 0.4, "should be in [0.0, 0.4].")
      .createWithDefault(0.1)

  val WORKER_DIRECT_MEMORY_RATIO_FOR_SHUFFLE_STORAGE: ConfigEntry[Double] =
    buildConf("celeborn.worker.directMemoryRatioForMemoryShuffleStorage")
      .categories("worker")
      .doc("Max ratio of direct memory to store shuffle data")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 0.4, "should be in [0.0, 0.4].")
      .createWithDefault(0.1)

  val WORKER_DIRECT_MEMORY_RATIO_PAUSE_RECEIVE: ConfigEntry[Double] =
    buildConf("celeborn.worker.directMemoryRatioToPauseReceive")
      .withAlternative("rss.pause.pushdata.memory.ratio")
      .categories("worker")
      .doc("If direct memory usage reaches this limit, the worker will stop to receive data from Celeborn shuffle clients.")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 1.0, "should be in [0.0, 1.0].")
      .createWithDefault(0.85)

  val WORKER_DIRECT_MEMORY_RATIO_PAUSE_REPLICATE: ConfigEntry[Double] =
    buildConf("celeborn.worker.directMemoryRatioToPauseReplicate")
      .withAlternative("rss.pause.replicate.memory.ratio")
      .categories("worker")
      .doc("If direct memory usage reaches this limit, the worker will stop to receive replication data from other workers.")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 1.0, "should be in [0.0, 1.0].")
      .createWithDefault(0.95)

  val WORKER_DIRECT_MEMORY_RATIO_RESUME: ConfigEntry[Double] =
    buildConf("celeborn.worker.directMemoryRatioToResume")
      .withAlternative("rss.resume.memory.ratio")
      .categories("worker")
      .doc("If direct memory usage is less than this limit, worker will resume.")
      .version("0.2.0")
      .doubleConf
      .checkValue(v => v >= 0.0 && v <= 1.0, "should be in [0.0, 1.0].")
      .createWithDefault(0.5)

  val WORKER_DIRECT_MEMORY_CHECK_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.memory.checkInterval")
      .withAlternative("rss.worker.memory.check.interval")
      .categories("worker")
      .doc("Interval of worker direct memory checking.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10ms")

  val WORKER_DIRECT_MEMORY_REPORT_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.memory.reportInterval")
      .withAlternative("rss.worker.memory.report.interval")
      .categories("worker")
      .doc("Interval of worker direct memory tracker reporting to log.")
      .version("0.2.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("10s")

  val WORKER_CONGESTION_CONTROL_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.worker.congestionControl.enabled")
      .categories("worker")
      .doc("Whether to enable congestion control or not.")
      .version("0.3.0")
      .booleanConf
      .createWithDefault(false)

  val WORKER_CONGESTION_CONTROL_SAMPLE_TIME_WINDOW: ConfigEntry[Long] =
    buildConf("celeborn.worker.congestionControl.sample.time.window")
      .categories("worker")
      .doc("The worker holds a time sliding list to calculate users' produce/consume rate")
      .version("0.3.0")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("10s")

  val WORKER_CONGESTION_CONTROL_LOW_WATERMARK: OptionalConfigEntry[Long] =
    buildConf("celeborn.worker.congestionControl.low.watermark")
      .categories("worker")
      .doc("Will stop congest users if the total pending bytes of disk buffer is lower than " +
        "this configuration")
      .version("0.3.0")
      .bytesConf(ByteUnit.BYTE)
      .createOptional

  val WORKER_CONGESTION_CONTROL_HIGH_WATERMARK: OptionalConfigEntry[Long] =
    buildConf("celeborn.worker.congestionControl.high.watermark")
      .categories("worker")
      .doc("If the total bytes in disk buffer exceeds this configure, will start to congest" +
        "users whose produce rate is higher than the potential average consume rate. " +
        "The congestion will stop if the produce rate is lower or equal to the " +
        "average consume rate, or the total pending bytes lower than " +
        s"${WORKER_CONGESTION_CONTROL_LOW_WATERMARK.key}")
      .version("0.3.0")
      .bytesConf(ByteUnit.BYTE)
      .createOptional

  val WORKER_CONGESTION_CONTROL_USER_INACTIVE_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.congestionControl.user.inactive.interval")
      .categories("worker")
      .doc("How long will consider this user is inactive if it doesn't send data")
      .version("0.3.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("10min")

  val WORKER_GRACEFUL_SHUTDOWN_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.worker.graceful.shutdown.enabled")
      .withAlternative("rss.worker.graceful.shutdown")
      .categories("worker")
      .doc("When true, during worker shutdown, the worker will wait for all released slots " +
        s"to be committed or destroyed.")
      .version("0.2.0")
      .booleanConf
      .createWithDefault(false)

  val WORKER_GRACEFUL_SHUTDOWN_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.graceful.shutdown.timeout")
      .withAlternative("rss.worker.shutdown.timeout")
      .categories("worker")
      .doc("The worker's graceful shutdown timeout time.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("600s")

  val WORKER_CHECK_SLOTS_FINISHED_INTERVAL: ConfigEntry[Long] =
    buildConf("celeborn.worker.graceful.shutdown.checkSlotsFinished.interval")
      .withAlternative("rss.worker.checkSlots.interval")
      .categories("worker")
      .doc("The wait interval of checking whether all released slots " +
        "to be committed or destroyed during worker graceful shutdown")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("1s")

  val WORKER_CHECK_SLOTS_FINISHED_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.graceful.shutdown.checkSlotsFinished.timeout")
      .withAlternative("rss.worker.checkSlots.timeout")
      .categories("worker")
      .doc("The wait time of waiting for the released slots" +
        " to be committed or destroyed during worker graceful shutdown.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("480s")

  val WORKER_RECOVER_PATH: ConfigEntry[String] =
    buildConf("celeborn.worker.graceful.shutdown.recoverPath")
      .withAlternative("rss.worker.recoverPath")
      .categories("worker")
      .doc("The path to store levelDB.")
      .version("0.2.0")
      .stringConf
      .transform(_.replace("<tmp>", System.getProperty("java.io.tmpdir")))
      .createWithDefault(s"<tmp>/recover")

  val PARTITION_SORTER_SHUTDOWN_TIMEOUT: ConfigEntry[Long] =
    buildConf("celeborn.worker.graceful.shutdown.partitionSorter.shutdownTimeout")
      .withAlternative("rss.worker.partitionSorterCloseAwaitTime")
      .categories("worker")
      .doc("The wait time of waiting for sorting partition files" +
        " during worker graceful shutdown.")
      .version("0.2.0")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("120s")

  val HDFS_DIR: OptionalConfigEntry[String] =
    buildConf("celeborn.storage.hdfs.dir")
      .withAlternative("rss.worker.hdfs.dir")
      .categories("worker", "client")
      .version("0.2.0")
      .doc("HDFS dir configuration for Celeborn to access HDFS.")
      .stringConf
      .createOptional

  val SHUFFLE_RANGE_READ_FILTER_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.shuffle.rangeReadFilter.enabled")
      .withAlternative("rss.range.read.filter.enabled")
      .categories("client")
      .version("0.2.0")
      .doc("If a spark application have skewed partition, this value can set to true to improve performance.")
      .booleanConf
      .createWithDefault(false)

  val COLUMNAR_SHUFFLE_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.columnar.shuffle.enabled")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Whether to enable columnar-based shuffle.")
      .booleanConf
      .createWithDefault(false)

  val COLUMNAR_SHUFFLE_BATCH_SIZE: ConfigEntry[Int] =
    buildConf("celeborn.columnar.shuffle.batch.size")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Vector batch size for columnar shuffle.")
      .intConf
      .checkValue(v => v > 0, "value must be positive")
      .createWithDefault(10000)

  val COLUMNAR_SHUFFLE_OFF_HEAP_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.columnar.offHeap.enabled")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Whether to use off heap columnar vector.")
      .booleanConf
      .createWithDefault(false)

  val COLUMNAR_SHUFFLE_DICTIONARY_ENCODING_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.columnar.shuffle.encoding.dictionary.enabled")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Whether to use dictionary encoding for columnar-based shuffle data.")
      .booleanConf
      .createWithDefault(false)

  val COLUMNAR_SHUFFLE_DICTIONARY_ENCODING_MAX_FACTOR: ConfigEntry[Double] =
    buildConf("celeborn.columnar.shuffle.encoding.dictionary.maxFactor")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Max factor for dictionary size. The max dictionary size is " +
        s"`min(${Utils.bytesToString(Short.MaxValue)}, ${COLUMNAR_SHUFFLE_BATCH_SIZE.key} * " +
        s"celeborn.columnar.shuffle.encoding.dictionary.maxFactor)`.")
      .doubleConf
      .createWithDefault(0.3)

  val COLUMNAR_SHUFFLE_CODEGEN_ENABLED: ConfigEntry[Boolean] =
    buildConf("celeborn.columnar.shuffle.codegen.enabled")
      .categories("columnar-shuffle")
      .version("0.2.0")
      .doc("Whether to use codegen for columnar-based shuffle.")
      .booleanConf
      .createWithDefault(false)

  val RPC_CACHE_SIZE: ConfigEntry[Int] =
    buildConf("celeborn.rpc.cache.size")
      .categories("client")
      .withAlternative("rss.rpc.cache.size")
      .version("0.2.0")
      .doc("The max cache items count for rpc cache.")
      .intConf
      .createWithDefault(256)

  val RPC_CACHE_CONCURRENCY_LEVEL: ConfigEntry[Int] =
    buildConf("celeborn.rpc.cache.concurrencyLevel")
      .categories("client")
      .withAlternative("rss.rpc.cache.concurrent.level")
      .version("0.2.0")
      .doc("The number of write locks to update rpc cache.")
      .intConf
      .createWithDefault(32)

  val RPC_CACHE_EXPIRE_TIME: ConfigEntry[Long] =
    buildConf("celeborn.rpc.cache.expireTime")
      .categories("client")
      .withAlternative("rss.rpc.cache.expire")
      .version("0.2.0")
      .doc("The time before a cache item is removed.")
      .timeConf(TimeUnit.MILLISECONDS)
      .createWithDefaultString("15s")

  val WORKER_PARTITION_READ_BUFFERS_MIN: ConfigEntry[Int] =
    buildConf("celeborn.worker.partition.initial.readBuffersMin")
      .categories("worker")
      .version("0.3.0")
      .doc("Min number of initial read buffers")
      .intConf
      .createWithDefault(8)

  val WORKER_PARTITION_READ_BUFFERS_MAX: ConfigEntry[Int] =
    buildConf("celeborn.worker.partition.initial.readBuffersMax")
      .categories("worker")
      .version("0.3.0")
      .doc("Max number of initial read buffers")
      .intConf
      .createWithDefault(8)

  val WORKER_BUFFERSTREAM_THREADS_PER_MOUNTPOINT: ConfigEntry[Int] =
    buildConf("celeborn.worker.bufferStream.threadsPerMountpoint")
      .categories("worker")
      .version("0.3.0")
      .doc("Threads count for read buffer per mount point.")
      .intConf
      .createWithDefault(8)
}

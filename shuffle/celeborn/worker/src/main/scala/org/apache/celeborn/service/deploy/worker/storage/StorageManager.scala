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

package org.apache.celeborn.service.deploy.worker.storage

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.{FileAlreadyExistsException, Files, Paths}
import java.util
import java.util.concurrent.{ConcurrentHashMap, ThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.{BiConsumer, IntUnaryOperator}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import io.netty.buffer.PooledByteBufAllocator
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.fs.permission.FsPermission
import org.iq80.leveldb.DB

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.exception.CelebornException
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, DiskStatus, FileInfo, TimeWindow}
import org.apache.celeborn.common.metrics.source.AbstractSource
import org.apache.celeborn.common.network.util.{NettyUtils, TransportConf}
import org.apache.celeborn.common.protocol.{PartitionLocation, PartitionSplitMode, PartitionType}
import org.apache.celeborn.common.quota.ResourceConsumption
import org.apache.celeborn.common.util.{CelebornHadoopUtils, JavaUtils, PbSerDeUtils, ThreadUtils, Utils}
import org.apache.celeborn.service.deploy.worker._
import org.apache.celeborn.service.deploy.worker.memory.MemoryManager.MemoryPressureListener
import org.apache.celeborn.service.deploy.worker.storage.StorageManager.hadoopFs

final private[worker] class StorageManager(conf: CelebornConf, workerSource: AbstractSource)
  extends ShuffleRecoverHelper with DeviceObserver with Logging with MemoryPressureListener {
  // mount point -> file writer
  val workingDirWriters =
    JavaUtils.newConcurrentHashMap[File, ConcurrentHashMap[String, FileWriter]]()

  val hasHDFSStorage = conf.hasHDFSStorage

  // (deviceName -> deviceInfo) and (mount point -> diskInfo)
  val (deviceInfos, diskInfos) = {
    val workingDirInfos =
      conf.workerBaseDirs.map { case (workdir, maxSpace, flusherThread, storageType) =>
        (new File(workdir, conf.workerWorkingDir), maxSpace, flusherThread, storageType)
      }

    if (workingDirInfos.size <= 0 && !hasHDFSStorage) {
      throw new IOException("Empty working directory configuration!")
    }

    DeviceInfo.getDeviceAndDiskInfos(workingDirInfos, conf)
  }
  val mountPoints = new util.HashSet[String](diskInfos.keySet())

  def disksSnapshot(): List[DiskInfo] = {
    diskInfos.synchronized {
      val disks = new util.ArrayList[DiskInfo](diskInfos.values())
      disks.asScala.toList
    }
  }

  def healthyWorkingDirs(): List[File] =
    disksSnapshot().filter(_.status == DiskStatus.HEALTHY).flatMap(_.dirs)

  private val diskOperators: ConcurrentHashMap[String, ThreadPoolExecutor] = {
    val cleaners = JavaUtils.newConcurrentHashMap[String, ThreadPoolExecutor]()
    disksSnapshot().foreach {
      diskInfo =>
        cleaners.put(
          diskInfo.mountPoint,
          ThreadUtils.newDaemonCachedThreadPool(s"Disk-cleaner-${diskInfo.mountPoint}", 1))
    }
    cleaners
  }

  val tmpDiskInfos = JavaUtils.newConcurrentHashMap[String, DiskInfo]()
  disksSnapshot().foreach { diskInfo =>
    tmpDiskInfos.put(diskInfo.mountPoint, diskInfo)
  }
  private val deviceMonitor =
    DeviceMonitor.createDeviceMonitor(conf, this, deviceInfos, tmpDiskInfos, workerSource)

  private val byteBufAllocator: PooledByteBufAllocator =
    NettyUtils.getPooledByteBufAllocator(new TransportConf("StorageManager", conf), null, true)
  // (mountPoint -> LocalFlusher)
  private val (
    localFlushers: ConcurrentHashMap[String, LocalFlusher],
    _totalLocalFlusherThread: Int) = {
    val flushers = JavaUtils.newConcurrentHashMap[String, LocalFlusher]()
    var totalThread = 0;
    disksSnapshot().foreach { diskInfo =>
      if (!flushers.containsKey(diskInfo.mountPoint)) {
        val flusher = new LocalFlusher(
          workerSource,
          deviceMonitor,
          diskInfo.threadCount,
          byteBufAllocator,
          conf.workerPushMaxComponents,
          diskInfo.mountPoint,
          diskInfo.storageType,
          diskInfo.flushTimeMetrics)
        flushers.put(diskInfo.mountPoint, flusher)
        totalThread = totalThread + diskInfo.threadCount
      }
    }
    (flushers, totalThread)
  }

  deviceMonitor.startCheck()

  val hdfsDir = conf.hdfsDir
  val hdfsPermission = new FsPermission("755")
  val hdfsWriters = JavaUtils.newConcurrentHashMap[String, FileWriter]()
  val (hdfsFlusher, _totalHdfsFlusherThread) =
    if (hasHDFSStorage) {
      logInfo(s"Initialize HDFS support with path ${hdfsDir}")
      StorageManager.hadoopFs = CelebornHadoopUtils.getHadoopFS(conf)
      (
        Some(new HdfsFlusher(
          workerSource,
          conf.workerHdfsFlusherThreads,
          byteBufAllocator,
          conf.workerPushMaxComponents)),
        conf.workerHdfsFlusherThreads)
    } else {
      (None, 0)
    }

  def totalFlusherThread: Int = _totalLocalFlusherThread + _totalHdfsFlusherThread

  override def notifyError(mountPoint: String, diskStatus: DiskStatus): Unit = this.synchronized {
    if (diskStatus == DiskStatus.CRITICAL_ERROR) {
      logInfo(s"Disk ${mountPoint} faces critical error, will remove its disk operator.")
      val operator = diskOperators.remove(mountPoint)
      if (operator != null) {
        operator.shutdown()
      }
    }
  }

  override def notifyHealthy(mountPoint: String): Unit = this.synchronized {
    if (!diskOperators.containsKey(mountPoint)) {
      diskOperators.put(
        mountPoint,
        ThreadUtils.newDaemonCachedThreadPool(s"Disk-cleaner-${mountPoint}", 1))
    }
  }

  private val counter = new AtomicInteger()
  private val counterOperator = new IntUnaryOperator() {
    override def applyAsInt(operand: Int): Int = {
      val dirs = healthyWorkingDirs()
      if (dirs.length > 0) {
        (operand + 1) % dirs.length
      } else 0
    }
  }

  // shuffleKey -> (fileName -> file info)
  private val fileInfos =
    JavaUtils.newConcurrentHashMap[String, ConcurrentHashMap[String, FileInfo]]()
  private val RECOVERY_FILE_NAME = "recovery.ldb"
  private var db: DB = null
  // ShuffleClient can fetch data from a restarted worker only
  // when the worker's fetching port is stable.
  if (conf.workerGracefulShutdown) {
    try {
      val recoverFile = new File(conf.workerGracefulShutdownRecoverPath, RECOVERY_FILE_NAME)
      this.db = LevelDBProvider.initLevelDB(recoverFile, CURRENT_VERSION)
      reloadAndCleanFileInfos(this.db)
    } catch {
      case e: Exception =>
        logError("Init level DB failed:", e)
        this.db = null
    }
  }
  cleanupExpiredAppDirs()
  if (!checkIfWorkingDirCleaned) {
    logWarning(
      "Worker still has residual files in the working directory before registering with Master, " +
        "please refer to the configuration document to increase " +
        s"${CelebornConf.WORKER_CHECK_FILE_CLEAN_TIMEOUT.key}.")
  } else {
    logInfo("Successfully remove all files under working directory.")
  }

  private def reloadAndCleanFileInfos(db: DB): Unit = {
    if (db != null) {
      val cache = JavaUtils.newConcurrentHashMap[String, UserIdentifier]()
      val itr = db.iterator
      itr.seek(SHUFFLE_KEY_PREFIX.getBytes(StandardCharsets.UTF_8))
      while (itr.hasNext) {
        val entry = itr.next
        val key = new String(entry.getKey, StandardCharsets.UTF_8)
        if (key.startsWith(SHUFFLE_KEY_PREFIX)) {
          val shuffleKey = parseDbShuffleKey(key)
          try {
            val files = PbSerDeUtils.fromPbFileInfoMap(entry.getValue, cache)
            logDebug(s"Reload DB: ${shuffleKey} -> ${files}")
            fileInfos.put(shuffleKey, files)
            db.delete(entry.getKey)
          } catch {
            case exception: Exception =>
              logError(s"Reload DB: ${shuffleKey} failed.", exception)
          }
        } else {
          return
        }
      }
    }
  }

  def updateFileInfosInDB(): Unit = {
    fileInfos.asScala.foreach { case (shuffleKey, files) =>
      try {
        db.put(dbShuffleKey(shuffleKey), PbSerDeUtils.toPbFileInfoMap(files))
        logDebug(s"Update FileInfos into DB: ${shuffleKey} -> ${files}")
      } catch {
        case exception: Exception =>
          logError(s"Update FileInfos into DB: ${shuffleKey} failed.", exception)
      }
    }
  }

  private def getNextIndex() = counter.getAndUpdate(counterOperator)

  private val newMapFunc =
    new java.util.function.Function[String, ConcurrentHashMap[String, FileInfo]]() {
      override def apply(key: String): ConcurrentHashMap[String, FileInfo] =
        JavaUtils.newConcurrentHashMap()
    }

  private val workingDirWriterListFunc =
    new java.util.function.Function[File, ConcurrentHashMap[String, FileWriter]]() {
      override def apply(t: File): ConcurrentHashMap[String, FileWriter] =
        JavaUtils.newConcurrentHashMap[String, FileWriter]()
    }

  @throws[IOException]
  def createWriter(
      appId: String,
      shuffleId: Int,
      location: PartitionLocation,
      splitThreshold: Long,
      splitMode: PartitionSplitMode,
      partitionType: PartitionType,
      rangeReadFilter: Boolean,
      userIdentifier: UserIdentifier): FileWriter = {
    if (healthyWorkingDirs().size <= 0 && !hasHDFSStorage) {
      throw new IOException("No available working dirs!")
    }

    val fileName = location.getFileName
    var retryCount = 0
    var exception: IOException = null
    val suggestedMountPoint = location.getStorageInfo.getMountPoint
    while (retryCount < conf.workerCreateWriterMaxAttempts) {
      val diskInfo = diskInfos.get(suggestedMountPoint)
      val dirs =
        if (diskInfo != null && diskInfo.status.equals(DiskStatus.HEALTHY)) {
          diskInfo.dirs
        } else {
          logDebug(s"Disk unavailable for $suggestedMountPoint, return all healthy" +
            s" working dirs. diskInfo $diskInfo")
          healthyWorkingDirs()
        }
      if (dirs.isEmpty && hdfsFlusher.isEmpty) {
        throw new IOException(s"No available disks! suggested mountPoint $suggestedMountPoint")
      }
      val shuffleKey = Utils.makeShuffleKey(appId, shuffleId)
      if (dirs.isEmpty) {
        val shuffleDir =
          new Path(new Path(hdfsDir, conf.workerWorkingDir), s"$appId/$shuffleId")
        FileSystem.mkdirs(StorageManager.hadoopFs, shuffleDir, hdfsPermission)
        val fileInfo =
          new FileInfo(new Path(shuffleDir, fileName).toString, userIdentifier, partitionType)
        val hdfsWriter = partitionType match {
          case PartitionType.MAP => new MapPartitionFileWriter(
              fileInfo,
              hdfsFlusher.get,
              workerSource,
              conf,
              deviceMonitor,
              splitThreshold,
              splitMode,
              rangeReadFilter)
          case PartitionType.REDUCE => new ReducePartitionFileWriter(
              fileInfo,
              hdfsFlusher.get,
              workerSource,
              conf,
              deviceMonitor,
              splitThreshold,
              splitMode,
              rangeReadFilter)
          case _ => throw new UnsupportedOperationException(s"Not support $partitionType yet")
        }

        fileInfos.computeIfAbsent(shuffleKey, newMapFunc).put(fileName, fileInfo)
        hdfsWriters.put(fileInfo.getFilePath, hdfsWriter)
        return hdfsWriter
      } else {
        val dir = dirs(getNextIndex() % dirs.size)
        val mountPoint = DeviceInfo.getMountPoint(dir.getAbsolutePath, mountPoints)
        val shuffleDir = new File(dir, s"$appId/$shuffleId")
        val file = new File(shuffleDir, fileName)
        try {
          shuffleDir.mkdirs()
          if (file.exists()) {
            throw new FileAlreadyExistsException(
              s"Shuffle data file ${file.getAbsolutePath} already exists.")
          } else {
            val createFileSuccess = file.createNewFile()
            if (!createFileSuccess) {
              throw new CelebornException(
                s"Create shuffle data file ${file.getAbsolutePath} failed!")
            }
          }
          val fileInfo = new FileInfo(file.getAbsolutePath, userIdentifier, partitionType)
          fileInfo.setMountPoint(mountPoint)
          val fileWriter = partitionType match {
            case PartitionType.MAP => new MapPartitionFileWriter(
                fileInfo,
                localFlushers.get(mountPoint),
                workerSource,
                conf,
                deviceMonitor,
                splitThreshold,
                splitMode,
                rangeReadFilter)
            case PartitionType.REDUCE => new ReducePartitionFileWriter(
                fileInfo,
                localFlushers.get(mountPoint),
                workerSource,
                conf,
                deviceMonitor,
                splitThreshold,
                splitMode,
                rangeReadFilter)
            case _ => throw new UnsupportedOperationException(s"Not support $partitionType yet")
          }
          deviceMonitor.registerFileWriter(fileWriter)
          val map = workingDirWriters.computeIfAbsent(dir, workingDirWriterListFunc)
          map.put(fileInfo.getFilePath, fileWriter)
          fileInfos.computeIfAbsent(shuffleKey, newMapFunc).put(fileName, fileInfo)
          location.getStorageInfo.setMountPoint(mountPoint)
          logDebug(s"location $location set disk hint to ${location.getStorageInfo} ")
          return fileWriter
        } catch {
          case fe: FileAlreadyExistsException =>
            logError("Failed to create fileWriter because of existed file", fe)
            throw fe
          case t: Throwable =>
            logError(
              s"Create FileWriter for ${file.getAbsolutePath} of mount $mountPoint " +
                s"failed, report to DeviceMonitor",
              t)
            exception = new IOException(t)
            deviceMonitor.reportNonCriticalError(
              mountPoint,
              exception,
              DiskStatus.READ_OR_WRITE_FAILURE)
        }
      }
      retryCount += 1
    }

    throw exception
  }

  def getFileInfo(shuffleKey: String, fileName: String): FileInfo = {
    val shuffleMap = fileInfos.get(shuffleKey)
    if (shuffleMap ne null) {
      shuffleMap.get(fileName)
    } else {
      null
    }
  }

  def getFetchTimeMetric(file: File): TimeWindow = {
    if (diskInfos != null) {
      val diskInfo = diskInfos.get(DeviceInfo.getMountPoint(file.getAbsolutePath, diskInfos))
      if (diskInfo != null) {
        diskInfo.fetchTimeMetrics
      } else null
    } else null
  }

  def shuffleKeySet(): util.HashSet[String] = {
    val hashSet = new util.HashSet[String]()
    hashSet.addAll(fileInfos.keySet())
    hashSet
  }

  def topAppDiskUsage: util.Map[String, Long] = {
    fileInfos.asScala.map { keyedWriters =>
      {
        keyedWriters._1 -> keyedWriters._2.values().asScala.map(_.getFileLength).sum
      }
    }.toList.map { case (shuffleKey, usage) =>
      shuffleKey.split("-")(0) -> usage
    }.groupBy(_._1).map { case (key, values) =>
      key -> values.map(_._2).sum
    }.toSeq.sortBy(_._2).reverse.take(conf.metricsAppTopDiskUsageCount * 2).toMap.asJava
  }

  def cleanFile(shuffleKey: String, fileName: String): Unit = {
    val fileInfo = getFileInfo(shuffleKey, fileName)
    if (fileInfo != null) {
      cleanFileInternal(shuffleKey, fileInfo)
    }
  }

  def cleanFileInternal(shuffleKey: String, fileInfo: FileInfo): Boolean = {
    var isHdfsExpired = false
    if (fileInfo.isHdfs) {
      isHdfsExpired = true
      val hdfsFileWriter = hdfsWriters.get(fileInfo.getFilePath)
      if (hdfsFileWriter != null) {
        hdfsFileWriter.destroy(new IOException(
          s"Destroy FileWriter ${hdfsFileWriter} caused by shuffle ${shuffleKey} expired."))
        hdfsWriters.remove(fileInfo.getFilePath)
      }
    } else {
      val workingDir =
        fileInfo.getFile.getParentFile.getParentFile.getParentFile
      val writers = workingDirWriters.get(workingDir)
      if (writers != null) {
        val fileWriter = writers.get(fileInfo.getFilePath)
        if (fileWriter != null) {
          fileWriter.destroy(new IOException(
            s"Destroy FileWriter ${fileWriter} caused by shuffle ${shuffleKey} expired."))
          writers.remove(fileInfo.getFilePath)
        }
      }
    }

    isHdfsExpired
  }

  def cleanupExpiredShuffleKey(expiredShuffleKeys: util.HashSet[String]): Unit = {
    expiredShuffleKeys.asScala.foreach { shuffleKey =>
      logInfo(s"Cleanup expired shuffle $shuffleKey.")
      if (fileInfos.containsKey(shuffleKey)) {
        val removedFileInfos = fileInfos.remove(shuffleKey)
        var isHdfsExpired = false
        if (removedFileInfos != null) {
          removedFileInfos.asScala.foreach {
            case (_, fileInfo) =>
              if (cleanFileInternal(shuffleKey, fileInfo)) {
                isHdfsExpired = true
              }
          }
        }
        val (appId, shuffleId) = Utils.splitShuffleKey(shuffleKey)
        disksSnapshot().filter(_.status != DiskStatus.IO_HANG).foreach { diskInfo =>
          diskInfo.dirs.foreach { dir =>
            val file = new File(dir, s"$appId/$shuffleId")
            deleteDirectory(file, diskOperators.get(diskInfo.mountPoint))
          }
        }
        if (isHdfsExpired) {
          try {
            StorageManager.hadoopFs.delete(
              new Path(new Path(hdfsDir, conf.workerWorkingDir), s"$appId/$shuffleId"),
              true)
          } catch {
            case e: Exception => logWarning("Clean expired HDFS shuffle failed.", e)
          }
        }
      }
    }
  }

  private val storageScheduler =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("storage-scheduler")

  storageScheduler.scheduleAtFixedRate(
    new Runnable {
      override def run(): Unit = {
        try {
          // Clean up dirs which it's application is expired.
          cleanupExpiredAppDirs()
        } catch {
          case exception: Exception =>
            logWarning(s"Cleanup expired shuffle data exception: ${exception.getMessage}")
        }
      }
    },
    0,
    30,
    TimeUnit.MINUTES)

  private def cleanupExpiredAppDirs(): Unit = {
    val appIds = shuffleKeySet().asScala.map(key => Utils.splitShuffleKey(key)._1)
    disksSnapshot().filter(_.status != DiskStatus.IO_HANG).foreach { diskInfo =>
      diskInfo.dirs.foreach {
        case workingDir if workingDir.exists() =>
          workingDir.listFiles().foreach { appDir =>
            // Don't delete shuffleKey's data that exist correct shuffle file info.
            if (!appIds.contains(appDir.getName)) {
              val threadPool = diskOperators.get(diskInfo.mountPoint)
              deleteDirectory(appDir, threadPool)
              logInfo(s"Delete expired app dir $appDir.")
            }
          }
        // workingDir not exist when initializing worker on new disk
        case _ => // do nothing
      }
    }
  }

  private def deleteDirectory(dir: File, threadPool: ThreadPoolExecutor): Unit = {
    val allContents = dir.listFiles
    if (allContents != null) {
      for (file <- allContents) {
        deleteDirectory(file, threadPool)
      }
    }
    threadPool.submit(new Runnable {
      override def run(): Unit = {
        deleteFileWithRetry(dir)
      }
    })
  }

  private def deleteFileWithRetry(file: File): Unit = {
    if (file.exists()) {
      var retryCount = 0
      var deleteSuccess = false
      while (!deleteSuccess && retryCount <= 3) {
        deleteSuccess = file.delete()
        retryCount = retryCount + 1
        if (!deleteSuccess) {
          Thread.sleep(200 * retryCount)
        }
      }
      if (deleteSuccess) {
        logDebug(s"Deleted expired shuffle file $file.")
      } else {
        logWarning(s"Failed to delete expired shuffle file $file.")
      }
    }
  }

  private def checkIfWorkingDirCleaned: Boolean = {
    var retryTimes = 0
    val workerCheckFileCleanTimeout = conf.workerCheckFileCleanTimeout
    val appIds = shuffleKeySet().asScala.map(key => Utils.splitShuffleKey(key)._1)
    while (retryTimes < conf.workerCheckFileCleanMaxRetries) {
      val localCleaned =
        !disksSnapshot().filter(_.status != DiskStatus.IO_HANG).exists { diskInfo =>
          diskInfo.dirs.exists {
            case workingDir if workingDir.exists() =>
              // Don't check appDirs that store information in the fileInfos
              workingDir.listFiles().exists(appDir => !appIds.contains(appDir.getName))
            case _ =>
              false
          }
        }

      val hdfsCleaned = hadoopFs match {
        case hdfs: FileSystem =>
          val hdfsWorkPath = new Path(hdfsDir, conf.workerWorkingDir)
          // HDFS path not exist when first time initialize
          if (hdfs.exists(hdfsWorkPath)) {
            !hdfs.listFiles(hdfsWorkPath, false).hasNext
          } else {
            true
          }
        case _ =>
          true
      }

      if (localCleaned && hdfsCleaned) {
        return true
      }
      retryTimes += 1
      if (retryTimes < conf.workerCheckFileCleanMaxRetries) {
        logInfo(s"Working directory's files have not been cleaned up completely, " +
          s"will start ${retryTimes + 1}th attempt after ${workerCheckFileCleanTimeout} milliseconds.")
      }
      Thread.sleep(workerCheckFileCleanTimeout)
    }
    false
  }

  def close(): Unit = {
    if (db != null) {
      try {
        updateFileInfosInDB()
        db.close()
      } catch {
        case exception: Exception =>
          logError("Store recover data to LevelDB failed.", exception)
      }
    }
    if (null != diskOperators) {
      if (!conf.workerGracefulShutdown) {
        cleanupExpiredShuffleKey(shuffleKeySet())
      }
      ThreadUtils.parmap(
        diskOperators.asScala.toMap,
        "ShutdownDiskOperators",
        diskOperators.size()) { entry =>
        ThreadUtils.shutdown(
          entry._2,
          conf.workerGracefulShutdownFlusherShutdownTimeoutMs.milliseconds)
      }
    }
    storageScheduler.shutdownNow()
    if (null != deviceMonitor) {
      deviceMonitor.close()
    }
  }

  private def flushFileWriters(): Unit = {
    workingDirWriters.forEach(new BiConsumer[File, ConcurrentHashMap[String, FileWriter]] {
      override def accept(t: File, writers: ConcurrentHashMap[String, FileWriter]): Unit = {
        writers.forEach(new BiConsumer[String, FileWriter] {
          override def accept(file: String, writer: FileWriter): Unit = {
            if (writer.getException == null) {
              try {
                writer.flushOnMemoryPressure()
              } catch {
                case t: Throwable =>
                  logError(
                    s"FileWrite of $writer faces unexpected exception when flush on memory pressure.",
                    t)
              }
            } else {
              logWarning(s"Skip flushOnMemoryPressure because ${writer.flusher} " +
                s"has error: ${writer.getException.getMessage}")
            }
          }
        })
      }
    })
    hdfsWriters.forEach(new BiConsumer[String, FileWriter] {
      override def accept(t: String, u: FileWriter): Unit = {
        u.flushOnMemoryPressure();
      }
    })
  }

  override def onPause(moduleName: String): Unit = {}

  override def onResume(moduleName: String): Unit = {}

  override def onTrim(): Unit = {
    logInfo(s"Trigger ${this.getClass.getCanonicalName} trim action")
    flushFileWriters()
    try {
      Thread.sleep(conf.workerDirectMemoryTrimFlushWaitInterval)
    } catch {
      case _: Exception => // Do nothing
    }
  }

  def updateDiskInfos(): Unit = this.synchronized {
    disksSnapshot().filter(_.status != DiskStatus.IO_HANG).foreach { diskInfo =>
      val totalUsage = diskInfo.dirs.map { dir =>
        val writers = workingDirWriters.get(dir)
        if (writers != null) {
          writers.synchronized {
            writers.values.asScala.map(_.getFileInfo.getFileLength).sum
          }
        } else {
          0
        }
      }.sum
      val fileSystemReportedUsableSpace = Files.getFileStore(
        Paths.get(diskInfo.mountPoint)).getUsableSpace
      val workingDirUsableSpace =
        Math.min(diskInfo.configuredUsableSpace - totalUsage, fileSystemReportedUsableSpace)
      logDebug(s"updateDiskInfos  workingDirUsableSpace:$workingDirUsableSpace filemeta:$fileSystemReportedUsableSpace conf:${diskInfo.configuredUsableSpace} totalUsage:$totalUsage")
      diskInfo.setUsableSpace(workingDirUsableSpace)
      diskInfo.updateFlushTime()
      diskInfo.updateFetchTime()
    }
    logInfo(s"Updated diskInfos:\n${disksSnapshot().mkString("\n")}")
  }

  def userResourceConsumptionSnapshot(): Map[UserIdentifier, ResourceConsumption] = {
    fileInfos.synchronized {
      // shuffleId -> (fileName -> fileInfo)
      fileInfos
        .asScala
        .toList
        .flatMap { case (_, fileInfoMaps) =>
          // userIdentifier -> fileInfo
          fileInfoMaps.values().asScala.map { fileInfo =>
            (fileInfo.getUserIdentifier, fileInfo)
          }
        }
        // userIdentifier -> List((userIdentifier, fileInfo))
        .groupBy(_._1)
        .map { case (userIdentifier, userWithFileInfoList) =>
          // collect resource consumed by each user on this worker
          val resourceConsumption = {
            val userFileInfos = userWithFileInfoList.map(_._2)
            val diskFileInfos = userFileInfos.filter(!_.isHdfs)
            val hdfsFileInfos = userFileInfos.filter(_.isHdfs)

            val diskBytesWritten = diskFileInfos.map(_.getFileLength).sum
            val diskFileCount = diskFileInfos.size
            val hdfsBytesWritten = hdfsFileInfos.map(_.getFileLength).sum
            val hdfsFileCount = hdfsFileInfos.size
            ResourceConsumption(diskBytesWritten, diskFileCount, hdfsBytesWritten, hdfsFileCount)
          }
          (userIdentifier, resourceConsumption)
        }
    }
  }
}

object StorageManager {
  var hadoopFs: FileSystem = _
}

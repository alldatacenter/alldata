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

package org.apache.griffin.measure.datasource.cache

import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.util.Random

import org.apache.spark.sql._

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.context.streaming.checkpoint.lock.CheckpointLock
import org.apache.griffin.measure.context.streaming.checkpoint.offset.OffsetCheckpointClient
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.utils.{HdfsUtil, TimeUtil}
import org.apache.griffin.measure.utils.DataFrameUtil._
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * data source cache in streaming mode
 * save data frame into hdfs in dump phase
 * read data frame from hdfs in calculate phase
 * with update and clean actions for the cache data
 */
trait StreamingCacheClient
    extends StreamingOffsetCacheable
    with WithFanIn[Long]
    with Loggable
    with Serializable {

  val sparkSession: SparkSession
  val param: Map[String, Any]
  val dsName: String
  val index: Int

  val timestampStorage: TimestampStorage
  protected def fromUntilRangeTmsts(from: Long, until: Long): Set[Long] =
    timestampStorage.fromUntil(from, until)

  protected def clearTmst(t: Long): mutable.SortedSet[Long] = timestampStorage.remove(t)
  protected def clearTmstsUntil(until: Long): mutable.SortedSet[Long] = {
    val outDateTmsts = timestampStorage.until(until)
    timestampStorage.remove(outDateTmsts)
  }
  protected def afterTilRangeTmsts(after: Long, til: Long): Set[Long] =
    fromUntilRangeTmsts(after + 1, til + 1)
  protected def clearTmstsTil(til: Long): mutable.SortedSet[Long] = clearTmstsUntil(til + 1)

  val _FilePath = "file.path"
  val _InfoPath = "info.path"
  val _ReadyTimeInterval = "ready.time.interval"
  val _ReadyTimeDelay = "ready.time.delay"
  val _TimeRange = "time.range"

  val rdmStr: String = Random.alphanumeric.take(10).mkString
  val defFilePath = s"hdfs:///griffin/cache/${dsName}_$rdmStr"
  val defInfoPath = s"$index"

  val filePath: String = param.getString(_FilePath, defFilePath)
  val cacheInfoPath: String = param.getString(_InfoPath, defInfoPath)
  val readyTimeInterval: Long =
    TimeUtil.milliseconds(param.getString(_ReadyTimeInterval, "1m")).getOrElse(60000L)

  val readyTimeDelay: Long =
    TimeUtil.milliseconds(param.getString(_ReadyTimeDelay, "1m")).getOrElse(60000L)

  def deltaTimeRange[T <: Seq[String]: Manifest]: (Long, Long) = {
    def negative(n: Long): Long = if (n <= 0) n else 0
    param.get(_TimeRange) match {
      case Some(seq: T) =>
        val nseq = seq.flatMap(TimeUtil.milliseconds)
        val ns = negative(nseq.headOption.getOrElse(0))
        val ne = negative(nseq.tail.headOption.getOrElse(0))
        (ns, ne)
      case _ => (0, 0)
    }
  }

  val _ReadOnly = "read.only"
  val readOnly: Boolean = param.getBoolean(_ReadOnly, defValue = false)

  val _Updatable = "updatable"
  val updatable: Boolean = param.getBoolean(_Updatable, defValue = false)

  val newCacheLock: CheckpointLock = OffsetCheckpointClient.genLock(s"$cacheInfoPath.new")
  val oldCacheLock: CheckpointLock = OffsetCheckpointClient.genLock(s"$cacheInfoPath.old")

  val newFilePath = s"$filePath/new"
  val oldFilePath = s"$filePath/old"

  val defOldCacheIndex = 0L

  protected def writeDataFrame(dfw: DataFrameWriter[Row], path: String): Unit
  protected def readDataFrame(dfr: DataFrameReader, path: String): DataFrame

  private def readDataFrameOpt(dfr: DataFrameReader, path: String): Option[DataFrame] = {
    val df = readDataFrame(dfr, path)
    if (df.count() > 0) Some(df) else None
  }

  /**
   * save data frame in dump phase
   * @param dfOpt    data frame to be saved
   * @param ms       timestamp of this data frame
   */
  def saveData(dfOpt: Option[DataFrame], ms: Long): Unit = {
    if (!readOnly) {
      dfOpt match {
        case Some(df) =>
          // cache df
          df.cache

          // cache df
          val cnt = df.count
          info(s"save $dsName data count: $cnt")

          if (cnt > 0) {
            // lock makes it safer when writing new cache data
            val newCacheLocked = newCacheLock.lock(-1, TimeUnit.SECONDS)
            if (newCacheLocked) {
              try {
                val dfw = df.write.mode(SaveMode.Append).partitionBy(ConstantColumns.tmst)
                writeDataFrame(dfw, newFilePath)
              } catch {
                case e: Throwable => error(s"save data error: ${e.getMessage}")
              } finally {
                newCacheLock.unlock()
              }
            }
          }

          // uncache df
          df.unpersist
        case _ =>
          info("no data frame to save")
      }

      // submit cache time and ready time
      if (fanIncrement(ms)) {
        info(s"save data [$ms] finish")
        submitCacheTime(ms)
        submitReadyTime(ms)
      }

    }
  }

  /**
   * read data frame in calculation phase
   * @return   data frame to calculate, with the time range of data
   */
  def readData(): (Option[DataFrame], TimeRange) = {
    // time range: (a, b]
    val timeRange = OffsetCheckpointClient.getTimeRange
    val reviseTimeRange = (timeRange._1 + deltaTimeRange._1, timeRange._2 + deltaTimeRange._2)

    // read partition info
    val filterStr = if (reviseTimeRange._1 == reviseTimeRange._2) {
      info(s"read time range: [${reviseTimeRange._1}]")
      s"`${ConstantColumns.tmst}` = ${reviseTimeRange._1}"
    } else {
      info(s"read time range: (${reviseTimeRange._1}, ${reviseTimeRange._2}]")

      s"`${ConstantColumns.tmst}` > ${reviseTimeRange._1} " +
        s"AND `${ConstantColumns.tmst}` <= ${reviseTimeRange._2}"
    }

    // new cache data
    val newDfOpt =
      try {
        val dfr = sparkSession.read
        readDataFrameOpt(dfr, newFilePath).map(_.filter(filterStr))
      } catch {
        case e: Throwable =>
          warn(s"read data source cache warn: ${e.getMessage}")
          None
      }

    // old cache data
    val oldCacheIndexOpt = if (updatable) readOldCacheIndex() else None
    val oldDfOpt = oldCacheIndexOpt.flatMap { idx =>
      val oldDfPath = s"$oldFilePath/$idx"
      try {
        val dfr = sparkSession.read
        readDataFrameOpt(dfr, oldDfPath).map(_.filter(filterStr))
      } catch {
        case e: Throwable =>
          warn(s"read old data source cache warn: ${e.getMessage}")
          None
      }
    }

    // whole cache data
    val cacheDfOpt = unionDfOpts(newDfOpt, oldDfOpt)

    // from until tmst range
    val (from, until) = (reviseTimeRange._1, reviseTimeRange._2)
    val tmstSet = afterTilRangeTmsts(from, until)

    val retTimeRange = TimeRange(reviseTimeRange, tmstSet)
    (cacheDfOpt, retTimeRange)
  }

  private def cleanOutTimePartitions(
      path: String,
      outTime: Long,
      partitionOpt: Option[String],
      func: (Long, Long) => Boolean): Unit = {
    val earlierOrEqPaths = listPartitionsByFunc(path: String, outTime, partitionOpt, func)
    // delete out time data path
    earlierOrEqPaths.foreach { path =>
      info(s"delete hdfs path: $path")
      HdfsUtil.deleteHdfsPath(path)
    }
  }
  private def listPartitionsByFunc(
      path: String,
      bound: Long,
      partitionOpt: Option[String],
      func: (Long, Long) => Boolean): Iterable[String] = {
    val names = HdfsUtil.listSubPathsByType(path, "dir")
    val regex = partitionOpt match {
      case Some(partition) => s"^$partition=(\\d+)$$".r
      case _ => "^(\\d+)$".r
    }
    names
      .filter { name =>
        name match {
          case regex(value) =>
            str2Long(value) match {
              case Some(t) => func(t, bound)
              case _ => false
            }
          case _ => false
        }
      }
      .map(name => s"$path/$name")
  }
  private def str2Long(str: String): Option[Long] = {
    try {
      Some(str.toLong)
    } catch {
      case _: Throwable => None
    }
  }

  /**
   * clean out-time cached data on hdfs
   */
  def cleanOutTimeData(): Unit = {
    // clean tmst
    val cleanTime = readCleanTime()
    cleanTime.foreach(clearTmstsTil)

    if (!readOnly) {
      // new cache data
      val newCacheCleanTime = if (updatable) readLastProcTime() else readCleanTime()
      newCacheCleanTime match {
        case Some(nct) =>
          // clean calculated new cache data
          val newCacheLocked = newCacheLock.lock(-1, TimeUnit.SECONDS)
          if (newCacheLocked) {
            try {
              cleanOutTimePartitions(
                newFilePath,
                nct,
                Some(ConstantColumns.tmst),
                (a: Long, b: Long) => a <= b)
            } catch {
              case e: Throwable => error(s"clean new cache data error: ${e.getMessage}")
            } finally {
              newCacheLock.unlock()
            }
          }
        case _ =>
          // do nothing
          info("should not happen")
      }

      // old cache data
      val oldCacheCleanTime = if (updatable) readCleanTime() else None
      oldCacheCleanTime match {
        case Some(_) =>
          val oldCacheIndexOpt = readOldCacheIndex()
          oldCacheIndexOpt.foreach { idx =>
            val oldCacheLocked = oldCacheLock.lock(-1, TimeUnit.SECONDS)
            if (oldCacheLocked) {
              try {
                // clean calculated old cache data
                cleanOutTimePartitions(oldFilePath, idx, None, (a: Long, b: Long) => a < b)
                // clean out time old cache data not calculated
//                cleanOutTimePartitions(oldDfPath, oct, Some(InternalColumns.tmst))
              } catch {
                case e: Throwable => error(s"clean old cache data error: ${e.getMessage}")
              } finally {
                oldCacheLock.unlock()
              }
            }
          }
        case _ =>
          // do nothing
          info("should not happen")
      }
    }
  }

  /**
   * update old cached data by new data frame
   * @param dfOpt    data frame to update old cached data
   */
  def updateData(dfOpt: Option[DataFrame]): Unit = {
    if (!readOnly && updatable) {
      dfOpt match {
        case Some(df) =>
          // old cache lock
          val oldCacheLocked = oldCacheLock.lock(-1, TimeUnit.SECONDS)
          if (oldCacheLocked) {
            try {
              val oldCacheIndexOpt = readOldCacheIndex()
              val nextOldCacheIndex = oldCacheIndexOpt.getOrElse(defOldCacheIndex) + 1

              val oldDfPath = s"$oldFilePath/$nextOldCacheIndex"
              val cleanTime = getNextCleanTime
              val filterStr = s"`${ConstantColumns.tmst}` > $cleanTime"
              val updateDf = df.filter(filterStr)

              val prlCount = sparkSession.sparkContext.defaultParallelism
              // repartition
              val repartitionedDf = updateDf.repartition(prlCount)
              val dfw = repartitionedDf.write.mode(SaveMode.Overwrite)
              writeDataFrame(dfw, oldDfPath)

              submitOldCacheIndex(nextOldCacheIndex)
            } catch {
              case e: Throwable => error(s"update data error: ${e.getMessage}")
            } finally {
              oldCacheLock.unlock()
            }
          }
        case _ =>
          info("no data frame to update")
      }
    }
  }

  /**
   * each time calculation phase finishes,
   * data source cache needs to submit some cache information
   */
  def processFinish(): Unit = {
    // next last proc time
    val timeRange = OffsetCheckpointClient.getTimeRange
    submitLastProcTime(timeRange._2)

    // next clean time
    val nextCleanTime = timeRange._2 + deltaTimeRange._1
    submitCleanTime(nextCleanTime)
  }

  // read next clean time
  private def getNextCleanTime: Long = {
    val timeRange = OffsetCheckpointClient.getTimeRange
    val nextCleanTime = timeRange._2 + deltaTimeRange._1
    nextCleanTime
  }

}

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

package org.apache.griffin.measure.context.streaming.checkpoint.offset

trait OffsetOps extends Serializable { this: OffsetCheckpoint =>

  val CacheTime = "cache.time"
  val LastProcTime = "last.proc.time"
  val ReadyTime = "ready.time"
  val CleanTime = "clean.time"
  val OldCacheIndex = "old.cache.index"

  def cacheTime(path: String): String = s"$path/$CacheTime"
  def lastProcTime(path: String): String = s"$path/$LastProcTime"
  def readyTime(path: String): String = s"$path/$ReadyTime"
  def cleanTime(path: String): String = s"$path/$CleanTime"
  def oldCacheIndex(path: String): String = s"$path/$OldCacheIndex"

  val infoPath = "info"

  val finalCacheInfoPath = "info.final"
  val finalReadyTime = s"$finalCacheInfoPath/$ReadyTime"
  val finalLastProcTime = s"$finalCacheInfoPath/$LastProcTime"
  val finalCleanTime = s"$finalCacheInfoPath/$CleanTime"

  def startOffsetCheckpoint(): Unit = {
    genFinalReadyTime()
  }

  def getTimeRange: (Long, Long) = {
    readTimeRange()
  }

  def getCleanTime: Long = {
    readCleanTime()
  }

  def endOffsetCheckpoint(): Unit = {
    genFinalLastProcTime()
    genFinalCleanTime()
  }

  private def genFinalReadyTime(): Unit = {
    val subPath = listKeys(infoPath)
    val keys = subPath.map { p =>
      s"$infoPath/$p/$ReadyTime"
    }
    val result = read(keys)
    val times = keys.flatMap { k =>
      getLongOpt(result, k)
    }
    if (times.nonEmpty) {
      val time = times.min
      val map = Map[String, String](finalReadyTime -> time.toString)
      cache(map)
    }
  }

  private def genFinalLastProcTime(): Unit = {
    val subPath = listKeys(infoPath)
    val keys = subPath.map { p =>
      s"$infoPath/$p/$LastProcTime"
    }
    val result = read(keys)
    val times = keys.flatMap { k =>
      getLongOpt(result, k)
    }
    if (times.nonEmpty) {
      val time = times.min
      val map = Map[String, String](finalLastProcTime -> time.toString)
      cache(map)
    }
  }

  private def genFinalCleanTime(): Unit = {
    val subPath = listKeys(infoPath)
    val keys = subPath.map { p =>
      s"$infoPath/$p/$CleanTime"
    }
    val result = read(keys)
    val times = keys.flatMap { k =>
      getLongOpt(result, k)
    }
    if (times.nonEmpty) {
      val time = times.min
      val map = Map[String, String](finalCleanTime -> time.toString)
      cache(map)
    }
  }

  private def readTimeRange(): (Long, Long) = {
    val map = read(List(finalLastProcTime, finalReadyTime))
    val lastProcTime = getLong(map, finalLastProcTime)
    val curReadyTime = getLong(map, finalReadyTime)
    (lastProcTime, curReadyTime)
  }

  private def readCleanTime(): Long = {
    val map = read(List(finalCleanTime))
    val cleanTime = getLong(map, finalCleanTime)
    cleanTime
  }

  private def getLongOpt(map: Map[String, String], key: String): Option[Long] = {
    try {
      map.get(key).map(_.toLong)
    } catch {
      case _: Throwable => None
    }
  }
  private def getLong(map: Map[String, String], key: String) = {
    getLongOpt(map, key).getOrElse(-1L)
  }

}

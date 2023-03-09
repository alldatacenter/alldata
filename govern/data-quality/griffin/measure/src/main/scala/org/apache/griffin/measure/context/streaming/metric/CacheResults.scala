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

package org.apache.griffin.measure.context.streaming.metric

import scala.collection.mutable.{Map => MutableMap}

import org.apache.griffin.measure.Loggable

/**
 * in streaming mode, some metrics may update,
 * the old metrics are cached here
 */
object CacheResults extends Loggable {

  case class CacheResult(timeStamp: Long, updateTime: Long, result: Metric) {
    def olderThan(ut: Long): Boolean = updateTime < ut
    def update[A <: result.T: Manifest](ut: Long, r: Metric): Option[Metric] = {
      r match {
        case m: A if olderThan(ut) =>
          val ur = result.update(m)
          Some(ur).filter(result.differsFrom)
        case _ => None
      }
    }
  }

  private val cacheGroup: MutableMap[Long, CacheResult] = MutableMap()

  private def update(r: CacheResult): Unit = {
    cacheGroup += (r.timeStamp -> r)
  }

  /**
   * input new metric results, output the updated metric results.
   */
  def update(cacheResults: Iterable[CacheResult]): Iterable[CacheResult] = {
    val updatedCacheResults = cacheResults.flatMap { cacheResult =>
      val CacheResult(t, ut, r) = cacheResult
      (cacheGroup.get(t) match {
        case Some(cr) => cr.update(ut, r)
        case _ => Some(r)
      }).map(m => CacheResult(t, ut, m))
    }
    updatedCacheResults.foreach(r => update(r))
    updatedCacheResults
  }

  /**
   * clean the out-time cached results, to avoid memory leak
   */
  def refresh(overtime: Long): Unit = {
    val curCacheGroup = cacheGroup.toMap
    val deadCache = curCacheGroup.filter { pr =>
      val (_, cr) = pr
      cr.timeStamp < overtime || cr.result.eventual()
    }
    info(s"=== dead cache group count: ${deadCache.size} ===")
    deadCache.keySet.foreach(cacheGroup -= _)
  }

}

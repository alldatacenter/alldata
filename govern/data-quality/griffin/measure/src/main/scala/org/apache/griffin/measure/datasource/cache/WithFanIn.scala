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

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.concurrent.{Map => ConcMap, TrieMap}

/**
 * fan in trait, for multiple input and one output
 * to support multiple parallel data connectors in one data source
 */
trait WithFanIn[T] {

  // total input number
  val totalNum: AtomicInteger = new AtomicInteger(0)
  // concurrent map of fan in count for each key
  val fanInCountMap: ConcMap[T, Int] = TrieMap[T, Int]()

  def registerFanIn(): Int = {
    totalNum.incrementAndGet()
  }

  /**
   * increment for a key, to test if all parallel inputs finished
   * @param key
   * @return
   */
  def fanIncrement(key: T): Boolean = {
    fanInc(key)
    fanInCountMap.get(key) match {
      case Some(n) if n >= totalNum.get =>
        fanInCountMap.remove(key)
        true
      case _ => false
    }
  }

  @scala.annotation.tailrec
  private def fanInc(key: T): Unit = {
    fanInCountMap.get(key) match {
      case Some(n) =>
        val suc = fanInCountMap.replace(key, n, n + 1)
        if (!suc) fanInc(key)
      case _ =>
        val oldOpt = fanInCountMap.putIfAbsent(key, 1)
        if (oldOpt.nonEmpty) fanInc(key)
    }
  }

}

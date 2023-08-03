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

package org.apache.celeborn.common.meta

import java.util.concurrent.atomic.LongAdder

class TimeWindow(windowSize: Int, minWindowCount: Int) {
  val totalCount = new LongAdder
  val totalTime = new LongAdder
  val timeWindow = new Array[(Long, Long)](windowSize)
  var index = 0

  for (i <- 0 until windowSize) {
    timeWindow(i) = (0L, 0L)
  }

  def update(delta: Long): Unit = {
    totalTime.add(delta)
    totalCount.increment()
  }

  def getAverage(): Long = {
    val currentTime = totalTime.sumThenReset()
    val currentCount = totalCount.sumThenReset()

    if (currentCount >= minWindowCount) {
      timeWindow(index) = (currentTime, currentCount)
      index = (index + 1) % windowSize
    }

    var time = 0L
    var count = 0L
    timeWindow.foreach { case (flushTime, flushCount) =>
      time = time + flushTime
      count = count + flushCount
    }

    if (count != 0) {
      time / count
    } else {
      0L
    }
  }
}

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

package org.apache.celeborn.common.metrics

import java.util

import com.codahale.metrics.{Reservoir, Snapshot, UniformSnapshot}

class ResettableSlidingWindowReservoir(size: Int) extends Reservoir {
  var measurements: Array[Long] = new Array[Long](size)
  var index: Int = 0
  var full = false

  def size(): Int = this.synchronized {
    if (!full) {
      Math.min(index, size)
    } else {
      size
    }
  }

  def update(value: Long): Unit = this.synchronized {
    measurements(index) = value
    index += 1
    if (index >= size) {
      index = 0
      if (!full) {
        full = true
      }
    }
  }

  override def getSnapshot: Snapshot = {
    val values = new Array[Long](size())
    this.synchronized {
      0 until values.length foreach (idx => values(idx) = measurements(idx))
    }
    new UniformSnapshot(values)
  }

  def reset(): Unit = this.synchronized {
    util.Arrays.fill(measurements, 0)
  }
}

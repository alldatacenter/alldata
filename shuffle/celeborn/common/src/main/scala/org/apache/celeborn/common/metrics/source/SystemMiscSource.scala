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

package org.apache.celeborn.common.metrics.source

import java.lang.management.ManagementFactory

import com.codahale.metrics.Gauge

import org.apache.celeborn.common.CelebornConf

class SystemMiscSource(conf: CelebornConf, role: String) extends AbstractSource(conf, role) {
  override val sourceName = "system"

  import SystemMiscSource._

  addGauge(
    SYSTEM_LOAD,
    new Gauge[Double] {
      override def getValue: Double = {
        ManagementFactory.getOperatingSystemMXBean.getSystemLoadAverage
      }
    })

  addGauge(
    AVAILABLE_PROCESSORS,
    new Gauge[Int] {
      override def getValue: Int = {
        ManagementFactory.getOperatingSystemMXBean.getAvailableProcessors
      }
    })
  // start cleaner
  startCleaner()
}

object SystemMiscSource {
  val SYSTEM_LOAD = "LastMinuteSystemLoad"
  val AVAILABLE_PROCESSORS = "AvailableProcessors"
}

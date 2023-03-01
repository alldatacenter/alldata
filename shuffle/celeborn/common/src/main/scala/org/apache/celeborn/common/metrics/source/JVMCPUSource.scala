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
import javax.management.{MBeanServer, ObjectName}

import scala.util.control.NonFatal

import com.codahale.metrics.Gauge

import org.apache.celeborn.common.CelebornConf

class JVMCPUSource(conf: CelebornConf, role: String) extends AbstractSource(conf, role) {
  override val sourceName = "CPU"

  import JVMCPUSource._

  addGauge(
    JVMCPUTime,
    new Gauge[Long] {
      val mBean: MBeanServer = ManagementFactory.getPlatformMBeanServer
      val name = new ObjectName("java.lang", "type", "OperatingSystem")

      override def getValue: Long = {
        try {
          // return JVM process CPU time if the ProcessCpuTime method is available
          mBean.getAttribute(name, "ProcessCpuTime").asInstanceOf[Long]
        } catch {
          case NonFatal(_) => -1L
        }
      }
    })
  // start cleaner
  startCleaner()
}

object JVMCPUSource {
  val JVMCPUTime = "JVMCPUTime"
}

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

package org.apache.celeborn.service.deploy.master

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.metrics.MetricsSystem
import org.apache.celeborn.common.metrics.source.AbstractSource
import org.apache.celeborn.service.deploy.master.MasterSource.OfferSlotsTime

class MasterSource(conf: CelebornConf)
  extends AbstractSource(conf, MetricsSystem.ROLE_MASTER) with Logging {
  override val sourceName = s"master"

  addTimer(OfferSlotsTime)
  // start cleaner
  startCleaner()
}

object MasterSource {
  val ServletPath = "/metrics/prometheus"

  val WorkerCount = "WorkerCount"

  val BlacklistedWorkerCount = "BlacklistedWorkerCount"

  val RegisteredShuffleCount = "RegisteredShuffleCount"

  val IsActiveMaster = "IsActiveMaster"

  val PartitionSize = "PartitionSize"

  val OfferSlotsTime = "OfferSlotsTime"
}

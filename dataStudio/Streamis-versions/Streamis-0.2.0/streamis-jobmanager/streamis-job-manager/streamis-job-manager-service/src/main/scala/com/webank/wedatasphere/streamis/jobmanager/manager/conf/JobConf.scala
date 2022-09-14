/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.conf

import org.apache.linkis.common.conf.{CommonVars, TimeType}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException


object JobConf {

  val STREAMIS_DEVELOPER: CommonVars[String] = CommonVars("wds.streamis.developer", "enjoyyin,davidhua")

  val STREAMIS_DEFAULT_TENANT: CommonVars[String] = CommonVars("wds.streamis.job.tenant.default", "")

  val STREAMIS_JOB_MONITOR_ENABLE: CommonVars[Boolean] = CommonVars("wds.streamis.job.monitor.enable", true)

  val FLINK_JOB_STATUS_NOT_STARTED: CommonVars[Int] = CommonVars("wds.streamis.job.status.not-started", 0,"Not Started")

  val FLINK_JOB_STATUS_COMPLETED: CommonVars[Int] = CommonVars("wds.streamis.job.status.completed", 1,"Completed")

  val FLINK_JOB_STATUS_WAIT_RESTART: CommonVars[Int] = CommonVars("wds.streamis.job.status.wait-restart", 2,"Wait for restart")

  val FLINK_JOB_STATUS_ALERT_RUNNING: CommonVars[Int] = CommonVars("wds.streamis.job.status.alert-running", 3,"Alert running")

  val FLINK_JOB_STATUS_SLOW_RUNNING: CommonVars[Int] = CommonVars("wds.streamis.job.status.slow-running", 4,"Slow running")

  val FLINK_JOB_STATUS_RUNNING: CommonVars[Int] = CommonVars("wds.streamis.job.status.running", 5,"running")

  val FLINK_JOB_STATUS_FAILED: CommonVars[Int] = CommonVars("wds.streamis.job.status.failed", 6,"Failed")

  val FLINK_JOB_STATUS_STOPPED: CommonVars[Int] = CommonVars("wds.streamis.job.status.stopped", 7,"Stopped")

  /**
   * Starting (middle status, before scheduling)
   */
  val FLINK_JOB_STATUS_STARTING: CommonVars[Int] = CommonVars("wds.streamis.job.status.starting", 8, "Starting")

  /**
   * Stopping (middle status, before scheduling)
   */
  val FLINK_JOB_STATUS_STOPPING: CommonVars[Int] = CommonVars("wds.streamis.job.status.stopping", 9, "Stopping")

  val STATUS_ARRAY: Array[CommonVars[Int]] = Array(FLINK_JOB_STATUS_COMPLETED, FLINK_JOB_STATUS_WAIT_RESTART, FLINK_JOB_STATUS_ALERT_RUNNING,
    FLINK_JOB_STATUS_SLOW_RUNNING, FLINK_JOB_STATUS_RUNNING, FLINK_JOB_STATUS_FAILED, FLINK_JOB_STATUS_STOPPED, FLINK_JOB_STATUS_STARTING, FLINK_JOB_STATUS_STOPPING)

  val NOT_COMPLETED_STATUS_ARRAY: Array[CommonVars[Int]] = Array(FLINK_JOB_STATUS_WAIT_RESTART, FLINK_JOB_STATUS_ALERT_RUNNING, FLINK_JOB_STATUS_SLOW_RUNNING,
    FLINK_JOB_STATUS_RUNNING)

  def isCompleted(status: Int): Boolean = status match {
    case 1 | 6 | 7 => true
    case _ => false
  }

  def isRunning(status: Int): Boolean = status match {
    case 2 | 3 | 4 | 5 => true
    case _ => false
  }

  def linkisStatusToStreamisStatus(status: String): Int = status.toLowerCase match {
    case "starting" | "unlock" | "locked" | "idle" | "busy" | "running" => FLINK_JOB_STATUS_RUNNING.getValue
    case "success" => FLINK_JOB_STATUS_COMPLETED.getValue
    case "failed" | "shuttingdown" => FLINK_JOB_STATUS_FAILED.getValue
  }

  def getStatusString(status: Int): String = STATUS_ARRAY.find(_.getValue == status).map(_.description)
    .getOrElse(throw new JobExecuteErrorException(30351, s"Unknown status $status."))

  val TASK_MONITOR_INTERVAL: CommonVars[TimeType] = CommonVars("wds.streamis.task.monitor.interval", new TimeType("1m"))

  val TASK_SUBMIT_TIME_MAX: CommonVars[TimeType] = CommonVars("wds.streamis.task.submit.time.max", new TimeType("5m"))

}

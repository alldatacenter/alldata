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

package com.webank.wedatasphere.streamis.jobmanager.manager.service

import java.util
import java.util.Date
import java.util.concurrent.{Future, TimeUnit}
import com.google.common.collect.Sets
import com.webank.wedatasphere.streamis.jobmanager.launcher.JobLauncherAutoConfiguration
import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants
import com.webank.wedatasphere.streamis.jobmanager.launcher.dao.StreamJobConfMapper
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.{FlinkJobInfo, LinkisJobInfo}
import com.webank.wedatasphere.streamis.jobmanager.manager.alert.{AlertLevel, Alerter}
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.{StreamJobMapper, StreamTaskMapper}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamTask}
import com.webank.wedatasphere.streamis.jobmanager.manager.utils.StreamTaskUtils

import javax.annotation.{PostConstruct, PreDestroy, Resource}
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.linkis.common.exception.ErrorException
import org.apache.linkis.common.utils.{Logging, RetryHandler, Utils}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.collection.convert.WrapAsScala._


@Service
class TaskMonitorService extends Logging {

  @Autowired private var streamTaskMapper:StreamTaskMapper=_
  @Autowired private var streamJobMapper:StreamJobMapper=_
  @Autowired private var jobService: StreamJobService =_

  @Autowired private var alerters:Array[Alerter] = _

  @Resource
  private var streamTaskService: StreamTaskService = _

  @Resource
  private var streamJobConfMapper: StreamJobConfMapper = _

  private var future: Future[_] = _

  @PostConstruct
  def init(): Unit = {
    if (JobConf.STREAMIS_JOB_MONITOR_ENABLE.getValue) {
      future = Utils.defaultScheduler.scheduleAtFixedRate(new Runnable {
        override def run(): Unit = Utils.tryAndWarnMsg {
          doMonitor()
        }("Monitor the status of all tasks failed!")
      }, JobConf.TASK_MONITOR_INTERVAL.getValue.toLong, JobConf.TASK_MONITOR_INTERVAL.getValue.toLong, TimeUnit.MILLISECONDS)
    }
  }

  @PreDestroy
  def close(): Unit = {
    Option(future).foreach(_.cancel(true))
  }

  def doMonitor(): Unit = {
    info("Try to update all StreamTasks status.")
    val jobLaunchManager = JobLaunchManager.getJobManager(JobLauncherAutoConfiguration.DEFAULT_JOB_LAUNCH_MANGER)
    val status = util.Arrays.asList(JobConf.NOT_COMPLETED_STATUS_ARRAY.map(c => new Integer(c.getValue)) :_*)
    val streamTasks = streamTaskMapper.getTasksByStatus(status)
    if(streamTasks == null || streamTasks.isEmpty) {
      info("No StreamTasks is running, return...")
      return
    }
    streamTasks.filter(shouldMonitor).foreach { streamTask =>
      streamTask.setLastUpdateTime(new Date)
      streamTaskMapper.updateTask(streamTask)
      val job = streamJobMapper.getJobById(streamTask.getJobId)
      info(s"Try to update status of StreamJob-${job.getName}.")
      val retryHandler = new RetryHandler {}
      retryHandler.setRetryNum(3)
      retryHandler.setRetryMaxPeriod(2000)
      retryHandler.addRetryException(classOf[ErrorException])
      var jobInfo:JobInfo = null
      Utils.tryCatch {
        jobInfo = retryHandler.retry(refresh(streamTask, jobLaunchManager), s"Task-Monitor-${job.getName}")
      } { ex => {
        error(s"Fetch StreamJob-${job.getName} failed, maybe the Linkis cluster is wrong, please be noticed!", ex)
        val errorMsg = ExceptionUtils.getRootCauseMessage(ex)
        if (errorMsg != null && errorMsg.contains("Not exists EngineConn")) {
          streamTask.setStatus(JobConf.FLINK_JOB_STATUS_FAILED.getValue)
          streamTask.setErrDesc("Not exists EngineConn.")
        } else {
          // 连续三次还是出现异常，说明Linkis的Manager已经不能正常提供服务，告警并不再尝试获取状态，等待下次尝试
          val users = getAlertUsers(job)
          users.add(job.getCreateBy)
          alert(jobService.getAlertLevel(job), s"请求LinkisManager失败，Linkis集群出现异常，请关注！影响任务[${job.getName}]", users, streamTask)
        }
      }
      }
      streamTaskMapper.updateTask(streamTask)
      if(streamTask.getStatus == JobConf.FLINK_JOB_STATUS_FAILED.getValue) {
        warn(s"StreamJob-${job.getName} is failed, please be noticed.")
        var extraMessage = ""
        Option(jobInfo) match {
          case Some(flinkJobInfo: FlinkJobInfo) =>
            extraMessage = s",${flinkJobInfo.getApplicationId}"
          case _ =>
        }
        // Need to add restart feature if user sets the restart parameters.
        var alertMsg = s"Streamis 流式应用[${job.getName}${extraMessage}]已经失败, 请登陆Streamis查看应用日志."
        this.streamJobConfMapper.getRawConfValue(job.getId, JobConfKeyConstants.FAIL_RESTART_SWITCH.getValue) match {
          case "ON" =>
            alertMsg = s"${alertMsg} 现将自动拉起该应用"
            Utils.tryCatch{
              info(s"Start to reLaunch the StreamisJob [${job.getName}], now to submit and schedule it...")
              // Use submit user to start job
              val future: Future[String] = streamTaskService.asyncExecute(job.getId, 0L, job.getSubmitUser, true)
            }{
              case e:Exception =>
                warn(s"Fail to reLaunch the StreamisJob [${job.getName}]", e)
            }
          case _ =>
        }
        val userList = Sets.newHashSet(job.getSubmitUser, job.getCreateBy)
        userList.addAll(getAlertUsers(job))
        alert(jobService.getAlertLevel(job), alertMsg, new util.ArrayList[String](userList), streamTask)
      }
    }
    info("All StreamTasks status have updated.")
  }

  /**
   * Refresh streamis task
   * @param streamTask stream task
   * @param jobLaunchManager launch manager
   */
  protected def refresh(streamTask: StreamTask, jobLaunchManager: JobLaunchManager[_ <: JobInfo]): JobInfo ={
    val jobClient = jobLaunchManager.connect(streamTask.getLinkisJobId, streamTask.getLinkisJobInfo)
    StreamTaskUtils.refreshInfo(streamTask, jobClient.getJobInfo(true))
    jobClient.getJobInfo
  }

  protected def getAlertUsers(job: StreamJob): util.List[String] = {
    var users = jobService.getAlertUsers(job)
    if (users == null) {
      users = new util.ArrayList[String]()
    }
    users.addAll(util.Arrays.asList(JobConf.STREAMIS_DEVELOPER.getValue.split(","):_*))
    users
  }

  protected def alert(alertLevel: AlertLevel, alertMsg: String, users: util.List[String], streamTask:StreamTask): Unit = alerters.foreach{ alerter =>
    Utils.tryCatch {
      alerter.alert(alertLevel, alertMsg, users, streamTask)
    }(t => error(s"failed to send alert message to ${alerter.getClass.getSimpleName}.", t))
  }

  protected def shouldMonitor(streamTask: StreamTask): Boolean =
    System.currentTimeMillis - streamTask.getLastUpdateTime.getTime >= JobConf.TASK_MONITOR_INTERVAL.getValue.toLong

  protected def getStatus(jobInfo: LinkisJobInfo): Int = {
    //TODO We should use jobInfo to get more accurate status, such as Alert running, Slow running
    JobConf.linkisStatusToStreamisStatus(jobInfo.getStatus)
  }

}

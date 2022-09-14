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

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.{JobClient, LaunchJob}
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobStateManager
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration.{VAR_FLINK_APP_NAME, VAR_FLINK_SAVEPOINT_PATH}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception.FlinkJobLaunchErrorException
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.LinkisJobInfo
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.computation.client.once.{OnceJob, SubmittableOnceJob}
import org.apache.linkis.computation.client.utils.LabelKeyUtils
import org.apache.linkis.protocol.utils.TaskUtils



trait FlinkJobLaunchManager extends LinkisJobLaunchManager with Logging {

  protected var jobStateManager: JobStateManager = _

  protected def buildOnceJob(job: LaunchJob): SubmittableOnceJob

  protected def createSubmittedOnceJob(id: String, jobInfo: LinkisJobInfo): OnceJob


  protected def createJobInfo(onceJob: SubmittableOnceJob, job: LaunchJob, jobState: JobState): LinkisJobInfo

  protected def createJobInfo(jobInfo: String): LinkisJobInfo

  /**
   * This method is used to launch a new job.
   *
   * @param job      a StreamisJob wanted to be launched.
   * @param jobState job state used to launch
   * @return the job id.
   */
  override def innerLaunch(job: LaunchJob, jobState: JobState): JobClient[LinkisJobInfo] = {
    // Transform the JobState into the params in LaunchJob
    Option(jobState).foreach(state => {
      val startUpParams = TaskUtils.getStartupMap(job.getParams)
      startUpParams.putIfAbsent(VAR_FLINK_SAVEPOINT_PATH.getValue,
        state.getLocation.toString)
    })
    TaskUtils.getStartupMap(job.getParams).put(VAR_FLINK_APP_NAME.getValue,
      Option(job.getJobName) match {
        case None => "EngineConn-Flink"
        case Some(jobName) =>
          val index = jobName.lastIndexOf(".")
          if (index > 0) jobName.substring(0, index) else jobName
    })
    job.getLabels.get(LabelKeyUtils.ENGINE_TYPE_LABEL_KEY) match {
      case engineConnType: String =>
        if(!engineConnType.toLowerCase.startsWith(FlinkJobLaunchManager.FLINK_ENGINE_CONN_TYPE))
          throw new FlinkJobLaunchErrorException(30401, s"Only ${FlinkJobLaunchManager.FLINK_ENGINE_CONN_TYPE} job is supported to be launched to Linkis, but $engineConnType is found.", null)
      case _ => throw new FlinkJobLaunchErrorException(30401, s"Not exists ${LabelKeyUtils.ENGINE_TYPE_LABEL_KEY}, StreamisJob cannot be submitted to Linkis successfully.", null)
    }
    Utils.tryCatch {
      val onceJob = buildOnceJob(job)
      onceJob.submit()
      val jobInfo = Utils.tryCatch(createJobInfo(onceJob, job, jobState)) {
        case e: FlinkJobLaunchErrorException =>
          throw e
        case t: Throwable =>
          error(s"${job.getSubmitUser} create jobInfo failed, now stop this EngineConn ${onceJob.getId}.")
          Utils.tryAndWarn(onceJob.kill())
          throw new FlinkJobLaunchErrorException(-1, "Fail to obtain launched job info", t)
      }
      createJobClient(onceJob, jobInfo)
    }{
      case e: FlinkJobLaunchErrorException => throw e
      case t: Throwable =>
        error(s"Server Exception in submitting Flink job [${job.getJobName}] to Linkis remote server", t)
        throw new FlinkJobLaunchErrorException(-1, s"Exception in submitting Flink job to Linkis remote server (提交至Linkis服务失败，请检查服务及网络)", t)
    }
  }

  override def launch(job: LaunchJob): JobClient[LinkisJobInfo] = {
    launch(job, null)
  }


  override def connect(id: String, jobInfo: String): JobClient[LinkisJobInfo] = {
    connect(id, createJobInfo(jobInfo))
  }

  override def connect(id: String, jobInfo: LinkisJobInfo): JobClient[LinkisJobInfo] = {
    createJobClient(createSubmittedOnceJob(id, jobInfo), jobInfo)
  }


  /**
   * Job state manager(store the state information, example: Checkpoint/Savepoint)
   *
   * @return state manager instance
   */
  override def getJobStateManager: JobStateManager = {
    Option(jobStateManager) match {
      case None =>
        this synchronized{
          // Flink job state manager
          jobStateManager = new FlinkJobStateManager
        }
        jobStateManager
      case Some(stateManager) => stateManager
    }
  }

  /**
   * Create job client
   * @param onceJob once job
   * @param jobInfo job info
   * @return
   */
  protected def createJobClient(onceJob: OnceJob, jobInfo: LinkisJobInfo): JobClient[LinkisJobInfo]
}
object FlinkJobLaunchManager {
  val FLINK_ENGINE_CONN_TYPE = "flink"
}
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

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.{JobState, JobStateInfo}
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.{JobClient, LaunchJob}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.{FlinkJobClient, FlinkJobInfo, LinkisJobInfo}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager.SimpleFlinkJobLaunchManager.INSTANCE_NAME
import org.apache.commons.lang.StringEscapeUtils
import org.apache.linkis.common.utils.{RetryHandler, Utils}
import org.apache.linkis.computation.client.once.simple.{SimpleOnceJob, SubmittableSimpleOnceJob}
import org.apache.linkis.computation.client.once.{OnceJob, SubmittableOnceJob}
import org.apache.linkis.computation.client.operator.impl.{EngineConnApplicationInfoOperator, EngineConnLogOperator}
import org.apache.linkis.httpclient.dws.DWSHttpClient
import org.apache.linkis.ujes.client.exception.UJESJobException

import java.util
import scala.collection.JavaConverters.mapAsScalaMapConverter

class SimpleFlinkJobLaunchManager extends FlinkJobLaunchManager {

  override def getName: String = INSTANCE_NAME

  protected def buildOnceJob(job: LaunchJob): SubmittableOnceJob = {
    val builder = SimpleOnceJob.builder().addExecuteUser(job.getSubmitUser).setLabels(job.getLabels)
      .setJobContent(job.getJobContent).setParams(job.getParams).setSource(job.getSource)
    if(job.getLaunchConfigs != null) {
      job.getLaunchConfigs.asScala.get(LaunchJob.LAUNCH_CONFIG_CREATE_SERVICE).foreach{ case createService: String => builder.setCreateService(createService)}
      job.getLaunchConfigs.asScala.get(LaunchJob.LAUNCH_CONFIG_DESCRIPTION).foreach{ case desc: String => builder.setDescription(desc)}
      job.getLaunchConfigs.asScala.get(LaunchJob.LAUNCH_CONFIG_MAX_SUBMIT_TIME).foreach{ case maxSubmitTime: Long => builder.setMaxSubmitTime(maxSubmitTime)}
    }
    builder.build()
  }

  override protected def createSubmittedOnceJob(id: String, jobInfo: LinkisJobInfo): OnceJob = SimpleOnceJob.build(id, jobInfo.getUser)


  override protected def createJobInfo(onceJob: SubmittableOnceJob, job: LaunchJob, jobState: JobState): LinkisJobInfo = {
    val nodeInfo = onceJob.getNodeInfo
    val jobInfo = new FlinkJobInfo
    // Escape the job name
    jobInfo.setName(StringEscapeUtils.escapeJava(job.getJobName))
    jobInfo.setId(onceJob.getId)
    jobInfo.setUser(job.getSubmitUser)
    onceJob match {
      case simpleOnceJob: SubmittableSimpleOnceJob =>
        jobInfo.setECMInstance(simpleOnceJob.getECMServiceInstance)
      case _ =>
    }
    Utils.tryCatch(fetchApplicationInfo(onceJob, jobInfo)) { t =>
      val message = s"Unable to fetch the application info of launched job [${job.getJobName}], maybe the engine has been shutdown"
      error(message, t)
      // Mark failed
      jobInfo.setStatus("failed")
      jobInfo.setCompletedMsg(message)
    }
    jobInfo.setResources(nodeInfo.get("nodeResource").asInstanceOf[util.Map[String, Object]])
    // Set job state info into
//    Option(jobState).foreach(state => {
//      val stateInfo = new JobStateInfo
//      stateInfo.setTimestamp(state.getTimestamp)
//      stateInfo.setLocation(state.getLocation.toString)
//      jobInfo.setJobStates(Array(stateInfo))
//    })
    jobInfo
  }

  override protected def createJobInfo(jobInfo: String): LinkisJobInfo = DWSHttpClient.jacksonJson.readValue(jobInfo, classOf[FlinkJobInfo])

  protected def fetchApplicationInfo(onceJob: OnceJob, jobInfo: FlinkJobInfo): Unit = {
    onceJob.getOperator(EngineConnApplicationInfoOperator.OPERATOR_NAME) match {
      case applicationInfoOperator: EngineConnApplicationInfoOperator =>
        val retryHandler = new RetryHandler {}
        retryHandler.setRetryNum(JobLauncherConfiguration.FLINK_FETCH_APPLICATION_INFO_MAX_TIMES.getValue)
        retryHandler.setRetryMaxPeriod(5000)
        retryHandler.setRetryPeriod(500)
        retryHandler.addRetryException(classOf[UJESJobException])
        val applicationInfo = retryHandler.retry(applicationInfoOperator(), "Fetch-Yarn-Application-Info")
        jobInfo.setApplicationId(applicationInfo.applicationId)
        jobInfo.setApplicationUrl(applicationInfo.applicationUrl)
    }
  }

  /**
   * Create job client
   *
   * @param onceJob once job
   * @param jobInfo job info
   * @return
   */
  override protected def createJobClient(onceJob: OnceJob, jobInfo: LinkisJobInfo): JobClient[LinkisJobInfo] = {
    jobInfo match {
      case flinkJobInfo: FlinkJobInfo =>
        new FlinkJobClient(onceJob, flinkJobInfo, this.jobStateManager).asInstanceOf[JobClient[LinkisJobInfo]]
      case _ => null
    }
  }

  /**
   * Init method
   */
  override def init(): Unit = {
    // Init the job state manager
     getJobStateManager.init()
  }


  /**
   * Destroy method
   */
  override def destroy(): Unit = {
    // Destroy the job state manager
    getJobStateManager.destroy()
  }
}
object SimpleFlinkJobLaunchManager{

  val INSTANCE_NAME = "simpleFlink";
}

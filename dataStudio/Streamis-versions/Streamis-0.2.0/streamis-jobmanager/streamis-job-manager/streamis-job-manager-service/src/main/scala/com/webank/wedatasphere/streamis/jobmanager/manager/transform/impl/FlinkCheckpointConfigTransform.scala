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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.impl

import com.webank.wedatasphere.streamis.jobmanager.launcher.JobLauncherAutoConfiguration
import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants

import java.util
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.Checkpoint
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.impl.FlinkCheckpointConfigTransform.CHECKPOINT_PATH_CONFIG_NAME
import org.apache.linkis.common.utils.Logging

import scala.collection.JavaConverters._

/**
 * Use the flink inner parameters instead of the engine parameter (in linkis)
 */
class FlinkCheckpointConfigTransform extends FlinkConfigTransform with Logging{


  /**
   * Config group name
   *
   * @return
   */
  override protected def configGroup(): String = JobConfKeyConstants.GROUP_PRODUCE.getValue

  override protected def transform(produceConfig: util.Map[String, Any], job: LaunchJob): LaunchJob = {
    produceConfig.get(JobConfKeyConstants.CHECKPOINT_SWITCH.getValue) match {
      case "ON" =>
        val checkpointConfig: util.Map[String, Any] =  new util.HashMap[String, Any]()
        val jobLaunchManager = JobLaunchManager.getJobManager(JobLauncherAutoConfiguration.DEFAULT_JOB_LAUNCH_MANGER)
        val checkpointPath = jobLaunchManager.getJobStateManager.getJobStateDir(classOf[Checkpoint], job.getJobName)
        checkpointConfig.put(FlinkConfigTransform.FLINK_CONFIG_PREFIX + CHECKPOINT_PATH_CONFIG_NAME, checkpointPath)
        info(s"Use the checkpoint dir, ${CHECKPOINT_PATH_CONFIG_NAME} => ${checkpointPath}")
        produceConfig.asScala.filter(_._1.startsWith(JobConfKeyConstants.CHECKPOINT.getValue))
          .foreach{
            case (key, value) =>
              checkpointConfig.put(FlinkConfigTransform.FLINK_CONFIG_PREFIX + key
                .replace(JobConfKeyConstants.CHECKPOINT.getValue, "execution.checkpointing."), value)
        }
        transformConfig(checkpointConfig, job)
      case _ => job
    }
  }

}

object FlinkCheckpointConfigTransform{
  val CHECKPOINT_PATH_CONFIG_NAME = "state.checkpoints.dir"
}

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

import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants

import java.util
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.ConfigTransform
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.impl.ResourceConfigTransform.RESOURCE_CONFIG_MAP
import com.webank.wedatasphere.streamis.jobmanager.manager.utils.JobUtils
import org.apache.linkis.protocol.utils.TaskUtils

import scala.collection.JavaConverters._


class ResourceConfigTransform extends ConfigTransform {


  /**
   * Config group name
   *
   * @return
   */
  override protected def configGroup(): String = JobConfKeyConstants.GROUP_RESOURCE.getValue


  override protected def transform(valueSet: util.Map[String, Any], job: LaunchJob): LaunchJob = {
    val startupMap = valueSet.asScala.map{
      case (key, value) =>
        RESOURCE_CONFIG_MAP.get(key) match {
          case Some(mappingKey) =>
            (mappingKey, value)
          case _ => (key, value)
        }
    }.asJava
    val params = if(job.getParams == null) new util.HashMap[String, Any] else job.getParams
    if(!startupMap.isEmpty) TaskUtils.addStartupMap(params, JobUtils.filterParameterSpec(startupMap))
    LaunchJob.builder().setLaunchJob(job).setParams(params).build()
  }
}

object ResourceConfigTransform{
  val RESOURCE_CONFIG_MAP = Map(
    "wds.linkis.flink.taskmanager.memory" ->"flink.taskmanager.memory",
    "wds.linkis.flink.jobmanager.memory" -> "flink.jobmanager.memory",
    "wds.linkis.flink.taskmanager.cpus" -> "flink.taskmanager.cpu.cores",
    "wds.linkis.flink.taskmanager.numberOfTaskSlots" -> "flink.taskmanager.numberOfTaskSlots",
    "wds.linkis.flink.app.parallelism" -> "wds.linkis.engineconn.flink.app.parallelism"
  )
}

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

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.ConfigTransform
import com.webank.wedatasphere.streamis.jobmanager.manager.utils.JobUtils
import org.apache.linkis.protocol.utils.TaskUtils

import scala.collection.convert.WrapAsScala._
import java.util

/**
 * Flink common config transform
 */
abstract class FlinkConfigTransform extends ConfigTransform {

  protected def transformConfig(getConfig: => util.Map[String, Any], job: LaunchJob): LaunchJob = {
    val startupMap = new util.HashMap[String, Any]
    Option(getConfig).foreach(configSeq => configSeq.foreach{
      case (key, value) => startupMap.put(key, value)
      case _ =>
    })
    val params = if(job.getParams == null) new util.HashMap[String, Any] else job.getParams
    if(!startupMap.isEmpty) TaskUtils.addStartupMap(params, JobUtils.filterParameterSpec(startupMap))
    LaunchJob.builder().setLaunchJob(job).setParams(params).build()
  }
}
object FlinkConfigTransform{

  val FLINK_CONFIG_PREFIX:String = "_FLINK_CONFIG_."
}

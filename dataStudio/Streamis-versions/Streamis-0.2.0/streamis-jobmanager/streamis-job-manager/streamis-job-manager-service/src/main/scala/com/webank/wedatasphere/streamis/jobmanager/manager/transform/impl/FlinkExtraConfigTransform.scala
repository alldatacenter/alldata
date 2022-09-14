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
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob

import java.util
import scala.collection.JavaConverters._

/**
 * Flink extra configuration(key => _FLINK_CONFIG_ + key)
 */
class FlinkExtraConfigTransform extends FlinkConfigTransform {


  /**
   * Config group name
   *
   * @return
   */
  override protected def configGroup(): String = JobConfKeyConstants.GROUP_FLINK_EXTRA.getValue

  override protected def transform(flinkExtra: util.Map[String, Any], job: LaunchJob): LaunchJob = {
    transformConfig(flinkExtra.asScala.map(entry =>{
      (FlinkConfigTransform.FLINK_CONFIG_PREFIX + entry._1, entry._2)
    }).asJava, job)
  }

}

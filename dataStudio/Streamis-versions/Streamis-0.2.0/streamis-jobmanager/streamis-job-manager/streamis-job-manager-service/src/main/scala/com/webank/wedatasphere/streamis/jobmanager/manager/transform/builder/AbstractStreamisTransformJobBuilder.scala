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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.builder

import org.apache.linkis.common.conf.CommonVars
import org.apache.linkis.manager.label.entity.engine.RunType.RunType
import com.webank.wedatasphere.streamis.jobmanager.launcher.service.StreamJobConfService
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.StreamJobMapper
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.StreamisTransformJobBuilder
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisJobEngineConnImpl, StreamisTransformJob, StreamisTransformJobContent, StreamisTransformJobImpl}
import org.springframework.beans.factory.annotation.Autowired

/**
  * Created by enjoyyin on 2021/9/22.
  */
abstract class AbstractStreamisTransformJobBuilder extends StreamisTransformJobBuilder {

  @Autowired private var streamJobMapper: StreamJobMapper = _
  @Autowired private var streamJobConfService: StreamJobConfService = _

  protected def createStreamisTransformJob(): StreamisTransformJobImpl = new StreamisTransformJobImpl

  protected def createStreamisTransformJobContent(transformJob: StreamisTransformJob): StreamisTransformJobContent

  override def build(streamJob: StreamJob): StreamisTransformJob = {
    val transformJob = createStreamisTransformJob()
    transformJob.setStreamJob(streamJob)
    transformJob.setConfigMap(streamJobConfService.getJobConfig(streamJob.getId))
//    transformJob.setConfig(configurationService.getFullTree(streamJob.getId))
    val streamJobVersions = streamJobMapper.getJobVersions(streamJob.getId)
    // 无需判断streamJobVersions是否非空，因为TaskService已经判断了
    transformJob.setStreamJobVersion(streamJobVersions.get(0))
    transformJob.setStreamisTransformJobContent(createStreamisTransformJobContent(transformJob))
    transformJob
  }

}

abstract class AbstractFlinkStreamisTransformJobBuilder extends AbstractStreamisTransformJobBuilder{

  private val flinkVersion = CommonVars("wds.streamis.flink.submit.version", "1.12.2").getValue

  protected def getRunType(transformJob: StreamisTransformJob): RunType

  override def build(streamJob: StreamJob): StreamisTransformJob = super.build(streamJob) match {
    case transformJob: StreamisTransformJobImpl =>
      val engineConn = new StreamisJobEngineConnImpl
      engineConn.setEngineConnType("flink-" + flinkVersion)
      engineConn.setRunType(getRunType(transformJob))
      transformJob.setStreamisJobEngineConn(engineConn)
      transformJob
    case job => job
  }
}
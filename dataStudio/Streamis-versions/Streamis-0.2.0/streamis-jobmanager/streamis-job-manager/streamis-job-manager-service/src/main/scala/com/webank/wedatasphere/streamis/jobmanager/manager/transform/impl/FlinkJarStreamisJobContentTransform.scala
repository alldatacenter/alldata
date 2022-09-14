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

import java.util
import org.apache.linkis.common.utils.JsonUtils
import org.apache.linkis.protocol.utils.TaskUtils
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamisFile
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.{StreamisJobContentTransform, Transform}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisJarTransformJobContent, StreamisTransformJob, StreamisTransformJobContent}
import com.webank.wedatasphere.streamis.jobmanager.manager.utils.JobUtils

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by enjoyyin on 2021/9/23.
  */
class FlinkJarStreamisJobContentTransform extends StreamisJobContentTransform {
  override protected def transformJobContent(transformJob: StreamisTransformJobContent): util.HashMap[String, Any] = transformJob match {
    case transformJobContent: StreamisJarTransformJobContent =>
      val jobContent = new util.HashMap[String, Any]
      jobContent.put("flink.app.args", transformJobContent.getArgs.asScala.mkString(" "))
      jobContent.put("flink.app.main.class", transformJobContent.getMainClass)
      jobContent
    case _ => null
  }
}

class FlinkJarStreamisStartupParamsTransform extends Transform {

  override def transform(streamisTransformJob: StreamisTransformJob, job: LaunchJob): LaunchJob = streamisTransformJob.getStreamisTransformJobContent match {
    case transformJobContent: StreamisJarTransformJobContent =>
      val startupMap = new util.HashMap[String, Any]
      startupMap.put("flink.app.main.class.jar", transformJobContent.getMainClassJar.getFileName)
      startupMap.put("flink.app.main.class.jar.bml.json",
        JsonUtils.jackson.writeValueAsString(getStreamisFileContent(transformJobContent.getMainClassJar)))
      val classpathFiles = if(transformJobContent.getDependencyJars != null && transformJobContent.getResources != null) {
        startupMap.put("flink.app.user.class.path", transformJobContent.getDependencyJars.asScala.map(_.getFileName).mkString(","))
        transformJobContent.getDependencyJars.asScala ++ transformJobContent.getResources.asScala
      } else if(transformJobContent.getDependencyJars != null) {
        startupMap.put("flink.app.user.class.path", transformJobContent.getDependencyJars.asScala.map(_.getFileName).mkString(","))
        transformJobContent.getDependencyJars.asScala
      } else if(transformJobContent.getResources != null) {
        startupMap.put("flink.yarn.ship-directories", transformJobContent.getResources.asScala.map(_.getFileName).mkString(","))
        transformJobContent.getResources.asScala
      }
      else mutable.Buffer[StreamisFile]()
      if(classpathFiles.nonEmpty)
        startupMap.put("flink.app.user.class.path.bml.json",
          JsonUtils.jackson.writeValueAsString(classpathFiles.map(getStreamisFileContent).asJava))
      if(transformJobContent.getHdfsJars != null)
        startupMap.put("flink.user.lib.path", transformJobContent.getHdfsJars.asScala.mkString(","))
      val params = if(job.getParams == null) new util.HashMap[String, Any] else job.getParams
      if(!startupMap.isEmpty) TaskUtils.addStartupMap(params, JobUtils.filterParameterSpec(startupMap))
      LaunchJob.builder().setLaunchJob(job).setParams(params).build()
    case _ => job
  }

  private def getStreamisFileContent(streamisFile: StreamisFile): util.Map[String, Object] = {
    val content = JsonUtils.jackson.readValue(streamisFile.getStorePath, classOf[util.Map[String, Object]])
    content.put("fileName", streamisFile.getFileName)
    content
  }

}
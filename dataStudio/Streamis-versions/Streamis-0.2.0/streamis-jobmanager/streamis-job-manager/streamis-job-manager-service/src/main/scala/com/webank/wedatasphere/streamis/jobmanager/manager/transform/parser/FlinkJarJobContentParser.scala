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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform.parser
import java.util

import org.apache.linkis.common.utils.JsonUtils
import org.apache.linkis.manager.label.entity.engine.RunType
import org.apache.linkis.manager.label.entity.engine.RunType.RunType
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{StreamJob, StreamJobVersion, StreamisFile}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.{StreamisJarTransformJobContent, StreamisTransformJobContent}
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component

import scala.collection.JavaConverters._

/**
  * Created by enjoyyin on 2021/9/23.
  */
@Component
class FlinkJarJobContentParser extends AbstractJobContentParser {

  override def parseTo(job: StreamJob, jobVersion: StreamJobVersion): StreamisTransformJobContent = {
    val transformJobContent = new StreamisJarTransformJobContent
    val jobContent = JsonUtils.jackson.readValue(jobVersion.getJobContent, classOf[util.Map[String, Object]])
    jobContent.get("main.class.jar") match {
      case mainClassJar: String =>
        val file = dealStreamisFile(job, jobVersion, mainClassJar, "main.class.jar")
        transformJobContent.setMainClassJar(file)
      case _ => throw new JobExecuteErrorException(30500, "main.class.jar is needed.")
    }
    jobContent.get("main.class") match {
      case mainClass: String =>
        transformJobContent.setMainClass(mainClass)
      case _ => throw new JobExecuteErrorException(30500, "main.class is needed.")
    }
    jobContent.get("args") match {
      case args: util.List[String] =>
        transformJobContent.setArgs(args)
      case _ =>
    }
    jobContent.get("hdfs.jars") match {
      case hdfsJars: util.List[String] =>
        transformJobContent.setHdfsJars(hdfsJars)
      case _ =>
    }
    jobContent.get("dependency.jars") match {
      case dependencyJars: util.List[String] =>
        val parsedDependencyJars = dependencyJars.asScala.filter(StringUtils.isNotBlank).map {
          dependencyJar => dealStreamisFile(job, jobVersion, dependencyJar, "dependency.jar")
        }.asJava
        transformJobContent.setDependencyJars(parsedDependencyJars)
      case _ =>
    }
    jobContent.get("resources") match {
      case resources: util.List[String] =>
        val parsedResources = resources.asScala.filter(StringUtils.isNotBlank).map {
          resource => dealStreamisFile(job, jobVersion, resource, "resources")
        }.asJava
        transformJobContent.setResources(parsedResources)
      case _ =>
    }
    transformJobContent
  }

  private def dealStreamisFile(job: StreamJob, jobVersion: StreamJobVersion, fileName: String, fileType: String): StreamisFile = {
    info(s"Try to parse the $fileType $fileName for StreamJob-${job.getName}.")
    this.findFile(job, jobVersion, fileName)
  }

  override val jobType: String = FlinkJarJobContentParser.JOB_TYPE
  override val runType: RunType = RunType.JAR
}

object FlinkJarJobContentParser {
  val JOB_TYPE = "flink.jar"
}
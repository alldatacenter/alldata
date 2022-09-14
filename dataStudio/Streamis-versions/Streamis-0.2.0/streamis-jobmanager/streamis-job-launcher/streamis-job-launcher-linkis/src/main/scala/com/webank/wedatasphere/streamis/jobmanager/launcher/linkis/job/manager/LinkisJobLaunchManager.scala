package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.{JobClient, LaunchJob}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.LinkisJobInfo
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager.LinkisJobLaunchManager.LINKIS_JAR_VERSION_PATTERN
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.computation.client.LinkisJob
import org.apache.linkis.protocol.utils.TaskUtils

import java.util
import scala.collection.JavaConverters._
import scala.util.matching.Regex

trait LinkisJobLaunchManager extends JobLaunchManager[LinkisJobInfo] with Logging{
  /**
   * This method is used to launch a new job.
   *
   * @param job      a StreamisJob wanted to be launched.
   * @param jobState job state used to launch
   * @return the job id.
   */
  override def launch(job: LaunchJob, jobState: JobState): JobClient[LinkisJobInfo] = {
    // Support different version of Linkis
    var linkisVersion = JobLauncherConfiguration.FLINK_LINKIS_RELEASE_VERSION.getValue
    if (StringUtils.isBlank(linkisVersion)) {
      val linkisJarPath = classOf[LinkisJob].getProtectionDomain.getCodeSource.getLocation.getPath;
      val lastSplit = linkisJarPath.lastIndexOf(IOUtils.DIR_SEPARATOR);
      if (lastSplit >= 0) {
        linkisVersion = linkisJarPath.substring(lastSplit + 1)
      }
    }
    if (StringUtils.isNotBlank(linkisVersion)) {
      Utils.tryAndWarn {
        val LINKIS_JAR_VERSION_PATTERN(version) = linkisVersion
        linkisVersion = version
      }
    }
     if (StringUtils.isNotBlank(linkisVersion)){
          val versionSplitter: Array[String] = linkisVersion.split("\\.")
          val major = Integer.valueOf(versionSplitter(0))
          val sub = Integer.valueOf(versionSplitter(1))
          val fix = Integer.valueOf(versionSplitter(2))
          val versionNum = major * 10000 + sub * 100 + fix
          info(s"Recognized the linkis release version: [${linkisVersion}, version number: [${versionNum}]")
          if (versionNum <= 10101){
            warn("Linkis version number is less than [10101], should compatible the startup params in launcher.")
            val startupParams = TaskUtils.getStartupMap(job.getParams)
            // Change the unit of memory params for linkis older version
            changeUnitOfMemoryToG(startupParams, "flink.taskmanager.memory")
            changeUnitOfMemoryToG(startupParams, "flink.jobmanager.memory")
            // Avoid the _FLINK_CONFIG_. prefix for linkis older version
            val newParams = avoidParamsPrefix(startupParams, "_FLINK_CONFIG_.")
            startupParams.clear();
            startupParams.putAll(newParams)
          }
     }
     innerLaunch(job, jobState)
  }

  private def changeUnitOfMemoryToG(params: util.Map[String, Any], name: String): Unit = {
      params.get(name) match {
        case memory: String =>
          var actualMem = Integer.valueOf(memory) / 1024
          actualMem = if (actualMem <= 0) 1 else actualMem
          info(s"Change the unit of startup param: [${name}], value [${memory}] => [${actualMem}]")
          params.put(name, actualMem)
        case _ => // Ignores
      }
  }

  /**
   * Avoid params prefix
   * @param params params
   * @param prefix prefix
   */
  private def avoidParamsPrefix(params: util.Map[String, Any], prefix: String): util.Map[String, Any] = {
      params.asScala.map{
        case (key, value) =>
          if (key.startsWith(prefix)){
             info(s"Avoid the prefix of startup param: [${key}] => [${key.substring(prefix.length)}]")
            (key.substring(prefix.length), value)
          } else {
            (key, value)
          }
      }.toMap.asJava
  }
  def innerLaunch(job: LaunchJob, jobState: JobState): JobClient[LinkisJobInfo]
}

object LinkisJobLaunchManager{
   val LINKIS_JAR_VERSION_PATTERN: Regex = "^[\\s\\S]*([\\d]+\\.[\\d]+\\.[\\d]+)[\\s\\S]*$".r
}

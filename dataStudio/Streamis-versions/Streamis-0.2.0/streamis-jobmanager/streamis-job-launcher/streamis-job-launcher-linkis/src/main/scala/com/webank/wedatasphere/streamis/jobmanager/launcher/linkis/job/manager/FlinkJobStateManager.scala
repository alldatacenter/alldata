package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.manager
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.{Checkpoint, CheckpointJobStateFetcher, Savepoint, SavepointJobStateFetcher}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.url.LinkisURLStreamHandlerFactory
import org.apache.linkis.common.utils.{Logging, Utils}

import java.net.URL
import scala.collection.JavaConverters.mapAsScalaMapConverter


/**
 * Flink job state manager
 */
class FlinkJobStateManager extends AbstractJobStateManager with Logging{
  /**
   * State type => root path
   */
  val stateRootPath: Map[String, String] = Map(
    classOf[Savepoint].getCanonicalName -> JobLauncherConfiguration.FLINK_SAVEPOINT_PATH.getValue,
    classOf[Checkpoint].getCanonicalName -> JobLauncherConfiguration.FLINK_CHECKPOINT_PATH.getValue
  )

  override def getJobStateRootPath[T <: JobState](clazz: Class[_], schema: String): String = {
     stateRootPath.getOrElse(clazz.getCanonicalName, "")
  }

  /**
   * Init method
   */
  override def init(): Unit = {
    info("Register the loader for JobState fetcher")
    // TODO register the fetcher
    registerJobStateFetcher(classOf[Checkpoint], () => new CheckpointJobStateFetcher(classOf[Checkpoint], this))
    registerJobStateFetcher(classOf[Savepoint], () => new SavepointJobStateFetcher(classOf[Savepoint], this))
  }

  /**
   * Destroy method
   */
  override def destroy(): Unit = {
     // Close the loaded fetcher
     jobStateFetcherHolder.asScala.foreach(stateFetcher => {
       Utils.tryAndWarn(stateFetcher._2.destroy())
     })
  }
}

object FlinkJobStateManager{
  // set urlStreamHandler use support schemas
  URL.setURLStreamHandlerFactory(new LinkisURLStreamHandlerFactory(JobLauncherConfiguration.FLINK_STATE_SUPPORT_SCHEMES.getValue.split(","): _*))

  def main(args: Array[String]): Unit = {

  }
}

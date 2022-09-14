package com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.{JobClient, JobInfo, LaunchJob}

import java.util.concurrent.ConcurrentHashMap

/**
  * Basic job manager interface for launching job
 */
trait JobLaunchManager[T <: JobInfo] {

  /**
   * Init method
   */
  def init(): Unit

  /**
   * Destroy method
   */
  def destroy(): Unit

  /**
   * Manager name
   * @return
   */
  def getName: String

  def launch(job: LaunchJob): JobClient[T]

  /**
   * This method is used to launch a new job.
   * @param job a StreamisJob wanted to be launched.
   * @param jobState job state used to launch
   * @return the job id.
   */
  def launch(job: LaunchJob, jobState: JobState): JobClient[T]
  /**
   * Connect the job which already launched in another process,
   * if the job has been stored in process, just return the job info
   * @param id id
   * @param jobInfo job info
   * @return
   */
  def connect(id: String, jobInfo: String): JobClient[T]

  def connect(id: String, jobInfo: T): JobClient[T]
  /**
   * Job state manager(store the state information, example: Checkpoint/Savepoint)
   * @return state manager instance
   */
  def getJobStateManager: JobStateManager

}
object JobLaunchManager{

  /**
   * Store the job launch managers
   */
  private val launchManagers = new ConcurrentHashMap[String, JobLaunchManager[_ <: JobInfo]]()

  def registerJobManager(name: String, jobLaunchManager: JobLaunchManager[_ <: JobInfo]): Unit = {
    launchManagers.put(name, jobLaunchManager)
  }

  def getJobManager(name: String): JobLaunchManager[_ <: JobInfo] = {
    launchManagers.get(name)
  }
}
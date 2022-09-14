package com.webank.wedatasphere.streamis.jobmanager.launcher.job

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobStateInfo

/**
 * Job client
 *
 * @tparam T job info type
 */
trait JobClient[T <: JobInfo] {

  def getJobInfo: T

  /**
   * Refresh job info and return
   * @param refresh refresh
   * @return
   */
  def getJobInfo(refresh: Boolean): T
  /**
   * Stop the job connected remote
   * @param snapshot if do snapshot to save the job state
   * @return return the jobState info (if use snapshot) else return null
   */
  def stop(snapshot: Boolean): JobStateInfo

  /**
   * Stop directly
   */
  def stop(): Unit


}

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo
import org.apache.linkis.common.ServiceInstance

trait LinkisJobInfo extends JobInfo {

  /**
   * Fetch engine conn manager instance info
   * @return
   */
  def getECMInstance: ServiceInstance


}

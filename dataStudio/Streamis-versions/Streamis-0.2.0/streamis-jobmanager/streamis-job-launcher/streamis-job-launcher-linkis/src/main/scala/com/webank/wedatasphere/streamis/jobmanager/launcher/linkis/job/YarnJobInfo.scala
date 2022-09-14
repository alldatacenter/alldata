package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job

trait YarnJobInfo extends LinkisJobInfo {

  def getApplicationId: String

  def getApplicationUrl: String

}

package com.webank.wedatasphere.streamis.jobmanager.launcher.job.state

/**
 * Basic info
 */
class JobStateInfo {
  /**
   * Location
   */
  private var location: String = _

  /**
   * Timestamp
   */
  private var timestamp: Long = -1

  def setLocation(location: String): Unit = {
    this.location = location
  }

  def getLocation: String = {
    this.location
  }

  def setTimestamp(timestamp: Long): Unit = {
    this.timestamp = timestamp
  }
  def getTimestamp: Long = {
    timestamp
  }
}

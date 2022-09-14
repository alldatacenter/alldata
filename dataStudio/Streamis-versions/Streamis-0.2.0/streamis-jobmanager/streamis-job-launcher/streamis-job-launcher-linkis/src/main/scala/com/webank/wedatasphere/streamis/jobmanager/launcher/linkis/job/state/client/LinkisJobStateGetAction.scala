package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client

import org.apache.linkis.httpclient.dws.request.DWSHttpAction
import org.apache.linkis.httpclient.request.{GetAction, UserAction}

/**
 * Get job state action
 */
class LinkisJobStateGetAction extends GetAction with DWSHttpAction with UserAction{

  private var user: String = _

  def this(user: String, path: String) = {
    this()
    this.user = user
    this.setParameter("path", path);
  }


  override def suffixURLs: Array[String] = Array("filesystem", "getDirFileTrees")

  override def setUser(user: String): Unit = this.user = user

  override def getUser: String = user
}

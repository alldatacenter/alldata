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

package com.webank.wedatasphere.streamis.jobmanager.launcher.job

import java.util


trait LaunchJob {

  /**
   * Job name
   * @return
   */
  def getJobName: String

  def getSubmitUser: String

  def getLabels: util.Map[String, Any]

  def getJobContent: util.Map[String, Any]

  def getParams: util.Map[String, Any]

  def getSource: util.Map[String, Any]

  def getLaunchConfigs: util.Map[String, Any]

}

object LaunchJob {

  val LAUNCH_CONFIG_CREATE_SERVICE = "createService"
  val LAUNCH_CONFIG_DESCRIPTION = "description"
  val LAUNCH_CONFIG_MAX_SUBMIT_TIME = "maxSubmitTime"

  def builder(): Builder = new Builder

  class Builder {
    private var submitUser: String = _
    private var jobName: String =  _
    private var labels: util.Map[String, Any] = _
    private var jobContent: util.Map[String, Any] = _
    private var params: util.Map[String, Any] = _
    private var source: util.Map[String, Any] = _
    private var launchConfigs: util.Map[String, Any] = _

    def setJobName(jobName: String): this.type = {
      this.jobName = jobName
      this
    }

    def setSubmitUser(submitUser: String): this.type = {
      this.submitUser = submitUser
      this
    }

    def setLabels(labels: util.Map[String, Any]): this.type = {
      this.labels = labels
      this
    }

    def setJobContent(jobContent: util.Map[String, Any]): this.type = {
      this.jobContent = jobContent
      this
    }

    def setParams(param: util.Map[String, Any]): this.type = {
      this.params = param
      this
    }

    def setSource(source: util.Map[String, Any]): this.type = {
      this.source = source
      this
    }

    def setLaunchConfigs(launchConfigs: util.Map[String, Any]): this.type = {
      this.launchConfigs = launchConfigs
      this
    }

    def setLaunchJob(launchJob: LaunchJob): this.type = {
      setSubmitUser(launchJob.getSubmitUser).setLabels(launchJob.getLabels)
        .setJobContent(launchJob.getJobContent).setParams(launchJob.getParams)
        .setSource(launchJob.getSource).setLaunchConfigs(launchJob.getLaunchConfigs).setJobName(launchJob.getJobName)
    }

    def build(): LaunchJob = new LaunchJob {
      override def getSubmitUser: String = submitUser

      override def getLabels: util.Map[String, Any] = labels

      override def getJobContent: util.Map[String, Any] = jobContent

      override def getParams: util.Map[String, Any] = params

      override def getSource: util.Map[String, Any] = source

      override def getLaunchConfigs: util.Map[String, Any] = launchConfigs

      override def toString: String = s"LaunchJob(submitUser: $submitUser, labels: $labels, jobContent: $jobContent, params: $params, source: $source)"

      /**
       * Job name
       *
       * @return
       */
      override def getJobName: String = jobName
    }
  }

}

package com.webank.wedatasphere.streamis.jobmanager.manager.service

import com.github.pagehelper.PageInfo
import com.webank.wedatasphere.streamis.jobmanager.manager.alert.AlertLevel
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.{MetaJsonInfo, StreamAlertRecord, StreamJob, StreamJobVersion}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.{QueryJobListVo, TaskCoreNumVo, VersionDetailVo}
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJobContent

import java.util

/**
 * Job service
 */
trait StreamJobService {


  def getJobById(jobId: Long): StreamJob
  /**
   * Page list query
   * @param projectName project name
   * @param jobName job name
   * @param jobStatus job status
   * @param jobCreator job creator
   * @return
   */
  def getByProList(projectName: String, userName: String, jobName: String, jobStatus: Integer, jobCreator: String): PageInfo[QueryJobListVo]

  /**
   * Count core norm
   * @param projectName project name
   * @return
   */
  def countByCores(projectName: String, userName: String): TaskCoreNumVo

  /**
   * Version detail information
   * @param jobId job id
   * @param version version
   */
  def versionDetail(jobId: Long, version: String): VersionDetailVo

  /**
   * Update version
   * @param preVersion version
   */
  def updateVersion(preVersion: String): String

  /**
   * Upload files
   * @param metaJsonInfo meta json
   * @param version version
   * @param path path
   */
  def uploadFiles(metaJsonInfo: MetaJsonInfo, version: StreamJobVersion, path: String): Unit

  /**
   * Create stream job
   * @param metaJsonInfo meta json
   * @param userName username
   * @return
   */
  def createStreamJob(metaJsonInfo: MetaJsonInfo, userName: String): StreamJobVersion

  /**
   * Upload job
   * @param projectName project name
   * @param userName username
   * @param inputZipPath input zip path
   * @return
   */
  def uploadJob(projectName: String, userName: String, inputZipPath: String): StreamJobVersion

  /**
   * Create or update job with meta json
   * @param userName username
   * @param metaJsonInfo meta json
   * @return
   */
  def createOrUpdate(userName: String, metaJsonInfo: MetaJsonInfo): StreamJobVersion

  /**
   * Get job content
   * @param jobId job id
   * @param version version
   * @return
   */
  def getJobContent(jobId: Long, version: String): StreamisTransformJobContent

  /**
   * Has permission
   * @param jobId job id
   * @param username username
   * @return
   */
  def hasPermission(jobId: Long, username: String): Boolean

  def hasPermission(job: StreamJob, username: String): Boolean

  /**
   * Alert user
   * @param job stream job
   * @return
   */
  def getAlertUsers(job: StreamJob): util.List[String]

  /**
   * Alert level
   * @param job stream job
   * @return
   */
  def getAlertLevel(job: StreamJob): AlertLevel

  /**
   * Is creator
   * @param jobId job id
   * @param username username
   * @return
   */
  def isCreator(jobId: Long, username: String): Boolean

  /**
   * List alert message list
   * @param username username
   * @param jobId job id
   * @param version version
   * @return
   */
  def getAlert(username: String, jobId: Long, version: String): util.List[StreamAlertRecord]
}

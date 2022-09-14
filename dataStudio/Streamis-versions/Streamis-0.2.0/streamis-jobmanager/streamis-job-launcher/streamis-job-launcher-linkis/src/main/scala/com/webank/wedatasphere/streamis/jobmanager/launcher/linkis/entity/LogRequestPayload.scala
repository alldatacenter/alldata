package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.entity

/**
 *
 * @date 2021-11-10
 * @author enjoyyin
 * @since 0.5.0
 */
class LogRequestPayload {

  private var pageSize = 100
  private var fromLine = 1
  private var ignoreKeywords: String = _
  private var onlyKeywords: String = _
  private var lastRows = 0
  private var logType: String = _
  def getPageSize: Int = pageSize
  def setPageSize(pageSize: Int): Unit = this.pageSize = pageSize

  def getFromLine: Int = fromLine
  def setFromLine(fromLine: Int): Unit = this.fromLine = fromLine

  def getIgnoreKeywords: String = ignoreKeywords
  def setIgnoreKeywords(ignoreKeywords: String): Unit = this.ignoreKeywords = ignoreKeywords

  def getOnlyKeywords: String = onlyKeywords
  def setOnlyKeywords(onlyKeywords: String): Unit = this.onlyKeywords = onlyKeywords

  def getLastRows: Int = lastRows
  def setLastRows(lastRows: Int): Unit = this.lastRows = lastRows

  def getLogType: String = logType

  def setLogType(logType: String): Unit = this.logType = logType
}

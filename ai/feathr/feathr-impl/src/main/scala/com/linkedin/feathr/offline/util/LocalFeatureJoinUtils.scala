package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.util.AclCheckUtils.getLatestPath
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.io.File
import java.nio.file.Paths

private[offline] object LocalFeatureJoinUtils {
  val EMPTY_STRING = ""
  private val defaultMockDataBaseDir = "src/test/resources/mockdata/"

  /**
   * get the path to store mock data for a source path
   * @param path path to mock
   * @return generated path to store the mock data
   */
  def getMockPath(path: String, mockDataBaseDir: Option[String] = None): String = {
    val baseDir = mockDataBaseDir.getOrElse(defaultMockDataBaseDir)
    if (path.startsWith(baseDir)) path
    else baseDir + Paths.get(path)
  }

  /**
   *  get the original path from mock path
   * @param path mock path
   * @return original path
   */
  def getOriginalFromMockPath(path: String, mockDataBaseDir: Option[String] = None): String = {
    val baseDir = mockDataBaseDir.getOrElse(defaultMockDataBaseDir)
    path.replace(baseDir, "")
  }

  /**
   * get the mock data path for a source, if the mock data exist, otherwise return None
   */
  def getMockPathIfExist(sourcePath: String, hadoopConf: Configuration, mockDataBaseDir: Option[String] = None): Option[String] = {
    val cleanedSourcePath = stripProtocol(sourcePath)
    val mockSourcePath = getMockPath(cleanedSourcePath, mockDataBaseDir)
    exist(hadoopConf, mockSourcePath)
  }

  /**
   * For local testing, we need to strip protocol name. The rest of the {@link path} should be the actual directory.
   * @param path Source path provided in the source config.
   * @return A cleaned source path without protocol.
   */
  def stripProtocol(path: String): String = {
    if (path.startsWith("abfss:/")) {
      // For example, abfss://azuredev.dfs.core.windows.net/xyz/green_tripdata_2021-01.csv
      // will become azuredev.dfs.core.windows.net/xyz/green_tripdata_2021-01.csv
      path.replace("abfss://", "")
    } else {
      path
    }
  }

  /**
   * Rewrite source path
   * @param mockDataBaseDir base direction to store mock data locally
   * @param path path to rewrite
   * @return rewritten source path
   */
  def rewriteSourcePath(mockDataBaseDir: String, path: String, sourceId: Long): String = {
    mockDataBaseDir + "/" + path
  }

  /**
   * Check if the mock source path exist or not
   * @param hadoopConf conf
   * @param mockSourcePath mock source path to check
   * @return if exist, return the resolved path, otherwise None
   */
  private def exist(hadoopConf: Configuration, mockSourcePath: String): Option[String] = {
    val path = new Path(mockSourcePath)
    val fs = path.getFileSystem(hadoopConf)
    val resolvedPathName = getLatestPath(fs, mockSourcePath)
    if (new File(resolvedPathName).exists()) Some(resolvedPathName) else None
  }
}

package com.linkedin.feathr.offline.testfwk

import com.linkedin.feathr.offline.source.SourceFormatType.SourceFormatType
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.SourceUtils.{FEATURE_MP_DEF_CONFIG_BASE_PATH, FEATURE_MP_DEF_CONFIG_SUFFIX}
import com.linkedin.feathr.offline.util.{AclCheckUtils, IncrementalAggUtils, LocalFeatureJoinUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Interval}

import java.nio.file.{Files, Paths}
import scala.io.Source

object TestFwkUtils {

  private val conf: Configuration = new Configuration()
  val EMPTY_STRING = ""
  val DATE_PARTITION = "datePartition"
  val LOCATION = "location"
  val EXCEPTION = "exception"
  val SOURCES = "sources"
  val PATH = "path"
  val HDFS_PATH = "hdfsPath"
  val AVRO_SCHEMA = "avroSchema"
  val JSON_DATA = "jsonData"
  val DATA = "data"
  val EXPECT_OUTPUT = "expectOutput"
  val FEATURE_DEF = "featureDef"
  val DESCRIPTION = "description"
  val JOIN_CONFIG = "joinConfig"
  val JOB_PARAMETER = "jobParameter"
  val PASSTHROUGH = "PASSTHROUGH"
  var IS_DEBUGGER_ENABLED = false
  var DERIVED_FEATURE_COUNTER = 10

  /**
   * Rewrite a path that contains LATEST to be used in generating mock data
   * If the path does not have LATEST, simply return it.
   * Otherwise, if the path has one LATEST, it assumes to be yyyyMMdd,
   * if the path has 3 LATEST, it assumes to be yyyy/MM/dd.
   * @param path path to rewrite
   * @param mockDataBaseDir mock data base directory
   * @return rewritten path
   */
  private def rewriteTimePath(path: String, mockDataBaseDir: Option[String]): String = {
    if (path.contains(AclCheckUtils.LATEST_PATTERN)) {
      val targetPath = LocalFeatureJoinUtils.getMockPath(path.replaceAll(AclCheckUtils.LATEST_PATTERN, ""), mockDataBaseDir)
      if (!Files.exists(Paths.get(targetPath)) || !Files.isDirectory(Paths.get(targetPath))) {
        val occurrence = AclCheckUtils.countOccurrences(path, AclCheckUtils.LATEST_PATTERN)
        val localDate = new DateTime()
        occurrence match {
          case 0 =>
            path
          case 1 =>
            val dtfOut = DateTimeFormat.forPattern("yyyyMMdd")
            val latestDateTimeString = "/" + dtfOut.print(localDate.toLocalDate)
            path.replace(AclCheckUtils.LATEST_PATTERN, latestDateTimeString)
          case 3 =>
            val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")
            val latestDateTimeString = "/" + dtfOut.print(localDate.toLocalDate)
            val pattern = "/" + AclCheckUtils.LATEST_PATTERN + "/" + AclCheckUtils.LATEST_PATTERN + "/" + AclCheckUtils.LATEST_PATTERN
            path.replace(pattern, latestDateTimeString)
          case _ =>
            throw new RuntimeException(s"cannot rewrite path ${path}")
        }
      } else {
        val subPaths = IncrementalAggUtils.getSubfolderPaths(targetPath)
        if (subPaths.nonEmpty) {
          LocalFeatureJoinUtils.getOriginalFromMockPath(targetPath, mockDataBaseDir)
        } else if (path.stripSuffix("/").endsWith("/daily")) {
          throw new RuntimeException(s"Seems folder ${targetPath} is incomplete, please delete it and try again")
        } else {
          path
        }
      }
    } else {
      path
    }
  }

  // return a time range for a source type, used to decide which days to generate mock data on
  private def getMockDataTimeInterval(sourceType: SourceFormatType, end: DateTime): Option[Interval] = {
    sourceType match {
      case SourceFormatType.TIME_SERIES_PATH =>
        val start = end.minusDays(1)
        val interval = new Interval(start, end)
        Some(interval)
      case _ => None
    }
  }

  // return the mock param for a source
  // if the mock data exist, return the parameter with existing timestamp
  private[feathr] def createMockParam(source: DataSource, date: DateTime = new DateTime, mockDataBaseDir: Option[String]): SourceMockParam = {
    val rewritePath = rewriteTimePath(source.path, mockDataBaseDir)
    val mockBasePath = mockDataBaseDir.map(_ + "/")
    val mockPath = LocalFeatureJoinUtils.getMockPath(rewritePath, mockBasePath)
    val dateParamInterval = getMockDataTimeInterval(source.sourceType, date)
    new SourceMockParam(mockPath, dateParamInterval)
  }

  /**
   * Gets all the file name under a path
   * @param basePath The basePath where to search for files
   * @param includeSuffixList The suffix that target file name must have, if empty, will return all files under the folder
   * @return A List[String] containing the paths of the file names
   */
  private[feathr] def getFileNamesInSubFolder(basePath: String, includeSuffixList: Seq[String] = List(), conf: Configuration = conf): Seq[String] = {
    val fs: FileSystem = FileSystem.get(conf)
    val filesNames: Seq[String] = fs
      .listStatus(new Path(basePath))
      .filter(_.isFile)
      .map(_.getPath.getName)
    val filteredFilenames =
      filesNames.filter(dirName => includeSuffixList.isEmpty || includeSuffixList.exists(suffix => dirName.endsWith(suffix)))
    filteredFilenames
  }

  /**
   * Get the feathr feature definition conf file content from dependent feature repo.
   * @return feature definition conf file as an optional string
   */
  private[feathr] def getFeathrConfFromFeatureRepo(): Option[String] = {
    try {
      val featureMPDefConfigPaths = getFileNamesInSubFolder(FEATURE_MP_DEF_CONFIG_BASE_PATH, Seq(FEATURE_MP_DEF_CONFIG_SUFFIX))
        .map(fileName => FEATURE_MP_DEF_CONFIG_BASE_PATH + "/" + fileName)
      if (featureMPDefConfigPaths.nonEmpty) {
        Some(readLocalConfFileAsString(featureMPDefConfigPaths.mkString(",")))
      } else {
        None
      }
    } catch {
      case _: Exception =>
        None
    }
  }

  /**
   * read local conf file(s) as string
   * @param paths path(s) of local conf files, concatenated by ','
   * @return the conf files as a string
   */
  private[feathr] def readLocalConfFileAsString(paths: String): String = {
    paths
      .split(",")
      .map { path =>
        val bufferedSource = Source.fromFile(path)
        val content = bufferedSource.mkString
        bufferedSource.close
        content
      }
      .mkString("\n")
  }

  val NULLABLE_SUFFIX = "*"
  val FIELD = "field"
  val FEATURE = "feature"
  val SAVETO = "saveTo"
  val FIELD_SEPARATOR: String = ":"
}

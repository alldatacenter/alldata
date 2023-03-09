package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.time.TimeUnit
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.permission.{AclEntry, AclEntryScope, AclEntryType, FsAction}
import org.apache.hadoop.fs.{FileSystem, Path, PathFilter}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.SparkSession

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

/**
 * Check read or write authorization for a given path or path list.
 * 1. Support access control list(ACL).
 * 2. Support path with one or multiple "#LATEST"
 */
private[offline] object AclCheckUtils {
  // Designation to specify the latest subdirectory
  val LATEST_PATTERN = "#LATEST"
  private val log = LogManager.getLogger(getClass)

  // Check read authorization on all paths in a given list
  def checkReadAuthorization(conf: Configuration, pathList: Seq[String]): Seq[(String, String)] = {
    (for (path <- pathList) yield (path, checkReadAuthorization(conf, path))) collect {
      case (path, Failure(e)) => (path + " , " + e.getMessage, path)
    }
  }

  // Check read authorization on a path string
  def checkReadAuthorization(conf: Configuration, pathName: String): Try[Unit] = {
    // no way to check jdbc auth yet
    if (pathName.startsWith("jdbc:")) {
      Success(())
    } else {
      val path = new Path(pathName)

      if (pathName.startsWith("hdfs") || pathName.startsWith("/")) {
        // check authorization for HDFS path
        val fs = path.getFileSystem(conf)
        val resolvedPathName = getLatestPath(fs, pathName)
        val resolvedPath = new Path(resolvedPathName)
        Try(fs.access(resolvedPath, FsAction.READ))
      } else {
        /*
         * Skip relative paths
         * 1. Relative path is only available for RawLocalFileSystem, and paths on HDFS are always absolute paths
         * 2. Here skip authorization check for relative paths used in some unit tests not designed for current function
         */
        Success(())
      }
    }
  }

  // check read authorization on all required features
  def checkReadAuthorization(
      ss: SparkSession,
      allRequiredFeatures: Seq[common.ErasedEntityTaggedFeature],
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource]): (Try[Unit], Seq[String]) = {

    val conf = ss.sparkContext.hadoopConfiguration
    val allRequiredPaths = for {
      requiredFeature <- allRequiredFeatures
      featureAnchorWithSource <- allAnchoredFeatures.get(requiredFeature.getFeatureName)
    } yield featureAnchorWithSource.source.path

    val shouldSkipFeature = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SKIP_MISSING_FEATURE).toBoolean
    val invalidPaths = AclCheckUtils.checkReadAuthorization(conf, allRequiredPaths.distinct)
    if (invalidPaths.isEmpty) {
      (Success(()), invalidPaths.map(_._2))
    } else {
      if (!shouldSkipFeature) {
        (Failure(
          new RuntimeException(
            "Can not verify read authorization on the following paths. This can be due to" +
              " 1) the user does not have correct ACL, 2) path does not exist, 3) IO exception when reading the data :\n" +
              invalidPaths.map(_._1).mkString("\n"))), invalidPaths.map(_._2))
      } else {
        (Success(()), invalidPaths.map(_._2))
      }
    }
  }

  // Check write authorization on a path string, i.e., check write and execute authorization on its parent path
  def checkWriteAuthorization(conf: Configuration, pathName: String): Try[Unit] = {
    val path = new Path(pathName)
    if (pathName.startsWith("hdfs") || pathName.startsWith("/")) {
      // check authorization for HDFS path
      val fs = path.getFileSystem(conf)
      val resolvedPathName = getLatestPath(fs, pathName)
      val resolvedPath = new Path(resolvedPathName)
      val parentPath = resolvedPath.getParent
      getNearestAncestor(fs, parentPath).flatMap(path => Try(fs.access(path, FsAction.WRITE_EXECUTE)))
    } else {
      /*
       * Skip relative paths
       * 1. Relative path is only available for RawLocalFileSystem, and paths on HDFS are always absolute paths
       * 2. Here skip authorization check for relative paths used in some unit tests not designed for current function
       */
      Success(())
    }
  }

  // For a given input, return the nearest ancestor path that exists.
  private def getNearestAncestor(fs: FileSystem, path: Path): Try[Path] = {
    def recurse(p: Path): Try[Path] = {
      Try(fs.exists(p)).flatMap {
        case true => Success(p)
        case false => getNearestAncestor(fs, p.getParent)
      }
    }
    recurse(path)
  }

  /*
   * Parse #LATEST in URI.
   * Utility method that supports resolving multiple #LATEST in the path, e.g. /x/y/#LATEST/#LATEST/#LATEST
   * gets converted to /x/y/2018/11/15.
   *
   */
  def getLatestPath(fs: FileSystem, inputPath: String): String = {
    require(inputPath != null && !inputPath.isEmpty, "The path to resolve is either null or empty")

    if (inputPath.contains(LATEST_PATTERN)) {
      val split = inputPath.replaceAll(LATEST_PATTERN, "/" + LATEST_PATTERN).split(LATEST_PATTERN)
      var resolvedPath = split(0)
      for (i <- 1 until split.length) {
        resolvedPath = getLatestPathHelper(fs, resolvedPath) + split(i)
      }
      if (inputPath.endsWith(LATEST_PATTERN)) {
        resolvedPath = getLatestPathHelper(fs, resolvedPath)
      }
      resolvedPath
    } else {
      inputPath
    }
  }

  // For path ending in #LATEST, determines the correct directory that corresponds to the input path
  private def getLatestPathHelper(fs: FileSystem, inputPath: String): String = {
    lazy val filter = new PathFilter() {
      override def accept(path: Path): Boolean = !(path.getName.startsWith("_") || path.getName.startsWith("."))
    }
    val path = new Path(inputPath)
    if (fs.exists(path)) {
      val statuses = fs.listStatus(path, filter).sortWith((a, b) => a.compareTo(b) < 0)

      // if directory empty, return the directory's path
      if (statuses.isEmpty) {
        inputPath
      } else {
        inputPath + statuses.last.getPath.getName
      }
    } else inputPath
  }

  def countOccurrences(src: String, target: String): Int = src.sliding(target.length).count(window => window == target)

  /**
   * get the latest daily path with 3 #LATEST placeholders, i.e. /path/to/data/daily/#LATEST/#LATEST/#LATEST, which means
   * /path/to/data/daily/yyyy/MM/dd
   * @param fs
   * @param inputPath
   * @param cutOffDate the returned path should have date before cutOffDate
   * @return parsed time-based path, e.g. /path/to/data/daily/2018/05/13
   */
  def getLatestPath(fs: FileSystem, inputPath: String, cutOffDate: LocalDateTime): Option[String] = {
    require(inputPath != null && !inputPath.isEmpty, "The path to resolve is either null or empty")
    val count = countOccurrences(inputPath, LATEST_PATTERN)
    val timeUnits = Seq(TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.DAY, TimeUnit.HOUR)
    if (count != 3) {
      throw new FeathrException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"getLatestPath only support HDFS path with 3 $LATEST_PATTERN" +
          s" in yyyy/MM/dd, e.g., $LATEST_PATTERN/$LATEST_PATTERN/$LATEST_PATTERN, but found $inputPath ")
    } else {
      val refinedPath = inputPath.replaceAll(LATEST_PATTERN, "/" + LATEST_PATTERN)
      val initTime = LocalDateTime.of(0, 1, 1, 0, 0)
      val resolvedTime = ResolvedTime(initTime)
      getLatestPathRecursive(fs, refinedPath, timeUnits, 0, initTime, cutOffDate, resolvedTime)
      if (!resolvedTime.time.equals(initTime)) {
        val yearStr = padWithPrefix(resolvedTime.time.getYear().toString, 4, '0')
        val monthStr = padWithPrefix(resolvedTime.time.getMonthValue.toString, 2, '0')
        val dayStr = padWithPrefix(resolvedTime.time.getDayOfMonth.toString, 2, '0')
        val latestPath = refinedPath.replaceFirst(LATEST_PATTERN, yearStr).replaceFirst(LATEST_PATTERN, monthStr).replaceFirst(LATEST_PATTERN, dayStr)
        Some(latestPath)
      } else {
        None
      }
    }
  }

  // pad the input string to a fixed length of len by adding pad as prefix
  private def padWithPrefix(input: String, len: Int, pad: Char): String = {
    input.reverse.padTo(len, pad).reverse
  }

  /**
   * Helper function for getLatestPath, resolve the latest time before cutOffDateTime for the given inputPath,
   * which contains multiple #LATEST placeholder. The output time will be stored in resolvedOutputTime.
   * @param fs file system
   * @param inputPath path to resolve #LATEST, e.g. /path/to/data/daily/#LATEST/#LATEST/#LATEST
   * @param timeUnits sequence of TimeUnits to resolve (in order), e.g. Seq(TimeUnit.YEAR, imeUnit.MONTH, imeUnit.DAY)
   *                  means resolve path in yyyy/MM/dd
   * @param index index of timeUnits, which defines the TimeUnit in the timeUnits the function is currently resolving
   * @param currentFolderDateTime current resolved date time
   * @param cutOffDateTime only find path with time before cutoffdateTime
   * @param resolvedOutputTime the output time, store the resolved 'latest' time before cutOffDateTime
   */
  private def getLatestPathRecursive(
      fs: FileSystem,
      inputPath: String,
      timeUnits: Seq[TimeUnit],
      index: Int,
      currentFolderDateTime: LocalDateTime,
      cutOffDateTime: LocalDateTime,
      resolvedOutputTime: ResolvedTime): Unit = {
    lazy val filter = new PathFilter() {
      override def accept(path: Path): Boolean = !(path.getName.startsWith("_") || path.getName.startsWith("."))
    }
    val baseInputPath = inputPath.replaceAll(LATEST_PATTERN, "")
    // the 'ls' command in file system, i.e. get all subfolders under baseInputPath
    try {
      val statuses = fs.listStatus(new Path(baseInputPath), filter).sortWith((a, b) => a.compareTo(b) < 0)

      if (!statuses.isEmpty) {
        statuses.foreach(status => {
          val timeStr = status.getPath.getName
          val timeInt = Integer.parseInt(timeStr)
          val resolvedPath = inputPath.replaceFirst(LATEST_PATTERN, timeStr)
          // find what time unit we are currently resolving
          val resolvedTime = timeUnits(index) match {
            case TimeUnit.YEAR =>
              currentFolderDateTime.withYear(timeInt)
            case TimeUnit.MONTH =>
              currentFolderDateTime.withMonth(timeInt)
            case TimeUnit.DAY =>
              currentFolderDateTime.withDayOfMonth(timeInt)
          }
          val allPlaceholdersResolved = !resolvedPath.contains(LATEST_PATTERN)
          if (allPlaceholdersResolved && resolvedTime.isBefore(cutOffDateTime) && resolvedTime.isAfter(resolvedOutputTime.time)) {
            // if a closer date to cutOffTime, update the final result
            resolvedOutputTime.time = resolvedTime
          }
          if (resolvedPath.contains(LATEST_PATTERN) && cutOffDateTime.isAfter(resolvedTime)) {
            // resolve next level time unit, i.e. level of index + 1
            getLatestPathRecursive(fs, resolvedPath, timeUnits, index + 1, resolvedTime, cutOffDateTime, resolvedOutputTime)
          }
        })
      }
    } catch {
      case e: Exception => log.trace(s"Unsupported path found under ${inputPath}" + e.getMessage)
    }
  }

  def aclEntry(scope: AclEntryScope, aclEntryType: AclEntryType, name: String, permission: FsAction): AclEntry = {
    new AclEntry.Builder().setScope(scope).setType(aclEntryType).setName(name).setPermission(permission).build()
  }

  def aclEntry(scope: AclEntryScope, aclEntryType: AclEntryType, permission: FsAction): AclEntry = {
    new AclEntry.Builder().setScope(scope).setType(aclEntryType).setPermission(permission).build()
  }
}
// wrapper of output result for getLatestPath
private case class ResolvedTime(var time: LocalDateTime)

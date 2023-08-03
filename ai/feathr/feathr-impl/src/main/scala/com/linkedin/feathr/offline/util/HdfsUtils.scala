package com.linkedin.feathr.offline.util

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path,LocatedFileStatus,FileSystem,PathFilter,RemoteIterator}
import org.apache.log4j.{Logger, PatternLayout, WriterAppender}

import java.io.{FileSystem => _, _}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import scala.annotation.tailrec


object HdfsUtils {

  /**
   * Default Hadoop Configuration used to interact with HDFS
   */
  private val conf: Configuration = new Configuration()

  /**
   * DateTimeFormatter used to format strings on the form yyyy-MM-dd
   */
  val dateStampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
   * DateTimeFormatter used to format strings on the form yyyy-MM-dd-HH-mm
   */
  val timeStampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

  /**
   * PST ZoneId
   */
  val pstZoneId: ZoneId = ZoneId.of("America/Los_Angeles")

  /**
   * DateTimeFormatter used to form the commonly-used path for a date strings.
   */
  val dailyDirFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("'daily'/yyyy/MM/dd")

  /**
   * Identifier for latest subdirectory.
   * Used by [[getLatestPath()]]
   * e.g. "/data/path/#LATEST"
   */
  val latestSubdirectory : String = "#LATEST"

  /**
   * Identifier for using a datetime in the path
   * Used by [[replaceTimestamp()]]
   * e.g. "/output/path/#TIMESTAMP/data" || "/output/path/#TIMESTAMP/metadata"
   */
  val timestampIdentifier: String = "#TIMESTAMP"

  /**
   * Default number of attempts to create a temporary file
   */
  val maxAttempts = 3

  /**
   * Returns PrintStream from path
   */
  def getPrintStream(path: String): PrintStream = {
    val p = new Path(path)
    new PrintStream(
      new BufferedOutputStream(p.getFileSystem(conf).create(p)),
      false, "UTF-8")
  }

  /**
   * Creates a File Path given one or multiple parts and returns it normalized
   * (no multiple consecutive or trailing "/")
   * @param pathParts  A Varargs with the parts used to create the String path
   * @return A normalized (no multiple consecutive or trailing "/") String path
   *         given for the provided path parts
   */
  def createStringPath(pathParts: String*): String = {
    new File(pathParts.map(_.trim).mkString("/")).toString
  }

  /**
   * Validate a path to be resolved. An inputPath:
   *  - Cannot be null
   *  - Cannot be empty or have only white spaces
   *  - Cannot start with the identifier
   *  - Cannot start with the identifier as the first component (e.g. "/#LATEST")
   *  - Cannot contain two consecutive identifiers without any characters in between (e.g. BAD: "/path/#LATEST#LATEST", GOOD: "/path/#LATEST/#LATEST"
   * @param inputPath The String path to be validated
   * @param identifier The identifier (e.g. "#LATEST")
   */
  private def validatePath(inputPath: String, identifier: String) = {
    require(inputPath != null,
      "The path to resolve cannot be null")
    require(!inputPath.trim.isEmpty,
      "The path to resolve cannot be empty or contain only white spaces")
    require(!inputPath.startsWith(identifier),
      "The path to resolve cannot start with the identifier as the first element")
    require(!inputPath.startsWith(s"/$identifier"),
      "The path to resolve cannot start with the identifier as the first element")
    require(!inputPath.contains(identifier + identifier),
      "The path to resolve cannot contain two consecutive identifiers with no characters in between")
  }

  /**
   * Replaces the [[latestSubdirectory]] identifier with the latest subdirectory in the path.
   * The [[latestSubdirectory]] identifier can appear multiple times with or without leading/training "/"
   * An inputPath must not be null or empty. It might not contain the identifier.
   *
   * @param inputPath The inputPath
   * @param conf Config
   * @return A normalized (no multiple consecutive or trailing "/") String with all the identifiers replaced with the latest subdirectory.
   */
  def getLatestPath(inputPath: String, conf: Configuration = conf): String = {
    validatePath(inputPath, latestSubdirectory)

    val workingPath = inputPath + "/"
    val pathParts: Array[String] = workingPath.split(latestSubdirectory)

    var resultPath = pathParts(0)
    for (partIdx <- 1 until pathParts.length) {
      val latestPath = getSortedSubfolderPaths(basePath = resultPath, conf = conf).headOption
      if (!latestPath.isDefined) {
        throw new RuntimeException(s"${resultPath} does not contain any valid subdirectories")
      }
      resultPath = HdfsUtils.createStringPath(latestPath.get, pathParts(partIdx))
    }

    HdfsUtils.createStringPath(resultPath)
  }

  /**
   * Replaces the [[timestampIdentifier]] identifier with the given LocalDateTime in the provided format
   * It could be used to partition different outputs of a flow within the same run and have a nicer folder structure. e.g.
   *  - /path/2016-05-25-10-15/trainingDataset
   *  - /path/2016-05-25-10-15/testDataset
   *  - /path/2016-05-25-10-15/scoringDataset
   *
   * NOTE: The localDateTime defaults to UTC. To conform to existing datasets (e.g. Tracking Data) you might want to
   *   use the Pacific time zone instead (e.g. `replaceTimestamp("path", LocalDateTime.now(pstZoneId))`)
   *
   * @param inputPath                The input path
   * @param localDateTime            The LocalDateTime object used to replace the [[timestampIdentifier]].
   *                                 Defaults to current time in UTC and can be overridden (e.g. `replaceTimestamp("path", LocalDateTime.now(pstZoneId))`)
   * @param dateTimeFormatterPattern The DateTimeFormatter used to format the localDateTime above. Defaults to [[timeStampFormatter]]
   * @return A normalized (no multiple consecutive or trailing "/") String with all the identifiers replaced with the provided localDateTime in the given format
   */
  def replaceTimestamp(inputPath: String,
                       localDateTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
                       dateTimeFormatterPattern: DateTimeFormatter = timeStampFormatter): String = {
    validatePath(inputPath, timestampIdentifier)

    val dateTimeString = localDateTime.format(dateTimeFormatterPattern)
    val resultPath = inputPath.replaceAll(timestampIdentifier, s"/$dateTimeString/")

    HdfsUtils.createStringPath(resultPath)
  }

  /**
   * An object for defining the date/hour formats in the dated directories.
   * This object is used by getPaths to format the returned paths.
   * An example of how this object can be overridden to change the format exists in the test.
   */
  object TemporalPathFormats {
    trait TemporalPathFormat[T <: ChronoUnit] {
      val formatter: DateTimeFormatter
    }
    implicit object Daily extends TemporalPathFormat[ChronoUnit.DAYS.type] {
      override val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    }
    implicit object Hourly extends TemporalPathFormat[ChronoUnit.HOURS.type] {
      override val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")
    }
  }

  /**
   * Returns a Seq of paths that are prefixed with basePath, and that cover the
   * period [startInclusive, endExclusive). The dates are formatted using
   * TemporalPathFormats or any object similarly defined.
   * @param basePath The prefix path under which the dated directories exist.
   * @param startInclusive The start time (e.g., 2017/01/01). Included in the returned paths.
   * @param endExclusive The end time (e.g., 2017/01/03). Not included in the returned paths.
   * @param unit The time unit (e.g., ChronoUnit.DAYS).
   * @tparam T The type of the unit (e.g., ChronoUnit.DAYS.type)
   * @return A Seq of paths under basePath covering the period [startInclusive, endExclusive).
   */
  def getPaths(
                basePath: String,
                startInclusive: LocalDateTime,
                endExclusive: LocalDateTime,
                unit: ChronoUnit): Seq[String] = {
    val formatter = unit match {
      case ChronoUnit.DAYS =>
        TemporalPathFormats.Daily.formatter
      case ChronoUnit.HOURS =>
        TemporalPathFormats.Hourly.formatter
      case _ =>
        throw new IllegalArgumentException(s"Unsupported path resolution unit: ${unit}")
    }
    getPaths(basePath, startInclusive, endExclusive, unit, formatter)
  }

  /**
   * Returns a Seq of paths that are prefixed with basePath, and that cover the
   * period [startInclusive, endExclusive). The dates are formatted using formatter.
   * @param basePath The prefix path under which the dated directories exist.
   * @param startInclusive The start time (e.g., 2017/01/01). Included in the returned paths.
   * @param endExclusive The end time (e.g., 2017/01/03). Not included in the returned paths.
   * @param unit The time unit (e.g., ChronoUnit.DAYS).
   * @param formatter A DateTimeFormatter for formatting the dates in the paths.
   * @return A Seq of paths under basePath covering the period [startInclusive, endExclusive).
   */
  def getPaths(
                basePath: String,
                startInclusive: LocalDateTime,
                endExclusive: LocalDateTime,
                unit: ChronoUnit,
                formatter: DateTimeFormatter): Seq[String] = {
    getTemporalRange(startInclusive, endExclusive, unit)
      .map { time => createStringPath(basePath, formatter.format(time)) }
  }

  /**
   * Strips hdfs://..., and similar prefixes.
   * Example: stripPrefix("hdfs://xyz.linkedin/a/b/c/foo/bar", "/a/b/c/") =>
   *          "/a/b/c/foo/bar"
   */
  def stripPrefix(prefixedPath: String, basePath: String): String = {
    if (prefixedPath.isEmpty || basePath.isEmpty) {
      throw new IllegalArgumentException(
        s"At least one of the paths prefixedPath ($prefixedPath) " +
          s"or basePath ($basePath) is empty.")
    }
    prefixedPath.substring(prefixedPath.indexOfSlice(basePath))
  }

  /**
   * Returns a boolean values to test if the file or directory name starts with
   * any character in the excludeFilesPrefixList.
   * @param path The file/directory path that needs to be analysed for filtering.
   * @param excludeFilesPrefixList Sequence of file prefixes that needs to be
   *                                excluded while reading.
   * @return boolean (true) if the file or directory starts with any character in
   * excludeFilesPrefixList, false otherwise.
   */
  def filterPath(path: String, excludeFilesPrefixList: Seq[String] = List(".", "_")): Boolean = {
    val lastComponent = path.split("/").last
    excludeFilesPrefixList.exists(prefix => lastComponent.startsWith(prefix))
  }

  /**
   * Gets a listing of all files rooted at the given directory.
   * Files starting with a prefix in excludeFilesPrefixList are excluded.
   * By default, the function filters out the files/directories
   * that starts with either `.` or `_`.
   */
  def listFiles(path: String,
                recursive: Boolean = true,
                excludeFilesPrefixList: Seq[String] = List(".", "_"),
                conf: Configuration = conf): Seq[String] = {

    @tailrec
    def traverseIterator(path: String,
                         iter: RemoteIterator[LocatedFileStatus],
                         accumulator: List[String],
                         conf: Configuration = conf): Seq[String] = {
      if (!iter.hasNext) {
        accumulator
      } else {
        val file: Path = iter.next.getPath
        if (!filterPath(file.toString, excludeFilesPrefixList)) {
          traverseIterator(
            path, iter,
            stripPrefix(file.toString, path) :: accumulator,
            conf)
        } else {
          traverseIterator(path, iter, accumulator, conf)
        }
      }
    }

    val iter = FileSystem.get(conf).listFiles(new Path(path), recursive)
    traverseIterator(path, iter, List.empty[String], conf)
  }

  /**
   * Returns all files under path with a given fileType.
   * Pass empty fileType to return all files.
   * If errorOnMissingFiles is set to true, a FileNotFoundException
   * exception is thrown if the path does not have files of type fileType.
   * If a file, or any of its parent directories under path, starts with
   * any char in excludeFilesPrefixList, it will be excluded.
   */
  def getAllFilesOfGivenType(path: String,
                             fileType: String = "",
                             recursive: Boolean = true,
                             errorOnMissingFiles: Boolean = true,
                             excludeFilesPrefixList: Seq[String] = List(".", "_"),
                             conf: Configuration = conf): Seq[String] = {
    def filterFileName(fileName: String): Boolean = {
      val lastComponent = fileName.split("/").last
      fileName.endsWith(fileType)
    }
    val files = listFiles(path, recursive, excludeFilesPrefixList, conf)
      .filter(filterFileName)
    if (errorOnMissingFiles && files.isEmpty) {
      throw new FileNotFoundException(
        s"There are no files of type ${fileType} under the path ${path}.")
    }
    files
  }

  /**
   * Returns a Seq of LocalDateTime that cover the period [startInclusive, endExclusive).
   * Throws an IllegalArgumentException if startInclusive or endExclusive are not at
   * the boundary of unit, or if startInclusive >= endExclusive.
   * @param startInclusive The start time (e.g., 2017/01/01). Included in the returned paths.
   * @param endExclusive The end time (e.g., 2017/01/03). Not included in the returned paths.
   * @param unit The time unit (e.g., ChronoUnit.DAYS).
   * @return A Seq of LocalDateTime covering the period [startInclusive, endExclusive).
   */
  private def getTemporalRange(
                                startInclusive: LocalDateTime,
                                endExclusive: LocalDateTime,
                                unit: ChronoUnit): Seq[LocalDateTime] = {
    if (startInclusive.truncatedTo(unit) != startInclusive) {
      throw new IllegalArgumentException(
        s"Invalid argument: The startInclusive (${startInclusive}}) " +
          s"should be at the boundary of unit (${unit}}).")
    }
    if (endExclusive.truncatedTo(unit) != endExclusive) {
      throw new IllegalArgumentException(
        s"Invalid argument: The endExclusive (${endExclusive}}) " +
          s"should be at the boundary of unit (${unit}}).")
    }
    if (!endExclusive.isAfter(startInclusive)) {
      throw new IllegalArgumentException(
        s"Invalid arguments: The startInclusive (${startInclusive}}) " +
          s"should be before endExclusive (${endExclusive}}).")
    }
    val temporalRange =
      for (step <- (0 until unit.between(startInclusive, endExclusive).toInt))
        yield startInclusive.plus(step, unit)
    if (temporalRange.isEmpty) {
      throw new IllegalArgumentException (
        s"Invalid config: The temporalRange (${temporalRange}}) " +
          s"is empty.")
    }
    if (temporalRange.distinct.length != temporalRange.length) {
      throw new IllegalArgumentException(
        s"Invalid config: The temporalRange (${temporalRange}}) " +
          s"has duplicate values.")
    }
    temporalRange
  }

  /**
   * Return a daily directory for an outputPath. Daily directories by default follow the
   * [path]/daily/yyyy/MM/dd format, but may be overriden.
   */
  def createDailyOutputPath(outputPath: String, localDateTime: LocalDateTime = LocalDateTime.now()): String = {
    appendDateFormatted(outputPath, dailyDirFormatter, localDateTime)
  }

  /**
   * Return a latest directory for an outputPath. Snapshot directories follow a timeStampFormatter containing
   * the year, month, day, and time of day.
   */
  def createLatestOutputPath(outputPath: String, localDateTime: LocalDateTime = LocalDateTime.now()): String = {
    appendDateFormatted(outputPath, timeStampFormatter, localDateTime)
  }

  /**
   * Append a date formatted string to the back of an output path
   * @param outputPath output path that should be appended to.
   * @param dateTimeFormatter DateTimeFormatter to use.
   * @return modified output path with a date representation.
   */
  def appendDateFormatted(outputPath: String, dateTimeFormatter: DateTimeFormatter,
                          localDateTime: LocalDateTime): String = {
    val dateFormattedExtension = dateTimeFormatter.format(localDateTime)

    HdfsUtils.createStringPath(outputPath, dateFormattedExtension)
  }

  /**
   * Deletes older paths. A path is old if there are at least `maxPathsToKeep` paths generated after it.
   * Paths are sorted in a consistent way with the [[getLatestPath()]] method: reverse lexicographic order
   * @param basePath The input path to clean up
   * @param maxPathsToKeep Max number of folders that will be kept in the `basePath`
   */
  def deleteOlderPaths(basePath: String, maxPathsToKeep: Int, conf: Configuration = conf): Unit = {
    require(maxPathsToKeep > 0, "maxPathToKeep must be greater than 0")
    val dirs: Seq[String] = getSortedSubfolderPaths(basePath)
    dirs.drop(maxPathsToKeep)       // Skip first `maxPathsToKeep` records
      .foreach(deletePath(_)) // Delete remaining (older) paths
  }

  /**
   * Gets all the subfolders paths in a given `basePath` sorted in reversed lexicographical order
   * @param basePath The basePath where to search for subfolders
   * @return A List[String] containing the paths of the subfolders sorted in reverse lexicographical order
   */
  private def getSortedSubfolderPaths(basePath: String,
                                      excludeDirsPrefixList: Seq[String] = List(".", "_"),
                                      conf: Configuration = conf): Seq[String] = {
    val fs: FileSystem = FileSystem.get(conf)
    val directories: Seq[String] = fs.listStatus(new Path(basePath))
      .filter(_.isDirectory)
      .map(_.getPath.getName)
      .filter(dirName =>
        !excludeDirsPrefixList.exists(prefix => dirName.startsWith(prefix)))
      .map(HdfsUtils.createStringPath(basePath, _))
      .sorted.reverse

    directories
  }

  /**
   * Deletes the given path in the FileSystem (typically HDFS) specified by the given configuration.
   * @param path The String path to be deleted
   * @param conf Configuration
   * @param recursive Whether to delete recursively or not. Defaults to true.
   * @return true if delete is successful else false.
   */
  def deletePath(path: String, recursive: Boolean = true, conf: Configuration = conf):
  Boolean = {
    FileSystem.get(conf).delete(new Path(path), recursive)
  }

  /**
   * Rename the src path to dst path in the FileSystem (typically HDFS) specified by the given configuration.
   * Rename could be a tricky action. please refer to the HDFS official documentation for behaviors.
   * E.g. hadoop-project-dist/hadoop-common/filesystem/filesystem.html
   *
   * @param src The src path as string
   * @param dst The dst path as string
   * @param conf Configuration
   * @return true if rename is successful else false.
   */
  def renamePath(src: String, dst: String, conf: Configuration = conf):
  Boolean = {
    FileSystem.get(conf).rename(new Path(src), new Path(dst))
  }

  /**
   * Checks if the given path in exists.
   * @param path The String path to check
   * @param conf Configuration
   * @return true if exists else false.
   */
  def exists(path: String, conf: Configuration = conf): Boolean = {
    FileSystem.get(conf).exists(new Path(path))
  }

  /**
   * For a given input path, provides sub folders.
   * @param conf Hadoop Configuration
   * @param inputPath input path
   * @return Array[String]
   */
  def hdfsSubdir(inputPath: String,
                 excludePathsPrefixList: Seq[String] = List(".", "_"),
                 conf: Configuration = conf): Array[String] = {
    val filter = new PathFilter() {
      override def accept(path : Path): Boolean =
        !excludePathsPrefixList.exists(prefix => path.getName.startsWith(prefix))
    }

    val fs = FileSystem.get(conf)
    val subDirs = fs.listStatus(new Path(inputPath), filter)
      .filter(_.isDirectory)
      .map(_.getPath.getName)
    if (subDirs.nonEmpty) {
      subDirs.flatMap(dir =>
        hdfsSubdir(inputPath + '/' + dir, excludePathsPrefixList, conf))
    } else {
      Array(inputPath)
    }
  }

  /**
   * Creaete directories as needed
   */
  def hdfsCreateDirectoriesAsNeeded(dir: String, conf: Configuration = conf): Unit = {
    val fs: FileSystem = FileSystem.get(conf)
    val path = new Path(dir)
    if (!fs.exists(path)) {
      fs.mkdirs(path)
    }
  }

  /**
   * Returns temp file path
   */
  def hdfsCreateTempFile(User: String, tempDir: String, prefix: String, suffix: String,
                         attempts: Int = maxAttempts, conf: Configuration = conf):  // scalaStyle:off magic.number
  Option[String] = {
    val usePrefix = if (prefix.nonEmpty) prefix else "file"
    val useSuffix = if (suffix.nonEmpty) suffix else "tmp"

    def getTempFileName(index: Int): String =
      User + "-" + usePrefix + "-" + index + "." + useSuffix

    val fs: FileSystem = FileSystem.get(conf)

    val availableFiles = (1 until attempts)
      .dropWhile { index =>
        val tempFileName = getTempFileName(index)
        val path = new Path(createStringPath(tempDir, tempFileName))
        fs.exists(path) || !fs.createNewFile(path)
      }

    if (availableFiles.isEmpty) {
      None
    } else {
      val tempFileName = getTempFileName(availableFiles.head)
      val tempFilePath = createStringPath(tempDir, tempFileName)
      fs.deleteOnExit(new Path(tempFilePath))
      Some(tempFilePath)
    }
  }

  /**
   * Converts Iterator to InputStream on HDFS
   */
  def hdfsConvertIteratorToInputStream(User: String, tempDirPrefix: String,
                                       iter: Iterator[String], attempts: Int = maxAttempts, conf: Configuration = conf):
  Option[InputStream] = {
    val tempPathOpt = hdfsCreateTempFile(User, tempDirPrefix,
      "iter", "tmp", attempts)
    tempPathOpt match {
      case None => None
      case Some(tempPath) =>
        val printStream = getPrintStream(tempPath)
        iter.foreach { s => printStream.println(s) }
        printStream.close()
        Some(FileSystem.get(conf).open(new Path(tempPath)))
    }
  }

  /**
   * Set up to write logs to HDFS at given path.
   */
  def setupLoggerForHDFS(conf: Configuration, logger: Logger, path: String): Unit = {
    val simpleLogLayout = new PatternLayout("%m%n")
    val logStream = FileSystem.get(conf).create(new Path(path), true)
    val hdfsWriter = new WriterAppender(simpleLogLayout,
      new BufferedWriter(new OutputStreamWriter(logStream)))
    logger.addAppender(hdfsWriter)
  }
}

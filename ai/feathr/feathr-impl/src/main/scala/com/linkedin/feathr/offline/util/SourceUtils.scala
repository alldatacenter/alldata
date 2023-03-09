package com.linkedin.feathr.offline.util

import com.databricks.spark.avro.SchemaConverterUtils
import com.databricks.spark.avro.SchemaConverters.convertStructToAvro
import com.fasterxml.jackson.databind.ObjectMapper
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.exception._
import com.linkedin.feathr.common.{AnchorExtractor, DateParam}
import com.linkedin.feathr.offline.client.InputData
import com.linkedin.feathr.offline.config.location.{DataLocation, SimplePath}
import com.linkedin.feathr.offline.generation.SparkIOUtils
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.mvel.{MvelContext, MvelUtils}
import com.linkedin.feathr.offline.source.SourceFormatType
import com.linkedin.feathr.offline.source.SourceFormatType.SourceFormatType
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.source.dataloader.hdfs.FileFormat
import com.linkedin.feathr.offline.source.dataloader.jdbc.{JdbcUtils, SnowflakeUtils}
import com.linkedin.feathr.offline.source.pathutil.{PathChecker, TimeBasedHdfsPathAnalyzer, TimeBasedHdfsPathGenerator}
import com.linkedin.feathr.offline.util.AclCheckUtils.getLatestPath
import com.linkedin.feathr.offline.util.DelimiterUtils.checkDelimiterOption
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils
import org.apache.avro.generic.GenericData.{Array, Record}
import org.apache.avro.generic.{GenericDatumReader, GenericRecord, IndexedRecord}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.mapred.AvroKey
import org.apache.avro.mapreduce.{AvroJob, AvroKeyOutputFormat}
import org.apache.avro.specific.{SpecificDatumReader, SpecificRecord, SpecificRecordBase}
import org.apache.avro.util.Utf8
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapreduce.Job
import org.apache.logging.log4j.LogManager
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.avro.SchemaConverters
import org.apache.spark.sql.types.StructType
import org.joda.time.{Days, Hours, Interval, DateTime => JodaDateTime, DateTimeZone => JodaTimeZone}
import org.mvel2.MVEL

import java.io.{ByteArrayInputStream, DataInputStream, File}
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDateTime, ZoneId}
import java.util
import java.util.{Calendar, Date, TimeZone}
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._
import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Load "raw" not-yet-featurized data from HDFS data sets
 */
private[offline] object SourceUtils {
  val HDFS_PREFIX = "/" // HDFS path always starts with "/"
  private final val conf: Configuration = new Configuration()

  private val log = LogManager.getLogger(getClass)
  // this path is defined by the feathr plugin
  val FEATURE_MP_DEF_CONFIG_BASE_PATH = "feathr-feature-configs/config/offline"
  val FEATURE_MP_DEF_CONFIG_SUFFIX = ".conf"
  val firstRecordName = "topLevelRecord"

  /**
   * get AVRO datum type of a dataset we should use to load,
   * it is determined by the expect datatype from a set of anchor transformers
   * @param transformers  transformers that uses the dataset
   * @return datatype to use, either avro generic record or specific record
   */
  def getExpectDatumType(transformers: Seq[AnyRef] = Nil): Class[_] = {
    // AnchorExtractorSpark does not support getInputType, and this rdd will not be used for AnchorExtractorSpark
    val expectedRecordTypes = transformers.collect {
      case transformer: AnchorExtractor[_] => transformer.getInputType
    }
    // if the transformer expect a specific record, we need to load the dataset as rdd[specificRecord], so return specific record here,
    // otherwise, return GenericRecord, since we will just load it as rdd[GenericRecord]
    val expectDatumType = if (expectedRecordTypes.exists(classOf[SpecificRecord].isAssignableFrom)) {
      // Determine which SpecificRecord subclass we need to use
      val expectedSpecificTypes = expectedRecordTypes.filter(classOf[SpecificRecord].isAssignableFrom).distinct
      assert(expectedSpecificTypes.nonEmpty)
      require(
        expectedSpecificTypes.size == 1,
        s"Can't determine which SpecificRecord subclass to use; " +
          s"transformers $transformers seem to require more than one record type: $expectedSpecificTypes")
      expectedSpecificTypes.head
    } else if (expectedRecordTypes.exists(classOf[GenericRecord].isAssignableFrom)) {
      classOf[GenericRecord]
    } else {
      classOf[AnyRef]
    }
    expectDatumType
  }

  def getPathList(
                   sourceFormatType: SourceFormatType,
                   sourcePath: String,
                   ss: SparkSession,
                   dateParam: Option[DateParam],
                   dataLoaderHandlers: List[DataLoaderHandler],
                   targetDate: Option[String] = None,
                   failOnMissing: Boolean = true): Seq[String] = {
    sourceFormatType match {
      case SourceFormatType.FIXED_PATH => Seq(HdfsUtils.getLatestPath(sourcePath, ss.sparkContext.hadoopConfiguration))
      case SourceFormatType.TIME_PATH =>
        val pathChecker = PathChecker(ss, dataLoaderHandlers)
        val pathGenerator = new TimeBasedHdfsPathGenerator(pathChecker)
        val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(pathChecker, dataLoaderHandlers)
        val pathInfo = pathAnalyzer.analyze(sourcePath)
        pathGenerator.generate(pathInfo, OfflineDateTimeUtils.createTimeIntervalFromDateParam(dateParam, None, targetDate), !failOnMissing)
      case SourceFormatType.LIST_PATH => sourcePath.split(";")
      case _ =>
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          "Trying to get source path list. " +
            "sourceFormatType should be either FIXED_PATH or DAILY_PATH. Please provide the correct sourceFormatType.")
    }
  }

  /**
   * Returns the avro schema [[Schema]] of a given dataframe
   * @param dataframe input dataframe
   * @return  The avro schema type
   */
  def getSchemaOfDF(dataframe: DataFrame): Schema = {
    val builder = SchemaBuilder.record(firstRecordName).namespace("")
    convertStructToAvro(dataframe.schema, builder, firstRecordName)
  }

  /** estimate RDD size within x seconds(by default, 30s)
   * count could be REALLY slow,
   * WARNING: could possibly return INT64_MAX, if RDD is too big and timeout is too small
   * @param rdd
   * @param timeout timeout in milliseconds
   * @tparam L
   * @return estimate size of rdd
   */
  def estimateRDDRow[L](rdd: RDD[L], timeout: Int = 30000): Long = {
    var estimateSize = 0d
    val estimateSizeInterval = rdd.countApprox(timeout, 0.8)
    val (lowCnt, highCnt) = (estimateSizeInterval.initialValue.low, estimateSizeInterval.initialValue.high)
    estimateSize = (lowCnt + highCnt) / 2.0
    estimateSize.longValue()
  }

  /**
   * convert RDD[GenericRecord] To DataFrame using spark avro converter
   * @param ss sparksession
   * @param rdd rdd to convert
   * @param schema schema of rdd
   * @return converted dataframe
   */
  def convert(ss: SparkSession, rdd: RDD[GenericRecord], schema: Schema): DataFrame = {
    val sqlType = SchemaConverters.toSqlType(schema).dataType.asInstanceOf[StructType]
    val converter = SchemaConverterUtils.converterSql(schema, sqlType)
    val rowRdd = rdd.flatMap(record => {
      Try(converter(record).asInstanceOf[Row]).toOption
    })
    ss.createDataFrame(rowRdd, sqlType)
  }

  /**
   * write dataframe to a give hdfs path, this function is job-safe, e.g. other job will not be able to see
   * the output path until the write is completed, this is done via writing to a temporary folder, then finally
   * rename to the expected folder
   *
   * @param df         dataframe to write
   * @param dataPath   path to write the dataframe
   * @param parameters spark parameters
   * @param dataLoaderHandlers additional data loader handlers that contain hooks for dataframe creation and manipulation
   */
  def safeWriteDF(df: DataFrame, dataPath: String, parameters: Map[String, String], dataLoaderHandlers: List[DataLoaderHandler]): Unit = {
    val tempBasePath = dataPath.stripSuffix("/") + "_temp_"
    HdfsUtils.deletePath(dataPath, true)
    SparkIOUtils.writeDataFrame(df, SimplePath(tempBasePath), parameters, dataLoaderHandlers)
    if (HdfsUtils.exists(tempBasePath) && !HdfsUtils.renamePath(tempBasePath, dataPath)) {
      throw new FeathrDataOutputException(
        ErrorLabel.FEATHR_ERROR,
        s"Trying to rename temp path to target path in safeWrite." +
          s"Rename ${tempBasePath} to ${dataPath} failed" +
          s"This is likely a system error. Please retry.")
    }
  }

  /**
   * write rdd to a give hdfs path, this function is job-safe, e.g. other job will not be able to see
   * the output path until the write is completed, this is done via writing to a temporary folder, then finally
   * rename to the expected folder
   *
   * @param ss           Spark session
   * @param outputSchema output avro schema
   * @param rdd          rdd to write out
   * @param dataPath     path to write to
   * @param numParts     number of partition output
   */
  def safeWriteRDD(ss: SparkSession, outputSchema: Schema, rdd: RDD[GenericRecord], dataPath: String, numParts: Option[Number]): Unit = {
    val parts = if (numParts.isDefined) numParts.get.intValue() else rdd.getNumPartitions
    val avroOutput = new PartitionLimiter(ss)
      .limitPartition(rdd.asInstanceOf[RDD[IndexedRecord]], parts, parts)
      .asInstanceOf[RDD[GenericRecord]]
      .map(x => (new AvroKey[GenericRecord](x), null))
    // write feature data, schema to a predefined path hdfs path
    val avroJobConf = Job.getInstance(ss.sparkContext.hadoopConfiguration)
    AvroJob.setOutputKeySchema(avroJobConf, outputSchema)
    val tempBasePath = dataPath.stripSuffix("/") + "_temp_"

    HdfsUtils.deletePath(dataPath, true)
    avroOutput.saveAsNewAPIHadoopFile(
      tempBasePath,
      classOf[AvroKey[Any]],
      classOf[NullWritable],
      classOf[AvroKeyOutputFormat[Any]],
      avroJobConf.getConfiguration)
    if (HdfsUtils.exists(tempBasePath) && !HdfsUtils.renamePath(tempBasePath, dataPath)) {
      throw new FeathrDataOutputException(
        ErrorLabel.FEATHR_ERROR,
        s"Trying to rename temp path to target path in safeWrite." +
          s"Rename $tempBasePath to $dataPath failed" +
          s"This is likely a system error. Please retry.")
    }
  }

  /**
   * Get Default value from avro record
   * @param field the avro field for which the default value is to be extracted
   * return the JsonNode containing the default value or otherwise null
   */
  def getDefaultValueFromAvroRecord(field: Schema.Field) = {
    // This utility method throws an error if the field does not have a default value, hence we need to check if the field has a default first.
    field.defaultVal()
  }

  /**
   * Get the needed fact/feature dataset for a feature anchor as a DataFrame.
   * @param ss Spark Session
   * @param factDataSourcePath Source path of fact dataset, could be a HDFS path
   * @return loaded fact dataset, as a DataFrame.
   */
  def getRegularAnchorDF(ss: SparkSession, factDataSourcePath: String, dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    if (ss.sparkContext.isLocal){
      getLocalDF(ss, factDataSourcePath, dataLoaderHandlers)
    }
    else {
      loadAsDataFrame(ss, SimplePath(factDataSourcePath), dataLoaderHandlers)
    }
  }

  /**
   * A Common Function to load Test DFs
   * Will be moved to Test Utils later
   * @param ss    Spark Session
   * @param path  File Path
   */
  def getLocalDF(ss: SparkSession, path: String, dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    val format = FileFormat.getType(path)
    val localPath = getLocalPath(path)
    format match {
      case FileFormat.AVRO_JSON => loadJsonFileAsAvroToDF(ss, localPath).get
      case FileFormat.JDBC => JdbcUtils.loadDataFrame(ss, path)
      case _ => {
        getLocalMockDataPath(ss, path) match {
          case Some(mockData) =>
            loadSeparateJsonFileAsAvroToDF(ss, mockData).getOrElse(throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot load mock data path ${mockData}"))
          case None => loadAsDataFrame(ss, SimplePath(localPath), dataLoaderHandlers)
        }
      }
    }
  }

  /**
   * Check if the local mock data exists. If so, return the resolved path to the mock data
   *
   * @param ss         Spark Session
   * @param sourcePath Source path of the dataset, could be a HDFS path
   * @return The resolved path to the mock data.
   *         If it's not in local mode or the mock data doesn't exist, return None.
   */
  def getLocalMockDataPath(ss: SparkSession, sourcePath: String): Option[String] = {
    if (!ss.sparkContext.isLocal) return None

    val mockSourcePath = LocalFeatureJoinUtils.getMockPath(sourcePath)
    val path = new Path(mockSourcePath)
    val hadoopConf = ss.sparkContext.hadoopConfiguration
    val fs = path.getFileSystem(hadoopConf)
    val mockSourcePathWithLatest = getLatestPath(fs, mockSourcePath)
    Some(mockSourcePathWithLatest).filter(HdfsUtils.exists(_))
  }


  /**
   * Use the time range of observation data, get the needed fact dataset for a
   * window aggregation feature anchor as a DataFrame.
   * @param ss Spark Session
   * @param factDataSourcePath Source path of fact dataset, could be a HDFS path
   * @param obsDataStartTime the start time of observation data
   * @param obsDataEndTime the end time of observation data
   * @param window the length of window time
   * @return loaded fact dataset, as a DataFrame.
   */
  def getWindowAggAnchorDF(
                            ss: SparkSession,
                            factDataSourcePath: String,
                            obsDataStartTime: LocalDateTime,
                            obsDataEndTime: LocalDateTime,
                            window: Duration,
                            timeDelayMapOpt: Map[String, Duration],
                            dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    val sparkConf = ss.sparkContext.getConf
    val inputSplitSize = sparkConf.get("spark.feathr.input.split.size", "")
    var dataIOParameters = Map(SparkIOUtils.SPLIT_SIZE -> inputSplitSize)

    val fileName = new File(factDataSourcePath).getName
    if (fileName.endsWith("daily") || fileName.endsWith("hourly")) { // when source is pure HDFS with time partitions
      // HDFS path with time partitions should have the following format:
      // [source directory]/[daily/hourly]/YYYY/MM/dd/hh/
      // In Feathr configuration, only [source directory]/[daily/hourly] needs to be given.
      // The rest section is constructed using `HdfsUtils.getPaths`.
      val isDaily = fileName.endsWith("daily") // TODO: better handling for extracting "daily" or "hourly"
      val (factDataStartTime, factDataEndTime) = getFactDataTimeRange(obsDataStartTime, obsDataEndTime, window, isDaily, timeDelayMapOpt)
      // getPaths is left-inclusive
      val hdfsPaths = if (isDaily) {
        HdfsUtils.getPaths(factDataSourcePath, factDataStartTime, factDataEndTime.plusDays(1), ChronoUnit.DAYS)
      } else {
        HdfsUtils.getPaths(factDataSourcePath, factDataStartTime, factDataEndTime.plusHours(1), ChronoUnit.HOURS)
      }
      val existingHdfsPaths = hdfsPaths.filter(HdfsUtils.exists(_))
      if (existingHdfsPaths.isEmpty) {
        throw new FeathrInputDataException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to load feature data in HDFS. No available date partition exist in HDFS for path. " +
            s"$factDataSourcePath between $factDataStartTime and $factDataEndTime. Please make sure there is needed " +
            s"data for that time range.")
      }
      log.info(s"Loading HDFS path ${existingHdfsPaths} as union DataFrame for sliding window aggregation, using parameters ${dataIOParameters}")
      SparkIOUtils.createUnionDataFrame(existingHdfsPaths, dataIOParameters, new JobConf(), dataLoaderHandlers)
    } else {
      // Load a single folder
      log.info(s"Loading HDFS path ${factDataSourcePath} as DataFrame for sliding window aggregation, using parameters ${dataIOParameters}")
      SparkIOUtils.createDataFrame(SimplePath(factDataSourcePath), dataIOParameters, new JobConf(), dataLoaderHandlers)
    }
  }

  /**
   * Calculate the observation data time range with fact data time range:
   *
   * fact data start time = observation data start time - window
   * fact data end time = observation data end time
   *
   * after the calculation, we round down the start time to the closest hour/day,
   * so that the data read is aligned with date partitions.
   *
   * @param obsDataStartTime start time of observation data.
   * @param obsDataEndTime end time of observation data.
   * @param window length of window
   * @return a tuple (factDataStartTime, factDataEndTime)
   *
   */
  private[feathr] def getFactDataTimeRange(
                                            obsDataStartTime: LocalDateTime,
                                            obsDataEndTime: LocalDateTime,
                                            window: Duration,
                                            isDaily: Boolean,
                                            timeDelayMap: Map[String, Duration]): (LocalDateTime, LocalDateTime) = {

    val minTimeDelay = timeDelayMap.values.size match {
      case 0 => Duration.ZERO
      case _ => timeDelayMap.values.min
    }
    val maxTimeDelay = timeDelayMap.values.size match {
      case 0 => Duration.ZERO
      case _ => timeDelayMap.values.max
    }

    if (isDaily) {
      // truncate to lower align by day, which will cover full range of obsDataStartTime
      val start = obsDataStartTime.minus(window).minus(maxTimeDelay).truncatedTo(ChronoUnit.DAYS)
      val end = obsDataEndTime.minus(minTimeDelay).truncatedTo(ChronoUnit.DAYS) // align
      (start, end)
    } else {
      val start = obsDataStartTime.minus(window).minus(maxTimeDelay).truncatedTo(ChronoUnit.HOURS)
      val end = obsDataEndTime.minus(minTimeDelay).truncatedTo(ChronoUnit.HOURS)
      (start, end)
    }
  }

  /*
   * Given a sequence of field names, return the corresponding field, must be the top level
   */
  private def extractorForFieldNames(allFields: Seq[String], mvelContext: Option[FeathrExpressionExecutionContext]): Any => Map[String, Any] = {
    val compiledExpressionMap = allFields
      .map(
        fieldName =>
          // Use MVEL to extract field by name. This automatically gives support for dot-delimited paths for nested fields.
          (fieldName, MVEL.compileExpression(fieldName, MvelContext.newParserContext())))
      .toMap
    record =>
      compiledExpressionMap
        .mapValues(expression => {
          MvelContext.ensureInitialized()
          MvelUtils.executeExpression(expression, record, null, "", mvelContext)
        })
        .collect { case (name, Some(value)) => (name, value) }
        .toMap
  }

  /**
   * Helper function for generating file names for daily and hourly format data
   * Supported path format include:
   * 1. Daily data can be AVRO data following in HomeDir/daily/yyyy/MM/dd folder
   * 2. Orc/Hive data following in HomeDir/datepartition=yyyy-MM-dd-00
   * Example:
   * input path: foo/bar, return foo/bar/daily/2020/06/02/00 or foo/bar/datepartition=2020-06-02-00
   *
   * @param filePath base HDFS path to generate time-based paths, should not contain the hourly or daily suffix
   * @param timeInterval [startTIme, endTime] are both inclusive
   * @param hourlyData is the data hourly partitioned or daily partitioned
   * @param skipMissingFiles whether skip missing files or not
   * @return time-based path generated for the base HDFS input path
   */
  def generateHDFSTimeBasedPaths(filePath: String, timeInterval: Interval, hourlyData: Boolean = false, skipMissingFiles: Boolean = true): Seq[String] = {
    val dailyFormat = "yyyy/MM/dd"
    val dailyHourlyFormat = "yyyy/MM/dd/HH"
    val dailyPartitionFormat = "yyyy-MM-dd-00" // Daily data in Hive follows the partition conversion of datepartition=yyyy-MM-dd-00
    val dailyFilePath = if (filePath.endsWith("/")) filePath + "daily/" else filePath + "/daily/"
    val hourlyFilePath = if (filePath.endsWith("/")) filePath + "hourly/" else filePath + "/hourly/"
    val dailyPartitionFilePath = if (filePath.endsWith("/")) filePath + "datepartition=" else filePath + "/datepartition="
    val numDays = Days.daysBetween(timeInterval.getStart, timeInterval.getEnd).getDays + 1
    val numHours = Hours.hoursBetween(timeInterval.getStart, timeInterval.getEnd).getHours + 1

    // This API throws an exception if trying to run on local machine, which is the expected behaviour
    val fileSystem = Try(FileSystem.get(new Configuration()))

    val filePaths = if (hourlyData) {
      (0 until numHours)
        .map(curHour => hourlyFilePath + timeInterval.getStart.plusHours(curHour).toString(dailyHourlyFormat))
    }
    // Daily data can be AVRO data following in HomeDir/daily/yyyy/MM/dd folder
    // Or Orc/Hive data following in HomeDir/datepartition=yyyy-MM-dd-00
    // We check whether the daily folder exists, if so proceed as avro data, otherwise proceed as Hive/Orc data.
    else if (fileSystem.get.exists(new Path(dailyFilePath)) || fileSystem.get.exists(new Path(LocalFeatureJoinUtils.getMockPath(dailyFilePath)))) {
      (0 until numDays)
        .map(curDays => dailyFilePath + timeInterval.getStart.plusDays(curDays).toString(dailyFormat))
    } else {
      (0 until numDays)
        .map(curDays => dailyPartitionFilePath + timeInterval.getStart.plusDays(curDays).toString(dailyPartitionFormat))
    }

    val returnedPaths = if (skipMissingFiles && fileSystem.isSuccess) {
      filePaths.filter(curPath => fileSystem.get.exists(new Path(curPath)))
    } else {
      filePaths
    }
    log.info(s"generateHDFSTimeBasedPaths returned ${returnedPaths} for input path ${filePath}")
    returnedPaths
  }

  // translate a certain string to the time, according to the timezone tz
  def createTimeFromString(dateString: String, formatString: String = "yyyyMMdd", tz: String = "America/Los_Angeles"): Date = {
    val format = new SimpleDateFormat(formatString)
    format.setTimeZone(TimeZone.getTimeZone(tz))
    // we support source path as both /path/to/datasets/daily and /path/to/datasets/daily/, and /path//to/data,
    // so need to strip potential '/' and replace '//' with '/'
    format.parse(dateString.stripMargin('/').replaceAll("//", "/"))
  }

  def createLocalTimeTimeFromString(dateString: String, formatString: String = "yyyyMMdd", tz: String = "America/Los_Angeles"): LocalDateTime = {
    val date = createTimeFromString(dateString, formatString, tz)
    LocalDateTime.ofInstant(date.toInstant(), ZoneId.of(tz))
  }

  // create TimeInterval from a start time and end time with format
  def createTimeInterval(
                          startDateOpt: Option[String],
                          endDateOpt: Option[String],
                          formatString: String = "yyyyMMdd",
                          tz: String = "America/Los_Angeles"): Interval = {
    val timeZone = TimeZone.getTimeZone(tz)
    (startDateOpt, endDateOpt) match {
      case (Some(startDate), Some(endDate)) =>
        val startTime = createTimeFromString(startDate, formatString, tz).getTime
        val endTime = createTimeFromString(endDate, formatString, tz).getTime
        new Interval(startTime, endTime, JodaTimeZone.forTimeZone(timeZone))
      case (_, _) =>
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to create TimeInterval from a start time and end time with specified format. Date is not defined. " +
            s"Please provide date.")
    }
  }

  // targetDate specifies the reference date for the "offset" and "numDates" to be computed based on
  def createTimeIntervalFromDateParam(dateParam: Option[DateParam], targetDate: Option[String] = None): Interval = {
    dateParam match {
      case Some(dp) => createTimeInterval(dp, targetDate)
      case _ =>
        throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Trying to create time Interval from DateParam. Date is not defined. Please provide date.")
    }
  }

  /**
   * create UTC time stamps from dateParam
   * @param dateParam input data parameter
   * @param targetDate                 The target/reference date, if targetDate is set, use this date as 'today'
   * @return (start epoch, end epoch)
   */
  def createTimestampsFromDateParam(dateParam: Option[DateParam], targetDate: Option[String] = None): (Long, Long) = {
    val interval = createTimeIntervalFromDateParam(dateParam, targetDate)
    val utcStart = interval.getStart.toDateTime(JodaTimeZone.UTC)
    val epocStart = utcStart.getMillis / 1000
    val utcEnd = interval.getEnd.toDateTime(JodaTimeZone.UTC)
    val epocEnd = utcEnd.getMillis / 1000
    (epocStart, epocEnd)
  }

  def createTimeInterval(dateParam: DateParam, targetDate: Option[String]): Interval = {
    // this will only create a valid time interval if either one of the two is true:
    // 1. both startDate and endDate are set
    // 2. both dateOffset and numDays are set and dateOffset is a non-negative integer and numDays is positive integer
    // any other parameter setting will trigger an exception
    // in the case of 2, the startDate will be today_in_pst - dateOffset - numDays + 1, endDate will be today_in_pst - dateOffset
    val absoluteDateSet = dateParam.startDate.nonEmpty && dateParam.endDate.nonEmpty
    val relativeDateSet = dateParam.numDays.nonEmpty && dateParam.dateOffset.nonEmpty

    if (absoluteDateSet && !relativeDateSet) {
      createTimeInterval(dateParam.startDate, dateParam.endDate)
    } else if (relativeDateSet && !absoluteDateSet) {
      val offset = dateParam.dateOffset.get.toInt
      val numdays = dateParam.numDays.get.toInt

      if (offset < 0) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to create a valid time interval. " +
            s"dateOffset($offset) should be non-negative. Please provide non-negative dateOffset.")
      }
      if (numdays <= 0) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Trying to create a valid time interval." +
            s"numdays($numdays) should be positive." +
            s"Please provide a positive numdays.")
      }
      val startDate = Calendar.getInstance()
      val endDate = Calendar.getInstance()

      if (targetDate.isDefined) {
        // if the target Date is defined, use this date instead of TODAY
        startDate.setTime(createTimeFromString(targetDate.get))
        endDate.setTime(createTimeFromString(targetDate.get))
      }

      val format = new SimpleDateFormat("yyyyMMdd")

      // set explicit timezone
      val tz = TimeZone.getTimeZone("America/Los_Angeles")
      format.setTimeZone(tz)

      startDate.add(Calendar.DATE, -offset - numdays + 1)
      endDate.add(Calendar.DATE, -offset)

      val startDateFromOffset = format.format(startDate.getTime())
      val endDateFromOffset = format.format(endDate.getTime())
      createTimeInterval(Some(startDateFromOffset), Some(endDateFromOffset))
    } else {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Trying to create a valid time interval." +
          s"The provided format is incorrect: dateParam - $dateParam, targetDate - $targetDate" +
          s"Please either set both startDate and endDate, or both numDays and dateOffset. Other parameter combinations are not accepted.")
    }
  }

  private def createTimeInterval(inputData: InputData): Interval = {
    createTimeIntervalFromDateParam(inputData.dateParam)
  }

  /**
   * load paths as union datasets
   * @param ss
   * @param inputPath
   * @return
   */
  def loadAsUnionDataFrame(ss: SparkSession, inputPath: Seq[String],
                           dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    val sparkConf = ss.sparkContext.getConf
    val inputSplitSize = sparkConf.get("spark.feathr.input.split.size", "")
    val dataIOParameters = Map(SparkIOUtils.SPLIT_SIZE -> inputSplitSize)
    val hadoopConf = ss.sparkContext.hadoopConfiguration
    log.info(s"Loading ${inputPath} as union DataFrame, using parameters ${dataIOParameters}")
    SparkIOUtils.createUnionDataFrame(inputPath, dataIOParameters, new JobConf(), dataLoaderHandlers)
  }

  /**
   * load single input path as dataframe
   * @param ss
   * @param inputPath
   * @return
   */
  def loadAsDataFrame(ss: SparkSession, location: DataLocation,
                      dataLoaderHandlers: List[DataLoaderHandler]): DataFrame = {
    val sparkConf = ss.sparkContext.getConf
    val inputSplitSize = sparkConf.get("spark.feathr.input.split.size", "")
    val dataIOParameters = Map(SparkIOUtils.SPLIT_SIZE -> inputSplitSize)
    log.info(s"Loading ${location} as DataFrame, using parameters ${dataIOParameters}")
    SparkIOUtils.createDataFrame(location, dataIOParameters, new JobConf(), dataLoaderHandlers)
  }

  /**
   * Load observation data as DataFrame
   * @param ss
   * @param conf
   * @param inputData HDFS path for observation
   * @return
   */
  def loadObservationAsDF(ss: SparkSession, conf: Configuration, inputData: InputData, dataLoaderHandlers: List[DataLoaderHandler],
                          failOnMissing: Boolean = true): DataFrame = {
    // TODO: Split isLocal case into Test Packages
    val format = FileFormat.getType(inputData.inputPath)
    log.info(s"loading ${inputData.inputPath} input Path as Format: ${format}")

    // Get csvDelimiterOption set with spark.feathr.inputFormat.csvOptions.sep and check if it is set properly (Only for CSV and TSV)
    val csvDelimiterOption = checkDelimiterOption(ss.sqlContext.getConf("spark.feathr.inputFormat.csvOptions.sep", ","))

    format match {
      case FileFormat.PATHLIST => {
        val pathList = getPathList(sourceFormatType=inputData.sourceType,
          sourcePath=inputData.inputPath,
          ss=ss,
          dateParam=inputData.dateParam,
          targetDate=None,
          failOnMissing=failOnMissing,
          dataLoaderHandlers=dataLoaderHandlers
        )
        if (ss.sparkContext.isLocal) { // for test
          try {
            loadAsUnionDataFrame(ss, pathList, dataLoaderHandlers)
          } catch {
            case _: Throwable => loadSeparateJsonFileAsAvroToDF(ss, inputData.inputPath).get
          }
        } else {
          loadAsUnionDataFrame(ss, pathList, dataLoaderHandlers)
        }
      }
      case FileFormat.JDBC => {
        JdbcUtils.loadDataFrame(ss, inputData.inputPath)
      }
      case FileFormat.SNOWFLAKE => {
        SnowflakeUtils.loadDataFrame(ss, inputData.inputPath)
      }
      case FileFormat.CSV => {
        ss.read.format("csv").option("header", "true").option("delimiter", csvDelimiterOption).load(inputData.inputPath)
      }
      case _ => {
        loadAsDataFrame(ss, SimplePath(inputData.inputPath),dataLoaderHandlers)
      }
    }
  }

  def getLocalPath(path: String): String = {
    getClass.getClassLoader.getResource(path).getPath()
  }

  /**
   * parse the input dataArray json as RDD
   * @param ss spark seesion
   * @param dataArrayAsJson input data array as json string
   * @param schemaAsString avro schema of the input data array
   * @return the converted rdd and avro schema
   */
  def parseJsonAsAvroRDD[T](ss: SparkSession, dataArrayAsJson: String, schemaAsString: String)(implicit tag: ClassTag[T]): (RDD[_], Schema) = {
    val sc = ss.sparkContext
    val jackson = new ObjectMapper(new HoconFactory)
    val schema = Schema.parse(schemaAsString)
    val jsonDataArray = jackson.readTree("{ data:" + dataArrayAsJson + " }").get("data")
    val records = jsonDataArray.map(jsonNode => {
      val input = new ByteArrayInputStream(jsonNode.toString.getBytes)
      val din = new DataInputStream(input)
      val decoder = DecoderFactory.get().jsonDecoder(schema, din)
      if (!classOf[SpecificRecordBase].isAssignableFrom(scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])) {
        val reader = new GenericDatumReader[GenericRecord](schema)
        reader.read(null, decoder)
      } else {
        val reader = new SpecificDatumReader[T](scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])
        reader.read(null.asInstanceOf[T], decoder)
      }
    })
    (sc.parallelize(records.toSeq), schema)
  }

  /**
   * load rdd from a path which hosts files in separate json format, e.g.
   * /path/
   *     | mockData.json
   *     | schema.avsc
   * @param ss spark session
   * @param path path to load
   */
  def loadSeparateJsonFileAsAvroToRDD[T](ss: SparkSession, path: String)(implicit tag: ClassTag[T]): Option[(RDD[_], Schema)] = {
    try {
      val dataPath = path + "/mockData.json"
      val dataAsString = readLocalConfFileAsString(dataPath)
      val schemaPath = path + "/schema.avsc"
      val schemaAsString = readLocalConfFileAsString(schemaPath)
      Some(parseJsonAsAvroRDD(ss, dataAsString, schemaAsString))
    } catch {
      case e: Exception => None
    }
  }

  /**
   * Load .avro.json file as datafile
   * @param ss
   * @param path
   * @return DataFrame of input .avro.json path
   */
  def loadSeparateJsonFileAsAvroToDF(ss: SparkSession, path: String): Option[DataFrame] = {
    loadSeparateJsonFileAsAvroToRDD(ss, path).map { res =>
      val schema = res._2
      val sqlType = SchemaConverters.toSqlType(schema).dataType.asInstanceOf[StructType]
      val converter = SchemaConverterUtils.converterSql(schema, sqlType)
      val rowRdd = res._1
        .asInstanceOf[RDD[GenericRecord]]
        .flatMap(record => {
          Try(converter(record).asInstanceOf[Row]).toOption
        })
      ss.createDataFrame(rowRdd, sqlType)
    }
  }


  /**
   * Avro Record schemas are required to have names. So we will make a random-ish record name.
   * (Pig's AvroStorage also makes random-ish Record names of the form "TUPLE_1", "TUPLE_2", etc.)
   */
  def getArbitraryRecordName(x: AnyRef): String = {
    "AnonRecord_" + Integer.toHexString(x.hashCode)
  }

  /**
   * Constructs an Avro GenericData compatible representation of some object, based on an Avro schema.
   * If the schema is of type RECORD, expects the object to be a Map with all the right fields.
   * If the schema is of type ARRAY, expects the object to be a List with elements of the correct element type.
   * Etc.
   */
  def coerceToAvro(schema: Schema, obj: Any): Any = {
    schema.getType match {
      case Schema.Type.RECORD =>
        val record = new Record(schema)
        schema.getFields.foreach(field => record.put(field.name, coerceToAvro(field.schema, obj.asInstanceOf[java.util.Map[_, _]].get(field.name))))
        record
      case Schema.Type.ARRAY =>
        val list = obj.asInstanceOf[java.util.List[_]]
        val array = new Array[Any](list.size, schema.getElementType)
        list.map(coerceToAvro(schema.getElementType, _)).foreach(array.add)
        array
      case Schema.Type.STRING =>
        new Utf8(obj.asInstanceOf[String])
      case _ => obj
    }
  }


  def loadJsonTextFile(ss: SparkSession, path: String): RDD[_] = {
    val sc = ss.sparkContext
    require(sc.isLocal)
    require(path.endsWith(".json"))
    val contents = Source.fromFile(getClass.getClassLoader.getResource(path).toURI).mkString
    sc.parallelize(doLoadJsonDocument(contents))
  }

  def doLoadJsonDocument(contents: String): Seq[_] = {
    // this is just for unit tests, so it's ok.
    val hackedContents = "{root:[" + contents + "]}"
    val jackson = new ObjectMapper(new HoconFactory)
    jackson
      .readValue[java.util.Map[_, _]](hackedContents, classOf[java.util.Map[_, _]])
      .get("root")
      .asInstanceOf[java.util.Collection[_]]
      .toSeq
  }

  /**
   * Load .avro.json file as RDD
   * @param ss
   * @param path
   * @return return result RDD and schema, it path does not exist, return None
   */
  def loadJsonFileAsAvroToRDD[T](ss: SparkSession, path: String)(implicit tag: ClassTag[T]): Option[(RDD[_], Schema)] = {
    val sc = ss.sparkContext
    require(sc.isLocal)
    require(path.endsWith(".avro.json"))
    val absolutePath = getClass.getClassLoader.getResource(path)
    if (absolutePath != null) {
      val contents = Source.fromFile(absolutePath.toURI).mkString
      val jackson = new ObjectMapper(new HoconFactory)
      val tree = jackson.readTree(contents)
      val schema = Schema.parse(tree.get("schema").toString)
      val jsonDataArray = tree.get("data")

      val records = jsonDataArray.map(jsonNode => {
        val input = new ByteArrayInputStream(jsonNode.toString.getBytes)
        val din = new DataInputStream(input)
        val decoder = DecoderFactory.get().jsonDecoder(schema, din)
        if (!classOf[SpecificRecordBase].isAssignableFrom(scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])) {
          val reader = new GenericDatumReader[GenericRecord](schema)
          reader.read(null, decoder)
        } else {
          val reader = new SpecificDatumReader[T](scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])
          reader.read(null.asInstanceOf[T], decoder)
        }
      })
      Some(sc.parallelize(records.toSeq), schema)
    } else None
  }

  /**
   * Load .avro.json file as datafile
   * @param ss
   * @param path
   * @return DataFrame of input .avro.json path
   */
  def loadJsonFileAsAvroToDF(ss: SparkSession, path: String): Option[DataFrame] = {
    loadJsonFileAsAvroToRDD(ss, path) match {
      case Some((rdd, schema)) => convertRddToDataFrame(ss, rdd.asInstanceOf[RDD[GenericRecord]], schema)
      case None => None
    }
  }

  /**
   * convert a RDD of generic record to dataframe
   * @param ss spark session
   * @param rdd rdd of generic record
   * @param schema schema of the RDD
   * @return converted dataframe
   */
  def convertRddToDataFrame(ss: SparkSession, rdd: RDD[GenericRecord], schema: Schema): Option[DataFrame] = {
    val sqlType = SchemaConverters.toSqlType(schema).dataType.asInstanceOf[StructType]
    val converter = SchemaConverterUtils.converterSql(schema, sqlType)
    // Here we need to collect the rows and apply the converter instead of applying the converter to the RDD,
    // because the converter may not be serializable and unable to distribute to the executors.
    // We will trigger a action and collect the data in the driver. It' OK to do so because this function is only
    // used to load local testing files.
    val rows = rdd.collect()
      .flatMap(record => {
        Try(converter(record).asInstanceOf[Row]).toOption
      })
    Some(ss.createDataFrame(util.Arrays.asList(rows: _*), sqlType))
  }

  /*
   * Check if a string is a file path
   * Rules: for HDFS path, it should contain "/"; for unit tests, we now support .csv and .json
   */
  def isFilePath(sourceIdentifier: String): Boolean = {
    sourceIdentifier.contains("/") || sourceIdentifier.contains(".csv") || sourceIdentifier.contains(".json") || sourceIdentifier.contains(".orc")
  }

  /**
   * concat (compound) keys to a String that can be used for filtering
   * note: the values of the keys should be passed into this function, not keyTag, e.g, it can be "123#456". Not "entityId#entityId2"
   * If any key in the keys is null, return null. Use null instead of None, because rdd.join can only handle null correctly
   * * */
  def generateFilterKeyString(keys: Seq[String]): String = {
    if (keys.count(_ != null) == keys.size) {
      keys.reduce(_ + "#" + _)
    } else {
      null
    }
  }

  /**
   * load a time series dataset(daily) stored under basePath
   * this is used for local test, file name under each day's folder must be data.avro.json
   * @param ss spark session
   * @param paths full paths of each day's dataset, e.g. [/path/to/datasets/2019/05/01, /path/to/datasets/2019/05/02]
   * @param basePath base paths of the dataset, e.g., /path/to/datasets
   * @return a sequence of (dataframe of one day, the date of this dataframe)
   */
  private[feathr] def loadTimeSeriesAvroJson(
                                              ss: SparkSession,
                                              paths: Seq[String],
                                              basePath: String,
                                              isSeparateAvroJson: Boolean = false): Seq[(DataFrame, Interval)] = {
    if (ss.sparkContext.isLocal && !paths.head.startsWith(HDFS_PREFIX)) {
      // If runs locally and source is not HDFS, this is for local test API
      // Assume feature data file always named as "data.avro.json", this is similar to hadoop names output as part-00000.avro
      // scan the input files specified by the time range, since the range is not accurate, file might not exist
      paths
        .map(path => {
          val df = if (isSeparateAvroJson) {
            loadSeparateJsonFileAsAvroToDF(ss, path)
          } else {
            loadJsonFileAsAvroToDF(ss, path + "/data.avro.json")
          }
          val timeStr = path.substring(basePath.length)
          val interval = if (!timeStr.equals("")) {
            createTimeInterval(Some(timeStr), Some(timeStr), "yyyy/MM/dd")
          } else { // load whole dataset for SWA.  Note that this is not needed in the code-yellow branch.
            createTimeInterval(Some("000/01/01"), Some("9999/12/31"), "yyyy/MM/dd")
          }
          (df, interval)
        })
        .collect { case (Some(df: DataFrame), interval: Interval) => (df, interval) }
    } else {
      throw new FeathrInputDataException(
        ErrorLabel.FEATHR_ERROR,
        s"Trying to load a time series dataset." +
          s"Cannot load time series avro json data from ${basePath}, Currently, " +
          s"this test API only runs locally and source path has to start with ${HDFS_PREFIX}" +
          s"Please do not use this API in non-local environment.")
    }
  }

  /**

   * convert a joda DateTime to java LocalDateTime in "America/Los_Angeles" timezone
   * @param jodaDateTime date time with time zone
   * @return date time in PST/PDT, ignoring hour and minute
   */
  private[feathr] def jodaDateTimeToPSTPDTLocalTime(jodaDateTime: JodaDateTime): LocalDateTime = {
    val time = jodaDateTime.withZone(JodaTimeZone.forID("America/Los_Angeles"))
    LocalDateTime.of(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0)
  }

  /**
   * This function is the sister function of jodaDateTimeToPSTPDTLocalTime, ignore the hour and minutes of LocalDateTime
   * @param localDateTime local date time
   * @return data time with hour and minutes removed
   */
  private[feathr] def toDailyLocalTime(localDateTime: LocalDateTime): LocalDateTime = {
    LocalDateTime.of(localDateTime.getYear, localDateTime.getMonth, localDateTime.getDayOfMonth, 0, 0)
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

  /**
   * Gets all the file name under a path
   * @param basePath The basePath where to search for files
   * @param includeSuffixList The suffix that target file name must have, if empty, will return all files under the folder
   * @return A List[String] containing the paths of the file names
   */
  def getFileNamesInSubFolder(basePath: String, includeSuffixList: Seq[String] = List(), conf: Configuration = conf): Seq[String] = {
    val fs: FileSystem = FileSystem.get(conf)
    val filesNames: Seq[String] = fs
      .listStatus(new Path(basePath))
      .filter(_.isFile)
      .map(_.getPath.getName)
    val filteredFilenames =
      filesNames.filter(dirName => includeSuffixList.isEmpty || includeSuffixList.exists(suffix => dirName.endsWith(suffix)))
    filesNames
  }

  /**
   * Get the feathr feature definition conf file content from dependent feature repo.
   * The implementation assumes the current repo depend on some feathr feature and the feathr plugin
   * @return feature definition conf file as an optional string
   */
  def getFeathrConfFromFeatureRepo(): Option[String] = {
    try {
      val featureMPDefConfigPaths = getFileNamesInSubFolder(FEATURE_MP_DEF_CONFIG_BASE_PATH, Seq(FEATURE_MP_DEF_CONFIG_SUFFIX))
        .map(fileName => FEATURE_MP_DEF_CONFIG_BASE_PATH + "/" + fileName)
      if (featureMPDefConfigPaths.nonEmpty) {
        Some(SourceUtils.readLocalConfFileAsString(featureMPDefConfigPaths.mkString(",")))
      } else {
        None
      }
    } catch {
      case _: Exception =>
        None
    }
  }


  /**
   * Print  partition info. Used to debug data skew
   * @param dfNamePairs a sequence of (dataframe name, dataframe)
   */
  def printDataFramePartitionInfo(dfNamePairs: Seq[(String, DataFrame)]): Unit = {
    implicit def single[A](implicit c: ClassTag[A]): Encoder[A] = Encoders.kryo[A](c)
    log.info(s"Printing info of ${dfNamePairs.map(pr => pr._1 + ':' + pr._2 + ';')}")
    dfNamePairs foreach {
      case (dfName, df) =>
        val res = df.mapPartitions(x => Seq(new Integer(x.size)).toIterator)
        res.foreach(num => {
          log.info(num)
        })
    }
  }

  /**
   * Check if it is in SanityCheck mode mode, if so, limit the DataFrame size for fast run.
   * @param ss Spark session
   * @param df Input source dataframe
   * @return processed dataframe
   */
  def processSanityCheckMode(ss: SparkSession, df: DataFrame): DataFrame = {
    val isSanityCheckMode = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.ENABLE_SANITY_CHECK_MODE).toBoolean
    if (isSanityCheckMode) {
      val rowCount = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SANITY_CHECK_MODE_ROW_COUNT).toInt
      df.limit(rowCount)
    } else {
      df
    }
  }
}
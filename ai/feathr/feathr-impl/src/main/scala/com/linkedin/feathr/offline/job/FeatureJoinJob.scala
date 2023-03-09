package com.linkedin.feathr.offline.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrDataOutputException, FeathrInputDataException}
import com.linkedin.feathr.common.{Header, JoiningFeatureParams}
import com.linkedin.feathr.offline._
import com.linkedin.feathr.offline.client._
import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.config.datasource.{DataSourceConfigUtils, DataSourceConfigs}
import com.linkedin.feathr.offline.config.location.{DataLocation, SimplePath}
import com.linkedin.feathr.offline.generation.SparkIOUtils
import com.linkedin.feathr.offline.source.SourceFormatType
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.util.SourceUtils.getPathList
import com.linkedin.feathr.offline.util._
import com.linkedin.feathr.offline.transformation.AnchorToDataSourceMapper
import org.apache.avro.generic.GenericRecord
import org.apache.commons.cli.{Option => CmdOption}
import org.apache.hadoop.conf.Configuration
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level, LogManager, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import collection.JavaConverters._
import scala.collection.mutable

/**
 * Join features to some observations for training/testing
 *
 * Output Format:
 * For some input observations having schema S,
 * the output will have the following schema <pre>
 * {
 * S,
 * featureGroupA: [ { name: string, term: string, value: double } ],
 * featureGroupB: [ { name: string, term: string, value: double } ],
 * ...
 * }</pre>
 *
 * Note: Most of the actual join logic is found in FeathrClient.joinFeatures.
 * This class is mostly concerned with input/output serialization and formatting, configuration, and wrapping the join
 * logic with a job we can invoke via Azkaban.
 */
object FeatureJoinJob {

  val logger: Logger = LogManager.getLogger(getClass)
  val SKIP_OUTPUT = "skip_output"

  // We found that if we have too many parallelism, then during the join shuffling, memoryOverhead could be too high,
  // cap it to 10000 to make sure memoryOverhead is less than 2g (Feathr default value)
  val SPARK_JOIN_MAX_PARALLELISM = "10000"
  val SPARK_JOIN_MIN_PARALLELISM = "10"
  // If spark.default.parallelism is not set, this value will be used as the default and used
  // to cap numPartitions later; otherwise, the spark.default.paralleism value set by user will be used
  val SPARK_JOIN_PARALLELISM_DEFAULT = "5000"
  // Internal parameter (We don't expect user to change it) as an empirical factor 'threshold' to control whether limit the partition or not
  val SPARK_JOIN_LIMIT_PARTITION_FACTOR = 2

  val log: Logger = LogManager.getLogger(getClass)

  def run(ss: SparkSession, hadoopConf: Configuration, jobContext: FeathrJoinJobContext, dataPathHandlers: List[DataPathHandler]): Unit = {
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val joinConfig = FeatureJoinConfig.parseJoinConfig(hdfsFileReader(ss, jobContext.joinConfig))
    print("join config is, ",joinConfig)
    // check read authorization for observation data, and write authorization for output path
    checkAuthorization(ss, hadoopConf, jobContext, dataLoaderHandlers)

    feathrJoinRun(ss=ss,
      hadoopConf=hadoopConf,
      joinConfig=joinConfig,
      jobContext=jobContext.jobJoinContext,
      localTestConfig=None,
      dataPathHandlers=dataPathHandlers,
      useFCM = jobContext.useFCM)
  }

  // Log the feature names for bookkeeping. Global config may be merged with local config(s).
  def stringifyFeatureNames(nameSet: Set[String]): String = nameSet.toSeq.sorted.toArray.mkString("\n\t")

  def hdfsFileReader(ss: SparkSession, path: String): String = {
    print("ss.sparkContext.textFile(path),", path)
    ss.sparkContext.textFile(path).collect.mkString("\n")
  }

  private def checkAuthorization(ss: SparkSession, hadoopConf: Configuration, jobContext: FeathrJoinJobContext, dataLoaderHandlers: List[DataLoaderHandler]): Unit = {

    jobContext.jobJoinContext.outputPath match {
      case SimplePath(path) => {
        AclCheckUtils.checkWriteAuthorization(hadoopConf, path) match {
          case Failure(e) =>
            throw new FeathrDataOutputException(ErrorLabel.FEATHR_USER_ERROR, s"No write permission for output path ${jobContext.jobJoinContext.outputPath}.", e)
          case Success(_) => log.debug("Checked write authorization on output path: " + jobContext.jobJoinContext.outputPath)
        }
      }
      case _ => {}
    }

    jobContext.jobJoinContext.inputData.map(inputData => {
      val failOnMissing = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
      val pathList = getPathList(sourceFormatType=inputData.sourceType,
      sourcePath=inputData.inputPath,
      ss=ss,
      dateParam=inputData.dateParam,
      targetDate=None,
      failOnMissing=failOnMissing,
      dataLoaderHandlers=dataLoaderHandlers)
      val invalidPathsAndErrors = AclCheckUtils.checkReadAuthorization(hadoopConf, pathList)
      if (invalidPathsAndErrors.isEmpty) {
        log.debug("Checked read authorization on observation data of the following paths:\n" + pathList.mkString("\n"))
      } else {
        val invalidPaths = invalidPathsAndErrors.map(_._2)
        val errorMsgs = invalidPathsAndErrors.map(_._1)
        throw new FeathrInputDataException(ErrorLabel.FEATHR_USER_ERROR, s"No read permission on observation data $invalidPaths with  $errorMsgs")
      }
    })
  }

  /**
   * This function will get the feathr client using the spark session and jobContext, and call FeathrClient#joinObsAndFeatures
   * method.
   * @param ss  spark session
   * @param observations  observations DF
   * @param featureGroupings   feature groups to join
   * @param joinConfig  join config
   * @param jobContext  job context
   * @param localTestConfigOpt  Local test config
   * @return  Dataframe and header associated with it.
   */
  private[offline] def getFeathrClientAndJoinFeatures(
      ss: SparkSession,
      observations: DataFrame,
      featureGroupings: Map[String, Seq[JoiningFeatureParams]],
      joinConfig: FeatureJoinConfig,
      jobContext: JoinJobContext,
      dataPathHandlers: List[DataPathHandler],
      localTestConfigOpt: Option[LocalTestConfig] = None): (DataFrame, Header) = {

    val feathrClient = getFeathrClient(ss, jobContext, dataPathHandlers, localTestConfigOpt)
    feathrClient.doJoinObsAndFeatures(joinConfig, jobContext, observations)
  }

  private[offline] def getFeathrClient(
    ss: SparkSession,
    jobContext: JoinJobContext,
    dataPathHandlers: List[DataPathHandler],
    localTestConfigOpt: Option[LocalTestConfig] = None): FeathrClient = {

    localTestConfigOpt match {
      case None =>
        FeathrClient.builder(ss)
          .addFeatureDefPath(jobContext.feathrFeatureConfig)
          .addLocalOverrideDefPath(jobContext.feathrLocalConfig)
          .addDataPathHandlers(dataPathHandlers)
          .build()
      case Some(localTestConfig) =>
        FeathrClient.builder(ss)
          .addFeatureDef(localTestConfig.featureConfig)
          .addLocalOverrideDef(localTestConfig.localConfig)
          .addDataPathHandlers(dataPathHandlers)
          .build()
    }
  }

  /**
   * This function will get the FCM client using the spark session and jobContext, and call FCM client (FeathrClient2)#joinObsAndFeatures
   * method.
   * @param ss  spark session
   * @param observations  observations DF
   * @param featureGroupings   feature groups to join
   * @param joinConfig  join config
   * @param jobContext  job context
   * @param localTestConfigOpt  Local test config
   * @return  Dataframe and header associated with it.
   */
  private[offline] def getFCMClientAndJoinFeatures(
    ss: SparkSession,
    observations: DataFrame,
    featureGroupings: Map[String, Seq[JoiningFeatureParams]],
    joinConfig: FeatureJoinConfig,
    jobContext: JoinJobContext,
    dataPathHandlers: List[DataPathHandler],
    localTestConfigOpt: Option[LocalTestConfig] = None): DataFrame = {

    val feathrClient2 = getFCMClient(ss, jobContext, dataPathHandlers, localTestConfigOpt)
    feathrClient2.joinFeatures(joinConfig, SparkFeaturizedDataset(observations, FeaturizedDatasetMetadata()), jobContext)._1.df
  }

  private[offline] def getFCMClient(
    ss: SparkSession,
    jobContext: JoinJobContext,
    dataPathHandlers: List[DataPathHandler],
    localTestConfigOpt: Option[LocalTestConfig] = None): FeathrClient2 = {

    localTestConfigOpt match {
      case None =>
        FeathrClient2.builder(ss)
          .addFeatureDefPath(jobContext.feathrFeatureConfig)
          .addLocalOverrideDefPath(jobContext.feathrLocalConfig)
          .addDataPathHandlers(dataPathHandlers)
          .build()
      case Some(localTestConfig) =>
        FeathrClient2.builder(ss)
          .addFeatureDef(localTestConfig.featureConfig)
          .addLocalOverrideDef(localTestConfig.localConfig)
          .addDataPathHandlers(dataPathHandlers)
          .build()
    }
  }

  /**
   * This function will collect the data, build the schema and do the join work for hdfs records.
   *
   * @param ss                 SparkSession
   * @param hadoopConf         Hadoop Configuration
   * @param joinConfig         Feathr join config
   * @param jobContext         Other parameters, including output path, passthroughFeatures, inputData and etc.
   * @param localTestConfig    local featureDef config
   * @return the output DataFrame
   */
  private[feathr] def feathrJoinRun(
      ss: SparkSession,
      hadoopConf: Configuration,
      joinConfig: FeatureJoinConfig,
      jobContext: JoinJobContext,
      dataPathHandlers: List[DataPathHandler],
      localTestConfig: Option[LocalTestConfig] = None,
      useFCM: Boolean = false): (Option[RDD[GenericRecord]], Option[DataFrame]) = {
    val sparkConf = ss.sparkContext.getConf
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val featureGroupings = joinConfig.featureGroupings

    /*
     * load FeathrClient and perform the Feature Join
     */
    val failOnMissing = FeathrUtils.getFeathrJobParam(ss, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
    val observationsDF = SourceUtils.loadObservationAsDF(ss, hadoopConf, jobContext.inputData.get, dataLoaderHandlers, failOnMissing)

    val joinedDF = if (useFCM) {
      getFCMClientAndJoinFeatures(ss, observationsDF, featureGroupings, joinConfig, jobContext, dataPathHandlers, localTestConfig)
    } else {
      getFeathrClientAndJoinFeatures(ss, observationsDF, featureGroupings, joinConfig, jobContext, dataPathHandlers, localTestConfig)._1
    }

    val parameters = Map(SparkIOUtils.OUTPUT_PARALLELISM -> jobContext.numParts.toString, SparkIOUtils.OVERWRITE_MODE -> "ALL")

    SparkIOUtils.writeDataFrame(joinedDF, jobContext.outputPath, parameters, dataLoaderHandlers)
    (None, Some(joinedDF))
  }

  /**
   * Parse command line arguments into the join job context.
   *
   * @param args command line arguments
   * @return join job context
   */
  def parseInputArgument(args: Array[String]): FeathrJoinJobContext = {
    val params = Map(
      // option long name, short name, description, arg name (null means not argument), default value (null means required)
      "feathr-config" -> OptionParam("f", "Path of the feathr local config file", "FCONF", ""),
      "feature-config" -> OptionParam("ef", "Names of the feathr feature config files", "EFCONF", ""),
      "local-override-all" -> OptionParam("loa", "Local config overrides all other configs", "LOCAL_OVERRIDE", "true"),
      "join-config" -> OptionParam("j", "Path of the join config file", "JCONF", ""),
      "input" -> OptionParam("i", "Path of the input data set", "INPUT", ""),
      "output" -> OptionParam("o", "Path of the output", "OUTPUT", ""),
      "num-parts" -> OptionParam("n", "Number of output part files", "NPARTS", "-1"),
      "pass-through-field" -> OptionParam("p", "Pass-through feature field name", "PFIELD", ""),
      "pass-through-features" -> OptionParam("t", "Pass-through feature list, comma-separated", "PLIST", ""),
      "source-type" -> OptionParam("st", "Source type of the observation data", "SRCTYPE", "FIXED_PATH"),
      "start-date" -> OptionParam("sd", "Start date of the observation data if it's time based", "SDATE", ""),
      "end-date" -> OptionParam("ed", "End date of the observation data if it's time based", "EDATE", ""),
      "num-days" -> OptionParam("nd", "Number of days before the offset date if it's time based", "NDAYS", ""),
      "date-offset" -> OptionParam("do", "Offset of observation data if it's time based", "DOFFSET", ""),
      "join-parallelism" -> OptionParam("p", "Multiplier to increase the number of partitions of feature datasets during joins", "PARALLEL", "8"),
      "row-bloomfilter-threshold" -> OptionParam("rbt", "Performance tuning, if observation record # is less than the threshold, " +
        "a bloomfilter will be applied", "ROWFILTERTHRESHOLD", "-1"),
      "job-version" -> OptionParam("jv", "Job version, integer, job version 2 uses DataFrame and SQL based anchor, default is 2", "JOBVERSION", "2"),
      "as-tensors" -> OptionParam("at", "If set to true, get features as tensors else as term-vectors", "AS_TENSORS", "false"),
      "s3-config" -> OptionParam("sc", "Authentication config for S3", "S3_CONFIG", ""),
      "adls-config" -> OptionParam("adlc", "Authentication config for ADLS (abfs)", "ADLS_CONFIG", ""),
      "blob-config" -> OptionParam("bc", "Authentication config for Azure Blob Storage (wasb)", "BLOB_CONFIG", ""),
      "sql-config" -> OptionParam("sqlc", "Authentication config for Azure SQL Database (jdbc)", "SQL_CONFIG", ""),
      "snowflake-config" -> OptionParam("sfc", "Authentication config for Snowflake Database (jdbc)", "SNOWFLAKE_CONFIG", ""),
      "use-fcm" -> OptionParam("ufcm", "If set to true, use FCM client, else use Feathr Client", "USE_FCM", "false"),
      "snowflake-config" -> OptionParam("sfc", "Authentication config for Snowflake Database (jdbc)", "SNOWFLAKE_CONFIG", ""),
      "system-properties" -> OptionParam("sps", "Additional System Properties", "SYSTEM_PROPERTIES_CONFIG", "")
    )

    val extraOptions = List(new CmdOption("LOCALMODE", "local-mode", false, "Run in local mode"))

    val cmdParser = new CmdLineParser(args, params, extraOptions)

    // Set system properties passed via arguments
    val sps = cmdParser.extractOptionalValue("system-properties").getOrElse("{}")
    val props = (new ObjectMapper()).registerModule(DefaultScalaModule).readValue(sps, classOf[mutable.HashMap[String, String]])
    props.foreach(e => scala.util.Properties.setProp(e._1, e._2))

    val joinConfig = cmdParser.extractRequiredValue("join-config")

    val inputData = {
      val input = cmdParser.extractRequiredValue("input")
      val sourceType = SourceFormatType.withName(cmdParser.extractRequiredValue("source-type"))
      val startDate = cmdParser.extractOptionalValue("start-date")
      val endDate = cmdParser.extractOptionalValue("end-date")
      val numDays = cmdParser.extractOptionalValue("num-days")
      val dateOffset = cmdParser.extractOptionalValue("date-offset")

      InputData(input, sourceType, startDate, endDate, dateOffset, numDays)
    }

    val passThroughFeatures = {
      cmdParser.extractRequiredValue("pass-through-features") match {
        case "" => Set.empty[String]
        case str => str.split(",") map (_.trim) toSet
      }
    }

    val feathrLocalConfig = cmdParser.extractOptionalValue("feathr-config")
    val feathrFeatureConfig = cmdParser.extractOptionalValue("feature-config")
    val localOverrideAll = cmdParser.extractRequiredValue("local-override-all")
    val outputPath = DataLocation(cmdParser.extractRequiredValue("output"))
    val numParts = cmdParser.extractRequiredValue("num-parts").toInt

    val joinJobContext = {
      JoinJobContext(
        feathrLocalConfig,
        feathrFeatureConfig,
        Some(inputData),
        outputPath,
        numParts
        )
    }

    val dataSourceConfigs = DataSourceConfigUtils.getConfigs(cmdParser)
    val useFCM = cmdParser.extractRequiredValue("use-fcm").toBoolean
    FeathrJoinJobContext(joinConfig, joinJobContext, dataSourceConfigs, useFCM)
  }

  type KeyTag = Seq[String]
  type FeatureName = String

  /**
   */
  def parseJoinConfig(joinConfString: String): FeatureJoinConfig = FeatureJoinConfig.parseJoinConfig(joinConfString)

  /**
   * Load the DataFrames for sources that needs preprocessing by Pyspark.
   * @param args Same arguments for the main job.
   * @param featureNamesInAnchorSet A set of feature names of an anchor sorted and joined by comma. For example,
   *                                anchor1 -> f1, f2, anchor2 -> f3, f4. Then the set is ("f1,f2", "f3,f4")
   * @return A Java map whose key is Feature names of an anchor sorted and joined by comma and value is the DataFrame
   *         for this anchor source. For example, anchor1 -> f1, f2, anchor2 -> f3, f4. Then the result is
   *         Map("f1,f2" -> df1, "f3,f4" -> df2).
   */
  def loadSourceDataframe(args: Array[String], featureNamesInAnchorSet: java.util.Set[String]): java.util.Map[String, DataFrame] = {
    logger.info("FeatureJoinJob args are: " + args)
    logger.info("Feature join job: loadDataframe")
    logger.info(featureNamesInAnchorSet)
    val feathrJoinPreparationInfo = prepareSparkSession(args)
    val sparkSession = feathrJoinPreparationInfo.sparkSession
    val hadoopConf = feathrJoinPreparationInfo.hadoopConf
    val jobContext = feathrJoinPreparationInfo.jobContext

    // check read authorization for observation data, and write authorization for output path
    checkAuthorization(sparkSession, hadoopConf, jobContext, List())

    // Doesn't support loading local test client for this yet
    val feathrClient = getFeathrClient(sparkSession, jobContext.jobJoinContext, List()) //TODO: fix python errors for loadSourceDataFrame, add dataPathLoader Support
    val allAnchoredFeatures = feathrClient.allAnchoredFeatures

    // Using AnchorToDataSourceMapper to load DataFrame for preprocessing
    val failOnMissing = FeathrUtils.getFeathrJobParam(sparkSession, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
    val anchorToDataSourceMapper = new AnchorToDataSourceMapper(List()) //TODO: fix python errors for loadSourceDataFrame, add dataPathLoader Support
    val anchorsWithSource = anchorToDataSourceMapper.getBasicAnchorDFMapForJoin(
      sparkSession,
      allAnchoredFeatures.values.toSeq,
      failOnMissing)
    val updatedAnchorsWithSource = anchorsWithSource.filter(anchorEntry => anchorEntry._2.isDefined)
      .map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)

    // Only load DataFrames for anchors that have preprocessing UDF
    // So we filter out anchors that doesn't have preprocessing UDFs
    // We use feature names sorted and merged as the key to find the anchor
    // For example, f1, f2 belongs to anchor. Then Map("f1,f2"-> anchor)
    val dataFrameMapForPreprocessing = updatedAnchorsWithSource
      .filter(x => featureNamesInAnchorSet.contains(x._1.featureAnchor.features.toSeq.sorted.mkString(",")))
      .map(x => (x._1.featureAnchor.features.toSeq.sorted.mkString(","), x._2.get()))

    // Pyspark only understand Java map so we need to convert Scala map back to Java map.
    dataFrameMapForPreprocessing.asJava
  }

  def mainWithPreprocessedDataFrame(args: Array[String], preprocessedDfMap: java.util.Map[String, DataFrame]) {
    // Set the preprocessed DataFrame here for future usage.
    PreprocessedDataFrameManager.preprocessedDfMap = preprocessedDfMap.asScala.toMap

    main(args)
  }

  def main(args: Array[String]) {
    logger.info("FeatureJoinJob args are: " + args.mkString("Array(", ", ", ")"))
    val feathrJoinPreparationInfo = prepareSparkSession(args)

    run(feathrJoinPreparationInfo.sparkSession, feathrJoinPreparationInfo.hadoopConf, feathrJoinPreparationInfo.jobContext, List()) //TODO: accept handlers instead of empty List
  }

  def prepareSparkSession(args: Array[String]): FeathrJoinPreparationInfo = {
    val jobContext = parseInputArgument(args)

    val sparkConf = new SparkConf().registerKryoClasses(Array(classOf[GenericRecord]))
    // sparkConf.set("spark.kryo.registrator", "org.apache.spark.serializer.AvroGenericArrayKryoRegistrator")

    val sparkSessionBuilder = SparkSession
      .builder()
      .config(sparkConf)
      .appName(getClass.getName)
      .enableHiveSupport()

    val sparkSession = sparkSessionBuilder.getOrCreate()
    val conf = sparkSession.sparkContext.hadoopConfiguration

    DataSourceConfigUtils.setupHadoopConf(sparkSession, jobContext.dataSourceConfigs)

    FeathrUdfRegistry.registerUdf(sparkSession)
    jobContext.jobJoinContext.outputPath match {
      case SimplePath(path) => HdfsUtils.deletePath(path, recursive = true, conf)
      case _ => {}
    }

    val enableDebugLog = FeathrUtils.getFeathrJobParam(sparkConf, FeathrUtils.ENABLE_DEBUG_OUTPUT).toBoolean
    if (enableDebugLog) {
      Configurator.setAllLevels(LogManager.getRootLogger.getName, Level.DEBUG)
    }

    FeathrJoinPreparationInfo(sparkSession, conf, jobContext)
  }
}

case class FeathrJoinPreparationInfo(sparkSession: SparkSession, hadoopConf: Configuration, jobContext: FeathrJoinJobContext)

case class FeathrJoinJobContext(joinConfig: String, jobJoinContext: JoinJobContext, dataSourceConfigs: DataSourceConfigs, useFCM: Boolean) {}

/**
 * This case class describes feature record after join process
 */
case class FeathrJointFeatureRecord[L: ClassTag](observation: L, joinedFeatures: Map[StringTaggedFeatureName, common.FeatureValue])

package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.generation.SparkIOUtils
import org.apache.log4j.{Level, Logger}
import org.apache.logging.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.Properties
import java.util.concurrent.atomic.AtomicLong

private[feathr] object FeathrUtils {

  val ENVIRONMENT = "offline"
  val ENABLE_DEBUG_OUTPUT = "debug.enabled"
  val DEBUG_FEATURE_NAMES = "debug.feature.names"
  val DEBUG_OUTPUT_PATH = "debug.output.path"
  val DEBUG_OUTPUT_PART_NUM = "debug.output.num.parts"
  val FEATHR_PARAMS_PREFIX = "spark.feathr."
  /*
   * The execution config controls feathr-offline behavior when loading date partitioned feature data.
   * If set to true, feature join / feature generation workflows fail if a date partition is missing in feature data source.
   * Else, the missing partition is ignored.
   * Default value: false.
   */
  val FAIL_ON_MISSING_PARTITION = "fail.on.missing.partition"
  /*
   * When a base feature in Sequential Join has ArrayType keys, the DataFrame join condition is constructed as array_contains.
   * In Spark 2.3, this results in BroadcastNestedLoopJoin which adds memory pressure on the driver. (See: APA-21936).
   * This is a user hidden / dev only config when enabled, explodes ArrayType join keys to avoid BroadcastNestedLoopJoin.
   */
  val SEQ_JOIN_ARRAY_EXPLODE_ENABLED = "seq.join.array.explode.enabled"
  val ENABLE_SALTED_JOIN = "enable.salted.join"
  val SKIP_MISSING_FEATURE = "skip.missing.feature"
  val SALTED_JOIN_FREQ_ITEM_THRESHOLD = "salted.join.freq.item.threshold"
  val SALTED_JOIN_FREQ_ITEM_ESTIMATOR = "salted.join.freq.item.estimator"
  val SALTED_JOIN_PERSIST = "salted.join.persist"
  val SALTED_JOIN_REPLICATION_FACTOR_HIGH = "salted.join.replication.factor.high"
  val ENABLE_SLICK_JOIN = "enable.slickJoin"
  val ENABLE_METRICS = "enable.metrics"
  val ENABLE_CHECKPOINT = "enable.checkpoint"
  val CHECKPOINT_FREQUENCY = "checkpoint.frequency"
  val ROW_BLOOMFILTER_MAX_THRESHOLD = "row.bloomfilter.maxThreshold"
  val SPARK_JOIN_MAX_PARALLELISM = "max.parallelism"
  val CHECKPOINT_OUTPUT_PATH = "checkpoint.dir"
  val SPARK_JOIN_MIN_PARALLELISM = "min.parallelism"
  val MAX_DATA_LOAD_RETRY = "max.data.load.retry"
  val DATA_LOAD_WAIT_IN_MS = "data.load.wait.in.ms"
  val ENABLE_SANITY_CHECK_MODE = "enable.sanity.check.mode"
  val SANITY_CHECK_MODE_ROW_COUNT = "sanity.check.row.count"
  val STRING_PARAMETER_DELIMITER = ","

  // Used to check if the current dataframe has satisfied the checkpoint frequency
  val checkPointSequenceNumber = new AtomicLong(0)

  val sqlConfsWithDefaultParam = Map(
    ENABLE_DEBUG_OUTPUT  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_DEBUG_OUTPUT )).stringConf.createOptional, "false"),
    DEBUG_FEATURE_NAMES  -> (SQLConf.buildConf(getFullConfigKeyName(DEBUG_FEATURE_NAMES )).stringConf.createOptional, ""),
    DEBUG_OUTPUT_PATH  -> (SQLConf.buildConf(getFullConfigKeyName(DEBUG_OUTPUT_PATH )).stringConf.createOptional, "/tmp/debug/feathr/output"),
    CHECKPOINT_OUTPUT_PATH  -> (SQLConf.buildConf(getFullConfigKeyName(CHECKPOINT_OUTPUT_PATH )).stringConf.createOptional, "/tmp/feathr/checkpoints"),
    ENABLE_CHECKPOINT  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_CHECKPOINT )).stringConf.createOptional, "false"),
    // Check point every {CHECKPOINT_FREQUENCY} dataframes
    CHECKPOINT_FREQUENCY  -> (SQLConf.buildConf(getFullConfigKeyName(CHECKPOINT_FREQUENCY )).stringConf.createOptional, "10"),
    DEBUG_OUTPUT_PART_NUM  -> (SQLConf.buildConf(getFullConfigKeyName(DEBUG_OUTPUT_PART_NUM )).stringConf.createOptional, "200"),
    FAIL_ON_MISSING_PARTITION  -> (SQLConf.buildConf(getFullConfigKeyName(FAIL_ON_MISSING_PARTITION )).stringConf.createOptional, "false"),
    SEQ_JOIN_ARRAY_EXPLODE_ENABLED  -> (SQLConf.buildConf(getFullConfigKeyName(SEQ_JOIN_ARRAY_EXPLODE_ENABLED )).stringConf.createOptional, "true"),
    ENABLE_SALTED_JOIN  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_SALTED_JOIN )).stringConf.createOptional, "false"),
    SKIP_MISSING_FEATURE  -> (SQLConf.buildConf(getFullConfigKeyName(SKIP_MISSING_FEATURE )).stringConf.createOptional, "false"),
    MAX_DATA_LOAD_RETRY -> (SQLConf.buildConf(getFullConfigKeyName(MAX_DATA_LOAD_RETRY)).stringConf.createOptional, "0"),
    DATA_LOAD_WAIT_IN_MS -> (SQLConf.buildConf(getFullConfigKeyName(DATA_LOAD_WAIT_IN_MS)).stringConf.createOptional, "1"),
    // If one key appears more than 0.02% in the dataset, we will salt this join key and split them into multiple partitions
    // This is an empirical value
    SALTED_JOIN_FREQ_ITEM_THRESHOLD  -> (SQLConf.buildConf(getFullConfigKeyName(SALTED_JOIN_FREQ_ITEM_THRESHOLD )).stringConf.createOptional, "0.0002"),
    SALTED_JOIN_REPLICATION_FACTOR_HIGH  -> (SQLConf.buildConf(getFullConfigKeyName(SALTED_JOIN_REPLICATION_FACTOR_HIGH )).stringConf.createOptional, "10"),
    SALTED_JOIN_FREQ_ITEM_ESTIMATOR  -> (SQLConf.buildConf(getFullConfigKeyName(SALTED_JOIN_FREQ_ITEM_ESTIMATOR )).stringConf.createOptional, "spark"),
    ENABLE_SLICK_JOIN  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_SLICK_JOIN )).stringConf.createOptional, "false"),
    SALTED_JOIN_PERSIST  -> (SQLConf.buildConf(getFullConfigKeyName(SALTED_JOIN_PERSIST )).stringConf.createOptional, "true"),
    ROW_BLOOMFILTER_MAX_THRESHOLD  -> (SQLConf.buildConf(getFullConfigKeyName(ROW_BLOOMFILTER_MAX_THRESHOLD )).stringConf.createOptional, "-1"),
    // We found that if we have too many parallelism, then during the join shuffling, memoryOverhead could be too high,
    ENABLE_METRICS  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_METRICS )).stringConf.createOptional, "false"),
    // cap it to 10000 to make sure memoryOverhead is less than 2g (Feathr default value)
    SPARK_JOIN_MAX_PARALLELISM  -> (SQLConf.buildConf(getFullConfigKeyName(SPARK_JOIN_MAX_PARALLELISM )).stringConf.createOptional, "10000"),
    SPARK_JOIN_MIN_PARALLELISM  -> (SQLConf.buildConf(getFullConfigKeyName(SPARK_JOIN_MIN_PARALLELISM )).stringConf.createOptional, "10"),
    ENABLE_SANITY_CHECK_MODE  -> (SQLConf.buildConf(getFullConfigKeyName(ENABLE_SANITY_CHECK_MODE )).stringConf.createOptional, "false"),
    SANITY_CHECK_MODE_ROW_COUNT  -> (SQLConf.buildConf(getFullConfigKeyName(SANITY_CHECK_MODE_ROW_COUNT )).stringConf.createOptional, "10")
  )

  /**
   * Get Feathr Offline version string from .properties file that gets created at build time
   * @return Feathr Offline version string
   */
  def feathrVersion: String = {
    val versionProperties = new Properties()
    val resourceStream = getClass.getClassLoader.getResourceAsStream("metric.properties")
    versionProperties.load(resourceStream)
    versionProperties.getProperty("version")
  }

  /**
   * Get Feathr job parameter values
   * @param ss spark session
   * @param paramName parameter name
   */
  def getFeathrJobParam(ss: SparkSession, paramName: String): String = {
    val sparkConf = ss.sparkContext.getConf
    getFeathrJobParam(sparkConf, paramName)
  }

  /**
   * Get Feathr job parameter values
   * @param sparkConf spark conf
   * @param paramName parameter name
   */
  def getFeathrJobParam(sparkConf: SparkConf, paramName: String): String = {
    val key = getFullConfigKeyName(paramName)
    val conf = sqlConfsWithDefaultParam.get(paramName)
    if (conf.isDefined) {
      val sqlConfValue = SQLConf.get.getConf(conf.get._1)
      sqlConfValue.getOrElse(sparkConf.get(key, conf.get._2))
    } else {
      sparkConf.get(key)
    }
  }

  /**
   * Set the job parameter value
   * @param paramName parameter name
   * @param value value to set
   */
  def setFeathrJobParam(paramName: String, value: String): Unit = {
    val conf = sqlConfsWithDefaultParam.get(paramName)
    if (conf.isDefined) {
      val key = getFullConfigKeyName(paramName)
      SQLConf.get.setConfString(key, value)
    } else {
      log.warn(s"Unsupported parameter ${paramName}. Please check parameter name.")
    }
  }

  /**
   * Get the full parameter name as key
   * @param paramName parameter setting name
   */
  def getFullConfigKeyName(paramName: String) = {
    s"${FEATHR_PARAMS_PREFIX}${paramName}"
  }

  /**
   * Check and enable debug logging if it is configured by the end user
 *
   * @param sparkConf spark conf
   */
  def enableDebugLogging(sparkConf: SparkConf): Unit = {
    val enableDebugLog = isDebugOutputEnabled(sparkConf)
    if (enableDebugLog) {
      Logger.getRootLogger.setLevel(Level.DEBUG)
    }
  }

  /**
   * Helper method to parse a hdfs file path
   *
   * @param path File path string
   * @return contents of the file as a string
   */
  def readFileFromHDFS(ss: SparkSession, path: String): String =
    ss.sparkContext.textFile(path).collect.mkString("\n")

  /**
   * Check if debug output is configured by the end user
   * @param sparkConf spark conf
   */
  private def isDebugOutputEnabled(sparkConf: SparkConf): Boolean = {
    FeathrUtils.getFeathrJobParam(sparkConf, FeathrUtils.ENABLE_DEBUG_OUTPUT).toBoolean
  }

  /**
   * Dump a dataframe to disk if debug mode is enabled.
   * The function will check the features against the 'spark.feathr.debug.feature.names',
   * making sure there's overlap before dumping the dataframe.
   * @param ss spark session
   * @param df input dataframe
   * @param features features in the input dataframe
   * @param tag tag used in the log
   * @param pathSuffix path suffix used to dump the dataframe
   */
  def dumpDebugInfo(ss: SparkSession,
                            df: DataFrame,
                            features: Set[String],
                            tag: String,
                            pathSuffix: String): Unit = {
    if (isDebugMode(ss)) {
      val basePath = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.DEBUG_OUTPUT_PATH)
      val debugFeatureNames = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.DEBUG_FEATURE_NAMES)
        .split(FeathrUtils.STRING_PARAMETER_DELIMITER).toSet
      val featureNames = "features_" + features.mkString("_") + "_"
      if (debugFeatureNames.isEmpty || features.intersect(debugFeatureNames).nonEmpty) {
        val savePath = SimplePath(basePath + "/" + featureNames + pathSuffix)
        log.info(s"${tag}, Start dumping data ${featureNames} to ${savePath}")
        if (!df.isEmpty) {
          SparkIOUtils.writeDataFrame(df, savePath, Map(), List())
        }
        log.info(s"{tag}. Finish dumping data ${featureNames} to ${savePath}")
      } else {
        log.info(s"{tag}. Skipping dumping data as feature names to debug are ${debugFeatureNames}, " +
          s"and current dataframe has feature ${featureNames}")
      }
    }
  }

  def isDebugMode(ss: SparkSession) = {
    FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.ENABLE_DEBUG_OUTPUT).toBoolean
  }

  /**
   * Check if we should checkpoint for the current call
   *
   * @param ss SparkSession
   * @return
   */
  def shouldCheckPoint(ss: SparkSession): Boolean = {
    val enableCheckPoint = getFeathrJobParam(ss, FeathrUtils.ENABLE_CHECKPOINT).toBoolean
    if (enableCheckPoint) {
      val currentCount = checkPointSequenceNumber.getAndIncrement()
      val checkpoint_frequency = getFeathrJobParam(ss, FeathrUtils.CHECKPOINT_FREQUENCY).toInt
      currentCount % checkpoint_frequency == 0
    } else {
      false
    }
  }

  @transient lazy val log = LogManager.getLogger(getClass.getName)

}

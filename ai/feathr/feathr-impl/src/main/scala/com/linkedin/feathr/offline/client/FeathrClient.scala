package com.linkedin.feathr.offline.client

import com.linkedin.feathr.common.exception._
import com.linkedin.feathr.common.{FeatureInfo, Header, InternalApi, JoiningFeatureParams, RichConfig, TaggedFeatureName}
import com.linkedin.feathr.offline.config.sources.FeatureGroupsUpdater
import com.linkedin.feathr.offline.config.{FeathrConfig, FeathrConfigLoader, FeatureGroupsGenerator, FeatureJoinConfig}
import com.linkedin.feathr.offline.generation.{DataFrameFeatureGenerator, FeatureGenKeyTagAnalyzer, StreamingFeatureGenerator}
import com.linkedin.feathr.offline.job._
import com.linkedin.feathr.offline.join.DataFrameFeatureJoiner
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlanner}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.util._
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.UUID
import scala.util.{Failure, Success}

/**
 * FeathrClient is the entry point into Feathr for joining observation data with features. To achieve this, instantiate this class
 * using the FeathrClient#getFeathrClientWithConfStrings or FeathrClient#getFeathrClientWithConfFile, then use either joinFeatures or
 * joinFeatureAsRdd method.
 *
 * The FeathrClient takes in a [[FeatureGroups]] object, which can be created from the featureDefConf files using the [[FeatureGroupsGenerator]]
 * class.
 *
 */
class FeathrClient private[offline] (sparkSession: SparkSession, featureGroups: FeatureGroups, logicalPlanner: MultiStageJoinPlanner,
  featureGroupsUpdater: FeatureGroupsUpdater, dataPathHandlers: List[DataPathHandler], mvelContext: Option[FeathrExpressionExecutionContext]) {
  private val log = LogManager.getLogger(getClass)

  type KeyTagStringTuple = Seq[String]
  private[offline] val allAnchoredFeatures = featureGroups.allAnchoredFeatures
  private[offline] val allDerivedFeatures = featureGroups.allDerivedFeatures
  private[offline] val allPassthroughFeatures = featureGroups.allPassthroughFeatures
  private[offline] val allWindowAggFeatures = featureGroups.allWindowAggFeatures
  private[offline] val allSeqJoinFeatures = featureGroups.allSeqJoinFeatures

  /**
   * Joins observation data on the feature data. Observation data is loaded as SparkFeaturizedDataset, and the
   * joined data is returned as a SparkFeaturizedDataset.
   *
   * This API takes feathr's internal feature-join.md instead of the PDL config, and it will be only used inside feathr,
   * to support LocalJoinJob, which are used by other repo for testing. We should remove it once the users can migrate to
   * use the PDL config.
   * <p>
   * The feature values in the DataFrame are represented as tensors. Values are represented as struct of arrays
   * which is a columnar representation where each dimension and tensor value is in its own column. The schema in
   * the latter case is the one specified for tensors
   * avro-schemas repo for details).
   *
   * @param joinConfig feathr offline's [[FeatureJoinConfig]]
   * @param obsData    Observation data taken in as a [[SparkFeaturizedDataset]].
   * @param jobContext [[JoinJobContext]]
   * @return Joined observation and feature data as a SparkFeaturizedDataset.
   */
  @InternalApi
  def joinFeatures(joinConfig: FeatureJoinConfig, obsData: SparkFeaturizedDataset, jobContext: JoinJobContext = JoinJobContext()): SparkFeaturizedDataset = {
    val sparkConf = sparkSession.sparkContext.getConf
    FeathrUtils.enableDebugLogging(sparkConf)

    val (joinedDF, header) = doJoinObsAndFeatures(joinConfig, jobContext, obsData.data)
    SparkFeaturizedDataset(joinedDF, FeaturizedDatasetMetadata(header=Some(header)))
  }

  /**
   * Generates features by extracting feature data from its source. It returns generated features, the DataFrame for feature data
   * and the feature related metadata. The API takes in [[FeatureGenSpec]] as the input, which is expected to contain all the
   * necessary information for generating features.
   *
   * The FeathrClient does the planning and hands over the execution to [[DataFrameFeatureGenerator]].
   *
   * @param featureGenSpec feature generation specification
   * @return generated feature data as SparkFeaturizedDataset,
   *         if having output processor, return the merge processed feature data of the all processors
   *         otherwise, return the unprocessed data
   */
  @InternalApi
  def generateFeatures(featureGenSpec: FeatureGenSpec): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    prepareExecuteEnv()
    // Compute KeyTags for Anchored and Derived features
    val keyTaggedAnchoredFeatures = FeatureGenKeyTagAnalyzer.inferKeyTagsForAnchoredFeatures(featureGenSpec, featureGroups)
    val keyTaggedDerivedFeatures = FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(featureGenSpec, featureGroups, keyTaggedAnchoredFeatures)
    val keyTaggedRequiredFeatures = keyTaggedAnchoredFeatures ++ keyTaggedDerivedFeatures
    if (isStreaming(featureGenSpec)) {
      val streamingFeatureGenerator = new StreamingFeatureGenerator(dataPathHandlers=dataPathHandlers)
      streamingFeatureGenerator.generateFeatures(sparkSession, featureGenSpec, featureGroups, keyTaggedRequiredFeatures)
      Map()
    } else {
      // Get logical plan
      val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedRequiredFeatures)
      // This pattern is consistent with the join use case which uses DataFrameFeatureJoiner.
      val dataFrameFeatureGenerator = new DataFrameFeatureGenerator(logicalPlan=logicalPlan,dataPathHandlers=dataPathHandlers, mvelContext)
      val featureMap: Map[TaggedFeatureName, (DataFrame, Header)] =
        dataFrameFeatureGenerator.generateFeaturesAsDF(sparkSession, featureGenSpec, featureGroups, keyTaggedRequiredFeatures)

      featureMap map {
        case (taggedFeatureName, (df, _)) =>
          // Build FDS and convert key columns to Feature columns instead of Opaque columns
          val fds = SparkFeaturizedDataset(df, FeaturizedDatasetMetadata())
          (taggedFeatureName, fds)
      }
    }
  }

  /**
   * Check if the feature generation job needs to run in streaming mode
   * @param featureGenSpec the feature generation specification.
   * @return  If 'streaming' is set to true in any of its output processor, return true, otherwise, return false.
   */
  private def isStreaming(featureGenSpec: FeatureGenSpec) = {
    val outputProcessors = featureGenSpec.getProcessorList()
    if (!outputProcessors.isEmpty) {
      outputProcessors.map(p=>
          p.outputProcessorConfig.getParams.getBooleanWithDefault("streaming", false)
        )
        .reduce(_ || _)
    } else {
      false
    }
  }

  private def prepareExecuteEnv(): Unit = {
    SQLConf.get.setConfString("spark.sql.legacy.allowUntypedScalaUDF", "true")
  }


  /**
   * get all the sources in the feature configs
   * @param requestedFeatures Requested features
   */
  private[offline] def getAllFeatureSources(requestedFeatures: Seq[JoiningFeatureParams]): List[DataSource] = {
    // build dependency graph
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, requestedFeatures)
    val allRequiredFeatures = logicalPlan.allRequiredFeatures
    val sources = allRequiredFeatures.collect {
      case feature if allAnchoredFeatures.contains(feature.getFeatureName) =>
        val featureName = feature.getFeatureName
        allAnchoredFeatures(featureName).source
    }.distinct
    sources.toList
  }

  /**
   * Joins observation data on the feature data. Both observation and feature data are loaded as DataFrame, and the
   * joined data is returned as a DataFrame. In this, the observation or feature data have no time context. We treat the
   * entire observation as a single partition.
   *
   * @param joinConfig                 [[FeatureJoinConfig]]
   * @param jobContext                 [[JoinJobContext]]
   * @param obsData                    Observation data taken in as [[DataFrame]]
   * @return A tuple of joined observation and feature data as a DataFrame, and the header. The header provides a
   *         mapping from the feature name to the column name in the dataframe.
   */
  private[offline] def doJoinObsAndFeatures(joinConfig: FeatureJoinConfig, jobContext: JoinJobContext, obsData: DataFrame): (DataFrame, Header) = {
    log.info("All anchored feature names (sorted):\n\t" + stringifyFeatureNames(allAnchoredFeatures.keySet))
    log.info("All derived feature names (sorted):\n\t" + stringifyFeatureNames(allDerivedFeatures.keySet))
    prepareExecuteEnv()
    val enableCheckPoint = FeathrUtils.getFeathrJobParam(sparkSession, FeathrUtils.ENABLE_CHECKPOINT).toBoolean
    val checkpointDir = FeathrUtils.getFeathrJobParam(sparkSession, FeathrUtils.CHECKPOINT_OUTPUT_PATH)
    if (enableCheckPoint) {
      if (checkpointDir.equals("")) {
        throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR,
          s"Please set ${FeathrUtils.FEATHR_PARAMS_PREFIX}${FeathrUtils.CHECKPOINT_OUTPUT_PATH} to a folder with write permission.")
      }
      // clean up old checkpoints
      HdfsUtils.deletePath(checkpointDir, true)
      sparkSession.sparkContext.setCheckpointDir(checkpointDir)
    }

    val featureGroupings = joinConfig.featureGroupings

    log.info(s"Join job context: ${jobContext})")
    log.info(s"joinConfig: ${joinConfig}")
    log.info(s"featureGroupings passed in: ${featureGroupings}")

    val rowBloomFilterThreshold = FeathrUtils.getFeathrJobParam(sparkSession, FeathrUtils.ROW_BLOOMFILTER_MAX_THRESHOLD).toInt
    val joinFeatures = featureGroupings.values.flatten.toSeq.distinct

    val (joinedDF, header) = {
      if (featureGroupings.isEmpty) {
        log.warn("Feature groupings from the join config is empty, returning the obs data without joining any features.")
        (obsData, new Header(Map.empty[TaggedFeatureName, FeatureInfo]))
      } else {
        joinFeaturesAsDF(joinConfig, joinFeatures, obsData, Some(rowBloomFilterThreshold))
      }
    }

    if (log.isDebugEnabled) {
      log.debug("joinedDF:")
      joinedDF.show(false)
      log.debug(s"header featureInfoMap: ${header.featureInfoMap}")
    }
    (joinedDF, header)
  }

  /**
   * Find feature names that are the same as the field names (from observation data)
   * @param keyTaggedFeatures request features
   * @param fieldNames observation data field names
   * @return feature names that are the same as field names
   */
  private def findConflictFeatureNames(keyTaggedFeatures: Seq[JoiningFeatureParams], fieldNames: Seq[String]): Seq[String] = {
   keyTaggedFeatures.map(_.featureName).intersect(fieldNames)
  }

  /**
   * Rename feature names conflicting with data set column names
   * by applying provided suffix
   * Eg. If have the feature name 'example' and the suffix '1',
   * it will become 'example_1" after renaming
   *
   * @param df                original dataframe
   * @param header            original header
   * @param conflictFeatureNames conflicted feature names
   * @param suffix            suffix to apply to feature names
   *
   * @return pair of renamed (dataframe, header)
   */
  private[offline] def renameFeatureNames(
                                           df: DataFrame,
                                           header: Header,
                                           conflictFeatureNames: Seq[String],
                                           suffix: String): (DataFrame, Header) = {
    val uuid = UUID.randomUUID()
    var renamedDF = df
    conflictFeatureNames.foreach(name => {
      renamedDF = renamedDF.withColumnRenamed(name, name + '_' + uuid)
      renamedDF = renamedDF.withColumnRenamed(name + '_' + suffix, name)
      renamedDF = renamedDF.withColumnRenamed(name + '_' + uuid, name + '_' + suffix)
    })

    val featuresInfoMap = header.featureInfoMap.map {
      case (featureName, featureInfo) =>
        val name = featureInfo.columnName
        val conflict = conflictFeatureNames.contains(name)
        val fi = if (conflict) new FeatureInfo(name + '_' + suffix, featureInfo.featureType) else featureInfo
        val fn = if (conflict) new TaggedFeatureName(featureName.getKeyTag, name + '_' + suffix) else featureName
        fn -> fi
    }
    (renamedDF, new Header(featuresInfoMap))
  }

  /**
   * Join all requested feature to the observation dataset
   *
   * @param joinConfig         Feature Join Config
   * @param keyTaggedFeatures  requested features
   * @param left               observation data
   * @param rowBloomFilterThreshold
   * @return pair of (joined dataframe, header)
   */
  private[offline] def joinFeaturesAsDF(
      joinConfig: FeatureJoinConfig,
      keyTaggedFeatures: Seq[JoiningFeatureParams],
      left: DataFrame,
      rowBloomFilterThreshold: Option[Int] = None): (DataFrame, Header) = {
    if (left.head(1).isEmpty) {
      log.info("Observation is empty")
      return (left, new Header(Map.empty[TaggedFeatureName, FeatureInfo]))
    }

    /**
     * The original feature groups may need to be updated after seeing the join features requested. Currently,
     * we handle 2 cases:-
     *     1. If requested with a feature alias, and a time delay is specified, we will insert this into the the feature groups
     * and treat it as a new feature.
     *     2. If dateParams are specified for any feature, update the "FeatureAnchorWithSource" object to decorate it with the specified
     * dateParams.
     */
    val updatedFeatureGroups = featureGroupsUpdater.updateFeatureGroups(featureGroups, keyTaggedFeatures)

    var logicalPlan = logicalPlanner.getLogicalPlan(updatedFeatureGroups, keyTaggedFeatures)
    val shouldSkipFeature = FeathrUtils.getFeathrJobParam(sparkSession.sparkContext.getConf, FeathrUtils.SKIP_MISSING_FEATURE).toBoolean
    val featureToPathsMap = (for {
      requiredFeature <- logicalPlan.allRequiredFeatures
      featureAnchorWithSource <- allAnchoredFeatures.get(requiredFeature.getFeatureName)
    } yield (requiredFeature.getFeatureName -> featureAnchorWithSource.source.path)).toMap
    if (!sparkSession.sparkContext.isLocal) {
      // Check read authorization for all required features
      val featurePathsTest = AclCheckUtils.checkReadAuthorization(sparkSession, logicalPlan.allRequiredFeatures, allAnchoredFeatures)
      featurePathsTest._1 match {
        case Failure(exception) => // If skip feature, remove the corresponding anchored feature from the feature group and produce a new logical plan
          if (shouldSkipFeature) {
            val featureGroupsWithoutInvalidFeatures = FeatureGroupsUpdater()
              .getUpdatedFeatureGroupsWithoutInvalidPaths(featureToPathsMap, updatedFeatureGroups, featurePathsTest._2)
            logicalPlanner.getLogicalPlan(featureGroupsWithoutInvalidFeatures, keyTaggedFeatures)
          } else {
            throw new FeathrInputDataException(
              ErrorLabel.FEATHR_USER_ERROR,
              "Unable to verify " +
                "read authorization on feature data, it can be due to the following reasons: 1) input not exist, 2) no permission.",
              exception)
          }
        case Success(_) => log.debug("Checked read authorization on all feature data")
      }
    }
    val invalidFeatureNames = findInvalidFeatureRefs(keyTaggedFeatures)
    if (invalidFeatureNames.nonEmpty) {
      throw new FeathrInputDataException(
        ErrorLabel.FEATHR_USER_ERROR,
        "Feature names must conform to " +
          s"regular expression: ${AnchorUtils.featureNamePattern}, but found feature names: $invalidFeatureNames")
    }

    val joiner = new DataFrameFeatureJoiner(logicalPlan=logicalPlan,dataPathHandlers=dataPathHandlers, mvelContext)
    // Check conflicts between feature names and data set column names
    val conflictFeatureNames: Seq[String] = findConflictFeatureNames(keyTaggedFeatures, left.schema.fieldNames)
    val joinConfigSettings = joinConfig.settings
    val conflictsAutoCorrectionSetting = if(joinConfigSettings.isDefined) joinConfigSettings.get.conflictsAutoCorrectionSetting else None
    if (conflictFeatureNames.nonEmpty) {
      if(!conflictsAutoCorrectionSetting.isDefined) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          "Feature names must be different from field names in the observation data. " +
            s"Please rename feature ${conflictFeatureNames} or rename the same field names in the observation data.")
      }

      // If auto correction is required, will solve conflicts automatically
      val renameFeatures = conflictsAutoCorrectionSetting.get.renameFeatureList
      val suffix = conflictsAutoCorrectionSetting.get.suffix
      log.warn(s"Found conflicted field names: ${conflictFeatureNames}. Will auto correct them by applying suffix: ${suffix}")
      var leftRenamed = left
      conflictFeatureNames.foreach(name => {
        leftRenamed = leftRenamed.withColumnRenamed(name, name+'_'+suffix)
      })
      val conflictFeatureNames2: Seq[String] = findConflictFeatureNames(keyTaggedFeatures, leftRenamed.schema.fieldNames)
      if (conflictFeatureNames2.nonEmpty) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Failed to apply auto correction to solve conflicts. Still got conflicts after applying provided suffix ${suffix} for fields: " +
              s"${conflictFeatureNames}. Please provide another suffix or solve conflicts manually.")
      }
      val (df, header) = joiner.joinFeaturesAsDF(sparkSession, joinConfig, updatedFeatureGroups, keyTaggedFeatures, leftRenamed, rowBloomFilterThreshold)
      if(renameFeatures) {
        log.warn(s"Suffix :${suffix} is applied into feature names: ${conflictFeatureNames} to avoid conflicts in outputs")
        renameFeatureNames(df, header, conflictFeatureNames, suffix)
      } else {
        log.warn(s"Suffix :${suffix} is applied into dataset Column names: ${conflictFeatureNames} to avoid conflicts in outputs")
        (df, header)
      }
    }
    else
      joiner.joinFeaturesAsDF(sparkSession, joinConfig, updatedFeatureGroups, keyTaggedFeatures, left, rowBloomFilterThreshold)
  }

  private[offline] def getFeatureGroups(): FeatureGroups = {
    featureGroups
  }

  private[offline] def getLogicalPlanner(): MultiStageJoinPlanner = {
    logicalPlanner
  }

  /**
   * Log the feature names for bookkeeping. Global config may be merged with local config(s)
   */
  private def stringifyFeatureNames(nameSet: Set[String]): String = nameSet.toSeq.sorted.toArray.mkString("\n\t")

  /*
   * Check feature name syntax
   */
  private def findInvalidFeatureRefs(features: Seq[JoiningFeatureParams]): List[String] = {
    features.foldLeft(List.empty[String]) { (acc, f) =>
      val featureRefStr = f.featureName.toString
      // featureRefStr could have '-' now.

      val featureRefStrInDF = DataFrameColName.getEncodedFeatureRefStrForColName(featureRefStr)
      val isValidSyntax = AnchorUtils.featureNamePattern.matcher(featureRefStrInDF).matches()
      if (isValidSyntax) acc
      else featureRefStr :: acc
    }
  }
}

object FeathrClient {

  /**
   * Create an instance of a builder for constructing a FeathrClient
   * @param sparkSession  the SparkSession required for the FeathrClient to perform its operations
   * @return  Builder class
   */
  def builder(sparkSession: SparkSession): Builder = {
    FeathrUdfRegistry.registerUdf(sparkSession)
    new Builder(sparkSession)
  }

  /**
   * Feathr Client builder class.
   * To create an instance of FeathrClient, we need a sparkSession and a feature def config or a local override def config or both. To use this builder,
   * instantiate this builder with a spark session. You would need
   * a. feature def config string or feature def config file path, or
   * b. local override def config string or local override def config file path
   * c. At least one of the above parameters is required.
   *
   * Here are a few examples on how to instantiate a FeathrClient.
   *
   * FeathrClient.builder(ss).addFeatureDef(..).build() // build with a feature definition content
   * FeathrClient.builder(ss).addFeatureDef(..).addFeatureDefPath(..).build() // build with a combination of feature definition content and path
   *
   * FeathrClient.builder(ss).addFeatureDef(..).addLocalOverrideFeatureDef(..).build() // build with a feature definition with a localOverride
   * FeathrClient.builder(ss).addFeatureDef(..).addLocalOverrideFeatureDef(..).addLocalOverrideDefPath(..).build() // build with a feature definition
   *                                                                                      //and a combination of local override config content and path
   * This returns an instance of FeathrClient, which can be used to join/generate features.
   *
   * @param sparkSession  the SparkSession required for the FeathrClient to perform its operations
   * @return  a new instance of a FeathrClient based on the provided feature definitions (including any local override definitions)
   */
  class Builder private[FeathrClient] (sparkSession: SparkSession) {
    private val feathrConfigLoader = FeathrConfigLoader()
    private var featureDef: List[String] = List()
    private var localOverrideDef: List[String] = List()
    private var featureDefPath: List[String] = List()
    private var localOverrideDefPath: List[String] = List()
    private var featureDefConfs: List[FeathrConfig] = List()
    private var dataPathHandlers: List[DataPathHandler] = List()
    private var mvelContext: Option[FeathrExpressionExecutionContext] = None;


    /**
     * Add a list of data path handlers to the builder. Used to handle accessing and loading paths caught by user's udf, validatePath
     *
     * @param dataPathHandlers custom data path handlers
     * @return FeathrClient.Builder
     */
    def addDataPathHandlers(dataPathHandlers: List[DataPathHandler]): Builder = {
      this.dataPathHandlers = dataPathHandlers ++ this.dataPathHandlers
      this
    }

    /**
     * Add a data path handler to the builder. Used to handle accessing and loading paths caught by user's udf, validatePath
     *
     * @param dataPathHandler custom data path handler
     * @return FeathrClient.Builder
     */
    def addDataPathHandler(dataPathHandler: DataPathHandler): Builder = {
      this.dataPathHandlers = dataPathHandler :: this.dataPathHandlers
      this
    }

    /**
     * Same as {@code addDataPathHandler(DataPathHandler)} but the input dataPathHandlers is optional and when it is missing,
     * this method performs an no-op.
     *
     * @param dataPathHandler custom data path handler
     * @return FeathrClient.Builder
     */
    def addDataPathHandler(dataPathHandler: Option[DataPathHandler]): Builder = {
      if (dataPathHandler.isDefined) addDataPathHandler(dataPathHandler.get) else this
    }


    /**
     * Add a feature definition config string to the builder.
     *
     * Note: When multiple feature definitions configs are added to the builder here or using {@code addFeatureDefPath()},
     * the same feature name is not allowed across multiple definition configs.
     *
     * @param featureDef feature def config string
     * @return FeathrClient.Builder
     */
    def addFeatureDef(featureDef: String): Builder = {
      this.featureDef = featureDef :: this.featureDef
      this
    }

    /**
     * Same as {@code addFeatureDef(String)} but the input feature definition string is optional and when it is missing,
     * this method performs an no-op.
     *
     * @param featureDef feature def config string option
     * @return FeathrClient.Builder
     */
    def addFeatureDef(featureDef: Option[String]): Builder = {
      if (featureDef.isDefined) addFeatureDef(featureDef.get) else this
    }

    /**
     * Add a local override feature definition string to the builder.  A feature defined in this override config will replace
     * the same feature defined in the definitions created from {@code addFeatureDef()} or {@code addFeatureDefPath()} methods.
     *
     * Note: When multiple local override configs are added to the builder here or using {@code addLocalOverrideDefPath()}, the
     * same feature name is not allowed across multiple override files.
     *
     * @param localOverrideDef local feature def config string
     * @return FeathrClient.Builder
     */
    def addLocalOverrideDef(localOverrideDef: String): Builder = {
      this.localOverrideDef = localOverrideDef :: this.localOverrideDef
      this
    }

    /**
     * Same as {@code addLocalOverrideDef(String)} except the input feature definition path is optional and when it is missing,
     * * this method performs an no-op.
     *
     * @param localOverrideDef local feature def config string option
     * @return FeathrClient.Builder
     */
    def addLocalOverrideDef(localOverrideDef: Option[String]): Builder = {
      if (localOverrideDef.isDefined) addFeatureDef(localOverrideDef.get) else this
    }

    /**
     * Add a feature definition config path to the builder.
     *
     * Note: When multiple feature definitions configs are added to the builder here or using {@code addFeatureDef()}, the
     * same feature name is not allowed across multiple definition configs.
     *
     * @param featureDefPath Feature def config file path
     * @return FeathrClient.Builder
     */
    def addFeatureDefPath(featureDefPath: String): Builder = {
      this.featureDefPath = featureDefPath :: this.featureDefPath
      this
    }

    /**
     * Same as {@code addFeatureDefPath(String)} except the input feature definition path is optional and when it is missing,
     * this method performs an no-op.
     *
     * @param featureDefPath Feature def config file path option
     * @return FeathrClient.Builder
     */
    def addFeatureDefPath(featureDefPath: Option[String]): Builder = {
      if (featureDefPath.isDefined) addFeatureDefPath(featureDefPath.get) else this
    }

    /**
     * Add a local override feature definition path to the builder. A feature defined in this override config will replace
     * the same feature defined in the definitions created from {@code addFeatureDef()} or {@code addFeatureDefPath()} methods.
     *
     * Note: When multiple local override configs are added to the builder here or using {@code addLocalOverrideDef()}, the same
     * feature cannot be across multiple override files.z
     *
     * @param localOverrideDefPath local feature def config file path option
     * @return FeathrClient.Builder
     */
    def addLocalOverrideDefPath(localOverrideDefPath: String): Builder = {
      this.localOverrideDefPath = localOverrideDefPath :: this.localOverrideDefPath
      this
    }

    /**
     * Same as {@code addLocalOverrideDefPath(String)} except the input feature definition path is optional and when it is missing,
     * this method performs an no-op.
     *
     * @param localOverrideDefPath local feature def config file path option
     * @return FeathrClient.Builder
     */
    def addLocalOverrideDefPath(localOverrideDefPath: Option[String]): Builder = {
      if (localOverrideDefPath.isDefined) addLocalOverrideDefPath(localOverrideDefPath.get) else this
    }

    /**
     * Some instances within Feathr call with directly a sequence of feathrConfigs. A convenience method for internal feathr usage
     * only.
     * @param featureDefConfs List of feathr configs, with a sequence of feature def configs
     * @return  FeathrClient.Builder
     */
    private[offline] def addFeatureDefConfs(featureDefConfs: Option[List[FeathrConfig]]): Builder = {
      if (featureDefConfs.isDefined) addFeatureDefConfs(featureDefConfs.get) else this
    }

    /**
     * Some instances within Feathr call with directly a sequence of feathrConfigs. A convenience method for internal feathr usage
     * only.
     * @param featureDefConfs List of feathr configs, with a sequence of feature def configs
     * @return  FeathrClient.Builder
     */
    private[offline] def addFeatureDefConfs(featureDefConfs: List[FeathrConfig]): Builder = {
      this.featureDefConfs = featureDefConfs
      this
    }
    def addFeathrExpressionContext(_mvelContext: Option[FeathrExpressionExecutionContext]): Builder = {
      this.mvelContext = _mvelContext
      this
    }

    /**
     * Build a new instance of the FeathrClient from the added feathr definition configs and any local overrides.
     *
     * @throws [[IllegalArgumentException]] an error when no feature definitions nor local overrides are configured.
     */
    def build(): FeathrClient = {
      require(
        !localOverrideDefPath.isEmpty || !localOverrideDef.isEmpty || !featureDefPath.isEmpty || !featureDef.isEmpty || !featureDefConfs.isEmpty,
        "Cannot build feathrClient without a feature def conf file/string or local override def conf file/string")

      // Append all the configs to this empty list, with the local override def config going last
      var featureDefConfigs = List.empty[FeathrConfig]
      var localDefConfigs = List.empty[FeathrConfig]

      // Local config, if provided, should always be the last element in the list of configs, so read it first
      if (featureDefPath.nonEmpty) {
        featureDefPath map (path =>
          readHdfsFile(Some(path))
            .foreach(cfg => featureDefConfigs = feathrConfigLoader.load(cfg) :: featureDefConfigs))
      }

      if (featureDef.nonEmpty) featureDef map (confStr => featureDefConfigs = feathrConfigLoader.load(confStr) :: featureDefConfigs)
      if (localOverrideDefPath.nonEmpty) {
        localOverrideDefPath map (path =>
          readHdfsFile(Some(path))
            .foreach(cfg => localDefConfigs = feathrConfigLoader.load(cfg) :: localDefConfigs))
      }
      if (localOverrideDef.nonEmpty) localOverrideDef map (confStr => localDefConfigs = feathrConfigLoader.load(confStr) :: localDefConfigs)

      // This ensures users cannot pass in the files as both list of feathr configs and also with a feature def config/local override def config
      featureDefConfigs = featureDefConfigs ++ featureDefConfs

      val featureGroups = FeatureGroupsGenerator(featureDefConfigs, Some(localDefConfigs)).getFeatureGroups()
      val feathrClient = new FeathrClient(sparkSession, featureGroups, MultiStageJoinPlanner(), FeatureGroupsUpdater(), dataPathHandlers, mvelContext)

      feathrClient
    }

    /**
     * Helper method to parse a hdfs file path
     *
     * @param path File path string
     * @return contents of the file as a string
     */
    private[offline] def readHdfsFile(path: Option[String]): Option[String] =
      path.map(p => sparkSession.sparkContext.textFile(p).collect.mkString("\n"))
  }
}

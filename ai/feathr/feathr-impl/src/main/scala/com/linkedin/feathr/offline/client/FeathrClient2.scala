package com.linkedin.feathr.offline.client

import com.linkedin.feathr.common.{FeatureTypeConfig, JoiningFeatureParams, TaggedFeatureName}
import com.linkedin.feathr.compute._
import com.linkedin.feathr.compute.converter.FeatureDefinitionsConverter
import com.linkedin.feathr.config.FeatureDefinitionLoaderFactory
import com.linkedin.feathr.config.join.FrameFeatureJoinConfig
import com.linkedin.feathr.core.configdataprovider.{ResourceConfigDataProvider, StringConfigDataProvider}
import com.linkedin.feathr.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.config.join.converters.PegasusRecordFrameFeatureJoinConfigConverter
import com.linkedin.feathr.offline.config.{FeathrConfig, FeatureJoinConfig}
import com.linkedin.feathr.offline.exception.DataFrameApiUnsupportedOperationException
import com.linkedin.feathr.offline.graph.NodeUtils.getFeatureTypeConfigsMap
import com.linkedin.feathr.offline.graph.{FCMGraphTraverser, NodeUtils}
import com.linkedin.feathr.offline.job.{FeatureGenSpec, JoinJobContext}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.util.FCMUtils.makeFeatureNameForDuplicates
import com.linkedin.feathr.offline.util.{AnchorUtils, FeaturizedDatasetUtils, SparkFeaturizedDataset}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._
import scala.collection.mutable

sealed trait VisitedState
case object NOT_VISITED extends VisitedState
case object IN_PROGRESS extends VisitedState
case object VISITED extends VisitedState

/**
 * FrameClient2 is the new entry point into Feathr for joining observation data with features. To achieve this, instantiate this class
 * via the FrameClient2 builder which will take your feature config files and prepare a FrameClient2 instance which can join observation
 * data with a join config via the joinFeatures API.
 *
 * The FrameClient takes in a [[ComputeGraph]] object, which can be created from the featureDefConf files using the [[FeatureDefinitionsConverter]]
 * class.
 */
class FeathrClient2(ss: SparkSession, computeGraph: ComputeGraph, dataPathHandlers: List[DataPathHandler], mvelContext: Option[FeathrExpressionExecutionContext]) {
  private val log = LogManager.getLogger(getClass.getName)

  def joinFeatures(frameJoinConfig: FrameFeatureJoinConfig, obsData: SparkFeaturizedDataset, jobContext: JoinJobContext):
  (FeatureDataFrame, Map[String, FeatureTypeConfig], Seq[String]) = {
    val joinConfig = PegasusRecordFrameFeatureJoinConfigConverter.convert(frameJoinConfig)
    joinFeatures(joinConfig, obsData, jobContext)
  }

  private def findInvalidFeatureRefs(features: Seq[String]): List[String] = {
    features.foldLeft(List.empty[String]) { (acc, f) =>
      // featureRefStr could have '-' now.
      // TODO - 8037) unify featureRef/featureName and check for '-'
      val featureRefStrInDF = DataFrameColName.getEncodedFeatureRefStrForColName(f)
      val isValidSyntax = AnchorUtils.featureNamePattern.matcher(featureRefStrInDF).matches()
      if (isValidSyntax) acc
      else f :: acc
    }
  }

  /**
   * Validate feature names in compute graph. Two things are checked here:
   * 1. Feature names conform to regular expression as defined in feathr specs
   * 2. Feature names don't conflict with any field names in the observation data
   * TODO: Add ACL validation for all data sources
   * TODO: Move validation to core library as this is shared among all environments.
   * @param obsFieldNames Field names in observation data feathr
   */
  private def validateFeatureNames(obsFieldNames: Array[String])= {
    val allFeaturesInGraph = computeGraph.getFeatureNames.asScala.keys.toSeq
    val invalidFeatureNames = findInvalidFeatureRefs(allFeaturesInGraph)
    if (invalidFeatureNames.nonEmpty) {
      throw new DataFrameApiUnsupportedOperationException(
        "Feature names must conform to " +
          s"regular expression: ${AnchorUtils.featureNamePattern}, but found feature names: $invalidFeatureNames")
    }
    val conflictFeatureNames: Seq[String] = allFeaturesInGraph.intersect(obsFieldNames)
    if (conflictFeatureNames.nonEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        "Feature names must be different from field names in the observation data. " +
          s"Please rename feature ${conflictFeatureNames} or rename the same field names in the observation data.")
    }
  }

  /**
   * Joins observation data on the feature data. Observation data is loaded as SparkFeaturizedDataset, and the
   * joined data is returned as a SparkFeaturizedDataset.
   * @param joinConfig HOCON based join config
   * @param obsData Observation data in the form of SparkFeaturizedDataset
   * @param jobContext [[JoinJobContext]]
   * @return Feature data join with observation data in the form of SparkFeaturizedDataset
   */
  def joinFeatures(joinConfig: FeatureJoinConfig, obsData: SparkFeaturizedDataset, jobContext: JoinJobContext = JoinJobContext()):
  (FeatureDataFrame, Map[String, FeatureTypeConfig], Seq[String]) = {
    // Set up spark conf parameters needed. This call is crucial otherwise scala UDFs will cause errors when running in spark.
    prepareExecuteEnv()

    val featureNames = joinConfig.joinFeatures.map(_.featureName)
    val duplicateFeatureNames = featureNames.diff(featureNames.distinct).distinct
    val joinFeatures = NodeUtils.getFeatureRequestsFromJoinConfig(joinConfig).asJava

    // Check for invalid feature names
    validateFeatureNames(obsData.data.schema.fieldNames)

    // Create resolved graph using the joinFeatures
    val resolvedGraph = new Resolver(computeGraph).resolveForRequest(joinFeatures)

    // Execute the resolved graph
    val graphTraverser = new FCMGraphTraverser(ss, joinConfig, resolvedGraph, obsData.data, dataPathHandlers, mvelContext)
    val newDf = graphTraverser.traverseGraph()

    val passthroughFeaturesList = resolvedGraph.getNodes.asScala.filter(node => node.getTransformation != null
      && node.getTransformation.getFunction.getOperator().contains("passthrough")).map(node => node.getTransformation.getFeatureName)

    val userProvidedFeatureTypeConfigs = getFeatureTypeConfigsMap(resolvedGraph.getNodes.asScala)
    (newDf, userProvidedFeatureTypeConfigs, passthroughFeaturesList)
  }

  private def prepareExecuteEnv() = {
    ss.conf.set("spark.sql.legacy.allowUntypedScalaUDF", "true")
    ss.conf.set("spark.sql.unionToStructConversion.avro.useNativeSchema", "true")
  }

  def generateFeatures(featureGenSpec: FeatureGenSpec): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    throw new UnsupportedOperationException()
  }
}

object FeathrClient2 {

  /**
   * Create an instance of a builder for constructing a FrameClient2
   * @param sparkSession  the SparkSession required for the FrameClient2 to perform its operations
   * @return  Builder class
   */
  def builder(sparkSession: SparkSession): Builder = {
    new Builder(sparkSession)
  }

  class Builder(ss: SparkSession) {
    private val featureDefinitionLoader = FeatureDefinitionLoaderFactory.getInstance()

    private var featureDef: List[String] = List()
    private var localOverrideDef: List[String] = List()
    private var featureDefPath: List[String] = List()
    private var localOverrideDefPath: List[String] = List()
    private var dataPathHandlers: List[DataPathHandler] = List()
    private var mvelContext: Option[FeathrExpressionExecutionContext] = None;

    def addFeatureDef(featureDef: String): Builder = {
      this.featureDef = featureDef :: this.featureDef
      this
    }

    def addFeatureDef(featureDef: Option[String]): Builder = {
      if (featureDef.isDefined) addFeatureDef(featureDef.get) else this
    }

    def addLocalOverrideDef(localOverrideDef: String): Builder = {
      this.localOverrideDef = localOverrideDef :: this.localOverrideDef
      this
    }

    def addLocalOverrideDef(localOverrideDef: Option[String]): Builder = {
      if (localOverrideDef.isDefined) addFeatureDef(localOverrideDef.get) else this
    }

    def addFeatureDefPath(featureDefPath: String): Builder = {
      this.featureDefPath = featureDefPath :: this.featureDefPath
      this
    }

    def addFeatureDefPath(featureDefPath: Option[String]): Builder = {
      if (featureDefPath.isDefined) addFeatureDefPath(featureDefPath.get) else this
    }

    def addLocalOverrideDefPath(localOverrideDefPath: String): Builder = {
      this.localOverrideDefPath = localOverrideDefPath :: this.localOverrideDefPath
      this
    }

    def addLocalOverrideDefPath(localOverrideDefPath: Option[String]): Builder = {
      if (localOverrideDefPath.isDefined) addLocalOverrideDefPath(localOverrideDefPath.get) else this
    }

    private[offline] def addFeatureDefConfs(featureDefConfs: Option[List[FeathrConfig]]): Builder = {
      // Unlike FrameClient, we can't support this right now, since we only can convert to ComputeGraph from FR definitions
      // and NOT from "FrameConfig" (at least for now – but this seems rarely used so probably not worth it.)
      throw new UnsupportedOperationException()
    }

    private[offline] def addFeatureDefConfs(featureDefConfs: List[FeathrConfig]): Builder = {
      // Unlike FrameClient, we can't support this right now, since we only can convert to ComputeGraph from FR definitions
      // and NOT from "FrameConfig" (at least for now – but this seems rarely used so probably not worth it.)
      throw new UnsupportedOperationException()
    }

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
    def addFeathrExpressionContext(_mvelContext: Option[FeathrExpressionExecutionContext]): Builder = {
      this.mvelContext = _mvelContext
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
     * Build a new instance of the FrameClient2 from the added feathr definition configs and any local overrides.
     *
     * @throws [[IllegalArgumentException]] an error when no feature definitions nor local overrides are configured.
     */
    def build(): FeathrClient2 = {
      import scala.collection.JavaConverters._

      require(
        localOverrideDefPath.nonEmpty || localOverrideDef.nonEmpty || featureDefPath.nonEmpty || featureDef.nonEmpty,
        "Cannot build frameClient without a feature def conf file/string or local override def conf file/string")

      // Append all the configs to this empty list, with the local override def config going last
      val configDocsInOrder = featureDef ::: featureDefPath.flatMap(x => readHdfsFile(Some(x))) :::
        localOverrideDef ::: localOverrideDefPath.flatMap(x => readHdfsFile(Some(x)))

      val partialComputeGraphs = configDocsInOrder.map(new StringConfigDataProvider(_)).map (config =>
        new FeatureDefinitionsConverter().convert(FeatureDefinitionLoaderFactory.getInstance.loadAllFeatureDefinitions(config)))
      val graph = ComputeGraphs.removeRedundancies(ComputeGraphs.merge(partialComputeGraphs.asJava))

      new FeathrClient2(ss, graph, dataPathHandlers, mvelContext)
    }

    private def readHdfsFile(path: Option[String]): Option[String] =
      path.map(p => ss.sparkContext.textFile(p).collect.mkString("\n"))
  }
}
// scalastyle:on
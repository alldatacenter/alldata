package com.linkedin.feathr.offline.evaluator.aggregation

import com.linkedin.feathr.compute.{Aggregation, AnyNode}
import com.linkedin.feathr.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.client.{NOT_VISITED, VISITED, VisitedState}
import com.linkedin.feathr.offline.config.JoinConfigSettings
import com.linkedin.feathr.offline.evaluator.NodeEvaluator
import com.linkedin.feathr.offline.graph.NodeUtils.{getDefaultConverter, getFeatureTypeConfigsMap}
import com.linkedin.feathr.offline.graph.NodeGrouper.groupSWANodes
import com.linkedin.feathr.offline.graph.{DataframeAndColumnMetadata, FCMGraphTraverser}
import com.linkedin.feathr.offline.job.FeatureTransformation
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.{FDS_TENSOR, FeatureColumnFormat, RAW}
import com.linkedin.feathr.swj.{FactData, GroupBySpec, LabelData, LateralViewParams, SlidingWindowFeature, SlidingWindowJoin, WindowSpec}
import com.linkedin.feathr.swj.aggregate.{AggregationSpec, AggregationType, AvgAggregate, AvgPoolingAggregate, CountAggregate, LatestAggregate, MaxAggregate, MaxPoolingAggregate, MinAggregate, MinPoolingAggregate, SumAggregate}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.DataFrame

import scala.collection.JavaConverters._
import java.time.Duration
import scala.collection.mutable

/**
 * This aggregation node evaluator class executes sliding window aggregation as defined by the Aggregation node. The inputs
 * to Aggregation nodes will always be Event Nodes which represent time aware feature data. The main function here is
 * processAggregationNode which will be called by the FCMGraphTraverser to evaluate aggregation nodes.
 */
object AggregationNodeEvaluator extends NodeEvaluator {

  /**
   * Construct the label data required for SWA join.
   * @param aggregation
   * @param featureJoinConfig
   * @param df
   * @param nodeIdToDataframeAndColumnMetadataMap
   * @return
   */
  private def getLabelData(aggregation: Aggregation, joinConfigSettings: Option[JoinConfigSettings], df: DataFrame,
    nodeIdToDataframeAndColumnMetadataMap: mutable.Map[Int, DataframeAndColumnMetadata]): LabelData = {
    val concreteKeys = aggregation.getConcreteKey.getKey.asScala.flatMap(x => nodeIdToDataframeAndColumnMetadataMap(x).keyExpression)
    val obsKeys = concreteKeys.map(k => s"CAST (${k} AS string)")
    val timestampCol = SlidingWindowFeatureUtils.constructTimeStampExpr(joinConfigSettings.get.joinTimeSetting.get.timestampColumn.name,
      joinConfigSettings.get.joinTimeSetting.get.timestampColumn.format)
    val updatedTimestampExpr = if (joinConfigSettings.isDefined && joinConfigSettings.get.joinTimeSetting.isDefined &&
      joinConfigSettings.get.joinTimeSetting.get.useLatestFeatureData) {
      "unix_timestamp()"
    } else timestampCol
    LabelData(df, obsKeys, updatedTimestampExpr)
  }

  private def getLateralViewParams(aggregation: Aggregation): Option[LateralViewParams] = {
    val lateralViewDef = aggregation.getFunction.getParameters.get("lateral_view_expression_0") match {
      case x: String => Some(x)
      case null => None
    }

    val lateralViewAlias = aggregation.getFunction.getParameters.get("lateral_view_table_alias_0") match {
      case x: String => Some(x)
      case null => None
    }

    val lateralViewParams = if (lateralViewDef.isDefined && lateralViewAlias.isDefined) {
      Some(LateralViewParams(lateralViewDef.get, lateralViewAlias.get, None))
    } else None
    lateralViewParams
  }

  private def getAggSpec(aggType: AggregationType.Value, featureDef: String): AggregationSpec = {
    aggType match {
      case AggregationType.SUM => new SumAggregate(featureDef)
      case AggregationType.COUNT =>
        // The count aggregation in spark-algorithms MP is implemented as Sum over partial counts.
        // In feathr's use case, we want to treat the count aggregation as simple count of non-null items.
        val rewrittenDef = s"CASE WHEN ${featureDef} IS NOT NULL THEN 1 ELSE 0 END"
        new CountAggregate(rewrittenDef)
      case AggregationType.AVG => new AvgAggregate(featureDef) // TODO: deal with avg. of pre-aggregated data
      case AggregationType.MAX => new MaxAggregate(featureDef)
      case AggregationType.MIN => new MinAggregate(featureDef)
      case AggregationType.LATEST => new LatestAggregate(featureDef)
      case AggregationType.MAX_POOLING => new MaxPoolingAggregate(featureDef)
      case AggregationType.MIN_POOLING => new MinPoolingAggregate(featureDef)
      case AggregationType.AVG_POOLING => new AvgPoolingAggregate(featureDef)
    }
  }

  private def getSimTimeDelay(featureName: String, joinConfigSettings: Option[JoinConfigSettings],
    featuresToTimeDelayMap: Map[String, String]): Duration = {
    if (featuresToTimeDelayMap.contains(featureName)) {
      if (joinConfigSettings.isEmpty || joinConfigSettings.get.joinTimeSetting.isEmpty ||
        joinConfigSettings.get.joinTimeSetting.get.simulateTimeDelay.isEmpty) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          "overrideTimeDelay cannot be defined without setting a simulateTimeDelay in the " +
            "joinTimeSettings")
      }
      WindowTimeUnit.parseWindowTime(featuresToTimeDelayMap(featureName))
    } else {
      if (joinConfigSettings.isDefined && joinConfigSettings.get.joinTimeSetting.isDefined &&
        joinConfigSettings.get.joinTimeSetting.get.simulateTimeDelay.isDefined) {
        joinConfigSettings.get.joinTimeSetting.get.simulateTimeDelay.get
      } else {
        Duration.ZERO
      }
    }
  }

  // Get a set of [[FactData]] grouped by feature data source, keys and lateral view params.
  private def getFactDataSet(swaNodeIdToNode: Map[Integer, AnyNode], swaMegaNodeMap: Map[Integer, Seq[Integer]],
    aggregation: Aggregation, nodeIdToDataframeAndColumnMetadataMap: mutable.Map[Int, DataframeAndColumnMetadata],
    featureColumnFormatsMap: mutable.HashMap[String, FeatureColumnFormat],
    joinConfigSettings: Option[JoinConfigSettings],
    featuresToTimeDelayMap: Map[String, String],
    nodeIdToFeatureName: Map[Integer, String]): List[FactData] = {
    val allSwaFeatures = swaMegaNodeMap(aggregation.getId)
    val nodes = allSwaFeatures.map(swaNodeIdToNode(_))

    // We will group the nodes by the feature datasource, key expression and the lateral view params as prescribed by the SWA library
    val groupedNodes = nodes.groupBy(x => {
      val lateralViewParams = getLateralViewParams(x.getAggregation)
      (nodeIdToDataframeAndColumnMetadataMap(x.getAggregation.getInput.getId()).dataSource,
        nodeIdToDataframeAndColumnMetadataMap(x.getAggregation.getInput.getId()).keyExpression,
        lateralViewParams)
    })

    // Again sort the acc to size of the groupings to reduce shuffle size.
    groupedNodes.values.toList.sortBy(p => p.size).reverse.map(nodesAtSameLevel => {
      val exampleNode = nodesAtSameLevel.filter(x => nodeIdToDataframeAndColumnMetadataMap.contains(x.getAggregation.getInput.getId())).head.getAggregation
      val featureDf = nodeIdToDataframeAndColumnMetadataMap(exampleNode.getInput.getId()).df
      val featureKeys = nodeIdToDataframeAndColumnMetadataMap(exampleNode.getInput.getId()).keyExpression
      val timestampExpr = nodeIdToDataframeAndColumnMetadataMap(exampleNode.getInput.getId()).timestampColumn.get
      val featureKeysAsString = featureKeys.map(k => s"CAST (${k} AS string)")

      val lateralViewParams = getLateralViewParams(exampleNode)
      val slidingWindowFeatureList = nodesAtSameLevel.map(node => {
        val aggNode = node.getAggregation
        val featureName = nodeIdToFeatureName(aggNode.getId())

        val aggType = AggregationType.withName(aggNode.getFunction.getParameters.get("aggregation_type"))
        val featureDef = aggNode.getFunction.getParameters.get("target_column")
        val rewrittenFeatureDef = if (featureDef.contains(FeatureTransformation.USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME)) {
          // If the feature definition contains USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME then the feature column is already in FDS format.
          // So we strip the udf name and return only the feature name.
          (FeatureTransformation.parseMultiDimTensorExpr(featureDef), FDS_TENSOR)
        } else (featureDef, RAW)
        val aggregationSpec = getAggSpec(aggType, rewrittenFeatureDef._1)

        val window = Duration.parse(aggNode.getFunction.getParameters.get("window_size"))
        val simTimeDelay = getSimTimeDelay(featureName, joinConfigSettings, featuresToTimeDelayMap)

        val filterCondition = aggNode.getFunction.getParameters.get("filter_expression") match {
          case x: String => Some(x)
          case null => None
        }

        val groupBy = aggNode.getFunction.getParameters.get("group_by_expression") match {
          case x: String => Some(x)
          case null => None
        }

        val limit = aggNode.getFunction.getParameters.get("max_number_groups") match {
          case x: String => Some(x.toInt)
          case null => Some(0)
        }

        val groupbySpec = if (groupBy.isDefined) {
          Some(GroupBySpec(groupBy.get, limit.get))
        } else None

        featureColumnFormatsMap(featureName) = rewrittenFeatureDef._2
        SlidingWindowFeature(featureName, aggregationSpec, WindowSpec(window, simTimeDelay), filterCondition, groupbySpec, lateralViewParams)
      })
      FactData(featureDf, featureKeysAsString, timestampExpr, slidingWindowFeatureList.toList)
    }
    )
  }

  /**
   * The nodes are first grouped by the label data, and then further grouped by the feature datasource,
   * feature keys and lateral view params. We invoke the SWA library achieve the SWA join.
   *
   * @param nodes Seq[AnyNode]
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   *  @return DataFrame
   */
  override def batchEvaluate(nodes: Seq[AnyNode], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val groupedAggregationNodeMap = groupSWANodes(nodes)
    val swaNodeIdToNode = graphTraverser.nodes.filter(node => node.isAggregation).map(node => node.getAggregation.getId() -> node).toMap
    val featureColumnFormatsMap = graphTraverser.featureColumnFormatsMap
    val defaultConverter = getDefaultConverter(nodes)
    val featureTypeConfigs = getFeatureTypeConfigsMap(nodes)

    var df: DataFrame = contextDf

    // We sort the group of nodes in ascending order. This is because we want to join the
    // smallest group of features first to reduce shuffle partitions.
    val processedState = Array.fill[VisitedState](graphTraverser.nodes.length)(NOT_VISITED)
    groupedAggregationNodeMap.values.toList.sortBy(p => p.size).reverse.map(listOfnodeIds => {
      // We can take any node from this group as they have been grouped by the same label data, keys, and timestamp column
      val node = swaNodeIdToNode(listOfnodeIds.head)
      if (processedState(node.getAggregation.getId()) != VISITED) {
        val labelData = getLabelData(node.getAggregation, graphTraverser.timeConfigSettings.timeConfigSettings, df,
          graphTraverser.nodeIdToDataframeAndColumnMetadataMap)
        val featureDataSet = getFactDataSet(swaNodeIdToNode, groupedAggregationNodeMap.toMap,
          node.getAggregation, graphTraverser.nodeIdToDataframeAndColumnMetadataMap,
          featureColumnFormatsMap,
          graphTraverser.timeConfigSettings.timeConfigSettings,
          graphTraverser.timeConfigSettings.featuresToTimeDelayMap,
          graphTraverser.nodeIdToFeatureName)
        df = SlidingWindowJoin.join(labelData, featureDataSet)
        val allSwaFeatures = groupedAggregationNodeMap(node.getAggregation.getId)
        // Mark all the nodes evaluated at this stage as visited.
        allSwaFeatures.map(nId => {
          val featureName = graphTraverser.nodeIdToFeatureName(nId)
          // Convert to FDS before applying default values
          df = SlidingWindowFeatureUtils.convertSWADFToFDS(df, Set(featureName), featureColumnFormatsMap.toMap, featureTypeConfigs).df
          // Mark feature as converted to FDS
          featureColumnFormatsMap(featureName) = FeatureColumnFormat.FDS_TENSOR
          df = substituteDefaults(df, Seq(featureName), defaultConverter, featureTypeConfigs, graphTraverser.ss)
          // NOTE: This appending of a dummy column is CRUCIAL to forcing the RDD of the df to have the appropriate schema.
          // Same behavior is present in feathr but feathr unintentionally resolves it by using internal naming for features
          // and only converting to use the real feature name at the end. This step in theory does nothing at all to the data
          // but somehow it affects the schema of the RDD.
          df = df.withColumnRenamed(featureName, featureName + "__dummy__")
          df = df.withColumn(featureName, col(featureName + "__dummy__"))
          df = df.drop(featureName + "__dummy__")
          graphTraverser.nodeIdToDataframeAndColumnMetadataMap(nId) =
            DataframeAndColumnMetadata(df, Seq.empty, Some(featureName)) // Key column for SWA feature is not needed in node context.
          processedState(nId) = VISITED
        })
      }
    })
    df
  }

  override def evaluate(node: AnyNode, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchEvaluate(Seq(node), graphTraverser, contextDf, dataPathHandlers: List[DataPathHandler])
  }
}


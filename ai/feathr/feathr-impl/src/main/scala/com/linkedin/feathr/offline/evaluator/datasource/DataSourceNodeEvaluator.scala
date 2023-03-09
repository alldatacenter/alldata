package com.linkedin.feathr.offline.evaluator.datasource

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.common.{AnchorExtractor, DateTimeResolution}
import com.linkedin.feathr.compute.{AnyNode, DataSourceType, KeyExpressionType}
import com.linkedin.feathr.core.config.producer.common.KeyListExtractor
import com.linkedin.feathr.offline.client.plugins.{AnchorExtractorAdaptor, FeathrUdfPluginContext, SourceKeyExtractorAdaptor}
import com.linkedin.feathr.offline.config.ConfigLoaderUtils
import com.linkedin.feathr.offline.evaluator.NodeEvaluator
import com.linkedin.feathr.offline.graph.{DataframeAndColumnMetadata, FCMGraphTraverser}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType, TimeWindowParams}
import com.linkedin.feathr.offline.source.accessor.{DataPathHandler, DataSourceAccessor}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.source.pathutil.{PathChecker, TimeBasedHdfsPathAnalyzer}
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils.{TIMESTAMP_PARTITION_COLUMN, constructTimeStampExpr}
import com.linkedin.feathr.offline.util.datetime.{DateTimeInterval, OfflineDateTimeUtils}
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.Duration
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable

/**
 * Node evaluator class for data source nodes. We have one private function per data source node type which are responsible
 * for handling the 3 different data source types we support: CONTEXT, EVENT, and TABLE.
 */
object DataSourceNodeEvaluator extends NodeEvaluator{
  val log = LogManager.getLogger(getClass)
  /**
   * Process datasource node of type CONTEXT but with no concrete key (non-passthrough feature context nodes).
   * @param contextDataFrame
   * @param dataSource
   * @return
   */
  private def processContextNode(contextDataFrame: DataFrame, dataSource: com.linkedin.feathr.compute.DataSource): DataframeAndColumnMetadata = {
    // This is the feature column being extracted
    val colName = dataSource.getExternalSourceRef
    DataframeAndColumnMetadata(contextDataFrame, Seq(colName))
  }

  /**
   * Process an event node. Event nodes represent SWA data sources. Here we load in the appropriate time range for the datasource
   * given the time parameters.
   * @param ss Spark session
   * @param dataSourceNode Event node
   * @param timeRange Optional time range to load in for data source.
   * @return DataframeAndColumnMetadata with df loaded
   */
  private def processEventNode(ss: SparkSession, dataSourceNode: com.linkedin.feathr.compute.DataSource,
    timeRange: Option[DateTimeInterval], dataPathHandlers: List[DataPathHandler]): DataframeAndColumnMetadata = {
    assert(dataSourceNode.hasConcreteKey)
    assert(dataSourceNode.getConcreteKey.getKey.asScala.nonEmpty)
    val path = dataSourceNode.getExternalSourceRef // We are using ExternalSourceRef for way too many things at this point.

    // Augment time information also here. Table node should not have time info?
    val source = com.linkedin.feathr.offline.source.DataSource(path, SourceFormatType.TIME_SERIES_PATH, if (dataSourceNode.hasTimestampColumnInfo) {
      Some(TimeWindowParams(dataSourceNode.getTimestampColumnInfo().getExpression(),
        dataSourceNode.getTimestampColumnInfo().getFormat))
    } else None, if (dataSourceNode.hasFilePartitionFormat) {
      Some(dataSourceNode.getFilePartitionFormat)
    } else None)

    val timeWindowParam = if (dataSourceNode.hasTimestampColumnInfo) {
      TimeWindowParams(dataSourceNode.getTimestampColumnInfo().getExpression, dataSourceNode.getTimestampColumnInfo().getFormat)
    } else {
      TimeWindowParams(TIMESTAMP_PARTITION_COLUMN, "epoch")
    }
    val timeStampExpr = constructTimeStampExpr(timeWindowParam.timestampColumn, timeWindowParam.timestampColumnFormat)
    val needTimestampColumn = if (dataSourceNode.hasTimestampColumnInfo) false else true
    val dataSourceAccessor = DataSourceAccessor(ss, source, timeRange, None, failOnMissingPartition = false, needTimestampColumn, dataPathHandlers = dataPathHandlers)
    val sourceDF = dataSourceAccessor.get()
    val (df, keyExtractor, timestampExpr) = if (dataSourceNode.getKeyExpressionType == KeyExpressionType.UDF) {
      val className = Class.forName(dataSourceNode.getKeyExpression())
      val keyExtractorClass = className.newInstance match {
        case keyExtractorClass: SourceKeyExtractor =>
          keyExtractorClass
        case _ =>
          FeathrUdfPluginContext.getRegisteredUdfAdaptor(className) match {
            case Some(adaptor: SourceKeyExtractorAdaptor) =>
              adaptor.adaptUdf(className.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef])
            case _ =>
              throw new UnsupportedOperationException("Unknown extractor type: " + className)
          }
      }
      (keyExtractorClass.appendKeyColumns(sourceDF), keyExtractorClass.getKeyColumnNames(), timeStampExpr)
    } else {
      val featureKeys = ConfigLoaderUtils.javaListToSeqWithDeepCopy(KeyListExtractor.getInstance().
        extractFromHocon(dataSourceNode.getKeyExpression)).map(k => s"CAST (${k} AS string)")
      (sourceDF, featureKeys, timeStampExpr)
    }

    // Only for datasource node, we will append the timestampExpr with the key field. TODO - find a better way of doing this.
    DataframeAndColumnMetadata(df, keyExtractor, None, Some(source), Some(timestampExpr))
  }

  /**
   * Process table nodes. Table nodes represent HDFS sources with a fixed path and no time partition data. Here we load
   * in the data specified in the data source node and apply key extractor logic here if there is one.
   * @param ss Spark session
   * @param dataSourceNode Table node
   * @return DataframeAndColumnMetadata with source loaded into df
   */
  private def processTableNode(ss: SparkSession, dataSourceNode: com.linkedin.feathr.compute.DataSource, dataPathHandlers: List[DataPathHandler]): DataframeAndColumnMetadata = {
    assert(dataSourceNode.hasConcreteKey)
    assert(dataSourceNode.getConcreteKey.getKey.asScala.nonEmpty)
    val path = dataSourceNode.getExternalSourceRef // We are using ExternalSourceRef for way too many things at this point.

    // Augment time information also here. Table node should not have time info?
    val dataSource = com.linkedin.feathr.offline.source.DataSource(path, SourceFormatType.FIXED_PATH)
    val dataSourceAccessor = DataSourceAccessor(ss, dataSource, None, None, failOnMissingPartition = false, dataPathHandlers = dataPathHandlers)
    val sourceDF = dataSourceAccessor.get()
    val (df, keyExtractor) = if (dataSourceNode.getKeyExpressionType == KeyExpressionType.UDF) {
      val className = Class.forName(dataSourceNode.getKeyExpression())
       className.newInstance match {
        case keyExtractorClass: SourceKeyExtractor =>
          val updatedDf = keyExtractorClass.appendKeyColumns(sourceDF)
          (updatedDf, keyExtractorClass.getKeyColumnNames())
        case _: AnchorExtractor[_] =>
          // key will be evaluated at the time of anchor evaluation.
          (sourceDF, Seq())
        case _ =>
          val x = FeathrUdfPluginContext.getRegisteredUdfAdaptor(className)
          log.info("x is " + x + " and x type is " + x.getClass)
          FeathrUdfPluginContext.getRegisteredUdfAdaptor(className) match {
            case Some(adaptor: SourceKeyExtractorAdaptor) =>
              val keyExtractor = adaptor.adaptUdf(className.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef])
              val updatedDf = keyExtractor.appendKeyColumns(sourceDF)
              (updatedDf, keyExtractor.getKeyColumnNames())
            case Some(adaptor: AnchorExtractorAdaptor) =>
              (sourceDF, Seq())
            case _ =>
              throw new UnsupportedOperationException("Unknown extractor type: " + className + " FeathrUdfPluginContext" +
                ".getRegisteredUdfAdaptor(className) is " + FeathrUdfPluginContext.getRegisteredUdfAdaptor(className) + "and type is " + x.get.isInstanceOf[AnchorExtractorAdaptor])
          }
      }
    } else {
      val featureKeys = ConfigLoaderUtils.javaListToSeqWithDeepCopy(KeyListExtractor.getInstance().extractFromHocon(dataSourceNode.getKeyExpression()))
      (sourceDF, featureKeys)
    }

    DataframeAndColumnMetadata(df, keyExtractor, dataSource = Some(dataSource))
  }

  private def getOptimizedDurationMap(nodes: Seq[AnyNode]): Map[String, Duration] = {
    val allSWANodes = nodes.filter(node => node.getAggregation != null)
    // Create a map from SWA's event node to window duration in order to compute event node.
    val swaDurationMap = allSWANodes.map(node => node.getAggregation.getInput.getId() -> Duration.parse(node.getAggregation.getFunction.getParameters
      .get("window_size"))).toMap
    val allEventSourceNodes = nodes.filter(node => node.isDataSource && node.getDataSource.getSourceType() == DataSourceType.EVENT)
    val pathToDurationMap = mutable.HashMap.empty[String, Duration]
    allEventSourceNodes.map(node => {
      val sourcePath = node.getDataSource.getExternalSourceRef
      if (!pathToDurationMap.contains(sourcePath)) {
        pathToDurationMap.put(sourcePath, swaDurationMap(node.getDataSource.getId))
      } else {
        val duration = pathToDurationMap(sourcePath)
        if (duration.toHours < swaDurationMap(node.getDataSource.getId()).toHours) pathToDurationMap.put(sourcePath, swaDurationMap(node.getDataSource.getId))
      }
    })
    pathToDurationMap.toMap
  }

  /**
   * Evaluate a single data source node according to the datasource type and return the context df.
   * In this case only the graphTraverser's nodeIdToDataframeAndColumnMetadataMap is updated for the datasource node evaluation and the context df
   * is not modified. Note that we don't process passthrough features at this point.
   *
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf      Context df
   * @return DataFrame
   */
  override def evaluate(node: AnyNode, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val dataSource = node.getDataSource
    val nodeId = node.getDataSource.getId
    dataSource.getSourceType match {
      case DataSourceType.CONTEXT =>
        if (dataSource.hasConcreteKey) {
          val key = dataSource.getKeyExpression
          val df = contextDf
          graphTraverser.nodeIdToDataframeAndColumnMetadataMap(nodeId) = DataframeAndColumnMetadata(df, Seq(key))
        } else {
          graphTraverser.nodeIdToDataframeAndColumnMetadataMap(nodeId) = processContextNode(contextDf, dataSource)
        }
      case DataSourceType.UPDATE =>
        graphTraverser.nodeIdToDataframeAndColumnMetadataMap(nodeId) = processTableNode(graphTraverser.ss, dataSource, dataPathHandlers: List[DataPathHandler])
      case DataSourceType.EVENT =>
        val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
        val pathChecker = PathChecker(graphTraverser.ss, dataLoaderHandlers = dataLoaderHandlers)
        val pathAnalyzer = new TimeBasedHdfsPathAnalyzer(pathChecker, dataLoaderHandlers = dataLoaderHandlers)
        val pathInfo = pathAnalyzer.analyze(node.getDataSource.getExternalSourceRef)
        val adjustedObsTimeRange = if (pathInfo.dateTimeResolution == DateTimeResolution.DAILY)
        {
          graphTraverser.timeConfigSettings.obsTimeRange.adjustWithDateTimeResolution(DateTimeResolution.DAILY)
        } else graphTraverser.timeConfigSettings.obsTimeRange

        val eventPathToDurationMap = getOptimizedDurationMap(graphTraverser.nodes)
        val duration = eventPathToDurationMap(node.getDataSource.getExternalSourceRef())
        if (graphTraverser.timeConfigSettings.timeConfigSettings.isEmpty || graphTraverser.timeConfigSettings.timeConfigSettings.get.joinTimeSetting.isEmpty) {
          throw new FeathrConfigException(
            ErrorLabel.FEATHR_USER_ERROR,
            "joinTimeSettings section is not defined in join config," +
              " cannot perform window aggregation operation")
        }

        val adjustedTimeRange = OfflineDateTimeUtils.getFactDataTimeRange(adjustedObsTimeRange, duration,
          Array(graphTraverser.timeConfigSettings.timeConfigSettings.get.joinTimeSetting.get.simulateTimeDelay.getOrElse(Duration.ZERO)))
        graphTraverser.nodeIdToDataframeAndColumnMetadataMap(node.getDataSource.getId) =
          processEventNode(graphTraverser.ss, node.getDataSource, Some(adjustedTimeRange), dataPathHandlers: List[DataPathHandler])
    }
    contextDf
  }

  override def batchEvaluate(nodes: Seq[AnyNode], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foreach(evaluate(_, graphTraverser, contextDf, dataPathHandlers))
    contextDf
  }
}

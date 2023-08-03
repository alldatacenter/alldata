package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.common.{AnchorExtractor, AnchorExtractorBase, CanConvertToAvroRDD, FeatureTypeConfig}
import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.anchored.anchorExtractor.SQLConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.keyExtractor.{SQLSourceKeyExtractor, SpecificRecordSourceKeyExtractor}
import com.linkedin.feathr.offline.client.plugins.{AnchorExtractorAdaptor, FeathrUdfPluginContext, SimpleAnchorExtractorSparkAdaptor}
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.{createFeatureDF, dropAndRenameCols, joinResultToContextDfAndApplyDefaults}
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.graph.NodeUtils.getFeatureTypeConfigsMapForTransformationNodes
import com.linkedin.feathr.offline.job.FeatureTransformation.{applyRowBasedTransformOnRdd, getFeatureKeyColumnNames}
import com.linkedin.feathr.offline.source.accessor.{DataPathHandler, DataSourceAccessor, NonTimeBasedDataSourceAccessor}
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.util.{FeaturizedDatasetUtils, SourceUtils}
import com.linkedin.feathr.sparkcommon.{FDSExtractor, GenericAnchorExtractorSpark, SimpleAnchorExtractorSpark}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Column, DataFrame}

object AnchorUDFOperator extends TransformationOperator {
  private val FDSExtractorUserFacingName = "com.linkedin.feathr.sparkcommon.FDSExtractor"
  /**
   * Compute the anchor UDF transformation and return the result df and output key columns.
   * @param nodes
   * @param graphTraverser
   * @return (DataFrame, Seq[String])
   */
  def computeUDFResult(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame,
    appendKeyColumns: Boolean, dataPathHandlers: List[DataPathHandler]): (DataFrame, Seq[String]) = {
    // All nodes in UDF anchor group will have the same key expression and input node so we can just use the head.
    val inputNodeId = nodes.head.getInputs.get(0).getId // Anchor operators should only have a single input
    val keySeq = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).keyExpression
    val inputDf = if (appendKeyColumns) graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).df else contextDf
    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(nodes)

    // Grab extractor class and create appropriate extractor. All extractors in batch should have the same class.
    val className = nodes.head.getFunction.getParameters.get("class")
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    val extractor = if (className.equals(FDSExtractorUserFacingName)) { // Support for FDSExtractor, which is a canned extractor.
      new FDSExtractor(featureNamesInBatch.toSet)
    } else {
      Class.forName(className).newInstance
    }

    val newExtractor = FeathrUdfPluginContext.getRegisteredUdfAdaptor(Class.forName(className)) match {
      case Some(adaptor: SimpleAnchorExtractorSparkAdaptor) =>
        adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
      case Some(adaptor: AnchorExtractorAdaptor) =>
        adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
      case None => extractor
    }

    val (withFeaturesDf, outputJoinKeyColumnNames) = newExtractor match {
      case sparkExtractor: SimpleAnchorExtractorSpark =>
        // Note that for Spark UDFs we only support SQL keys.
        print("in simpleanchorextractorspark = " + newExtractor)
        val sqlKeyExtractor = new SQLSourceKeyExtractor(keySeq)
        val withKeyColumnDF = if (appendKeyColumns) sqlKeyExtractor.appendKeyColumns(inputDf) else inputDf
        val outputJoinKeyColumnNames = getFeatureKeyColumnNames(sqlKeyExtractor, withKeyColumnDF)

        val tensorizedFeatureColumns = sparkExtractor.getFeatures(inputDf, Map())
        val transformedColsAndFormats: Map[(String, Column), FeatureColumnFormat] = extractor match {
          case extractor2: SQLConfigurableAnchorExtractor =>
            print("in SQLConfigurableAnchorExtractor = " + newExtractor)
            // If instance of SQLConfigurableAnchorExtractor, get Tensor features
            // Get DataFrame schema for tensor based on FML or inferred tensor type.
            val featureSchemas = featureNamesInBatch.map(featureName => {
              // Currently assumes that tensor type is undefined
              val featureTypeConfig = featureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
              val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForFeatureRef(featureName, None, featureTypeConfig)
              val schema = FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(tensorType)
              featureName -> schema
            })
              .toMap
            extractor2.getTensorFeatures(inputDf, featureSchemas)
          case _ => newExtractor match {
            case extractor1: FDSExtractor =>
              // While using the FDS extractor, the feature columns are already in FDS format.
              featureNamesInBatch.foreach(featureName => graphTraverser.featureColumnFormatsMap(featureName) = FeatureColumnFormat.FDS_TENSOR)
              extractor1.transformAsColumns(inputDf).map(c => (c, FeatureColumnFormat.FDS_TENSOR)).toMap
            case _ => if (tensorizedFeatureColumns.isEmpty) {
              // If transform.getFeatures() returns empty Seq, then transform using transformAsColumns
              sparkExtractor.transformAsColumns(inputDf).map(c => (c, FeatureColumnFormat.RAW)).toMap
            } else {
              // transform.getFeature() expects user to return FDS tensor
              featureNamesInBatch.foreach(featureName => graphTraverser.featureColumnFormatsMap(featureName) = FeatureColumnFormat.FDS_TENSOR)
              tensorizedFeatureColumns.map(c => (c, FeatureColumnFormat.FDS_TENSOR)).toMap
            }
          }
        }
        val transformedDF = createFeatureDF(withKeyColumnDF, transformedColsAndFormats.keys.toSeq)
        (transformedDF, outputJoinKeyColumnNames)
      case sparkExtractor: GenericAnchorExtractorSpark =>
        // Note that for Spark UDFs we only support SQL keys.
        val sqlKeyExtractor = new SQLSourceKeyExtractor(keySeq)
        val withKeyColumnDF = if (appendKeyColumns) sqlKeyExtractor.appendKeyColumns(inputDf) else inputDf
        val outputJoinKeyColumnNames = getFeatureKeyColumnNames(sqlKeyExtractor, withKeyColumnDF)

        val transformedDF = sparkExtractor.transform(inputDf)
        (transformedDF, outputJoinKeyColumnNames)
      case _ => newExtractor match {
        case rowBasedExtractor: AnchorExtractorBase[Any] =>
          // Note that for row based extractors we will be using MVEL source key extractor and row based extractor requires us
          // to create a rdd so we can't just use the input df.
          val userProvidedFeatureTypes = featureTypeConfigs map { case (key, value) => (key, value.getFeatureType) }
          val dataSource = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(nodes.head.getInputs.get(0).getId).dataSource.get
          val expectDatumType = SourceUtils.getExpectDatumType(Seq(rowBasedExtractor))
          val dataSourceAccessor = DataSourceAccessor(graphTraverser.ss, dataSource, None, Some(expectDatumType), failOnMissingPartition = false, dataPathHandlers = dataPathHandlers)
          val rdd = newExtractor.asInstanceOf[CanConvertToAvroRDD].convertToAvroRdd(dataSourceAccessor.asInstanceOf[NonTimeBasedDataSourceAccessor].get())
          val sourceKeyExtractors = nodes.map(node => {
            val className = node.getFunction.getParameters.get("class")
            val createdExtractor = FeathrUdfPluginContext.getRegisteredUdfAdaptor(Class.forName(className)) match {
              case Some(adaptor: SimpleAnchorExtractorSparkAdaptor) =>
                adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
              case Some(adaptor: AnchorExtractorAdaptor) =>
                adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
              case None => extractor
            }
            new SpecificRecordSourceKeyExtractor(createdExtractor.asInstanceOf[AnchorExtractor[Any]], Seq.empty[String])
          })

          val anchorExtractors = nodes.map(node => {
            val className = node.getFunction.getParameters.get("class")
            val createdExtractor = FeathrUdfPluginContext.getRegisteredUdfAdaptor(Class.forName(className)) match {
              case Some(adaptor: SimpleAnchorExtractorSparkAdaptor) =>
                adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
              case Some(adaptor: AnchorExtractorAdaptor) =>
                adaptor.adaptUdf(extractor.asInstanceOf[AnyRef])
              case None => extractor
            }
            createdExtractor.asInstanceOf[AnchorExtractorBase[Any]]
          })

          val (transformedDf, keyNames) = applyRowBasedTransformOnRdd(userProvidedFeatureTypes, featureNamesInBatch,
            rdd,
            sourceKeyExtractors,
            anchorExtractors, featureTypeConfigs)
          (transformedDf, keyNames)
        case _ =>
          throw new UnsupportedOperationException("Unknow extractor type : " + extractor + " and it's class is " + extractor.getClass)
      }
    }
    (withFeaturesDf, outputJoinKeyColumnNames)
  }

  /**
   * Operator for batch anchor UDF transformations. Given context df and a grouped set of UDF transformation nodes,
   * perform the UDF transformations and return the context df with all the UDF features joined.
   * @param nodes Seq of nodes with UDF anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (transformationResult, outputJoinKeyColumnNames) = computeUDFResult(nodes, graphTraverser, contextDf, appendKeyColumns = true, dataPathHandlers)
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    val (prunedResult, keyColumns) = dropAndRenameCols(transformationResult, outputJoinKeyColumnNames, featureNamesInBatch)
    joinResultToContextDfAndApplyDefaults(nodes, graphTraverser, prunedResult, keyColumns, contextDf)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }

}

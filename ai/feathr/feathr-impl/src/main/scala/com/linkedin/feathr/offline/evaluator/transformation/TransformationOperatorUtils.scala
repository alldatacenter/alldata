package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.graph.NodeUtils.{getDefaultConverterForTransformationNodes, getFeatureTypeConfigsMapForTransformationNodes}
import com.linkedin.feathr.offline.graph.{DataframeAndColumnMetadata, FCMGraphTraverser}
import com.linkedin.feathr.offline.join.algorithms.{EqualityJoinConditionBuilder, JoinType, SparkJoinWithJoinCondition}
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._

import scala.collection.JavaConverters.asScalaBufferConverter

/**
 * Util functions which are shared among different operators.
 */
object TransformationOperatorUtils {
  /**
   * Keeps only feature column + key columns and drops all other columns. Key columns are renamed with __frame__key__column__ prefix.
   * @param df
   * @param keyCols
   * @param featureName
   * @return
   */
  def dropAndRenameCols(df: DataFrame, keyCols: Seq[String], featureName: Seq[String]): (DataFrame, Seq[String]) = {
    val toDropCols = df.columns diff (keyCols ++ featureName)
    val modifiedDf = df.drop(toDropCols: _*)
    val renamedKeyColumns = keyCols.map(c => "__frame__key__column__" + c)
    val oldKeyColToNewKeyCOl = (keyCols zip renamedKeyColumns).toMap
    val withRenamedColsDF = modifiedDf.select(
      modifiedDf.columns.map(c => modifiedDf(c).alias(oldKeyColToNewKeyCOl.getOrElse(c, c))): _*
    )
    (withRenamedColsDF, renamedKeyColumns)
  }

  /**
   * Create data frame by combining inputDf and Seq of feature name -> spark Column. Some extractors in Frame outputs the result
   * in the form of Seq[(String, Column)] so we need this utility to append the result to the input df.
   * @param inputDf
   * @param featureColumnDefs
   * @return
   */
  def createFeatureDF(inputDf: DataFrame, featureColumnDefs: Seq[(String, Column)]): DataFrame = {
    // first add a prefix to the feature column name in the schema
    val featureColumnNamePrefix = "_frame_sql_feature_prefix_"
    print(inputDf.columns.mkString("Array(", ", ", ")"))
    val transformedDF = featureColumnDefs.foldLeft(inputDf)((baseDF, columnWithName) => {
      print("COLUMN NAME = " + columnWithName)
      val columnName = featureColumnNamePrefix + columnWithName._1
      baseDF.withColumn(columnName, expr(columnWithName._2.toString()))
    })
    val featureNames = featureColumnDefs.map(_._1)
    // drop the context column that have the same name as feature names
    val withoutDupContextFieldDF = transformedDF.drop(featureNames: _*)
    // remove the prefix we just added, so that we have a dataframe with feature names as their column names
    featureNames
      .zip(featureNames)
      .foldLeft(withoutDupContextFieldDF)((baseDF, namePair) => {
        baseDF.withColumnRenamed(featureColumnNamePrefix + namePair._1, namePair._2)
      })
  }

  /**
   * Joins result df to context df using concrete keys and applies default values. Returns new context df.
   * @param nodes
   * @param graphTraverser
   * @param resultDf
   * @param resultKeyColumns
   * @param contextDf
   * @return
   */
  def joinResultToContextDfAndApplyDefaults(nodes: Seq[Transformation],
    graphTraverser: FCMGraphTraverser,
    resultDf: DataFrame,
    resultKeyColumns: Seq[String],
    contextDf: DataFrame): DataFrame = {
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    // Update node context map for all nodes in this batch
    nodes.foreach(node => {
      graphTraverser.nodeIdToDataframeAndColumnMetadataMap(node.getId) =
        DataframeAndColumnMetadata(resultDf, resultKeyColumns, Some(graphTraverser.nodeIdToFeatureName(node.getId)))
    })

    // Get concrete keys from nodeIdToDataframeAndColumnMetadataMap to join transformation result to contextDf
    val concreteKeys = nodes.head.getConcreteKey.getKey.asScala.flatMap(x => {
      if (graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).featureColumn.isDefined) {
        Seq(graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).featureColumn.get)
      } else {
        graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).keyExpression
      }
    })

    // Join result to context df and drop transformation node key columns.
    // NOTE: If the batch of nodes only contains look up expansion features, we can not join to the context df at this point.
    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(nodes)
    val defaultConverter = getDefaultConverterForTransformationNodes(nodes)
    val allLookupExpansionNodes = graphTraverser.nodes.filter(node => node.getLookup != null).map(node => node.getLookup.getLookupNode)
    val isLookupExpansionGroup = nodes.forall(node => allLookupExpansionNodes.contains(node.getId))
    if (isLookupExpansionGroup) {
      val withDefaultsDf = substituteDefaults(resultDf, featureNamesInBatch,
        defaultConverter, featureTypeConfigs, graphTraverser.ss)
      nodes.foreach(node => {
        graphTraverser.nodeIdToDataframeAndColumnMetadataMap(node.getId) =
          DataframeAndColumnMetadata(withDefaultsDf, resultKeyColumns, Some(graphTraverser.nodeIdToFeatureName(node.getId)))
      })
      contextDf
    } else {
      // If the feature name is already present in the contextDf, it must have been needed for a derived feature. Drop the
      // column and join the new one.
      val newContextDf = featureNamesInBatch.foldLeft(contextDf)((currContextDf, featureName) => {
        if (currContextDf.columns.contains(featureName)) currContextDf.drop(featureName) else currContextDf
      })
      val result = SparkJoinWithJoinCondition(EqualityJoinConditionBuilder).join(concreteKeys, newContextDf, resultKeyColumns, resultDf, JoinType.left_outer)
        .drop(resultKeyColumns: _*)
      substituteDefaults(result, featureNamesInBatch, defaultConverter, featureTypeConfigs, graphTraverser.ss)
    }
  }

  /**
   * Given a seq of transformation nodes, updates graphTraverser's nodeIdToDataframeAndColumnMetadataMap with the result
   * and returns the new context df. This function is used by passthrough and derived operators as they don't perform any joins.
   * @param nodes
   * @param graphTraverser
   * @param resultDf
   * @param resultKeyColumns
   * @return
   */
  def updateDataframeMapAndApplyDefaults(nodes: Seq[Transformation],
    graphTraverser: FCMGraphTraverser,
    resultDf: DataFrame,
    resultKeyColumns: Seq[String]): DataFrame = {
    // Update node context map for all processed nodes this stage.
    nodes.foreach(node => {
      graphTraverser.nodeIdToDataframeAndColumnMetadataMap(node.getId) =
        DataframeAndColumnMetadata(resultDf, resultKeyColumns, Some(graphTraverser.nodeIdToFeatureName(node.getId)))
    })
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(nodes)
    val defaultConverter = getDefaultConverterForTransformationNodes(nodes)
    substituteDefaults(resultDf, featureNamesInBatch, defaultConverter, featureTypeConfigs, graphTraverser.ss)
  }
}

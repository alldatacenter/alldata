package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.{Header, TaggedFeatureName}
import com.linkedin.feathr.offline._
import com.linkedin.feathr.offline.client._
import com.linkedin.feathr.offline.job.FeatureTransformation
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.{StructField, StructType}

import scala.collection.convert.wrapAll._

/**
 * Represents a post-processing stage after feature generation. This stage prunes / decorates / standardizes
 * the feature data and constructs metadata necessary for downstream processing.
 */
private[offline] class PostGenPruner() {

  /**
   * Applies necessary decoration / transformation before handing it off to output processors.
   * Prunes unnecessary columns and constructs the header data (metadata) for the requested features.
   * @param featureData         Generated feature data with join keys.
   * @param requestedFeatures   features requested by user.
   * @param logicalPlan         feature information after analyzing the requested features.
   * @param featureGroups       features identified by the group they belong to.
   */
  def prune(
      featureData: FeatureDataWithJoinKeys,
      requestedFeatures: Seq[FeatureName],
      logicalPlan: MultiStageJoinPlan,
      featureGroups: FeatureGroups): Map[TaggedFeatureName, (DataFrame, Header)] = {
    // Build a feature name -> keyTag look up table.
    // Note: In feature generation, same feature cannot be associated with multiple key tags.
    val featureNameToKeyTagMap: Map[FeatureName, KeyTagIdTuple] = logicalPlan.allRequiredFeatures.map {
      case ErasedEntityTaggedFeature(keyTags: Seq[Int], featureName: String) => featureName -> keyTags
    }.toMap

    // Filter to keep only requested features. The key columns and the requested features are the only columns keep.
    val filteredFeatureData = featureData.filter(f => requestedFeatures.contains(f._1))
    // Group by DataFrame and process all features on the DataFrame.
    filteredFeatureData.groupBy(_._2._1).flatMap {
      case (_, featureDataOnDf) =>
        val df = featureDataOnDf.head._2._1
        val joinKeys = featureDataOnDf.head._2._2
        val featuresToPrune = featureDataOnDf.keys.toSeq
        // All features on the DataFrame would share the same key tags. Hence pick the keyTag for the first feature.
        val keyTags = featureNameToKeyTagMap(featuresToPrune.head)
        val prunedFeatureData =
          pruneColumns(df, joinKeys, keyTags.map(logicalPlan.keyTagIntsToStrings).toList, featuresToPrune, featureGroups)
        // Rename key names to standard names such as key0, key1, key2, etc.
        val keyColumnNames = FeatureTransformation.getStandardizedKeyNames(keyTags.size)
        prunedFeatureData.map {
          case (featureName, dfWithHeader) =>
            val taggedFeatureName = new TaggedFeatureName(keyColumnNames, featureName)
            taggedFeatureName -> dfWithHeader
        }
    }
  }

  /**
   * Prune unwanted features, unwanted columns (if any), intermediate columns created during transformation.
   * @param featureDataFrame  generated feature data with feature types.
   * @param joinKeys          join keys for all the features that lie on the DataFrame.
   * @param keyTagsInfo       string keyTags (or just keys) for the generated features.
   * @param featuresToKeep   features to keep or requested features for this iteration.
   * @return
   */
  private def pruneColumns(
      featureDataFrame: FeatureDataFrame,
      joinKeys: JoinKeys,
      keyTagsInfo: List[String],
      featuresToKeep: Seq[FeatureName],
      featureGroups: FeatureGroups): Map[String, (DataFrame, Header)] = {
    val df = featureDataFrame.df
    val featureTypeMap = featureDataFrame.inferredFeatureType
    val columnsToProcess = df.columns
    val cleanedDF = FeatureTransformation.pruneAndRenameColumnWithTags(df, joinKeys, featuresToKeep, columnsToProcess, keyTagsInfo)
    if (joinKeys.size != keyTagsInfo.size) {
      throw new FeathrException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Number of transformer returned join key names of features ${featuresToKeep.mkString(",")} " +
          s"does not match the number of key tags declared in the features, returned: ${joinKeys.mkString(",")}, size: ${joinKeys.size}, "
          + s"expected: ${keyTagsInfo.mkString(",")}, size: ${keyTagsInfo.size}")
    }
    val keyColumnNames = FeatureTransformation.getStandardizedKeyNames(keyTagsInfo.size)
    val resultFDS: DataFrame = standardizeColumns(joinKeys, keyColumnNames, cleanedDF)

    val taggedFeatureToColumnNameMap = DataFrameColName.getTaggedFeatureToNewColumnName(resultFDS)

    val (dfWithoutFDSMetadata, header) =
      DataFrameColName.adjustFeatureColNamesAndGetHeader(
        resultFDS,
        taggedFeatureToColumnNameMap,
        featureGroups.allAnchoredFeatures,
        featureGroups.allDerivedFeatures,
        featureTypeMap)
    // Adjust key tag name to standard name in the header
    val headerInfo = header.featureInfoMap.map {
      case (taggedFeatureName, v) =>
        val newTaggedFeatureName = new TaggedFeatureName(keyColumnNames, taggedFeatureName.getFeatureName)
        (newTaggedFeatureName, v)
    }
    val newHeader = new Header(headerInfo)
    featuresToKeep.map(f => (f, (dfWithoutFDSMetadata, newHeader))).toMap
  }

  /**
   * Change the columns names and types accordingly to Feathr convention
   * Rename key names to standard names such as key0, key1, key2, etc.
   * Set key column to non-nullable and feature columns to nullable.
   * @param joinKeys
   * @param keyColumnNames
   * @param cleanedDF
   * @return
   */
  def standardizeColumns(joinKeys: JoinKeys, keyColumnNames: Seq[String], cleanedDF: DataFrame) = {
    val keyColumnRenamedDF =
      joinKeys
        .zip(keyColumnNames)
        .foldLeft(cleanedDF)((inputDF, renamePair) => inputDF.withColumnRenamed(renamePair._1, renamePair._2))
    val schemaWithNonNullKeys = StructType(keyColumnRenamedDF.schema.map {
      case StructField(name, dataType, isNullable, metadata) =>
        if (keyColumnNames.contains(name)) StructField(name, dataType, nullable = false, metadata)
        else StructField(name, dataType, nullable = true, metadata)
    })
    val resultFDS = keyColumnRenamedDF.sqlContext.createDataFrame(keyColumnRenamedDF.rdd, schemaWithNonNullKeys)
    resultFDS
  }
}

/**
 * Companion object.
 */
private[offline] object PostGenPruner {
  def apply(): PostGenPruner = new PostGenPruner()
}

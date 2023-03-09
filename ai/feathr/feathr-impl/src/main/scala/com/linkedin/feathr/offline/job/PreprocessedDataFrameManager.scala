package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import org.apache.spark.sql.DataFrame

/**
 * Object to keep the preprocessed DataFrames by Pyspark.
 */
object PreprocessedDataFrameManager {
  var preprocessedDfMap: Map[String, DataFrame] = Map()


  /***
   * Get the uniqueness of the anchor if it has preprocessing UDFs.
   * preprocessedDfMap is in the form of feature list of an anchor separated by comma to their preprocessed DataFrame.
   * We use feature list to denote if the anchor has corresponding preprocessing UDF.
   * For example, an anchor have f1, f2, f3, then corresponding preprocessedDfMap is Map("f1,f2,f3"-> df1)
   * (Anchor name is not chosen since it may be duplicated and not unique and stable.)
   * If this anchor has preprocessing UDF, then we need to make its FeatureGroupingCriteria unique so it won't be
   * merged by different anchor of same source. Otherwise our preprocessing UDF may impact anchors that dont'
   * need preprocessing.
   */
  def getPreprocessingUniquenessForAnchor(featureAnchorWithSource: FeatureAnchorWithSource): String = {
    val featuresInAnchor = featureAnchorWithSource.featureAnchor.features.toList
    val sortedMkString = featuresInAnchor.sorted.mkString(",")
    val featureNames = if (preprocessedDfMap.contains(sortedMkString)) sortedMkString else ""
    featureNames
  }

  /***
   * Get the preprocessed DataFrame for the anchor group.
   */
  def getPreprocessedDataframe(featureAnchors: Seq[FeatureAnchorWithSource]): Option[DataFrame] = {
    // Obtain the preprocessed DataFrame by Pyspark for this anchor to replace the origin source DataFrame.
    // At this point, we have make the anchors that have preprocessing UDF unique so it will be only one anchor if
    // the anchor has preprocessing UDF.
    // Then we use the feature names separated by comma as the key to get the preprocessed DataFrame from Pyspark.
    // If there are multiple features in an anchor, there will be multiple anchors in ths anchorsWithSameSource
    // so we need to merge the duplicates via set, for example [f1, f1, f2, f2] => [f1, f2]
    val allAnchorsFeatures = featureAnchors.flatMap(_.featureAnchor.features).toSet.toList.sorted
    val featureMkString = allAnchorsFeatures.sorted.mkString(",")
    val preprocessedDf = PreprocessedDataFrameManager.preprocessedDfMap.get(featureMkString)
    preprocessedDf
  }
}

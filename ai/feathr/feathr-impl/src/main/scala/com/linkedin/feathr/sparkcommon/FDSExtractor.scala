package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.exception.{ErrorLabel, FrameFeatureJoinException}
import org.apache.spark.sql.{Column, DataFrame}

/**
 * A canned extractor class to extract features which are already present in FDS format. We do not support any type of
 * SQL or MVEL expressions to extract the features. These features will be joined to the observation data as is. Also, it is
 * a pre-requisite for these columns to already be in the FDS format.
 * Usage - Please specify the class name "com.linkedin.frame.sparkcommon.FDSExtractor" in the extractor field of the anchor.
 * All the features contained within that anchor will be extracted using this class.
 * This class is final and cannot be further inherited.
 * @param features List of features to be extracted.
 */
final class FDSExtractor(val features: Set[String]) extends SimpleAnchorExtractorSpark {

  override def getProvidedFeatureNames: Seq[String] = features.toSeq

  /**
   * Return the sequence of feature names to the respective column using the input ddataframe.
   * In this case, as the features are already in the FDS format, the columns will be return as is, without any processing.
   *
   * @param inputDF input dataframe
   * @return Seq of extracted feature names with the columns.
   */
  override def transformAsColumns(inputDF: DataFrame): Seq[(String, Column)] = {
    val schema = inputDF.schema
    features
      .map(featureName => {
        try {
          (featureName, inputDF.col(featureName))
        } catch {
          case e: Exception => throw new FrameFeatureJoinException(ErrorLabel.FEATHR_ERROR, s"Unable to extract column" +
            s" $featureName from the input dataframe with schema $schema.")
        }
      })
  }.toSeq
}


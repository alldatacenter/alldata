package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig
import com.linkedin.feathr.common.{Header, TaggedFeatureName}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * base class for a post processor in feature data generation
  * User can apply customized operation on the feature data, e.g, write to disk/console
  * see example in feathr repo, e.g, [[WriteToConsolePostProcessor]], [[WriteToHDFSPostProcessor]]
  *
  * @param outputProcessorConfig output processor config object
  * @param endTimeOpt end time of feature generation date, in form of yyyy/MM/dd
  */
abstract class OutputProcessor(val outputProcessorConfig: OutputProcessorConfig, endTimeOpt: Option[String] = None) extends  Serializable {
  /**
    * process all feature data(dataframes)
    * @param ss Spark session
    * @param featureData map of string tagged feature name to pair of (feature column name in the dataframe, the dataframe contains the feature)
    *                    each feature is represented as a column in the dataframe
    * @return processed dataframe and header
    */
  def processAll(ss: SparkSession, featureData: Map[TaggedFeatureName, (DataFrame, Header)]): Map[TaggedFeatureName, (DataFrame, Header)]

  /**
    * process a single dataframe, typically called by processAll.
    * @param ss Spark session
    * @param df dataframe contains a set of features
    * @param header contains meta of the datefeathr
    * @param parentPath path to save the dataframe
    * @return processed dataframe and header
    */
  def processSingle(ss: SparkSession, df: DataFrame, header: Header, parentPath: String): (DataFrame, Header)
}
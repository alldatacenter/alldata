package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.common.RichConfig
import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

class SampleAdvancedDerivationFunctionExtractor extends FeatureDerivationFunctionSpark {

  /**
   * transform to produce features
   * input feature names (parameter names) are nc and nd
   */
  override def transform(dataframe: DataFrame): DataFrame = {
    val onlyProduceP = if (_params.isDefined) {
      _params.get.getBooleanWithDefault("onlyProduceP", false)
    } else false
    if (onlyProduceP) {
      dataframe.withColumn("P", expr("nc"))
    } else {
      dataframe
        .withColumn("M", expr("nc"))
        .withColumn("N", expr("nd"))
        .withColumn("Q", expr("nd"))
    }
  }

  /**
   * return produced feature list
   */
  override def getOutputFeatureList(): Seq[String] = Seq("M", "N", "Q", "P")
}

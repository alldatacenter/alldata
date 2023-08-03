package com.linkedin.feathr.offline.derived

import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark
import org.apache.spark.sql.DataFrame

class TestDataFrameDerivationFunctionExtractor extends FeatureDerivationFunctionSpark {
  // input column is nc, nd
  override def transform(dataframe: DataFrame): DataFrame = {
    import org.apache.spark.sql.functions._
    dataframe.withColumn("E", expr("nd"))
  }
}

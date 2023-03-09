package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

// Sample key extractor. The only different between this and the SimpleSampleKeyExtractor is the key column name
// Used to test feature generation with different key tags while forced to join into the same dataframe by the feature
// generation config
class SimpleSampleKeyExtractorWithOtherKey extends SourceKeyExtractor {
  val JOIN_KEY_PREFIX = getClass.getSimpleName + "_"
  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    dataFrame
      .withColumn(JOIN_KEY_PREFIX + "x", explode(expr("y")))
      .withColumn(JOIN_KEY_PREFIX + "entityId", expr("x"))
  }

  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = Seq("entityId", "x").map(JOIN_KEY_PREFIX + _)

}

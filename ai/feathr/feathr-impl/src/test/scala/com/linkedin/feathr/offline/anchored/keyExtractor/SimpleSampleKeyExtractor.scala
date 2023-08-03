package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

class SimpleSampleKeyExtractor extends SourceKeyExtractor {
  val JOIN_KEY_PREFIX = getClass.getSimpleName + "_"
  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    dataFrame
      .withColumn(JOIN_KEY_PREFIX + "x", explode(expr("y")))
      .withColumn(JOIN_KEY_PREFIX + "Mid", expr("x"))
  }

  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = Seq("MId", "x").map(JOIN_KEY_PREFIX + _)

}

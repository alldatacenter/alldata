package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.expr

class SimpleSampleKeyExtractor2 extends SourceKeyExtractor {
  val JOIN_KEY_PREFIX = getClass.getSimpleName + "_"
  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    dataFrame.withColumn(JOIN_KEY_PREFIX + "x", expr("x"))
  }

  override def getKeyColumnNames(datum: Option[Any] = None): Seq[String] = Seq(JOIN_KEY_PREFIX + "x")
}

package com.linkedin.feathr.offline.job

import com.linkedin.feathr.common.{AnchorExtractor, FeatureValue => JavaFeatureValue}
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.avro.generic.GenericRecord
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

// sample user customized extractor expect AVRO generic record
class SampleGenericRecordMVELExtractor extends AnchorExtractor[GenericRecord] {
  private val providedFeatureNames = Seq("mockdata_a_b_row_generic_extractor", "mockdata_a_b_row_generic_extractor2")
  override def getKey(datum: GenericRecord): Seq[String] = {
    Seq(datum.get("x").toString)
  }

  override def getFeatures(datum: GenericRecord): Map[String, JavaFeatureValue] = {
    Map(
      "mockdata_a_b_row_generic_extractor" ->
        JavaFeatureValue.createNumeric(Option(datum.get("y")).getOrElse(0).toString.toInt))
  }

  override def getInputType: Class[_] = classOf[GenericRecord]

  override def getProvidedFeatureNames: Seq[String] = providedFeatureNames
}

// sample user customized key extractor
class SampleDataFrameBasedKeyExtractor extends SourceKeyExtractor {
  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = {
    Seq("x2")
  }

  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    dataFrame.withColumn("x2", expr("x"))
  }
}

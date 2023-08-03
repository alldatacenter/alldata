package com.linkedin.feathr.common

import org.apache.avro.generic.IndexedRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

/**
 * If an AnchorExtractor only works on a Avro record, it should extends
 * this trait, and use convertToAvroRdd to do a one-time batch conversion of DataFrame to RDD of their choice.
 * convertToAvroRdd will be called by Feathr engine before calling getKeyFromRow() and getFeaturesFromRow() in AnchorExtractor.
 */
trait CanConvertToAvroRDD {

  /**
   * One time batch converting the input data source into a RDD[IndexedRecord] for feature extraction later
   * @param df input data source
   * @return batch preprocessed dataframe, as RDD[IndexedRecord]
   */
  def convertToAvroRdd(df: DataFrame) : RDD[IndexedRecord]
}

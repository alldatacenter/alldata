package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.common.AnchorExtractor
import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.util.AnchorUtils.removeNonAlphaNumChars
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._

/**
 * This is the source key extractor class for MVEL based anchor features
 * @param anchorExtractorV1
 */
private[feathr] class MVELSourceKeyExtractor(val anchorExtractorV1: AnchorExtractor[Any], private val keyAlias: Option[Seq[String]] = None)
    extends SourceKeyExtractor {

  private val JOIN_KEY_PREFIX = removeNonAlphaNumChars(anchorExtractorV1.toString) + "_"
  private val MAX_KEY_FIELD_NUM = 5

  /**
   * This is only used for sequential join's MVEL based expand feature evaluation
   * Performance is not as good as raw DataFrame or raw RDD
   * Append key columns to the input dataframe,
   * used to prepare feature data for feature join
   * @param dataFrame
   * @return
   */
  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    val keyColumnNames = if (dataFrame.rdd.isEmpty()) {
      getKeyColumnNames(None)
    } else {
      getKeyColumnNames(Some(dataFrame.first()))
    }
    val outputSchema = StructType(dataFrame.schema.union(StructType(keyColumnNames.map(col => StructField(col, StringType, true)))))
    val encoder = RowEncoder(outputSchema)
    dataFrame
      .map(row => {
        val keys = getKey(row.asInstanceOf[GenericRowWithSchema])
        Row.merge(row, Row.fromSeq(keys))
      })(encoder)
      .toDF()
  }

  def getKey(datum: Any): Seq[String] = {
    anchorExtractorV1.getKeyFromRow(datum)
  }

  /**
   * Return the key column names for a source, if the rdd is empty,
   * pass None as datum, and the function will return empty Seq to signal empty dataframe
   * @param datum a row of the input rdd/dataframe
   * @return
   */
  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = {
    if (datum.isDefined) {
      val size = getKey(datum.get).size
      (1 to size).map(JOIN_KEY_PREFIX + _)
    } else {
      // return empty join key to signal empty dataset
      Seq()
    }
  }

  /**
   * return key column alias
   * if keyAlias is not explicitly set,
   * 1. MVEL expression based anchor will fallback to return the key mvel expression with non-AlphaNum removed
   * 2. Customized extractor that extends AnchorExtractor[_] will return empty Seq
   * @return
   */
  override def getKeyColumnAlias(datum: Option[Any]): Seq[String] = {
    val fallbackKeyAlias = keyExprs.map(removeNonAlphaNumChars)
    keyAlias.getOrElse(fallbackKeyAlias)
  }

  // key expressions of the anchor
  val keyExprs = anchorExtractorV1 match {
    case extractor: SimpleConfigurableAnchorExtractor => extractor.getKeyExpression()
    case _ => Seq.empty[String] // user customized extractor extends anchorExtractorV1 does not have key expression
  }

  // override toString function so that MVEL based anchor with same key expression can be grouped and
  // evaluated on the one dataframe in a sequential order,
  // this helps to reduce the number of joins
  // to the observation data
  // The default toString does not work, because toString of each object have different values
  override def toString: String = getClass.getSimpleName + " with keyExprs:" + keyExprs.mkString(" key:") +
    "anchorExtractor:" + anchorExtractorV1.toString
}
